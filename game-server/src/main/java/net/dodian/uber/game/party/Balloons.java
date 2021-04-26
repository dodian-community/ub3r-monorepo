package net.dodian.uber.game.party;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.entity.player.Player;
import net.dodian.uber.game.model.entity.player.PlayerHandler;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.model.item.GroundItem;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.player.packets.outgoing.InventoryInterface;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.Misc;
import net.dodian.utilities.Utils;

import java.util.ArrayList;

public class Balloons {

    private static ArrayList<Object> balloons = new ArrayList<Object>();
    private static ArrayList<Position> partyEventPos = new ArrayList<Position>();
    private static ArrayList<RewardItem> partyItems = new ArrayList<RewardItem>();
    private static ArrayList<RewardItem> droppedItems = new ArrayList<RewardItem>();
    private static boolean eventActive = false;
    private static int defaultBalloons = 63;
    private static int defaultIncrement = 9;
    private static int MAX_LENGTH = 200;
    private static int totalBalloons = defaultBalloons;
    private static int balloonIncrement = defaultIncrement;

    public static void triggerBalloonEvent(Client c) {
        if (spawnBalloon(c.getPosition().copy()))
            Client.publicyell(c.getPlayerName() + " just spawned a balloon! Go and pop it!");
    }

    private static boolean canAddPos(Position pos) {
        if (pos.getX() >= 3042 && pos.getX() <= 3049 && pos.getY() == 3378)
            return false;
        if ((pos.getX() == 3051 && pos.getY() == 3375) || (pos.getX() == 3040 && pos.getY() == 3375) || (pos.getX() == 3040 && pos.getY() == 3381) || (pos.getX() == 3051 && pos.getY() == 3381))
            return false;
        if ((pos.getX() == 3045 && pos.getY() == 3384) || (pos.getX() == 3048 && pos.getY() == 3372))
            return false;
        for (Object balloon : balloons) {
            if (balloon.x == pos.getX() && balloon.y == pos.getY())
                return false;
        }
        return true;
    }

    private static boolean inPartyRoom(Position pos) {
        return pos.getX() >= 3036 && pos.getX() <= 3055 && pos.getY() >= 3370 && pos.getY() <= 3385;
    }

    private static void setPartyPos() {
        for (int x = 3039; x <= 3052; x++) {
            for (int y = 3372; y <= 3384; y++) {
                Position checkPos = new Position(x, y, 0);
                if (canAddPos(checkPos))
                    partyEventPos.add(checkPos);
            }
        }
    }

    private static void sendPartyTimer(String message) {
        for (Player p : PlayerHandler.players) {
            if (p == null || !p.isActive) {
                continue;
            }
            Client temp = (Client) p;
            if (inPartyRoom(temp.getPosition())) {
                temp.send(new SendMessage(message));
            }
        }
    }

    public static void spawnPartyEventBalloon() {
        for (int i = 1; i <= (totalBalloons > balloonIncrement ? balloonIncrement : totalBalloons); i++) {
            if (!partyEventPos.isEmpty()) {
                int random = Misc.random(partyEventPos.size() - 1);
                Position pos = partyEventPos.get(random);
                partyEventPos.remove(random);
                spawnBalloon(pos);
            } else
                totalBalloons = 0;
        }
    }

    public static void triggerPartyEvent(Client c) {
        if (eventActive) {
            c.send(new SendMessage("Event is already active!"));
            return;
        }
        eventActive = true;
        Client.publicyell("<col=664400>A drop party has been started in the Partyroom!");
        partyEventPos.clear();
        setPartyPos();
        EventManager.getInstance().registerEvent(new Event(600) {
            int timer = 9;

            @Override
            public void execute() {
                /* Initiate a balloon event timer! */
                if (timer == 0) {
                    sendPartyTimer("PARTYYYYYYYYYYYYYYYYYYYYYYYYYY! Get popping!");
                    spawnPartyEventBalloon();
                    totalBalloons -= balloonIncrement;
                    EventManager.getInstance().registerEvent(new Event(600) {
                        int timer = 4;

                        @Override
                        public void execute() {
                            if (totalBalloons <= 0) {
                                eventActive = false;
                                totalBalloons = defaultBalloons;
                                balloonIncrement = defaultIncrement;
                                Client.publicyell("<col=664400>The drop party in the Partyroom has just concluded!");
                                stop();
                            }
                            if (timer == 0) {
                                spawnPartyEventBalloon();
                                totalBalloons -= balloonIncrement;
                                timer = 4;
                            } else
                                timer--;
                        }
                    });
                    stop();
                } else
                    sendPartyTimer("Partyroom drops commencing in: " + timer);
                timer--;
            }
        });
    }

