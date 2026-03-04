package net.dodian.uber.game.persistence;

import net.dodian.uber.game.model.item.GameItem;
import net.dodian.uber.game.runtime.loop.GameThreadTaskQueue;
import net.dodian.utilities.DbTables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.dodian.utilities.DatabaseKt.getDbConnection;

public final class CommandDbService {

    private CommandDbService() {
    }

    public static <T> void submit(
            String taskName,
            ThrowingSupplier<T> work,
            Consumer<T> onGameThread,
            Consumer<Exception> onGameThreadError
    ) {
        if (work == null) {
            return;
        }
        DbDispatchers.commandExecutor.execute(() -> {
            try {
                T result = work.get();
                if (onGameThread != null) {
                    GameThreadTaskQueue.submit(() -> onGameThread.accept(result));
                }
            } catch (Exception exception) {
                if (onGameThreadError != null) {
                    GameThreadTaskQueue.submit(() -> onGameThreadError.accept(exception));
                }
            }
        });
    }

    public static OfflineContainerViewResult loadOfflineContainerView(String playerName, String columnName) throws Exception {
        String containerColumn = validateContainerColumn(columnName);
        int userId;
        try (Connection connection = getDbConnection()) {
            userId = loadUserId(connection, playerName);
            if (userId < 0) {
                return OfflineContainerViewResult.usernameNotFound(playerName);
            }

            String query = "SELECT " + containerColumn + " FROM " + DbTables.GAME_CHARACTERS + " WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, userId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        return OfflineContainerViewResult.characterNotFound(playerName);
                    }
                    return OfflineContainerViewResult.ready(playerName, toGameItems(parseContainerEntries(results.getString(containerColumn))));
                }
            }
        }
    }

    public static OfflineSkillMutationResult removeOfflineExperience(String playerName, String skillName, int xp) throws Exception {
        String skillColumn = validateSkillColumn(skillName);
        try (Connection connection = getDbConnection()) {
            int userId = loadUserId(connection, playerName);
            if (userId < 0) {
                return OfflineSkillMutationResult.notFound(playerName, skillColumn);
            }

            String select = "SELECT " + skillColumn + ", totalxp, total FROM " + DbTables.GAME_CHARACTERS_STATS + " WHERE uid = ?";
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setInt(1, userId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        return OfflineSkillMutationResult.notFound(playerName, skillColumn);
                    }

                    SkillMutationComputation computation = computeSkillMutation(
                            results.getInt(skillColumn),
                            results.getInt("totalxp"),
                            results.getInt("total"),
                            xp
                    );

                    String update = "UPDATE " + DbTables.GAME_CHARACTERS_STATS +
                            " SET " + skillColumn + " = ?, totalxp = ?, total = ? WHERE uid = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(update)) {
                        updateStatement.setInt(1, computation.getNewXp());
                        updateStatement.setInt(2, computation.getNewTotalXp());
                        updateStatement.setInt(3, computation.getNewTotalLevel());
                        updateStatement.setInt(4, userId);
                        updateStatement.executeUpdate();
                    }
                    return OfflineSkillMutationResult.ready(playerName, skillColumn, computation.getCurrentXp(), computation.getRemovedXp());
                }
            }
        }
    }

    public static OfflineItemRemovalResult removeOfflineItems(String playerName, int itemId, int amount) throws Exception {
        try (Connection connection = getDbConnection()) {
            int userId = loadUserId(connection, playerName);
            if (userId < 0) {
                return OfflineItemRemovalResult.notFound(playerName, itemId);
            }

            String query = "SELECT bank, inventory, equipment FROM " + DbTables.GAME_CHARACTERS + " WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setInt(1, userId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        return OfflineItemRemovalResult.notFound(playerName, itemId);
                    }

                    int remaining = amount;
                    int totalRemoved = 0;
                    ContainerMutation bankMutation = applyItemRemoval(results.getString("bank"), itemId, remaining);
                    remaining = bankMutation.getRemainingAmount();
                    totalRemoved += bankMutation.getRemovedAmount();

                    ContainerMutation inventoryMutation = applyItemRemoval(results.getString("inventory"), itemId, remaining);
                    remaining = inventoryMutation.getRemainingAmount();
                    totalRemoved += inventoryMutation.getRemovedAmount();

                    ContainerMutation equipmentMutation = applyItemRemoval(results.getString("equipment"), itemId, remaining);
                    totalRemoved += equipmentMutation.getRemovedAmount();

                    String update = "UPDATE " + DbTables.GAME_CHARACTERS +
                            " SET equipment = ?, inventory = ?, bank = ? WHERE id = ?";
                    try (PreparedStatement updateStatement = connection.prepareStatement(update)) {
                        updateStatement.setString(1, equipmentMutation.getUpdatedText());
                        updateStatement.setString(2, inventoryMutation.getUpdatedText());
                        updateStatement.setString(3, bankMutation.getUpdatedText());
                        updateStatement.setInt(4, userId);
                        updateStatement.executeUpdate();
                    }

                    return OfflineItemRemovalResult.ready(playerName, itemId, totalRemoved);
                }
            }
        }
    }

    public static CommandWriteResult updateRank(String playerName, int rankId) throws Exception {
        String query = "UPDATE " + DbTables.WEB_USERS_TABLE + " SET usergroupid = ? WHERE username = ?";
        try (Connection connection = getDbConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, rankId);
            statement.setString(2, playerName);
            return new CommandWriteResult(statement.executeUpdate());
        }
    }

    public static CommandWriteResult deleteNpcDrop(int npcId, int itemId, double chance) throws Exception {
        String query = "DELETE FROM " + DbTables.GAME_NPC_DROPS + " WHERE npcid = ? AND itemid = ? AND percent = ?";
        try (Connection connection = getDbConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, npcId);
            statement.setInt(2, itemId);
            statement.setDouble(3, chance);
            return new CommandWriteResult(statement.executeUpdate());
        }
    }

    public static CommandWriteResult insertNpcDrop(
            int npcId,
            double chance,
            int itemId,
            int minAmount,
            int maxAmount,
            String rareShout
    ) throws Exception {
        String query = "INSERT INTO " + DbTables.GAME_NPC_DROPS +
                " (npcid, percent, itemid, amt_min, amt_max, rareShout) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getDbConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, npcId);
            statement.setDouble(2, chance);
            statement.setInt(3, itemId);
            statement.setInt(4, minAmount);
            statement.setInt(5, maxAmount);
            statement.setString(6, rareShout);
            return new CommandWriteResult(statement.executeUpdate());
        }
    }

    public static CommandWriteResult insertObjectDefinition(int id, int x, int y, int type) throws Exception {
        String query = "INSERT INTO " + DbTables.GAME_OBJECT_DEFINITIONS + " (id, x, y, type) VALUES (?, ?, ?, ?)";
        try (Connection connection = getDbConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, type);
            return new CommandWriteResult(statement.executeUpdate());
        }
    }

    private static int loadUserId(Connection connection, String playerName) throws Exception {
        String query = "SELECT userid FROM " + DbTables.WEB_USERS_TABLE + " WHERE username = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, playerName);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getInt("userid") : -1;
            }
        }
    }

    static ArrayList<ContainerEntry> parseContainerEntries(String text) {
        ArrayList<ContainerEntry> items = new ArrayList<>();
        if (text == null || text.length() <= 2) {
            return items;
        }
        for (String line : text.split(" ")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("-");
            if (parts.length < 3) {
                continue;
            }
            items.add(new ContainerEntry(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
        }
        return items;
    }

    private static ArrayList<GameItem> toGameItems(ArrayList<ContainerEntry> entries) {
        ArrayList<GameItem> items = new ArrayList<>(entries.size());
        for (ContainerEntry entry : entries) {
            items.add(new GameItem(entry.getItemId(), entry.getAmount()));
        }
        return items;
    }

    static ContainerMutation applyItemRemoval(String text, int itemId, int amountToRemove) {
        if (text == null || text.length() <= 2) {
            return new ContainerMutation("", 0, amountToRemove);
        }
        StringBuilder builder = new StringBuilder();
        int remaining = amountToRemove;
        int removed = 0;
        for (String line : text.split(" ")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("-");
            if (parts.length < 3) {
                continue;
            }
            int parsedItemId = Integer.parseInt(parts[1]);
            int parsedAmount = Integer.parseInt(parts[2]);
            if (parsedItemId == itemId && remaining > 0) {
                int canRemove = Math.min(parsedAmount, remaining);
                int newAmount = parsedAmount - canRemove;
                remaining -= canRemove;
                removed += canRemove;
                if (newAmount > 0) {
                    builder.append(parts[0]).append("-").append(parts[1]).append("-").append(newAmount).append(" ");
                }
            } else {
                builder.append(parts[0]).append("-").append(parts[1]).append("-").append(parts[2]).append(" ");
            }
        }
        return new ContainerMutation(builder.toString(), removed, remaining);
    }

    static SkillMutationComputation computeSkillMutation(int currentXp, int totalXp, int totalLevel, int requestedXp) {
        int removedXp = Math.min(currentXp, requestedXp);
        int newXp = currentXp - removedXp;
        int newTotalXp = totalXp - removedXp;
        int newTotalLevel = totalLevel -
                net.dodian.uber.game.model.player.skills.Skills.getLevelForExperience(currentXp) +
                net.dodian.uber.game.model.player.skills.Skills.getLevelForExperience(newXp);
        return new SkillMutationComputation(currentXp, removedXp, newXp, newTotalXp, newTotalLevel);
    }

    private static String validateSkillColumn(String skillName) {
        if (skillName == null || !skillName.matches("[A-Za-z_]+")) {
            throw new IllegalArgumentException("Invalid skill column: " + skillName);
        }
        return skillName;
    }

    private static String validateContainerColumn(String columnName) {
        if (!"inventory".equals(columnName) && !"bank".equals(columnName)) {
            throw new IllegalArgumentException("Invalid container column: " + columnName);
        }
        return columnName;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    static final class ContainerMutation {
        private final String updatedText;
        private final int removedAmount;
        private final int remainingAmount;

        ContainerMutation(String updatedText, int removedAmount, int remainingAmount) {
            this.updatedText = updatedText;
            this.removedAmount = removedAmount;
            this.remainingAmount = remainingAmount;
        }

        public String getUpdatedText() {
            return updatedText;
        }

        public int getRemovedAmount() {
            return removedAmount;
        }

        public int getRemainingAmount() {
            return remainingAmount;
        }
    }

    static final class ContainerEntry {
        private final int itemId;
        private final int amount;

        ContainerEntry(int itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        public int getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }
    }

    static final class SkillMutationComputation {
        private final int currentXp;
        private final int removedXp;
        private final int newXp;
        private final int newTotalXp;
        private final int newTotalLevel;

        SkillMutationComputation(int currentXp, int removedXp, int newXp, int newTotalXp, int newTotalLevel) {
            this.currentXp = currentXp;
            this.removedXp = removedXp;
            this.newXp = newXp;
            this.newTotalXp = newTotalXp;
            this.newTotalLevel = newTotalLevel;
        }

        public int getCurrentXp() {
            return currentXp;
        }

        public int getRemovedXp() {
            return removedXp;
        }

        public int getNewXp() {
            return newXp;
        }

        public int getNewTotalXp() {
            return newTotalXp;
        }

        public int getNewTotalLevel() {
            return newTotalLevel;
        }
    }

    public static final class OfflineContainerViewResult {
        public enum Status {
            READY,
            USERNAME_NOT_FOUND,
            CHARACTER_NOT_FOUND
        }

        private final Status status;
        private final String playerName;
        private final ArrayList<GameItem> items;

        private OfflineContainerViewResult(Status status, String playerName, ArrayList<GameItem> items) {
            this.status = status;
            this.playerName = playerName;
            this.items = items;
        }

        public static OfflineContainerViewResult ready(String playerName, ArrayList<GameItem> items) {
            return new OfflineContainerViewResult(Status.READY, playerName, items);
        }

        public static OfflineContainerViewResult usernameNotFound(String playerName) {
            return new OfflineContainerViewResult(Status.USERNAME_NOT_FOUND, playerName, new ArrayList<>());
        }

        public static OfflineContainerViewResult characterNotFound(String playerName) {
            return new OfflineContainerViewResult(Status.CHARACTER_NOT_FOUND, playerName, new ArrayList<>());
        }

        public Status getStatus() {
            return status;
        }

        public String getPlayerName() {
            return playerName;
        }

        public ArrayList<GameItem> getItems() {
            return items;
        }
    }

    public static final class OfflineSkillMutationResult {
        public enum Status {
            READY,
            NOT_FOUND
        }

        private final Status status;
        private final String playerName;
        private final String skillName;
        private final int currentXp;
        private final int removedXp;

        private OfflineSkillMutationResult(Status status, String playerName, String skillName, int currentXp, int removedXp) {
            this.status = status;
            this.playerName = playerName;
            this.skillName = skillName;
            this.currentXp = currentXp;
            this.removedXp = removedXp;
        }

        public static OfflineSkillMutationResult ready(String playerName, String skillName, int currentXp, int removedXp) {
            return new OfflineSkillMutationResult(Status.READY, playerName, skillName, currentXp, removedXp);
        }

        public static OfflineSkillMutationResult notFound(String playerName, String skillName) {
            return new OfflineSkillMutationResult(Status.NOT_FOUND, playerName, skillName, 0, 0);
        }

        public Status getStatus() {
            return status;
        }

        public String getPlayerName() {
            return playerName;
        }

        public String getSkillName() {
            return skillName;
        }

        public int getCurrentXp() {
            return currentXp;
        }

        public int getRemovedXp() {
            return removedXp;
        }
    }

    public static final class OfflineItemRemovalResult {
        public enum Status {
            READY,
            NOT_FOUND
        }

        private final Status status;
        private final String playerName;
        private final int itemId;
        private final int totalItemRemoved;

        private OfflineItemRemovalResult(Status status, String playerName, int itemId, int totalItemRemoved) {
            this.status = status;
            this.playerName = playerName;
            this.itemId = itemId;
            this.totalItemRemoved = totalItemRemoved;
        }

        public static OfflineItemRemovalResult ready(String playerName, int itemId, int totalItemRemoved) {
            return new OfflineItemRemovalResult(Status.READY, playerName, itemId, totalItemRemoved);
        }

        public static OfflineItemRemovalResult notFound(String playerName, int itemId) {
            return new OfflineItemRemovalResult(Status.NOT_FOUND, playerName, itemId, 0);
        }

        public Status getStatus() {
            return status;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getItemId() {
            return itemId;
        }

        public int getTotalItemRemoved() {
            return totalItemRemoved;
        }
    }

    public static final class CommandWriteResult {
        private final int updatedRows;

        public CommandWriteResult(int updatedRows) {
            this.updatedRows = updatedRows;
        }

        public int getUpdatedRows() {
            return updatedRows;
        }
    }
}
