package nro.models.map;

import nro.models.consts.ConstTask;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.item.Item;
import nro.models.mob.Mob;
import nro.models.npc.Npc;
import nro.models.map.service.NpcManager;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.boss.luyen_tap_tu_dong.TrainingBoss;
import nro.models.consts.ConstMob;
import nro.models.map.service.ItemMapService;
import nro.models.services.ItemService;
import nro.models.map.service.MapService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.services.InventoryService;
import nro.models.utils.FileIO;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import nro.models.Bot.BotAttackplayer;
import nro.models.mob_bigboss.GauTuongCuop;
import nro.models.npc.*;
import nro.models.player.Pet;

public class Zone {

    public static final byte PLAYERS_TIEU_CHUAN_TRONG_MAP = 7;
    public final List<BotAttackplayer> bots = new ArrayList<>();

    public int countItemAppeaerd = 0;

    public Map map;
    public int zoneId;
    public int maxPlayer;
    // Separate from the zone monitor used for items; moving a held ball may touch the old zone.
    public final Object playerAdmissionLock = new Object();
    /** Clan dungeon admission stays closed after disposal until the next valid opening. */
    public volatile boolean dungeonClosing;
    public int shenronType = -1;

    @Getter
    private final List<Player> nonInteractiveNPCs; //npc
    private final List<Player> humanoids; //player, boss, pet
    private final List<Player> notBosses; //player, pet
    public final List<Player> players; //player
    @Getter
    private final List<Player> bosses; //boss
    private final List<Player> pets; //pet

    public final List<Mob> mobs;
    public final List<ItemMap> items;

    public long lastTimeDropBlackBall;
    public boolean finishBlackBallWar;
    public boolean finishMapMaBu;

    public boolean isbulon1Alive = true;
    public boolean isbulon2Alive = true;
    public boolean isTUTAlive = true;
    public boolean isGoldenFriezaAlive;

    public boolean isCompeting;
    public String rankName1;
    public String rankName2;
    public int rank1;
    public int rank2;

    public List<TrapMap> trapMaps;
    public List<MaBuHold> maBuHolds;

    @Setter
    @Getter
    public Player Npc;
    public int mapId;
    private int nextMobId = 1000;

    public boolean isFullPlayer() {
        return this.players.size() >= this.maxPlayer;
    }

    public void addBot(BotAttackplayer bot) {
        if (bot == null) {
            return;
        }

        synchronized (bots) {
            if (!bots.contains(bot)) {
                bots.add(bot);
                bot.zone = this;
            }
        }

        for (Player player : players) {
            if (player != null && !player.isDie() && player.session != null) {
                Service.gI().sendAppear(bot, player);
            }
        }
    }

    public List<BotAttackplayer> getBots() {
        synchronized (bots) {
            return new ArrayList<>(bots);
        }
    }

    public void removeBot(BotAttackplayer bot) {
        if (bot == null) {
            return;
        }

        synchronized (bots) {
            bots.remove(bot);
        }

        for (Player player : players) {
            if (player != null) {
                Service.gI().sendDisappear(bot, player);
            }
        }
    }

    private void udMob() {
        for (int i = this.mobs.size() - 1; i >= 0; i--) {
            try {
                Mob mob = mobs.get(i);
                if (map.mapId != 126 || !(mob instanceof nro.models.mob_bigboss.Hirudegarn)) mob.update();
            } catch (Exception e) {
                Logger.logException(Zone.class, e, "Lỗi update mobs");
            }
        }
    }

    private void udNonInteractiveNPC() {
        if (nonInteractiveNPCs.isEmpty()) {
            return;
        }
        try {
            List<Player> npcList = new ArrayList<>(nonInteractiveNPCs);
            for (int i = npcList.size() - 1; i >= 0; i--) {
                Player pl = npcList.get(i);
                if (pl != null && pl.zone != null) {
                    pl.update();
                }
            }
        } catch (Exception e) {
            Logger.logException(Zone.class, e, "Lỗi update non-interactive NPCs");
        }
    }

    private void udItem() {
        List<ItemMap> snapshot = getItemsSnapshot();
        for (int i = snapshot.size() - 1; i >= 0; i--) {
            ItemMap item = snapshot.get(i);
            try {
                if (item != null && item.itemTemplate != null) item.update();
                else removeItemMap(item);
            } catch (Exception e) { Logger.logException(Zone.class, e, "Lỗi update item"); }
        }
    }

