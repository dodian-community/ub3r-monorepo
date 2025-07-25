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
        try (java.sql.Connection conn = getDbConnection();
             Statement localStatement = conn.createStatement();
             ResultSet results = localStatement.executeQuery("SELECT * FROM " + DbTables.GAME_DOOR_DEFINITIONS)) {
            
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
            System.out.println("Loaded " + i + " doors...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}