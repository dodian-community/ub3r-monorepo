package net.dodian.uber.game.model.object;

import net.dodian.utilities.DbTables;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class DoorHandler {
    public static int[] doorX = new int[100];
    public static int[] doorY = new int[100];
    public static int[] doorId = new int[100];
    public static int[] doorHeight = new int[100];
    public static int[] doorFaceOpen = new int[100];
    public static int[] doorFaceClosed = new int[100];
    public static int[] doorFace = new int[100];
    public static int[] doorState = new int[100];
    public static Statement statement;
    public static Connection conn;

    public DoorHandler() {
        try {
            mysql_connect();
            ResultSet results = statement.executeQuery("SELECT * FROM " + DbTables.GAME_DOOR_DEFINITIONS);
            int i = 0;
            while (results.next()) {
                doorX[i] = results.getInt("doorX");
                doorY[i] = results.getInt("doorY");
                doorId[i] = results.getInt("doorId");
                doorFaceOpen[i] = results.getInt("doorFaceOpen");
                doorFaceClosed[i] = results.getInt("doorFaceClosed");
                doorFace[i] = results.getInt("doorFace");
                doorState[i] = results.getInt("doorState");
                doorHeight[i] = results.getInt("doorHeight");
                i++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean mysql_connect() {
        try {
            conn = getDbConnection();
            statement = conn.createStatement();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}