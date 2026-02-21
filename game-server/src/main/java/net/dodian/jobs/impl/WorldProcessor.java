package net.dodian.jobs.impl;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.persistence.WorldDbPollService;
import net.dodian.uber.game.persistence.WorldPollInput;
import net.dodian.uber.game.persistence.WorldPollResult;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.dodian.utilities.DotEnvKt.getAsyncWorldDbEnabled;
import static net.dodian.utilities.DotEnvKt.getGameWorldId;

public class WorldProcessor implements Runnable {

    private static Integer cachedLatestNewsId = null;

    @Override
    public void run() {
        Thread.currentThread().setName("WorldProcessor-Thread");
        try {
            WorldPollInput input = createInput();

            WorldPollResult result;
            if (getAsyncWorldDbEnabled()) {
                WorldDbPollService.pollAsync(input);
                result = WorldDbPollService.getLatestResult();
            } else {
                result = WorldDbPollService.runBlockingPoll(input);
            }

            applyResult(result);
            Server.chat.clear();
        } catch (Exception e) {
            System.err.println("Critical error in WorldProcessor run: " + e.getMessage());
            e.printStackTrace();
        } finally {
            Thread.currentThread().setName("WorldProcessor-Thread-Idle");
        }
    }

    private WorldPollInput createInput() {
        List<Integer> onlinePlayerDbIds = Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> (Client) p)
                .map(c -> c.dbId)
                .collect(Collectors.toList());

        return new WorldPollInput(getGameWorldId(), PlayerHandler.getPlayerCount(), onlinePlayerDbIds);
    }

    private void applyResult(WorldPollResult result) {
        if (result == null || result == WorldPollResult.EMPTY) {
            return;
        }

        applyLatestNews(result);
        applyRefundNotifications(result);
        applyMuteAndBanState(result);
    }

    private void applyLatestNews(WorldPollResult result) {
        Integer latestNews = result.getLatestNewsId();
        if (latestNews == null) {
            return;
        }

        if (cachedLatestNewsId == null || latestNews > cachedLatestNewsId) {
            cachedLatestNewsId = latestNews;
            notifyPlayersOfNews(latestNews);
        }
    }

    private void notifyPlayersOfNews(int newsId) {
        Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> (Client) p)
                .filter(c -> c.loadingDone && c.latestNews != newsId)
                .forEach(c -> {
                    c.latestNews = newsId;
                    c.send(new SendMessage("[SERVER]: There is a new post on the homepage! type ::news"));
                });
    }

    private void applyRefundNotifications(WorldPollResult result) {
        if (result.getPlayersWithRefunds().isEmpty()) {
            return;
        }

        Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> (Client) p)
                .filter(c -> c.loadingDone && result.getPlayersWithRefunds().contains(c.dbId))
                .forEach(c -> c.send(new SendMessage("<col=4C4B73>You have some unclaimed items to claim!")));
    }

    private void applyMuteAndBanState(WorldPollResult result) {
        Arrays.stream(PlayerHandler.players)
                .filter(Objects::nonNull)
                .filter(p -> p instanceof Client)
                .map(p -> (Client) p)
                .forEach(c -> {
                    Long muteTime = result.getMuteTimes().get(c.dbId);
                    if (muteTime != null && c.mutedTill != muteTime) {
                        c.mutedTill = muteTime;
                    }

                    if (result.getBannedPlayerIds().contains(c.dbId)) {
                        c.disconnected = true;
                    }
                });
    }
}
