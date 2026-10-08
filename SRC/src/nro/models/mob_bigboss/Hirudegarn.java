package nro.models.mob_bigboss;

import nro.models.item.Item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.mob.BigBoss;
import nro.models.mob.Mob;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

public class Hirudegarn extends BigBoss {

    private int errors;

    public Hirudegarn(Mob mob) {
        super(mob);
    }

    private boolean phaseDeathRewarded;

    @Override
    public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
        // SMTN and automatic pickup normalize pet attackers to their owner.
        Player owner = plAtt instanceof nro.models.player.Pet pet ? pet.master : plAtt;
        if (owner != null) {
            synchronized (owner) { injureDuringSession(plAtt); }
        } else if (plAtt == null) injureDuringSession(null);
    }

    private void injureDuringSession(Player attacker) {
        synchronized (nro.models.services.HirudegarnEventService.gI().eventGate) {
            if (!nro.models.services.HirudegarnEventService.gI().prepareZone(zone)) return;
            long damage = this.point.hp / 100 > 0 ? this.point.hp / 100 : 1;
            super.injured(attacker, damage, false);
        }
    }

    /** A session always starts at phase one with clean combat state. */
    public boolean resetEventLife() {
        lvMob = 0;
        action = 0;
        location.x = Util.nextInt(100, 900);
        location.y = 360;
        if (!restorePhaseLife()) return false;
        sendPhaseSpawn();
        return true;
    }

    private boolean restorePhaseLife() {
        java.time.Instant lifeSession = nro.models.services.HirudegarnEventService.gI().sessionEnd();
        if (lifeSession == null) return false;
        this.point.hp = 0;
        this.effectSkill.update(); // clear effects and send their existing removal packets
        if (!lifeSession.equals(nro.models.services.HirudegarnEventService.gI().sessionEnd())) return false;
        this.hoiSinh(); // resets Mob.kolDeathCounted for every life
        this.lastTimeDie = 0;
        this.lastTimePhucHoi = 0;
        this.lastBigBossAttackTime = 0;
        this.temporaryEnemies.clear();
        this.phaseDeathRewarded = false;
        return true;
    }

    private void sendPhaseSpawn() {
        Service.gI().sendBigBoss2(this.zone, action, this);
        Message msg = null;
        try {
            msg = new Message(-9);
            msg.writer().writeByte(this.id);
            msg.writer().writeInt(this.point.gethp());
            msg.writer().writeInt(1);
            Service.gI().sendMessAllPlayerInMap(this.zone, msg);
        } catch (Exception e) {
        } finally { if (msg != null) msg.cleanup(); }
    }

    @Override
    public void update() {
        synchronized (nro.models.services.HirudegarnEventService.gI().eventGate) {
            if (!nro.models.services.HirudegarnEventService.gI().prepareZone(zone)) return;
            if (isDie()) {
                if (lvMob == 3) {
                    resetEventLife();
                    return;
                }
                if (!phaseDeathRewarded) {
                    phaseDeathRewarded = true;
                    rewardPhaseDeath();
                }
                if (!nro.models.services.HirudegarnEventService.gI().isOpen()) return;
                switch (lvMob) {
                    case 0 -> { lvMob = 1; action = 6; }
                    case 1 -> { lvMob = 2; action = 5; }
                    case 2 -> { lvMob = 3; action = 9; }
                    default -> { return; }
                }
                if (lvMob <= 2) {
                    if (!restorePhaseLife()) return;
                    sendPhaseSpawn();
                } else {
                    Service.gI().sendBigBoss2(this.zone, action, this);
                    this.location.x = -1000;
                    this.location.y = -1000;
                }
                return;
            }
            effectSkill.update();
        }
        // Attack acquires target monitors only after releasing the lifecycle gate.
        attack();
    }

    private void rewardPhaseDeath() {
        int trai = 0;
        int phai = 1;
        int next = 0;
        for (int i = 0; i < 30; i++) {
            if (!nro.models.services.HirudegarnEventService.gI().isOpen()) return;
            int X = next == 0 ? -5 * trai : 5 * phai;
            if (next == 0) {
                trai++;
            } else {
                phai++;
            }
            next = next == 0 ? 1 : 0;
            if (trai > 10) {
                trai = 0;
            }
            if (phai > 10) {
                phai = 1;
            }
            dropEventItem(new ItemMap(zone, 190, 32000, this.location.x + X, this.location.y, -1));
        }
        if (!nro.models.services.HirudegarnEventService.gI().isOpen()) return;
        if (Util.isTrue(20, 100)) {
            ItemMap it = new ItemMap(this.zone, 568, 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x,
                    this.location.y - 24), -1);
            dropEventItem(it);
        }
        if (!nro.models.services.HirudegarnEventService.gI().isOpen()) return;
        if (Util.isTrue(20, 100)) {
            ItemMap it = new ItemMap(this.zone, 1795, 1, this.location.x,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), -1);

            it.options.add(new ItemOption(250, 0));

            dropEventItem(it);
        }

        if (!nro.models.services.HirudegarnEventService.gI().isOpen()) return;
        if (Util.isTrue(1, 40)) {
            short[] possibleIds = {555, 557, 559};
            short idItem = possibleIds[Util.nextInt(possibleIds.length)];
            ItemMap it = new ItemMap(this.zone, idItem, 1, this.location.x,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), -1);
            ItemMap itemWithOptions = ItemService.gI().randDoTL(this.zone, 1, this.location.x,
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24), -1);
            it.options.clear();
            it.options.addAll(itemWithOptions.options);
            this.zone.removeItemMap(itemWithOptions);
            dropEventItem(it);

        }
    }

    private void dropEventItem(ItemMap item) {
        if (nro.models.services.HirudegarnEventService.gI().isOpen()) Service.gI().dropItemMap(zone, item);
        else zone.removeItemMap(item);
    }

    @Override
    public void attack() {
        if (!nro.models.services.HirudegarnEventService.gI().isOpen()) return;
        if (!isDie() && !effectSkill.isHaveEffectSkill() && Util.canDoWithTime(lastBigBossAttackTime, 3000)) {
            Message msg = null;
            try {
                int[] idAction = new int[]{1, 2, 3, 7};
                if (this.lvMob >= 2) {
                    idAction = new int[]{1, 2};
                }
                action = action == 7 ? 0 : idAction[Util.nextInt(0, idAction.length - 1)];
                java.util.List<Player> players = this.zone.getPlayers();
                if (players.isEmpty()) {
                    return;
                }
                int index = Util.nextInt(0, players.size() - 1);
                Player player = players.get(index);
                if (player == null || player.isDie()) {
                    return;
                }
                if (action == 1) {
                    this.location.x = (short) player.location.x;
                    Service.gI().sendBigBoss2(this.zone, 8, this);
                }
                msg = new Message(101);
                msg.writer().writeByte(action);
                if (action >= 0 && action <= 4) {
                    switch (action) {
                        case 1:
                            msg.writer().writeByte(1);
                            int dame = damageDuringSession(player);
                            msg.writer().writeInt((int) player.id);
                            msg.writer().writeInt(dame);
                            break;
                        case 3:
                            this.location.x = (short) player.location.x;
                            msg.writer().writeShort(this.location.x);
                            msg.writer().writeShort(this.location.y);
                            break;
                        default:
                            msg.writer().writeByte(players.size());
                            for (int i = 0; i < players.size(); i++) {
                                Player pl = players.get(i);
                                dame = damageDuringSession(pl);
                                msg.writer().writeInt((int) pl.id);
                                msg.writer().writeInt(dame);
                            }
                            break;
                    }
                } else {
                    if (action == 6 || action == 8) {
                        this.location.x = (short) player.location.x;
                        msg.writer().writeShort(this.location.x);
                        msg.writer().writeShort(this.location.y);
                    }
                }
                Service.gI().sendMessAllPlayerInMap(this.zone, msg);
                lastBigBossAttackTime = System.currentTimeMillis();
            } catch (Exception e) {
                if (errors < 5) {
                    errors++;
                    e.printStackTrace();
                }
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private int damageDuringSession(Player player) {
        synchronized (player) {
            synchronized (nro.models.services.HirudegarnEventService.gI().eventGate) {
                if (player.zone != zone || !nro.models.services.HirudegarnEventService.gI().isOpen() || isDie()) return 0;
                return player.injured(null, this.point.getDameAttack(), false, true);
            }
        }
    }

}
