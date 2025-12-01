package com.runescape.cache.graphics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import com.runescape.Client;
import com.runescape.Configuration;
import com.runescape.cache.FileArchive;
import com.runescape.cache.anim.Frame;
import com.runescape.cache.def.ItemDefinition;
import com.runescape.cache.def.NpcDefinition;
import com.runescape.cache.graphics.interfaces.Dropdown;
import com.runescape.cache.graphics.interfaces.DropdownMenu;
import com.runescape.cache.graphics.interfaces.makeAll;
import com.runescape.collection.ReferenceCache;
import com.runescape.entity.model.Model;
import com.runescape.io.Buffer;
import com.runescape.util.StringUtils;

/**
 * Previously known as RSInterface, which is a class used to create and show
 * game interfaces.
 */
public class Widget {

    public static final int OPTION_OK = 1;
    public static final int OPTION_USABLE = 2;
    public static final int OPTION_CLOSE = 3;
    public static final int OPTION_TOGGLE_SETTING = 4;
    public static final int OPTION_RESET_SETTING = 5;
    public static final int OPTION_CONTINUE = 6;

    public static final int TYPE_CONTAINER = 0;
    public static final int TYPE_MODEL_LIST = 1;
    public static final int TYPE_INVENTORY = 2;
    public static final int TYPE_RECTANGLE = 3;
    public static final int TYPE_TEXT = 4;
    public static final int TYPE_SPRITE = 5;
    public static final int TYPE_MODEL = 6;
    public static final int TYPE_ITEM_LIST = 7;
    private static int parentchilds = 0;
    public String disabledMessage;
    public int itemId;

    public void swapInventoryItems(int i, int j) {
        int id = inventoryItemId[i];
        inventoryItemId[i] = inventoryItemId[j];
        inventoryItemId[j] = id;
        id = inventoryAmounts[i];
        inventoryAmounts[i] = inventoryAmounts[j];
        inventoryAmounts[j] = id;
    }

    public static void load(FileArchive interfaces, GameFont textDrawingAreas[], FileArchive graphics) {
        spriteCache = new ReferenceCache(50000);
        Buffer buffer = new Buffer(interfaces.readFile("data"));
        int defaultParentId = -1;
        buffer.readUShort();
        interfaceCache = new Widget[57000];

        while (buffer.currentPosition < buffer.payload.length) {
            int interfaceId = buffer.readUShort();
            if (interfaceId == 65535) {
                defaultParentId = buffer.readUShort();
                interfaceId = buffer.readUShort();
            }

            Widget widget = interfaceCache[interfaceId] = new Widget();
            widget.id = interfaceId;
            widget.parent = defaultParentId;
            widget.type = buffer.readUnsignedByte();
            widget.atActionType = buffer.readUnsignedByte();
            widget.contentType = buffer.readUShort();
            widget.width = buffer.readUShort();
            widget.height = buffer.readUShort();
            widget.opacity = (byte) buffer.readUnsignedByte();
            widget.hoverType = buffer.readUnsignedByte();
            if (widget.hoverType != 0)
                widget.hoverType = (widget.hoverType - 1 << 8) + buffer.readUnsignedByte();
            else
                widget.hoverType = -1;
            int operators = buffer.readUnsignedByte();
            if (operators > 0) {
                widget.valueCompareType = new int[operators];
                widget.requiredValues = new int[operators];
                for (int index = 0; index < operators; index++) {
                    widget.valueCompareType[index] = buffer.readUnsignedByte();
                    widget.requiredValues[index] = buffer.readUShort();
                }

            }
            int scripts = buffer.readUnsignedByte();
            if (scripts > 0) {
                widget.valueIndexArray = new int[scripts][];
                for (int script = 0; script < scripts; script++) {
                    int instructions = buffer.readUShort();
                    widget.valueIndexArray[script] = new int[instructions];
                    for (int instruction = 0; instruction < instructions; instruction++)
                        widget.valueIndexArray[script][instruction] = buffer.readUShort();

                }

            }
            if (widget.type == TYPE_CONTAINER) {
                widget.drawsTransparent = false;
                widget.scrollMax = buffer.readUShort();
                widget.invisible = buffer.readUnsignedByte() == 1;
                int length = buffer.readUShort();

                if(widget.id == 5608) {

                    widget.children = new int[PRAYER_INTERFACE_CHILDREN];
                    widget.childX = new int[PRAYER_INTERFACE_CHILDREN];
                    widget.childY = new int[PRAYER_INTERFACE_CHILDREN];

                    for (int index = 0; index < length; index++) {
                        widget.children[BEGIN_READING_PRAYER_INTERFACE+ index] = buffer.readUShort();
                        widget.childX[BEGIN_READING_PRAYER_INTERFACE+ index] = buffer.readShort();
                        widget.childY[BEGIN_READING_PRAYER_INTERFACE+ index] = buffer.readShort();
                    }

                } else {


                    widget.children = new int[length];
                    widget.childX = new int[length];
                    widget.childY = new int[length];

                    for (int index = 0; index < length; index++) {
                        widget.children[index] = buffer.readUShort();
                        widget.childX[index] = buffer.readShort();
                        widget.childY[index] = buffer.readShort();
                    }
                }
            }
            if (widget.type == TYPE_MODEL_LIST) {
                buffer.readUShort();
                buffer.readUnsignedByte();
            }
            if (widget.type == TYPE_INVENTORY) {
                widget.inventoryItemId = new int[widget.width * widget.height];
                widget.inventoryAmounts = new int[widget.width * widget.height];
                widget.allowSwapItems = buffer.readUnsignedByte() == 1;
                widget.hasActions = buffer.readUnsignedByte() == 1;
                widget.usableItems = buffer.readUnsignedByte() == 1;
                widget.replaceItems = buffer.readUnsignedByte() == 1;
                widget.spritePaddingX = buffer.readUnsignedByte();
                widget.spritePaddingY = buffer.readUnsignedByte();
                widget.spritesX = new int[20];
                widget.spritesY = new int[20];
                widget.sprites = new Sprite[20];
                for (int j2 = 0; j2 < 20; j2++) {
                    int k3 = buffer.readUnsignedByte();
                    if (k3 == 1) {
                        widget.spritesX[j2] = buffer.readShort();
                        widget.spritesY[j2] = buffer.readShort();
                        String s1 = buffer.readString();
                        if (graphics != null && s1.length() > 0) {
                            int i5 = s1.lastIndexOf(",");

                            int index = Integer.parseInt(s1.substring(i5 + 1));

                            String name = s1.substring(0, i5);

                            widget.sprites[j2] = getSprite(index, graphics, name);
                        }
                    }
                }
                widget.actions = new String[6];
                for (int actionIndex = 0; actionIndex < 5; actionIndex++) {
                    widget.actions[actionIndex] = buffer.readString();
                    if (widget.actions[actionIndex].length() == 0)
                        widget.actions[actionIndex] = null;
                    if (widget.parent == 1644)
                        widget.actions[2] = "Operate";
                    if (widget.parent == 3824) {
                        widget.actions[4] = "Buy X";
                    }
                    if (widget.parent == 3822) {
                        widget.actions[4] = "Sell X";
                    }
                }
            }
            if (widget.type == TYPE_RECTANGLE)
                widget.filled = buffer.readUnsignedByte() == 1;
            if (widget.type == TYPE_TEXT || widget.type == TYPE_MODEL_LIST) {
                widget.centerText = buffer.readUnsignedByte() == 1;
                int k2 = buffer.readUnsignedByte();
                if (textDrawingAreas != null)
                    widget.textDrawingAreas = textDrawingAreas[k2];
                widget.textShadow = buffer.readUnsignedByte() == 1;
            }

            if (widget.type == TYPE_TEXT) {
                widget.defaultText = buffer.readString().replaceAll("RuneScape", Configuration.CLIENT_NAME);
                widget.secondaryText = buffer.readString();
            }

            if (widget.type == TYPE_MODEL_LIST || widget.type == TYPE_RECTANGLE || widget.type == TYPE_TEXT)
                widget.textColor = buffer.readInt();
            if (widget.type == TYPE_RECTANGLE || widget.type == TYPE_TEXT) {
                widget.secondaryColor = buffer.readInt();
                widget.defaultHoverColor = buffer.readInt();
                widget.secondaryHoverColor = buffer.readInt();
            }
            if (widget.type == TYPE_SPRITE) {
                widget.drawsTransparent = false;
                String name = buffer.readString();
                if (graphics != null && name.length() > 0) {
                    int index = name.lastIndexOf(",");
                    widget.disabledSprite = getSprite(Integer.parseInt(name.substring(index + 1)), graphics,
                            name.substring(0, index));
                }
                name = buffer.readString();
                if (graphics != null && name.length() > 0) {
                    int index = name.lastIndexOf(",");
                    widget.enabledSprite = getSprite(Integer.parseInt(name.substring(index + 1)), graphics,
                            name.substring(0, index));
                }
            }
            if (widget.type == TYPE_MODEL) {
                int content = buffer.readUnsignedByte();
                if (content != 0) {
                    widget.defaultMediaType = 1;
                    widget.defaultMedia = (content - 1 << 8) + buffer.readUnsignedByte();
                }
                content = buffer.readUnsignedByte();
                if (content != 0) {
                    widget.anInt255 = 1;
                    widget.anInt256 = (content - 1 << 8) + buffer.readUnsignedByte();
                }
                content = buffer.readUnsignedByte();
                if (content != 0)
                    widget.defaultAnimationId = (content - 1 << 8) + buffer.readUnsignedByte();
                else
                    widget.defaultAnimationId = -1;
                content = buffer.readUnsignedByte();
                if (content != 0)
                    widget.secondaryAnimationId = (content - 1 << 8) + buffer.readUnsignedByte();
                else
                    widget.secondaryAnimationId = -1;
                widget.modelZoom = buffer.readUShort();
                widget.modelRotation1 = buffer.readUShort();
                widget.modelRotation2 = buffer.readUShort();
            }
            if (widget.type == TYPE_ITEM_LIST) {
                widget.inventoryItemId = new int[widget.width * widget.height];
                widget.inventoryAmounts = new int[widget.width * widget.height];
                widget.centerText = buffer.readUnsignedByte() == 1;
                int l2 = buffer.readUnsignedByte();
                if (textDrawingAreas != null)
                    widget.textDrawingAreas = textDrawingAreas[l2];
                widget.textShadow = buffer.readUnsignedByte() == 1;
                widget.textColor = buffer.readInt();
                widget.spritePaddingX = buffer.readShort();
                widget.spritePaddingY = buffer.readShort();
                widget.hasActions = buffer.readUnsignedByte() == 1;
                widget.actions = new String[5];
                for (int actionCount = 0; actionCount < 5; actionCount++) {
                    widget.actions[actionCount] = buffer.readString();
                    if (widget.actions[actionCount].length() == 0)
                        widget.actions[actionCount] = null;
                }

            }
            if (widget.atActionType == OPTION_USABLE || widget.type == TYPE_INVENTORY) {
                widget.selectedActionName = buffer.readString();
                widget.spellName = buffer.readString();
                widget.spellUsableOn = buffer.readUShort();
            }

            if (widget.type == 8) {
                widget.defaultText = buffer.readString();
            }

            if (widget.atActionType == OPTION_OK || widget.atActionType == OPTION_TOGGLE_SETTING
                    || widget.atActionType == OPTION_RESET_SETTING || widget.atActionType == OPTION_CONTINUE) {
                widget.tooltip = buffer.readString();
                if (widget.tooltip.length() == 0) {
                    // TODO
                    if (widget.atActionType == OPTION_OK)
                        widget.tooltip = "Ok";
                    if (widget.atActionType == OPTION_TOGGLE_SETTING)
                        widget.tooltip = "Select";
                    if (widget.atActionType == OPTION_RESET_SETTING)
                        widget.tooltip = "Select";
                    if (widget.atActionType == OPTION_CONTINUE)
                        widget.tooltip = "Lol Continue";
                }
            }
        }
        interfaceLoader = interfaces;
        clanChatTab(textDrawingAreas);
        clanChatSettings(textDrawingAreas);
        configureLunar(textDrawingAreas);
        quickPrayers(textDrawingAreas);
        equipmentScreen(textDrawingAreas);
        geSearch(textDrawingAreas);
        makeAll.makeAll(textDrawingAreas);
        skillGuide(textDrawingAreas);
        //spinTab(textDrawingAreas);
        PlayerPanel(textDrawingAreas);
        geInterfaceMain(textDrawingAreas);
        skillsTab(textDrawingAreas);
        lobby(textDrawingAreas);
        equipmentTab(textDrawingAreas);
        itemsKeptOnDeath(textDrawingAreas);
        chatboxOptions2(textDrawingAreas);
        //bounty(textDrawingAreas);
        shop();
        prayerBook();
        priceChecker(textDrawingAreas);

        bankInterface(textDrawingAreas);
        bankSettings(textDrawingAreas);

        //teleportInterface(textDrawingAreas);

        //mainTeleports();
        settingsTab();

        pvpTab(textDrawingAreas);
        spawnTab(textDrawingAreas);
        presets(textDrawingAreas);

        basicSettings(textDrawingAreas);
        fKeys(textDrawingAreas);
        levelUpInterfaces();

        spriteCache = null;
    }

    //Keys by Oak
    public static void fKeys(GameFont[] tda) {
        Widget w = addTabInterface(53000);

        addSprite(53001, 430); // Bg

        addButton(53002, 52000, 150, 43, 448, 448, 53003, "Restore Defaults");
        addHoveredButton_sprite_loader(53003, 447, 150, 43, 53004);

        addText(53005, "Restore Defaults", tda, 2, 0xff981f, false, true);
        addText(53006, "Esc. closes current interface:", tda, 1, 0xFFFFFF, false, true);
        addButton(53007, 53000, 14, 15, 1, 817, 334, 333, -1, "Toggle"); //Toggle



        w.totalChildren(92); // 64

        final int tab_bg_start = 53020;
        final int tab_sprites_start = tab_bg_start + 14;
        final int f_keys_bg_start = tab_sprites_start + 14;
        final int dropDown_bg_start = f_keys_bg_start + 50;
        final int fkeys_Start = dropDown_bg_start + 14;
        for(int tab = 0, button = f_keys_bg_start; tab < 14; tab++, button+=3) {
            addSprite(tab_bg_start + tab, 445);
            addSprite(tab_sprites_start + tab, 431+tab);

            addButton(button, 52000, 35, 35, 450, 450, button + 1, "");
            addHoveredButton_sprite_loader(button + 1, 451, 35, 35, button + 2);
            interfaceCache[button].actions = new String[]{"@or1@F1", "@or1@F2", "@or1@F3", "@or1@F4", "@or1@F5", "@or1@F6", "@or1@F7", "@or1@F8", "@or1@F9", "@or1@F10", "@or1@F11", "@or1@F12", "@or1@ESC"};

            addSprite(dropDown_bg_start + tab, 449);

            addText(fkeys_Start + tab, "F"+tab, tda, 1, 0xff981f, false, true);
        }

        int child = 0;
        w.child(child++, 53001, 7, 4); //Bg
        w.child(child++, 38117, 482, 11); //Close button
        w.child(child++, 38118, 482, 11); //Close button hover
        w.child(child++, 53002, 351, 277); //Defaults
        w.child(child++, 53003, 351, 277); //Defaults hover
        w.child(child++, 53005, 368, 290); //Defaults text
        w.child(child++, 53006, 65, 295); //Esc. closes interface text
        w.child(child++, 53007, 242, 296); //Esc. closes interface toggle

        for(int buttonIndex = f_keys_bg_start, xPos = 70, yPos = 55, tab = 0; tab < 14; tab++) {
            w.child(child++, tab_bg_start+tab, xPos, yPos); //The tab icons' bg

            //ICONS
            int iconX = xPos;
            int iconY = yPos;
            //Cheaphax icons sprites pos
            if(tab == 2) { //Spawn tab
                iconX += 5;
                iconY += 4;
            } else if(tab == 3) { //Inventory
                iconY -= 2;
            } else if(tab == 4) { //Equipment
                iconX += 2;
            } else if(tab == 6) {
                iconY -= 1;
            } else if(tab == 9) {
                iconX += 2;
            } else if(tab == 11) {
                iconX -= 2;
                iconY -= 2;
            } else if(tab == 13) {
                iconX += 4;
                iconY += 4;
            }
            w.child(child++, tab_sprites_start+tab, iconX, iconY); //The tab icons

            //F KEYS BG

            w.child(child++, buttonIndex, xPos + 40, yPos);
            w.child(child++, buttonIndex + 1, xPos + 40, yPos);
            buttonIndex += 3;

            //Drop down button sprite
            w.child(child++, dropDown_bg_start + tab, xPos + 77, yPos + 10);

            //Text
            w.child(child++, fkeys_Start + tab, xPos + 48, yPos + 9);

            yPos += 44;
            //New row?
            if(tab == 4 || tab == 9) {
                xPos += 160;
                yPos = 55;
            }
        }
    }

    public static void addSelection(int id, GameFont[] tda, Widget parent, int x, int y, int menuId) {
        Widget w = addTabInterface(id);
        //w.totalChildren(4);
        addButton1(id + 1, 41002, 161, 32, 507, 507, id + 2, "Select", menuId, true);
        addHoveredButton_sprite_loader2(id + 2, 537, 161, 32, 56295);
        addText(id + 3, "Item name", tda, 1, 00000, true, false);
        //int child = 0;
        parent.child(parentchilds++, id + 1, x, y);
        parent.child(parentchilds++, id + 2, x, y);
        parent.child(parentchilds++, id + 3, x + 100, y + 8);
    }

    //Skill tab by Oak
    public static void skillTab(GameFont[] tda) {
        Widget w = addTabInterface(24000);
        w.totalChildren(47);
        addButton(52001, 52000, 188, 26, 412, 412, 52002, "@gre@Set Hitpoints Level");
        addHoveredButton_sprite_loader(52002, 413, 188, 26, 52003);

        addButton(52004, 52000, 188, 26, 414, 414, 52005, "@gre@Set Attack Level");
        addHoveredButton_sprite_loader(52005, 415, 188, 26, 52006);

        addButton(52007, 52000, 188, 26, 416, 416, 52008, "@gre@Set Strength Level");
        addHoveredButton_sprite_loader(52008, 417, 188, 26, 52009);

        addButton(52010, 52000, 188, 26, 418, 418, 52011, "@gre@Set Defence Level");
        addHoveredButton_sprite_loader(52011, 419, 188, 26, 52012);

        addButton(52013, 52000, 188, 26, 420, 420, 52014, "@gre@Set Ranged Level");
        addHoveredButton_sprite_loader(52014, 421, 188, 26, 52015);

        addButton(52016, 52000, 188, 26, 422, 422, 52017, "@gre@Set Magic Level");
        addHoveredButton_sprite_loader(52017, 423, 188, 26, 52018);

        addButton(52019, 52000, 188, 26, 424, 424, 52020, "@gre@Set Prayer Level");
        addHoveredButton_sprite_loader(52020, 425, 188, 26, 52021);

        addText(52022, "@or1@Hitpoints", tda, 2, 00000, false, true);
        addText(52023, "@or1@Attack", tda, 2, 00000, false, true);
        addText(52052, "@or1@Strength", tda, 2, 00000, false, true);
        addText(52025, "@or1@Defence", tda, 2, 00000, false, true);
        addText(52026, "@or1@Ranged", tda, 2, 00000, false, true);
        addText(52027, "@or1@Magic", tda, 2, 00000, false, true);
        addText(52028, "@or1@Prayer", tda, 2, 00000, false, true);

        addText(52029, "@or1@Killstreak: 0", tda, 1, 00000, false, true);
        addText(52030, "@or1@Kills: ", tda, 1, 00000, false, true);
        addText(52031, "@or1@Deaths: ", tda, 1, 00000, false, true);
        addText(52032, "@or1@Points: ", tda, 1, 00000, false, true);
        addText(52033, "@or1@K/D Ratio: ", tda, 1, 00000, false, true);
        addText(52034, "@or1@Donated: ", tda, 1, 00000, false, true);

        addSprite(52040, 426);
        addSprite(52041, 427);
        addSprite(52042, 162);
        addSprite(52043, 428);
        addSprite(52044, 429);
        addSprite(52045, 455);

        int child = 0;
        w.child(child++, 52001, 0, 0);
        w.child(child++, 52002, 0, 0);

        w.child(child++, 52004, 0, 27);
        w.child(child++, 52005, 0, 27);

        w.child(child++, 52007, 0, 54);
        w.child(child++, 52008, 0, 54);

        w.child(child++, 52010, 0, 81);
        w.child(child++, 52011, 0, 81);

        w.child(child++, 52013, 0, 108);
        w.child(child++, 52014, 0, 108);

        w.child(child++, 52016, 0, 135);
        w.child(child++, 52017, 0, 135);

        w.child(child++, 52019, 0, 162);
        w.child(child++, 52020, 0, 162);

        //Change font color and font sizes
        for(int i = 4004; i <= 4017; i++) {
            interfaceCache[i].textDrawingAreas = tda[2];
            interfaceCache[i].textColor = 0xFFFF00;
        }

        //Hitpoints
        w.child(child++, 4016, 134, 6);
        w.child(child++, 4017, 135 + 32, 6);

        //Attack
        w.child(child++, 4004, 134, 33);
        w.child(child++, 4005, 135 + 32, 33);

        //Strength
        w.child(child++, 4006, 134, 60);
        w.child(child++, 4007, 135 + 32, 60);

        //Defence
        w.child(child++, 4008, 134, 87);
        w.child(child++, 4009, 135 + 32, 87);

        //Ranged
        w.child(child++, 4010, 134, 114);
        w.child(child++, 4011, 135 + 32, 114);

        //Magic
        w.child(child++, 4014, 134, 141);
        w.child(child++, 4015, 135 + 32, 141);

        //Prayer
        w.child(child++, 4012, 134, 168);
        w.child(child++, 4013, 135 + 32, 168);

        //Skill names
        w.child(child++, 52022, 48, 6);
        w.child(child++, 52023, 48, 33);
        w.child(child++, 52052, 48, 60);
        w.child(child++, 52025, 48, 87);
        w.child(child++, 52026, 48, 114);
        w.child(child++, 52027, 48, 141);
        w.child(child++, 52028, 48, 168);


        w.child(child++, 52030, 16, 193);
        w.child(child++, 52029, 108, 193);
        w.child(child++, 52031, 16, 211);
        w.child(child++, 52032, 16, 229);
        w.child(child++, 52033, 108, 211);
        w.child(child++, 52034, 108, 229);

        w.child(child++, 52041, 0, 193);
        w.child(child++, 52040, 90, 193);
        w.child(child++, 52042, 0, 210);
        w.child(child++, 52043, 0, 228);
        w.child(child++, 52044, 90, 211);
        w.child(child++, 52045, 90, 228);
    }

    //Basic settings by Professor Oak
    public static void basicSettings(GameFont[] tda) {
        Widget w = addTabInterface(23000);
        w.totalChildren(19);

        addText(23001, "Settings Tab", tda, 2, 0xFFFFFF, true, true);

        addText(23002, "Always Left Click Attack", tda, 1, 0xe4a146, false, true); //Option
        addButton(23003, 23000, 14, 15, 1, 809, 334, 333, -1, "Toggle"); //Toggle

        addText(23004, "Combat Overlay Box", tda, 1, 0xe4a146, false, true); //Option
        addButton(23005, 23000, 14, 15, 1, 810, 334, 333, -1, "Toggle"); //Toggle

        addText(23006, "New Hitmarks", tda, 1, 0xe4a146, false, true); //Option
        addButton(23007, 23000, 14, 15, 1, 811, 334, 333, -1, "Toggle"); //Toggle

        //	addText(23008, "New HP Bars", tda, 1, 0xe4a146, false, true); //Option
        //	addButton(23009, 23000, 14, 15, 1, 812, 334, 333, -1, "Toggle"); //Toggle

        addText(23010, "Tweening", tda, 1, 0xe4a146, false, true); //Option
        addButton(23011, 23000, 14, 15, 1, 813, 334, 333, -1, "Toggle"); //Toggle

        addText(23012, "Roofs", tda, 1, 0xe4a146, false, true); //Option
        addButton(23013, 23000, 14, 15, 1, 814, 334, 333, -1, "Toggle"); //Toggle

        addText(23014, "Fog", tda, 1, 0xe4a146, false, true); //Option
        addButton(23015, 23000, 14, 15, 1, 815, 334, 333, -1, "Toggle"); //Toggle

        addHoverButton_sprite_loader(23020, 135, 190, 24, "Confirm", -1, 23021, 1);
        addHoveredButton_sprite_loader(23021, 136, 190, 24, 23022);

        addText(23023, "Old magic Interface", tda, 1, 0xe4a146, false, true); //Option
        addButton(23024, 23000, 14, 15, 1, 816, 334, 333, -1, "Toggle"); //Toggle

        addText(23018, "Key Bindings:", tda, 1, 0xe4a146, false, true); //Option
        addButton(23019, 23000, 32, 32, 190, 190, -1, "Key Bindings");

        int child = 0;

        w.child(child++, 23001, 95, 1);

        w.child(child++, 23002, 7, 20);
        w.child(child++, 23003, 160, 20);

        w.child(child++, 23023, 7, 45);
        w.child(child++, 23024, 160, 45);

        w.child(child++, 23004, 7, 70);
        w.child(child++, 23005, 160, 70);

        w.child(child++, 23006, 7, 95);
        w.child(child++, 23007, 160, 95);

        //w.child(child++, 23008, 7, 120);
        //	w.child(child++, 23009, 160, 120);

        w.child(child++, 23010, 7, 120);
        w.child(child++, 23011, 160, 120);

        w.child(child++, 23012, 7, 145);
        w.child(child++, 23013, 160, 145);

        w.child(child++, 23014, 7, 170);
        w.child(child++, 23015, 160, 170);

        w.child(child++, 23018, 7, 195);
        w.child(child++, 23019, 92, 188);

        w.child(child++, 23020, 2, 230);
        w.child(child++, 23021, 2, 230);

    }

    //By Professor Oak
    public static void presets(GameFont[] tda) {
        Widget w = addTabInterface(45000);
        w.totalChildren(90);

        //Add background sprite
        addSprite(45001, 355);

        //Add title
        addText(45002, "Presets", tda, 2, 0xff981f, true, false);

        //Add categories
        addText(45003, "@or1@Spellbook", tda, 0, 00000, false, false);
        addText(45004, "@or1@Inventory", tda, 0, 00000, false, false);
        addText(45005, "@or1@Equipment", tda, 0, 00000, false, false);
        addText(45006, "@or1@Stats", tda, 0, 00000, false, false);

        //Add stats strings
        for(int i = 0; i <= 6; i++) {
            addText(45007 + i, "", tda, 2, 00000, false, false);
        }

        //Add spellbook string
        addText(45014, "", tda, 1, 00000, true, false);

        //Add inventory
        for(int i = 0; i < 28; i++) {
            addItemOnInterface(45015 + i, 45000, new String[]{});
        }

        //Add equipment
        for(int i = 0; i < 14; i++) {
            addItemOnInterface(45044 + i, 45000, new String[]{});
        }

        //Open presets on death text
        addText(45059, "@or1@Open on death: ", tda, 1, 00000, false, false);

        //Open presets on death config tick
        addButton(45060, 45000, 14, 15, 1, 987, 332, 334, -1, "Toggle");

        //Set preset button
        addButton(45061, 45000, 146, 26, 357, 357, 45062, "Select");
        addHoveredButton_sprite_loader(45062, 358, 146, 26, 45063);

        //Load preset button
        addButton(45064, 45000, 146, 26, 357, 357, 45065, "Select");
        addHoveredButton_sprite_loader(45065, 358, 146, 26, 45066);

        //Preset buttons text
        addText(45067, "@yel@Edit this preset", tda, 2, 00000, false, false);
        addText(45068, "@yel@Load this preset", tda, 2, 00000, false, false);

        //Global Presets
        Widget list = addTabInterface(45069);
        list.totalChildren(10);
        for (int i = 45070, child = 0, yPos = 3; i < 45080; i++, yPos += 20) {
            addHoverText(i, "Empty", null, tda, 1, 0xff8000, false, true, 240, 0xFFFFFF);
            interfaceCache[i].actions = new String[]{"Select"};
            list.children[child] = i;
            list.childX[child] = 5;
            list.childY[child] = yPos;
            child++;
        }
        list.height = 98;
        list.width = 85;
        list.scrollMax = 210;

        //Global presets title
        addText(45080, "@whi@Global Presets", tda, 0, 00000, false, false);

        //Custom Presets
        list = addTabInterface(45081);
        list.totalChildren(10);
        for (int i = 45082, child = 0, yPos = 3; i < 45092; i++, yPos += 20) {
            addHoverText(i, "Empty", null, tda, 1, 0xff8000, false, true, 240, 0xFFFFFF);
            interfaceCache[i].actions = new String[]{"Select"};
            list.children[child] = i;
            list.childX[child] = 5;
            list.childY[child] = yPos;
            child++;
        }
        list.height = 107;
        list.width = 85;
        list.scrollMax = 210;

        //Custom presets title
        addText(45092, "@whi@Your Presets", tda, 0, 00000, false, false);

        //Children
        int child = 0;
        w.child(child++, 45001, 7, 2); //Background sprite
        w.child(child++, 38117, 482, 5); //Close button
        w.child(child++, 38118, 482, 5); //Close button hover
        w.child(child++, 45002, 253, 5); //Title
        w.child(child++, 45003, 42, 26); //Category 1 - spellbook
        w.child(child++, 45004, 180, 26); //Category 1 - inventory
        w.child(child++, 45005, 333, 26); //Category 1 - equipment
        w.child(child++, 45006, 453, 26); //Category 1 - stats

        //Stats
        for(int i = 0, yPos = 55; i <= 6; i++, yPos += 40) {
            w.child(child++, 45007 + i, 469, yPos);
        }

        //Spellbook
        w.child(child++, 45014, 65, 46); //Spellbook

        //Inventory
        for(int i = 0, xPos = 130, yPos = 48; i < 28; i++, xPos += 39) {
            w.child(child++, 45015 + i, xPos, yPos);
            if(xPos >= 247) {
                xPos = 91;
                yPos += 39;
            }
        }

        //Equipment bg sprites
        w.child(child++, 1645, 337, 149 - 52 - 17);
        w.child(child++, 1646, 337, 163- 17);
        w.child(child++, 1647, 337, 114);
        w.child(child++, 1648, 337, 58 + 146- 17);
        w.child(child++, 1649, 282, 110 - 44 + 118 - 13 + 5- 17);
        w.child(child++, 1650, 260 + 22, 58 + 154- 17);
        w.child(child++, 1651, 260 + 134, 58 + 118- 17);
        w.child(child++, 1652, 260 + 134, 58 + 154- 17);
        w.child(child++, 1653, 260 + 48, 58 + 81- 17);
        w.child(child++, 1654, 260 + 107, 58 + 81- 17);
        w.child(child++, 1655, 260 + 58, 58 + 42- 17);
        w.child(child++, 1656, 260 + 112, 58 + 41- 17);
        w.child(child++, 1657, 260 + 78, 58 + 4- 17);
        w.child(child++, 1658, 260 + 37, 58 + 43- 17);
        w.child(child++, 1659, 260 + 78, 58 + 43- 17);
        w.child(child++, 1660, 260 + 119, 58 + 43- 17);
        w.child(child++, 1661, 260 + 22, 58 + 82- 17);
        w.child(child++, 1662, 260 + 78, 58 + 82- 17);
        w.child(child++, 1663, 260 + 134, 58 + 82- 17);
        w.child(child++, 1664, 260 + 78, 58 + 122- 17);
        w.child(child++, 1665, 260 + 78, 58 + 162- 17);
        w.child(child++, 1666, 260 + 22, 58 + 162- 17);
        w.child(child++, 1667, 260 + 134, 58 + 162- 17);

        //Equipment
        w.child(child++, 45044, 341, 47); //Head slot
        w.child(child++, 45045, 300, 86); //Cape slot
        w.child(child++, 45046, 341, 86); //Amulet slot
        w.child(child++, 45047, 285, 125); //Weapon slot
        w.child(child++, 45048, 341, 125); //Body slot
        w.child(child++, 45049, 396, 125); //Shield slot
        w.child(child++, 45051, 341, 165); //Legs slot

        w.child(child++, 45053, 285, 205); //Hands slot
        w.child(child++, 45054, 341, 205); //Feet slot
        w.child(child++, 45056, 397, 205); //Ring slot
        w.child(child++, 45057, 381, 86); //Ammo slot


        //Open preset interface on death
        w.child(child++, 45059, 300, 243); //Open presets on death text
        w.child(child++, 45060, 400, 243); //Open presets on death tick config

        //Buttons
        w.child(child++, 45061, 285, 263); //Button 1 - Save This Preset
        w.child(child++, 45062, 285, 263); //Button 1 hover -  Save This Preset

        w.child(child++, 45064, 285, 294); //Button 2 - Load This Preset
        w.child(child++, 45065, 285, 294); //Button 2 hover - Load This Preset

        //Button text
        w.child(child++, 45067, 306, 267); //Save this preset text
        w.child(child++, 45068, 306, 299); //Load this preset text

        //Preset lists
        w.child(child++, 45069, 12, 90); //Global presets list
        w.child(child++, 45080, 24, 75); //Global presets list text title

        w.child(child++, 45081, 12, 214); //Custom presets list
        w.child(child++, 45092, 28, 200); //Custom presets list text title
    }

