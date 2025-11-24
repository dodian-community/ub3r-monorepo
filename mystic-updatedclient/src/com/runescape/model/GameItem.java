package com.runescape.model;

public class GameItem {

	public GameItem(int item, int amount) {
		this.item = item;
		this.amount = amount;
	}
	
	int item;
	int amount;

	public int getItem() {
		return item;
	}
	
	public int getAmount() {
		return amount;
	}
}
