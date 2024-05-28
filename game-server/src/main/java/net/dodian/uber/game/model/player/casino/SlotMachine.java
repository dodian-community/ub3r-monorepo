package net.dodian.uber.game.model.player.casino;

import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.network.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.network.packets.outgoing.SendMessage;
import net.dodian.uber.game.network.packets.outgoing.SendString;
import net.dodian.utilities.DbTables;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import static net.dodian.utilities.DotEnvKt.getGameWorldId;
import static net.dodian.utilities.DatabaseKt.getDbConnection;
import static net.dodian.utilities.DatabaseKt.getDbStatement;

public class SlotMachine {

    public int slotsGames = -1;
    public int peteBalance = 0;
    public int slotsJackpot = 240000;

    ArrayList<Symbol> symbols = new ArrayList<Symbol>();

    public SlotMachine() {
        symbols.add(new Symbol(1, "0", new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}));
        symbols.add(new Symbol(2, "1", new int[]{17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33}));
        symbols.add(new Symbol(3, "2", new int[]{34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50}));
        symbols.add(new Symbol(4, "3", new int[]{51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67}));
        symbols.add(new Symbol(5, "4", new int[]{68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84}));
        symbols.add(new Symbol(6, "5", new int[]{85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101}));
        symbols.add(new Symbol(7, "X", new int[]{102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125}));
        symbols.add(new Symbol(8, "7", new int[]{126, 127}));
        /*
         * symbols.add(new Symbol(1, "X", new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}));
         * symbols.add(new Symbol(2, "5", new int[] {13, 14, 15, 16, 17, 18, 19, 20,
         * 21, 22, 23})); symbols.add(new Symbol(3, "4", new int[] {25, 26, 27, 28,
         * 29, 30, 31, 32, 33, 34, 35, 36, 37, 9})); symbols.add(new Symbol(4, "3",
         * new int[] {38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 24, 10}));
         * symbols.add(new Symbol(5, "2", new int[] {51, 52, 53, 54, 55, 56, 57, 58,
         * 59, 60, 61, 12})); symbols.add(new Symbol(6, "7", new int[] {63, 62, 50,
         * 11}));
         */
    }

    public Spin spin() {
        Symbol[] symbols = new Symbol[3];
        for (int i = 0; i < 3; i++) {
            int stop = Misc.random(127);
            Symbol s = translate(stop);
            symbols[i] = s;
        }
        return new Spin(symbols);
    }

    private Symbol translate(int stop) {
        for (Symbol s : symbols) {
            if (s.check(stop))
                return s;
        }
        return null;
    }

    public void rollDice(Client c, int amt) {
        if (amt > 50000000 || amt < 100000) {
            c.NpcDialogueSend = true;
            c.convoId = -1;
            c.send(new RemoveInterfaces());
            c.setInterfaceWalkable(-1);
            if (amt > 50000000)
                c.send(new SendMessage("You can't bet more than 50M"));
            if (amt < 100000)
                c.send(new SendMessage("Party pete do not accept anything less then 100k as a gamble!"));
            return;
        }
        if (c.playerHasItem(995, 2000000000)) {
            c.NpcDialogueSend = true;
            c.convoId = -1;
            c.send(new RemoveInterfaces());
            c.send(new SendMessage("You can't bet with more then 2b cash in your inventory!"));
            return;
        }
        if (!c.playerHasItem(995, amt)) {
            c.NpcDialogueSend = true;
            c.convoId = -1;
            c.send(new RemoveInterfaces());
            c.send(new SendMessage("You do not have enough cash!"));
            return;
        }
        c.deleteItem(995, amt);
        c.send(new SendString("", 7815));
        c.send(new SendString("", 8399));
        c.send(new SendString("Royal Dice", 7815));
        c.send(new SendString("Bet: " + amt, 8399));
        int first = ((int) (Math.random() * 999999999) % 6) + 1;
        int second = ((int) (Math.random() * 999999999) % 6) + 1;
        int total = first + second;
        c.send(new SendString(first + "", 8424));
        c.send(new SendString(second + "", 8425));
        if (total > 7 && total < 12) {
            c.send(new SendString("Roll: " + total + ".  You win!", 8426));
            c.addItem(995, amt * 2);
            if (c.playerGroup != 6 || c.playerRights < 2)
                trackDice(1, amt);
        } else if (total == 12) {
            c.send(new SendString("Roll: " + total + ".  You win a Jackpot!", 8426));
            c.addItem(995, amt + amt + amt / 2);
            if ((amt + amt / 2) > 50000000)
                Client.publicyell(c.getPlayerName() + " has won " + Utils.format((amt + amt / 2)) + " gp jackpot at the Dice!");
            if (c.playerGroup != 6 || c.playerRights < 2)
                trackDice(1, (amt + amt / 2));
        } else {
            c.send(new SendString("Roll: " + total + ".  You lose", 8426));
            if (c.playerGroup != 6 || c.playerRights < 2)
                trackDice(2, amt);
        }
        // c.showInterface(6675);
        c.checkItemUpdate();
        c.setInterfaceWalkable(6675);
    }

    public int CoinsBillion_Win, CoinsBillion_Lose = 0;
    public int Coins_Win, Coins_Lose = 0;

    public void loadGamble() {
        if (getGameWorldId() == 5) {
            return;
        }
        try {
            ResultSet results = getDbStatement().executeQuery("SELECT * FROM " + DbTables.GAME_PETE_CO);
            if (results.next()) {
                if (results.getInt("Tracker_ID") == 1) {
                    CoinsBillion_Win = results.getInt("CoinsBillion");
                    Coins_Win = results.getInt("Coins");
                }
                if (results.getInt("Tracker_ID") == 2) {
                    CoinsBillion_Lose = results.getInt("CoinsBillion");
                    Coins_Lose = results.getInt("Coins");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //System.out.println("Lose: " + CoinsBillion_Lose + " billion " + Coins_Lose + " gp.");
        //System.out.println("Win: " + CoinsBillion_Win + " billion " + Coins_Win + " gp.");
    }

    public void trackDice(int id, int amt) {
        if (getGameWorldId() == 5) {
            return;
        }
        try {
            Connection conn = getDbConnection();
            Statement statement = conn.createStatement();
            if (id == 1) {
                Coins_Win = Coins_Win + amt;
                if (Coins_Win > 1000000000) {
                    Coins_Win = Coins_Win - 1000000000;
                    CoinsBillion_Win = CoinsBillion_Win + 1;
                }
                statement.executeUpdate("INSERT " + DbTables.GAME_PETE_CO + " SET CoinsBillion = " + CoinsBillion_Win + ", Coins = " + Coins_Win
                        + " where Tracker_ID=1");
            }
            if (id == 2) {
                Coins_Lose = Coins_Lose + amt;
                if (Coins_Lose > 1000000000) {
                    Coins_Lose = Coins_Lose - 1000000000;
                    CoinsBillion_Lose = CoinsBillion_Lose + 1;
                }
                statement.executeUpdate("INSERT " + DbTables.GAME_PETE_CO + " SET CoinsBillion = " + CoinsBillion_Lose + ", Coins = " + Coins_Lose
                        + " where Tracker_ID=2");
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void playSlots(Client client, int times) {
        if (!client.playerHasItem(995, 3000)) {
            client.send(new RemoveInterfaces());
            client.send(new SendMessage("You don't have enough gold to play!"));
            return;
        }
        slotsGames = times;
        if (slotsGames > 0) {
            // System.out.println("Sending screen");
            slotsGames--;
            client.send(new SendString("PETE'S SLOTS CO - JACKPOT: " + slotsGames + " plays remaining", 13896));
        } else {
            client.send(new SendString("PETE'S SLOTS CO - JACKPOT: Manual play", 13896));
        }
        client.deleteItem(995, 3000);
        peteBalance += peteBalance + 3000 < Integer.MAX_VALUE ? (double) (3000 * 0.15) : 0;
        slotsJackpot += slotsJackpot + 3000 < Integer.MAX_VALUE ? (double) (3000 * 0.50) : 0;
        //client.send(new SendMessage("Debug: PBAL: " + peteBalance + ", JPOT: " + slotsJackpot + ""));
        Spin s = spin();
        int win = s.getWinnings();
        client.send(new SendString("", 13884));
        client.send(new SendString(s.getSymbols()[0].output(), 13885));
        client.send(new SendString(s.getSymbols()[1].output(), 13886));
        client.send(new SendString(s.getSymbols()[2].output(), 13887));
        // client.send(new SendString("The current jackpot is " +
        // Utils.format(slotsJackpot) + " gp!", 18813));
        if (win > 0 && win < 240000) {
            client.addItem(995, s.getWinnings());
            client.send(new SendMessage("You have won " + Utils.format(s.getWinnings()) + " gp!"));
            //client.send(new SendString(Utils.format(s.getWinnings()) + "gp", 18812));
        } else if (win >= 240000) {
            client.send(new SendMessage("You have won the jackpot!  There is a 15% tax on winnings."));
            int amount = (int) ((double) s.getWinnings() * 0.85);
            client.addItem(995, amount);
            client.send(new SendMessage("You receive " + Utils.format(amount) + " gp"));
            client.send(new SendString("Jackpot!", 18812));
            slotsJackpot = (int) ((double) slotsJackpot / 0.85);
            Client.publicyell(client.getPlayerName() + " has won the " + Utils.format(win) + " gp jackpot at the slots!");
            //client.send(new SendString(Utils.format(amount) + "gp", 18812));
        }
    }

}
