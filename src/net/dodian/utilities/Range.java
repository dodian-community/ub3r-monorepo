package net.dodian.utilities;

/**
 * 
 * @author Dashboard
 *
 */
public class Range {

  private int floor, ceiling;

  public Range(int floor, int ceiling) {
    this.floor = floor;
    this.ceiling = ceiling;
  }

  public int getFloor() {
    return this.floor;
  }

  public int getCeiling() {
    return this.ceiling;
  }

  public int getValue() {
    return floor + (int) (Math.random() * (ceiling - floor + 1));
  }

}
