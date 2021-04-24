package net.dodian.utilities;

import net.dodian.uber.game.Server;

import java.sql.*;

public class Database {
  public static Connection conn = null;
  public static Statement statement = null;

  public static void init() {
    try {
      if (conn == null) {
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(Server.MySQLURL, Server.MySQLUser, Server.MySQLPassword);
        statement = conn.createStatement();
      } else {
        conn.close();
        if (statement != null)
          statement.close();
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(Server.MySQLURL, Server.MySQLUser, Server.MySQLPassword);
        statement = conn.createStatement();
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void mysql_error(String error) {
    System.out.println(error);
  }
}