package nro.models.services;

import java.sql.*;

/** Settings writes participate in the command/receipt transaction. */
public final class LiveSettingsDAO {
    public record ExpSetting(int rate,long version) {}
    private LiveSettingsDAO() {}
    public static ExpSetting readExp(Connection c)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT setting_value,version FROM server_runtime_setting WHERE setting_key='exp_rate'");ResultSet r=q.executeQuery()) {
            if(!r.next())return null;
            int rate=Integer.parseInt(r.getString("setting_value"));
            if(rate<1||rate>100)throw new IllegalArgumentException("Persisted EXP rate must be 1..100");
            return new ExpSetting(rate,r.getLong("version"));
        }
    }
    public static int loadExpRate(Connection c,int fallback)throws SQLException {
        ExpSetting setting=readExp(c);return setting==null?fallback:setting.rate();
    }
    public static ExpSetting storeExp(Connection c,int rate)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("INSERT INTO server_runtime_setting(setting_key,setting_value,version) VALUES('exp_rate',?,1) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value),version=version+1")) {
            q.setString(1,Integer.toString(rate));q.executeUpdate();
        }
        return readExp(c);
    }
}
