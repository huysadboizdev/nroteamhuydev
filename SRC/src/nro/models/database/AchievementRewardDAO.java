package nro.models.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.sql.*;
import nro.models.data.LocalManager;

/** Wallet and claimed marker commit together; the marker makes retries idempotent. */
public class AchievementRewardDAO {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    public record Snapshot(long id, int account, int index, String inventory, String achievements) {}
    public record Result(boolean credited, String achievements) {}
    public static class CommitUncertain extends SQLException { public CommitUncertain(SQLException cause) { super("Achievement commit requires reconciliation", cause); } }
    private final Connections connections;
    public AchievementRewardDAO() { this(LocalManager::getConnection); }
    public AchievementRewardDAO(Connections connections) { this.connections = connections; }
    private static JsonArray array(String json) throws SQLException {
        try { return new JsonParser().parse(json == null || json.isBlank() ? "[]" : json).getAsJsonArray(); }
        catch (RuntimeException failure) { throw new SQLException("Invalid achievement/wallet data", failure); }
    }
    private static JsonArray row(JsonElement value) throws SQLException {
        try { return value.isJsonArray() ? value.getAsJsonArray() : array(value.getAsString()); }
        catch (RuntimeException failure) { throw new SQLException("Invalid achievement row", failure); }
    }
    private static JsonArray legacyRow(JsonElement value) {
        JsonArray repaired = new JsonArray(); long count = 0; boolean claimed = false;
        try {
            JsonArray original = row(value);
            try { if (original.size() > 1) claimed = original.get(1).getAsBoolean(); } catch (RuntimeException ignored) {}
            try { if (original.size() > 0) count = Math.max(0, original.get(0).getAsLong()); } catch (RuntimeException ignored) {}
            repaired.add(count); repaired.add(claimed);
            if (original.size() > 2 && original.get(2).isJsonArray()) repaired.add(original.get(2));
            return repaired;
        } catch (SQLException ignored) { repaired.add(count); repaired.add(claimed); return repaired; }
    }
    public Result claim(Snapshot snapshot) throws SQLException {
        boolean attempted = false;
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                JsonArray storedInventory, storedAchievements;
                try (PreparedStatement query = connection.prepareStatement("SELECT data_inventory,data_achievement FROM player WHERE id=? AND account_id=? FOR UPDATE")) {
                    query.setLong(1, snapshot.id()); query.setInt(2, snapshot.account());
                    try (ResultSet result = query.executeQuery()) {
                        if (!result.next()) throw new SQLException("Achievement player identity missing");
                        storedInventory = array(result.getString(1)); storedAchievements = array(result.getString(2));
                    }
                }
                if (snapshot.index() < 0 || snapshot.index() >= 20) throw new SQLException("Invalid achievement index");
                if (snapshot.index() < storedAchievements.size()) {
                    JsonArray existing = legacyRow(storedAchievements.get(snapshot.index()));
                    if (existing.size() > 1 && existing.get(1).getAsBoolean()) { connection.rollback(); return new Result(false, storedAchievements.toString()); }
                }
                JsonArray inventory = array(snapshot.inventory()), achievements = array(snapshot.achievements());
                if (storedInventory.size() < 3 || inventory.size() != 5 || achievements.size() != 20) throw new SQLException("Invalid achievement snapshot shape");
                for (int i = 0; i < 5; i++) { if (i < storedInventory.size()) storedInventory.set(i, inventory.get(i)); else storedInventory.add(inventory.get(i)); }
                // Never erase prior durable claims or counters when saving newer live progress.
                for (int i = 0; i < Math.min(storedAchievements.size(), achievements.size()); i++) {
                    JsonArray old = legacyRow(storedAchievements.get(i)), next = row(achievements.get(i));
                    if (old.size() > 1 && old.get(1).getAsBoolean()) next.set(1, old.get(1));
                    if (old.size() > 0 && old.get(0).getAsLong() > next.get(0).getAsLong()) next.set(0, old.get(0));
                    achievements.set(i, new com.google.gson.JsonPrimitive(next.toString()));
                }
                try (PreparedStatement update = connection.prepareStatement("UPDATE player SET data_inventory=?,data_achievement=? WHERE id=? AND account_id=?")) {
                    update.setString(1, storedInventory.toString()); update.setString(2, achievements.toString());
                    update.setLong(3, snapshot.id()); update.setInt(4, snapshot.account());
                    if (update.executeUpdate() != 1) throw new SQLException("Achievement player update failed");
                }
                attempted = true; connection.commit(); return new Result(true, achievements.toString());
            } catch (SQLException | RuntimeException failure) {
                if (attempted) throw new CommitUncertain(failure instanceof SQLException sql ? sql : new SQLException(failure));
                try { connection.rollback(); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                if (failure instanceof SQLException sql) throw sql;
                throw new SQLException("Achievement persistence failed", failure);
            }
        } catch (SQLException failure) {
            if (attempted && !(failure instanceof CommitUncertain)) throw new CommitUncertain(failure);
            throw failure;
        }
    }
}
