package net.dodian.uber.game.model.entity.player;

import java.util.ArrayList;

/**
 * Handles all the stuff for fixing the trading dupe
 * 
 * @author Fabrice L
 *
 */

public class TradeDupeFix {

  /**
   * List of all the players involved in trade processes
   */
  private static ArrayList<Client> traders = new ArrayList<Client>();
  private static ArrayList<Client> tradingWith = new ArrayList<Client>();

  /**
   * returns the traders lists
   * 
   * @return traders
   */
  public static ArrayList<Client> getTraders() {
    return traders;
  }

  public static ArrayList<Client> getTradeWith() {
    return tradingWith;
  }

  /**
   * adds a new trader to the list
   * 
   * @param client
   */
  public static void add(Client client, Client otherClient) {
    getTraders().add(client);
    getTradeWith().add(otherClient);
  }

  /**
   * removes a trader from the list
   * 
   * @param client
   */
  public static void remove(Client client, Client otherClient) {
    for (int i = 0; i < traders.size(); i++) {
      if (traders.contains(client)) {
        traders.remove(i);
      }
    }
    for (int i = 0; i < tradingWith.size(); i++) {
      if (tradingWith.contains(otherClient)) {
        tradingWith.remove(i);
      }
    }
  }

  /**
   * Checks whether the player is in the list or not
   * 
   * @param client
   * @param otherClient
   * @return boolean
   */
  public static boolean contains(Client client, Client otherClient) {
    if (tradingWith.contains(otherClient)) {
      otherClient.declineTrade();
      return true;
    }
    if (traders.contains(client)) {
      client.declineTrade();
      return true;
    }
    return false;
  }

}
