package net.dodian.uber.game.model.entity.player;

import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.netty.listener.out.SendString;
import net.dodian.uber.game.netty.listener.out.PlayerDetails;
import net.dodian.uber.game.netty.listener.out.CameraReset;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.DbTables;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.player.quests.QuestSend;

import java.sql.ResultSet;
import java.sql.Statement;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class PlayerInitializer {
    

    public void initializePlayer(Client client) {
        /* Login write settings */
        client.send(new PlayerDetails(client.playerIsMember, client.getSlot()));
        client.send(new CameraReset()); // Resets the camera position
        client.setChatOptions(0, 0, 0);
        client.varbit(287, 1); // SPLIT PRIVATE CHAT ON/OFF
        

        QuestSend.clearQuestName(client);
        client.questPage = 1;
        client.WriteEnergy();
        client.pmstatus(2);
        client.setConfigIds();
        client.resetTabs(); // Set tabs!

        // Now that interface structure is set up, refresh all skills including HP
        Skill.enabledSkills().forEach(skill -> {
            client.refreshSkill(skill);
        });

        if (client.lookNeeded) {
            client.showInterface(3559);
        } else {
            client.setLook(client.playerLooks);
        }
        

        client.checkItemUpdate();
        for (int i = 0; i < Equipment.SIZE; i++) { // Equipment
            client.setEquipment(client.getEquipment()[i], client.getEquipmentN()[i], i);
        }
        
        /* Friend configs */
        for (Client c : PlayerHandler.playersOnline.values()) {
            if (c.hasFriend(client.longName)) {
                c.refreshFriends();
            }
        }
        
        initializeInterfaceTexts(client);

        
        client.loaded = true;
        //TODO everyone is premium for now
        client.premium = true;
        /* Initialize save timers */
        client.lastSave = System.currentTimeMillis();
        client.lastProgressSave = client.lastSave;
        
        /* Set a player active to a world */
        PlayerHandler.playersOnline.put(client.longName, client);
        sendWelcomeMessages(client);
        checkRefundedItems(client);

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
     * Checks for any  refunded items the player needs to claim.
     */
    private void checkRefundedItems(Client client) {
        String query = "SELECT * FROM " + DbTables.GAME_REFUND_ITEMS + " WHERE receivedBy='" + client.dbId +
                      "' AND message='0' AND claimed IS NULL ORDER BY date ASC";
        
        try (java.sql.Connection conn = getDbConnection();
             Statement stm = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
             ResultSet rs = stm.executeQuery(query)) {
            
            boolean gotResult = rs.next();
            if (gotResult) {
                client.send(new SendMessage("<col=4C4B73>You have some unclaimed items to claim!"));
                stm.executeUpdate("UPDATE " + DbTables.GAME_REFUND_ITEMS + " SET message='1' where message='0'");
            }
        } catch (Exception e) {
            System.out.println("Error in checking sql!!" + e.getMessage() + ", " + e);
        }
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
