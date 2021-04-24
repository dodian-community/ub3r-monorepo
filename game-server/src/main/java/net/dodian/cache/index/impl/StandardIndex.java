package net.dodian.cache.index.impl;

import net.dodian.cache.index.Index;

/**
 * A standard index where each index maps to a single file.
 * 
 * @author Graham Edgecombe
 * 
 */
public class StandardIndex extends Index {

  /**
   * The file.
   */
  private int file;

  /**
   * Creates the index.
   * 
   * @param identifier
   *          The identifier.
   * @param file
   *          The file.
   */
  public StandardIndex(int identifier, int file) {
    super(identifier);
    this.file = file;
  }

  /**
   * Gets the file.
   * 
   * @return The file.
   */
  public int getFile() {
    return file;
  }

}
