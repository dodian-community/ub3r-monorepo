package net.dodian.uber.game.model.player.casino;

public class Symbol {
  int id;
  int[] triggers;
  String symbol = "", color = "";

  public Symbol(int id, String symbol, int[] triggers) {
    this.id = id;
    this.symbol = symbol;
    this.triggers = triggers;
  }

  public boolean check(int stop) {
    for (int i : triggers) {
      if (i == stop)
        return true;
    }
    return false;
  }

  public String getSymbol() {
    return symbol;
  }

  public int getId() {
    return id;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public void setColor(String color) {
    this.color = "@" + color + "@";
  }

  public String getColor() {
    return color;
  }

  public String output() {
    return color + symbol;
  }
}
