package nro.models.map;

import java.util.ArrayList;
import java.util.List;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.item.Item.ItemOption;
import nro.models.player.Player;
import nro.models.utils.Util;
import nro.models.map.service.ItemMapService;
import nro.models.services.ItemService;
import nro.models.map.service.MapService;
import nro.models.services.Service;

public class ItemMap {

    public final boolean bossAdminSuppressed;
    public volatile Zone zone;
    public int itemMapId;
    public volatile ItemTemplate itemTemplate;
    public int quantity;

    public int x;
    public int y;
    public long playerId;
    public boolean keepOwnerUntilDespawn;
    public String commesonMatchId;
    public List<ItemOption> options;

    public long createTime;

    public int clanId = -1;

    public boolean isBlackBall;
    public boolean isNamecBall;
    public boolean isPickedUp;
    public String id;

    public ItemMap(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
        this(zone, tempId, quantity, x, y, playerId, false);
    }

    public ItemMap(Zone zone, int tempId, int quantity, int x, int y, long playerId, boolean keepOwnerUntilDespawn) {
        this(zone, tempId, quantity, x, y, playerId, keepOwnerUntilDespawn, List.of());
    }

    /** Installs independent options before addItem exposes this drop to pickup or zone snapshots. */
    public ItemMap(Zone zone, int tempId, int quantity, int x, int y, long playerId,
            boolean keepOwnerUntilDespawn, List<ItemOption> initialOptions) {
        this.bossAdminSuppressed = nro.models.services.BossAdminDropService.suppressConstruction(tempId);
        this.zone = zone;
        this.itemMapId = allocateItemMapId(zone);
        this.itemTemplate = ItemService.gI().getTemplate((short) tempId);
        this.quantity = quantity;
        this.x = x;
        this.y = y;
        this.keepOwnerUntilDespawn = keepOwnerUntilDespawn;
        this.playerId = keepOwnerUntilDespawn ? playerId : playerId != -1 ? Math.abs(playerId) : playerId;
        this.createTime = System.currentTimeMillis();
        this.options = new ArrayList<>();
        for (ItemOption option : initialOptions) this.options.add(new ItemOption(option));
        this.isBlackBall = ItemMapService.gI().isBlackBall(this.itemTemplate.id);
        this.isNamecBall = ItemMapService.gI().isNamecBall(this.itemTemplate.id);
        this.lastTimeMoveToPlayer = System.currentTimeMillis();
        if (!bossAdminSuppressed) this.zone.addItem(this);
    }

    public ItemMap(Zone zone, ItemTemplate temp, int quantity, int x, int y, long playerId) {
        this.bossAdminSuppressed = nro.models.services.BossAdminDropService.suppressConstruction(temp.id);
        this.zone = zone;
        this.itemMapId = allocateItemMapId(zone);
        this.itemTemplate = temp;
        this.quantity = quantity;
        this.x = x;
        this.y = y;
        this.playerId = playerId != -1 ? Math.abs(playerId) : playerId;
        this.createTime = System.currentTimeMillis();
        this.options = new ArrayList<>();
        this.isBlackBall = ItemMapService.gI().isBlackBall(this.itemTemplate.id);
        this.isNamecBall = ItemMapService.gI().isNamecBall(this.itemTemplate.id);
        this.lastTimeMoveToPlayer = System.currentTimeMillis();
        if (!bossAdminSuppressed) this.zone.addItem(this);
    }

    private static int allocateItemMapId(Zone zone) {
        synchronized (zone) {
            int id = zone.countItemAppeaerd++;
            if (zone.countItemAppeaerd >= 2000000000) zone.countItemAppeaerd = 0;
            return id;
        }
    }

    public ItemMap(ItemMap itemMap) {
        this.bossAdminSuppressed = itemMap.bossAdminSuppressed || nro.models.services.BossAdminDropService.suppressConstruction(itemMap.itemTemplate.id);
        this.zone = itemMap.zone;
        this.itemMapId = itemMap.itemMapId;
        this.itemTemplate = itemMap.itemTemplate;
        this.quantity = itemMap.quantity;
        this.x = itemMap.x;
        this.y = itemMap.y;
        this.playerId = itemMap.playerId;
        this.keepOwnerUntilDespawn = itemMap.keepOwnerUntilDespawn;
        this.commesonMatchId = itemMap.commesonMatchId;
        this.options = itemMap.options;
        this.isBlackBall = itemMap.isBlackBall;
        this.isNamecBall = itemMap.isNamecBall;
        this.lastTimeMoveToPlayer = itemMap.lastTimeMoveToPlayer;
        this.createTime = System.currentTimeMillis();
        if (!bossAdminSuppressed) this.zone.addItem(this);
    }

    public void update() {
        if (isNotNullItem()) {
            if (this.isBlackBall) {
                if (Util.canDoWithTime(lastTimeMoveToPlayer, timeMoveToPlayer)) {
                    if (this.zone != null && !this.zone.getPlayers().isEmpty()) {
                        Player player = this.zone.getPlayers().get(0);
                        if (player.zone != null && player.zone.equals(this.zone)) {
                            this.x = player.location.x;
                            this.y = this.zone.map.yPhysicInTop(this.x, player.location.y - 24);
                            reAppearItem();
                            this.lastTimeMoveToPlayer = System.currentTimeMillis();
                        }
                    }
                }
                return;
            }

            //========================SATELLITE========================
            if (this.itemTemplate.type == 22) {
                nro.models.services.SatelliteService.update(this);
                return;
            }
            if (!keepOwnerUntilDespawn && Util.canDoWithTime(createTime, 45000)) {
                if (this.itemTemplate.type != 22 && this.itemTemplate.id != 726 && this.itemTemplate.id != 992) {
                    this.playerId = -1;
                }
            }
            if ((Util.canDoWithTime(createTime, commesonMatchId == null ? 50000 : 60000) && isNotNullItem() && itemTemplate.type != 22 || Util.canDoWithTime(createTime, 1800000)) && !this.isNamecBall) {
                if (this.zone != null && this.zone.map.mapId != 21 && this.zone.map.mapId != 22
                        && this.zone.map.mapId != 23 && this.itemTemplate.id != 78
                        && this.itemTemplate.id != 726 && !(MapService.gI().isMapDoanhTrai(this.zone.map.mapId) && this.itemTemplate.id >= 14 && this.itemTemplate.id <= 20)) {
                    ItemMapService.gI().removeItemMapAndSendClient(this);
                }
            }
            //========================DHVT ITEM 726========================
            if (this.zone != null && this.zone.map.mapId == 52 && isNotNullItem() && this.itemTemplate.id == 726) {
                if (!findPlayerByID(this.playerId)) {
                    ItemMapService.gI().removeItemMapAndSendClient(this);
                }
            }
            if (this.zone != null && isNotNullItem() && this.itemTemplate.id == 460 && this.playerId == 123456789 && Util.canDoWithTime(createTime, 5000)) {
                ItemMapService.gI().removeItemMapAndSendClient(this);
            }
        }
    }

    private boolean findPlayerByID(long id) {
        for (Player pl : this.zone.getPlayers()) {
            if (pl.id == id) {
                return true;
            }
        }
        return false;
    }

    private final int timeMoveToPlayer = 10000;
    private long lastTimeMoveToPlayer;

    private void reAppearItem() {
        ItemMapService.gI().sendItemMapDisappear(this);
        Service.gI().dropItemMap(this.zone, this);
    }

    public boolean isNotNullItem() {
        return itemTemplate != null;
    }

    public void dispose() {
        this.zone = null;
        this.itemTemplate = null;
        this.options = null;
    }
}
