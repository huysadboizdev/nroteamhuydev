package nro.models.shop_ky_gui;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import nro.models.data.LocalManager;
import org.json.simple.JSONValue;

/**
 *
 * @author By Mr Blue
 * 
 */

public class ConsignShopManager {

    private static ConsignShopManager instance;

    public static ConsignShopManager gI() {
        if (instance == null) {
            instance = new ConsignShopManager();
        }
        return instance;
    }

    public long lastTimeUpdate;

    public String[] tabName = {"Áo Quần", "Găng, Giày, Rada", "Phụ Kiện", "Linh tinh", ""};

    public List<ConsignItem> listItem = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Every operation persists atomically through ConsignDAO; shutdown has nothing to flush. */
    public void save() { }
}