    //By Professor Oak
    public static void pvpTab(GameFont[] tda) {

        /* Pker feed */
        Widget pkerFeed = addTabInterface(32001);
        pkerFeed.totalChildren(10);
        int child = 0;
        for (int i = 32002, yPos = 3; i < 32012; i++, yPos += 20) {
            addText(i, "", tda, 1, 0xff9040, false, true);
            pkerFeed.children[child] = i;
            pkerFeed.childX[child] = 5;
            pkerFeed.childY[child] = yPos;
            child++;
        }
        pkerFeed.height = 90;
        pkerFeed.width = 174;
        pkerFeed.scrollMax = 230;

        /* Server feed */
        Widget serverFeed = addTabInterface(32013);
        serverFeed.totalChildren(10);
        child = 0;
        for (int i = 32014, yPos = 3; i < 32024; i++, yPos += 20) {
            addText(i, "", tda, 0, 0xff9040, false, true);
            serverFeed.children[child] = i;
            serverFeed.childX[child] = 5;
            serverFeed.childY[child] = yPos;
            child++;
        }
        serverFeed.height = 90;
        serverFeed.width = 174;
        serverFeed.scrollMax = 230;

        //The tab
        Widget tab = addTabInterface(32000);
        tab.totalChildren(6);

        addSpriteLoader(32027, 351);
        addSpriteLoader(32028, 351);

        addText(32025, "@or1@Toplist - Best Online Pkers", tda, 2, 0xFFFFFF, false, true);
        addText(32026, "@or1@Feed", tda, 2, 0xFFFFFF, false, true);

        child = 0;

        tab.child(child++, 32027, 1, 30);
        tab.child(child++, 32028, 1, 163);

        tab.child(child++, 32001, 1, 32);
        tab.child(child++, 32025, 5, 12);
        tab.child(child++, 32013, 1, 165);
        tab.child(child++, 32026, 5, 146);
    }

    public static void spawnTab(GameFont[] tda) {
        Widget tab = addTabInterface(31000);


        addText(31002, "Option here", tda, 0, 0xFFFFFF, true, true);

        addText(31003, "Item", tda, 1, 0xff8000, false, true);


        addHoverButton_sprite_loader(31004, 330, 172, 20, "Search", -1, 31005, 1);
        addHoveredButton_sprite_loader(31005, 331, 172, 20, 31006);

        //Inventory spawn
        addText(31010, "Inventory:", tda, 0, 0xFFFFFF, false, true);
        addHoverButton_sprite_loader(31007, 332, 14, 15, "Select", -1, 31008, 1);
        addHoveredButton_sprite_loader(31008, 333, 14, 15, 31009);

        //Bank spawn
        addText(31014, "Bank:", tda, 0, 0xFFFFFF, false, true);
        addHoverButton_sprite_loader(31011, 332, 14, 15, "Select", -1, 31012, 1);
        addHoveredButton_sprite_loader(31012, 333, 14, 15, 31013);


        addHoverButton_sprite_loader(31015, 353, 79, 30, "Presets", -1, 31016, 1);
        addHoveredButton_sprite_loader(31016, 354, 79, 30, 31017);

        addSpriteLoader(31001, 196);
        tab.totalChildren(14);

        tab.child(0, 31001, 0, 89);
        tab.child(1, 31030, 0, 91);
        tab.child(2, 31002, 95, 1);
        tab.child(3, 31004, 10, 25);
        tab.child(4, 31005, 10, 25);
        tab.child(5, 31003, 15, 28);
        tab.child(6, 31007, 75, 50);
        tab.child(7, 31008, 75, 50);
        tab.child(8, 31010, 11, 52);
        tab.child(9, 31011, 75, 70);
        tab.child(10, 31012, 75, 70);
        tab.child(11, 31014, 11, 72);

        tab.child(12, 31015, 103, 52);
        tab.child(13, 31016, 103, 52);

        /* Text area */
        Widget list = addTabInterface(31030);
        list.totalChildren(700);

        int child = 0;
        for (int i = 31031, yPos = 0; i < 31731; i++, yPos += 22) {
            addHoverText(i, "", null, tda, 1, 0xff8000, false, true, 240, 0xFFFFFF);
            interfaceCache[i].actions = new String[]{"Spawn", "Spawn X"};
            list.children[child] = i;
            list.childX[child] = 5;
            list.childY[child] = yPos;
            child++;
        }

        list.height = 154;
        list.width = 174;
        list.scrollMax = 2200;
    }

    public static void settingsTab() {
        Widget p = addTabInterface(44500);

        //Removing adjust bars such as music/sounds
        int[] to_remove = {19131, 19149, 19157, 22635, 941, 942, 943, 944, 945, 19150, 19151, 19152, 19153, 19154, 19155};
        for(int i : to_remove) {
            removeSomething(i);
        }

        for(int i : new int[]{930, 931, 932, 933, 934, 22634}) {
            interfaceCache[i].tooltip = interfaceCache[i].defaultText = "Adjust Camera Zoom";
        }

        //Adding zoom image
        addSpriteLoader(44508, 189);

        //Adding key bindings image
        addSpriteLoader(44510, 411); // 190 key bindings
        addButton(44511, 44500, 40, 40, interfaceCache[19156].disabledSprite, interfaceCache[19156].enabledSprite, -1, "More Settings");
        removeSomething(19156); //Removes house button

        //Adding screen sizes
        addButton(44501, 44500, 54, 46, 185, 186, 44502, "Fixed Screen");
        addHoveredButton_sprite_loader(44502, 186, 54, 46, 44503);

        addButton(44504, 42500, 54, 46, 187, 188, 44505, "Resized Screen");
        addHoveredButton_sprite_loader(44505, 188, 54, 46, 44506);

        p.totalChildren(8);

        //Screen sizes
        setBounds(44501, 30, 95, 0, p);
        setBounds(44502, 30, 95, 1, p);
        setBounds(44504, 110, 95, 2, p);
        setBounds(44505, 110, 95, 3, p);

        //Camera zoom image
        setBounds(44508, 10, 49, 4, p);

        //key bindings images
        setBounds(44511, 132, 212, 5, p);
        setBounds(44510, 141, 220, 6, p);

        //Main settings interface
        setBounds(904, 0, 0, 7, p);

    }

    public static void mainTeleports() {
        addButton(39101, 38100, 79, 30, 1, 805, 174, 175, 39102, "Home Teleport");
        addHoveredButton_sprite_loader(39102, 175, 79, 30, 39103);
        addButton(39104, 38100, 79, 30, 1, 806, 176, 177, 39105, "Other Teleports");
        addHoveredButton_sprite_loader(39105, 177, 79, 30, 39106);
    }

    public static void ancientSpellbookEdit(GameFont[] t) {
        Widget tab = addInterface(39100);
        tab.totalChildren(36);

        //ADD "HOME" AND "OTHER" TELEPORTS
        setBounds(39101, 10, 9, 0, tab);
        setBounds(39102, 10, 9, 1, tab);
        setBounds(39104, 105, 9, 2, tab);
        setBounds(39105, 105, 9, 3, tab);

        //Row 1
        setBounds(12861, 25, 50, 4, tab);
        setBounds(12901, 65, 50, 5, tab);
        setBounds(12987, 105, 50, 6, tab);
        setBounds(12939, 145, 50, 7, tab);

        //Row 2
        setBounds(12881, 25, 90, 8, tab);
        setBounds(12919, 65, 90, 9, tab);
        setBounds(13011, 105, 90, 10, tab);
        setBounds(12963, 145, 90, 11, tab);

        //Row 3
        setBounds(12871, 25, 130, 12, tab);
        setBounds(12911, 65, 130, 13, tab);
        setBounds(12999, 105, 130, 14, tab);
        setBounds(12951, 145, 130, 15, tab);

        //Row 4
        setBounds(12891, 25, 170, 16, tab);
        setBounds(12929, 65, 170, 17, tab);
        setBounds(13023, 105, 170, 18, tab);
        setBounds(12975, 145, 170, 19, tab);

        //Spell hovers

        //Row 1
        setBounds(21758, 3, 180, 20, tab);
        setBounds(21793, 3, 180, 21, tab);
        setBounds(21874, 3, 180, 22, tab);
        setBounds(21903, 3, 180, 23, tab);

        //Row 2
        setBounds(21988, 3, 180, 24, tab);
        setBounds(22018, 3, 180, 25, tab);
        setBounds(22068, 3, 180, 26, tab);
        setBounds(22093, 3, 180, 27, tab);

        //Row 3
        setBounds(22169, 3, 180, 28, tab);
        setBounds(22198, 3, 180, 29, tab);
        setBounds(22252, 3, 180, 30, tab);
        setBounds(22277, 3, 180, 31, tab);

        //Row 4
        setBounds(22352, 3, 10, 32, tab);
        setBounds(22381, 3, 10, 33, tab);
        setBounds(22431, 3, 10, 34, tab);
        setBounds(22460, 3, 10, 35, tab);
    }

    public static void normalSpellbookEdit(GameFont[] t) {
        Widget tab = addInterface(39000);
        tab.totalChildren(62);

        //ADD "HOME" AND "OTHER" TELEPORTS
        setBounds(39101, 10, 9, 0, tab);
        setBounds(39102, 10, 9, 1, tab);
        setBounds(39104, 105, 9, 2, tab);
        setBounds(39105, 105, 9, 3, tab);

        //Row 1
        setBounds(1152, 10, 50, 4, tab);
        setBounds(1154, 40, 50, 5, tab);
        setBounds(1156, 70, 50, 6, tab);
        setBounds(1158, 100, 50, 7, tab);

        //Row 2
        setBounds(1160, 10, 80, 8, tab);
        setBounds(1163, 40, 80, 9, tab);
        setBounds(1166, 70, 80, 10, tab);
        setBounds(1169, 100, 80, 11, tab);

        //Row 3
        setBounds(1172, 10, 110, 12, tab);
        setBounds(1175, 40, 110, 13, tab);
        setBounds(1177, 70, 110, 14, tab);
        setBounds(1181, 100, 110, 15, tab);

        //Row 4
        setBounds(1183, 10, 140, 16, tab);
        setBounds(1185, 40, 140, 17, tab);
        setBounds(1188, 70, 140, 18, tab);
        setBounds(1189, 100, 140, 19, tab);

        //Row 5
        setBounds(1539, 9, 172, 20, tab);
        setBounds(1190, 40, 175, 21, tab);
        setBounds(1191, 70, 173, 22, tab);
        setBounds(1192, 100, 175, 23, tab);

        //Row 6
        setBounds(1572, 35, 230, 24, tab);
        setBounds(1582, 65, 230, 25, tab);
        setBounds(1592, 95, 230, 26, tab);
        setBounds(12445, 125, 230, 27, tab);


        //Side row
        setBounds(1159, 160, 50, 28, tab);
        setBounds(15877, 160, 80, 29, tab);
        setBounds(1173, 164, 110, 30, tab);
        setBounds(1162, 164, 140, 31, tab);
        setBounds(1178, 160, 170, 32, tab);


        //HOVERS

        //Row 1
        setBounds(19226, 3, 180, 33, tab);
        setBounds(19297, 3, 180, 34, tab);
        setBounds(19371, 3, 180, 35, tab);
        setBounds(19429, 3, 180, 36, tab);
        setBounds(19458, 3, 180, 37, tab);

        //Row 2
        setBounds(19487, 3, 180, 38, tab);
        setBounds(19591, 3, 180, 39, tab);
        setBounds(19672, 3, 180, 40, tab);
        setBounds(19753, 3, 180, 41, tab);
        setBounds(20418, 3, 180, 42, tab);

        //Row 3
        setBounds(19897, 3, 180, 43, tab);
        setBounds(19966, 3, 180, 44, tab);
        setBounds(20201, 3, 180, 45, tab);
        setBounds(20360, 3, 180, 46, tab);
        setBounds(19920, 3, 180, 47, tab);

        //Row 4
        setBounds(20576, 3, 180, 48, tab);
        setBounds(20663, 3, 180, 49, tab);
        setBounds(20780, 3, 180, 50, tab);
        setBounds(20867, 3, 180, 51, tab);
        setBounds(19568, 3, 180, 52, tab);

        //Row 5
        setBounds(20088, 3, 10, 53, tab);
        setBounds(20448, 3, 10, 54, tab);
        setBounds(20483, 3, 10, 55, tab);
        setBounds(20518, 3, 10, 56, tab);
        setBounds(20230, 3, 10, 57, tab);

        //Row 6
        setBounds(19539, 3, 10, 58, tab);
        setBounds(20119, 3, 10, 59, tab);
        setBounds(20896, 3, 10, 60, tab);
        setBounds(21012, 3, 10, 61, tab);
    }

    public static void teleportInterface(GameFont[] t) {
        Widget tab = addInterface(38100);
        tab.totalChildren(13);
        tab.drawsTransparent = true;

        //Background
        addTransparentSprite(38101, 163, 255);
        setBounds(38101, 5, 5, 0, tab);

        //Buttons

        /** MONSTERS **/
        addButton(38102, 38100, 173, 38, 1, 800, 164, 165, 38103, "Monsters");
        addHoveredButton_sprite_loader(38103, 165, 173, 38, 38104);
        setBounds(38102, 15, 131, 1, tab);
        setBounds(38103, 15, 131, 2, tab);

        /** BOSSES **/
        addButton(38105, 38100, 173, 38, 1, 801, 166, 167, 38106, "Bosses");
        addHoveredButton_sprite_loader(38106, 167, 173, 38, 38107);
        setBounds(38105, 15, 88, 3, tab);
        setBounds(38106, 15, 88, 4, tab);

        /** Skills **/
        addButton(38108, 38100, 173, 38, 1, 802, 168, 169, 38109, "Skills");
        addHoveredButton_sprite_loader(38109, 169, 173, 38, 38110);
        setBounds(38108, 15, 174, 5, tab);
        setBounds(38109, 15, 174, 6, tab);

        /** Minigames **/
        addButton(38111, 38100, 173, 38, 1, 803, 170, 171, 38112, "Minigames");
        addHoveredButton_sprite_loader(38112, 171, 173, 38, 38113);
        setBounds(38111, 15, 217, 7, tab);
        setBounds(38112, 15, 217, 8, tab);

        /** Wilderness **/
        addButton(38114, 38100, 173, 38, 1, 804, 172, 173, 38115, "Wilderness");
        addHoveredButton_sprite_loader(38115, 173, 173, 38, 38116);
        setBounds(38114, 15, 45, 9, tab);
        setBounds(38115, 15, 45, 10, tab);

        //Close button
        addHoverButton_sprite_loader(38117, 137, 17, 17, "Close", -1, 38118, 1);
        addHoveredButton_sprite_loader(38118, 138, 17, 17, 38119);
        setBounds(38117, 480, 15, 11, tab);
        setBounds(38118, 480, 15, 12, tab);

        monsters(t);
        bosses(t);
        skills(t);
        minigames(t);
        wilderness(t);
    }

    public static void monsters(GameFont[] t) {
        Widget tab = addInterface(38200);
        tab.parent = 38100;
        tab.totalChildren(2);
        setBounds(38100, 0, 0, 0, tab); //Main interface
        setBounds(38201, 180, 47, 1, tab); //Scroll interface (monsters below)

        Widget scroll = addInterface(38201);
        scroll.width = 300;
        scroll.height = 270;
        scroll.scrollPosition = 0;
        scroll.scrollMax = 400;

        //Add all monsters into the scroll..
        String[] tooltips = {"Teleport: Rock Crabs", "Teleport: Pack Yaks", "Teleport: Experiments", "Teleport: Zombies", "Teleport: Bandits", "Teleport: Rock Crab", "Teleport: Rock Crab", "Teleport: Rock Crab", "Teleport: Rock Crab"};
        int[] sprites = {178, 179, 178, 179, 178, 179, 178, 179, 178, 179, 178, 179, 178, 179, 178, 179, 178, 179};
        int sprite_w = 65;
        int sprite_h = 54;
        int index = 0;

        scroll.totalChildren(tooltips.length * 3);

        int frame = 38202, frameHover = frame + 1, bounds = 0;
        for(int i = 0, counter = 0, yDraw = 0; i < tooltips.length; i++, frame+=4, frameHover +=4, counter++, index+=2) {

            int hoverXOffset = 1;
            int hoverYOffset = 55;

            if(counter == 3) {
                hoverXOffset = -120;
                hoverYOffset = 20;
            } else if(counter == 4) {
                counter = 0;
                yDraw += 60;
            }

            int x = 22 + (counter * 70);

            String s = tooltips[i];
            addButton(frame, 38200, sprite_w, sprite_h, sprites[index], sprites[index], frameHover, s);
            addHoveredButtonWTooltip(frameHover, sprites[index+1], sprite_w, sprite_h, frameHover + 1, frameHover + 2, s, hoverXOffset, hoverYOffset);
            setBounds(frame, x, yDraw, bounds++, scroll);
            setBounds(frameHover, x, yDraw, bounds++, scroll);
        }

        //Now do hovers so that sprites dont draw over
        frameHover = 38203;
        for(int i = 0, counter = 0, yDraw = 0; i < tooltips.length; i++, frameHover +=4, counter++) {

            if(counter == 4) {
                counter = 0;
                yDraw += 60;
            }

            int x = 22 + (counter * 70);
            setBounds(frameHover + 2, x, yDraw, bounds++, scroll); // HOVER
        }
    }

    public static void bosses(GameFont[] t) {
        Widget tab = addInterface(38300);
        tab.parent = 38100;
        tab.totalChildren(2);
        setBounds(38100, 0, 0, 0, tab);
        setBounds(38301, 180, 47, 1, tab); //Scroll interface (wildy teles below)

        Widget scroll = addInterface(38301);
        scroll.width = 300;
        scroll.height = 270;
        scroll.scrollPosition = 0;
        scroll.scrollMax = 400;

        //Add all monsters into the scroll..
        String[] tooltips = {"Venenatis", "Callisto", "Chaos Elemental"};
        int[] sprites = {485, 486, 483, 484, 481, 482};
        int sprite_w = 65;
        int sprite_h = 54;
        int index = 0;

        scroll.totalChildren(tooltips.length * 3);

        int frame = 38302, frameHover = frame + 1, bounds = 0;
        for(int i = 0, counter = 0, yDraw = 0; i < tooltips.length; i++, frame+=4, frameHover +=4, counter++, index+=2) {

            int hoverXOffset = 1;
            int hoverYOffset = 55;

            if(counter == 3) {
                hoverXOffset = -120;
                hoverYOffset = 20;
            } else if(counter == 4) {
                counter = 0;
                yDraw += 60;
            }

            int x = 22 + (counter * 70);

            String s = tooltips[i];
            addButton(frame, 38300, sprite_w, sprite_h, sprites[index], sprites[index], frameHover, s);
            addHoveredButtonWTooltip(frameHover, sprites[index+1], sprite_w, sprite_h, frameHover + 1, frameHover + 2, s, hoverXOffset, hoverYOffset);
            setBounds(frame, x, yDraw, bounds++, scroll);
            setBounds(frameHover, x, yDraw, bounds++, scroll);
        }

        //Now do hovers so that sprites dont draw over
        frameHover = 38303;
        for(int i = 0, counter = 0, yDraw = 0; i < tooltips.length; i++, frameHover +=4, counter++) {

            if(counter == 4) {
                counter = 0;
                yDraw += 60;
            }

            int x = 22 + (counter * 70);
            setBounds(frameHover + 2, x, yDraw, bounds++, scroll); // HOVER
        }
    }

    public static void skills(GameFont[] t) {
        Widget tab = addInterface(38400);
        tab.parent = 38100;
        tab.totalChildren(1);
        setBounds(38100, 0, 0, 0, tab);
    }

    public static void minigames(GameFont[] t) {
        Widget tab = addInterface(38500);
        tab.parent = 38100;
        tab.totalChildren(1);
        setBounds(38100, 0, 0, 0, tab);
    }

    public static void wilderness(GameFont[] t) {
        Widget tab = addInterface(38600);
        tab.parent = 38100;
        tab.totalChildren(2);
        setBounds(38100, 0, 0, 0, tab);
        setBounds(38601, 180, 47, 1, tab); //Scroll interface (wildy teles below)

        Widget scroll = addInterface(38601);
        scroll.width = 300;
        scroll.height = 270;
        scroll.scrollPosition = 0;
        scroll.scrollMax = 400;

        //Add all monsters into the scroll..
        String[] tooltips = {"Ditch", "West Dragons", "Obelisk", "Graveyard", "Bandit Camp @red@(Multi)", "Hunter Hill", "Demonic Ruins @red@(Multi)", "Runite Rocks", "The Gate", "Target Teleport"};
        int[] sprites = {393, 394, 395, 396, 397, 398, 399, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 452, 453};
        int sprite_w = 65;
        int sprite_h = 54;
        int index = 0;

        scroll.totalChildren(tooltips.length * 3);

        int frame = 38602, frameHover = frame + 1, bounds = 0;
        for(int i = 0, counter = 0, yDraw = 0; i < tooltips.length; i++, frame+=4, frameHover +=4, counter++, index+=2) {

            int hoverXOffset = 1;
            int hoverYOffset = 55;

            if(counter == 3) {
                hoverXOffset = -120;
                hoverYOffset = 20;
            } else if(counter == 4) {
                counter = 0;
                yDraw += 60;
            }

            int x = 22 + (counter * 70);

            String s = tooltips[i];
            addButton(frame, 38600, sprite_w, sprite_h, sprites[index], sprites[index], frameHover, s);
            addHoveredButtonWTooltip(frameHover, sprites[index+1], sprite_w, sprite_h, frameHover + 1, frameHover + 2, s, hoverXOffset, hoverYOffset);
            setBounds(frame, x, yDraw, bounds++, scroll);
            setBounds(frameHover, x, yDraw, bounds++, scroll);
        }

        //Now do hovers so that sprites dont draw over
        frameHover = 38603;
        for(int i = 0, counter = 0, yDraw = 0; i < tooltips.length; i++, frameHover +=4, counter++) {

            if(counter == 4) {
                counter = 0;
                yDraw += 60;
            }

            int x = 22 + (counter * 70);
            setBounds(frameHover + 2, x, yDraw, bounds++, scroll); // HOVER
        }
    }

    public static void addButton(int i, int parent, int w, int h, int config, int configFrame, int sprite1, int sprite2, int hoverOver, String tooltip) {
        Widget p = addInterface(i);
        p.parent = parent;
        p.type = TYPE_SPRITE;
        p.atActionType = 1;
        p.width = w;
        p.height = h;
        p.requiredValues = new int[1];
        p.valueCompareType = new int[1];
        p.valueCompareType[0] = 1;
        p.requiredValues[0] = config;
        p.valueIndexArray = new int[1][3];
        p.valueIndexArray[0][0] = 5;
        p.valueIndexArray[0][1] = configFrame;
        p.valueIndexArray[0][2] = 0;
        p.tooltip = tooltip;
        p.defaultText = tooltip;
        p.hoverType = hoverOver;
        p.disabledSprite = Client.cacheSprite[sprite1];
        p.enabledSprite = Client.cacheSprite[sprite2];
    }

    public static void addButton(int i, int parent, int w, int h, int sprite1, int sprite2, int hoverOver, String tooltip) {
        Widget p = addInterface(i);
        p.parent = parent;
        p.type = TYPE_SPRITE;
        p.atActionType = 1;
        p.width = w;
        p.height = h;
        p. tooltip = tooltip;
        p.defaultText = tooltip;
        p.hoverType = hoverOver;
        p.disabledSprite = Client.cacheSprite[sprite1];
        p.enabledSprite = Client.cacheSprite[sprite2];
    }


    public static void addButton(int i, int parent, int w, int h, Sprite sprite1, Sprite sprite2, int hoverOver, String tooltip) {
        Widget p = addInterface(i);
        p.parent = parent;
        p.type = TYPE_SPRITE;
        p.atActionType = 1;
        p.width = w;
        p.height = h;
        p.tooltip = tooltip;
        p.defaultText = tooltip;
        p.hoverType = hoverOver;
        p.disabledSprite = sprite1;
        p.enabledSprite = sprite2;
    }

    public static void addHoveredButtonWTooltip(int i, int spriteId, int w, int h, int IMAGEID, int tooltipId, String hover, int hoverXOffset, int hoverYOffset) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.parent = i;
        tab.id = i;
        tab.type = 0;
        tab.atActionType = 0;
        tab.width = w;
        tab.height = h;
        tab.invisible = true;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.scrollMax = 0;
        addHoverImage_sprite_loader(IMAGEID, spriteId);
        tab.totalChildren(1);
        tab.child(0, IMAGEID, 0, 0);

        Widget p = addTabInterface(tooltipId);
        p.parent = i;
        p.type = 8;
        p.width = w;
        p.height = h;

        p.hoverText = p.defaultText =
                p.tooltip = hover;

