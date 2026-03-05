package net.dodian.uber.game.model.player.skills.agility;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import net.dodian.uber.game.event.GameEventScheduler;
import net.dodian.uber.game.model.Position;
import net.dodian.uber.game.model.entity.player.Client;
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces;
import net.dodian.uber.game.netty.listener.out.SendMessage;

public class DesertCarpet {
    final Client c;

    public DesertCarpet(Client c) {
        this.c = c;
    }

    private void runLater(int delayMs, Runnable action) {
        GameEventScheduler.runLaterMs(delayMs, action);
    }

    private void runRepeating(int delayMs, BooleanSupplier action) {
        GameEventScheduler.runRepeatingMs(delayMs, action);
    }

    private void startRide(int initialCount, Runnable beforeLoop, IntPredicate tickHandler) {
        runLater(600, () -> {
            beforeLoop.run();
            final int[] count = {initialCount};
            runRepeating(600, () -> {
                count[0]++;
                return tickHandler.test(count[0]);
            });
        });
    }

    public void sophanem(int pos) { //17
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getX() != 3286 && c.getPosition().getX() != 3287) {
            return;
        }
        int x = -1, y = -1;
        if (c.getPosition().getX() == 3287 && c.getPosition().getY() == 2813) {
            y = 0;
        }
        boolean extraStep = y == 0;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35));
        startRide(extraStep ? -1 : 0, () -> {
            if (extraStep) {
                c.AddToWalkCords(-1, 0, 600 + (600L * 35));
            }
        }, count -> {
            boolean countAbove = count > 0;
            if (countAbove && count < 20) {
                c.setWalkAnim(6936);
                c.setRunAnim(6936);
                c.requestAnim(6936, 0);
                c.AddToRunCords(pos == 2 ? -2 : pos == 1 ? 2 : 0, 2, 600L * (35 - count));
            } else if (count == 20) {
                c.showInterface(18460);
                c.requestAnim(6936, 0);
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0);
            }
            switch (pos) {
                case 0:
                    if (count == 23) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3349, 3003, 0));
                    }
                    if (count == 25) {
                        landed(c);
                        c.AddToWalkCords(0, -1, 600);
                        c.send(new SendMessage("Welcome to Pollnivneach."));
                        return false;
                    }
                    break;
                case 1:
                    if (count == 26) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3401, 2916, 0));
                    }
                    if (count == 28) {
                        landed(c);
                        c.AddToWalkCords(0, 1, 600);
                        c.send(new SendMessage("Welcome to Nardah."));
                        return false;
                    }
                    break;
                case 2:
                    if (count == 29) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3180, 3045, 0));
                    }
                    if (count == 31) {
                        landed(c);
                        c.AddToWalkCords(-1, 0, 600);
                        c.send(new SendMessage("Welcome to Bedabin Camp."));
                        return false;
                    }
                    break;
                default:
                    c.transport(new Position(3287, 2813, 0));
                    return false;
            }
            return true;
        });
    }

    public void pollnivneach(int pos) { //20
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getX() < 3347 && c.getPosition().getX() > 3349 && c.getPosition().getY() != 3002 && c.getPosition().getY() != 3003) {
            return;
        }
        int x = 0, y = 1;
        if ((c.getPosition().getX() == 3347 && c.getPosition().getY() == 3002) || (c.getPosition().getX() == 3348 && c.getPosition().getY() == 3003)) {
            y = 0;
            x = 1;
        }
        boolean extraStep = c.getPosition().getX() == 3347 && c.getPosition().getY() == 3002;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35));
        startRide(extraStep ? -1 : 0, () -> {
            if (extraStep) {
                c.AddToWalkCords(1, 1, 600 + (600L * 35));
            }
        }, count -> {
            boolean countAbove = count > 0;
            if (countAbove && count < 20) {
                c.setWalkAnim(6936);
                c.setRunAnim(6936);
                c.requestAnim(6936, 0);
                if (pos == 0) {
                    if (count - 1 <= 2) {
                        c.AddToRunCords(2, 2, 600L * (35 - count));
                    } else {
                        c.AddToRunCords(2, 0, 600L * (35 - count));
                    }
                } else if (pos == 1) {
                    c.AddToRunCords(-1, 1, 600L * (35 - count));
                } else if (pos == 2) {
                    if (count - 1 <= 1) {
                        c.AddToRunCords(2, 0, 600L * (35 - count));
                    } else if (count - 1 <= 14) {
                        c.AddToRunCords(0, -2, 600L * (35 - count));
                    } else if (count - 1 <= 16) {
                        c.AddToRunCords(2, 0, 600L * (35 - count));
                    } else if (count - 1 <= 18) {
                        c.AddToRunCords(0, -2, 600L * (35 - count));
                    } else {
                        c.AddToRunCords(2, 0, 600L * (35 - count));
                    }
                }
            } else if (count == 20) {
                c.showInterface(18460);
                c.requestAnim(6936, 0);
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0);
            }
            switch (pos) {
                case 0:
                    if (count == 23) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3401, 2916, 0));
                    }
                    if (count == 25) {
                        landed(c);
                        c.AddToWalkCords(0, 1, 600);
                        c.send(new SendMessage("Welcome to Nardah."));
                        return false;
                    }
                    break;
                case 1:
                    if (count == 26) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3180, 3045, 0));
                    }
                    if (count == 28) {
                        landed(c);
                        c.AddToWalkCords(-1, 0, 600);
                        c.send(new SendMessage("Welcome to Bedabin Camp."));
                        return false;
                    }
                    break;
                case 2:
                    if (count == 29) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3285, 2813, 0));
                    }
                    if (count == 31) {
                        landed(c);
                        c.AddToWalkCords(0, -1, 600);
                        c.send(new SendMessage("Welcome to Sophanem."));
                        return false;
                    }
                    break;
                default:
                    c.transport(new Position(3350, 3001, 0));
                    return false;
            }
            return true;
        });
    }

    public void nardah(int pos) { //22
        if (c.UsingAgility) {
            return;
        }
        if (c.getPosition().getY() != 2918 && c.getPosition().getX() != 3401) {
            return;
        }
        int x = 0, y = -1;
        if (c.getPosition().getX() == 3400 && c.getPosition().getY() == 2918) {
            x = 1;
        }
        boolean extraStep = x == 1;
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35));
        startRide(extraStep ? -1 : 0, () -> {
            if (extraStep) {
                c.AddToWalkCords(0, -1, 600 + (600L * 35));
            }
        }, count -> {
            boolean countAbove = count > 0;
            if (countAbove && count < 20) {
                c.setWalkAnim(6936);
                c.setRunAnim(6936);
                c.requestAnim(6936, 0);
                if (pos != 1 && count >= 18) {
                    c.AddToRunCords(-2, 0, 600L * (35 - count));
                } else {
                    c.AddToRunCords(-2, pos == 1 ? -2 : 2, 600L * (35 - count));
                }
            } else if (count == 20) {
                c.showInterface(18460);
                c.requestAnim(6936, 0);
            } else if (countAbove && count < 23) {
                c.requestAnim(6936, 0);
            }
            switch (pos) {
                case 0:
                    if (count == 23) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3349, 3003, 0));
                    }
                    if (count == 25) {
                        landed(c);
                        c.AddToWalkCords(0, -1, 600);
                        c.send(new SendMessage("Welcome to Pollnivneach."));
                        return false;
                    }
                    break;
                case 1:
                    if (count == 26) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3285, 2813, 0));
                    }
                    if (count == 28) {
                        landed(c);
                        c.AddToWalkCords(0, -1, 600);
                        c.send(new SendMessage("Welcome to Sophanem."));
                        return false;
                    }
                    break;
                case 2:
                    if (count == 29) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3180, 3045, 0));
                    }
                    if (count == 31) {
                        landed(c);
                        c.AddToWalkCords(-1, 0, 600);
                        c.send(new SendMessage("Welcome to Bedabin Camp."));
                        return false;
                    }
                    break;
                default:
                    c.transport(new Position(3400, 2918, 0));
                    return false;
            }
            return true;
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
        if (c.getPosition().getX() == 3181 && c.getPosition().getY() == 3044) {
            y = 1;
        }
        c.send(new RemoveInterfaces());
        c.faceNpc(-1);
        c.UsingAgility = true;
        c.clearTabs();
        c.AddToWalkCords(x, y, 600 + (600L * 35L));
        final int[] count = {0};
        runRepeating(600, () -> {
            count[0]++;
            boolean countAbove = count[0] > 0;
            if (countAbove && count[0] < 20) {
                c.setWalkAnim(6936);
                c.setRunAnim(6936);
                c.requestAnim(6936, 0);
                c.AddToRunCords(pos < 2 ? 2 : 0, -2, 600L * (35 - count[0]));
            } else if (count[0] == 20) {
                c.showInterface(18460);
                c.requestAnim(6936, 0);
            } else if (countAbove && count[0] < 23) {
                c.requestAnim(6936, 0);
            }
            switch (pos) {
                case 0:
                    if (count[0] == 23) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3349, 3003, 0));
                    }
                    if (count[0] == 25) {
                        landed(c);
                        c.AddToWalkCords(0, -1, 600);
                        c.send(new SendMessage("Welcome to Pollnivneach."));
                        return false;
                    }
                    break;
                case 1:
                    if (count[0] == 26) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3401, 2916, 0));
                    }
                    if (count[0] == 28) {
                        landed(c);
                        c.AddToWalkCords(0, 1, 600);
                        c.send(new SendMessage("Welcome to Nardah."));
                        return false;
                    }
                    break;
                case 2:
                    if (count[0] == 29) {
                        c.requestAnim(-1, 0);
                        c.showInterface(18452);
                        c.transport(new Position(3285, 2813, 0));
                    }
                    if (count[0] == 31) {
                        landed(c);
                        c.AddToWalkCords(0, -1, 600);
                        c.send(new SendMessage("Welcome to Sophanem."));
                        return false;
                    }
                    break;
                default:
                    c.transport(new Position(3182, 3044, 0));
                    return false;
            }
            return true;
        });
    }

    private void landed(Client c) {
        c.requestWeaponAnims();
        c.send(new RemoveInterfaces());
        c.UsingAgility = false;
        c.resetTabs();
    }
}
