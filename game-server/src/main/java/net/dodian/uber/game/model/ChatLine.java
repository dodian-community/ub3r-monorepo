package net.dodian.uber.game.model;

public class ChatLine {
  public String playerName = "", chat = "";
  public int dbId = -1, type = 0, absX = 0, absY = 0;
  public long timestamp = 0;

  public ChatLine(String playerName, int dbId, int type, String chat, int absX, int absY) {
    this.playerName = playerName;
    this.dbId = dbId;
    this.type = type;
    this.chat = chat;
    this.absX = absX;
    this.absY = absY;
    timestamp = System.currentTimeMillis();
  }
}