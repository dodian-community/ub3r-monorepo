package net.dodian.uber.game;

import net.dodian.uber.game.model.object.RS2Object;
import net.dodian.utilities.DbTables;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public class ObjectLoaderService {

    public static ArrayList<RS2Object> objects = new ArrayList<>();

    public static void loadObjects() {
        try {
            Statement statement = getDbConnection().createStatement();
            ResultSet results = statement.executeQuery("SELECT * from " + DbTables.GAME_OBJECT_DEFINITIONS);
            while (results.next()) {
                objects.add(new RS2Object(results.getInt("id"), results.getInt("x"), results.getInt("y"), results.getInt("type")));
            }
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
