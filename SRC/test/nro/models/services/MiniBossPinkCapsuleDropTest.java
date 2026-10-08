package nro.models.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.Odo;
import nro.models.map.ItemMap;
import nro.models.player.Location;
import nro.models.map.Zone;
import nro.models.player.Pet;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

public final class MiniBossPinkCapsuleDropTest {
    static Object helper() throws Exception {
        try { return Class.forName("nro.models.services.MiniBossPinkCapsuleDrop").getConstructor().newInstance(); }
        catch (ClassNotFoundException missing) { throw new AssertionError("private mini boss capsule drop helper is missing", missing); }
    }
    static boolean drop(Object helper, Boss boss, Player player, int roll) throws Exception {
        Method m = helper.getClass().getDeclaredMethod("drop", Boss.class, Player.class, IntUnaryOperator.class);
        m.setAccessible(true);
        return (boolean)m.invoke(helper, boss, player, (IntUnaryOperator)bound -> {
            check(bound == 5, "random roll has exactly five equiprobable outcomes");
            return roll;
        });
    }
    static void reset(Object helper) throws Exception { helper.getClass().getMethod("reset").invoke(helper); }
    static Boss boss() throws Exception {
        Boss b = blank(Odo.class); b.zone = zone(); b.location = blank(Location.class);
        b.location.x = 2; b.location.y = 200; return b;
    }
    static void bounds() throws Exception {
        install();
        for (int roll = 0; roll < 5; roll++) {
            Object h = helper(); Boss b = boss(); Player p = player(777);
            check(drop(h,b,p,roll), "valid kill grants capsules");
            check(b.zone.items.size() == roll + 1, "roll maps to one through five separate capsules");
            for (ItemMap d : b.zone.items) {
                check(d.itemTemplate.id == 722 && d.quantity == 1, "individual capsule drops");
                check(d.playerId == 777 && kept(d), "exact killer owns capsule until expiry");
                check(d.x >= 0 && d.x < 240 && d.y == 100 + d.x, "terrain uses clamped drop coordinate");
            }
        }
    }
    static void pickup() throws Exception {
        install(); Object h = helper(); Boss b = boss(); Player owner = player(777), other = player(778);
        owner.zone = b.zone; other.zone = b.zone; drop(h,b,owner,4);
        List<ItemMap> items = new ArrayList<>(b.zone.items);
        for (ItemMap d : items) {
            d.createTime = System.currentTimeMillis() - 46000; d.update();
            check(d.playerId == owner.id && kept(d), "capsule remains private beyond public transition time");
            b.zone.pickItem(other,d.itemMapId);
            check(!d.isPickedUp && b.zone.items.contains(d), "intruder cannot collect aged capsule");
            b.zone.pickItem(owner,d.itemMapId);
            check(d.isPickedUp && !b.zone.items.contains(d), "killer can collect aged capsule");
            b.zone.pickItem(owner,d.itemMapId);
        }
        check(owner.kolQuestState.pinkCapsulesPicked == 5 && other.kolQuestState.pinkCapsulesPicked == 0,
                "only successful killer pickups advance KOL once per capsule");
    }
    static void expiry() throws Exception {
        install(); Boss b = boss(); drop(helper(),b,player(777),0); ItemMap d = b.zone.items.get(0);
        d.createTime = System.currentTimeMillis() - 51000; d.update();
        check(d.playerId == 777 && !b.zone.items.contains(d) && d.itemTemplate == null,
                "private capsule expires without ever becoming public");
    }
    static void guard() throws Exception {
        install(); Object h = helper(); Boss b = boss(); Player p = player(777);
        check(drop(h,b,p,4), "first reward grants");
        check(!drop(h,b,p,4) && b.zone.items.size() == 5, "duplicate reward grants nothing");
        reset(h); check(drop(h,b,p,0) && b.zone.items.size() == 6, "respawn enables a new reward");
    }
    static void invalid() throws Exception {
        install(); Object h = helper(); Boss b = boss(); Player p = player(777); Zone z = b.zone;
        check(!drop(h,b,null,4), "null killer rejected");
        check(!drop(h,b,blank(Odo.class),4), "boss killer rejected");
        Player nonPlayer = player(888); nonPlayer.isPlayer = false;
        check(!drop(h,b,nonPlayer,4), "nonplayer killer rejected");
        Player bot = player(888); bot.isBot = true;
        check(!drop(h,b,bot,4), "bot killer rejected");
        Pet orphan = blank(Pet.class); orphan.isPet = true;
        check(!drop(h,b,orphan,4), "orphan pet rejected");
        b.zone = null; check(!drop(h,b,p,4), "missing zone rejected"); b.zone = z;
        var map = z.map; z.map = null; check(!drop(h,b,p,4), "missing map rejected"); z.map = map;
        int width = map.mapWidth; map.mapWidth = 0; check(!drop(h,b,p,4), "empty terrain rejected"); map.mapWidth = width;
        var loc = b.location; b.location = null; check(!drop(h,b,p,4), "missing location rejected"); b.location = loc;
        check(z.items.isEmpty() && drop(h,b,p,4) && z.items.size() == 5, "invalid requests do not burn reward guard");
    }
    static void pet() throws Exception {
        install(); Boss b = boss(); Pet pet = blank(Pet.class); pet.isPet = true; pet.id = -777;
        pet.master = player(777); check(drop(helper(),b,pet,0), "pet kill grants master reward");
        check(b.zone.items.get(0).playerId == 777, "pet master is the pickup owner");
        nro.models.player.NewPet summon = blank(nro.models.player.NewPet.class);
        summon.isNewPet = true; summon.master = player(778);
        Boss second = boss();
        check(drop(helper(),second,summon,0), "summoned pet kill grants master reward");
        check(second.zone.items.get(0).playerId == 778, "summoned pet master owns capsule");
    }
    static void concurrent() throws Exception {
        install(); Object h = helper(); Boss b = boss(); Player p = player(777);
        CountDownLatch go = new CountDownLatch(1); AtomicReference<Throwable> error = new AtomicReference<>();
        Runnable action = () -> { try { go.await(); drop(h,b,p,4); } catch(Throwable t) { error.set(t); } };
        Thread first = new Thread(action), second = new Thread(action);
        first.start(); second.start(); go.countDown(); first.join(3000); second.join(3000);
        check(!first.isAlive() && !second.isAlive() && error.get() == null, "concurrent rewards finish safely");
        check(b.zone.items.size() == 5, "concurrent kill callbacks grant once per life");
    }
    public static void main(String[] args) throws Exception {
        try {
            switch(args[0]) {
                case "bounds" -> bounds(); case "pickup" -> pickup(); case "expiry" -> expiry();
                case "guard" -> guard(); case "invalid" -> invalid(); case "pet" -> pet(); case "concurrent" -> concurrent();
                default -> throw new AssertionError(args[0]);
            }
            System.out.println("PASS MiniBossPinkCapsuleDrop " + args[0]); System.exit(0);
        } catch(Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }
}
