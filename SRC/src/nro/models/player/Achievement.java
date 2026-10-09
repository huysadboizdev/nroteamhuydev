package nro.models.player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.consts.ConstAchievement;
import nro.models.player_system.Template.AchievementQuest;
import nro.models.server.Manager;

/** Persistent achievement counters; older two-field rows remain readable. */
public class Achievement {
    private Player player;
    private List<AchievementQuest> achievementList = new ArrayList<>();
    private final Set<Long> defeatedOpponents = new HashSet<>();
    private long lastOnlineAt;
    private long flightRemainder;
    public Achievement(Player player) { this.player = player; }
    private void ensureSlots() {
        if (achievementList != null) while (achievementList.size() < Manager.ACHIEVEMENT_TEMPLATE.size())
            achievementList.add(new AchievementQuest(0, false));
    }
    public synchronized List<AchievementQuest> getAchievementList() { ensureSlots(); return achievementList; }
    public synchronized void add(AchievementQuest quest) { achievementList.add(quest); }
    public synchronized AchievementQuest get(int index) {
        ensureSlots();
        return achievementList != null && index >= 0 && index < achievementList.size() ? achievementList.get(index) : null;
    }
    public synchronized long getCompleted(int index) {
        AchievementQuest quest = get(index);
        if (quest == null) return 0;
        long current = 0;
        if (player != null) {
            if ((index == 0 || index == 1 || index == 16) && player.nPoint != null) current = player.nPoint.power;
            else if (index == 2 && player.magicTree != null) current = player.magicTree.level;
        }
        quest.completed = Math.max(Math.max(0, quest.completed), current);
        return index == ConstAchievement.HOAT_DONG_CHAM_CHI ? quest.completed / 3_600_000L : quest.completed;
    }
    public synchronized boolean isFinish(int index, long maxCount) {
        AchievementQuest quest = get(index);
        return quest != null && (quest.isRecieve || (maxCount > 0 && getCompleted(index) >= maxCount));
    }
    public synchronized boolean isRecieve(int index) { AchievementQuest quest = get(index); return quest != null && quest.isRecieve; }
    public synchronized boolean canReward(int index) {
        return index >= 0 && index < Manager.ACHIEVEMENT_TEMPLATE.size() && get(index) != null
                && !isRecieve(index) && Manager.ACHIEVEMENT_TEMPLATE.get(index).maxCount > 0
                && getCompleted(index) >= Manager.ACHIEVEMENT_TEMPLATE.get(index).maxCount;
    }
    private long limit(int index) {
        if (index < 0 || index >= Manager.ACHIEVEMENT_TEMPLATE.size()) return Long.MAX_VALUE;
        long limit = Manager.ACHIEVEMENT_TEMPLATE.get(index).maxCount;
        if (limit <= 0) return Long.MAX_VALUE;
        return index == ConstAchievement.HOAT_DONG_CHAM_CHI
                ? (limit > Long.MAX_VALUE / 3_600_000L ? Long.MAX_VALUE : limit * 3_600_000L) : limit;
    }
    public synchronized void done(int index, long amount) {
        AchievementQuest quest = get(index);
        if (quest == null || quest.isRecieve || amount <= 0) return;
        long old = Math.max(0, quest.completed), cap = limit(index);
        quest.completed = old >= cap ? old : old + Math.min(amount, cap - old);
    }
    public synchronized void doneNotAdd(int index, long amount) {
        AchievementQuest quest = get(index);
        if (quest != null && !quest.isRecieve && amount >= 0) quest.completed = Math.max(quest.completed, Math.min(amount, limit(index)));
    }
    public synchronized void reward(int index) { AchievementQuest quest = get(index); if (quest != null) quest.isRecieve = true; }
    public synchronized void recordWin(Player opponent) {
        if (player == null || opponent == null || !player.isPl() || !opponent.isPl()
                || opponent.id <= 0 || opponent.id == player.id || isRecieve(ConstAchievement.TRAM_TRAN_TRAM_THANG)
                || getCompleted(ConstAchievement.TRAM_TRAN_TRAM_THANG) >= limit(ConstAchievement.TRAM_TRAN_TRAM_THANG)) return;
        if (defeatedOpponents.add(opponent.id)) done(ConstAchievement.TRAM_TRAN_TRAM_THANG, 1);
    }
    public synchronized void updateOnlineTime(long now) {
        if (lastOnlineAt != 0 && now > lastOnlineAt) done(ConstAchievement.HOAT_DONG_CHAM_CHI, now - lastOnlineAt);
        lastOnlineAt = now;
    }
    public synchronized void recordFlight(int pixels) {
        long total = flightRemainder + Math.abs((long) pixels);
        done(ConstAchievement.KHINH_CONG_THANH_THAO, total / 10);
        flightRemainder = total % 10;
    }
    public synchronized void load(String json) {
        achievementList.clear(); defeatedOpponents.clear(); lastOnlineAt = 0;
        JsonArray rows = new JsonArray();
        try { JsonElement parsed = new JsonParser().parse(json == null || json.isBlank() ? "[]" : json); if (parsed.isJsonArray()) rows = parsed.getAsJsonArray(); }
        catch (RuntimeException invalid) { /* Missing legacy data is initialized below. */ }
        for (int i = 0; i < Manager.ACHIEVEMENT_TEMPLATE.size(); i++) {
            long count = 0; boolean received = false;
            try {
                JsonElement element = i < rows.size() ? rows.get(i) : null;
                JsonArray row = element == null ? new JsonArray() : (element.isJsonArray() ? element.getAsJsonArray() : new JsonParser().parse(element.getAsString()).getAsJsonArray());
                if (row.size() > 1) received = row.get(1).getAsBoolean();
                try { if (row.size() > 0) count = Math.max(0, row.get(0).getAsLong()); } catch (RuntimeException invalidCount) { count = 0; }
                if (i == ConstAchievement.TRAM_TRAN_TRAM_THANG && row.size() > 2 && row.get(2).isJsonArray())
                    for (JsonElement id : row.get(2).getAsJsonArray()) { try { if (id.getAsLong() > 0 && defeatedOpponents.size() < limit(i)) defeatedOpponents.add(id.getAsLong()); } catch (RuntimeException invalidId) {} }
            } catch (RuntimeException invalidRow) { /* A broken row must not disable other missions. */ }
            achievementList.add(new AchievementQuest(count, received));
        }
    }
    public synchronized String toJson() { return toJson(-1); }
    public synchronized void mergeStored(String json) {
        Achievement stored = new Achievement(null); stored.load(json); ensureSlots();
        for (int i = 0; i < achievementList.size(); i++) {
            AchievementQuest previous = stored.get(i), current = achievementList.get(i);
            if (previous != null) { current.completed = Math.max(current.completed, previous.completed); current.isRecieve |= previous.isRecieve; }
        }
        defeatedOpponents.addAll(stored.defeatedOpponents);
    }
    public synchronized String toJson(int claimedIndex) {
        ensureSlots(); JsonArray rows = new JsonArray();
        for (int i = 0; i < achievementList.size(); i++) {
            getCompleted(i); AchievementQuest quest = achievementList.get(i); JsonArray row = new JsonArray();
            row.add(quest.completed); row.add(quest.isRecieve || i == claimedIndex);
            if (i == ConstAchievement.TRAM_TRAN_TRAM_THANG) { JsonArray opponents = new JsonArray(); defeatedOpponents.stream().sorted().forEach(opponents::add); row.add(opponents); }
            rows.add(row.toString());
        }
        return rows.toString();
    }
    public synchronized void dispose() { if (achievementList != null) achievementList.clear(); achievementList = null; defeatedOpponents.clear(); player = null; }
}
