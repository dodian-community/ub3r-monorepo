package net.dodian.uber.game.model.player.skills.agility;

import net.dodian.uber.game.event.Event;
import net.dodian.uber.game.event.EventManager;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.model.player.packets.outgoing.RemoveInterfaces;
import net.dodian.uber.game.model.player.packets.outgoing.SendMessage;

public class DesertCarpet {
    final Client c;

    public DesertCarpet(Client c) {
        this.c = c;
    }

    public void sophanem(int pos) { //17
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getX() != 3286 && c.getPosition().getX() != 3287) { //Two corners!
            return;
        }
        int x = -1, y = -1;
        if(c.getPosition().getX() == 3287 && c.getPosition().getY() == 2813)
            y = 0;
        boolean time = y == 0;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35));
        EventManager.getInstance().registerEvent(new Event(600) {
            public void execute() {
                if(time) c.AddToWalkCords(-1, 0, 600 + (600L * 35));
        EventManager.getInstance().registerEvent(new Event(600) {
            int count = time ? -1 : 0;
            public void execute() {
                count++;
                boolean countAbove = count > 0;
                if(countAbove && count < 20) { //Movement timer!
                    c.setWalkAnim(6936);
                    c.setRunAnim(6936);
                    c.requestAnim(6936, 0);
                    c.AddToRunCords(pos == 2 ? -2 : pos == 1 ? 2 : 0, 2, 600L * (35 - count));
                } else if(count == 20) { //19 ticks!
                    c.showInterface(18460);
                    c.requestAnim(6936, 0);
                } else if (countAbove && count < 23) c.requestAnim(6936, 0);
                switch(pos) {
                    case 0: //Pollnivneach
                        if(count == 23) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3349, 3003, 0));
                        }
                        if(count == 25) {
                            landed(c);
                            c.AddToWalkCords(0, -1, 600);
                            c.send(new SendMessage("Welcome to Pollnivneach."));
                            stop();
                        }
                    break;
                    case 1: //Nardah
                        if(count == 26) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3401, 2916, 0));
                        }
                        if(count == 28) {
                            landed(c);
                            c.AddToWalkCords(0, 1, 600);
                            c.send(new SendMessage("Welcome to Nardah."));
                            stop();
                        }
                        break;
                    case 2: //Bedabin Camp
                        if(count == 29) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3180, 3045, 0));

                        }
                        if(count == 31) {
                            landed(c);
                            c.AddToWalkCords(-1, 0, 600);
                            c.send(new SendMessage("Welcome to Bedabin Camp."));
                            stop();
                        }
                        break;
                    default:
                        c.transport(new Position(3287, 2813, 0)); //Incase it fails!
                        stop();
                }
            }
        });
                stop();
            }
        });
    }

    public void pollnivneach(int pos) { //20
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getX() < 3347 && c.getPosition().getX() > 3349 && c.getPosition().getY() != 3002 && c.getPosition().getY() != 3003) { //Two corners!
            return;
        }
        int x = 0, y = 1;
        if((c.getPosition().getX() == 3347 && c.getPosition().getY() == 3002) || (c.getPosition().getX() == 3348 && c.getPosition().getY() == 3003)) {
            y = 0;
            x = 1;
        }
        boolean time = c.getPosition().getX() == 3347 && c.getPosition().getY() == 3002;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35));
        EventManager.getInstance().registerEvent(new Event(600) {
            public void execute() {
                if(time) c.AddToWalkCords(1, 1, 600 + (600L * 35));
        EventManager.getInstance().registerEvent(new Event(600) {
            int count = time ? -1 : 0;
            public void execute() {
                count++;
                boolean countAbove = count > 0;
                if(countAbove && count < 20) { //Movement timer!
                    c.setWalkAnim(6936);
                    c.setRunAnim(6936);
                    c.requestAnim(6936, 0);
                    if(pos == 0) { //2 + 5 = 7
                        if(count - 1 <= 2)
                            c.AddToRunCords(2, 2, 600L * (35 - count));
                        else c.AddToRunCords(2, 0, 600L * (35 - count));
                    }
                    else if(pos == 1) {
                        c.AddToRunCords(-1, 1, 600L * (35 - count));
                    }
                    else if(pos == 2) {
                        if(count - 1 <= 1)
                            c.AddToRunCords(2, 0, 600L * (35 - count));
                        else if(count - 1 <= 14)
                            c.AddToRunCords(0, -2, 600L * (35 - count));
                        else if(count - 1 <= 16)
                            c.AddToRunCords(2, 0, 600L * (35 - count));
                        else if(count - 1 <= 18)
                            c.AddToRunCords(0, -2, 600L * (35 - count));
                        else c.AddToRunCords(2, 0, 600L * (35 - count));
                    }
                } else if(count == 20) { //19 ticks!
                    c.showInterface(18460);
                    c.requestAnim(6936, 0);
                } else if (countAbove && count < 23) c.requestAnim(6936, 0);
                switch(pos) {
                    case 0: //Nardah
                        if(count == 23) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3401, 2916, 0));
                        }
                        if(count == 25) {
                            landed(c);
                            c.AddToWalkCords(0, 1, 600);
                            c.send(new SendMessage("Welcome to Nardah."));
                            stop();
                        }
                        break;
                    case 1: //Bedabin Camp
                        if(count == 26) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3180, 3045, 0));
                        }
                        if(count == 28) {
                            landed(c);
                            c.AddToWalkCords(-1, 0, 600);
                            c.send(new SendMessage("Welcome to Bedabin Camp."));
                            stop();
                        }
                        break;
                    case 2: //Sophanem
                        if(count == 29) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3285, 2813, 0));

                        }
                        if(count == 31) {
                            landed(c);
                            c.AddToWalkCords(0, -1, 600);
                            c.send(new SendMessage("Welcome to Sophanem."));
                            stop();
                        }
                        break;
                    default:
                        c.transport(new Position(3350, 3001, 0));
                        stop();
                }
            }
        });
                stop();
            }
        });
    }

    public void nardah(int pos) { //22
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getY() != 2918 && c.getPosition().getX() != 3401) { //Two corners!
            return;
        }
        int x = 0, y = -1;
        if(c.getPosition().getX() == 3400 && c.getPosition().getY() == 2918)
            x = 1;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        boolean time = x == 1;
        c.AddToWalkCords(x, y, 600 + (600L * 35)); //(600L * 35)
        EventManager.getInstance().registerEvent(new Event(600) {
            public void execute() {
            if(time) c.AddToWalkCords(0, -1, 600 + (600L * 35));
        EventManager.getInstance().registerEvent(new Event(600) {
            int count = time ? -1 : 0;
            public void execute() {
                count++;
                boolean countAbove = count > 0;
                if(countAbove && count < 20) { //Movement timer!
                    c.setWalkAnim(6936);
                    c.setRunAnim(6936);
                    c.requestAnim(6936, 0);
                    if(pos != 1 && count >= 18)
                        c.AddToRunCords(-2, 0, 600L * (35 - count));
                    else c.AddToRunCords(-2, pos == 1 ? -2 : 2, 600L * (35 - count));
                } else if(count == 20) {
                    c.showInterface(18460);
                    c.requestAnim(6936, 0);
                } else if (countAbove && count < 23) c.requestAnim(6936, 0);
                switch(pos) {
                    case 0: //Pollnivneach
                        if(count == 23) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3349, 3003, 0));
                        }
                        if(count == 25) {
                            landed(c);
                            c.AddToWalkCords(0, -1, 600);
                            c.send(new SendMessage("Welcome to Pollnivneach."));
                            stop();
                        }
                        break;
                    case 1: //Sophanem
                        if(count == 26) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3285, 2813, 0));
                        }
                        if(count == 28) {
                            landed(c);
                            c.AddToWalkCords(0, -1, 600);
                            c.send(new SendMessage("Welcome to Sophanem."));
                            stop();
                        }
                        break;
                    case 2: //Bedabin Camp
                        if(count == 29) {
                            c.requestAnim(-1, 0);
                            c.showInterface(18452);
                            c.transport(new Position(3180, 3045, 0));

                        }
                        if(count == 31) {
                            landed(c);
                            c.AddToWalkCords(-1, 0, 600);
                            c.send(new SendMessage("Welcome to Bedabin Camp."));
                            stop();
                        }
                        break;
                    default:
                        c.transport(new Position(3400, 2918, 0)); //Incase it fails!
                        stop();
                    }
                }
            });
        stop();
        }
    });
    }

    public void bedabinCamp(int pos) { //19
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getY() != 3044 && c.getPosition().getY() != 3046) {
            return;
        }
        int x = -1, y = -1;
        if(c.getPosition().getX() == 3181 && c.getPosition().getY() == 3044)
            y = 1;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35L));
        EventManager.getInstance().registerEvent(new Event(600) {
            int count = 0;
            public void execute() {
                        count++;
                        boolean countAbove = count > 0;
                        if(countAbove && count < 20) { //Movement timer!
                            c.setWalkAnim(6936);
                            c.setRunAnim(6936);
                            c.requestAnim(6936, 0);
                            c.AddToRunCords(pos < 2 ? 2 : 0, -2, 600L * (35 - count));
                        } else if(count == 20) {
                            c.showInterface(18460);
                            c.requestAnim(6936, 0);
                        } else if (countAbove && count < 23) c.requestAnim(6936, 0);
                        switch(pos) {
                            case 0: //Pollnivneach
                                if(count == 23) {
                                    c.requestAnim(-1, 0);
                                    c.showInterface(18452);
                                    c.transport(new Position(3349, 3003, 0));
                                }
                                if(count == 25) {
                                    landed(c);
                                    c.AddToWalkCords(0, -1, 600);
                                    c.send(new SendMessage("Welcome to Pollnivneach."));
                                    stop();
                                }
                                break;
                            case 1: //Nardah
                                if(count == 26) {
                                    c.requestAnim(-1, 0);
                                    c.showInterface(18452);
                                    c.transport(new Position(3401, 2916, 0));
                                }
                                if(count == 28) {
                                    landed(c);
                                    c.AddToWalkCords(0, 1, 600);
                                    c.send(new SendMessage("Welcome to Nardah."));
                                    stop();
                                }
                                break;
                            case 2: //Sophanem
                                if(count == 29) {
                                    c.requestAnim(-1, 0);
                                    c.showInterface(18452);
                                    c.transport(new Position(3285, 2813, 0));

                                }
                                if(count == 31) {
                                    landed(c);
                                    c.AddToWalkCords(0, -1, 600);
                                    c.send(new SendMessage("Welcome to Sophanem."));
                                    stop();
                                }
                                break;
                            default:
                                c.transport(new Position(3182, 3044, 0)); //Incase it fails!
                                stop();
                        }
            }
        });
    }

    private void landed(Client c) {
        c.requestWeaponAnims();
        c.send(new RemoveInterfaces());
        c.UsingAgility = false;
        c.resetTabs();
    }

}