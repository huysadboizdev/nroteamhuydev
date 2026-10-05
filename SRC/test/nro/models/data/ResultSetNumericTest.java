package nro.models.data;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

/** Real JDBC column types; only a connection-local temporary table is written. */
public final class ResultSetNumericTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("Config.properties")) { p.load(in); }
        Class.forName(p.getProperty("database.driver"));
        String url = "jdbc:mysql://" + p.getProperty("database.host") + ":"
                + p.getProperty("database.port") + "/" + p.getProperty("database.name")
                + "?useSSL=false&allowPublicKeyRetrieval=true";
        try (Connection c = DriverManager.getConnection(url, p.getProperty("database.user"), p.getProperty("database.pass", ""))) {
            Statement setup = c.createStatement();
            setup.execute("CREATE TEMPORARY TABLE login_numeric_test (id INT, wide BIGINT, small SMALLINT, tiny TINYINT, decval DECIMAL(10,2), realval FLOAT, doubleval DOUBLE, empty INT)");
            setup.execute("INSERT INTO login_numeric_test VALUES (7,5000000000,7,7,7.5,7.5,7.5,NULL)");
            setup.close();
            LocalResultSet r = new ResultSetImpl(c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
                    .executeQuery("SELECT * FROM login_numeric_test"));
            check(r.first(), "fixture row");
            check(r.getObject("id") instanceof Integer, "reproduce production INT player ID");
            check(r.getLong("id") == 7 && r.getLong(1) == 7, "login INT ID read as long");
            check(r.getLong("wide") == 5000000000L && r.getLong(2) == 5000000000L, "BIGINT preserved");
            String[] names = {"id", "small", "tiny", "decval", "realval", "doubleval", "empty"};
            int[] columns = {1, 3, 4, 5, 6, 7, 8};
            for (int i = 0; i < names.length; i++) {
                int expected = i == 6 ? 0 : 7;
                check(r.getInt(names[i]) == expected && r.getInt(columns[i]) == expected, "int " + names[i]);
                check(r.getLong(names[i]) == expected && r.getLong(columns[i]) == expected, "long " + names[i]);
                check(r.getByte(names[i]) == expected && r.getByte(columns[i]) == expected, "byte " + names[i]);
                check(r.getShort(names[i]) == expected && r.getShort(columns[i]) == expected, "short " + names[i]);
                double fraction = i >= 3 && i <= 5 ? 7.5 : expected;
                check(r.getFloat(names[i]) == fraction && r.getFloat(columns[i]) == fraction, "float " + names[i]);
                check(r.getDouble(names[i]) == fraction && r.getDouble(columns[i]) == fraction, "double " + names[i]);
            }
            r.dispose();
            LocalResultSet live = new ResultSetImpl(c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
                    .executeQuery("SELECT id FROM player LIMIT 1"));
            if (live.first()) check(live.getLong("id") == ((Number)live.getObject("id")).longValue(), "actual player ID login read");
            live.dispose();
        }
        System.out.println("PASS JDBC login ID and numeric getters: INT/BIGINT/SMALLINT/TINYINT/DECIMAL/FLOAT/DOUBLE/NULL, names and indexes");
    }
}