    public void update() {
        try (nro.models.services.ServerMaintenanceGate.Lease permit = nro.models.services.ServerMaintenanceGate.enter()) {
            if (!permit.allowed()) return;
            updateActive();
        }
    }

    private void updateActive() {
        udMob();
        udItem();
        udPlayer();
        udNonInteractiveNPC();
    }

    public Zone(Map map, int zoneId, int maxPlayer) {
        this.map = map;
        this.zoneId = zoneId;
        this.maxPlayer = maxPlayer;
        this.nonInteractiveNPCs = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.humanoids = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.notBosses = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.players = map.mapId == 126 ? new java.util.concurrent.CopyOnWriteArrayList<>()
                : java.util.Collections.synchronizedList(new ArrayList<>());
        this.bosses = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.pets = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.mobs = new ArrayList<>();
        this.items = map.mapId == 126 ? new java.util.concurrent.CopyOnWriteArrayList<>() : new ArrayList<>();
        this.trapMaps = new ArrayList<>();
        this.maBuHolds = new ArrayList<>();
    }

    public List<Player> getPlayers() { return map.mapId == 126 ? new ArrayList<>(players) : players; }
    public List<Player> getPlayersSnapshot() {
        synchronized (players) { return new ArrayList<>(players); }
    }

    public synchronized List<ItemMap> getItemsSnapshot() { return new ArrayList<>(items); }
    public List<Player> getHumanoids() { return map.mapId == 126 ? new ArrayList<>(humanoids) : humanoids; }
    public List<Player> getNotBosses() { return map.mapId == 126 ? new ArrayList<>(notBosses) : notBosses; }

    public int getNumOfPlayers() {
        return this.players.size();
    }

    public int getNumOfBosses() {
        return this.bosses.size();
    }

    public boolean isBossCanJoin(Boss boss) {
        for (Player b : this.bosses) {
            if (b.id == boss.id) {
                return false;
            }
        }
        return true;
    }

    public void addPlayer(Player player) {
        if (player != null) {
          synchronized (player) {
            if (!this.humanoids.contains(player)) {
                this.humanoids.add(player);
            }

            if (player instanceof NonInteractiveNPC && !this.nonInteractiveNPCs.contains(player)) {
                this.nonInteractiveNPCs.add(player);
            }

            if (!player.isBoss && !this.notBosses.contains(player) && !player.isNewPet && !(player instanceof NonInteractiveNPC)) {
                this.notBosses.add(player);
            }

            if (!player.isBoss && !player.isNewPet && !player.isPet && !this.players.contains(player) && !(player instanceof NonInteractiveNPC)) {
                this.players.add(player);
            }

            if (player.isBoss && !this.bosses.contains(player)) {
                this.bosses.add(player);
            }
            if ((player.isPet || player.isNewPet) && !this.pets.contains(player)) {
                this.pets.add(player);
            }
          }
        }
    }

    private void udPlayer() {
        List<Player> current = getNotBosses();
        for (int i = current.size() - 1; i >= 0; i--) {
            Player pl = current.get(i);
            if (pl != null && !pl.isPl() && !pl.isPet && !pl.isBot && !pl.isNewPet) {
                pl.update();
            }
        }
    }

    public void removePlayer(Player player) {
      synchronized (player) {
        player.zoneExitSequence++;
        this.nonInteractiveNPCs.remove(player);
        this.humanoids.remove(player);
        this.notBosses.remove(player);
        this.players.remove(player);
        this.bosses.remove(player);
        this.pets.remove(player);
      }
    }

    public ItemMap getItemMapByItemMapId(int itemId) {
        for (ItemMap item : this.items) {
            if (item != null && item.itemMapId == itemId) {
                return item;
            }
        }
        return null;
    }

    public ItemMap getItemMapByTempId(int tempId) {
        for (ItemMap item : this.items) {
            if (item.itemTemplate.id == tempId) {
                return item;
            }
        }
        return null;
    }