    public static boolean spawnBalloon(Position pos) {
        for (Object balloon : balloons) {
            if (pos.getX() == balloon.x && pos.getY() == balloon.y) {
                return false;
            }
        }
        int id = 115 + Utils.random(7);
        Object obj = new Object(id, pos.getX(), pos.getY(), pos.getZ(), 10, 0);
        balloons.add(obj);
        boolean check = Misc.random(99) < 75;
        if (!partyItems.isEmpty() && check) { //Add loot!
            int random = Misc.random(partyItems.size() - 1);
            partyItems.get(random).setPosition(pos);
            droppedItems.add(partyItems.get(random));
            partyItems.remove(random);
        }
        for (int slot = 0; slot < PlayerHandler.players.length; slot++) {
            Player p = PlayerHandler.players[slot];
            if (p != null) {
                Client person = (Client) p;
                if (person.distanceToPoint(pos.getX(), pos.getY()) <= 104) {
                    person.ReplaceObject2(pos.getX(), pos.getY(), id, 0, 10);
                    if (person.isPartyInterface)
                        displayItems(person);
                }
            }
        }
        return true;
    }

    public static boolean lootBalloon(Client c, Position pos) {
        for (Object balloon : balloons) {
            if (pos.getX() == balloon.x && pos.getY() == balloon.y) {
                balloons.remove(balloon);
                c.requestAnim(794, 0);
                c.ReplaceObject2(balloon.x, balloon.y, balloon.id + 8, 0, 10);
                EventManager.getInstance().registerEvent(new Event(600) {
                    @Override
                    public void execute() {
                        if (!droppedItems.isEmpty()) {
                            for (int i = 0; i < droppedItems.size(); i++) {
                                if (droppedItems.get(i).getPosition().equals(pos)) {
                                    GroundItem drop = new GroundItem(pos.getX(), pos.getY(), droppedItems.get(i).getId(), droppedItems.get(i).getAmount(), c.getSlot(), -1);
                                    Ground.items.add(drop);
                                    c.send(new SendMessage("<col=664400>Something odd appears on the ground."));
                                    droppedItems.remove(i);
                                }
                            }
                        } else
                            c.send(new SendMessage("<col=664400>The balloon bursts open and yields nothing."));
                        if (inPartyRoom(pos) && !partyEventPos.contains(pos)) //Adding the spawn back to the array!
                            partyEventPos.add(pos);
                        stop();
                    }
                });
                for (int slot = 0; slot < PlayerHandler.players.length; slot++) {
                    Player p = PlayerHandler.players[slot];
                    if (p != null || p == c) {
                        Client person = (Client) p;
                        if (person.distanceToPoint(balloon.x, balloon.y) <= 104) {
                            person.ReplaceObject2(balloon.x, balloon.y, balloon.id + 8, 0, 10);
                            EventManager.getInstance().registerEvent(new Event(1200) {
                                @Override
                                public void execute() { //To delete the object after 2 ticks!
                                    person.ReplaceObject2(balloon.x, balloon.y, -1, 0, 10);
                                    stop();
                                }
                            });
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public static void openInterface(Client c) {
        displayItems(c);
        displayOfferItems(c);
        c.resetItems(5064);
        c.send(new InventoryInterface(2156, 5063));
        c.isPartyInterface = true;
    }

    public static void displayItems(Client c) {
        c.getOutputStream().createFrameVarSizeWord(53);
        c.getOutputStream().writeWord(2273);
        c.getOutputStream().writeWord(partyItems.size());
        for (int i = 0; i < partyItems.size(); i++) {
            if (partyItems.get(i).getId() > 254) {
                c.getOutputStream().writeByte(255);
                c.getOutputStream().writeDWord_v2(partyItems.get(i).getAmount());
            } else
                c.getOutputStream().writeByte(partyItems.get(i).getAmount());
            c.getOutputStream().writeWordBigEndianA(partyItems.get(i).getId() + 1); // item id
        }
        c.getOutputStream().endFrameVarSizeWord();
    }

    public static void displayOfferItems(Client c) {
        c.getOutputStream().createFrameVarSizeWord(53);
        c.getOutputStream().writeWord(2274);
        c.getOutputStream().writeWord(c.offeredPartyItems.size());
        for (int i = 0; i < c.offeredPartyItems.size(); i++) {
            if (c.offeredPartyItems.get(i).getId() > 254) {
                c.getOutputStream().writeByte(255);
                c.getOutputStream().writeDWord_v2(c.offeredPartyItems.get(i).getAmount());
            } else
                c.getOutputStream().writeByte(c.offeredPartyItems.get(i).getAmount());
            c.getOutputStream().writeWordBigEndianA(c.offeredPartyItems.get(i).getId() + 1); // item id
        }
        c.getOutputStream().endFrameVarSizeWord();
    }

    public static void offerItems(Client c, int id, int amount, int slot) {
        if (c.playerRights < 2) { //Only staff should be able to do this!
            return;
        }
        boolean stackable = Server.itemManager.isStackable(id);
        amount = !stackable && amount >= 8 - c.offeredPartyItems.size() ? 8 - c.offeredPartyItems.size() : amount;
        if (!stackable) {
            amount = amount > c.getInvAmt(id) ? c.getInvAmt(id) : amount;
            for (int i = 0; i < amount; i++) {
                c.deleteItem(id, 1);
                c.offeredPartyItems.add(new RewardItem(id, 1));
            }
        } else {
            amount = amount > c.getInvAmt(id) ? c.getInvAmt(id) : amount;
            c.deleteItem(id, amount);
            boolean found = false;
            for (RewardItem item : c.offeredPartyItems) {
                if (item.getId() == id) {
                    found = true;
                    item.setAmount(amount);
                }
            }
            if (!found) {
                if (c.offeredPartyItems.size() < 8)
                    c.offeredPartyItems.add(new RewardItem(id, amount));
                else
                    amount = 0;
            }
        }
        if (amount > 0) { //To update!
            displayOfferItems(c);
            c.resetItems(5064);
        }
    }

    public static void removeOfferItems(Client c, int id, int amount, int slot) {
        if (c.playerRights < 2) { //Only staff should be able to do this!
            return;
        }
        boolean stackable = Server.itemManager.isStackable(id);
        int checkAmt = 0;
        if (!stackable) {
            for (int i = 0; i < c.offeredPartyItems.size(); i++)
                if (c.offeredPartyItems.get(i).getId() == id)
                    checkAmt++;
            amount = amount > checkAmt ? checkAmt : amount;
            for (int i = 0; i < amount; i++) {
                c.addItem(id, 1);
                for (RewardItem item : c.offeredPartyItems)
                    if (item.getId() == id) {
                        c.offeredPartyItems.remove(item);
                        break;
                    }
            }
        } else {
            if (amount >= c.offeredPartyItems.get(slot).getAmount()) {
                amount = c.offeredPartyItems.get(slot).getAmount();
                c.offeredPartyItems.remove(slot);
            } else {
                c.offeredPartyItems.get(slot).setAmount(-amount);
            }
            c.addItem(id, amount);
        }
        if (amount > 0) { //To update!
            displayOfferItems(c);
            c.resetItems(5064);
        }
    }

    public static void acceptItems(Client c) {
        if (partyItems.size() >= MAX_LENGTH) {
            c.send(new SendMessage("You cant put in more items!"));
            return;
        }
        for (int i = c.offeredPartyItems.size() - 1; c.offeredPartyItems.size() > 0 && partyItems.size() < MAX_LENGTH; i--) {
            partyItems.add(c.offeredPartyItems.get(i));
            c.offeredPartyItems.remove(i);
        }
        displayItems(c);
        displayOfferItems(c);
    }

	/*public static void displayItems(Client c) {
		c.getOutputStream().createFrameVarSizeWord(34);
		c.getOutputStream().writeWord(2273);
		//c.getOutputStream().writeWord(partyItems.size());
		for (int i = 0; i < partyItems.size(); i++) {
			c.getOutputStream().writeByte(i);
			c.getOutputStream().writeWord(partyItems.get(i).getId() + 1);
			if (partyItems.get(i).getId() > 254) {
				c.getOutputStream().writeByte(255);
				c.getOutputStream().writeDWord(partyItems.get(i).getAmount());
			} else
				c.getOutputStream().writeByte(partyItems.get(i).getAmount());
		}
		c.getOutputStream().endFrameVarSizeWord();
	}
	public static void displayOfferItems(Client c) {
		c.getOutputStream().createFrameVarSizeWord(34);
		c.getOutputStream().writeWord(2274);
		//c.getOutputStream().writeWord(partyItems.size());
		for (int i = 0; i < offeredItems.size(); i++) {
			c.getOutputStream().writeByte(i);
			c.getOutputStream().writeWord(offeredItems.get(i).getId() + 1);
			if (offeredItems.get(i).getId() > 254) {
				c.getOutputStream().writeByte(255);
				c.getOutputStream().writeDWord(offeredItems.get(i).getAmount());
			} else
				c.getOutputStream().writeByte(offeredItems.get(i).getAmount());
		}
		c.getOutputStream().endFrameVarSizeWord();
	}*/

    public static boolean eventActive() {
        return eventActive;
    }

    public static boolean spawnedBalloons() {
        return totalBalloons < defaultBalloons;
    }

    public static void updateBalloons(Client c) {
        for (Object balloon : balloons) {
            if (c.distanceToPoint(balloon.x, balloon.y) <= 104)
                c.ReplaceObject2(balloon.x, balloon.y, balloon.id, 0, 10);
        }
    }

}