        p.hoverXOffset = hoverXOffset;
        p.hoverYOffset = hoverYOffset;
        p.regularHoverBox = true;

    }

    public static final int BEGIN_READING_PRAYER_INTERFACE = 6;//Amount of total custom prayers we've added
    public static final int CUSTOM_PRAYER_HOVERS = 3; //Amount of custom prayer hovers we've added

    public static final int PRAYER_INTERFACE_CHILDREN = 80 + BEGIN_READING_PRAYER_INTERFACE + CUSTOM_PRAYER_HOVERS;

    public int hoverXOffset = 0;
    public int hoverYOffset = 0;
    public int spriteXOffset = 0;
    public int spriteYOffset = 0;
    public boolean regularHoverBox;
    public boolean bigText = false;
    public boolean drawNumber = false;
    public boolean skillHoverBox = false;
    public boolean rightHover = false;
    public boolean glowing = false;
    public int totalWidth = 0;

    public static String[] names = {"Attack", "Hitpoints", "Mining", "Strength", "Agility", "Smithing", "Defence",
            "Herblore", "Fishing", "Ranged", "Thieving", "Cooking", "Prayer", "Crafting",
            "Firemaking", "Magic", "Fletching", "Woodcutting", "Runecraft", "Slayer",
            "Farming", "Construction", "Hunter"};

    public static void skillsTab(GameFont[] tda) {

        Widget widget = addTabInterface(10000);
        int skillTabChild = 0;
        int x = 1;
        int y = 1;
        widget.totalChildren(118);

        for(int i = 0; i < 24; i++) {
            if(i <= 22) {
                addButton(10001 + i, 10000, 62, 32, 548, 548, 10151 + i, "View @or1@" + names[i] + " @whi@guide");

                addSprite(10031 + i, 550 + i);
                addText(10061 + i, "", tda, 0, 0xffff00, true, true);
                addText(10091 + i, "", tda, 0, 0xffff00, true, true);


            } else if(i >= 23) {
                addSprite(10001 + i, 549);
                addText(10120, "Total level:", tda, 0, 0xffff00, true, true);
                addText(10121, "", tda, 0, 0xffff00, true, true);
            }
            if(x < 180) {
                widget.child(skillTabChild++, 10001 + i, x, y);

                if(i < 23) {
                    widget.child(skillTabChild++, 10031 + i, x + 4, y + 4);
                    widget.child(skillTabChild++, 10061 + i, x + 39, y + 4);
                    widget.child(skillTabChild++, 10091 + i, x + 51, y + 15);
                }
            } else {
                x = 0;
                y += 32;
                widget.child(skillTabChild++, 10001 + i, x, y);
                if(i < 23) {
                    widget.child(skillTabChild++, 10031 + i, x + 4, y + 4);
                    widget.child(skillTabChild++, 10061 + i, x + 39, y + 4);
                    widget.child(skillTabChild++, 10091 + i, x + 51, y + 15);

                }
            }
            x+= 63;

        }
        int y2 = 35;
        addSkillHover(10151, 10, y2, true, 0, names[0], 150, false);
        addSkillHover(10152, -5, y2, true, 3, names[1], 150, false);
        addSkillHover(10153, -30, y2, true, 14, names[2], 150, true);
        addSkillHover(10154, 10, y2, true, 2, names[3], 150, false);
        addSkillHover(10155, -5, y2, true, 16, names[4], 150, false);
        addSkillHover(10156, -30, y2, true, 13, names[5], 150, true);
        addSkillHover(10157, 10, y2, true, 1, names[6], 150, false);
        addSkillHover(10158, -5, y2, true, 15, names[7], 150, false);
        addSkillHover(10159, -30, y2, true, 10, names[8], 150, true);
        addSkillHover(10160, 10, y2, true, 4, names[9], 150, false);
        addSkillHover(10161, -5, y2, true, 17, names[10], 150, false);
        addSkillHover(10162, -30, y2, true, 7, names[11], 150, true);
        addSkillHover(10163, 10, y2, true, 5, names[12], 150, false);
        addSkillHover(10164, -5, y2, true, 12, names[13], 150, false);
        addSkillHover(10165, -30, y2, true, 11, names[14], 150, true);
        addSkillHover(10166, 10, y2, true, 6, names[15], 150, false);
        addSkillHover(10167, -5, y2, true, 9, names[16], 150, false);
        addSkillHover(10168, -30, y2, true, 8, names[17], 150, true);
        addSkillHover(10169, 10, -50, true, 20, names[18], 150, false);
        addSkillHover(10170, -5, -50, true, 18, names[19], 150, false);
        addSkillHover(10171, -30, -50, true, 19, names[20], 150, true);
        addSkillHover(10172, 10, -50, true, 22, names[21], 150, false);
        addSkillHover(10173, -5, -50, true, 21, names[22], 150, false);
        widget.child(skillTabChild++, 10120, 157, 231);
        widget.child(skillTabChild++, 10121, 157, 241);
        y = 1;
        x = 1;
        for(int i = 0; i <= 22; i++) {
            if(x < 180) {
                widget.child(skillTabChild++, 10151 + i, x, y);
            } else {
                y += 32;
                x = 0;
                widget.child(skillTabChild++, 10151 + i, x, y);

            }
            x += 63;
        }




    }



    public static void prayerBook() {

        Widget rsinterface = interfaceCache[5608];

        //Moves down chivalry
        rsinterface.childX[50 + BEGIN_READING_PRAYER_INTERFACE] = 10;
        rsinterface.childY[50+ BEGIN_READING_PRAYER_INTERFACE] = 195;
        rsinterface.childX[51+ BEGIN_READING_PRAYER_INTERFACE] = 10;
        rsinterface.childY[51+ BEGIN_READING_PRAYER_INTERFACE] = 195;
        rsinterface.childX[63+ BEGIN_READING_PRAYER_INTERFACE] = 10;
        rsinterface.childY[63+ BEGIN_READING_PRAYER_INTERFACE] = 190;
        //Adjust prayer glow sprites position - Chivalry
        interfaceCache[rsinterface.children[50+ BEGIN_READING_PRAYER_INTERFACE]].spriteXOffset = -7;
        interfaceCache[rsinterface.children[50+ BEGIN_READING_PRAYER_INTERFACE]].spriteYOffset = -2;

        //Moves piety to the right
        setBounds(19827, 43, 191, 52+ BEGIN_READING_PRAYER_INTERFACE, rsinterface);

        //Adjust prayer glow sprites position - Piety
        interfaceCache[rsinterface.children[52+ BEGIN_READING_PRAYER_INTERFACE]].spriteXOffset = -2;
        interfaceCache[rsinterface.children[52+ BEGIN_READING_PRAYER_INTERFACE]].spriteYOffset = 2;

        rsinterface.childX[53+ BEGIN_READING_PRAYER_INTERFACE] = 43;
        rsinterface.childY[53+ BEGIN_READING_PRAYER_INTERFACE] = 204;
        rsinterface.childX[64+ BEGIN_READING_PRAYER_INTERFACE] = 43;
        rsinterface.childY[64+ BEGIN_READING_PRAYER_INTERFACE] = 190;


        //Now we add new prayers..
        //AddPrayer adds a glow at the id
        //Adds the actual prayer sprite at id+1
        //Adds a hover box at id + 2
        addPrayer(28001, "Activate @or1@Preserve", 31, 32, 150, -2, -1, 151, 152, 1, 708, 28003);
        setBounds(28001, 155, 160, 0, rsinterface); //Prayer glow sprite
        setBounds(28002, 155, 160, 1, rsinterface); //Prayer sprites


        addPrayer(28004, "Activate @or1@Rigour", 31, 32, 150, -3, -5, 153, 154, 1, 710, 28006);
        setBounds(28004, 81, 196, 2, rsinterface); //Prayer glow sprite
        setBounds(28005, 81, 196, 3, rsinterface); //Prayer sprites

        addPrayer(28007, "Activate @or1@Augury", 31, 32, 150, -3, -5, 155, 156, 1, 712, 28009);
        setBounds(28007, 118, 194, 4, rsinterface); //Prayer glow sprite
        setBounds(28008, 118, 194, 5, rsinterface); //Prayer sprites

        //Now we add hovers..
        addPrayerHover(28003, "Level 55\nPreserve\nBoosted stats last 50% longer.", -135, -60, true);
        setBounds(28003, 153, 158,  86, rsinterface); //Hover box

        addPrayerHover(28006, "Level 74\nRigour\nIncreases your ranged attack\nby 20% and damage by 23%,\n and your defence by 25%", -70, -100, true);
        setBounds(28006, 84, 200, 87, rsinterface); //Hover box

        addPrayerHover(28009, "Level 77\nAugury\nIncreases your magical attack\nand defence by 25%, and your"
                + "\ndefence by 25%", -110, -100, true);
        setBounds(28009, 120, 198, 88, rsinterface); //Hover box

    }

    public static void addPrayer(int ID, String tooltip, int w, int h, int glowSprite, int glowX, int glowY, int disabledSprite, int enabledSprite, int config, int configFrame, int hover) {
        Widget p = addTabInterface(ID);

        //Adding config-toggleable glow on the prayer
        //Also clickable
        p.parent = 5608;
        p.type = TYPE_SPRITE;
        p.atActionType = 1;
        p.width = w;
        p.height = h;
        p.requiredValues = new int[1];
        p.valueCompareType = new int[1];
        p.valueCompareType[0] = 1;
        p.requiredValues[0] = config;
        p.valueIndexArray = new int[1][3];
        p.valueIndexArray[0][0] = 5;
        p.valueIndexArray[0][1] = configFrame;
        p.valueIndexArray[0][2] = 0;
        p.tooltip = tooltip;
        p.defaultText = tooltip;
        p.hoverType = 52;
        p.enabledSprite = Client.cacheSprite[glowSprite];
        p.spriteXOffset = glowX;
        p.spriteYOffset = glowY;

        //Adding config-toggleable prayer sprites
        //not clickable
        p = addTabInterface(ID + 1);
        p.parent = 5608;
        p.type = TYPE_SPRITE;
        p.atActionType = 0;
        p.width = w;
        p.height = h;
        p.requiredValues = new int[1];
        p.valueCompareType = new int[1];
        p.valueCompareType[0] = 2;
        p.requiredValues[0] = 1;
        p.valueIndexArray = new int[1][3];
        p.valueIndexArray[0][0] = 5;
        p.valueIndexArray[0][1] = configFrame + 1;
        p.valueIndexArray[0][2] = 0;
        p.tooltip = tooltip;
        p.defaultText = tooltip;
        p.enabledSprite = Client.cacheSprite[disabledSprite]; //imageLoader(disabledSprite, "s");
        p.disabledSprite = Client.cacheSprite[enabledSprite]; //imageLoader(enabledSprite, "s");
        p.hoverType = hover;
    }

    public static void addPrayerHover(int ID, String hover, int xOffset, int yOffset, boolean big) {
        //Adding hover box
        Widget p = addTabInterface(ID);
        p.parent = 5608;
        p.type = 8;
        p.width = 40;
        p.height = 32;
        p.hoverText = p.defaultText =  hover;
        p.hoverXOffset = xOffset - 10;
        p.hoverYOffset = yOffset;
        p.regularHoverBox = true;
        p.bigText = true;
    }
    public static void addSkillHover(int ID, int xOffset, int yOffset, boolean big, int skillId, String skillName, int intWidth, boolean farRight) {
        //Adding hover box
        Widget p = addTabInterface(ID);
        p.parent = 10000;
        p.type = 8;
        p.width = 63;
        p.height = 32;
        p.hoverXOffset = xOffset - 10;
        p.hoverYOffset = yOffset;
        p.skillHoverBox = true;
        p.bigText = true;
        p.skillId = skillId;
        p.skillName = skillName;
        p.intWidth = intWidth;
        p.rightHover  = farRight;
    }

    /*
     * Price checker interface
     */
    private static void priceChecker(GameFont[] fonts) {
        Widget rsi = addTabInterface(22000);
        final String[] options = {"Remove 1", "Remove 5", "Remove 10", "Remove All", "Remove X", null};
        addSprite(21999, 573);


        addHoverButton_sprite_loader(22003, 574, 36, 36, "Deposit All", -1, 22004, 1);
        addHoveredButton_sprite_loader(22004, 575, 35, 35, 18254);

        addText(22005, "0", fonts, 1, 0xFFFFFF, true, true);
        addText(22006, "Total guide price:", fonts, 1, 0xff981f, true, true);

        addHoverButton_sprite_loader(22001, 579, 23, 23, "Close", -1, 22002, 1);
        addHoveredButton_sprite_loader2(22002, 580, 23, 23, 18251);

        addButton(22007, 22000, 40, 36, 578, 578, 18200, "Search for item");
        addSprite(22008, 577);
        addTransparentSpriteGlow(22009, 576, 0);
        addText(22010, "", fonts, 1, 0xff981f, false, true);
        addText(22011, "", fonts, 1, 0xffffff, false, true);
        Widget itemContainerScroller = addTabInterface(18503);

        int scrollchild = 0;
        itemContainerScroller.totalChildren(57);
        itemContainerScroller.child(scrollchild++, 18500, 36, 1);
        itemContainerScroller.height = 222;
        itemContainerScroller.width = 449;
        itemContainerScroller.scrollMax = 230;

        Widget rewardContainer = addTabInterface(18501);
        rewardContainer.spritesX = new int[1];
        rewardContainer.spritesY = new int[1];
        rewardContainer.inventoryItemId = new int[1];
        rewardContainer.inventoryAmounts = new int[1];
        rewardContainer.filled = false;
        rewardContainer.replaceItems = false;
        rewardContainer.usableItems = false;
        rewardContainer.hasActions = false;
        rewardContainer.invisible = true;
        rewardContainer.allowSwapItems = false;
        rewardContainer.spritePaddingX = 0;
        rewardContainer.spritePaddingY = 0;
        rewardContainer.height = 1;
        rewardContainer.width = 1;
        rewardContainer.parent = 22000;
        rewardContainer.type = TYPE_INVENTORY;
        rewardContainer.drawNumber = true;
        //Actual items
        Widget container = addTabInterface(18500);
        container.actions = options;
        container.spritesX = new int[20];
        container.spritesY = new int[20];
        container.inventoryItemId = new int[28];
        container.inventoryAmounts = new int[28];
        container.centerText = true;
        container.filled = false;
        container.replaceItems = false;
        container.usableItems = false;
        //rsi.isInventoryInterface = false;
        container.allowSwapItems = false;
        container.spritePaddingX = 55;
        container.spritePaddingY = 30;
        container.height = 6;
        container.width = 5;
        container.parent = 42000;
        container.type = TYPE_INVENTORY;

        rsi.totalChildren(14);
        int child = 0;

        rsi.child(child++, 21999, 15, 10);//was 10 so + 10
        rsi.child(child++, 22001, 465, 16);
        rsi.child(child++, 22005, 251, 298);
        rsi.child(child++, 22006, 250, 282);
        rsi.child(child++, 22002, 465, 16); //Close button hover
        rsi.child(child++, 18503, 20, 50); //Container
        rsi.child(child++, 22007, 24, 280);
        rsi.child(child++, 22009, 24, 280);
        rsi.child(child++, 22010, 70, 282);
        rsi.child(child++, 22011, 70, 297);
        rsi.child(child++, 18501, 28, 282);
        rsi.child(child++, 22008, 27, 283);

        //Deposit hovers
        rsi.child(child++, 22003, 449, 280);
        rsi.child(child++, 22004, 449, 280);

        //Add text next to items, ROW 1
        int interface_ = 18300;
        int xDraw = 55;
        int yDraw = 35;
        int counter = 0;
        for(int i = 0; i < container.inventoryItemId.length; i++) {

            addText(interface_, "", fonts, 0, 0xFFFFFF, true, true);
            itemContainerScroller.child(scrollchild++, interface_, xDraw, yDraw);

            interface_++;
            counter++;
            xDraw += 86;

            if(counter == container.width) {
                xDraw = 55;
                yDraw += 62;
                counter = 0;
            }
        }

        //Add text next to items, ROW 2
        interface_ = 18400;
        xDraw = 55;
        yDraw = 48;
        counter = 0;
        for(int i = 0; i < container.inventoryItemId.length; i++) {

            addText(interface_, "", fonts, 0, 0xFFFFFF, true, true);
            itemContainerScroller.child(scrollchild++, interface_, xDraw, yDraw);

            interface_++;
            counter++;
            xDraw += 86;

            if(counter == container.width) {
                xDraw = 55;
                yDraw += 62;
                counter = 0;
            }
        }
    }

    public static void shop() {

        //Set up the shop inventory
        Widget shopInventory = interfaceCache[3900];
        shopInventory.inventoryItemId = new int[1000];
        shopInventory.inventoryAmounts = new int[1000];
        shopInventory.drawInfinity = true;
        shopInventory.width = 9;
        shopInventory.height = 200;
        shopInventory.spritePaddingX = 18;
        shopInventory.spritePaddingY = 25;

        //The scroll, add the shop inventory to it.
        Widget scroll = addTabInterface(29995);
        scroll.totalChildren(1);
        setBounds(3900, 0, 0, 0, scroll);
        scroll.height = 210;
        scroll.width = 445;
        scroll.scrollMax = 230;

        //Position the item container in the actual shop interface
        setBounds(29995, 26, 65, 75, interfaceCache[3824]);
    }

    public static void bounty(GameFont[] TDA) {
        Widget tab = addTabInterface(23300);
        addTransparentSprite(23301, 97, 150);

        addConfigSprite(23303, -1, 98, 0, 876);
        //  addSprite(23304, 104);

        addText(23305, "---", TDA, 0, 0xffff00, true, true);
        addText(23306, "Target:", TDA, 0, 0xffff00, true, true);
        addText(23307, "None", TDA, 1, 0xffffff, true, true);
        addText(23308, "Level: ------", TDA, 0, 0xffff00, true, true);

        addText(23309, "Current  Record", TDA, 0, 0xffff00, true, true);
        addText(23310, "0", TDA, 0, 0xffff00, true, true);
        addText(23311, "0", TDA, 0, 0xffff00, true, true);
        addText(23312, "0", TDA, 0, 0xffff00, true, true);
        addText(23313, "0", TDA, 0, 0xffff00, true, true);
        addText(23314, "Rogue:", TDA, 0, 0xffff00, true, true);
        addText(23315, "Hunter:", TDA, 0, 0xffff00, true, true);

        addConfigSprite(23316, -1, 99, 0, 877);
        addConfigSprite(23317, -1, 100, 0, 878);
        addConfigSprite(23318, -1, 101, 0, 879);
        addConfigSprite(23319, -1, 102, 0, 880);
        addConfigSprite(23320, -1, 103, 0, 881);
        addText(23321, "Level: ", TDA, 1, 0xFFFF33, true, false);

        //Kda
        addTransparentSprite(23322, 97, 150);
        addText(23323, "Targets killed: 0", TDA, 0, 0xFFFF33, true, false);
        addText(23324, "Players killed: 0", TDA, 0, 0xFFFF33, true, false);
        addText(23325, "Deaths: 0", TDA, 0, 0xFFFF33, true, false);

        tab.totalChildren(17);
        tab.child(0, 23301, 319, 1);
        tab.child(1, 23322, 319, 47);
        //  tab.child(1, 23302, 339, 56);
        tab.child(2, 23303, 345, 58);
        // tab.child(2, 23304, 348, 73);
        tab.child(3, 23305, 358, 77);
        tab.child(4, 23306, 455, 51);
        tab.child(5, 23307, 456, 64);
        tab.child(6, 23308, 457, 80);
        //  tab.child(8, 23309, 460, 59);
        //  tab.child(9, 23310, 438, 72);
        //  tab.child(10, 23311, 481, 72);
        //  tab.child(11, 23312, 438, 85);
        //  tab.child(12, 23313, 481, 85);
        //  tab.child(13, 23314, 393, 72);
        //  tab.child(14, 23315, 394, 85);
        tab.child(7, 23316, 345, 58);
        tab.child(8, 23317, 345, 58);
        tab.child(9, 23318, 345, 58);
        tab.child(10, 23319, 345, 58);
        tab.child(11, 23320, 345, 58);

        tab.child(12, 23323, 435, 6);
        tab.child(13, 23324, 435, 19);
        tab.child(14, 23325, 435, 32);

        interfaceCache[197].childX[0] = 0;
        interfaceCache[197].childY[0] = 0;

        tab.child(15, 197, 331, 6);
        tab.child(16, 23321, 361, 31);

    }
    public static void addHoverButton_sprite_loader5(int i, int spriteId, int width, int height, String text,
                                                     int contentType, int hoverOver, int aT) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.id = i;
        tab.parent = i;
        tab.type = 5;
        tab.atActionType = OPTION_CLOSE;
        tab.contentType = contentType;
        tab.opacity = 0;
        tab.hoverType = hoverOver;
        tab.disabledSprite = Client.cacheSprite[spriteId];
        tab.enabledSprite = Client.cacheSprite[spriteId];
        tab.width = width;
        tab.height = height;
        tab.tooltip = text;
    }

    public static void addHoveredButton_sprite_loader5(int i, int spriteId, int w, int h, int IMAGEID) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.parent = i;
        tab.id = i;
        tab.type = 0;
        tab.atActionType = OPTION_CLOSE;
        tab.width = w;
        tab.height = h;
        tab.invisible = true;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.scrollMax = 0;
        addHoverImage_sprite_loader(IMAGEID, spriteId);
        tab.totalChildren(1);
        tab.child(0, IMAGEID, 0, 0);
    }

	/*public static void adjustableConfig(int id, String tooltip, int sprite, int opacity, int enabledSpriteBehind, int disabledSpriteBehind) {
		Widget tab = addInterface(id);
		tab.tooltip = tooltip;
		tab.atActionType = OPTION_OK;
		tab.type = TYPE_ADJUSTABLE_CONFIG;
		tab.enabledSprite = Client.cacheSprite3[sprite];
		tab.enabledAltSprite = Client.cacheSprite3[enabledSpriteBehind];
		tab.disabledAltSprite = Client.cacheSprite3[disabledSpriteBehind];
		tab.width = tab.enabledAltSprite.myWidth;
		tab.height = tab.disabledAltSprite.myHeight;
		tab.spriteOpacity = opacity;
	}



	private static void bankInterface(GameFont[] tda) {
		Widget bank = addInterface(5292);

		setChildren(45, bank);

		int id = 50000;
		int child = 0;

		Sprite disabled = Client.cacheSprite4[7];
		Sprite enabled = Client.cacheSprite4[8];

		addSprite(id, 106);
		addHoverButton_sprite_loader5(id + 1, 107, 25, 25, "Close", -1, id + 2, 1);
		addHoveredButton_sprite_loader5(id + 2, 108, 25, 25, id + 3);

		bank.child(child++, id, 12, 2);
		bank.child(child++, id + 1, 472, 9);
		bank.child(child++, id + 2, 472, 9);
		adjustableConfig(id + 9, "Toggle @lre@Always set placeholders", 125, 180, 127, 126);
		bank.child(child++, id + 9, 335, 291);
		adjustableConfig(id + 10, "Search", 128, 180, 127, 126);
		bank.child(child++, id + 10, 378, 291);

		adjustableConfig(id + 7, "Deposit worn items", 130, 180, 127, 126);
		bank.child(child++, id + 7, 458, 291);
		adjustableConfig(id + 4, "Deposit inventory", 129, 180, 127, 126);
		bank.child(child++, id + 4, 421, 291);
		addButton(id + 13, getSprite(0, interfaceLoader, "miscgraphics3"), getSprite(0, interfaceLoader, "miscgraphics3"), "Show menu", 25, 25);
		addSprite(id + 14, 5);
		bank.child(child++, id + 13, 463, 44);
		bank.child(child++, id + 14, 468, 48);
		addText(id + 53, "%1", tda, 0, 0xFE9624, true);
		Widget line = addInterface(id + 54);
		line.type = 3;
		line.allowSwapItems = true;
		line.width = 12;
		line.height = 1;
		line.textColor = 0xFE9624;
		addText(id + 55, "352", tda, 0, 0xFE9624, true);
		addText(15383, "352", tda, 2, 0xFE9624, true, true);
		bank.child(child++, id + 53, 30, 8);
		bank.child(child++, id + 54, 24, 19);
		bank.child(child++, id + 55, 30, 20);

		bank.child(child++, 15383, 255, 12);

		bank.child(child++, 5385, 29, 79);
		bank.child(child++, 8131, 102, 306);
		bank.child(child++, 8130, 17, 306);
		bank.child(child++, 5386, 282, 306);
		bank.child(child++, 5387, 197, 306);
		bank.child(child++, 8132, 114, 309);
		bank.child(child++, 8133, 41, 309);
		bank.child(child++, 5390, 45, 291);
		bank.child(child++, 5389, 202, 309);
		bank.child(child++, 5391, 276, 309);
		bank.child(child++, 5388, 217, 291);

		id = 50070;
		for (int tab = 0, counter = 0; tab <= 36; tab += 4, counter++) {
			int[] requiredValues = new int[]{1};
			int[] valueCompareType = new int[]{1};
			int[][] valueIndexArray = new int[1][3];
			valueIndexArray[0][0] = 5;
			valueIndexArray[0][1] = 1000 + counter; //Config
			valueIndexArray[0][2] = 0;

			addHoverConfigButton(id + tab, id + 1 + tab, 206, -1, 39, 40, null, valueCompareType, requiredValues, valueIndexArray);
			addHoveredConfigButton(interfaceCache[id + tab], id + 1 + tab, id + 2 + tab, 207, -1);

			interfaceCache[id + tab].actions = new String[]{"Select", tab == 0 ? null : "Collapse", null, null, null};
			interfaceCache[id + tab].parent = id;
			interfaceCache[id + tab].drawingDisabled = true;
			interfaceCache[id + 1 + tab].parent = id;
			bank.child(child++, id + tab, Client.bankExtraX + 18 + 40 * (tab / 4), 37);
			bank.child(child++, id + 1 + tab, Client.bankExtraX + 18 + 40 * (tab / 4), 37);
		}

		interfaceCache[5385].height = 210;
		interfaceCache[5385].width = 449;

		int[] interfaces = new int[] { 5386, 5387, 8130, 8131 };

		for (int rsint : interfaces) {
			interfaceCache[rsint].disabledSprite = disabled;
			interfaceCache[rsint].enabledSprite = enabled;
			interfaceCache[rsint].width = enabled.myWidth;
			interfaceCache[rsint].height = enabled.myHeight;
		}

		interfaceCache[8130].disabledSprite = Client.cacheSprite[7];
		interfaceCache[8130].enabledSprite = Client.cacheSprite4[8];
		interfaceCache[8130].width = 75;

		interfaceCache[8131].disabledSprite = Client.cacheSprite4[7];
		interfaceCache[8131].enabledSprite = Client.cacheSprite4[8];
		interfaceCache[8131].width = 75;
		interfaceCache[8131].horizontalOffset = -9;

		interfaceCache[5387].disabledSprite = Client.cacheSprite4[7];
		interfaceCache[5387].enabledSprite = Client.cacheSprite4[8];
		interfaceCache[5387].width = 75;
		interfaceCache[5387].horizontalOffset = -21;

		interfaceCache[5386].disabledSprite = Client.cacheSprite4[7];
		interfaceCache[5386].enabledSprite = Client.cacheSprite4[8];
		interfaceCache[5386].width = 75;
		interfaceCache[5386].horizontalOffset = -30;

		addSprite(50040, 208);
		bank.child(child++, 50040, 20 + Client.bankExtraX, 41);


		final Widget scrollBar = Widget.interfaceCache[5385];
		scrollBar.totalChildren(Client.MAX_BANK_TABS);
		for(int i = 0; i < Client.MAX_BANK_TABS; i++) {
			addBankTabContainer(50300 + i, 109, 10, 35, 352, new String[] { "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-X", "Withdraw-All", "Withdraw-All-but-one", "Placeholder" });
			scrollBar.child(i, 50300 + i, 38, 0);
		}
	}*/

    public static void itemsKeptOnDeath(GameFont[] tda) {

        removeSomething(16999); //close button in text
        Widget rsinterface = interfaceCache[10494];
        rsinterface.spritePaddingX = 6;
        rsinterface.spritePaddingY = 5;
        rsinterface = interfaceCache[10600];
        rsinterface.spritePaddingX = 6;
        rsinterface.spritePaddingY = 5;


        rsinterface = addInterface(17100);
        addSpriteLoader(17101, 139);
		/*Widget scroll = addTabInterface(17149);
		scroll.width = 300; scroll.height = 183; scroll.scrollMax = 220;*/
        addText(17103, "Items Kept on Death", tda, 2, 0xff981f, false, false);
        addText(17104, "Items you will keep on death:", tda, 1, 0xff981f, false, false);
        addText(17105, "Items you will lose on death:", tda, 1, 0xff981f, false, false);
        addText(17106, "Info", tda, 1, 0xff981f, false, false);
        addText(17107, "3", tda, 2, 0xffff00, false, false);
        String[] options = {null};

        /*
         * Items on interface
         */

        //Top Row
        for(int top = 17108; top <= 17111; top++) {
            addItemOnInterface(top, 17100, options);
        }
        //1st row
        for(int top = 17112; top <= 17119; top++) {
            addItemOnInterface(top, 17100, options);
        }
        //2nd row
        for(int top = 17120; top <= 17127; top++) {
            addItemOnInterface(top, 17100, options);
        }
        //3rd row
        for(int top = 17128; top <= 17135; top++) {
            addItemOnInterface(top, 17100, options);
        }
        //4th row
        for (int top = 17136; top <= 17142; top++) {
            addItemOnInterface(top, 17100, options);
        }
        //5th row
        for (int top = 17143; top <= 17148; top++) {
            addItemOnInterface(top, 17100, options);
        }

        //6th row (4 items)
        for(int top = 17149; top <= 17152; top++) {
            addItemOnInterface(top, 17100, options);
        }

        setChildren(56, rsinterface);
        //addTabInterface(5);
        setBounds(17101, 7,8, 0, rsinterface);
        setBounds(16999, 478, 14, 1, rsinterface);
        setBounds(17103, 185, 18, 2, rsinterface);
        setBounds(17104, 22, 50, 3, rsinterface);
        setBounds(17105, 22, 110, 4, rsinterface);
        setBounds(17106, 347, 50, 5, rsinterface);

        setBounds(17107, 412, 287, 6, rsinterface);
        setBounds(17149, 23, 132, 7, rsinterface);
        //setBounds(17018, 480, 18, 8, rsinterface);
        //setBounds(17019, 480, 18, 9, rsinterface);

        setBounds(38117, 480, 18, 8, rsinterface);
        setBounds(38118, 480, 18, 9, rsinterface);

        //setBounds(5, 480, 18, 10, rsinterface);

        //Positions for  item on interface (items kept on death
        int	child_index = 10;
        int topPos = 26;
        for(int top = 17108; top <= 17111; top++) {
            setBounds(top, topPos, 72, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }
        //setBounds(17000, 478, 14, child_index++, rsinterface);
        itemsOnDeathDATA(tda);
        setBounds(17315, 348, 64, child_index++, rsinterface);

        topPos = 26;

        //1st row
        for(int top = 17112; top <= 17118; top++) {
            setBounds(top, topPos, 133, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }
        //2nd row
        topPos = 26;
        for(int top = 17119; top <= 17125; top++) {
            setBounds(top, topPos, 168, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }
        //3rd row
        topPos = 26;
        for(int top = 17126; top <= 17132; top++) {
            setBounds(top, topPos, 203, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }
        //4th row
        topPos = 26;
        for (int top = 17133; top <= 17139; top++) {
            setBounds(top, topPos, 238, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }
        //5th row
        topPos = 26;
        for (int top = 17140; top <= 17145; top++) {
            setBounds(top, topPos, 273, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }

        //6th row (4 items)
        topPos = 26;
        for(int top = 17146; top <= 17152; top++) {
            setBounds(top, topPos, 311, child_index, rsinterface);
            topPos += 44;
            child_index++;
        }
    }

    public static void itemsOnDeathDATA(GameFont[] tda) {
        Widget RSinterface = addInterface(17315);
        addText(17309, "", 0xff981f, false, false, 0, tda, 0);
        addText(17310, "The normal amount of", 0xff981f, false, false, 0, tda, 0);
        addText(17311, "items kept is three.", 0xff981f, false, false, 0, tda, 0);
        addText(17312, "", 0xff981f, false, false, 0, tda, 0);
        addText(17313, "If you are skulled,", 0xff981f, false, false, 0, tda, 0);
        addText(17314, "you will lose all your", 0xff981f, false, false, 0, tda, 0);
        addText(17317, "items, unless an item", 0xff981f, false, false, 0, tda, 0);
        addText(17318, "protecting prayer is", 0xff981f, false, false, 0, tda, 0);
        addText(17319, "used.", 0xff981f, false, false, 0, tda, 0);
        addText(17320, "", 0xff981f, false, false, 0, tda, 0);
        addText(17321, "Item protecting prayers", 0xff981f, false, false, 0, tda, 0);
        addText(17322, "will allow you to keep", 0xff981f, false, false, 0, tda, 0);
        addText(17323, "one extra item.", 0xff981f, false, false, 0, tda, 0);
        addText(17324, "", 0xff981f, false, false, 0, tda, 0);
        addText(17325, "The items kept are", 0xff981f, false, false, 0, tda, 0);
        addText(17326, "selected by the server", 0xff981f, false, false, 0, tda, 0);
        addText(17327, "and include the most", 0xff981f, false, false, 0, tda, 0);
        addText(17328, "expensive items you're", 0xff981f, false, false, 0, tda, 0);
        addText(17329, "carrying.", 0xff981f, false, false, 0, tda, 0);
        addText(17330, "", 0xff981f, false, false, 0, tda, 0);
        RSinterface.parent = 17315;
        RSinterface.id = 17315;
        RSinterface.type = 0;
        RSinterface.atActionType = 0;
        RSinterface.contentType = 0;
        RSinterface.width = 130;
        RSinterface.height = 197;
        RSinterface.opacity = 0;
        RSinterface.hoverType = -1;
        RSinterface.scrollMax = 280;
        RSinterface.children = new int[20];
        RSinterface.childX = new int[20];
        RSinterface.childY = new int[20];
        RSinterface.children[0] = 17309;
        RSinterface.childX[0] = 0;
        RSinterface.childY[0] = 0;
        RSinterface.children[1] = 17310;
        RSinterface.childX[1] = 0;
        RSinterface.childY[1] = 12;
        RSinterface.children[2] = 17311;
        RSinterface.childX[2] = 0;
        RSinterface.childY[2] = 24;
        RSinterface.children[3] = 17312;
        RSinterface.childX[3] = 0;
        RSinterface.childY[3] = 36;
        RSinterface.children[4] = 17313;
        RSinterface.childX[4] = 0;
        RSinterface.childY[4] = 48;
        RSinterface.children[5] = 17314;
        RSinterface.childX[5] = 0;
        RSinterface.childY[5] = 60;
        RSinterface.children[6] = 17317;
        RSinterface.childX[6] = 0;
        RSinterface.childY[6] = 72;
        RSinterface.children[7] = 17318;
        RSinterface.childX[7] = 0;
        RSinterface.childY[7] = 84;
        RSinterface.children[8] = 17319;
        RSinterface.childX[8] = 0;
        RSinterface.childY[8] = 96;
        RSinterface.children[9] = 17320;
        RSinterface.childX[9] = 0;
        RSinterface.childY[9] = 108;
        RSinterface.children[10] = 17321;
        RSinterface.childX[10] = 0;
        RSinterface.childY[10] = 120;
        RSinterface.children[11] = 17322;
        RSinterface.childX[11] = 0;
        RSinterface.childY[11] = 132;
        RSinterface.children[12] = 17323;
        RSinterface.childX[12] = 0;
        RSinterface.childY[12] = 144;
        RSinterface.children[13] = 17324;
        RSinterface.childX[13] = 0;
        RSinterface.childY[13] = 156;
        RSinterface.children[14] = 17325;
        RSinterface.childX[14] = 0;
        RSinterface.childY[14] = 168;
        RSinterface.children[15] = 17326;
        RSinterface.childX[15] = 0;
        RSinterface.childY[15] = 180;
        RSinterface.children[16] = 17327;
        RSinterface.childX[16] = 0;
        RSinterface.childY[16] = 192;
        RSinterface.children[17] = 17328;
        RSinterface.childX[17] = 0;
        RSinterface.childY[17] = 204;
        RSinterface.children[18] = 17329;
        RSinterface.childX[18] = 0;
        RSinterface.childY[18] = 216;
        RSinterface.children[19] = 17330;
        RSinterface.childX[19] = 0;
        RSinterface.childY[19] = 228;
    }

    public static void clanChatSettings(GameFont[] tda) {
        Widget tab = addTabInterface(38128);
        Widget list = addTabInterface(38151);
        int yPos = 0;
        list.width = 277;
        list.height = 237;
        list.scrollMax = 238;
        addSprite(38129, 511);
        addHoverButton_sprite_loader(38130, 137, 16, 16, "Close", 250, 38131, 3);
        addHoveredButton_sprite_loader(38131, 138, 16, 16, 38126);
        addHoverButton_sprite_loader(38132, 512, 145, 45, "Set prefix", 250, 38133, 0);
        addHoveredButton_sprite_loader(38133, 513, 145, 45, 4300);

        addButton(38132, 37128, 145, 45, 512, 513, 38133, "Set prefix");
        addHoveredButton_sprite_loader(38133, 513, 145, 45, 36666);

        addHoverButton_sprite_loader(38136, 512, 145, 45, "Set permission", 250, 38137, 0);
        addHoveredButton_sprite_loader(38137, 513, 145, 45, 4300);
        addHoverButton_sprite_loader(38140, 512, 145, 45, "Set permission", 250, 38141, 0);
        addHoveredButton_sprite_loader(38141, 513, 145, 45, 4300);
        addHoverButton_sprite_loader(38144, 512, 145, 45, "Set permission", 250, 38145, 0);
        addHoveredButton_sprite_loader(38145, 513, 145, 45, 4300);
        addText(38146, "Right click on the", tda, 2, 0xffff9d, true, true);
        addText(38147, "player name to", tda, 2, 0xffff9d, true, true);
        addText(38148, "set rank", tda, 2, 0xffff9d, true, true);
        addText(38134, "Clan name:", tda, 0, 0xff9040, true, true);
        addText(38135, "Chat disabled", tda, 2, 0xffffff, true, true);
        addText(38138, "Who can enter chat?", tda, 0, 0xff9040, true, true);
        addText(38139, "Anyone", tda, 2, 0xffffff, true, true);
        addText(38142, "Who can talk on chat?", tda, 0, 0xff9040, true, true);
        addText(38143, "Anyone", tda, 2, 0xffffff, true, true);
        addText(38149, "Who can kick on chat?", tda, 0, 0xff9040, true, true);
        addText(38150, "Only me", tda, 2, 0xffffff, true, true);
        addButton(37445, 37128, 9, 7, 508, 508, 37446, "Filter");
        addHoveredButton_sprite_loader(37446, 509, 9, 7, 36667);
        addButton(37447, 37128, 9, 7, 508, 508, 37448, "Filter");
        addHoveredButton_sprite_loader(37448, 509, 9, 7, 36668);
        interfaceCache[38136].actions = new String[] {
                "Anyone",
                "Any friends",
                "Recruit+",
                "Corporal+",
                "Sergeant+",
                "Lieutenant+",
                "Captain+",
                "General+",
                "Only me"
        };
        interfaceCache[38140].actions = new String[] {
                "Anyone",
                "Any friends",
                "Recruit+",
                "Corporal+",
                "Sergeant+",
                "Lieutenant+",
                "Captain+",
                "General+",
                "Only me"
        };
        interfaceCache[38144].actions = new String[] {
                "Anyone",
                "Any friends",
                "Recruit+",
                "Corporal+",
                "Sergeant+",
                "Lieutenant+",
                "Captain+",
                "General+",
                "Only me"
        };
        list.totalChildren(200);
        for (int i = 38152; i <= 38252; i++) {
            addText(i, "", tda, 2, 0xffff64, false, true);
        }
        for (int j = 38254; j <= 38354; j++) {
            addText(j, "", tda, 2, 0xffffff, false, true);
        }
        for (int id = 38152, i = 0; id <= 38252 && i < 100; id++, i++) {
            interfaceCache[id].actions = new String[] {
                    "Recruit",
                    "Corporal",
                    "Sergeant",
                    "Lieutenant",
                    "Captain",
                    "General",
                    "Demote"
            };
            list.children[i] = id;
            list.childX[i] = 0;
            list.childY[i] = yPos;
            yPos += 14;
        }
        yPos = 0;
        for (int id = 38254, i = 100; id <= 38354 && i < 200; id++, i++) {
            list.children[i] = id;
            list.childX[i] = 173;
            list.childY[i] += yPos;
            yPos += 14;
        }
        tab.totalChildren(27);
        int tabChild = 0;
        tab.child(tabChild++, 38129, 15, 15);
        tab.child(tabChild++, 38151, 190, 71);
        tab.child(tabChild++, 38130, 468, 25);
        tab.child(tabChild++, 38131, 468, 25);
        tab.child(tabChild++, 38132, 25, 54);
        tab.child(tabChild++, 38133, 25, 54);
        tab.child(tabChild++, 38136, 25, 104);
        tab.child(tabChild++, 38137, 25, 104);
        tab.child(tabChild++, 38140, 25, 154);
        tab.child(tabChild++, 38141, 25, 154);
        tab.child(tabChild++, 38144, 25, 204);
        tab.child(tabChild++, 38145, 25, 204);
        tab.child(tabChild++, 38146, 97, 262);
        tab.child(tabChild++, 38147, 97, 274);
        tab.child(tabChild++, 38148, 97, 286);
        tab.child(tabChild++, 38134, 97, 63);
        tab.child(tabChild++, 38135, 97, 77);
        tab.child(tabChild++, 38138, 97, 113);
        tab.child(tabChild++, 38139, 97, 127);
        tab.child(tabChild++, 38142, 97, 163);
        tab.child(tabChild++, 38143, 97, 177);
        tab.child(tabChild++, 38149, 97, 213);
        tab.child(tabChild++, 38150, 97, 227);
        tab.child(tabChild++, 37445, 260, 60);
        tab.child(tabChild++, 37446, 260, 60);
        tab.child(tabChild++, 37447, 413, 60);
        tab.child(tabChild++, 37448, 413, 60);
    }

    public static void clanChatTab(GameFont[] tda) {
        Widget tab = addTabInterface(37128);
        int y = 0;

        addButton(37129, 37128, 72, 32, 194, 195, 37130, "Select");
        addHoveredButton_sprite_loader(37130, 195, 72, 32, 37131);

        addButton(37132, 37128, 72, 32, 194, 195, 37133, "Select");
        addHoveredButton_sprite_loader(37133, 195, 72, 32, 37134);

        // addButton(37250, 0, "/Clan Chat/Lootshare", "Toggle lootshare");
        addText(37135, "Join Chat", tda, 0, 0xff9b00, true, true);
        addText(37136, "Clan Setup", tda, 0, 0xff9b00, true, true);

        addSpriteLoader(37137, 196);

        addText(37138, "Clan Chat", tda, 2, 0xff9b00, true, true);
        addText(37139, "Talking in: N/A", tda, 0, 0xff9b00, false, true);
        addText(37140, "Owner: N/A", tda, 0, 0xff9b00, false, true);
        addButton(37445, 37128, 9, 7, 508, 508, 37446, "Filter");
        addHoveredButton_sprite_loader(37446, 509, 9, 7, 3744);
        addButton(37447, 37128, 9, 7, 508, 508, 37448, "Filter");
        addHoveredButton_sprite_loader(37448, 509, 9, 7, 3744);
        addButton(37449, 37128, 9, 7, 508, 508, 37450, "Filter");
        addHoveredButton_sprite_loader(37450, 509, 9, 7, 3744);
        addButton(37451, 37128, 9, 7, 508, 508, 37452, "Filter");
        addHoveredButton_sprite_loader(37452, 509, 9, 7, 3744);
        addButton(37453, 37128, 9, 7, 508, 508, 37454, "Filter");
        addHoveredButton_sprite_loader(37454, 509, 9, 7, 3744);
        tab.totalChildren(21);
        //tab.child(0, 16126, 0, 221);
        //tab.child(1, 16126, 0, 59);

        tab.child(0, 37137, 3, 55);
        tab.child(1, 37143, 0, 72);
        tab.child(2, 37129, 15, 226);
        tab.child(3, 37130, 15, 226);
        tab.child(4, 37132, 103, 226);
        tab.child(5, 37133, 103, 226);
        tab.child(6, 37135, 51, 237);
        tab.child(7, 37136, 139, 237);
        tab.child(8, 37138, 95, 4);
        tab.child(9, 37139, 10, 23);
        tab.child(10, 37140, 25, 38);
        tab.child(11, 37445, 14, 63);
        tab.child(12, 37446, 14, 63);
        tab.child(13, 37447, 45, 63);
        tab.child(14, 37448, 45, 63);
        tab.child(15, 37449, 90, 63);
        tab.child(16, 37450, 90, 63);
        tab.child(17, 37451, 135, 63);
        tab.child(18, 37452, 135, 63);
        tab.child(19, 37453, 166, 63);
        tab.child(20, 37454, 166, 63);
        /* Text area */
        Widget list = addTabInterface(37143);
        list.totalChildren(200);
        for (int i = 37144; i <= 37244; i++) {
            addText(i, "", tda, 0, 0xffffff, false, true);
        }
        for (int i = 37344; i <= 37444; i++) {
            addText(i, "", tda, 0, 0xffffff, false, true);
        }
        for (int id = 37344, i = 100; id < 37444 && i <= 199; id++, i++) {
            list.children[i] = id;
            list.childX[i] = 124;
            list.childY[i] = 3 + y;
            y += 14;
        }
        for (int id = 37144, i = 0; id <= 37243 && i <= 99; id++, i++) {
            interfaceCache[id].actions = new String[] {
                    "Kick User",
                    "Ban User"

            };
            interfaceCache[id].parent = 37128;
            list.children[i] = id;
            list.childX[i] = 13;
            for (int id2 = 37144, i2 = 1; id2 <= 37243 && i2 <= 99; id2++, i2++) {
                list.childY[0] = 3;
                list.childY[i2] = list.childY[i2 - 1] + 14;
            }
        }
        list.height = 145;
        list.width = 165;
        list.scrollMax = 145;
    }

    public static void addHoverText2(int id, String text, String[] tooltips, GameFont tda[], int idx, int color,
                                     boolean center, boolean textShadowed, int width) {
        Widget rsinterface = addInterface(id);
        rsinterface.id = id;
        rsinterface.parent = id;
        rsinterface.type = 4;
        rsinterface.atActionType = 1;
        rsinterface.width = width;
        rsinterface.height = 11;
        rsinterface.contentType = 0;
        rsinterface.opacity = 0;
        rsinterface.hoverType = -1;
        rsinterface.centerText = center;
        rsinterface.textShadow = textShadowed;
        rsinterface.textDrawingAreas = tda[idx];
        rsinterface.defaultText = text;
        rsinterface.secondaryText = "";
        rsinterface.textColor = color;
        rsinterface.secondaryColor = 0;
        rsinterface.defaultHoverColor = 0xffffff;
        rsinterface.secondaryHoverColor = 0;
        rsinterface.tooltips = tooltips;
    }

    public static void addText2(int id, String text, GameFont tda[], int idx, int color, boolean center,
                                boolean shadow) {
        Widget tab = addTabInterface(id);
        tab.parent = id;
        tab.id = id;
        tab.type = 4;
        tab.atActionType = 0;
        tab.width = 0;
        tab.height = 11;
        tab.contentType = 0;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.centerText = center;
        tab.textShadow = shadow;
        tab.textDrawingAreas = tda[idx];
        tab.defaultText = text;
        tab.secondaryText = "";
        tab.textColor = color;
        tab.secondaryColor = 0;
        tab.defaultHoverColor = 0;
        tab.secondaryHoverColor = 0;
    }

    public static void addAdvancedSprite(int id, int spriteId) {
        Widget widget = addInterface(id);
        widget.id = id;
        widget.parent = id;
        widget.type = 5;
        widget.atActionType = 0;
        widget.contentType = 0;
        widget.hoverType = 52;
        widget.enabledSprite = Client.cacheSprite[spriteId];
        widget.disabledSprite = Client.cacheSprite[spriteId];
        widget.drawsTransparent = true;
        widget.opacity = 64;
        widget.width = 512;
        widget.height = 334;
    }

    public static void addConfigSprite(int id, int spriteId, int spriteId2, int state, int config) {
        Widget widget = addTabInterface(id);
        widget.id = id;
        widget.parent = id;
        widget.type = 5;
        widget.atActionType = 0;
        widget.contentType = 0;
        widget.width = 512;
        widget.height = 334;
        widget.opacity = 0;
        widget.hoverType = -1;
        widget.valueCompareType = new int[1];
        widget.requiredValues = new int[1];
        widget.valueCompareType[0] = 1;
        widget.requiredValues[0] = state;
        widget.valueIndexArray = new int[1][3];
        widget.valueIndexArray[0][0] = 5;
        widget.valueIndexArray[0][1] = config;
        widget.valueIndexArray[0][2] = 0;
        widget.enabledSprite = spriteId < 0 ? null : Client.cacheSprite[spriteId];
        widget.disabledSprite = spriteId2 < 0 ? null : Client.cacheSprite[spriteId2];
    }

    public static void addGEBuySell(int id) {
        Widget widget = addTabInterface(id);
        widget.invisible = false;
        addSprite(id + 1, 582);
        addHoverButton_sprite_loader(id + 2, 585, 35, 35, "Buy", -1, id + 3, 1);
        addHoveredButton_sprite_loader2(id + 3, 586, 35, 35, id + 4);
        addHoverButton_sprite_loader(id + 5, 583, 35, 35, "Sell", -1, id + 6, 1);
        addHoveredButton_sprite_loader2(id + 6, 584, 35, 35, id + 7);
        widget.totalChildren(5);
        widget.child(0, id + 1, 0, 0);
        widget.child(1, id + 2, 13, 47);
        widget.child(2, id + 3, 13, 47);
        widget.child(3, id + 5, 68, 47);
        widget.child(4, id + 6, 68, 47);

    }
    public static void addName(int id, GameFont[] tda) {
        Widget widget = addTabInterface(id);
        addText(id + 1, "", tda, 0, 0xff981f, false, true);
        addText(id + 2, "", tda, 0, 0xff981f, false, true);
        addText(id + 3, "", tda, 0, 0xff981f, false, true);
        widget.totalChildren(3);
        widget.child(0, id + 1, 0, 0);
        widget.child(1, id + 2, 0, 10);
        widget.child(2, id + 3, 0, 20);
    }
    public static void addGESlot(int id, String status, GameFont[] tda) {
        Widget widget = addTabInterface(id);
        widget.invisible = true;
        addHoverButton_sprite_loader(id + 1, 587, 115, 110, "View offer", -1, id + 2, 1);
        addHoveredButton_sprite_loader2(id + 2, 588, 115, 110, id + 3);
        addText(id + 4, status, tda, 2, 0xff981f, true, true);
        addText(id + 5, "1 coin", tda, 0, 0xff981f, true, true);
        Widget rewardContainer = addTabInterface(id + 6);
        addName(id + 7, tda);

        rewardContainer.spritesX = new int[1];
        rewardContainer.spritesY = new int[1];
        rewardContainer.inventoryItemId = new int[1];
        rewardContainer.inventoryAmounts = new int[1];
        rewardContainer.filled = false;
        rewardContainer.replaceItems = false;
        rewardContainer.usableItems = false;
        rewardContainer.hasActions = false;
        rewardContainer.invisible = true;
        rewardContainer.allowSwapItems = false;
        rewardContainer.spritePaddingX = 0;
        rewardContainer.spritePaddingY = 0;
        rewardContainer.height = 1;
        rewardContainer.width = 1;
        rewardContainer.parent = 41002;
        rewardContainer.type = TYPE_INVENTORY;
        addPixels(id + 11, 0xd88020, 0, 13, 0, true, 105); //105 is max length
        addTransparentSprite(id + 12, 604, 50);
        widget.totalChildren(8);
        widget.child(0, id + 1, 0, 0);
        widget.child(1, id + 2, 0, 0);
        widget.child(2, id + 4, 55, 5);
        widget.child(3, id + 5, 55, 93);
        widget.child(4, id + 6, 7, 33);
        widget.child(5, id + 7, 47, 33);
        widget.child(6, id + 11, 5, 75);
        widget.child(7, id + 12, 5, 75);

    }

    public static void addSprite(int id, int spriteId) {
        Widget rsint = interfaceCache[id] = new Widget();
        rsint.id = id;
        rsint.parent = id;
        rsint.type = 5;
        rsint.atActionType = 0;
        rsint.contentType = 0;
        rsint.opacity = 0;
        rsint.hoverType = 0;

        if (spriteId != -1) {
            rsint.disabledSprite = Client.cacheSprite[spriteId];
            rsint.enabledSprite = Client.cacheSprite[spriteId];
        }

        rsint.width = 0;
        rsint.height = 0;
    }

    public static void addSpriteGlow(int id, int spriteId) {
        Widget rsint = interfaceCache[id] = new Widget();
        rsint.id = id;
        rsint.parent = id;
        rsint.type = 5;
        rsint.atActionType = 0;
        rsint.contentType = 0;
        rsint.opacity = 0;
        rsint.hoverType = 0;
        rsint.glowing = true;

        if (spriteId != -1) {
            rsint.disabledSprite = Client.cacheSprite[spriteId];
            rsint.enabledSprite = Client.cacheSprite[spriteId];
        }

        rsint.width = 0;
        rsint.height = 0;
    }

    public static void addText(int id, String text, GameFont wid[], int idx, int color) {
        Widget rsinterface = addTabInterface(id);
        rsinterface.id = id;
        rsinterface.parent = id;
        rsinterface.type = 4;
        rsinterface.atActionType = 0;
        rsinterface.width = 174;
        rsinterface.height = 11;
        rsinterface.contentType = 0;
        rsinterface.opacity = 0;
        rsinterface.centerText = false;
        rsinterface.textShadow = true;
        rsinterface.textDrawingAreas = wid[idx];
        rsinterface.defaultText = text;
        rsinterface.secondaryText = "";
        rsinterface.textColor = color;
        rsinterface.defaultHoverColor = 0;
        rsinterface.secondaryHoverColor = 0;
    }



    public static void addPixels(int id, int color, int width, int height, int alpha, boolean filled, int totalWidth) {
        Widget rsi = addInterface(id);
        rsi.type = 3;
        rsi.opacity = (byte)alpha;
        rsi.textColor = color;
        rsi.secondaryColor = color;
        rsi.defaultHoverColor = color;
        rsi.secondaryHoverColor = color;
        rsi.filled = filled;
        rsi.width = width;
        rsi.height = height;
        rsi.totalWidth = totalWidth;
    }


    public static void chatboxOptions2(GameFont[] tda) {
        Widget interface_ = addTabInterface(11000);
        addSprite(11001, 605);
        addSprite(11002, 606);
        addText(11003, "Text here", tda, 3, 0x800000, true, false);
        addHoverText3(11004, "Duel Arena", "Ok", tda, 3, 0x000000, true, false, 75, 0xffffff);
        addHoverText3(11005, "Al-kharid", "Ok", tda, 3, 0x000000, true, false, 75, 0xffffff);
        int chatboxChild = 0;
        interface_.totalChildren(5);
        interface_.child(chatboxChild++, 11001, 210, 5);
        interface_.child(chatboxChild++, 11002, 210, 5);
        interface_.child(chatboxChild++, 11003, 238, 1);
        interface_.child(chatboxChild++, 11004, 200, 32);
        interface_.child(chatboxChild++, 11005, 200, 65);
    }

//    public static void spinTab(GameFont[] wid) {
//        Widget list = addTabInterface(13000);
//        Widget items = addTabInterface(13100);
//        Widget reward = addTabInterface(13300);
//        reward.invisible = true;
//        addTransparentSprite(13301, 546, 90);
//        addSprite(13302, 547);
//        addText(13304, "You won:", wid, 2, 0xff9040, true, true);
//        addText(13305, "name_here", wid, 1, 0xff9040, true, true);
//        Widget rewardContainer = addTabInterface(13303);
//        rewardContainer.spritesX = new int[1];
//        rewardContainer.spritesY = new int[1];
//        rewardContainer.inventoryItemId = new int[1];
//        rewardContainer.inventoryAmounts = new int[1];
//        rewardContainer.filled = false;
//        rewardContainer.replaceItems = false;
//        rewardContainer.usableItems = false;
//        rewardContainer.hasActions = false;
//        rewardContainer.invisible = true;
//        rewardContainer.allowSwapItems = false;
//        rewardContainer.spritePaddingX = 0;
//        rewardContainer.spritePaddingY = 0;
//        rewardContainer.height = 1;
//        rewardContainer.width = 1;
//        rewardContainer.parent = 41002;
//        rewardContainer.type = TYPE_INVENTORY;
//        int rewardChild = 0;
//        reward.totalChildren(8);
//        reward.child(rewardChild++, 13301, 0, 0);
//        reward.child(rewardChild++, 13302, 140, 110);
//        reward.child(rewardChild++, 13003, 190, 42);
//        reward.child(rewardChild++, 13004, 190, 42);
//        reward.child(rewardChild++, 13007, 245, 47);
//        reward.child(rewardChild++, 13303, 230, 127);
//        reward.child(rewardChild++, 13304, 245, 172);
//        reward.child(rewardChild++, 13305, 244, 188);
//        items.width = 391;
//        addSprite(13001, 539);
//        addSprite(13002, 538);
//        addHoverButton_sprite_loader(13003, 542, 114, 25, "Spin", 250, 13004, 0);
//        addHoveredButton_sprite_loader(13004, 543, 114, 25, 4400);
//        addText(13007, "SPIN", wid, 2, 0xff9040, true, true);
//        addText(13009, "Spin to win!", wid, 2, 0xff9040, true, true);
//        addText(13010, "Possible rewards:", wid, 2, 0xff9040, true, true);
//        addHoverText3(13011, "<u>Buy more spins", "Go to website", wid, 0, 0xff9040, true, false, 75, 0xFFB885);
//        addSprite(13101, 541);
//        addSprite(13012, 544);
//        addText(13014, "New items in:", wid, 2, 0xff9040, true, true);
//        addText(13015, "24:00", wid, 0, 0xff9040, false, true);
//        addText(13016, "Spins left:", wid, 2, 0xff9040, true, true);
//        addText(13017, "100", wid, 0, 0xff9040, false, true);
//        addText(13018, "Total gp won:", wid, 2, 0xff9040, true, true);
//        addText(13019, "1.5b", wid, 0, 0xff9040, false, true);
//        Widget container = addTabInterface(13200);
//        container.spritesX = new int[300];
//        container.spritesY = new int[300];
//        container.inventoryItemId = new int[300];
//        container.inventoryAmounts = new int[300];
//        container.filled = false;
//        container.replaceItems = false;
//        container.usableItems = false;
//        container.hasActions = false;
//        container.invisible = true;
//        container.allowSwapItems = false;
//        container.spritePaddingX = 9;
//        container.spritePaddingY = 0;
//        container.height = 1;
//        container.width = 300;
//        container.parent = 41002;
//        container.type = TYPE_INVENTORY;
//        Widget possibleRewards = addTabInterface(13202);
//        possibleRewards.spritesX = new int[300];
//        possibleRewards.spritesY = new int[300];
//        possibleRewards.inventoryItemId = new int[300];
//        possibleRewards.inventoryAmounts = new int[300];
//        possibleRewards.filled = false;
//        possibleRewards.replaceItems = false;
//        possibleRewards.usableItems = false;
//        possibleRewards.hasActions = false;
//        possibleRewards.invisible = true;
//        possibleRewards.allowSwapItems = false;
//        possibleRewards.spritePaddingX = 9;
//        possibleRewards.spritePaddingY = 0;
//        possibleRewards.height = 20;
//        possibleRewards.width = 4;
//        possibleRewards.parent = 41002;
//        possibleRewards.type = TYPE_INVENTORY;
//        Widget rewardScroll = addTabInterface(13201);
//        rewardScroll.width = 161;
//        rewardScroll.height = 133;
//        rewardScroll.scrollMax = 455;
//        int rewardScrollChild = 0;
//        rewardScroll.totalChildren(1);
//        rewardScroll.child(rewardScrollChild, 13202, 0, 0);
//        int listChild = 0;
//        int itemChild = 0;
//        items.totalChildren(2);
//        items.child(itemChild++, 13101, 0, 0);
//        items.child(itemChild++, 13200, 3, 3);
//        list.totalChildren(18);
//        list.child(listChild++, 13001, 10, 10);
//        list.child(listChild++, 13003, 200, 52);
//        list.child(listChild++, 13004, 200, 52);
//        list.child(listChild++, 13007, 255, 57);
//        list.child(listChild++, 13009, 252, 20);
//        list.child(listChild++, 13100, 58, 86);
//        list.child(listChild++, 13002, 250, 83);
//        list.child(listChild++, 13010, 145, 130);
//        list.child(listChild++, 13201, 60, 152);
//        list.child(listChild++, 13011, 27, 21);
//        list.child(listChild++, 13012, 265, 170);
//        list.child(listChild++, 13014, 337, 180);
//        list.child(listChild++, 13015, 385, 183);
//        list.child(listChild++, 13016, 345, 203);
//        list.child(listChild++, 13017, 385, 206);
//        list.child(listChild++, 13018, 345, 226);
//        list.child(listChild++, 13019, 392, 229);
//        list.child(listChild++, 13300, 10, 10);
//    }

    public static void equipmentTab(GameFont[] wid) {
        Widget Interface = interfaceCache[1644];
        addSprite(15101, 0, "Interfaces/Equipment/bl");// cheap hax
        addSprite(15102, 1, "Interfaces/Equipment/bl");// cheap hax
        addSprite(15109, 2, "Interfaces/Equipment/bl");// cheap hax
        removeConfig(21338);
        removeConfig(21344);
        removeConfig(21342);
        removeConfig(21341);
        removeConfig(21340);
        removeConfig(15103);
        removeConfig(15104);
        // Interface.children[23] = 15101;
        // Interface.childX[23] = 40;
        // Interface.childY[23] = 205;
        Interface.children[24] = 15102;
        Interface.childX[24] = 110;
        Interface.childY[24] = 205;
        Interface.children[25] = 15109;
        Interface.childX[25] = 39;
        Interface.childY[25] = 240;
        Interface.children[26] = 27650;
        Interface.childX[26] = 0;
        Interface.childY[26] = 0;
        Interface = addInterface(27650);

        addHoverButton_sprite_loader(27651, 146, 40, 40, "Price-checker", -1, 27652, 1);
        addHoveredButton_sprite_loader(27652, 147, 40, 40, 27658);

        addHoverButton_sprite_loader(27653, 144, 40, 40, "Show Equipment Stats", -1, 27655, 1);
        addHoveredButton_sprite_loader(27655, 145, 40, 40, 27665);

        addHoverButton_sprite_loader(27654, 148, 40, 40, "Show items kept on death", -1, 27657, 1);
        addHoveredButton_sprite_loader(27657, 149, 40, 40, 27666);

        setChildren(6, Interface);
        setBounds(27651, 75, 205, 0, Interface);
        setBounds(27652, 75, 205, 1, Interface);
        setBounds(27653, 23, 205, 2, Interface);
        setBounds(27654, 127, 205, 3, Interface);
        setBounds(27655, 23, 205, 4, Interface);
        setBounds(27657, 127, 205, 5, Interface);
    }

    public static void removeConfig(int id) {
        @SuppressWarnings("unused")
        Widget rsi = interfaceCache[id] = new Widget();
    }

    public static void addTitle(int id, String text, GameFont tda[], int idx, int color, boolean center,
                                boolean shadow, boolean fancy) {
        Widget tab = addTabInterface(id);
        tab.parent = id;
        tab.id = id;
        tab.type = 4;
        tab.atActionType = 0;
        tab.width = 0;
        tab.height = 11;
        tab.contentType = 0;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.centerText = center;
        tab.textShadow = shadow;
        tab.textDrawingAreas = tda[idx];
        tab.defaultText = text;
        tab.secondaryText = "";
        tab.textColor = color;
        tab.secondaryColor = 0;
        tab.defaultHoverColor = 0;
        tab.secondaryHoverColor = 0;
        tab.fancy = fancy;

    }
    public static void addHoverText3(int id, String text, String tooltip, GameFont tda[], int idx, int color, boolean center, boolean textShadow, int width, int hoverColor) {
        Widget rsinterface = addInterface(id);
        rsinterface.id = id;
        rsinterface.parent = id;
        rsinterface.type = 4;
        rsinterface.atActionType = 1;
        rsinterface.width = width;
        rsinterface.height = 11;
        rsinterface.contentType = 0;
        rsinterface.opacity = 0;
        rsinterface.hoverType = -1;
        rsinterface.centerText = center;
        rsinterface.textShadow = textShadow;
        rsinterface.textDrawingAreas = tda[idx];
        rsinterface.defaultText = text;
        rsinterface.secondaryText = "";
        rsinterface.tooltip = tooltip;
        rsinterface.textColor = color;
        rsinterface.secondaryColor = 0;
        rsinterface.defaultHoverColor = hoverColor;
        rsinterface.secondaryHoverColor = 0;
    }

    public static void lobby(GameFont[] tda) {
        Widget lobby = addTabInterface(26100);
        lobby.width = 512;
        lobby.height = 334;
        lobby.type = 0;
        addSprite(26101, 518);
        addSprite(26102, 519);
        addHoverButton_sprite_loader(26103, 520, 507, 39, "Play now", 250, 26104, 3);
        addHoveredButton_sprite_loader(26104, 521, 507, 39, 26099);
        addText(26105, "Welcome to Mystic", tda, 1, 0xffffff, true, true);
        addText(26106, "You last logged in today from: 127.0.0.1", tda, 1, 0xffffff, true, true);
        addText(26108, "LATEST UPDATES", tda, 2, 0xffffff, true, true);
        addText(26109, "Clan chat, OSRS login, make-all and Autochat!", tda, 1, 0xffffff, true, true);
        addText(26110, "MESSAGE OF THE WEEK", tda, 3, 0x000000, true, false);
        addText(26111, "Join our discord <u>here</u> to keep up to date with all the updates", tda, 3, 0x000000, true, false);
        addText(26112, "and chat with the community!", tda, 3, 0x000000, true, false);
        addSprite(26107, 535);

        int lobbychild = 0;
        lobby.totalChildren(12);
        lobby.child(lobbychild++, 26101, 0, 0);
        lobby.child(lobbychild++, 26102,  0, 0);
        lobby.child(lobbychild++, 26103, 130, 430);
        lobby.child(lobbychild++, 26104,  130, 430);
        lobby.child(lobbychild++, 26105, 380, 60);
        lobby.child(lobbychild++, 26106,  380, 75);
        lobby.child(lobbychild++, 26107, 85, 140);
        lobby.child(lobbychild++, 26108, 380, 103);
        lobby.child(lobbychild++, 26109, 380, 118);
        lobby.child(lobbychild++, 26110, 380, 315);
        lobby.child(lobbychild++, 26111, 385, 340);
        lobby.child(lobbychild++, 26112, 385, 355);
    }

    public static void geSearch(GameFont[] tda) { // FIXME hovers going outside bounds
        Widget geSearch = addTabInterface(40500);
        addText(40501, "", tda, 2, 0x446320a, true, false);
        addSprite(40502, 536);
        Widget noMatch = addTabInterface(41000);
        addText(41001, "Start typing the name of an item to search for it.", tda, 1, 0x000000, true, false);
        Widget items = addTabInterface(41002);

        int y = 0;
        int x = 1;
        int xCount = 0;

        items.totalChildren(2101);
        items.invisible = true;
        for(int i = 41003, id = 0, itemSelect = 0; id < 700; i+=4, id++, itemSelect++) {
            if(xCount > 2) {
                xCount = 0;
                x = 1;
                y += 32;
                addSelection(i, tda, items, x, y, itemSelect);
            } else {
                addSelection(i, tda, items, x, y, itemSelect);
            }

            x += 161;
            xCount++;
        }
        Widget container = addTabInterface(33000);
        container.spritesX = new int[300];
        container.spritesY = new int[300];
        container.inventoryItemId = new int[300];
        container.inventoryAmounts = new int[300];
        container.filled = false;
        container.replaceItems = false;
        container.usableItems = false;
        container.hasActions = false;
        container.invisible = true;
        container.allowSwapItems = false;
        container.spritePaddingX = 130;
        container.spritePaddingY = 0;
        container.height = 100;
        container.width = 3;
        container.parent = 41002;
        container.type = TYPE_INVENTORY;
        container.drawNumber  = true;

        items.child(parentchilds++, 33000, 1, 0);


        noMatch.totalChildren(1);
        int noMatchChild = 0;
        noMatch.child(noMatchChild++, 41001, 255, 75);
        geSearch.totalChildren(4);
        int scrollChild = 0;
        geSearch.child(scrollChild++, 40501, 255, 8);
        geSearch.child(scrollChild++, 40502, 7, 27);
        geSearch.child(scrollChild++, 41002, 9, 29);
        geSearch.child(scrollChild++, 41000, 0, 0);

        items.width = 485;
        items.height = 104;
        items.scrollMax = 1200;

    }
    public static void skillGuide(GameFont[] tda) {

        Widget mainScroll = addTabInterface(36100);
        Widget list = addTabInterface(36350);
        Widget side = addTabInterface(36150);
        int pos = 36151;
        int y = 0;
        side.height = 75;
        side.width = 260;
        list.height = 237;
        list.width = 284;
        list.scrollMax = 1750;
        addSprite(36101, 496);
        addSprite(36351, 497);
        addSprite(36352, 498);
        addButton(36104, 36100, 24, 24, 501, 501, 36105, "Close");
        addHoveredButton_sprite_loader(36105, 502, 24, 24, 36106);
        addHoverText3(36353, "Weapons", "Open", tda, 3, 0x46320a, true, false, 150, 0x645028);
        addHoverText3(36354, "Armour", "Open", tda, 3, 0x46320a, true, false, 150, 0x645028);
        addHoverText3(36355, "Salamanders", "Open", tda, 3, 0x46320a, true, false, 150, 0x645028);
        addTitle(36102, "Attack", tda, 3, 0x46320a, true, false, true);
        addText(36103, "Weapons", tda, 0, 0x446320a, true, false);
        Widget container = addTabInterface(36250);
        container.spritesX = new int[20];
        container.spritesY = new int[20];
        container.inventoryItemId = new int[58];
        container.inventoryAmounts = new int[58];
        container.filled = false;
        container.replaceItems = false;
        container.usableItems = false;
        container.hasActions = false;
        container.invisible = true;
        container.allowSwapItems = false;
        container.spritePaddingX = 0;
        container.spritePaddingY = 3;
        container.height = 58;
        container.width = 1;
        container.parent = 42000;
        container.type = TYPE_INVENTORY;
        for(int i = 0; i < 200; i++) {
            addText(pos + i, "" + i, tda, 1, 0x46320a, true, false);
            addText(pos + (i + 1), "" + (i + 1), tda, 1, 0x46320a, false, false);
            addText(pos + (i + 2), "" + (i + 2), tda, 1, 0x46320a, false, false);
            i += 3;
        }
        mainScroll.totalChildren(7);
        side.totalChildren(5);
        list.totalChildren(451);
        int scrollChild = 0;
        mainScroll.child(scrollChild++, 36101, 0, 0);
        mainScroll.child(scrollChild++, 36104, 470, 5);
        mainScroll.child(scrollChild++, 36105, 470, 5);
        mainScroll.child(scrollChild++, 36102, 180, 15);
        mainScroll.child(scrollChild++, 36103, 180, 32);
        mainScroll.child(scrollChild++, 36150, 355, 35);
        mainScroll.child(scrollChild++, 36350, 30, 75);
        int sideChild = 0;
        side.child(sideChild++, 36351, 0, 0);
        side.child(sideChild++, 36352, 0, 66);
        side.child(sideChild++, 36353, 0, 10);
        side.child(sideChild++, 36354, 0, 28);
        side.child(sideChild++, 36355, 0, 46);
        int listChild = 0;
        list.child(listChild++, 36250, 23, 0);
        for(int i = 0; i < 600; i++) {
            list.child(listChild++, pos + i, 11, y + 2);
            list.child(listChild++, pos + (i + 1), 61, y);
            list.child(listChild++, pos + (i + 2), 61, y + 13);
            y += 35;
            i += 3;
        }
    }

    public static void equipmentScreen(GameFont[] wid) {
        Widget Interface = Widget.interfaceCache[1644];
        addButton(19144, 140, "Show Equipment Stats");
        removeSomething(19145);
        removeSomething(19146);
        removeSomething(19147);
        // setBounds(19144, 21, 210, 23, Interface);
        setBounds(19145, 40, 210, 24, Interface);
        setBounds(19146, 40, 210, 25, Interface);
        setBounds(19147, 40, 210, 26, Interface);
        Widget tab = addTabInterface(15106);
        addSpriteLoader(15107, 141);

        addHoverButton_sprite_loader(15210, 142, 21, 21, "Close", 250, 15211, 3);
        addHoveredButton_sprite_loader(15211, 143, 21, 21, 15212);

        addText(15111, "Equip Your Character...", wid, 2, 0xe4a146, false, true);
        addText(15112, "Attack bonus", wid, 2, 0xe4a146, false, true);
        addText(15113, "Defence bonus", wid, 2, 0xe4a146, false, true);
        addText(15114, "Other bonuses", wid, 2, 0xe4a146, false, true);

        addText(15115, "Melee maxhit: 1", wid, 1, 0xe4a146, false, true);
        addText(15116, "Ranged maxhit: 1", wid, 1, 0xe4a146, false, true);
        addText(15117, "Magic maxhit: 1", wid, 1, 0xe4a146, false, true);

        for (int i = 1675; i <= 1684; i++) {
            textSize(i, wid, 1);
        }
        textSize(1686, wid, 1);
        textSize(1687, wid, 1);
        addChar(15125);
        tab.totalChildren(47);
        tab.child(0, 15107, 4, 20);
        tab.child(1, 15210, 476, 29);
        tab.child(2, 15211, 476, 29);
        tab.child(3, 15111, 14, 30);
        int Child = 4;
        int Y = 69;
        for (int i = 1675; i <= 1679; i++) {
            tab.child(Child, i, 20, Y);
            Child++;
            Y += 14;
        }
        tab.child(9, 1680, 20, 161);
        tab.child(10, 1681, 20, 177);
        tab.child(11, 1682, 20, 192);
        tab.child(12, 1683, 20, 207);
        tab.child(13, 1684, 20, 221);
        tab.child(14, 1686, 20, 262);
        tab.child(15, 15125, 170, 200);
        tab.child(16, 15112, 16, 55);
        tab.child(17, 1687, 20, 276);
        tab.child(18, 15113, 16, 147);
        tab.child(19, 15114, 16, 248);
        tab.child(20, 1645, 104 + 295, 149 - 52);
        tab.child(21, 1646, 399, 163);
        tab.child(22, 1647, 399, 163);
        tab.child(23, 1648, 399, 58 + 146);
        tab.child(24, 1649, 26 + 22 + 297 - 2, 110 - 44 + 118 - 13 + 5);
        tab.child(25, 1650, 321 + 22, 58 + 154);
        tab.child(26, 1651, 321 + 134, 58 + 118);
        tab.child(27, 1652, 321 + 134, 58 + 154);
        tab.child(28, 1653, 321 + 48, 58 + 81);
        tab.child(29, 1654, 321 + 107, 58 + 81);
        tab.child(30, 1655, 321 + 58, 58 + 42);
        tab.child(31, 1656, 321 + 112, 58 + 41);
        tab.child(32, 1657, 321 + 78, 58 + 4);
        tab.child(33, 1658, 321 + 37, 58 + 43);
        tab.child(34, 1659, 321 + 78, 58 + 43);
        tab.child(35, 1660, 321 + 119, 58 + 43);
        tab.child(36, 1661, 321 + 22, 58 + 82);
        tab.child(37, 1662, 321 + 78, 58 + 82);
        tab.child(38, 1663, 321 + 134, 58 + 82);
        tab.child(39, 1664, 321 + 78, 58 + 122);
        tab.child(40, 1665, 321 + 78, 58 + 162);
        tab.child(41, 1666, 321 + 22, 58 + 162);
        tab.child(42, 1667, 321 + 134, 58 + 162);
        tab.child(43, 1688, 50 + 297 - 2, 110 - 13 + 5);

        //Maxhits
        tab.child(44, 15115, 370, 260);
        tab.child(45, 15116, 370, 275);
        tab.child(46, 15117, 370, 290);

        for (int i = 1675; i <= 1684; i++) {
            Widget rsi = interfaceCache[i];
            rsi.textColor = 0xe4a146;
            rsi.centerText = false;
        }
        for (int i = 1686; i <= 1687; i++) {
            Widget rsi = interfaceCache[i];
            rsi.textColor = 0xe4a146;
            rsi.centerText = false;
        }
    }

    public static void addChar(int ID) {
        Widget t = interfaceCache[ID] = new Widget();
        t.id = ID;
        t.parent = ID;
        t.type = 6;
        t.atActionType = 0;
        t.contentType = 328;
        t.width = 136;
        t.height = 168;
        t.opacity = 0;
        t.modelZoom = 560;
        t.modelRotation1 = 150;
        t.modelRotation2 = 0;
        t.defaultAnimationId = -1;
        t.secondaryAnimationId = -1;
    }

    public static void addModel(int ID) {
        Widget t = interfaceCache[ID] = new Widget();
        t.id = ID;
        t.parent = ID;
        t.type = 6;
        t.atActionType = 0;
        t.contentType = 0;
        t.width = 1;
        t.height = 1;
        t.opacity = 0;
        t.verticalOffset -= 20;
    }

    public static void addButton(int id, int sid, String tooltip) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 1;
        tab.contentType = 0;
        tab.opacity = (byte) 0;
        tab.hoverType = 52;
        tab.disabledSprite = Client.cacheSprite[sid];// imageLoader(sid, spriteName);
        tab.enabledSprite = Client.cacheSprite[sid];//imageLoader(sid, spriteName);
        tab.width = tab.disabledSprite.myWidth;
        tab.height = tab.enabledSprite.myHeight;
        tab.tooltip = tooltip;
    }

    public static void addTooltipBox(int id, String text) {
        Widget rsi = addInterface(id);
        rsi.id = id;
        rsi.parent = id;
        rsi.type = 8;
        rsi.defaultText = text;
    }

    public static void addTooltip(int id, String text) {
        Widget rsi = addInterface(id);
        rsi.id = id;
        rsi.type = 0;
        rsi.invisible = true;
        rsi.hoverType = -1;
        addTooltipBox(id + 1, text);
        rsi.totalChildren(1);
        rsi.child(0, id + 1, 0, 0);
    }

    public static Widget addInterface(int id) {
        Widget rsi = interfaceCache[id] = new Widget();
        rsi.id = id;
        rsi.parent = id;
        rsi.width = 512;
        rsi.height = 334;
        return rsi;
    }

    public static void addText(int id, String text, GameFont tda[], int idx, int color, boolean centered) {
        Widget rsi = interfaceCache[id] = new Widget();
        if (centered)
            rsi.centerText = true;
        rsi.textShadow = true;
        rsi.textDrawingAreas = tda[idx];
        rsi.defaultText = text;
        rsi.textColor = color;
        rsi.id = id;
        rsi.type = 4;
    }

    public static void textColor(int id, int color) {
        Widget rsi = interfaceCache[id];
        rsi.textColor = color;
    }

    public static void textSize(int id, GameFont tda[], int idx) {
        Widget rsi = interfaceCache[id];
        rsi.textDrawingAreas = tda[idx];
    }

    public static void addCacheSprite(int id, int sprite1, int sprite2, String sprites) {
        Widget rsi = interfaceCache[id] = new Widget();
        rsi.disabledSprite = getSprite(sprite1, interfaceLoader, sprites);
        rsi.enabledSprite = getSprite(sprite2, interfaceLoader, sprites);
        rsi.parent = id;
        rsi.id = id;
        rsi.type = 5;
    }

    public static void sprite1(int id, int sprite) {
        Widget class9 = interfaceCache[id];
        class9.disabledSprite = Client.cacheSprite[sprite];
    }

    public static void addActionButton(int id, int sprite, int sprite2, int width, int height, String s) {
        Widget rsi = interfaceCache[id] = new Widget();
        rsi.disabledSprite = Client.cacheSprite[sprite];
        if (sprite2 == sprite)
            rsi.enabledSprite = Client.cacheSprite[sprite];
        else
            rsi.enabledSprite = Client.cacheSprite[sprite2];
        rsi.tooltip = s;
        rsi.contentType = 0;
        rsi.atActionType = 1;
        rsi.width = width;
        rsi.hoverType = 52;
        rsi.parent = id;
        rsi.id = id;
        rsi.type = 5;
        rsi.height = height;
    }

    public static void addToggleButton(int id, int sprite, int setconfig, int width, int height, String s) {
        Widget rsi = addInterface(id);
        rsi.disabledSprite = Client.cacheSprite[sprite];
        rsi.enabledSprite = Client.cacheSprite[sprite];
        rsi.requiredValues = new int[1];
        rsi.requiredValues[0] = 1;
        rsi.valueCompareType = new int[1];
        rsi.valueCompareType[0] = 1;
        rsi.valueIndexArray = new int[1][3];
        rsi.valueIndexArray[0][0] = 5;
        rsi.valueIndexArray[0][1] = setconfig;
        rsi.valueIndexArray[0][2] = 0;
        rsi.atActionType = 4;
        rsi.width = width;
        rsi.hoverType = -1;
        rsi.parent = id;
        rsi.id = id;
        rsi.type = 5;
        rsi.height = height;
        rsi.tooltip = s;
    }

    public void totalChildren(int id, int x, int y) {
        children = new int[id];
        childX = new int[x];
        childY = new int[y];
    }

    public static void removeSomething(int id) {
        @SuppressWarnings("unused")
        Widget rsi = interfaceCache[id] = new Widget();
    }

    public static void quickPrayers(GameFont[] TDA) {
        int frame = 0;
        Widget tab = addTabInterface(17200);

        addTransparentSprite(17235, 131, 50);
        addSpriteLoader(17201, 132);
        addText(17231, "Select your quick prayers below.", TDA, 0, 0xFF981F, false, true);

        int child = 17202;
        int config = 620;
        for (int i = 0; i < 29; i++) {
            addConfigButton(child++, 17200, 134, 133, 14, 15, "Select", 0, 1, config++);
        }

        addHoverButton_sprite_loader(17232, 135, 190, 24, "Confirm Selection", -1, 17233, 1);
        addHoveredButton_sprite_loader(17233, 136, 190, 24, 17234);

        setChildren(64, tab);//
        setBounds(5632, 5, 8 + 20, frame++, tab);
        setBounds(5633, 44, 8 + 20, frame++, tab);
        setBounds(5634, 79, 11 + 20, frame++, tab);
        setBounds(19813, 116, 10 + 20, frame++, tab);
        setBounds(19815, 153, 9 + 20, frame++, tab);
        setBounds(5635, 5, 48 + 20, frame++, tab);
        setBounds(5636, 44, 47 + 20, frame++, tab);
        setBounds(5637, 79, 49 + 20, frame++, tab);
        setBounds(5638, 116, 50 + 20, frame++, tab);
        setBounds(5639, 154, 50 + 20, frame++, tab);
        setBounds(5640, 4, 84 + 20, frame++, tab);
        setBounds(19817, 44, 87 + 20, frame++, tab);
        setBounds(19820, 81, 85 + 20, frame++, tab);
        setBounds(5641, 117, 85 + 20, frame++, tab);
        setBounds(5642, 156, 87 + 20, frame++, tab);
        setBounds(5643, 5, 125 + 20, frame++, tab);
        setBounds(5644, 43, 124 + 20, frame++, tab);
        setBounds(13984, 83, 124 + 20, frame++, tab);
        setBounds(5645, 115, 121 + 20, frame++, tab);
        setBounds(19822, 154, 124 + 20, frame++, tab);
        setBounds(19824, 5, 160 + 20, frame++, tab);
        setBounds(5649, 41, 158 + 20, frame++, tab);
        setBounds(5647, 79, 163 + 20, frame++, tab);
        setBounds(5648, 116, 158 + 20, frame++, tab);

        //Preserve
        setBounds(28002, 157, 160 + 20, frame++, tab);

        //Chivarly
        setBounds(19826, 10, 208 , frame++, tab);

        //Piety
        setBounds(19828, 45, 207 + 13, frame++, tab);

        //Rigour
        setBounds(28005, 85, 210, frame++, tab);

        //Augury
        setBounds(28008, 124, 210, frame++, tab);

        setBounds(17235, 0, 25, frame++, tab);// Faded backing
        setBounds(17201, 0, 22, frame++, tab);// Split
        setBounds(17201, 0, 237, frame++, tab);// Split

        setBounds(17202, 5 - 3, 8 + 17, frame++, tab);
        setBounds(17203, 44 - 3, 8 + 17, frame++, tab);
        setBounds(17204, 79 - 3, 8 + 17, frame++, tab);
        setBounds(17205, 116 - 3, 8 + 17, frame++, tab);
        setBounds(17206, 153 - 3, 8 + 17, frame++, tab);
        setBounds(17207, 5 - 3, 48 + 17, frame++, tab);
        setBounds(17208, 44 - 3, 48 + 17, frame++, tab);
        setBounds(17209, 79 - 3, 48 + 17, frame++, tab);
        setBounds(17210, 116 - 3, 48 + 17, frame++, tab);
        setBounds(17211, 153 - 3, 48 + 17, frame++, tab);
        setBounds(17212, 5 - 3, 85 + 17, frame++, tab);
        setBounds(17213, 44 - 3, 85 + 17, frame++, tab);
        setBounds(17214, 79 - 3, 85 + 17, frame++, tab);
        setBounds(17215, 116 - 3, 85 + 17, frame++, tab);
        setBounds(17216, 153 - 3, 85 + 17, frame++, tab);
        setBounds(17217, 5 - 3, 124 + 17, frame++, tab);
        setBounds(17218, 44 - 3, 124 + 17, frame++, tab);
        setBounds(17219, 79 - 3, 124 + 17, frame++, tab);
        setBounds(17220, 116 - 3, 124 + 17, frame++, tab);
        setBounds(17221, 153 - 3, 124 + 17, frame++, tab);
        setBounds(17222, 5 - 3, 160 + 17, frame++, tab);
        setBounds(17223, 44 - 3, 160 + 17, frame++, tab);
        setBounds(17224, 79 - 3, 160 + 17, frame++, tab);
        setBounds(17225, 116 - 3, 160 + 17, frame++, tab);
        setBounds(17226, 153 - 3, 160 + 17, frame++, tab);

        setBounds(17227, 1, 207 + 4, frame++, tab); //Chivalry toggle button
        setBounds(17228, 41, 207 + 4, frame++, tab); //Piety toggle button
        setBounds(17229, 77, 207 + 4, frame++, tab); //Rigour toggle button
        setBounds(17230, 116, 207 + 4, frame++, tab); //Augury toggle button

        setBounds(17231, 5, 5, frame++, tab);// text
        setBounds(17232, 0, 237, frame++, tab);// confirm
        setBounds(17233, 0, 237, frame++, tab);// Confirm hover
    }

    public int transparency = 255;

    private static void addTransparentSprite(int id, int spriteId, int transparency) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 0;
        tab.contentType = 0;
        tab.transparency = transparency;
        tab.hoverType = 52;
        tab.disabledSprite = Client.cacheSprite[spriteId];
        tab.enabledSprite = Client.cacheSprite[spriteId];
        tab.width = 512;
        tab.height = 334;
        tab.drawsTransparent = true;
    }

    private static void addTransparentSpriteGlow(int id, int spriteId, int transparency) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 0;
        tab.contentType = 0;
        tab.glowing = true;
        tab.transparency = transparency;
        tab.hoverType = 52;
        tab.disabledSprite = Client.cacheSprite[spriteId];
        tab.enabledSprite = Client.cacheSprite[spriteId];
        tab.width = 512;
        tab.height = 334;
        tab.drawsTransparent = true;
    }

    public static void Pestpanel(GameFont[] tda) {
        Widget RSinterface = addInterface(21119);
        addText(21120, "What", 0x999999, false, true, 52, tda, 1);
        addText(21121, "What", 0x33cc00, false, true, 52, tda, 1);
        addText(21122, "(Need 5 to 25 players)", 0xFFcc33, false, true, 52, tda, 1);
        addText(21123, "Points", 0x33ccff, false, true, 52, tda, 1);
        int last = 4;
        RSinterface.children = new int[last];
        RSinterface.childX = new int[last];
        RSinterface.childY = new int[last];
        setBounds(21120, 15, 12, 0, RSinterface);
        setBounds(21121, 15, 30, 1, RSinterface);
        setBounds(21122, 15, 48, 2, RSinterface);
        setBounds(21123, 15, 66, 3, RSinterface);
    }
    public static void geInterfaceMain(GameFont[] tda) {
        Widget widget = addInterface(25000);
        addSprite(25001, 581);
        addGEBuySell(25002);
        addGEBuySell(25012);
        addGEBuySell(25022);
        addGEBuySell(25032);
        addGEBuySell(25042);
        addGEBuySell(25052);
        addGEBuySell(25062);
        addGEBuySell(25072);
        addGESlot(25082, "Buy", tda);
        addGESlot(25095, "Buy", tda);
        addGESlot(25108, "Buy", tda);
        addGESlot(25121, "Buy", tda);
        addGESlot(25134, "Buy", tda);
        addGESlot(25147, "Buy", tda);
        addGESlot(25160, "Buy", tda);
        addGESlot(25173, "Buy", tda);
        int widgetChild = 0;
        widget.totalChildren(17);
        widget.child(widgetChild++, 25001, 10, 10);
        widget.child(widgetChild++, 25002, 19, 75);
        widget.child(widgetChild++, 25012, 136, 75);
        widget.child(widgetChild++, 25022, 253, 75);
        widget.child(widgetChild++, 25032, 370, 75);
        widget.child(widgetChild++, 25042, 19, 193);
        widget.child(widgetChild++, 25052, 136, 193);
        widget.child(widgetChild++, 25062, 253, 193);
        widget.child(widgetChild++, 25072, 370, 193);
        widget.child(widgetChild++, 25082, 19, 75);
        widget.child(widgetChild++, 25095, 136, 75);
        widget.child(widgetChild++, 25108, 253, 75);
        widget.child(widgetChild++, 25121, 370, 75);
        widget.child(widgetChild++, 25134, 19, 193);
        widget.child(widgetChild++, 25147, 136, 193);
        widget.child(widgetChild++, 25160, 253, 193);
        widget.child(widgetChild++, 25173, 370, 193);
        geBuyInterface(tda);
        geSellInterface(tda);
    }
    public static void geBuyInterface(GameFont[] tda) {
        Widget widget = addInterface(25500);
        addSprite(25501, 589);
        addButton(25502, 25500, 40, 36, 578, 578, 52002, "Search for item");
        addTransparentSpriteGlow(25503, 576, 0);
        addSprite(25504, 577);
        addText(25505, "Choose an item...", tda, 2, 0xff981f, false, true);
        addText(25506, "Click the icon on the left to search for items.", tda, 0, 0xffb83f, false, true);
        addHoverButton_sprite_loader(25507, 595, 20, 18, "-1", -1, 25508, 1);
        addHoveredButton_sprite_loader(25508, 597, 20, 18, 25509);
        addHoverButton_sprite_loader(25510, 596, 20, 18, "+1", -1, 25511, 1);
        addHoveredButton_sprite_loader(25511, 598, 20, 18, 25512);
        addHoverButton_sprite_loader(25513, 595, 20, 18, "-1", -1, 25514, 1);
        addHoveredButton_sprite_loader(25514, 597, 20, 18, 25515);
        addHoverButton_sprite_loader(25516, 596, 20, 18, "+1", -1, 25517, 1);
        addHoveredButton_sprite_loader(25517, 598, 20, 18, 25518);
        addHoverButton_sprite_loader(25519, 590, 35, 35, "+1", -1, 25520, 1);
        addHoveredButton_sprite_loader(25520, 591, 35, 35, 25521);
        addHoverButton_sprite_loader(25522, 590, 35, 35, "+10", -1, 25523, 1);
        addHoveredButton_sprite_loader(25523, 591, 35, 35, 25524);
        addHoverButton_sprite_loader(25525, 590, 35, 35, "+100", -1, 25526, 1);
        addHoveredButton_sprite_loader(25526, 591, 35, 35, 25527);
        addHoverButton_sprite_loader(25528, 590, 35, 35, "+1K", -1, 25529, 1);
        addHoveredButton_sprite_loader(25529, 591, 35, 35, 25530);
        addHoverButton_sprite_loader(25531, 590, 35, 35, "...", -1, 25532, 1);
        addHoveredButton_sprite_loader(25532, 591, 35, 35, 25533);
        addText(25534, "+1", tda, 0, 0xff981f, true, true);
        addText(25535, "+10", tda, 0, 0xff981f, true, true);
        addText(25536, "+100", tda, 0, 0xff981f, true, true);
        addText(25537, "+1k", tda, 0, 0xff981f, true, true);
        addText(25538, "...", tda, 0, 0xff981f, true, true);
        addHoverButton_sprite_loader(25539, 590, 35, 35, "-5%", -1, 25540, 1);
        addHoveredButton_sprite_loader(25540, 591, 35, 35, 25541);
        addHoverButton_sprite_loader(25542, 590, 35, 35, "Guide price", -1, 25543, 1);
        addHoveredButton_sprite_loader(25543, 591, 35, 35, 25544);
        addHoverButton_sprite_loader(25545, 590, 35, 35, "Enter price", -1, 25546, 1);
        addHoveredButton_sprite_loader(25546, 591, 35, 35, 25547);
        addHoverButton_sprite_loader(25548, 590, 35, 35, "+5%", -1, 25549, 1);
        addHoveredButton_sprite_loader(25549, 591, 35, 35, 25550);
        addSprite(25551, 593);
        addSprite(25552, 592);
        addText(25553, "...", tda, 0, 0xff981f, true, true);
        addSprite(25554, 594);
        addButton(25555, 25500, 152, 40, 599, 599, 25556, "Confirm");
        addHoverButton_sprite_loader(25557, 600, 152, 40, "Confirm", -1, 25558, 1);
        addHoveredButton_sprite_loader(25558, 601, 152, 40, 25559);
        Widget rewardContainer = addTabInterface(25560);
        rewardContainer.spritesX = new int[1];
        rewardContainer.spritesY = new int[1];
        rewardContainer.inventoryItemId = new int[1];
        rewardContainer.inventoryAmounts = new int[1];
        rewardContainer.filled = false;
        rewardContainer.replaceItems = false;
        rewardContainer.usableItems = false;
        rewardContainer.hasActions = false;
        rewardContainer.invisible = true;
        rewardContainer.allowSwapItems = false;
        rewardContainer.spritePaddingX = 0;
        rewardContainer.spritePaddingY = 0;
        rewardContainer.height = 1;
        rewardContainer.width = 1;
        rewardContainer.parent = 25500;
        rewardContainer.type = TYPE_INVENTORY;
        rewardContainer.drawNumber = true;
        addText(25561, "", tda, 0, 0xff981f, true, true); //guide price
        addText(25562, "", tda, 0, 0xff981f, true, true); //quantity
        addText(25563, "", tda, 0, 0xff981f, true, true); //price per item
        addText(25564, "", tda, 0, 0xffffff, true, true); // total price
        int widgetChild = 0;
        widget.totalChildren(49);
        widget.child(widgetChild++, 25501, 10, 10);
        widget.child(widgetChild++, 25502, 86, 81);
        widget.child(widgetChild++, 25503, 86, 81);
        widget.child(widgetChild++, 25560, 90, 84);
        widget.child(widgetChild++, 25504, 89, 84);
        widget.child(widgetChild++, 25505, 190, 55);
        widget.child(widgetChild++, 25506, 190, 75);
        widget.child(widgetChild++, 25507, 38, 169);
        widget.child(widgetChild++, 25508, 38, 169);
        widget.child(widgetChild++, 25510, 224, 169);
        widget.child(widgetChild++, 25511, 224, 169);
        widget.child(widgetChild++, 25513, 260, 169);
        widget.child(widgetChild++, 25514, 260, 169);
        widget.child(widgetChild++, 25516, 444, 169);
        widget.child(widgetChild++, 25517, 444, 169);
        widget.child(widgetChild++, 25519, 43, 185);
        widget.child(widgetChild++, 25520, 43, 185);
        widget.child(widgetChild++, 25522, 84, 185);
        widget.child(widgetChild++, 25523, 84, 185);
        widget.child(widgetChild++, 25525, 125, 185);
        widget.child(widgetChild++, 25526, 125, 185);
        widget.child(widgetChild++, 25528, 166, 185);
        widget.child(widgetChild++, 25529, 166, 185);
        widget.child(widgetChild++, 25531, 207, 185);
        widget.child(widgetChild++, 25532, 207, 185);
        widget.child(widgetChild++, 25534, 59, 196);
        widget.child(widgetChild++, 25535, 100, 196);
        widget.child(widgetChild++, 25536, 141, 196);
        widget.child(widgetChild++, 25537, 182, 196);
        widget.child(widgetChild++, 25538, 223, 196);
        widget.child(widgetChild++, 25539, 268, 185);
        widget.child(widgetChild++, 25540, 268, 185);
        widget.child(widgetChild++, 25542, 326, 185);
        widget.child(widgetChild++, 25543, 326, 185);
        widget.child(widgetChild++, 25545, 366, 185);
        widget.child(widgetChild++, 25546, 366, 185);
        widget.child(widgetChild++, 25548, 424, 185);
        widget.child(widgetChild++, 25549, 424, 185);
        widget.child(widgetChild++, 25551, 276, 193);
        widget.child(widgetChild++, 25552, 334, 193);
        widget.child(widgetChild++, 25553, 383, 197);
        widget.child(widgetChild++, 25554, 432, 193);
        widget.child(widgetChild++, 25555, 175, 258);
        widget.child(widgetChild++, 25557, 175, 258);
        widget.child(widgetChild++, 25558, 175, 258);
        widget.child(widgetChild++, 25561, 108, 125);
        widget.child(widgetChild++, 25562, 140, 173);
        widget.child(widgetChild++, 25563, 362, 173);
        widget.child(widgetChild++, 25564, 249, 226);
        Widget.interfaceCache[25557].drawingDisabled = true;
        Widget.interfaceCache[25558].drawingDisabled = true;
        Widget.interfaceCache[25557].tooltip = "";
        Widget.interfaceCache[25558].tooltip = "";
    }
    public static void geSellInterface(GameFont[] tda) {
        Widget widget = addInterface(26000);
        addSprite(26001, 603);
        addSprite(26002, 578);
        addText(25505, "Choose an item...", tda, 2, 0xff981f, false, true);
        addText(25506, "Choose an item from your inventory to sell.", tda, 0, 0xffb83f, false, true);
        addHoverButton_sprite_loader(25507, 595, 20, 18, "-1", -1, 25508, 1);
        addHoveredButton_sprite_loader(25508, 597, 20, 18, 25509);
        addHoverButton_sprite_loader(25510, 596, 20, 18, "+1", -1, 25511, 1);
        addHoveredButton_sprite_loader(25511, 598, 20, 18, 25512);
        addHoverButton_sprite_loader(25513, 595, 20, 18, "-1", -1, 25514, 1);
        addHoveredButton_sprite_loader(25514, 597, 20, 18, 25515);
        addHoverButton_sprite_loader(25516, 596, 20, 18, "+1", -1, 25517, 1);
        addHoveredButton_sprite_loader(25517, 598, 20, 18, 25518);
        addHoverButton_sprite_loader(25519, 590, 35, 35, "+1", -1, 25520, 1);
        addHoveredButton_sprite_loader(25520, 591, 35, 35, 25521);
        addHoverButton_sprite_loader(25522, 590, 35, 35, "+10", -1, 25523, 1);
        addHoveredButton_sprite_loader(25523, 591, 35, 35, 25524);
        addHoverButton_sprite_loader(25525, 590, 35, 35, "+100", -1, 25526, 1);
        addHoveredButton_sprite_loader(25526, 591, 35, 35, 25527);
        addHoverButton_sprite_loader(25528, 590, 35, 35, "All", -1, 25529, 1);
        addHoveredButton_sprite_loader(25529, 591, 35, 35, 25530);
        addHoverButton_sprite_loader(25531, 590, 35, 35, "...", -1, 25532, 1);
        addHoveredButton_sprite_loader(25532, 591, 35, 35, 25533);
        addText(25534, "+1", tda, 0, 0xff981f, true, true);
        addText(25535, "+10", tda, 0, 0xff981f, true, true);
        addText(25536, "+100", tda, 0, 0xff981f, true, true);
        addText(26037, "All", tda, 0, 0xff981f, true, true);
        addText(25538, "...", tda, 0, 0xff981f, true, true);
        addHoverButton_sprite_loader(25539, 590, 35, 35, "-5%", -1, 25540, 1);
        addHoveredButton_sprite_loader(25540, 591, 35, 35, 25541);
        addHoverButton_sprite_loader(25542, 590, 35, 35, "Guide price", -1, 25543, 1);
        addHoveredButton_sprite_loader(25543, 591, 35, 35, 25544);
        addHoverButton_sprite_loader(25545, 590, 35, 35, "Enter price", -1, 25546, 1);
        addHoveredButton_sprite_loader(25546, 591, 35, 35, 25547);
        addHoverButton_sprite_loader(25548, 590, 35, 35, "+5%", -1, 25549, 1);
        addHoveredButton_sprite_loader(25549, 591, 35, 35, 25550);
        addSprite(25551, 593);
        addSprite(25552, 592);
        addText(25553, "...", tda, 0, 0xff981f, true, true);
        addSprite(25554, 594);
        addButton(25555, 25500, 152, 40, 599, 599, 25556, "Confirm");
        addHoverButton_sprite_loader(25557, 600, 152, 40, "Confirm", -1, 25558, 1);
        addHoveredButton_sprite_loader(25558, 601, 152, 40, 25559);
        Widget rewardContainer = addTabInterface(25560);
        rewardContainer.spritesX = new int[1];
        rewardContainer.spritesY = new int[1];
        rewardContainer.inventoryItemId = new int[1];
        rewardContainer.inventoryAmounts = new int[1];
        rewardContainer.filled = false;
        rewardContainer.replaceItems = false;
        rewardContainer.usableItems = false;
        rewardContainer.hasActions = false;
        rewardContainer.invisible = true;
        rewardContainer.allowSwapItems = false;
        rewardContainer.spritePaddingX = 0;
        rewardContainer.spritePaddingY = 0;
        rewardContainer.height = 1;
        rewardContainer.width = 1;
        rewardContainer.parent = 25500;
        rewardContainer.type = TYPE_INVENTORY;
        rewardContainer.drawNumber = true;
        addText(25561, "", tda, 0, 0xff981f, true, true); //guide price
        addText(25562, "", tda, 0, 0xff981f, true, true); //quantity
        addText(25563, "", tda, 0, 0xff981f, true, true); //price per item
        addText(25564, "", tda, 0, 0xffffff, true, true); // total price
        int widgetChild = 0;
        widget.totalChildren(47);
        widget.child(widgetChild++, 26001, 10, 10);
        widget.child(widgetChild++, 26002, 86, 81);
        widget.child(widgetChild++, 25560, 90, 84);
        widget.child(widgetChild++, 25505, 190, 55);
        widget.child(widgetChild++, 25506, 190, 75);
        widget.child(widgetChild++, 25507, 38, 169);
        widget.child(widgetChild++, 25508, 38, 169);
        widget.child(widgetChild++, 25510, 224, 169);
        widget.child(widgetChild++, 25511, 224, 169);
        widget.child(widgetChild++, 25513, 260, 169);
        widget.child(widgetChild++, 25514, 260, 169);
        widget.child(widgetChild++, 25516, 444, 169);
        widget.child(widgetChild++, 25517, 444, 169);
        widget.child(widgetChild++, 25519, 43, 185);
        widget.child(widgetChild++, 25520, 43, 185);
        widget.child(widgetChild++, 25522, 84, 185);
        widget.child(widgetChild++, 25523, 84, 185);
        widget.child(widgetChild++, 25525, 125, 185);
        widget.child(widgetChild++, 25526, 125, 185);
        widget.child(widgetChild++, 25528, 166, 185);
        widget.child(widgetChild++, 25529, 166, 185);
        widget.child(widgetChild++, 25531, 207, 185);
        widget.child(widgetChild++, 25532, 207, 185);
        widget.child(widgetChild++, 25534, 59, 196);
        widget.child(widgetChild++, 25535, 100, 196);
        widget.child(widgetChild++, 25536, 141, 196);
        widget.child(widgetChild++, 26037, 182, 196);
        widget.child(widgetChild++, 25538, 223, 196);
        widget.child(widgetChild++, 25539, 268, 185);
        widget.child(widgetChild++, 25540, 268, 185);
        widget.child(widgetChild++, 25542, 326, 185);
        widget.child(widgetChild++, 25543, 326, 185);
        widget.child(widgetChild++, 25545, 366, 185);
        widget.child(widgetChild++, 25546, 366, 185);
        widget.child(widgetChild++, 25548, 424, 185);
        widget.child(widgetChild++, 25549, 424, 185);
        widget.child(widgetChild++, 25551, 276, 193);
        widget.child(widgetChild++, 25552, 334, 193);
        widget.child(widgetChild++, 25553, 383, 197);
        widget.child(widgetChild++, 25554, 432, 193);
        widget.child(widgetChild++, 25555, 175, 258);
        widget.child(widgetChild++, 25557, 175, 258);
        widget.child(widgetChild++, 25558, 175, 258);
        widget.child(widgetChild++, 25561, 108, 125);
        widget.child(widgetChild++, 25562, 140, 173);
        widget.child(widgetChild++, 25563, 362, 173);
        widget.child(widgetChild++, 25564, 249, 226);
        Widget.interfaceCache[25557].drawingDisabled = true;
        Widget.interfaceCache[25558].drawingDisabled = true;
        Widget.interfaceCache[25557].tooltip = "";
        Widget.interfaceCache[25558].tooltip = "";
    }

    public static void PlayerPanel(GameFont[] tda) {
        Widget widget = addInterface(23100);
        addSprite(23101, 581);
        addText(23102, "Players profile", tda, 2, 0xff981f, true, true);
        addHoverButton_sprite_loader(23103, 24, 16, 16, "Close", -1, 23104, 1);
        addHoveredButton_sprite_loader(23104, 25, 16, 16, 23105);
        addSprite(23106, 583);
        addHoverButton_sprite_loader(23107, 582, 113, 18, "Select", -1, 23108, 1);
        addHoveredButton_sprite_loader(23108, 583, 113, 18, 23109);
        addHoverButton_sprite_loader(23110, 582, 113, 18, "Select", -1, 23111, 1);
        addHoveredButton_sprite_loader(23111, 583, 113, 18, 23112);
        addText(23113, "Player Rank", tda, 1, 0xff981f, true, true);
        addText(23114, "PvP Statistics", tda, 1, 0xff981f, true, true);
        addText(23115, "PvM Statistics", tda, 1, 0xff981f, true, true);
        int widgetChild = 0;
        widget.totalChildren(12);
        widget.child(widgetChild++, 23101, 10, 10);
        widget.child(widgetChild++, 23102, 250, 20);
        widget.child(widgetChild++, 23103, 460, 20);
        widget.child(widgetChild++, 23104, 460, 20);
        widget.child(widgetChild++, 23106, 28, 53);
        widget.child(widgetChild++, 23107, 133, 53);
        widget.child(widgetChild++, 23108, 133, 53);
        widget.child(widgetChild++, 23110, 238, 53);
        widget.child(widgetChild++, 23111, 238, 53);
        widget.child(widgetChild++, 23113, 78, 55);
        widget.child(widgetChild++, 23114, 183, 55);
        widget.child(widgetChild++, 23115, 288, 55);

    }

    public static void Pestpanel2(GameFont[] tda) {
        Widget RSinterface = addInterface(21100);
        addSprite(21101, 0, "Pest Control/PEST1");
        addSprite(21102, 1, "Pest Control/PEST1");
        addSprite(21103, 2, "Pest Control/PEST1");
        addSprite(21104, 3, "Pest Control/PEST1");
        addSprite(21105, 4, "Pest Control/PEST1");
        addSprite(21106, 5, "Pest Control/PEST1");
        addText(21107, "", 0xCC00CC, false, true, 52, tda, 1);
        addText(21108, "", 0x0000FF, false, true, 52, tda, 1);
        addText(21109, "", 0xFFFF44, false, true, 52, tda, 1);
        addText(21110, "", 0xCC0000, false, true, 52, tda, 1);
        addText(21111, "250", 0x99FF33, false, true, 52, tda, 1);// w purp
        addText(21112, "250", 0x99FF33, false, true, 52, tda, 1);// e blue
        addText(21113, "250", 0x99FF33, false, true, 52, tda, 1);// se yel
        addText(21114, "250", 0x99FF33, false, true, 52, tda, 1);// sw red
        addText(21115, "200", 0x99FF33, false, true, 52, tda, 1);// attacks
        addText(21116, "0", 0x99FF33, false, true, 52, tda, 1);// knights hp
        addText(21117, "Time Remaining:", 0xFFFFFF, false, true, 52, tda, 0);
        addText(21118, "", 0xFFFFFF, false, true, 52, tda, 0);
        int last = 18;
        RSinterface.children = new int[last];
        RSinterface.childX = new int[last];
        RSinterface.childY = new int[last];
        setBounds(21101, 361, 26, 0, RSinterface);
        setBounds(21102, 396, 26, 1, RSinterface);
        setBounds(21103, 436, 26, 2, RSinterface);
        setBounds(21104, 474, 26, 3, RSinterface);
        setBounds(21105, 3, 21, 4, RSinterface);
        setBounds(21106, 3, 50, 5, RSinterface);
        setBounds(21107, 371, 60, 6, RSinterface);
        setBounds(21108, 409, 60, 7, RSinterface);
        setBounds(21109, 443, 60, 8, RSinterface);
        setBounds(21110, 479, 60, 9, RSinterface);
        setBounds(21111, 362, 10, 10, RSinterface);
        setBounds(21112, 398, 10, 11, RSinterface);
        setBounds(21113, 436, 10, 12, RSinterface);
        setBounds(21114, 475, 10, 13, RSinterface);
        setBounds(21115, 32, 32, 14, RSinterface);
        setBounds(21116, 32, 62, 15, RSinterface);
        setBounds(21117, 8, 88, 16, RSinterface);
        setBounds(21118, 87, 88, 17, RSinterface);
    }

    public String hoverText;
    public boolean isHovered = false;

    public static void addHoverBox(int id, int ParentID, String text, String text2, int configId, int configFrame) {
        Widget rsi = addTabInterface(id);
        rsi.id = id;
        rsi.parent = ParentID;
        rsi.type = 8;
        rsi.secondaryText = text;
        rsi.defaultText = text2;
        rsi.valueCompareType = new int[1];
        rsi.requiredValues = new int[1];
        rsi.valueCompareType[0] = 1;
        rsi.requiredValues[0] = configId;
        rsi.valueIndexArray = new int[1][3];
        rsi.valueIndexArray[0][0] = 5;
        rsi.valueIndexArray[0][1] = configFrame;
        rsi.valueIndexArray[0][2] = 0;
    }

    public DropdownMenu dropDown;
    public boolean hovered = false;
    public Widget dropDownOpen;
    public int dropDownHover = -1;
    public boolean geSearchButton = false;

    //0xFD961E

    public static void addDropdownMenu(int identification, int width, int defaultOption, boolean split, boolean center, Dropdown dropdown, String... options) {
        addDropdownMenu(identification, width, defaultOption, 0xFD961E, split, center, dropdown, options);
    }

    public static void addDropdownMenu(int identification, int width, int defaultOption, int textColor, boolean split, boolean center, Dropdown dropdown, String... options) {
        Widget dropdownMenu = addInterface(identification);
        dropdownMenu.type = 69;
        dropdownMenu.atActionType = 69;
        dropdownMenu.centerText = center;
        dropdownMenu.textColor = textColor;
        dropdownMenu.disabledMessage = "";
        dropdownMenu.dropDown = new DropdownMenu(width, defaultOption, dropdown, options);
    }

    public static void addItemOnInterface(int childId, int interfaceId, String[] options) {
        Widget rsi = interfaceCache[childId] = new Widget();
        rsi.actions = new String[5];
        rsi.spritesX = new int[20];
        rsi.inventoryItemId = new int[30];
        rsi.inventoryAmounts = new int[30];
        rsi.spritesY = new int[20];
        rsi.children = new int[0];
        rsi.childX = new int[0];
        rsi.childY = new int[0];
        for (int i = 0; i < rsi.actions.length; i++) {
            if (i < options.length) {
                if (options[i] != null) {
                    rsi.actions[i] = options[i];
                }
            }
        }
        rsi.centerText = true;
        rsi.filled = false;
        rsi.replaceItems = false;
        rsi.usableItems = false;
        //rsi.isInventoryInterface = false;
        rsi.allowSwapItems = false;
        rsi.spritePaddingX = 4;
        rsi.spritePaddingY = 5;
        rsi.height = 1;
        rsi.width = 1;
        rsi.parent = interfaceId;
        rsi.id = childId;
        rsi.type = TYPE_INVENTORY;
    }

    public static void addText(int id, String text, GameFont tda[], int idx, int color, boolean center,
                               boolean shadow) {
        Widget tab = addTabInterface(id);
        tab.parent = id;
        tab.id = id;
        tab.type = 4;
        tab.atActionType = 0;
        tab.width = 0;
        tab.height = 11;
        tab.contentType = 0;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.centerText = center;
        tab.textShadow = shadow;
        tab.textDrawingAreas = tda[idx];
        tab.defaultText = text;
        tab.secondaryText = "";
        tab.textColor = color;
        tab.secondaryColor = 0;
        tab.defaultHoverColor = 0;
        tab.secondaryHoverColor = 0;
    }

    public static void addText(int i, String s, int k, boolean l, boolean m, int a, GameFont[] TDA, int j) {
        Widget RSInterface = addInterface(i);
        RSInterface.parent = i;
        RSInterface.id = i;
        RSInterface.type = 4;
        RSInterface.atActionType = 0;
        RSInterface.width = 0;
        RSInterface.height = 0;
        RSInterface.contentType = 0;
        RSInterface.opacity = 0;
        RSInterface.hoverType = a;
        RSInterface.centerText = l;
        RSInterface.textShadow = m;
        RSInterface.textDrawingAreas = TDA[j];
        RSInterface.defaultText = s;
        RSInterface.secondaryText = "";
        RSInterface.textColor = k;
    }

    public static void addConfigButton(int ID, int pID, int bID, int bID2, int width, int height,
                                       String tT, int configID, int aT, int configFrame) {
        Widget Tab = addTabInterface(ID);
        Tab.parent = pID;
        Tab.id = ID;
        Tab.type = 5;
        Tab.atActionType = aT;
        Tab.contentType = 0;
        Tab.width = width;
        Tab.height = height;
        Tab.opacity = 0;
        Tab.hoverType = -1;
        Tab.valueCompareType = new int[1];
        Tab.requiredValues = new int[1];
        Tab.valueCompareType[0] = 1;
        Tab.requiredValues[0] = configID;
        Tab.valueIndexArray = new int[1][3];
        Tab.valueIndexArray[0][0] = 5;
        Tab.valueIndexArray[0][1] = configFrame;
        Tab.valueIndexArray[0][2] = 0;
        Tab.disabledSprite = Client.cacheSprite[bID];//imageLoader(bID, bName);
        Tab.enabledSprite = Client.cacheSprite[bID2];
        Tab.tooltip = tT;
    }

    public static void addSprite(int id, int spriteId, String spriteName) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 0;
        tab.contentType = 0;
        tab.opacity = (byte) 0;
        tab.hoverType = 52;
        tab.disabledSprite = imageLoader(spriteId, spriteName);
        tab.enabledSprite = imageLoader(spriteId, spriteName);
        tab.width = 512;
        tab.height = 334;
    }

    public static void addSprite(int id, int spriteId, String spriteName, int opacity) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 0;
        tab.contentType = 0;
        tab.opacity = (byte) 0;
        tab.hoverType = 52;
        tab.disabledSprite = imageLoader(spriteId, spriteName);
        tab.enabledSprite = imageLoader(spriteId, spriteName);
        tab.width = 512;
        tab.height = 334;
    }

    public static void addHoverButton(int i, String imageName, int j, int width, int height, String text,
                                      int contentType, int hoverOver, int aT) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.id = i;
        tab.parent = i;
        tab.type = 5;
        tab.atActionType = aT;
        tab.contentType = contentType;
        tab.opacity = 0;
        tab.hoverType = hoverOver;
        tab.disabledSprite = imageLoader(j, imageName);
        tab.enabledSprite = imageLoader(j, imageName);
        tab.width = width;
        tab.height = height;
        tab.tooltip = text;
    }

    public static void addHoverText(int id, String text, String tooltip, GameFont tda[], int idx, int color, boolean center, boolean textShadow, int width) {
        Widget rsinterface = addInterface(id);
        rsinterface.id = id;
        rsinterface.parent = id;
        rsinterface.type = 4;
        rsinterface.atActionType = 1;
        rsinterface.width = width;
        rsinterface.height = 11;
        rsinterface.contentType = 0;
        rsinterface.opacity = 0;
        rsinterface.hoverType = -1;
        rsinterface.centerText = center;
        rsinterface.textShadow = textShadow;
        rsinterface.textDrawingAreas = tda[idx];
        rsinterface.defaultText = text;
        rsinterface.secondaryText = "";
        rsinterface.tooltip = tooltip;
        rsinterface.textColor = color;
        rsinterface.secondaryColor = 0;
        rsinterface.defaultHoverColor = 0xFFFFFF;
        rsinterface.secondaryHoverColor = 0;
    }

    private static void bankInterface(GameFont[] tda) {
        Widget bank = addInterface(5292);

        setChildren(47, bank);

        int id = 50000;
        int child = 0;

        Sprite disabled = Client.cacheSprite[129];
        Sprite enabled = Client.cacheSprite[130];
        ///Sprite button1 = getSprite(0, interfaceLoader, "miscgraphics");
        //Sprite button2 = getSprite(9, interfaceLoader, "miscgraphics");

        addSprite(id, 106);
        addHoverButton_sprite_loader(id + 1, 107, 17, 17, "Close", -1, id + 2, 1);
        addHoveredButton_sprite_loader(id + 2, 108, 17, 17, id + 3);

        bank.child(child++, id, 12, 2);
        bank.child(child++, id + 1, 472, 9);
        bank.child(child++, id + 2, 472, 9);

        addHoverButton_sprite_loader(id + 4, 117, 32, 32, "Deposit Inventory", -1, id + 5, 1);
        addHoveredButton_sprite_loader(id + 5, 118, 32, 32, id + 6);

        addHoverButton_sprite_loader(id + 7, 119, 32, 32, "Deposit Equipment", -1, id + 8, 1);
        addHoveredButton_sprite_loader(id + 8, 120, 32, 32, id + 9);

        addHoverButtonWConfig(id + 10, 115, 116, 32, 32, "Search", -1, id + 11, 1, 117, 117);
        addHoveredButton_sprite_loader(id + 11, 116, 32, 32, id + 12);


        bank.child(child++, id + 4, 415, 292);
        bank.child(child++, id + 5, 415, 292);
        bank.child(child++, id + 7, 455, 292);
        bank.child(child++, id + 8, 455, 292);
        bank.child(child++, id + 10, 375, 292);
        bank.child(child++, id + 11, 375, 292);

        addButton(id + 13, getSprite(0, interfaceLoader, "miscgraphics3"), getSprite(0, interfaceLoader, "miscgraphics3"), "Show menu", 25, 25);
        addSprite(id + 14, 209);
        bank.child(child++, id + 13, 463, 43);
        bank.child(child++, id + 14, 463, 44);

        //Text
        addText(id + 53, "%1", tda, 0, 0xFE9624, true);
        Widget line = addInterface(id + 54);
        line.type = 3;
        line.allowSwapItems = true;
        line.width = 14;
        line.height = 1;
        line.textColor = 0xFE9624;
        addText(id + 55, "352", tda, 0, 0xFE9624, true);
        bank.child(child++, id + 53, 30, 8);
        bank.child(child++, id + 54, 24, 19);
        bank.child(child++, id + 55, 30, 20);

        bank.child(child++, 5383, 190, 12);
        bank.child(child++, 5385, 0, 79);
        bank.child(child++, 8131, 102, 306);
        bank.child(child++, 8130, 17, 306);
        bank.child(child++, 5386, 282, 306);
        bank.child(child++, 5387, 197, 306);
        bank.child(child++, 8132, 127, 309);
        bank.child(child++, 8133, 45, 309);
        bank.child(child++, 5390, 54, 291);
        bank.child(child++, 5389, 227, 309);
        bank.child(child++, 5391, 311, 309);
        bank.child(child++, 5388, 248, 291);

        id = 50070;
        for (int tab = 0, counter = 0; tab <= 36; tab += 4, counter++) {

            //	addHoverButton_sprite_loader(id + 1 + tab, 206, 39, 40, null, -1, id + 2 + tab, 1);
            //	addHoveredButton_sprite_loader(id + 2 + tab, 207, 39, 40, id + 3 + tab);


            int[] requiredValues = new int[]{1};
            int[] valueCompareType = new int[]{1};
            int[][] valueIndexArray = new int[1][3];
            valueIndexArray[0][0] = 5;
            valueIndexArray[0][1] = 1000 + counter; //Config
            valueIndexArray[0][2] = 0;


            addHoverConfigButton(id + tab, id + 1 + tab, 206, -1, 39, 40, null, valueCompareType, requiredValues, valueIndexArray);
            addHoveredConfigButton(interfaceCache[id + tab], id + 1 + tab, id + 2 + tab, 207, -1);


            //addHoverButtonWConfig(id + 1 + tab, 206, -1, 39, 40, null, -1, id + 2 + tab, 1, 1, 1000+counter);
            //addHoveredButton_sprite_loader(id + 2 + tab, 207, 39, 40, id + 3 + tab);

            interfaceCache[id + tab].actions = new String[]{"Select", tab == 0 ? null : "Collapse", null, null, null};
            interfaceCache[id + tab].parent = id;
            interfaceCache[id + tab].drawingDisabled = true;
            interfaceCache[id + 1 + tab].parent = id;
            bank.child(child++, id + tab, 19 + 40 * (tab / 4), 37);
            bank.child(child++, id + 1 + tab, 19 + 40 * (tab / 4), 37);
        }

        interfaceCache[5385].height = 206;
        interfaceCache[5385].width = 474;

        int[] interfaces = new int[] { 5386, 5387, 8130, 8131 };

        for (int rsint : interfaces) {
            interfaceCache[rsint].disabledSprite = disabled;
            interfaceCache[rsint].enabledSprite = enabled;
            interfaceCache[rsint].width = enabled.myWidth;
            interfaceCache[rsint].height = enabled.myHeight;
        }

        addSprite(50040, 208);
        bank.child(child++, 50040, 20, 41);


        final Widget scrollBar = Widget.interfaceCache[5385];
        scrollBar.totalChildren(Client.MAX_BANK_TABS);
        for(int i = 0; i < Client.MAX_BANK_TABS; i++) {
            addBankTabContainer(50300 + i, 109, 10, 35, 352, new String[] { "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X", null, "Withdraw-All but one" });
            scrollBar.child(i, 50300 + i, 40, 0);
        }
    }


    public static void addHoverText(int id, String text, String tooltip, GameFont tda[], int idx, int color, boolean center, boolean textShadow, int width, int hoveredColor) {
        Widget rsinterface = addInterface(id);
        rsinterface.id = id;
        rsinterface.parent = id;
        rsinterface.type = 4;
        rsinterface.atActionType = 1;
        rsinterface.width = width;
        rsinterface.height = 13;
        rsinterface.contentType = 0;
        rsinterface.opacity = 0;
        rsinterface.hoverType = -1;
        rsinterface.centerText = center;
        rsinterface.textShadow = textShadow;
        rsinterface.textDrawingAreas = tda[idx];
        rsinterface.defaultText = text;
        rsinterface.secondaryText = "";
        rsinterface.textColor = color;
        rsinterface.secondaryColor = 0;
        rsinterface.defaultHoverColor = 0xffffff;
        rsinterface.secondaryHoverColor = 0;
        rsinterface.tooltip = tooltip;
    }


    /**
     * Bank settings
     * @param t
     */
    public static void bankSettings(GameFont[] t) {
        Widget tab = addInterface(32500);
        addSprite(32501, 229);
        addText(32502, ""+Configuration.CLIENT_NAME+" Bank Settings", 0xff9933, true, true, -1, t, 2);

        addHoverButton_sprite_loader(32503, 107, 21, 21, "Close", -1, 32504, 1);
        addHoveredButton_sprite_loader(32504, 108, 21, 21, 32505);

        addConfigButton(32506, 32500, 230, 231, 14, 15, "Select", 0, 5, 1111);
        addConfigButton(32507, 32500, 230, 231, 14, 15, "Select", 1, 5, 1111);
        addConfigButton(32508, 32500, 230, 231, 14, 15, "Select", 2, 5, 1111);

        addText(32509, "First item in tab", 0xff9933, true, true, -1, t, 1);
        addText(32510, "Digit (1, 2, 3)", 0xff9933, true, true, -1, t, 1);
        addText(32511, "Roman numeral (I, II, III)", 0xff9933, true, true, -1, t, 1);
        addHoverText(32512, "Back to bank", "View", t, 1, 0xcc8000, true, true, 100, 0xFFFFFF);
        tab.totalChildren(11);
        tab.child(0, 32501, 115, 35);
        tab.child(1, 32502, 263, 44);
        tab.child(2, 32503, 373, 42);
        tab.child(3, 32504, 373, 42);
        tab.child(4, 32506, 150, 65 + 30);
        tab.child(5, 32507, 150, 65 + 60);
        tab.child(6, 32508, 150, 65 + 90);
        tab.child(7, 32509, 218, 65 + 30);
        tab.child(8, 32510, 210, 65 + 60);
        tab.child(9, 32511, 239, 65 + 90);
        tab.child(10, 32512, 275, 265);
    }

    public static void addHoveredConfigButton(Widget original, int ID, int IMAGEID, int disabledID, int enabledID) {
        Widget rsint = addTabInterface(ID);
        rsint.parent = original.id;
        rsint.id = ID;
        rsint.type = 0;
        rsint.atActionType = 0;
        rsint.contentType = 0;
        rsint.width = original.width;
        rsint.height = original.height;
        rsint.opacity = 0;
        rsint.hoverType = -1;
        Widget hover = addInterface(IMAGEID);
        hover.type = 5;
        hover.width = original.width;
        hover.height = original.height;
        hover.valueCompareType = original.valueCompareType;
        hover.requiredValues = original.requiredValues;
        hover.valueIndexArray = original.valueIndexArray;
        if(disabledID != -1)
            hover.disabledSprite = Client.cacheSprite[disabledID];
        if(enabledID != -1)
            hover.enabledSprite = Client.cacheSprite[enabledID];
        rsint.totalChildren(1);
        setBounds(IMAGEID, 0, 0, 0, rsint);
        rsint.tooltip = original.tooltip;
        rsint.invisible = true;
    }

    public static void addHoverConfigButton(int id, int hoverOver, int disabledID, int enabledID, int width, int height, String tooltip, int[] valueCompareType, int[] requiredValues, int[][] valueIndexArray) {
        Widget rsint = addTabInterface(id);
        rsint.parent = id;
        rsint.id = id;
        rsint.type = 5;
        rsint.atActionType = 5;
        rsint.contentType = 206;
        rsint.width = width;
        rsint.height = height;
        rsint.opacity = 0;
        rsint.hoverType = hoverOver;
        rsint.valueCompareType = valueCompareType;
        rsint.requiredValues = requiredValues;
        rsint.valueIndexArray = valueIndexArray;
        if(disabledID != -1)
            rsint.disabledSprite = Client.cacheSprite[disabledID];
        if(enabledID != -1)
            rsint.enabledSprite = Client.cacheSprite[enabledID];
        rsint.tooltip = tooltip;
    }

    public static void addButton(int id, Sprite enabled, Sprite disabled, String tooltip, int w, int h) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 1;
        tab.contentType = 0;
        tab.opacity = (byte) 0;
        tab.hoverType = 52;
        tab.disabledSprite = disabled;
        tab.enabledSprite = enabled;
        tab.width = w;
        tab.height = h;
        tab.tooltip = tooltip;
    }

    public static void addConfigButton(int ID, int pID, Sprite disabled, Sprite enabled, int width, int height, String tT, int configID, int aT, int configFrame) {
        Widget Tab = addTabInterface(ID);
        Tab.parent = pID;
        Tab.id = ID;
        Tab.type = 5;
        Tab.atActionType = aT;
        Tab.contentType = 0;
        Tab.width = width;
        Tab.height = height;
        Tab.opacity = 0;
        Tab.hoverType = -1;
        Tab.valueCompareType = new int[1];
        Tab.requiredValues = new int[1];
        Tab.valueCompareType[0] = 1;
        Tab.requiredValues[0] = configID;
        Tab.valueIndexArray = new int[1][3];
        Tab.valueIndexArray[0][0] = 5;
        Tab.valueIndexArray[0][1] = configFrame;
        Tab.valueIndexArray[0][2] = 0;
        Tab.disabledSprite = disabled;
        Tab.enabledSprite = enabled;
        Tab.tooltip = tT;
    }


    public static Widget addBankTabContainer(int id, int contentType, int width, int height, int size, String... actions) {
        Widget container = addInterface(id);
        container.parent = id;
        container.type = 2;
        container.contentType = contentType;
        container.width = width;
        container.height = height;
        container.sprites = new Sprite[20];
        container.spritesX = new int[20];
        container.spritesY = new int[20];
        container.spritePaddingX = 12;
        container.spritePaddingY = 10;
        container.inventoryItemId = new int[size]; // 10 bank tabs
        container.inventoryAmounts = new int[size]; // 10 bank tabs
        container.allowSwapItems = true;
        container.actions = actions;
        return container;
    }

    public static void addSpriteLoader(int childId, int spriteId) {
        Widget rsi = interfaceCache[childId] = new Widget();
        rsi.id = childId;
        rsi.parent = childId;
        rsi.type = 5;
        rsi.atActionType = 0;
        rsi.contentType = 0;
        rsi.disabledSprite = Client.cacheSprite[spriteId];
        rsi.enabledSprite = Client.cacheSprite[spriteId];


        //rsi.sprite1.spriteLoader = rsi.sprite2.spriteLoader = true;
        //rsi.hoverSprite1 = Client.cacheSprite[hoverSpriteId];
        //rsi.hoverSprite2 = Client.cacheSprite[hoverSpriteId];
        //rsi.hoverSprite1.spriteLoader = rsi.hoverSprite2.spriteLoader = true;
        //rsi.sprite1 = rsi.sprite2 = spriteId;
        //rsi.hoverSprite1Id = rsi.hoverSprite2Id = hoverSpriteId;
        rsi.width = rsi.disabledSprite.myWidth;
        rsi.height = rsi.enabledSprite.myHeight - 2;
        //rsi.isFalseTooltip = true;
    }


    public static void addSprite(int childId, Sprite sprite1, Sprite sprite2) {
        Widget rsi = interfaceCache[childId] = new Widget();
        rsi.id = childId;
        rsi.parent = childId;
        rsi.type = 5;
        rsi.atActionType = 0;
        rsi.contentType = 0;
        rsi.disabledSprite = sprite1;
        rsi.enabledSprite = sprite2;
        rsi.width = rsi.disabledSprite.myWidth;
        rsi.height = rsi.enabledSprite.myHeight - 2;
    }



    public static void addButtonWSpriteLoader(int id, int spriteId) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = id;
        tab.type = 5;
        tab.atActionType = 1;
        tab.contentType = 0;
        tab.opacity = (byte) 0;
        tab.hoverType = 52;
        tab.disabledSprite = Client.cacheSprite[spriteId];
        tab.enabledSprite = Client.cacheSprite[spriteId];
        tab.width = tab.disabledSprite.myWidth;
        tab.height = tab.enabledSprite.myHeight - 2;
    }

    public static void addHoverButtonWConfig(int i, int spriteId, int spriteId2, int width, int height, String text,
                                             int contentType, int hoverOver, int aT, int configId, int configFrame) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.id = i;
        tab.parent = i;
        tab.type = 5;
        tab.atActionType = aT;
        tab.contentType = contentType;
        tab.opacity = 0;
        tab.hoverType = hoverOver;
        tab.width = width;
        tab.height = height;
        tab.tooltip = text;
        tab.valueCompareType = new int[1];
        tab.requiredValues = new int[1];
        tab.valueCompareType[0] = 1;
        tab.requiredValues[0] = configId;
        tab.valueIndexArray = new int[1][3];
        tab.valueIndexArray[0][0] = 5;
        tab.valueIndexArray[0][1] = configFrame;
        tab.valueIndexArray[0][2] = 0;
        if(spriteId != -1)
            tab.disabledSprite = Client.cacheSprite[spriteId];
        if(spriteId2 != -1)
            tab.enabledSprite = Client.cacheSprite[spriteId2];
    }


    public static void addHoverButton_sprite_loader(int i, int spriteId, int width, int height, String text,
                                                    int contentType, int hoverOver, int aT) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.id = i;
        tab.parent = i;
        tab.type = 5;
        tab.atActionType = aT;
        tab.contentType = contentType;
        tab.opacity = 0;
        tab.hoverType = hoverOver;
        tab.disabledSprite = Client.cacheSprite[spriteId];
        tab.enabledSprite = Client.cacheSprite[spriteId];
        tab.width = width;
        tab.height = height;
        tab.tooltip = text;
    }

    public static void addHoveredButton_sprite_loader(int i, int spriteId, int w, int h, int IMAGEID) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.parent = i;
        tab.id = i;
        tab.type = 0;
        tab.atActionType = 0;
        tab.width = w;
        tab.height = h;
        tab.invisible = true;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.scrollMax = 0;
        addHoverImage_sprite_loader(IMAGEID, spriteId);
        tab.totalChildren(1);
        tab.child(0, IMAGEID, 0, 0);
    }
    public static void addButton1(int i, int parent, int w, int h, int sprite1, int sprite2, int hoverOver, String tooltip, int itemId, boolean geSearch) {
        Widget p = addInterface(i);
        p.parent = parent;
        p.type = TYPE_SPRITE;
        p.atActionType = 1;
        p.width = w;
        p.height = h;
        p. tooltip = tooltip;
        p.defaultText = tooltip;
        p.hoverType = hoverOver;
        p.isHovered = true;;
        p.disabledSprite = Client.cacheSprite[sprite1];
        p.enabledSprite = Client.cacheSprite[sprite2];
        p.itemId = itemId;
        p.geSearchButton  = geSearch;
    }

    public static void addHoveredButton_sprite_loader2(int i, int spriteId, int w, int h, int IMAGEID) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.parent = 41002;
        tab.id = i;
        tab.type = 0;
        tab.atActionType = 0;
        tab.width = w;
        tab.height = h;
        tab.invisible = true;
        tab.hoverType = -1;
        tab.scrollMax = 0;
        addTransparentSprite(IMAGEID, spriteId, 70);
        tab.drawsTransparent = true;
        tab.transparency = 0;
        tab.totalChildren(1);
        tab.child(0, IMAGEID, 0, 0);
    }

    public static void addHoveredButton_sprite_loader2(int i, int spriteId, int w, int h, int IMAGEID, int itemId) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.parent = 41002;
        tab.id = i;
        tab.type = 0;
        tab.atActionType = 0;
        tab.width = w;
        tab.height = h;
        tab.invisible = true;
        tab.hoverType = -1;
        tab.scrollMax = 0;
        addTransparentSprite(IMAGEID, spriteId, 70);
        tab.drawsTransparent = true;
        tab.transparency = 0;
        tab.itemId = itemId;
        tab.totalChildren(1);
        tab.child(0, IMAGEID, 0, 0);
    }

    public static void addHoverImage_sprite_loader(int i, int spriteId) {
        Widget tab = addTabInterface(i);
        tab.id = i;
        tab.parent = i;
        tab.type = 5;
        tab.atActionType = 0;
        tab.contentType = 0;
        tab.width = 512;
        tab.height = 334;
        tab.opacity = 0;
        tab.hoverType = 52;
        tab.disabledSprite = Client.cacheSprite[spriteId];
        tab.enabledSprite = Client.cacheSprite[spriteId];
    }

    public static void addBankItem(int index, Boolean hasOption)
    {
        Widget rsi = interfaceCache[index] = new Widget();
        rsi.actions = new String[5];
        rsi.spritesX = new int[20];
        rsi.inventoryAmounts = new int[30];
        rsi.inventoryItemId = new int[30];
        rsi.spritesY = new int[20];

        rsi.children = new int[0];
        rsi.childX = new int[0];
        rsi.childY = new int[0];

        //rsi.hasExamine = false;

        rsi.spritePaddingX = 24;
        rsi.spritePaddingY = 24;
        rsi.height = 5;
        rsi.width = 6;
        rsi.parent = 5292;
        rsi.id = index;
        rsi.type = 2;
    }

    public static void addHoveredButton(int i, String imageName, int j, int w, int h, int IMAGEID) {// hoverable
        // button
        Widget tab = addTabInterface(i);
        tab.parent = i;
        tab.id = i;
        tab.type = 0;
        tab.atActionType = 0;
        tab.width = w;
        tab.height = h;
        tab.invisible = true;
        tab.opacity = 0;
        tab.hoverType = -1;
        tab.scrollMax = 0;
        addHoverImage(IMAGEID, j, j, imageName);
        tab.totalChildren(1);
        tab.child(0, IMAGEID, 0, 0);
    }

    public static void addHoverImage(int i, int j, int k, String name) {
        Widget tab = addTabInterface(i);
        tab.id = i;
        tab.parent = i;
        tab.type = 5;
        tab.atActionType = 0;
        tab.contentType = 0;
        tab.width = 512;
        tab.height = 334;
        tab.opacity = 0;
        tab.hoverType = 52;
        tab.disabledSprite = imageLoader(j, name);
        tab.enabledSprite = imageLoader(k, name);
    }


    public static Widget addTabInterface(int id) {
        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;// 250
        tab.parent = id;// 236
        tab.type = 0;// 262
        tab.atActionType = 0;// 217
        tab.contentType = 0;
        tab.width = 512;// 220
        tab.height = 700;// 267
        tab.opacity = (byte) 0;
        tab.hoverType = -1;// Int 230
        return tab;
    }

    public static Widget addTabInterface(int id, Widget toClone) {

        Widget tab = interfaceCache[id] = new Widget();
        tab.id = id;
        tab.parent = toClone.parent;
        tab.type = toClone.type;
        tab.atActionType = toClone.atActionType;
        tab.contentType = toClone.contentType;
        tab.width = toClone.width;
        tab.height = toClone.height;
        tab.opacity = toClone.opacity;
        tab.hoverType = toClone.hoverType;

        return tab;
    }

    private static Sprite imageLoader(int i, String s) {
        long l = (StringUtils.hashSpriteName(s) << 8) + (long) i;
        Sprite sprite = (Sprite) spriteCache.get(l);
        if (sprite != null)
            return sprite;
        try {
            sprite = new Sprite(s + "" + i);
            spriteCache.put(sprite, l);
        } catch (Exception exception) {
            return null;
        }
        return sprite;
    }

    public void child(int id, int interID, int x, int y) {
        children[id] = interID;
        childX[id] = x;
        childY[id] = y;
    }

    public void totalChildren(int t) {
        children = new int[t];
        childX = new int[t];
        childY = new int[t];
    }

    private Model getModel(int type, int mobId) {
        Model model = (Model) models.get((type << 16) + mobId);

        if (model != null) {
            return model;
        }

        if (type == 1) {
            model = Model.getModel(mobId);
        }

        if (type == 2) {
            model = NpcDefinition.lookup(mobId).model();
        }

        if (type == 3) {
            model = Client.localPlayer.getHeadModel();
        }

        if (type == 4) {
            model = ItemDefinition.lookup(mobId).getUnshadedModel(50);
        }

        if (type == 5) {
            model = null;
        }

        if (model != null) {
            models.put(model, (type << 16) + mobId);
        }

        return model;
    }

    private static Sprite getSprite(int i, FileArchive streamLoader, String s) {
        long l = (StringUtils.hashSpriteName(s) << 8) + (long) i;
        Sprite sprite = (Sprite) spriteCache.get(l);
        if (sprite != null)
            return sprite;
        try {
            sprite = new Sprite(streamLoader, s, i);
            spriteCache.put(sprite, l);
        } catch (Exception _ex) {
            return null;
        }
        return sprite;
    }

    public static void method208(boolean flag, Model model) {
        int i = 0;// was parameter
        int j = 5;// was parameter
        if (flag)
            return;
        models.clear();
        if (model != null && j != 4)
            models.put(model, (j << 16) + i);
    }

    public Model method209(int j, int k, boolean flag) {
        Model model;
        if (flag)
            model = getModel(anInt255, anInt256);
        else
            model = getModel(defaultMediaType, defaultMedia);
        if (model == null)
            return null;
        if (k == -1 && j == -1 && model.triangleColours == null)
            return model;
        Model model_1 = new Model(true, Frame.noAnimationInProgress(k) & Frame.noAnimationInProgress(j), false, model);
        if (k != -1 || j != -1)
            model_1.skin();
        if (k != -1)
            model_1.applyTransform(k);
        if (j != -1)
            model_1.applyTransform(j);
        model_1.light(64, 768, -50, -10, -50, true);
        return model_1;
    }

    public Widget() {
    }

    public static FileArchive interfaceLoader;
    public boolean drawsTransparent;
    public Sprite disabledSprite;
    public int lastFrameTime;

    public Sprite sprites[];
    public static Widget interfaceCache[];
    public int requiredValues[];
    public int contentType;
    public int spritesX[];
    public int defaultHoverColor;
    public int atActionType;
    public String spellName;
    public int secondaryColor;
    public int width;
    public String tooltip;
    public String selectedActionName;
    public boolean centerText;
    public boolean rightText;
    public int scrollPosition;
    public String actions[];
    public int valueIndexArray[][];
    public boolean filled;
    public String secondaryText;
    public int hoverType;
    public int spritePaddingX;
    public int textColor;
    public int defaultMediaType;
    public int defaultMedia;
    public boolean replaceItems;
    public int parent;
    public int spellUsableOn;
    private static ReferenceCache spriteCache;
    public int secondaryHoverColor;
    public int children[];
    public int childX[];
    public boolean usableItems;
    public GameFont textDrawingAreas;
    public int spritePaddingY;
    public int valueCompareType[];
    public int currentFrame;
    public int spritesY[];
    public String defaultText;
    public boolean hasActions;
    public int id;
    public int inventoryAmounts[];
    public int inventoryItemId[];
    public byte opacity;
    private int anInt255;
    private int anInt256;
    public int defaultAnimationId;
    public int secondaryAnimationId;

    public boolean allowSwapItems;
    public Sprite enabledSprite;
    public int scrollMax;
    public int type;
    public int horizontalOffset;
    private static final ReferenceCache models = new ReferenceCache(30);
    public int verticalOffset;
    public boolean invisible;
    public boolean drawingDisabled;
    public int height;
    public boolean textShadow;
    public int modelZoom;
    public int sentItemId;
    public int modelRotation1;
    public int modelRotation2;
    public int translate_x;
    public int translate_yz;
    public int childY[];


    private static final int LUNAR_RUNE_SPRITES_START = 232;
    private static final int LUNAR_OFF_SPRITES_START = 246;
    private static final int LUNAR_ON_SPRITES_START = 285;
    private static final int LUNAR_HOVER_BOX_SPRITES_START = 324;

    public static void addLunarHoverBox(int interface_id, int spriteOffset) {
        Widget RSInterface = addInterface(interface_id);
        RSInterface.id = interface_id;
        RSInterface.parent = interface_id;
        RSInterface.type = 5;
        RSInterface.atActionType = 0;
        RSInterface.contentType = 0;
        RSInterface.opacity = 0;
        RSInterface.hoverType = 52;
        RSInterface.disabledSprite = Client.cacheSprite[LUNAR_HOVER_BOX_SPRITES_START + spriteOffset];
        RSInterface.width = 500;
        RSInterface.height = 500;
        RSInterface.tooltip = "";
    }

    public static void addLunarRune(int i, int spriteOffset, String runeName) {
        Widget RSInterface = addInterface(i);
        RSInterface.type = 5;
        RSInterface.atActionType = 0;
        RSInterface.contentType = 0;
        RSInterface.opacity = 0;
        RSInterface.hoverType = 52;
        RSInterface.disabledSprite = Client.cacheSprite[LUNAR_RUNE_SPRITES_START + spriteOffset];
        RSInterface.width = 500;
        RSInterface.height = 500;
    }

    public static void addLunarText(int ID, int runeAmount, int RuneID, GameFont[] font) {
        Widget rsInterface = addInterface(ID);
        rsInterface.id = ID;
        rsInterface.parent = 1151;
        rsInterface.type = 4;
        rsInterface.atActionType = 0;
        rsInterface.contentType = 0;
        rsInterface.width = 0;
        rsInterface.height = 14;
        rsInterface.opacity = 0;
        rsInterface.hoverType = -1;
        rsInterface.valueCompareType = new int[1];
        rsInterface.requiredValues = new int[1];
        rsInterface.valueCompareType[0] = 3;
        rsInterface.requiredValues[0] = runeAmount;
        rsInterface.valueIndexArray = new int[1][4];
        rsInterface.valueIndexArray[0][0] = 4;
        rsInterface.valueIndexArray[0][1] = 3214;
        rsInterface.valueIndexArray[0][2] = RuneID;
        rsInterface.valueIndexArray[0][3] = 0;
        rsInterface.centerText = true;
        rsInterface.textDrawingAreas = font[0];
        rsInterface.textShadow = true;
        rsInterface.defaultText = "%1/" + runeAmount + "";
        rsInterface.secondaryText = "";
        rsInterface.textColor = 12582912;
        rsInterface.secondaryColor = 49152;
    }

    public static void addLunar2RunesSmallBox(int ID, int r1, int r2, int ra1, int ra2, int rune1, int lvl, String name,
                                              String descr, GameFont[] TDA, int spriteOffset, int suo, int type) {
        Widget rsInterface = addInterface(ID);
        rsInterface.id = ID;
        rsInterface.parent = 1151;
        rsInterface.type = 5;
        rsInterface.atActionType = type;
        rsInterface.contentType = 0;
        rsInterface.hoverType = ID + 1;
        rsInterface.spellUsableOn = suo;
        rsInterface.selectedActionName = "Cast On";
        rsInterface.width = 20;
        rsInterface.height = 20;
        rsInterface.tooltip = "Cast @gre@" + name;
        rsInterface.spellName = name;
        rsInterface.valueCompareType = new int[3];
        rsInterface.requiredValues = new int[3];
        rsInterface.valueCompareType[0] = 3;
        rsInterface.requiredValues[0] = ra1;
        rsInterface.valueCompareType[1] = 3;
        rsInterface.requiredValues[1] = ra2;
        rsInterface.valueCompareType[2] = 3;
        rsInterface.requiredValues[2] = lvl;
        rsInterface.valueIndexArray = new int[3][];
        rsInterface.valueIndexArray[0] = new int[4];
        rsInterface.valueIndexArray[0][0] = 4;
        rsInterface.valueIndexArray[0][1] = 3214;
        rsInterface.valueIndexArray[0][2] = r1;
        rsInterface.valueIndexArray[0][3] = 0;
        rsInterface.valueIndexArray[1] = new int[4];
        rsInterface.valueIndexArray[1][0] = 4;
        rsInterface.valueIndexArray[1][1] = 3214;
        rsInterface.valueIndexArray[1][2] = r2;
        rsInterface.valueIndexArray[1][3] = 0;
        rsInterface.valueIndexArray[2] = new int[3];
        rsInterface.valueIndexArray[2][0] = 1;
        rsInterface.valueIndexArray[2][1] = 6;
        rsInterface.valueIndexArray[2][2] = 0;
        rsInterface.enabledSprite = Client.cacheSprite[LUNAR_ON_SPRITES_START+ spriteOffset];
        rsInterface.disabledSprite = Client.cacheSprite[LUNAR_OFF_SPRITES_START+ spriteOffset];

        Widget hover = addInterface(ID + 1);
        hover.parent = ID;
        hover.hoverType = -1;
        hover.type = 0;
        hover.opacity = 0;
        hover.scrollMax = 0;
        hover.invisible = true;
        setChildren(7, hover);
        addLunarHoverBox(ID + 2, 0);
        setBounds(ID + 2, 0, 0, 0, hover);
        addText(ID + 3, "Level " + (lvl + 1) + ": " + name, 0xFF981F, true, true, 52, TDA, 1);
        setBounds(ID + 3, 90, 4, 1, hover);
        addText(ID + 4, descr, 0xAF6A1A, true, true, 52, TDA, 0);
        setBounds(ID + 4, 90, 19, 2, hover);
        setBounds(30016, 37, 35, 3, hover);// Rune
        setBounds(rune1, 112, 35, 4, hover);// Rune
        addLunarText(ID + 5, ra1 + 1, r1, TDA);
        setBounds(ID + 5, 50, 66, 5, hover);
        addLunarText(ID + 6, ra2 + 1, r2, TDA);
        setBounds(ID + 6, 123, 66, 6, hover);
    }

    public static void addLunar3RunesSmallBox(int ID, int r1, int r2, int r3, int ra1, int ra2, int ra3, int rune1,
                                              int rune2, int lvl, String name, String descr, GameFont[] TDA, int spriteOffset, int suo, int type) {
        Widget rsInterface = addInterface(ID);
        rsInterface.id = ID;
        rsInterface.parent = 1151;
        rsInterface.type = 5;
        rsInterface.atActionType = type;
        rsInterface.contentType = 0;
        rsInterface.hoverType = ID + 1;
        rsInterface.spellUsableOn = suo;
        rsInterface.selectedActionName = "Cast on";
        rsInterface.width = 20;
        rsInterface.height = 20;
        rsInterface.tooltip = "Cast @gre@" + name;
        rsInterface.spellName = name;
        rsInterface.valueCompareType = new int[4];
        rsInterface.requiredValues = new int[4];
        rsInterface.valueCompareType[0] = 3;
        rsInterface.requiredValues[0] = ra1;
        rsInterface.valueCompareType[1] = 3;
        rsInterface.requiredValues[1] = ra2;
        rsInterface.valueCompareType[2] = 3;
        rsInterface.requiredValues[2] = ra3;
        rsInterface.valueCompareType[3] = 3;
        rsInterface.requiredValues[3] = lvl;
        rsInterface.valueIndexArray = new int[4][];
        rsInterface.valueIndexArray[0] = new int[4];
        rsInterface.valueIndexArray[0][0] = 4;
        rsInterface.valueIndexArray[0][1] = 3214;
        rsInterface.valueIndexArray[0][2] = r1;
        rsInterface.valueIndexArray[0][3] = 0;
        rsInterface.valueIndexArray[1] = new int[4];
        rsInterface.valueIndexArray[1][0] = 4;
        rsInterface.valueIndexArray[1][1] = 3214;
        rsInterface.valueIndexArray[1][2] = r2;
        rsInterface.valueIndexArray[1][3] = 0;
        rsInterface.valueIndexArray[2] = new int[4];
        rsInterface.valueIndexArray[2][0] = 4;
        rsInterface.valueIndexArray[2][1] = 3214;
        rsInterface.valueIndexArray[2][2] = r3;
        rsInterface.valueIndexArray[2][3] = 0;
        rsInterface.valueIndexArray[3] = new int[3];
        rsInterface.valueIndexArray[3][0] = 1;
        rsInterface.valueIndexArray[3][1] = 6;
        rsInterface.valueIndexArray[3][2] = 0;
        rsInterface.enabledSprite = Client.cacheSprite[LUNAR_ON_SPRITES_START+ spriteOffset];
        rsInterface.disabledSprite = Client.cacheSprite[LUNAR_OFF_SPRITES_START+ spriteOffset];

        Widget hover = addInterface(ID + 1);
        hover.parent = ID;
        hover.hoverType = -1;
        hover.type = 0;
        hover.opacity = 0;
        hover.scrollMax = 0;
        hover.invisible = true;
        setChildren(9, hover);
        addLunarHoverBox(ID + 2, 0);
        setBounds(ID + 2, 0, 0, 0, hover);
        addText(ID + 3, "Level " + (lvl + 1) + ": " + name, 0xFF981F, true, true, 52, TDA, 1);
        setBounds(ID + 3, 90, 4, 1, hover);
        addText(ID + 4, descr, 0xAF6A1A, true, true, 52, TDA, 0);
        setBounds(ID + 4, 90, 19, 2, hover);
        setBounds(30016, 14, 35, 3, hover);
        setBounds(rune1, 74, 35, 4, hover);
        setBounds(rune2, 130, 35, 5, hover);
        addLunarText(ID + 5, ra1 + 1, r1, TDA);
        setBounds(ID + 5, 26, 66, 6, hover);
        addLunarText(ID + 6, ra2 + 1, r2, TDA);
        setBounds(ID + 6, 87, 66, 7, hover);
        addLunarText(ID + 7, ra3 + 1, r3, TDA);
        setBounds(ID + 7, 142, 66, 8, hover);
    }

    public static void addLunar3RunesBigBox(int ID, int r1, int r2, int r3, int ra1, int ra2, int ra3, int rune1,
                                            int rune2, int lvl, String name, String descr, GameFont[] TDA, int spriteOffset, int suo, int type) {
        Widget rsInterface = addInterface(ID);
        rsInterface.id = ID;
        rsInterface.parent = 1151;
        rsInterface.type = 5;
        rsInterface.atActionType = type;
        rsInterface.contentType = 0;
        rsInterface.hoverType = ID + 1;
        rsInterface.spellUsableOn = suo;
        rsInterface.selectedActionName = "Cast on";
        rsInterface.width = 20;
        rsInterface.height = 20;
        rsInterface.tooltip = "Cast @gre@" + name;
        rsInterface.spellName = name;
        rsInterface.valueCompareType = new int[4];
        rsInterface.requiredValues = new int[4];
        rsInterface.valueCompareType[0] = 3;
        rsInterface.requiredValues[0] = ra1;
        rsInterface.valueCompareType[1] = 3;
        rsInterface.requiredValues[1] = ra2;
        rsInterface.valueCompareType[2] = 3;
        rsInterface.requiredValues[2] = ra3;
        rsInterface.valueCompareType[3] = 3;
        rsInterface.requiredValues[3] = lvl;
        rsInterface.valueIndexArray = new int[4][];
        rsInterface.valueIndexArray[0] = new int[4];
        rsInterface.valueIndexArray[0][0] = 4;
        rsInterface.valueIndexArray[0][1] = 3214;
        rsInterface.valueIndexArray[0][2] = r1;
        rsInterface.valueIndexArray[0][3] = 0;
        rsInterface.valueIndexArray[1] = new int[4];
        rsInterface.valueIndexArray[1][0] = 4;
        rsInterface.valueIndexArray[1][1] = 3214;
        rsInterface.valueIndexArray[1][2] = r2;
        rsInterface.valueIndexArray[1][3] = 0;
        rsInterface.valueIndexArray[2] = new int[4];
        rsInterface.valueIndexArray[2][0] = 4;
        rsInterface.valueIndexArray[2][1] = 3214;
        rsInterface.valueIndexArray[2][2] = r3;
        rsInterface.valueIndexArray[2][3] = 0;
        rsInterface.valueIndexArray[3] = new int[3];
        rsInterface.valueIndexArray[3][0] = 1;
        rsInterface.valueIndexArray[3][1] = 6;
        rsInterface.valueIndexArray[3][2] = 0;
        rsInterface.enabledSprite = Client.cacheSprite[LUNAR_ON_SPRITES_START+ spriteOffset];
        rsInterface.disabledSprite = Client.cacheSprite[LUNAR_OFF_SPRITES_START+ spriteOffset];

        Widget hover = addInterface(ID + 1);
        hover.parent = ID;
        hover.hoverType = -1;
        hover.type = 0;
        hover.opacity = 0;
        hover.scrollMax = 0;
        hover.invisible = true;
        setChildren(9, hover);
        addLunarHoverBox(ID + 2, 1);
        setBounds(ID + 2, 0, 0, 0, hover);
        addText(ID + 3, "Level " + (lvl + 1) + ": " + name, 0xFF981F, true, true, 52, TDA, 1);
        setBounds(ID + 3, 90, 4, 1, hover);
        addText(ID + 4, descr, 0xAF6A1A, true, true, 52, TDA, 0);
        setBounds(ID + 4, 90, 21, 2, hover);
        setBounds(30016, 14, 48, 3, hover);
        setBounds(rune1, 74, 48, 4, hover);
        setBounds(rune2, 130, 48, 5, hover);
        addLunarText(ID + 5, ra1 + 1, r1, TDA);
        setBounds(ID + 5, 26, 79, 6, hover);
        addLunarText(ID + 6, ra2 + 1, r2, TDA);
        setBounds(ID + 6, 87, 79, 7, hover);
        addLunarText(ID + 7, ra3 + 1, r3, TDA);
        setBounds(ID + 7, 142, 79, 8, hover);
    }

    public static void addLunar3RunesLargeBox(int ID, int r1, int r2, int r3, int ra1, int ra2, int ra3, int rune1,
                                              int rune2, int lvl, String name, String descr, GameFont[] TDA, int spriteOffset, int suo, int type) {
        Widget rsInterface = addInterface(ID);
        rsInterface.id = ID;
        rsInterface.parent = 1151;
        rsInterface.type = 5;
        rsInterface.atActionType = type;
        rsInterface.contentType = 0;
        rsInterface.hoverType = ID + 1;
        rsInterface.spellUsableOn = suo;
        rsInterface.selectedActionName = "Cast on";
        rsInterface.width = 20;
        rsInterface.height = 20;
        rsInterface.tooltip = "Cast @gre@" + name;
        rsInterface.spellName = name;
        rsInterface.valueCompareType = new int[4];
        rsInterface.requiredValues = new int[4];
        rsInterface.valueCompareType[0] = 3;
        rsInterface.requiredValues[0] = ra1;
        rsInterface.valueCompareType[1] = 3;
        rsInterface.requiredValues[1] = ra2;
        rsInterface.valueCompareType[2] = 3;
        rsInterface.requiredValues[2] = ra3;
        rsInterface.valueCompareType[3] = 3;
        rsInterface.requiredValues[3] = lvl;
        rsInterface.valueIndexArray = new int[4][];
        rsInterface.valueIndexArray[0] = new int[4];
        rsInterface.valueIndexArray[0][0] = 4;
        rsInterface.valueIndexArray[0][1] = 3214;
        rsInterface.valueIndexArray[0][2] = r1;
        rsInterface.valueIndexArray[0][3] = 0;
        rsInterface.valueIndexArray[1] = new int[4];
        rsInterface.valueIndexArray[1][0] = 4;
        rsInterface.valueIndexArray[1][1] = 3214;
        rsInterface.valueIndexArray[1][2] = r2;
        rsInterface.valueIndexArray[1][3] = 0;
        rsInterface.valueIndexArray[2] = new int[4];
        rsInterface.valueIndexArray[2][0] = 4;
        rsInterface.valueIndexArray[2][1] = 3214;
        rsInterface.valueIndexArray[2][2] = r3;
        rsInterface.valueIndexArray[2][3] = 0;
        rsInterface.valueIndexArray[3] = new int[3];
        rsInterface.valueIndexArray[3][0] = 1;
        rsInterface.valueIndexArray[3][1] = 6;
        rsInterface.valueIndexArray[3][2] = 0;
        rsInterface.enabledSprite = Client.cacheSprite[LUNAR_ON_SPRITES_START+ spriteOffset];
        rsInterface.disabledSprite = Client.cacheSprite[LUNAR_OFF_SPRITES_START+ spriteOffset];
        Widget hover = addInterface(ID + 1);
        hover.parent = ID;
        hover.hoverType = -1;
        hover.type = 0;
        hover.opacity = 0;
        hover.scrollMax = 0;
        hover.invisible = true;
        setChildren(9, hover);
        addLunarHoverBox(ID + 2, 2);
        setBounds(ID + 2, 0, 0, 0, hover);
        addText(ID + 3, "Level " + (lvl + 1) + ": " + name, 0xFF981F, true, true, 52, TDA, 1);
        setBounds(ID + 3, 90, 4, 1, hover);
        addText(ID + 4, descr, 0xAF6A1A, true, true, 52, TDA, 0);
        setBounds(ID + 4, 90, 34, 2, hover);
        setBounds(30016, 14, 61, 3, hover);
        setBounds(rune1, 74, 61, 4, hover);
        setBounds(rune2, 130, 61, 5, hover);
        addLunarText(ID + 5, ra1 + 1, r1, TDA);
        setBounds(ID + 5, 26, 92, 6, hover);
        addLunarText(ID + 6, ra2 + 1, r2, TDA);
        setBounds(ID + 6, 87, 92, 7, hover);
        addLunarText(ID + 7, ra3 + 1, r3, TDA);
        setBounds(ID + 7, 142, 92, 8, hover);
    }

    public static void configureLunar(GameFont[] tda) {
        constructLunar();
        addLunarRune(30003, 0, "Fire");
        addLunarRune(30004, 1, "Water");
        addLunarRune(30005, 2, "Air");
        addLunarRune(30006, 3, "Earth");
        addLunarRune(30007, 4, "Mind");
        addLunarRune(30008, 5, "Body");
        addLunarRune(30009, 6, "Death");
        addLunarRune(30010, 7, "Nature");
        addLunarRune(30011, 8, "Chaos");
        addLunarRune(30012, 9, "Law");
        addLunarRune(30013, 10, "Cosmic");
        addLunarRune(30014, 11, "Blood");
        addLunarRune(30015, 12, "Soul");
        addLunarRune(30016, 13, "Astral");

        addLunar3RunesSmallBox(30017, 9075, 554, 555, 0, 4, 3, 30003, 30004, 64, "Bake Pie",
                "Bake pies without a stove", tda, 0, 16, 2);
        addLunar2RunesSmallBox(30025, 9075, 557, 0, 7, 30006, 65, "Cure Plant", "Cure disease on farming patch", tda, 1,
                4, 2);
        addLunar3RunesBigBox(30032, 9075, 564, 558, 0, 0, 0, 30013, 30007, 65, "Monster Examine",
                "Detect the combat statistics of a\\nmonster", tda, 2, 2, 2);
        addLunar3RunesSmallBox(30040, 9075, 564, 556, 0, 0, 1, 30013, 30005, 66, "NPC Contact",
                "Speak with varied NPCs", tda, 3, 0, 2);
        addLunar3RunesSmallBox(30048, 9075, 563, 557, 0, 0, 9, 30012, 30006, 67, "Cure Other", "Cure poisoned players",
                tda, 4, 8, 2);
        addLunar3RunesSmallBox(30056, 9075, 555, 554, 0, 2, 0, 30004, 30003, 67, "Humidify",
                "Fills certain vessels with water", tda, 5, 0, 5);
        addLunar3RunesSmallBox(30064, 9075, 563, 557, 1, 0, 1, 30012, 30006, 68, "Moonclan Teleport",
                "Teleports you to moonclan island", tda, 6, 0, 5);
        addLunar3RunesBigBox(30075, 9075, 563, 557, 1, 0, 3, 30012, 30006, 69, "Tele Group Moonclan",
                "Teleports players to Moonclan\\nisland", tda, 7, 0, 5);
        addLunar3RunesSmallBox(30083, 9075, 563, 557, 1, 0, 5, 30012, 30006, 70, "Ourania Teleport",
                "Teleports you to ourania rune altar", tda, 8, 0, 5);
        addLunar3RunesSmallBox(30091, 9075, 564, 563, 1, 1, 0, 30013, 30012, 70, "Cure Me", "Cures Poison", tda, 9, 0,
                5);
        addLunar2RunesSmallBox(30099, 9075, 557, 1, 1, 30006, 70, "Hunter Kit", "Get a kit of hunting gear", tda, 10, 0,
                5);
        addLunar3RunesSmallBox(30106, 9075, 563, 555, 1, 0, 0, 30012, 30004, 71, "Waterbirth Teleport",
                "Teleports you to Waterbirth island", tda, 11, 0, 5);
        addLunar3RunesBigBox(30114, 9075, 563, 555, 1, 0, 4, 30012, 30004, 72, "Tele Group Waterbirth",
                "Teleports players to Waterbirth\\nisland", tda, 12, 0, 5);
        addLunar3RunesSmallBox(30122, 9075, 564, 563, 1, 1, 1, 30013, 30012, 73, "Cure Group",
                "Cures Poison on players", tda, 13, 0, 5);
        addLunar3RunesBigBox(30130, 9075, 564, 559, 1, 1, 4, 30013, 30008, 74, "Stat Spy",
                "Cast on another player to see their\\nskill levels", tda, 14, 8, 2);
        addLunar3RunesBigBox(30138, 9075, 563, 554, 1, 1, 2, 30012, 30003, 74, "Barbarian Teleport",
                "Teleports you to the Barbarian\\noutpost", tda, 15, 0, 5);
        addLunar3RunesBigBox(30146, 9075, 563, 554, 1, 1, 5, 30012, 30003, 75, "Tele Group Barbarian",
                "Teleports players to the Barbarian\\noutpost", tda, 16, 0, 5);
        addLunar3RunesSmallBox(30154, 9075, 554, 556, 1, 5, 9, 30003, 30005, 76, "Superglass Make",
                "Make glass without a furnace", tda, 17, 16, 2);
        addLunar3RunesSmallBox(30162, 9075, 563, 555, 1, 1, 3, 30012, 30004, 77, "Khazard Teleport",
                "Teleports you to Port khazard", tda, 18, 0, 5);
        addLunar3RunesSmallBox(30170, 9075, 563, 555, 1, 1, 7, 30012, 30004, 78, "Tele Group Khazard",
                "Teleports players to Port khazard", tda, 19, 0, 5);
        addLunar3RunesBigBox(30178, 9075, 564, 559, 1, 0, 4, 30013, 30008, 78, "Dream",
                "Take a rest and restore hitpoints 3\\n times faster", tda, 20, 0, 5);
        addLunar3RunesSmallBox(30186, 9075, 557, 555, 1, 9, 4, 30006, 30004, 79, "String Jewellery",
                "String amulets without wool", tda, 21, 0, 5);
        addLunar3RunesLargeBox(30194, 9075, 557, 555, 1, 9, 9, 30006, 30004, 80, "Stat Restore Pot\\nShare",
                "Share a potion with up to 4 nearby\\nplayers", tda, 22, 0, 5);
        addLunar3RunesSmallBox(30202, 9075, 554, 555, 1, 6, 6, 30003, 30004, 81, "Magic Imbue",
                "Combine runes without a talisman", tda, 23, 0, 5);
        addLunar3RunesBigBox(30210, 9075, 561, 557, 2, 1, 14, 30010, 30006, 82, "Fertile Soil",
                "Fertilise a farming patch with super\\ncompost", tda, 24, 4, 2);
        addLunar3RunesBigBox(30218, 9075, 557, 555, 2, 11, 9, 30006, 30004, 83, "Boost Potion Share",
                "Shares a potion with up to 4 nearby\\nplayers", tda, 25, 0, 5);
        addLunar3RunesSmallBox(30226, 9075, 563, 555, 2, 2, 9, 30012, 30004, 84, "Fishing Guild Teleport",
                "Teleports you to the fishing guild", tda, 26, 0, 5);
        addLunar3RunesLargeBox(30234, 9075, 563, 555, 1, 2, 13, 30012, 30004, 85, "Tele Group Fishing Guild",
                "Teleports players to the Fishing\\nGuild", tda, 27, 0, 5);
        addLunar3RunesSmallBox(30242, 9075, 557, 561, 2, 14, 0, 30006, 30010, 85, "Plank Make", "Turn Logs into planks",
                tda, 28, 16, 5);
        addLunar3RunesSmallBox(30250, 9075, 563, 555, 2, 2, 9, 30012, 30004, 86, "Catherby Teleport",
                "Teleports you to Catherby", tda, 29, 0, 5);
        addLunar3RunesSmallBox(30258, 9075, 563, 555, 2, 2, 14, 30012, 30004, 87, "Tele Group Catherby",
                "Teleports players to Catherby", tda, 30, 0, 5);
        addLunar3RunesSmallBox(30266, 9075, 563, 555, 2, 2, 7, 30012, 30004, 88, "Ice Plateau Teleport",
                "Teleports you to Ice Plateau", tda, 31, 0, 5);
        addLunar3RunesLargeBox(30274, 9075, 563, 555, 2, 2, 15, 30012, 30004, 89, "Tele Group Ice Plateau",
                "Teleports players to Ice Plateau", tda, 32, 0, 5);
        addLunar3RunesBigBox(30282, 9075, 563, 561, 2, 1, 0, 30012, 30010, 90, "Energy Transfer",
                "Spend HP and SA energy to\\n give another SA and run energy", tda, 33, 8, 2);
        addLunar3RunesBigBox(30290, 9075, 563, 565, 2, 2, 0, 30012, 30014, 91, "Heal Other",
                "Transfer up to 75% of hitpoints\\n to another player", tda, 34, 8, 2);
        addLunar3RunesBigBox(30298, 9075, 560, 557, 2, 1, 9, 30009, 30006, 92, "Vengeance Other",
                "Allows another player to rebound\\ndamage to an opponent", tda, 35, 8, 2);
        addLunar3RunesSmallBox(30306, 9075, 560, 557, 3, 1, 9, 30009, 30006, 93, "Vengeance",
                "Rebound damage to an opponent", tda, 36, 0, 5);
        addLunar3RunesBigBox(30314, 9075, 565, 563, 3, 2, 5, 30014, 30012, 94, "Heal Group",
                "Transfer up to 75% of hitpoints\\n to a group", tda, 37, 0, 5);
        addLunar3RunesBigBox(30322, 9075, 564, 563, 2, 1, 0, 30013, 30012, 95, "Spellbook Swap",
                "Change to another spellbook for 1\\nspell cast", tda, 38, 0, 5);
    }

    public static void constructLunar() {
        Widget Interface = addTabInterface(29999);
        setChildren(51, Interface); // 50 children
        int child = 0;

        //ADD "HOME" AND "OTHER" TELEPORTS
        setBounds(39101, 10, 9, child++, Interface);
        setBounds(39102, 10, 9, child++, Interface);
        setBounds(39104, 105, 9, child++, Interface);
        setBounds(39105, 105, 9, child++, Interface);


        //NOTE: Lunar Teleports have been removed from the spellbook (the ones that are commented out are teleports)

        //Row 1
        setBounds(30017, 20, 60, child++, Interface);
        setBounds(30025, 61, 62, child++, Interface);
        setBounds(30032, 102, 61, child++, Interface);
        setBounds(30040, 142, 62, child++, Interface);

        //Row 2
        setBounds(30048, 20, 93, child++, Interface);
        setBounds(30056, 60, 92, child++, Interface);
        setBounds(30091, 102, 92, child++, Interface);
        setBounds(30099, 142, 90, child++, Interface);

        //Row 3
        setBounds(30122, 20, 123, child++, Interface);
        setBounds(30130, 62, 123, child++, Interface);
        setBounds(30154, 106, 123, child++, Interface);
        setBounds(30154, 147, 123, child++, Interface);

        //Row 4
        setBounds(30178, 19, 154, child++, Interface);
        setBounds(30186, 63, 155, child++, Interface);
        setBounds(30194, 106, 155, child++, Interface);
        setBounds(30202, 145, 155, child++, Interface);

        //Row 5
        setBounds(30210, 21, 184, child++, Interface);
        setBounds(30218, 66, 186, child++, Interface);
        setBounds(30282, 105, 184, child++, Interface);
        setBounds(30290, 145, 183, child++, Interface);

        //Row 6
        setBounds(30298, 23, 214, child++, Interface);
        setBounds(30306, 66, 214, child++, Interface);
        setBounds(30314, 105, 214, child++, Interface);
        setBounds(30322, 147, 214, child++, Interface);


        //setBounds(30064, 39, 39, child++, Interface);
        //setBounds(30075, 71, 39, child++, Interface);
        //setBounds(30083, 103, 39, child++, Interface);
        //setBounds(30106, 12, 68, child++, Interface);
        //	setBounds(30114, 42, 68, child++, Interface);
        //	setBounds(30138, 135, 68, child++, Interface);
        //	setBounds(30146, 165, 68, child++, Interface);
        //	setBounds(30162, 42, 97, child++, Interface);
        //	setBounds(30170, 71, 97, child++, Interface);

        //	setBounds(30226, 103, 125, child++, Interface);
        //	setBounds(30234, 135, 125, child++, Interface);
        //	setBounds(30242, 164, 126, child++, Interface);
        //	setBounds(30250, 10, 155, child++, Interface);
        //	setBounds(30258, 42, 155, child++, Interface);
        //	setBounds(30266, 71, 155, child++, Interface);
        //	setBounds(30274, 103, 155, child++, Interface);

        setBounds(30018, 5, 176, child++, Interface);// hover
        setBounds(30026, 5, 176, child++, Interface);// hover
        setBounds(30033, 5, 163, child++, Interface);// hover
        setBounds(30041, 5, 176, child++, Interface);// hover
        setBounds(30049, 5, 176, child++, Interface);// hover
        setBounds(30057, 5, 176, child++, Interface);// hover
        //setBounds(30065, 5, 176, child++, Interface);// hover
        //setBounds(30076, 5, 163, child++, Interface);// hover
        //setBounds(30084, 5, 176, child++, Interface);// hover
        setBounds(30092, 5, 176, child++, Interface);// hover
        setBounds(30100, 5, 176, child++, Interface);// hover
        //	setBounds(30107, 5, 176, child++, Interface);// hover
        //	setBounds(30115, 5, 163, child++, Interface);// hover
        setBounds(30123, 5, 176, child++, Interface);// hover
        setBounds(30131, 5, 163, child++, Interface);// hover
        //	setBounds(30139, 5, 163, child++, Interface);// hover
        //	setBounds(30147, 5, 163, child++, Interface);// hover
        setBounds(30155, 5, 176, child++, Interface);// hover
        //	setBounds(30163, 5, 176, child++, Interface);// hover
        //	setBounds(30171, 5, 176, child++, Interface);// hover
        setBounds(30179, 5, 40, child++, Interface);// hover
        setBounds(30187, 5, 40, child++, Interface);// hover
        setBounds(30195, 5, 40, child++, Interface);// hover
        setBounds(30203, 5, 40, child++, Interface);// hover
        setBounds(30211, 5, 40, child++, Interface);// hover
        setBounds(30219, 5, 40, child++, Interface);// hover

        //	setBounds(30227, 5, 176, child++, Interface);// hover
        //	setBounds(30235, 5, 149, child++, Interface);// hover
        //	setBounds(30243, 5, 176, child++, Interface);// hover
        //	setBounds(30251, 5, 5, child++, Interface);// hover
        //	setBounds(30259, 5, 5, child++, Interface);// hover
        //	setBounds(30267, 5, 5, child++, Interface);// hover
        //	setBounds(30275, 5, 5, child++, Interface);// hover
        setBounds(30283, 5, 40, child++, Interface);// hover
        setBounds(30291, 5, 40, child++, Interface);// hover
        setBounds(30299, 5, 40, child++, Interface);// hover
        setBounds(30307, 5, 40, child++, Interface);// hover
        setBounds(30323, 5, 40, child++, Interface);// hover
        setBounds(30315, 5, 40, child++, Interface);// hover*/
    }

    private static void levelUpInterfaces() {
        Widget attack = interfaceCache[6247];
        Widget defence = interfaceCache[6253];
        Widget str = interfaceCache[6206];
        Widget hits = interfaceCache[6216];
        Widget rng = interfaceCache[4443];
        Widget pray = interfaceCache[6242];
        Widget mage = interfaceCache[6211];
        Widget cook = interfaceCache[6226];
        Widget wood = interfaceCache[4272];
        Widget flet = interfaceCache[6231];
        Widget fish = interfaceCache[6258];
        Widget fire = interfaceCache[4282];
        Widget craf = interfaceCache[6263];
        Widget smit = interfaceCache[6221];
        Widget mine = interfaceCache[4416];
        Widget herb = interfaceCache[6237];
        Widget agil = interfaceCache[4277];
        Widget thie = interfaceCache[4261];
        Widget slay = interfaceCache[12122];
        Widget farm = addTabInterface(5267);
        Widget rune = interfaceCache[4267];
        Widget cons = addTabInterface(7267);
        Widget hunt = addTabInterface(8267);
        Widget summ = addTabInterface(9267);
        Widget dung = addTabInterface(10267);
        addSkillChatSprite(29578, 0);
        addSkillChatSprite(29579, 1);
        addSkillChatSprite(29580, 2);
        addSkillChatSprite(29581, 3);
        addSkillChatSprite(29582, 4);
        addSkillChatSprite(29583, 5);
        addSkillChatSprite(29584, 6);
        addSkillChatSprite(29585, 7);
        addSkillChatSprite(29586, 8);
        addSkillChatSprite(29587, 9);
        addSkillChatSprite(29588, 10);
        addSkillChatSprite(29589, 11);
        addSkillChatSprite(29590, 12);
        addSkillChatSprite(29591, 13);
        addSkillChatSprite(29592, 14);
        addSkillChatSprite(29593, 15);
        addSkillChatSprite(29594, 16);
        addSkillChatSprite(29595, 17);
        addSkillChatSprite(29596, 18);
        addSkillChatSprite(11897, 19);
        addSkillChatSprite(29598, 20);
        addSkillChatSprite(29599, 21);
        addSkillChatSprite(29600, 22);
        addSkillChatSprite(29601, 23);
        addSkillChatSprite(29602, 24);
        setChildren(4, attack);
        setBounds(29578, 20, 30, 0, attack);
        setBounds(4268, 80, 15, 1, attack);
        setBounds(4269, 80, 45, 2, attack);
        setBounds(358, 95, 75, 3, attack);
        setChildren(4, defence);
        setBounds(29579, 20, 30, 0, defence);
        setBounds(4268, 80, 15, 1, defence);
        setBounds(4269, 80, 45, 2, defence);
        setBounds(358, 95, 75, 3, defence);
        setChildren(4, str);
        setBounds(29580, 20, 30, 0, str);
        setBounds(4268, 80, 15, 1, str);
        setBounds(4269, 80, 45, 2, str);
        setBounds(358, 95, 75, 3, str);
        setChildren(4, hits);
        setBounds(29581, 20, 30, 0, hits);
        setBounds(4268, 80, 15, 1, hits);
        setBounds(4269, 80, 45, 2, hits);
        setBounds(358, 95, 75, 3, hits);
        setChildren(4, rng);
        setBounds(29582, 20, 30, 0, rng);
        setBounds(4268, 80, 15, 1, rng);
        setBounds(4269, 80, 45, 2, rng);
        setBounds(358, 95, 75, 3, rng);
        setChildren(4, pray);
        setBounds(29583, 20, 30, 0, pray);
        setBounds(4268, 80, 15, 1, pray);
        setBounds(4269, 80, 45, 2, pray);
        setBounds(358, 95, 75, 3, pray);
        setChildren(4, mage);
        setBounds(29584, 20, 30, 0, mage);
        setBounds(4268, 80, 15, 1, mage);
        setBounds(4269, 80, 45, 2, mage);
        setBounds(358, 95, 75, 3, mage);
        setChildren(4, cook);
        setBounds(29585, 20, 30, 0, cook);
        setBounds(4268, 80, 15, 1, cook);
        setBounds(4269, 80, 45, 2, cook);
        setBounds(358, 95, 75, 3, cook);
        setChildren(4, wood);
        setBounds(29586, 20, 30, 0, wood);
        setBounds(4268, 80, 15, 1, wood);
        setBounds(4269, 80, 45, 2, wood);
        setBounds(358, 95, 75, 3, wood);
        setChildren(4, flet);
        setBounds(29587, 20, 30, 0, flet);
        setBounds(4268, 80, 15, 1, flet);
        setBounds(4269, 80, 45, 2, flet);
        setBounds(358, 95, 75, 3, flet);
        setChildren(4, fish);
        setBounds(29588, 20, 30, 0, fish);
        setBounds(4268, 80, 15, 1, fish);
        setBounds(4269, 80, 45, 2, fish);
        setBounds(358, 95, 75, 3, fish);
        setChildren(4, fire);
        setBounds(29589, 20, 30, 0, fire);
        setBounds(4268, 80, 15, 1, fire);
        setBounds(4269, 80, 45, 2, fire);
        setBounds(358, 95, 75, 3, fire);
        setChildren(4, craf);
        setBounds(29590, 20, 30, 0, craf);
        setBounds(4268, 80, 15, 1, craf);
        setBounds(4269, 80, 45, 2, craf);
        setBounds(358, 95, 75, 3, craf);
        setChildren(4, smit);
        setBounds(29591, 20, 30, 0, smit);
        setBounds(4268, 80, 15, 1, smit);
        setBounds(4269, 80, 45, 2, smit);
        setBounds(358, 95, 75, 3, smit);
        setChildren(4, mine);
        setBounds(29592, 20, 30, 0, mine);
        setBounds(4268, 80, 15, 1, mine);
        setBounds(4269, 80, 45, 2, mine);
        setBounds(358, 95, 75, 3, mine);
        setChildren(4, herb);
        setBounds(29593, 20, 30, 0, herb);
        setBounds(4268, 80, 15, 1, herb);
        setBounds(4269, 80, 45, 2, herb);
        setBounds(358, 95, 75, 3, herb);
        setChildren(4, agil);
        setBounds(29594, 20, 30, 0, agil);
        setBounds(4268, 80, 15, 1, agil);
        setBounds(4269, 80, 45, 2, agil);
        setBounds(358, 95, 75, 3, agil);
        setChildren(4, thie);
        setBounds(29595, 20, 30, 0, thie);
        setBounds(4268, 80, 15, 1, thie);
        setBounds(4269, 80, 45, 2, thie);
        setBounds(358, 95, 75, 3, thie);
        setChildren(4, slay);
        setBounds(29596, 20, 30, 0, slay);
        setBounds(4268, 80, 15, 1, slay);
        setBounds(4269, 80, 45, 2, slay);
        setBounds(358, 95, 75, 3, slay);
        setChildren(4, farm);
        setBounds(11897, 20, 30, 0, farm);
        setBounds(4268, 80, 15, 1, farm);
        setBounds(4269, 80, 45, 2, farm);
        setBounds(358, 95, 75, 3, farm);
        setChildren(4, rune);
        setBounds(29598, 20, 30, 0, rune);
        setBounds(4268, 80, 15, 1, rune);
        setBounds(4269, 80, 45, 2, rune);
        setBounds(358, 95, 75, 3, rune);
        setChildren(4, cons);
        setBounds(29599, 20, 30, 0, cons);
        setBounds(4268, 80, 15, 1, cons);
        setBounds(4269, 80, 45, 2, cons);
        setBounds(358, 95, 75, 3, cons);
        setChildren(4, hunt);
        setBounds(29600, 20, 30, 0, hunt);
        setBounds(4268, 80, 15, 1, hunt);
        setBounds(4269, 80, 45, 2, hunt);
        setBounds(358, 95, 75, 3, hunt);
        setChildren(4, summ);
        setBounds(29601, 20, 30, 0, summ);
        setBounds(4268, 80, 15, 1, summ);
        setBounds(4269, 80, 45, 2, summ);
        setBounds(358, 95, 75, 3, summ);
        setChildren(4, dung);
        setBounds(29602, 20, 30, 0, dung);
        setBounds(4268, 80, 15, 1, dung);
        setBounds(4269, 80, 45, 2, dung);
        setBounds(358, 95, 75, 3, dung);
    }

    public static void addSkillChatSprite(int id, int skill) {
        addSpriteLoader(id, 456 + skill);
    }

    public static void setChildren(int total, Widget i) {
        i.children = new int[total];
        i.childX = new int[total];
        i.childY = new int[total];
    }

    public static void setBounds(int ID, int X, int Y, int frame, Widget r) {
        r.children[frame] = ID;
        r.childX[frame] = X;
        r.childY[frame] = Y;
    }

    public static void addButton(int i, int j, String name, int W, int H, String S, int AT) {
        Widget RSInterface = addInterface(i);
        RSInterface.id = i;
        RSInterface.parent = i;
        RSInterface.type = 5;
        RSInterface.atActionType = AT;
        RSInterface.contentType = 0;
        RSInterface.opacity = 0;
        RSInterface.hoverType = 52;
        RSInterface.disabledSprite = imageLoader(j, name);
        RSInterface.enabledSprite = imageLoader(j, name);
        RSInterface.width = W;
        RSInterface.height = H;
        RSInterface.tooltip = S;
    }


    public static void addSprites(int ID, int i, int i2, String name, int configId, int configFrame) {
        Widget Tab = addTabInterface(ID);
        Tab.id = ID;
        Tab.parent = ID;
        Tab.type = 5;
        Tab.atActionType = 0;
        Tab.contentType = 0;
        Tab.width = 512;
        Tab.height = 334;
        Tab.opacity = 0;
        Tab.hoverType = -1;
        Tab.valueCompareType = new int[1];
        Tab.requiredValues = new int[1];
        Tab.valueCompareType[0] = 1;
        Tab.requiredValues[0] = configId;
        Tab.valueIndexArray = new int[1][3];
        Tab.valueIndexArray[0][0] = 5;
        Tab.valueIndexArray[0][1] = configFrame;
        Tab.valueIndexArray[0][2] = 0;
        Tab.disabledSprite = imageLoader(i, name);
        Tab.enabledSprite = imageLoader(i2, name);
    }

    public String[] tooltips;
    public boolean newScroller;
    public boolean drawInfinity;
    public boolean fancy = false;
    public int skillId = 0;
    public String skillName = "";
    public int intWidth = 0;
}
