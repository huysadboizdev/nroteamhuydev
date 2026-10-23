package nro.models.combine;

import com.google.gson.JsonArray;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.item.Item;
import nro.models.item.SKHCapsuleCatalog;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.shop_ky_gui.ConsignTransactions;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Milestone credit runs only inside paid VIP pair upgrades; no material-deposit route. */
public final class NangSKHVipTichLuy {
    private static final int TARGET = 50;
    private static final int MIN_BASE_STAT_BONUS_PERCENT = 1;
    private static final int MAX_BASE_STAT_BONUS_PERCENT = 15;
    private static final int[][] DIVINE = {
        {555, 556, 562, 563, 561}, {557, 558, 564, 565, 561}, {559, 560, 566, 567, 561}
    };
    private NangSKHVipTichLuy() { }

    public record Progress(int divine, int destroyer, List<Integer> rewards) {
        public Progress {
            if (divine < 0 || destroyer < 0 || divine > 99 || destroyer > 99
                    || Math.abs(divine - destroyer) > TARGET || Math.min(divine, destroyer) >= TARGET) {
                throw new IllegalArgumentException("Tiến độ SKH VIP không hợp lệ.");
            }
            rewards = List.copyOf(rewards);
            for (Integer planet : rewards) if (planet == null || planet < 0 || planet > 2) {
                throw new IllegalArgumentException("Hành tinh phần thưởng SKH VIP không hợp lệ.");
            }
        }
    }
    public record PreparedRewards(List<Item> bag, Progress progress, List<String> bonusNames) {
        public PreparedRewards { bonusNames = List.copyOf(bonusNames); }
    }

    public static Progress current(Player player) {
        List<Integer> queued = new ArrayList<>(player.skhVipRewardPlanets == null ? List.of() : player.skhVipRewardPlanets);
        int divine = player.skhVipDivineProgress, destroyer = player.skhVipDestroyerProgress;
        // A legacy paid 50/50 recipe has no recorded main item. Fix its planet to the owner.
        if (divine == TARGET && destroyer == TARGET) {
            if (player.skhVipPaidPairs != TARGET || player.gender < 0 || player.gender > 2) {
                throw new IllegalArgumentException("Tiến độ SKH VIP cũ không hợp lệ.");
            }
            queued.add((int) player.gender);
            divine = 0; destroyer = 0;
        }
        return new Progress(divine, destroyer, queued);
    }

    public static Progress advance(Progress previous, int planet) {
        if (planet < 0 || planet > 2) throw new IllegalArgumentException("Hành tinh món chính không hợp lệ.");
        int divine = previous.divine() + 1, destroyer = previous.destroyer() + 1;
        List<Integer> queued = new ArrayList<>(previous.rewards());
        if (divine >= TARGET && destroyer >= TARGET) {
            divine -= TARGET; destroyer -= TARGET; queued.add(planet);
        }
        return new Progress(divine, destroyer, queued);
    }

    public static void apply(Player player, Progress progress) {
        player.skhVipDivineProgress = progress.divine(); player.skhVipDestroyerProgress = progress.destroyer();
        player.skhVipPaidPairs = Math.min(progress.divine(), progress.destroyer());
        player.skhVipRewardPlanets = new ArrayList<>(progress.rewards());
    }

    public static String encodeVip(Player player, Progress progress) {
        JsonArray data = new JsonArray();
        data.add(player.timesPerDayCuuSat); data.add(player.lastTimeCuuSat);
        data.add(player.nhanDeTuNangVIP); data.add(player.nhanVangNangVIP); data.add(player.nhanSKHVIP);
        data.add(player.vip); data.add(player.timevip); data.add(player.vipPurchaseCount);
        data.add(progress.divine()); data.add(progress.destroyer());
        data.add(Math.min(progress.divine(), progress.destroyer()));
        JsonArray queued = new JsonArray(); for (int planet : progress.rewards()) queued.add(planet); data.add(queued);
        return data.toString();
    }

