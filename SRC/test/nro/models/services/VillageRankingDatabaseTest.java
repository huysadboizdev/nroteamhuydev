package nro.models.services;

import java.sql.*;
import java.util.List;
import nro.models.data.LocalManager;

/** Isolated rows exercise production SQL; the optional live read never mutates player saves. */
public final class VillageRankingDatabaseTest {
    private static void check(boolean value, String text) { if (!value) throw new AssertionError(text); }
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        if (args.length > 0 && args[0].equals("live")) {
            var loader = VillageRankingService.class.getDeclaredMethod("loadDatabase", VillageRankingService.Kind.class);
            loader.setAccessible(true);
            for (var kind : VillageRankingService.Kind.values()) {
                @SuppressWarnings("unchecked") List<VillageRankingService.Row> rows = (List<VillageRankingService.Row>) loader.invoke(null, kind);
                check(!rows.isEmpty() && rows.size() <= 100, "Live ranking projection missing rows");
                check(rows.equals(VillageRankingService.rank(rows, kind)), "Live numeric ordering");
                for (var row : rows) check(row.id() > 0 && !row.name().isEmpty(), "Live malformed row");
                System.out.println("VILLAGE_RANKING_LIVE_READ PASS " + kind + " rows=" + rows.size());
            }
            LocalManager.close(); return;
        }
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/team2026", "root", "")) {
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TEMPORARY TABLE player(id INT PRIMARY KEY,name VARCHAR(20),head INT,gender INT,data_point LONGTEXT,data_task LONGTEXT,items_body LONGTEXT)");
            }
            try (PreparedStatement s = c.prepareStatement("INSERT INTO player VALUES(?, ?, 64, 0, ?, ?, '[]')")) {
                for (int id = 1; id <= 106; id++) {
                    String power = "[0," + (id == 1 ? "10000000000" : id == 2 ? "90000000000" : Integer.toString(id)) + "]";
                    String task = id == 1 ? "[9,8,999,0]" : id == 2 ? "[10,0,0,0]" : "[1,0,0,0]";
                    if (id == 103) power = "malformed";
                    if (id == 104) power = "[0,-1]";
                    if (id == 105) power = "[0,9223372036854775808]";
                    if (id == 106) task = "malformed";
                    s.setInt(1, id); s.setString(2, "fixture"+id); s.setString(3, power); s.setString(4, task); s.addBatch();
                }
                s.executeBatch();
            }
            for (var kind : VillageRankingService.Kind.values()) {
                try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery(VillageRankingService.query(kind))) {
                    check(rs.next() && rs.getInt("id") == 2, "Numeric ordering before LIMIT");
                    if (kind == VillageRankingService.Kind.TASK) check(rs.next() && rs.getInt("id") == 1, "Task9 vs10 sorted as numbers");
                    int count = kind == VillageRankingService.Kind.TASK ? 2 : 1;
                    while (rs.next()) { check(rs.getInt("id") < 103, "Malformed or overflow row accepted"); count++; }
                    check(count == 100, "Production SQL must LIMIT100 valid rows");
                }
            }
        }
        System.out.println("VILLAGE_RANKING_DATABASE PASS numeric-order,task-tuple,invalid-json,negative,overflow,top100");
    }
}
