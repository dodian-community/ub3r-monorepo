package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.netty.listener.out.SendMessage;

import java.util.ArrayList;

class PlayerAccountState {
    private final Player owner;
    private volatile int saveDirtyMask = 0;
    private volatile long lastSavedRevision = 0L;
    private volatile long saveRevision = 0L;
    private volatile long lastProcessedCycle = 0L;

    PlayerAccountState(Player owner) {
        this.owner = owner;
    }

    String getPlayerName() {
        return owner.playerName;
    }

    void setPlayerName(String playerName) {
        owner.playerName = playerName;
    }

    String getPlayerPass() {
        return owner.playerPass;
    }

    void setPlayerPass(String playerPass) {
        owner.playerPass = playerPass;
    }

    int getDbId() {
        return owner.dbId;
    }

    void setDbId(int dbId) {
        owner.dbId = dbId;
    }

    int getPlayerRights() {
        return owner.playerRights;
    }

    void setPlayerRights(int playerRights) {
        owner.playerRights = playerRights;
    }

    int getPlayerGroup() {
        return owner.playerGroup;
    }

    void setPlayerGroup(int playerGroup) {
        owner.playerGroup = playerGroup;
    }

    boolean isPremium() {
        return owner.premium;
    }

    void setPremium(boolean premium) {
        owner.premium = premium;
    }

    int getLatestNews() {
        return owner.latestNews;
    }

    void setLatestNews(int latestNews) {
        owner.latestNews = latestNews;
    }

    String getConnectedFrom() {
        return owner.connectedFrom;
    }

    void setConnectedFrom(String connectedFrom) {
        owner.connectedFrom = connectedFrom;
    }

    int getIp() {
        return owner.ip;
    }

    void setIp(int ip) {
        owner.ip = ip;
    }

    String getUUID() {
        return owner.UUID;
    }

    void setUUID(String uuid) {
        owner.UUID = uuid;
    }

    boolean isInitialized() {
        return owner.initialized;
    }

    void setInitialized(boolean initialized) {
        owner.initialized = initialized;
    }

    boolean isDisconnected() {
        return owner.disconnected;
    }

    void setDisconnected(boolean disconnected) {
        owner.disconnected = disconnected;
    }

    boolean isActivePlayer() {
        return owner.isActive;
    }

    void setActivePlayer(boolean active) {
        owner.isActive = active;
    }

    boolean isKicked() {
        return owner.isKicked;
    }

    void setKicked(boolean kicked) {
        owner.isKicked = kicked;
    }

    boolean isSaveNeeded() {
        return owner.saveNeeded;
    }

    void setSaveNeeded(boolean saveNeeded) {
        owner.saveNeeded = saveNeeded;
    }

    boolean isYellOn() {
        return owner.yellOn;
    }

    void setYellOn(boolean yellOn) {
        owner.yellOn = yellOn;
    }

    boolean isDiscord() {
        return owner.discord;
    }

    void setDiscord(boolean discord) {
        owner.discord = discord;
    }

    boolean isInstaLoot() {
        return owner.instaLoot;
    }

    void setInstaLoot(boolean instaLoot) {
        owner.instaLoot = instaLoot;
    }

    int getDailyLogin() {
        return owner.dailyLogin;
    }

    void setDailyLogin(int dailyLogin) {
        owner.dailyLogin = dailyLogin;
    }

    ArrayList<String> getDailyReward() {
        return owner.dailyReward;
    }

    void defaultDailyReward(Client client) {
        owner.dailyReward.add(0, client.today.getTime() + "");
        owner.dailyReward.add(1, "6000");
        owner.dailyReward.add(2, "0");
        owner.dailyReward.add(3, "0");
        owner.dailyReward.add(4, "60");
    }

    void battlestavesData() {
        if (owner.dailyReward.isEmpty()) {
            return;
        }
        int time = Integer.parseInt(owner.dailyReward.get(1));
        int amount = Integer.parseInt(owner.dailyReward.get(2));
        int current = Integer.parseInt(owner.dailyReward.get(3));
        int maxAmount = Integer.parseInt(owner.dailyReward.get(4));
        if (current == maxAmount) {
            return;
        }
        time -= 1;
        if (time <= 0) {
            owner.dailyReward.set(1, "6000");
            owner.dailyReward.set(2, (amount + 20) + "");
            owner.dailyReward.set(3, (current + 20) + "");
            ((Client) owner).send(new SendMessage("<col=ff6200>You got " + (amount + 20) + " battlestaves that you can claim at Baba Yaga."));
        } else {
            owner.dailyReward.set(1, time + "");
        }
    }

    void markSaveDirty(int segmentMask) {
        saveDirtyMask |= segmentMask;
        saveRevision++;
    }

    void clearSaveDirtyMask(int segmentMask) {
        saveDirtyMask &= ~segmentMask;
    }

    void clearAllSaveDirty() {
        saveDirtyMask = 0;
        lastSavedRevision = saveRevision;
    }

    int getSaveDirtyMask() {
        return saveDirtyMask;
    }

    long getLastSavedRevision() {
        return lastSavedRevision;
    }

    long getSaveRevision() {
        return saveRevision;
    }

    void setLastSavedRevision(long lastSavedRevision) {
        this.lastSavedRevision = lastSavedRevision;
    }

    long getLastProcessedCycle() {
        return lastProcessedCycle;
    }

    void setLastProcessedCycle(long lastProcessedCycle) {
        this.lastProcessedCycle = lastProcessedCycle;
    }
}
