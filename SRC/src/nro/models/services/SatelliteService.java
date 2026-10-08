package nro.models.services;

import java.util.List;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ItemMapService;
import nro.models.map.service.MapService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player.Satellite;
import nro.models.player_system.Template.ItemTemplate;

/** Satellite operations never acquire a player monitor while holding the zone monitor. */
public final class SatelliteService {
    public static final int MAX_PER_ZONE = 3;
    public static final long LIFETIME_MS = 30 * 60 * 1000L;

    private SatelliteService() {}

    private static boolean active(ItemMap item, long now) {
        ItemTemplate template = item == null ? null : item.itemTemplate;
        return template != null && template.type == 22
                && now - item.createTime < LIFETIME_MS;
    }

    public static boolean canPlace(Zone zone) {
        if (zone == null) return false;
        synchronized (zone) {
            long now = System.currentTimeMillis();
            int count = 0;
            for (ItemMap item : zone.items) if (active(item, now) && ++count >= MAX_PER_ZONE) return false;
            return true;
        }
    }

    public static boolean place(Player player, Item item) {
        if (player == null || item == null) return false;
        // Inventory mutation and map changes already use the player monitor.
        synchronized (player) {
            Zone zone = player.zone;
            if (zone == null || player.inventory == null || player.location == null || player.nPoint == null
                    || !player.inventory.itemsBag.contains(item) || item.quantity < 1
                    || item.template == null || item.template.type != 22
                    || item.template.id < 342 || item.template.id > 345
                    || item.template.strRequire > player.nPoint.power
                    || CommesonChallengeService.hasPendingId(player.id) || RubyGiftService.hasPending(player)) return false;
            synchronized (zone) {
                if (!canPlace(zone)) {
                    Service.gI().sendThongBaoOK(player, "Mỗi khu chỉ đặt được 3 Vệ Tinh");
                    return false;
                }
                // Publish, initialize owner/clan and consume as one operation relative to item snapshots.
                Service.gI().dropSatellite(player, item, zone, player.location.x, player.location.y);
                InventoryService.gI().subQuantityItemsBag(player, item, 1);
                return true;
            }
        }
    }

    private static void expire(ItemMap item, Zone zone) {
        synchronized (zone) {
            if (item.zone == zone && zone.items.contains(item)) {
                ItemMapService.gI().removeItemMapAndSendClient(item);
            }
        }
    }

    public static void update(ItemMap item) {
        Zone zone = item.zone;
        ItemTemplate template = item.itemTemplate;
        if (zone == null || template == null || template.type != 22) return;
        if (!active(item, System.currentTimeMillis())) {
            expire(item, zone);
            return;
        }
        // Snapshot outside player locks. Entry/exit may change the original list at any time.
        List<Player> players = zone.getPlayersSnapshot();
        for (Player player : players) {
            if (player == null) continue;
            synchronized (player) {
                if (player.zone != zone || item.zone != zone || item.itemTemplate != template
                        || !active(item, System.currentTimeMillis()) || player.nPoint == null
                        || player.location == null || player.satellite == null || player.isDie()
                        || nro.models.utils.Util.getDistance(player.location.x, player.location.y, item.x, item.y) >= 200
                        || !(player.id == item.playerId || item.clanId != -1
                            && player.clan != null && player.clan.id == item.clanId)) continue;
                Satellite effect = player.satellite;
                synchronized (effect) {
                    long now = System.currentTimeMillis();
                    switch (template.id) {
                        case 342 -> {
                            if (!effect.isMP) {
                                effect.isMP = true;
                                effect.lastMPTime = now;
                                int before = player.nPoint.mp;
                                player.nPoint.addMp(player.nPoint.mpMax / 10);
                                if (player.nPoint.mp != before) PlayerService.gI().sendInfoMp(player);
                            }
                        }
                        case 343 -> {
                            if (!effect.isIntelligent) {
                                effect.isIntelligent = true;
                                effect.lastIntelligentTime = now;
                            }
                        }
                        case 344 -> {
                            if (!effect.isDefend) {
                                effect.isDefend = true;
                                effect.lastDefendTime = now;
                            }
                        }
                        case 345 -> {
                            if (!effect.isHP) {
                                effect.isHP = true;
                                effect.lastHPTime = now;
                                int before = player.nPoint.hp;
                                player.nPoint.addHp(player.nPoint.hpMax / 10);
                                if (player.nPoint.hp != before) {
                                    PlayerService.gI().sendInfoHp(player);
                                    sendHp(player, zone, players);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!active(item, System.currentTimeMillis())) expire(item, zone);
    }

    private static void sendHp(Player player, Zone zone, List<Player> recipients) {
        // Existing -30/9 format updates HP/max without an animation byte or optional reads.
        // Keep offline maps private, matching Service.sendMessAnotherNotMeInMap.
        if (MapService.gI().isMapOffline(zone.map.mapId)) return;
        Message message = null;
        try {
            message = Service.gI().messageSubCommand((byte) 9);
            message.writer().writeInt((int) player.id);
            message.writer().writeInt(player.nPoint.hp);
            message.writer().writeInt(player.nPoint.hpMax);
            for (Player observer : recipients) {
                if (observer != null && observer != player && observer.zone == zone
                        && observer.isPl() && observer.getSession() != null) observer.sendMessage(message);
            }
        } catch (Exception error) {
            nro.models.utils.Logger.logException(SatelliteService.class, error);
        } finally {
            if (message != null) message.cleanup();
        }
    }
}
