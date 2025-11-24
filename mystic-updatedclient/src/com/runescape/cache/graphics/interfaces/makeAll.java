package com.runescape.cache.graphics.interfaces;

import com.runescape.cache.graphics.GameFont;
import com.runescape.cache.graphics.Widget;

public class makeAll extends Widget {
	
	public static int quanid = 35204;
	public static int itemid = 35304;
	public static int quanchild = 0;
	public static int itemchild = 0;
	public static int x;
	public static int itemx;
	public static int centerx = 70;
	public static float amount;

	public static void makeAll(GameFont[] tda) {
		Widget inter = addTabInterface(35100);
		Widget quantity = addTabInterface(35200);
		Widget item = addTabInterface(35300);
		inter.totalChildren(4);
		quantity.totalChildren(15);
		item.totalChildren(9);
		addText(35101, "How many?", tda, 2, 0x403020, true, false);
		addText(35102, "Choose a quantity, then click an image to begin.", tda, 0, 0x605048, true, false);
		addQuantity(tda, "1", quantity, true);
		addQuantity(tda, "5", quantity, true);
		addQuantity(tda, "10", quantity, true);
		addQuantity(tda, "X", quantity, true);
		addQuantity(tda, "All", quantity, true);
		addItem(item);
		addItem(item);
		addItem(item);
		int child = 0;
		inter.child(child++, 35101, 150, 0);
		inter.child(child++, 35102, 150, 17);
		inter.child(child++, 35200, 0, 0);
		inter.child(child++, 35300, 0, 0);
	}
	
	public static void addQuantity(GameFont[] tda, String text, Widget quan, boolean enabled) {
		if(enabled) {
			addButton(quanid, 35200, 35, 30, 494, 494, quanid + 1, "Select");
		} else {
			addSprite(quanid, 495);
		}
		addHoveredButton_sprite_loader(quanid + 1, 495, 35, 30, quanid + 2);
		addText(quanid + 3, text, tda, 0, 0x403020, true, false);
		quan.child(quanchild++, quanid, 285 + x, 0);
		quan.child(quanchild++, quanid + 1, 285 + x, 0);
		quan.child(quanchild++, quanid + 3, 302 + x, 9);
		quanid += 4;
		x += 40;
	}
	public static void addItem(Widget item) {
		addModel(itemid + 3);
		addButton(itemid, 35300, 100, 75, 492, 492, itemid + 1, "Select");
		addHoveredButton_sprite_loader(itemid + 1, 493, 100, 75, itemid + 2);
			item.child(itemchild++, itemid, centerx + itemx, 35);
			item.child(itemchild++, itemid + 1, centerx +  itemx, 35);
			item.child(itemchild++, itemid + 3, centerx + 55 + itemx, 72);
		itemid += 4;
		itemx += 120;
		
	}
}