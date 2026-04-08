package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.listener.out.SendString;
import net.dodian.uber.game.netty.listener.out.PlayerDetails;
import net.dodian.uber.game.netty.listener.out.CameraReset;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.uber.game.systems.skills.ProgressionService;
import net.dodian.uber.game.systems.api.content.ContentRuntimeApi;
import net.dodian.uber.game.persistence.db.DbTables;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.quests.QuestSend;
import net.dodian.uber.game.persistence.account.AccountPersistenceService;
import net.dodian.uber.game.engine.lifecycle.PlayerDeferredLifecycleService;
import net.dodian.uber.game.systems.world.player.PlayerRegistry;

import java.sql.ResultSet;
import java.sql.Statement;

import static net.dodian.uber.game.persistence.db.DatabaseKt.getDbConnection;

public class PlayerInitializer {
    public void initializePlayer(Client client) {
        initializeCriticalLoginState(client);
        initializeDeferredPostLoginState(client);
    }

    public void initializeCriticalLoginState(Client client) {
        client.clearVerticalTravelState();
        /* Login write settings */
        client.send(new PlayerDetails(client.playerIsMember, client.getSlot()));
        client.send(new CameraReset()); // Resets the camera position
        client.setChatOptions(0, 0, 0);
        client.varbit(287, 1); // SPLIT PRIVATE CHAT ON/OFF
        

        QuestSend.clearQuestName(client);
        client.questPage = 1;
        client.pmstatus(2);
        client.setConfigIds();
        client.resetTabs(); // Set tabs!

        // Now that interface structure is set up, refresh all skills including HP
        Skill.enabledSkills().forEach(skill -> {
            ProgressionService.refresh(client, skill);
        });

        if (client.lookNeeded) {
            client.openInterface(3559);
        } else {
            client.setLook(client.playerLooks);
        }
        

        client.checkItemUpdate();
        for (int i = 0; i < Equipment.SIZE; i++) { // Equipment
            client.setEquipment(client.getEquipment()[i], client.getEquipmentN()[i], i);
        }

        client.loaded = true;
        //TODO everyone is premium for now
        client.premium = true;
        /* Initialize save timers */
        long now = System.currentTimeMillis();
        long minuteJitterMs = (client.dbId > 0 ? (client.dbId % 60L) : (client.getSlot() % 60L)) * 1000L;
        long hourJitterMs = (client.dbId > 0 ? (client.dbId % 3600L) : (client.getSlot() % 3600L)) * 1000L;
        client.lastSave = now + minuteJitterMs;
        client.lastProgressSave = now + hourJitterMs;
        PlayerDeferredLifecycleService.schedulePeriodicPersistence(client);
        PlayerDeferredLifecycleService.scheduleDailyResetTrigger(client);
    }

    public void initializeDeferredPostLoginState(Client client) {
        ContentRuntimeApi.onFarmingLogin(client, System.currentTimeMillis());
        initializeInterfaceTexts(client);
        client.onPostLoginUiInit();
        client.refreshFriends();
        refreshPresenceForInterestedFriends(client);
        sendWelcomeMessages(client);
        // Refund checks are account-db owned to avoid blocking the game thread.
        AccountPersistenceService.submitRefundCheck(client);
    }

    private void refreshPresenceForInterestedFriends(Client client) {
        for (Client other : PlayerRegistry.playersOnline.values()) {
            if (other != client && other.hasFriend(client.longName)) {
                other.refreshFriends();
            }
        }
    }
    
    /**
     * Initializes various interface texts for the player.
     */
    private void initializeInterfaceTexts(Client client) {
        client.send(new SendString("Click here to logout", 2458));
        client.send(new SendString("Using this will send a notification to all online mods", 5967));
        client.send(new SendString("@yel@Then click below to indicate which of our rules is being broken.", 5969));
        client.send(new SendString("4: Bug abuse (includes noclip)", 5974));
        client.send(new SendString("5: Dodian staff impersonation", 5975));
        client.send(new SendString("6: Monster luring or abuse", 5976));
        client.send(new SendString("8: Item Duplication", 5978));
        client.send(new SendString("10: Misuse of yell channel", 5980));
        client.send(new SendString("12: Possible duped items", 5982));
        client.send(new SendString("Old magic", 12585));
        client.send(new SendString("", 6067));
        client.send(new SendString("", 6071));
    }
    
    /**
     * Sends welcome messages to the player upon login.
     */
    private void sendWelcomeMessages(Client client) {
        client.send(new SendMessage("Welcome to Uber Server"));
        if (client.newPms > 0) {
            client.send(new SendMessage("You have " + client.newPms + " new messages. Check your inbox at Dodian.net to view them."));
        }
        if (client.playerGroup <= 3) {
            client.send(new SendMessage("Please activate your forum account by checking your mail or junk mail."));
            client.send(new SendMessage("If you still can't find it, contact a staff member."));
        }
    }
}