    public List<ItemMap> getItemMapsForPlayer(Player player) {
        List<ItemMap> list = new ArrayList<>();
        for (ItemMap item : items) {
            if (item.itemTemplate.id == 78) {
                if (TaskService.gI().getIdTask(player) != ConstTask.TASK_3_1) {
                    continue;
                }
            }
            if (item.itemTemplate.id == 74) {
                if (TaskService.gI().getIdTask(player) < ConstTask.TASK_3_0) {
                    continue;
                }
            }
            if (item.itemTemplate.id == 726 && item.playerId != player.id) {
                continue;
            }
            list.add(item);
        }
        return list;
    }

    public Player getPlayerInMap(long idPlayer) {
        for (Player pl : humanoids) {
            if (pl != null && pl.id == idPlayer) {
                return pl;
            }
        }
        return null;
    }

    public Player getPlayerInMapOffline(Player player, long idPlayer) {
        for (Player pl : bosses) {
            if (pl.id == idPlayer && pl instanceof TrainingBoss && ((TrainingBoss) pl).playerAtt.equals(player)) {
                return pl;
            }
        }
        return null;
    }

    public void pickItem(Player player, int itemMapId) {
        if(player==null)return;
        synchronized(player){pickItemInternal(player,itemMapId);}
    }

    private void pickItemInternal(Player player, int itemMapId) {
        ItemMap itemMap = getItemMapByItemMapId(itemMapId);
        if (itemMap != null && !itemMap.isPickedUp) {
            synchronized (itemMap) {
                try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {
                if(!nro.models.event.EventRuntimeService.canPick(itemMap))return;

                if (!itemMap.isPickedUp) {
                    if (itemMap.itemTemplate != null) {
                        if (itemMap.itemTemplate.type == 22) {
                            return;
                        }
                        int playerId = Math.abs(itemMap.playerId > 100_000_000 ? 1_000_000_000 - (int) itemMap.playerId : (int) itemMap.playerId);
                        if (itemMap.keepOwnerUntilDespawn ? itemMap.playerId == player.id
                                : playerId == player.id || itemMap.playerId == player.id || itemMap.playerId == -1) {
                            Item item = ItemService.gI().createItemFromItemMap(itemMap);
                            int pickupQuantity = item.quantity;
                            int pickupItemId = item.template.id;

                            if (item.template.id == 648) {
                                if (!InventoryService.gI().findItemTatVoGiangSinh(player)) {
                                    Service.gI().sendThongBao(player, "Cần thêm Tất,vớ giáng sinh");
                                    return;
                                }
                            }

                            // Capsule stack merging and opening share the same player monitor.
                            // Other items retain the already held item-map monitor.
                            synchronized (pickupItemId == nro.models.consts.ConstItem.CAPSULE_HONG || itemMap.commesonMatchId != null ? player : itemMap) {
                            if (!nro.models.services.CommesonChallengeService.gI().beforePickup(player,itemMap)) return;
                            if (InventoryService.gI().addItemBag(player, item)) {
                                if(!nro.models.services.CommesonChallengeService.gI().afterPickup(player,itemMap,true))return;
                                boolean retainedSpecial = (this.map.mapId >= 21 && this.map.mapId <= 23 && pickupItemId == 74)
                                    || (this.map.mapId >= 42 && this.map.mapId <= 44 && pickupItemId == 78);
                                if (!retainedSpecial) { itemMap.isPickedUp = true; removeItemMap(itemMap); }
                                if (!retainedSpecial) {
                                    nro.models.services.KolQuestService.gI().onItemPicked(player,pickupItemId,pickupQuantity);
                                    nro.models.task.BadgesTaskService.updateCountBagesTask(player, nro.models.consts.ConstTaskBadges.ONG_THAN_VE_CHAI, 1);
                                }
                                int itemType = item.template.type;
                                Message msg;
                                try {
                                    msg = new Message(-20);
                                    msg.writer().writeShort(itemMapId);
                                    switch (itemType) {
                                        case 9, 10, 34 -> {
                                            msg.writer().writeUTF(pickupQuantity > Short.MAX_VALUE ? "Bạn vừa nhận được " + Util.formatNumber(pickupQuantity) + " " + item.template.name : "");
                                            PlayerService.gI().sendInfoHpMpMoney(player);
                                        }
                                        default -> {
                                            switch (item.template.id) {
                                                case 73 ->
                                                    msg.writer().writeUTF("");
                                                case 74 ->
                                                    msg.writer().writeUTF("Bạn mới vừa ăn " + item.template.name);
                                                case 78 ->
                                                    msg.writer().writeUTF("Wow, một cậu bé dễ thương!");
                                                default -> {
                                                    if (item.template.type >= 0 && item.template.type < 5) {
                                                        msg.writer().writeUTF("Bạn nhận được " + item.template.name);
                                                    } else {
                                                        msg.writer().writeUTF("");
                                                    }
                                                    if (item.template.id == 648) {
                                                        InventoryService.gI().subQuantityItemsBag(player, InventoryService.gI().findItemBag(player, 649), 1);
                                                    }
                                                    InventoryService.gI().sendItemBags(player);
                                                }
                                            }
                                        }
                                    }
                                    nro.models.network.ItemOptionWire.writePickupQuantity(msg, player, pickupQuantity);
                                    player.sendMessage(msg);
                                    msg.cleanup();
                                    Service.gI().sendToAntherMePickItem(player, itemMapId);

                                } catch (Exception e) {
                                    Logger.logException(Zone.class, e);
                                }
                            } else {
                                nro.models.services.CommesonChallengeService.gI().afterPickup(player,itemMap,false);
                                if (!ItemMapService.gI().isBlackBall(item.template.id) && !ItemMapService.gI().isNamecBall(item.template.id) && !ItemMapService.gI().isNamecBallStone(item.template.id)) {
                                    String text = "Hành trang không còn chỗ trống, không thể nhặt thêm";
                                    Service.gI().sendThongBao(player, text);
                                    return;
                                }
                            }


                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không thể nhặt vật phẩm của người khác");
                            return;
                        }
                        TaskService.gI().checkDoneTaskPickItem(player, itemMap);
                        TaskService.gI().checkDoneSideTaskPickItem(player, itemMap);
                        TaskService.gI().checkDoneClanTaskPickItem(player, itemMap);
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                }

                }
            }
        }
    }

    public synchronized void addItem(ItemMap itemMap) {
        if (!nro.models.services.BossAdminDropService.mayPublish(itemMap)) return;
        try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {
        if(itemMap!=null && itemMap.itemTemplate!=null && nro.models.event.EventItemPolicy.isBlocked(itemMap.itemTemplate.id))return;
        if (itemMap != null && !items.contains(itemMap)) {
            if (itemMap.itemMapId < 0 || itemMap.itemMapId > Short.MAX_VALUE
                    || getItemMapByItemMapId(itemMap.itemMapId) != null) {
                itemMap.itemMapId = allocateItemMapId();
            }
            items.add(0, itemMap);
        }
        }
    }

    int allocateItemMapId() {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException("Item-map IDs must be allocated while holding the zone lock");
        }
        if (countItemAppeaerd < 0 || countItemAppeaerd > Short.MAX_VALUE) {
            countItemAppeaerd = 0;
        }
        for (int checked = 0; checked <= Short.MAX_VALUE; checked++) {
            int candidate = countItemAppeaerd;
            countItemAppeaerd = candidate == Short.MAX_VALUE ? 0 : candidate + 1;
            if (getItemMapByItemMapId(candidate) == null) {
                return candidate;
            }
        }
        throw new IllegalStateException("No signed-short item-map IDs available in map "
                + this.map.mapId + ", zone " + this.zoneId);
    }

    public synchronized void removeItemMap(ItemMap itemMap) {
        this.items.remove(itemMap);
    }

    public Player getRandomPlayerInMap() {
        List<Player> plNotVoHinh = new ArrayList();

        //Lỗi
        for (Player pl : this.notBosses) {
            if (pl != null && (pl.effectSkin == null || !pl.effectSkin.isVoHinh) && pl.maBuHold == null && !pl.isMabuHold) {
                plNotVoHinh.add(pl);
            }
        }

        if (!plNotVoHinh.isEmpty()) {
            return plNotVoHinh.get(Util.nextInt(0, plNotVoHinh.size() - 1));
        }

        return null;
    }

    public void load_Me_To_Another(Player player) {
        List<Player> currentPlayers = getPlayers();
        try {
            if (player.zone != null) {
                if (this.map.mapId == 164) {
                    return;
                }

                if (MapService.gI().isMapOffline(this.map.mapId)) {
                    // Load boss
                    if (player instanceof TrainingBoss || player instanceof NonInteractiveNPC) {
                        for (int i = currentPlayers.size() - 1; i >= 0; i--) {
                            Player pl = currentPlayers.get(i);
                            if (!player.equals(pl) && (player instanceof NonInteractiveNPC || ((TrainingBoss) player).playerAtt.equals(pl))) {
                                infoPlayer(pl, player);
                            }
                        }
                    }
                } else {
                    for (int i = currentPlayers.size() - 1; i >= 0; i--) {
                        Player pl = currentPlayers.get(i);
                        if (!player.equals(pl)) {
                            infoPlayer(pl, player);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    public void load_Another_To_Me(Player player) {
        try {
            if (this.map.mapId == 164) {
                return;
            }

            if (MapService.gI().isMapOffline(this.map.mapId)) {
                // Load boss
                for (int i = this.humanoids.size() - 1; i >= 0; i--) {
                    Player pl = this.humanoids.get(i);
                    if (pl != null && (pl instanceof NonInteractiveNPC
                            || pl instanceof TrainingBoss && ((TrainingBoss) pl).playerAtt.equals(player))) {
                        infoPlayer(player, pl);
                    }
                }
            } else {
                for (int i = this.humanoids.size() - 1; i >= 0; i--) {
                    Player pl = this.humanoids.get(i);
                    if (pl != null && !player.equals(pl)) {
                        infoPlayer(player, pl);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    public void loadBoss(Boss boss) {
        try {
            if (MapService.gI().isMapOffline(this.map.mapId)) {
                //Load boss
                for (Player pl : this.bosses) {
                    if (!boss.equals(pl) && !pl.isPl() && !pl.isPet && !pl.isNewPet) {
                        infoPlayer(boss, pl);
                        infoPlayer(pl, boss);
                    }
                }
            } else {
                for (Player pl : this.bosses) {
                    if (!boss.equals(pl)) {
                        infoPlayer(boss, pl);
                        infoPlayer(pl, boss);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(MapService.class, e);
        }
    }

    private void infoPlayer(Player plReceive, Player plInfo) {
        Message msg;
        try {
            msg = new Message(-5);
            msg.writer().writeInt((int) plInfo.id);
            if (plInfo.clan != null) {
                msg.writer().writeInt(plInfo.clan.id);
            } else if (plInfo.isBoss && (plInfo.id == BossID.MABU || plInfo.id == BossID.SUPERBU)) {
                msg.writer().writeInt(-100);
            } else if (plInfo.isCopy) {
                msg.writer().writeInt(-2);
            } else {
                msg.writer().writeInt(-1);
            }
            msg.writer().writeByte(Service.gI().getCurrLevel(plInfo));
            msg.writer().writeBoolean(false);
            msg.writer().writeByte(plInfo.typePk);
            msg.writer().writeByte(plInfo.gender);
            msg.writer().writeByte(plInfo.gender);
            msg.writer().writeShort(plInfo.getHead());
            msg.writer().writeUTF(Service.gI().name(plInfo));
            msg.writer().writeInt(plInfo.nPoint.hp);
            msg.writer().writeInt(plInfo.nPoint.hpMax);
            msg.writer().writeShort(plInfo.getBody());
            msg.writer().writeShort(plInfo.getLeg());
            int flagbag = plInfo.getFlagBag();
            if (plReceive.isPl() && plReceive.getSession() != null && plReceive.getSession().version >= 220) {
                switch (flagbag) {
                    case 83:
                        flagbag = 205;
                        break;
                }
            }
            msg.writer().writeByte(flagbag); //bag
            msg.writer().writeByte(-1);
            msg.writer().writeShort(plInfo.location.x);
            msg.writer().writeShort(plInfo.location.y);
            msg.writer().writeShort(0); // effbuffhp
            msg.writer().writeShort(0); // effbuffmp

            msg.writer().writeByte(0); // num eff

            //byte templateId, int timeStart, int timeLenght, short param
            msg.writer().writeByte(plInfo.idMark.getIdSpaceShip());

            msg.writer().writeByte(plInfo.effectSkill != null && plInfo.effectSkill.isMonkey ? 1 : 0);
            msg.writer().writeShort(plInfo.getMount());
            msg.writer().writeByte(plInfo.cFlag);

            msg.writer().writeByte(0);
            msg.writer().writeShort(plInfo.getAura()); //idauraeff
            msg.writer().writeByte(plInfo.getEffFront()); //seteff
            msg.writer().writeShort(plInfo.getHat()); //id hat
            msg.writer().writeByte(plInfo.isNewMember ? 1 : 0); // trailing badge required by the -5 client reader

            plReceive.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Service.gI().sendFlagPlayerToMe(plReceive, plInfo);
        try {
            if (plInfo.isPl()) {
                if (plInfo.effectSkill != null && plInfo.effectSkill.isChibi) {
                    Service.gI().sendChibiFollowToMe(plReceive, plInfo);
                }
            }
        } catch (Exception e) {
        }

        try {
            if (plInfo.isDie()) {
                msg = new Message(-8);
                msg.writer().writeInt((int) plInfo.id);
                msg.writer().writeByte(0);
                msg.writer().writeShort(plInfo.location.x);
                msg.writer().writeShort(plInfo.location.y);
                plReceive.sendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {

        }
    }

    public void mapInfo(Player pl) {
        Message msg;
        try {
            msg = new Message(-24);
            msg.writer().writeByte(this.map.mapId);
            msg.writer().writeByte(this.map.planetId);
            msg.writer().writeByte(this.map.tileId);
            msg.writer().writeByte(this.map.bgId);
            msg.writer().writeByte(this.map.type);
            msg.writer().writeUTF(this.map.mapName);
            msg.writer().writeByte(this.zoneId);

            msg.writer().writeShort(pl.location.x);
            msg.writer().writeShort(pl.location.y);

            // waypoint
            try {
                List<WayPoint> wayPoints = this.map.wayPoints;
                msg.writer().writeByte(wayPoints.size());
                for (WayPoint wp : wayPoints) {
                    msg.writer().writeShort(wp.minX);
                    msg.writer().writeShort(wp.minY);
                    msg.writer().writeShort(wp.maxX);
                    msg.writer().writeShort(wp.maxY);
                    msg.writer().writeBoolean(wp.isEnter);
                    msg.writer().writeBoolean(wp.isOffline);
                    msg.writer().writeUTF(wp.name);
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            // mob
            try {
                List<Mob> mobs = new ArrayList<>();
                for (Mob mob : this.mobs) {
                    if (mob.isBigBoss() && mob.tempId != 70 && mob.isDie()) {
                        continue;
                    }
                    mobs.add(mob);
                }
                msg.writer().writeByte(mobs.size());
                for (Mob mob : mobs) {
                    msg.writer().writeBoolean(false); //is disable
                    msg.writer().writeBoolean(mob.tempId == ConstMob.MAY_DO_SUC_MANH); //is dont move
                    msg.writer().writeBoolean(false); //is fire
                    msg.writer().writeBoolean(false); //is ice
                    msg.writer().writeBoolean(false); //is wind
                    msg.writer().writeByte(mob.tempId);
                    msg.writer().writeByte(0); // sys
                    msg.writer().writeInt(mob.point.gethp());
                    msg.writer().writeByte(mob.level);
                    msg.writer().writeInt((mob.point.getHpFull()));
                    msg.writer().writeShort(mob.location.x);
                    msg.writer().writeShort(mob.location.y);
                    msg.writer().writeByte(mob.status);
                    msg.writer().writeByte(mob.lvMob);
                    msg.writer().writeBoolean(mob.tempId == ConstMob.GAU_TUONG_CUOP || mob.tempId >= ConstMob.VOI_CHIN_NGA && mob.tempId <= ConstMob.PIANO); //is bigboss
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            msg.writer().writeByte(0);

            // npc
            try {
                List<Npc> npcs = NpcManager.getNpcsByMapPlayer(pl);
                msg.writer().writeByte(npcs.size());
                for (Npc npc : npcs) {
                    msg.writer().writeByte(npc.status);
                    msg.writer().writeShort(npc.cx);
                    msg.writer().writeShort(npc.cy);
                    msg.writer().writeByte(npc.tempId);
                    msg.writer().writeShort(npc.avartar);
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            // item
            try {
                List<ItemMap> itemsMap = this.getItemMapsForPlayer(pl);
                msg.writer().writeByte(itemsMap.size());
                for (ItemMap it : itemsMap) {
                    msg.writer().writeShort(it.itemMapId);
                    msg.writer().writeShort(it.itemTemplate.id);
                    msg.writer().writeShort(it.x);
                    msg.writer().writeShort(it.y);
                    msg.writer().writeInt((int) it.playerId);
                }
            } catch (Exception e) {
                msg.writer().writeByte(0);
            }

            // bg item
            try {
                final byte[] bgItem = FileIO.readFile("data/map/item_bg_map_data/" + this.map.mapId);
                msg.writer().write(bgItem);
            } catch (Exception e) {
                msg.writer().writeShort(0);
            }

            // eff item
            try {
                final byte[] effItem = FileIO.readFile("data/map/eff_map/" + this.map.mapId);
                msg.writer().write(effItem);
            } catch (Exception e) {
                msg.writer().writeShort(0);
            }

            msg.writer().writeByte(this.map.bgType);
            msg.writer().writeByte(pl.idMark.getIdSpaceShip());
            msg.writer().writeByte(this.map.mapId == 148 ? 1 : 0);
            pl.sendMessage(msg);

            msg.cleanup();

        } catch (Exception e) {
            Logger.logException(Service.class, e);
        }
    }

    public TrapMap isInTrap(Player player) {
        for (TrapMap trap : this.trapMaps) {
            if (player.location.x >= trap.x && player.location.x <= trap.x + trap.w
                    && player.location.y >= trap.y && player.location.y <= trap.y + trap.h) {
                return trap;
            }
        }
        return null;
    }

    public void sendBigBoss(Player player) {
        for (Mob mob : this.mobs) {
            if (!mob.isDie() && mob.tempId == ConstMob.HIRUDEGARN) {
                if (mob.lvMob >= 1) {
                    Service.gI().sendBigBoss2(player, 6, mob);
                }
                if (mob.lvMob >= 2) {
                    Service.gI().sendBigBoss2(player, 5, mob);
                }
                break;
            }
        }
    }

    public MaBuHold getMaBuHold() {
        for (MaBuHold hold : MapService.gI().getMapById(128).zones.get(this.zoneId).maBuHolds) {
            if (hold.player == null) {
                return hold;
            }
        }
        return null;
    }

    public void setMaBuHold(int slot, int zoneId, Player player) {
        MapService.gI().getMapById(128).zones.get(zoneId).maBuHolds.set(slot, new MaBuHold(slot, player));
    }

    public boolean isKhongCoTrongTaiTrongKhu() {
        boolean kovao = true;
        for (Player pl : players) {
            if (!pl.isPl()) {
                kovao = false;
                break;
            }
            if ((pl.zone.map.mapId >= 21
                    && pl.zone.map.mapId <= 23)
                    || pl.zone.map.mapId == 170
                    || pl.zone.map.mapId == 153
                    || pl.zone.map.mapId == 52
                    || pl.zone.map.mapId == 113
                    || pl.zone.map.mapId == 129
                    || MapService.gI().isMapDoanhTrai(pl.zone.map.mapId)
                    || MapService.gI().isMapBlackBallWar(pl.zone.map.mapId)
                    || MapService.gI().isMapBanDoKhoBau(pl.zone.map.mapId)
                    || MapService.gI().isMapPhoBan(pl.zone.map.mapId)
                    || MapService.gI().isMapMaBu(pl.zone.map.mapId)
                    || MapService.gI().isMapKhiGasHuyDiet(pl.zone.map.mapId)
                    || MapService.gI().isMapConDuongRanDoc(pl.zone.map.mapId)
                    || MapService.gI().isMapOffline(pl.zone.map.mapId)) {
                kovao = false;
            }
        }
        return kovao;
    }

    public Player findNearestPlayer(Pet pet) {
        Player closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Player p : this.players) {
            if (p != null && !p.isDie() && !p.equals(pet.master)) {
                int distance = Math.abs(p.location.x - pet.location.x);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = p;
                }
            }
        }

        return closest;
    }

    public Mob findNearestMob(Pet pet) {
        Mob closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (Mob m : this.mobs) {
            if (m != null && !m.isDie()) {
                int distance = Math.abs(m.location.x - pet.location.x);
                if (distance < minDistance) {
                    minDistance = distance;
                    closest = m;
                }
            }
        }

        return closest;
    }

}
