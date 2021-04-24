package net.dodian.uber.game.event;

import net.dodian.uber.game.Server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager implements Runnable {

  /**
   * The world instance.
   */
  private static EventManager instance = null;

  /**
   * A list of connected players.
   */
  // private EntityList<Player> players;

  public static EventManager getInstance() {
    if (instance == null) {
      instance = new EventManager();
    }
    return instance;
  }

  private EventManager() {
    events = new CopyOnWriteArrayList<Event>();
    eventsToAdd = new CopyOnWriteArrayList<Event>();
    eventsToRemove = new CopyOnWriteArrayList<Event>();
  }

  /**
   * A list of pending events.
   */
  private List<Event> events;
  private List<Event> eventsToAdd;
  private List<Event> eventsToRemove;

  public void run() {
    while (!Server.shutdownServer) {
      processEvents();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        break;
      } catch (Exception e) {
        e.printStackTrace();
        break;
      }
    }

  }

  /**
   * Registers an event.
   * 
   * @param event
   */
  public void registerEvent(Event event) {
    synchronized (eventsToAdd) {
      eventsToAdd.add(event);
    }
  }

  /**
   * Processes any pending events.
   */
  public void processEvents() {
    for (Event e : eventsToAdd) {
      events.add(e);
    }
    eventsToAdd.clear();
    for (Event e : events) {
      if (e == null) {
        eventsToRemove.add(e);
        continue;
      }
      if (e.isStopped()) {
        eventsToRemove.add(e);
      } else if (e.isReady()) {
        try {
          e.run();
        } catch (Exception exception) {
          exception.printStackTrace();
          eventsToRemove.add(e);
        }
      }
    }
    for (Event e : eventsToRemove) {
      events.remove(e);
    }
    eventsToRemove.clear();
  }

}
