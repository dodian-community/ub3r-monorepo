package net.dodian.uber.game.model.player.skills.agility;

import net.dodian.uber.game.Server;
import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.npc.Npc;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.item.Equipment;
import net.dodian.uber.game.model.item.Ground;
import net.dodian.uber.game.netty.listener.out.SendMessage;
import net.dodian.uber.game.model.player.skills.Skill;
import net.dodian.utilities.Misc;

public class Werewolf {
    final Client c;
    public Werewolf(Client c) {
        this.c = c;
    }

    public void giveEndExperience(int xp, boolean ringCheck) {
        if(ringCheck && c.getEquipment()[Equipment.Slot.RING.getId()] == 4202) xp = (int)(xp * 1.1);
        c.giveExperience(xp, Skill.AGILITY);
        c.triggerRandom(xp);
    }

    public void StepStone(Position pos) {
        if (c.UsingAgility) {
            return;
        }
        if (c.getLevel(Skill.AGILITY) < 60) {
            c.send(new SendMessage("You need level 60 agility to use this!"));
            return;
        }
        if(pos.getX() == 3538 && pos.getY() == 9875 && c.getPosition().getX() == 3538 && c.getPosition().getY() == 9873) {
            c.UsingAgility = true;
            final Npc npc = Server.npcManager.getNpc(Server.npcManager.werewolfSpawn); //Start werewolf!
            //npc.requestAnim(65565, 0); //reset any type of animation!
            c.requestAnim(769, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            int x = 3533 + Misc.random(4);
            int y = 9910 + Misc.random(2);
            int offsetX = (x - npc.getPosition().getY());
            int offsetY = (y - npc.getPosition().getX());
            int distance = c.distanceToPoint(x, y);
            npc.requestAnim(6547, 0);
            npc.setText("Go fetch!");
            c.createProjectile(npc.getPosition().getY(), npc.getPosition().getX(), offsetY, offsetX, 50, 50 + (distance * 5), 338,
                    0, 35, 0, 51, 16, 64);
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY(), 0));
                    giveEndExperience(160, true);
                    Ground.addFloorItem(c, new Position(x, y, 0), 4179, 1, 100);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
        else if(pos.getX() == 3538 && pos.getY() == 9877 && c.getPosition().getX() == 3538 && c.getPosition().getY() == 9875) {
            c.UsingAgility = true;
            c.requestAnim(769, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY(), 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
        else if(pos.getX() == 3540 && pos.getY() == 9877 && c.getPosition().getX() == 3538 && c.getPosition().getY() == 9877) {
            c.UsingAgility = true;
            c.requestAnim(769, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY(), 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
        else if(pos.getX() == 3540 && pos.getY() == 9879 && c.getPosition().getX() == 3540 && c.getPosition().getY() == 9877) {
            c.UsingAgility = true;
            c.requestAnim(769, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY(), 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
        else if(pos.getX() == 3540 && pos.getY() == 9881 && c.getPosition().getX() == 3540 && c.getPosition().getY() == 9879) {
            c.UsingAgility = true;
            c.requestAnim(769, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY(), 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
    }

    public void hurdle(Position pos) {
        if (c.UsingAgility) {
            return;
        }
        if (c.getLevel(Skill.AGILITY) < 60) {
            c.send(new SendMessage("You need level 60 agility to use this!"));
            return;
        }
        if(pos.getX() >= 3537 && pos.getX() <= 3543 && pos.getY() == 9893 && c.getPosition().getX() >= 3537 && c.getPosition().getX() <= 3543 && c.getPosition().getY() == 9892) {
            c.UsingAgility = true;
            final Npc npc = Server.npcManager.getNpc(Server.npcManager.werewolfSpawn + 1); //shouting npc #1
            npc.setText("GO GO GO!");
            c.setFocus(pos.getX(), pos.getY());
            c.requestAnim(2750, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY() + 1, 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
        else if(pos.getX() >= 3537 && pos.getX() <= 3543 && pos.getY() == 9896 && c.getPosition().getX() >= 3537 && c.getPosition().getX() <= 3543 && c.getPosition().getY() == 9895) {
            c.UsingAgility = true;
            c.setFocus(pos.getX(), pos.getY());
            c.requestAnim(2750, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY() + 1, 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
        else if(pos.getX() >= 3537 && pos.getX() <= 3543 && pos.getY() == 9899 && c.getPosition().getX() >= 3537 && c.getPosition().getX() <= 3543 && c.getPosition().getY() == 9898) {
            c.UsingAgility = true;
            c.setFocus(pos.getX(), pos.getY());
            c.requestAnim(2750, 0);
            c.walkBlock = System.currentTimeMillis() + 600;
            EventManager.getInstance().registerEvent(new Event(600) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.transport(new Position(pos.getX(), pos.getY() + 1, 0));
                    giveEndExperience(160, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
    }

    public void pipe(Position pos) {
        if (c.UsingAgility) {
            return;
        }
        if (c.getLevel(Skill.AGILITY) < 60) {
            c.send(new SendMessage("You need level 60 agility to use this!"));
            return;
        }
        if((pos.getX() == 3538 || pos.getX() == 3541 || pos.getX() == 3544) && pos.getY() == 9905 && c.getPosition().getX() == pos.getX() && c.getPosition().getY() == pos.getY() - 1) {
            c.UsingAgility = true;
            final Npc npc = Server.npcManager.getNpc(Server.npcManager.werewolfSpawn + 2); //shouting npc #2
            npc.setText("You smell good!!");
            c.setWalkAnim(746);
            c.AddToWalkCords(0, 6, 3600);
            EventManager.getInstance().registerEvent(new Event(600) {
                int part = 0;

                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    part++;
                    if (part == 6) {
                        c.requestWeaponAnims();
                        c.requestAnim(748, 0);
                        giveEndExperience(600, true);
                        c.UsingAgility = false;
                        stop();
                    } else if (part == 1)
                        c.setWalkAnim(747);
                }
            });
        }
    }

    public void slope(Position pos) {
        if (c.UsingAgility) {
            return;
        }
        if (c.getLevel(Skill.AGILITY) < 60) {
            c.send(new SendMessage("You need level 60 agility to use this!"));
            return;
        }
        if(pos.getX() == 3532 && pos.getY() >= 9909 && pos.getY() <= 9911 && c.getPosition().getX() == pos.getX() + 1 && c.getPosition().getY() >= 9909 && c.getPosition().getY() <= 9911) {
            c.UsingAgility = true;
            final Npc npc = Server.npcManager.getNpc(Server.npcManager.werewolfSpawn + 3); //shouting npc #3
            npc.setText("You fetch good... Now bleed!");
            c.setWalkAnim(737);
            c.AddToWalkCords(-3, 0, 1800);
            EventManager.getInstance().registerEvent(new Event(1800) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.requestWeaponAnims();
                    giveEndExperience(400, true);
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
    }

    public void zipLine(Position pos) {
        if (c.UsingAgility) {
            return;
        }
        if (c.getLevel(Skill.AGILITY) < 60) {
            c.send(new SendMessage("You need level 60 agility to use this!"));
            return;
        }
        if(pos.getX() >= 3527 && pos.getX() <= 3529 && pos.getY() >= 9909 && pos.getY() <= 9911 && c.getPosition().getX() == 3528 && (c.getPosition().getY() == 9910 || c.getPosition().getY() == 9909)) {
            c.UsingAgility = true;
            c.setFocus(3528, 9908);
            final Npc npc = Server.npcManager.getNpc(Server.npcManager.werewolfSpawn + 4); //shouting npc #4
            final Npc lastNpc = Server.npcManager.getNpc(Server.npcManager.werewolfSpawn + 5); //shouting last npc
            npc.setText("Run adventurer.. RUN!!!");
            //TODO: Get the right animation!
            c.setRunAnim(904);
            int time = 22800 / 2; //38 distance!
            c.AddToRunCords(0, -38, time);
            EventManager.getInstance().registerEvent(new Event(time) {
                public void execute() {
                    if (c.disconnected) {
                        stop();
                        return;
                    }
                    c.requestWeaponAnims();
                    giveEndExperience(1500, true);
                    lastNpc.setText("Hand in that juicy stick to me!");
                    c.UsingAgility = false;
                    stop();
                }
            });
        }
    }

    public void handStick() {
        if(c.playerHasItem(4179)) {
            c.showNPCChat(5927, 616, new String[]{"Thank you for that juicy stick.", "Have some agility knowledge!"});
            c.deleteItem(4179, 1);
            c.checkItemUpdate();
            giveEndExperience(4000, false);
        } else c.showNPCChat(5927, 616, new String[]{"You do not have a stick to give me!"});
    }

}