    public static void load(Player player, JSONArray data) {
        try {
            int divine = data.size() > 8 ? Integer.parseInt(String.valueOf(data.get(8))) : 0;
            int destroyer = data.size() > 9 ? Integer.parseInt(String.valueOf(data.get(9))) : 0;
            int paidPairs = data.size() > 10 ? Integer.parseInt(String.valueOf(data.get(10))) : 0;
            List<Integer> queued = new ArrayList<>();
            if (data.size() > 11) {
                Object parsed = JSONValue.parse(String.valueOf(data.get(11)));
                if (!(parsed instanceof JSONArray planets)) throw new IllegalArgumentException("Invalid reward list");
                for (Object planet : planets) queued.add(Integer.parseInt(String.valueOf(planet)));
            }
            if (paidPairs != Math.min(divine, destroyer)) throw new IllegalArgumentException("Invalid paid-pair state");
            player.skhVipDivineProgress = divine; player.skhVipDestroyerProgress = destroyer;
            player.skhVipPaidPairs = paidPairs; player.skhVipRewardPlanets = queued;
            apply(player, current(player));
        } catch (RuntimeException invalid) { apply(player, new Progress(0, 0, List.of())); }
    }

    public static PreparedRewards prepareClaim(Player player, IntUnaryOperator random) {
        return prepareRewards(player.inventory.itemsBag, current(player), random);
    }

    /** Never mutate live items: only replace empty slots in a new list. */
    public static PreparedRewards prepareRewards(List<Item> original, Progress progress, IntUnaryOperator random) {
        List<Item> bag = new ArrayList<>(original);
        List<Integer> queued = new ArrayList<>(progress.rewards()); List<String> names = new ArrayList<>();
        while (!queued.isEmpty()) {
            int empty = -1;
            for (int slot = 0; slot < bag.size(); slot++) {
                Item item = bag.get(slot); if (item != null && !item.isNotNullItem()) { empty = slot; break; }
            }
            if (empty < 0) break;
            int planet = queued.get(0), type = roll(random, 5); int[] sets = SKHCapsuleCatalog.sets(planet);
            Item reward = ItemService.gI().createActivatedDivineItem(DIVINE[planet][type], sets[roll(random, sets.length)], planet);
            if (reward == null || reward.template == null || reward.quantity != 1 || !reward.isSKH()) {
                throw new IllegalStateException("Thiếu dữ liệu phần thưởng Thần Linh kích hoạt.");
            }
            int bonusPercent = MIN_BASE_STAT_BONUS_PERCENT
                    + roll(random, MAX_BASE_STAT_BONUS_PERCENT - MIN_BASE_STAT_BONUS_PERCENT + 1);
            applyBaseStatBonus(reward, bonusPercent);
            bag.set(empty, reward); names.add(reward.template.name); queued.remove(0);
        }
        return new PreparedRewards(bag, new Progress(progress.divine(), progress.destroyer(), queued), names);
    }

    /** Matches the base-stat increase on purchased Destroyer equipment without adding its extra bonus option. */
    private static void applyBaseStatBonus(Item item, int bonusPercent) {
        for (Item.ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null) continue;
            int id = option.optionTemplate.id;
            if (id == 0 || id == 14 || id == 22 || id == 23 || id == 27 || id == 28 || id == 47) {
                option.param = Math.toIntExact(option.param + (long) option.param * bonusPercent / 100);
            }
        }
        item.info = item.getInfo();
    }

    private static int roll(IntUnaryOperator random, int bound) {
        int result = random.applyAsInt(bound);
        if (result < 0 || result >= bound) throw new IllegalArgumentException("Dữ liệu ngẫu nhiên không hợp lệ.");
        return result;
    }

    public static void open(Player player) {
        synchronized (player) {
            try {
                if (!current(player).rewards().isEmpty()) {
                    var result = new ConsignTransactions().claimSkhVip(player);
                    Service.gI().sendThongBao(player, result.message()); InventoryService.gI().sendItemBags(player);
                    if (ConsignTransactions.hasPending(player)) return;
                }
                CombineService.gI().openTabCombine(player, CombineService.NANG_SKH_VIP);
            } catch (IllegalArgumentException | IllegalStateException invalid) { Service.gI().sendThongBao(player, invalid.getMessage()); }
        }
    }
}
