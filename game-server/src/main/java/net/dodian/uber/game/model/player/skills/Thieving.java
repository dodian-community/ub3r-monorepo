package net.dodian.uber.game.model.player.skills;

import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.object.GlobalObject;
import net.dodian.uber.game.model.object.Object;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;
import net.dodian.utilities.Range;


public class Thieving {

    public static final int PICKPOCKET_EMOTE = 881;

    public static final int STALL_THIEVING_EMOTE = 832;

    public static final int EMPTY_STALL_ID = 634;

    public enum ThievingType {
        PICKPOCKETING,
        STALL_THIEVING,
        OTHER
    }

    public enum ThievingData {
        FARMER(3086, 10, 800, new int[]{314}, new Range[]{new Range(2, 5)}, new int[]{100}, 0, ThievingType.PICKPOCKETING),
        MASTER_FARMER(3257, 70, 1200, new int[]{314}, new Range[]{new Range(4, 10)}, new int[]{100}, 0, ThievingType.PICKPOCKETING),
        CAGE(20873, 1, 150, new int[]{995}, new Range[]{new Range(20, 50)}, new int[]{100}, 0, ThievingType.OTHER),
        BAKER_STALL(11730, 10, 1000, new int[]{2309}, new Range[]{new Range(1, 1)}, new int[]{100}, 12, ThievingType.STALL_THIEVING),
        FUR_STALL(11732, 40, 1800, new int[]{1751, 1753, 1739, 1759, 995}, new Range[]{new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(500, 1200)}, new int[]{5, 10, 15, 20, 100}, 25, ThievingType.STALL_THIEVING),
        SILVER_STALL(11734, 65, 2500, new int[]{2349, 2351, 2353, 2357, 2359, 995}, new Range[]{new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(800, 1500)}, new int[]{5, 10, 15, 20, 25, 100}, 25, ThievingType.STALL_THIEVING),
        SPICE_STALL(11733, 80, 4800, new int[]{215, 213, 209, 207, 203, 199}, new Range[]{new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1)}, new int[]{5, 10, 20, 35, 55, 100}, 35, ThievingType.STALL_THIEVING),
        GEM_STALL(11731, 90, 5800, new int[]{1617, 1619, 1621, 1623, 995}, new Range[]{new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1, 1), new Range(1200, 2500)}, new int[]{2, 5, 8, 15, 100}, 38, ThievingType.STALL_THIEVING);
        //RINGBELL(6847, 1, 0, new int[] {4084}, new int[] {1}, new int[] {100}, 25000000, ThievingType.OTHER);

        ThievingData(int entityId, int requiredLevel, int receivedExperience, int[] item, Range[] itemAmount, int[] itemChance, int respawnTime, ThievingType type) {
            this.entityId = entityId;
            this.requiredLevel = requiredLevel;
            this.receivedExperience = receivedExperience;

            this.item = item;
            this.itemAmount = itemAmount;
            this.itemChance = itemChance;
            this.respawnTime = respawnTime;
            this.type = type;
        }

        public final int entityId, requiredLevel, receivedExperience, respawnTime;

        public final int[] item;

        public final Range[] itemAmount;

        public final int[] itemChance;

        public final ThievingType type;

        public int getEntityId() {
            return entityId;
        }

        public int getRequiredLevel() {
            return requiredLevel;
        }

        public int getReceivedExperience() {
            return receivedExperience;
        }

        public int[] getItemId() {
            return item;
        }

        public Range[] getItemAmount() {
            return itemAmount;
        }

        public int[] getItemItemChance() {
            return itemChance;
        }

        public int getRespawnTime() {
            return respawnTime;
        }

        public ThievingType getThievingType() {
            return type;
        }

    }

    /**
     * This method is used to determine what information should be gathered if the entity you're thieving from exists in the Enum.
     *
     * @return id
     */
    public static ThievingData forId(int entityId) {
        for (ThievingData data : ThievingData.values()) {
            if (entityId == data.getEntityId()) {
                return data;
            }
        }
        return null;
    }

    /**
     * This method is used to generate chance of failure while thieving from an entity.
     *
     * @return failChance
     */
    private static int generateFailChance() {
        return 0;
    }

    /**
     * This method is used to determine whether to use a, an, or some depending on the received item's name.
     *
     * @return name
     */
    private static String aAnOrSome(String itemName) {
        if ((itemName.startsWith("a") || itemName.startsWith("e") || itemName.startsWith("i") || itemName.startsWith("o") || itemName.startsWith("u")) && !itemName.endsWith("s")) {
            return "an";
        } else if (itemName.endsWith("s")) {
            return "some";
        } else {
            return "a";
        }
    }


    /**
     * Attempts to steal from the entity.
     */
    public static void attemptSteal(final Client player, final int entityId, final Position position) {
        final ThievingData data = forId(entityId);

        final int failChance = generateFailChance();

        //final GameObjectDef definition = Misc.getObject(entityId, position.getX(), position.getY(), player.getPosition().getZ());
        if (data == null || player.chestEventOccur) {
            return;
        }
        int face = (position.getX() == 2658 && position.getY() == 3297) || (position.getX() == 2663 && position.getY() == 3296) ? 0 :
                (position.getX() == 2655 && position.getY() == 3311) || (position.getX() == 2656 && position.getY() == 3302) ? 1 :
                        (position.getX() == 2662 && position.getY() == 3314) || (position.getX() == 2657 && position.getY() == 3314) ? 2 :
                                (position.getX() == 2667 && position.getY() == 3303) || (position.getX() == 2667 && position.getY() == 3310) ? 3
                                        : -1;
        if (face == -1 && data.getThievingType() == ThievingType.STALL_THIEVING) {
            player.send(new SendMessage("Not added object!"));
            return;
        }
        final Object o = new Object(EMPTY_STALL_ID, position.getX(), position.getY(), position.getZ(), 10, face, data.getEntityId());
        if (player.getLevel(Skill.THIEVING) < data.getRequiredLevel()) {
            player.send(new SendMessage("You need a thieving level of " + data.getRequiredLevel() + " to steal from " + data.toString().toLowerCase().replace('_', ' ') + "s."));
            return;
        }
        if (System.currentTimeMillis() - player.lastAction < 2000) {
            return;
        }
        player.lastAction = System.currentTimeMillis();
        if (data.getThievingType() == ThievingType.PICKPOCKETING || data.getThievingType() == ThievingType.OTHER) {
            player.setFocus(position.getX(), position.getY());
            player.requestAnim(PICKPOCKET_EMOTE, 0);
            player.send(new SendMessage("You attempt to steal from the " + data.toString().toLowerCase().replace('_', ' ') + "..."));
        } else {
            if (GlobalObject.hasGlobalObject(o)) {
                return;
            }
            player.requestAnim(STALL_THIEVING_EMOTE, 0);
        }

//		if (ThievingData.RINGBELL != null) {
//			player.setFocus(position.getX(), position.getY());
//			player.requestAnim(PICKPOCKET_EMOTE, 0);
//			player.send(new SendMessage("You ring the bell to celebrate the season!"));
//		} else {
//			player.requestAnim(STALL_THIEVING_EMOTE, 0);
//		}

        EventManager.getInstance().registerEvent(new Event(600) {

            @Override
            public void execute() {
                if (player.disconnected) {
                    this.stop();
                    return;
                }

                if (failChance > 75) {
                    player.send(new SendMessage("You fail to thieve from the " + data.toString().toLowerCase().replace('_', ' ')));
                    this.stop();
                    return;
                }

                if (player.hasSpace()) {
                    player.giveExperience(data.getReceivedExperience(), Skill.THIEVING);
                    player.canPreformAction = false;

                    if (data.getItemId().length > 1) {
                        int rollChance = (int) (Math.random() * 100);

                        for (int i = 0; i < data.getItemId().length; i++) {
                            if (rollChance < data.getItemItemChance()[i]) {
                                player.addItem(data.getItemId()[i], data.getItemAmount()[i].getValue());
                                player.send(new SendMessage("You receive " + aAnOrSome(player.GetItemName(data.getItemId()[i])) + " " + player.GetItemName(data.getItemId()[i]).toLowerCase()));
                                break;
                            }
                        }

                    } else {
                        player.addItem(data.getItemId()[0], data.getItemAmount()[0].getValue());
                        player.send(new SendMessage("You receive " + aAnOrSome(player.GetItemName(data.getItemId()[0])) + " " + player.GetItemName(data.getItemId()[0]).toLowerCase()));
                    }
                    if (data.getThievingType() == ThievingType.STALL_THIEVING) {
                        final Object o = new Object(EMPTY_STALL_ID, position.getX(), position.getY(), position.getZ(), 10, face, data.getEntityId());
                        if (!GlobalObject.addGlobalObject(o, data.getRespawnTime() * 1000)) {
                            stop();
                        }
                    }
                    //player.send(new Sound(356));
                    player.triggerRandom(data.getReceivedExperience());
                    player.chestEvent++;
                    stop();

                } else {
                    player.send(new SendMessage("You don't have enough inventory space!"));
                    stop();
                }
            }
        });
    }
}