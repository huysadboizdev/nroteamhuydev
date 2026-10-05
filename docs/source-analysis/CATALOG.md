# Danh mục toàn bộ mã nguồn Java

Đã đọc tĩnh 548 tệp UTF-8, 104,276 dòng (bao gồm dòng trắng và chú thích).

Danh mục này được tạo bằng `.analysis/catalog_source.py`. Danh sách chữ ký dùng biểu thức chính quy, có thể thiếu phương thức nhiều dòng hoặc package-private; GitNexus cung cấp đồ thị cú pháp riêng. Đây là kiểm kê cấu trúc, không phải chứng nhận từng nhánh đã được kiểm thử.

## models (5 tệp)

### [AntiLogin.java](../../SRC/src/models/AntiLogin.java) — 35 dòng

Kiểu được nhận diện: AntiLogin.

```java
// dòng 13: public boolean canLogin() {
// dòng 17: public void wrong() {
// dòng 25: public void reset() {
// dòng 31: public String getNotifyCannotLogin() {
```

### [ConsignItem.java](../../SRC/src/models/ConsignItem.java) — 36 dòng

Kiểu được nhận diện: ConsignItem.

```java
// dòng 19: public ConsignItem(){
// dòng 22: public ConsignItem(int i,short id,int plId,byte t,int gold,int gem,int q,byte isUp,List<ItemOption> op,boolean b){
```

### [GiftCode.java](../../SRC/src/models/GiftCode.java) — 26 dòng

Kiểu được nhận diện: GiftCode.

```java
// dòng 19: public boolean isUsedGiftCode(Player player) {
// dòng 23: public boolean timeCode() {
```

### [ShenronEvent.java](../../SRC/src/models/ShenronEvent.java) — 237 dòng

Kiểu được nhận diện: ShenronEvent.

```java
// dòng 52: public void update() {
// dòng 80: public void reSummonShenron() {
// dòng 85: public void sendWhishesShenron() {
// dòng 89: public void showConfirmShenron(byte select) {
// dòng 100: public void activeShenron(boolean appear, byte type) {
// dòng 126: public void confirmWish() {
// dòng 220: public void shenronLeave() {
```

### [Template.java](../../SRC/src/models/Template.java) — 282 dòng

Kiểu được nhận diện: Template, ItemOptionTemplate, ItemTemplate, MobTemplate, NpcTemplate, MapTemplate, SkillTemplate, Part, PartDetail, HeadAvatar, FlagBag, BgItem, ArrHead2Frames, TileSetInfo, TileSetInfoData, WaitSuperRank, AchievementTemplate, AchievementQuest.

```java
// dòng 18: public ItemOptionTemplate() {
// dòng 21: public ItemOptionTemplate(int id, String name, int type) {
// dòng 62: public ItemTemplate() {
// dòng 65: public ItemTemplate(short id, byte type, byte gender, String name, String description, short iconID, short part, boolean isUpToUp, int strRequire) {
// dòng 126: public MapTemplate() {
// dòng 164: public Part() {
// dòng 177: public PartDetail(int iconId, byte dx, byte dy) {
// dòng 190: public HeadAvatar(int headId, int avatarId) {
// dòng 229: public TileSetInfo(List<TileSetInfoData> tileSetInfoData) {
// dòng 239: public TileSetInfoData(int type, int[] data) {
// dòng 250: public WaitSuperRank(long playerId, long rivalId) {
// dòng 263: public AchievementTemplate(String info1, String info2, int money, long maxCount) {
// dòng 276: public AchievementQuest(long completed, boolean isRecieve) {
```

## nro.models.Bot (7 tệp)

### [Bot.java](../../SRC/src/nro/models/Bot/Bot.java) — 271 dòng

Kiểu được nhận diện: Bot.

```java
// dòng 40: public Bot(short head, short body, short leg, int type, String name, BotGiaoDich shop, short flag) {
// dòng 52: public int MapToPow() {
// dòng 82: public void joinMap() {
// dòng 91: public Zone getRandomZone(int mapId) {
// dòng 123: public short getHead() {
// dòng 132: public short getBody() {
// dòng 141: public short getLeg() {
// dòng 150: public short getFlagBag() {
// dòng 155: public void update() {
// dòng 174: public void leakSkill() {
// dòng 190: public boolean UseLastTimeSkill() {
// dòng 199: private void increasePoint() {
// dòng 223: private boolean doUseTiemNang(long tiemNang) {
// dòng 232: public void useSkill(int skillId) {
// dòng 262: public String getChat() {
```

### [BotAttackplayer.java](../../SRC/src/nro/models/Bot/BotAttackplayer.java) — 270 dòng

Kiểu được nhận diện: BotAttackplayer.

```java
// dòng 35: public BotAttackplayer(short head, short body, short leg, int type, String name, short flag) {
// dòng 74: public void update() {
// dòng 94: private void findTarget() {
// dòng 113: private void followTarget() {
// dòng 119: private void tryAttack() {
// dòng 133: private void selectRandomSkill() {
// dòng 140: private void attackTarget() {
// dòng 186: public void changeMap(int mapId) {
// dòng 216: private void phanSatThuong(Player attacker, Player target, long dame) {
// dòng 249: private void hutHPMP(Player player, long dame, Player pl, Mob mob) {
```

### [BotGiaoDich.java](../../SRC/src/nro/models/Bot/BotGiaoDich.java) — 102 dòng

Kiểu được nhận diện: BotGiaoDich.

```java
// dòng 28: public BotGiaoDich(int item , int traodoi , int slot){
// dòng 34: public BotGiaoDich(BotGiaoDich shop){
// dòng 40: public void update(){
// dòng 45: public String getChat(){
// dòng 52: public void chat(){
// dòng 64: public void activeTraDe(Player pl){
// dòng 70: public void CheckTraDe(List<Item> item){
// dòng 84: public void active(int sl){
// dòng 92: public void mapL(){
```

### [BotManager.java](../../SRC/src/nro/models/Bot/BotManager.java) — 42 dòng

Kiểu được nhận diện: BotManager.

```java
// dòng 13: public static BotManager gI() {
// dòng 21: public void run() {
```

### [BotPemQuai.java](../../SRC/src/nro/models/Bot/BotPemQuai.java) — 186 dòng

Kiểu được nhận diện: BotPemQuai.

```java
// dòng 29: public BotPemQuai(Bot b) {
// dòng 39: private void autoChat() {
// dòng 57: public void update() {
// dòng 69: public void GetMobAttack() {
// dòng 77: public void Attack() {
// dòng 135: public void chanGeMap() {
// dòng 141: private void NhatItem() {
// dòng 180: public void run() {
```

### [BotSanBoss.java](../../SRC/src/nro/models/Bot/BotSanBoss.java) — 114 dòng

Kiểu được nhận diện: BotSanBoss.

```java
// dòng 26: public BotSanBoss(Bot b) {
// dòng 30: public void update() {
// dòng 34: public boolean isMap(int mapId) {
// dòng 41: public void GetBoss(int status) {
// dòng 70: public void GetSkil() {
// dòng 92: public void SanBot() {
```

### [NewBot.java](../../SRC/src/nro/models/Bot/NewBot.java) — 124 dòng

Kiểu được nhận diện: NewBot.

```java
// dòng 21: public static NewBot gI() {
// dòng 28: public void LoadPart() {
// dòng 47: public String Getname() {
// dòng 51: public int getIndex(int gender) {
// dòng 61: public void runBot(int type, BotGiaoDich shop, int slot) {
```

## nro.models.boss (4 tệp)

### [Boss.java](../../SRC/src/nro/models/boss/Boss.java) — 868 dòng

Kiểu được nhận diện: Boss.

```java
// dòng 108: public Boss(int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
// dòng 114: public Boss(BossType bossType, int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
// dòng 120: public Boss(int id, BossData... data) throws Exception {
// dòng 147: public Boss(BossType bossType, int id, BossData... data) throws Exception {
// dòng 203: public void initBase() {
// dòng 216: protected void initSkill() {
// dòng 232: protected void resetBase() {
// dòng 243: public short getHead() {
// dòng 254: public short getBody() {
// dòng 265: public short getLeg() {
// dòng 277: public short getFlagBag() {
// dòng 282: public byte getAura() {
// dòng 287: public byte getEffFront() {
// dòng 291: public Zone getMapJoin() {
// dòng 298: public void changeStatus(BossStatus status) {
// dòng 303: public Player getPlayerAttack() {
// dòng 322: public void changeToTypePK() {
// dòng 327: public void changeToTypeNonPK() {
// dòng 332: public void updateInfo() {
// dòng 337: public void update() {
// dòng 392: public void rest() {
// dòng 404: public void afk() {
// dòng 409: public void respawn() {
// dòng 419: public void joinMap() {
// dòng 502: public void joinMapByZone(Zone zone) {
// dòng 511: protected void notifyJoinMap() {
// dòng 517: private boolean canSendNotify() {
// dòng 526: public boolean chatS() {
// dòng 548: public void doneChatS() {
// dòng 553: public void chatM() {
// dòng 572: public void active() {
// dòng 582: public void attack() {
// dòng 615: public void checkPlayerDie(Player player) {
// dòng 621: protected int getRangeCanAttackWithSkillSelect() {
// dòng 632: public void die(Player plKill) {
// dòng 652: public void reward(Player plKill) {
// dòng 657: public boolean chatE() {
// dòng 679: public void doneChatE() {
// dòng 684: public void leaveMap() {
// dòng 698: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 722: public void moveToPlayer(Player pl) {
// dòng 729: public void moveTo(int x, int y) {
// dòng 735: public void chat(String text) {
// dòng 739: protected boolean chat(int prefix, String textChat) {
// dòng 775: public void wakeupAnotherBossWhenAppear() {
// dòng 799: public void wakeupAnotherBossWhenDisappear() {
// dòng 803: public void autoLeaveMap() {
// dòng 807: public void leaveMapNew() {
// dòng 815: public void setBom(Player plAtt) {
```

### [BossData.java](../../SRC/src/nro/models/boss/BossData.java) — 84 dòng

Kiểu được nhận diện: BossData.

```java
// dòng 40: private BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
// dòng 57: public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
// dòng 64: public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
// dòng 71: public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
// dòng 78: public BossData(String name, byte gender, short[] outfit, int dame, int[] hp,
```

### [BossesData.java](../../SRC/src/nro/models/boss/BossesData.java) — 2,708 dòng

Kiểu được nhận diện: BossesData.

### [BossID.java](../../SRC/src/nro/models/boss/BossID.java) — 199 dòng

Kiểu được nhận diện: BossID.

## nro.models.boss.Android (8 tệp)

### [Android13.java](../../SRC/src/nro/models/boss/Android/Android13.java) — 78 dòng

Kiểu được nhận diện: Android13.

```java
// dòng 18: public Android13() throws Exception {
// dòng 22: public void reward(Player plKill) {
// dòng 38: public void doneChatS() {
// dòng 56: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [Android14.java](../../SRC/src/nro/models/boss/Android/Android14.java) — 99 dòng

Kiểu được nhận diện: Android14.

```java
// dòng 21: public Android14() throws Exception {
// dòng 26: public void reward(Player plKill) {
// dòng 42: protected void resetBase() {
// dòng 48: public void active() {
// dòng 56: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 64: public void callApk13() {
// dòng 82: public void recoverHP() {
// dòng 87: public void doneChatS() {
```

### [Android15.java](../../SRC/src/nro/models/boss/Android/Android15.java) — 62 dòng

Kiểu được nhận diện: Android15.

```java
// dòng 17: public Android15() throws Exception {
// dòng 22: public void reward(Player plKill) {
// dòng 38: protected void resetBase() {
// dòng 44: public void active() {
// dòng 49: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 59: public void recoverHP() {
```

### [Android19.java](../../SRC/src/nro/models/boss/Android/Android19.java) — 80 dòng

Kiểu được nhận diện: Android19.

```java
// dòng 18: public Android19() throws Exception {
// dòng 23: public void reward(Player plKill) {
// dòng 39: public void autoLeaveMap() {
// dòng 49: public void joinMap() {
// dòng 56: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 74: public void wakeupAnotherBossWhenDisappear() {
```

### [DrKore.java](../../SRC/src/nro/models/boss/Android/DrKore.java) — 102 dòng

Kiểu được nhận diện: DrKore.

```java
// dòng 16: public DrKore() throws Exception {
// dòng 21: public void reward(Player plKill) {
// dòng 37: public void chatM() {
// dòng 55: public void autoLeaveMap() {
// dòng 65: public void joinMap() {
// dòng 71: public synchronized int injured(Player plAtt, int damage, boolean piercing, boolean isMobAttack) {
// dòng 88: public void doneChatS() {
// dòng 98: public void changeToTypePK() {
```

### [KingKong.java](../../SRC/src/nro/models/boss/Android/KingKong.java) — 59 dòng

Kiểu được nhận diện: KingKong.

```java
// dòng 18: public KingKong() throws Exception {
// dòng 23: public void reward(Player plKill) {
// dòng 39: public void autoLeaveMap() {
// dòng 49: public void joinMap() {
// dòng 56: public void doneChatS() {
```

### [Pic.java](../../SRC/src/nro/models/boss/Android/Pic.java) — 68 dòng

Kiểu được nhận diện: Pic.

```java
// dòng 18: public Pic() throws Exception {
// dòng 23: public void reward(Player plKill) {
// dòng 39: public void autoLeaveMap() {
// dòng 49: public void joinMap() {
// dòng 56: public void doneChatS() {
// dòng 61: public void doneChatE() {
```

### [Poc.java](../../SRC/src/nro/models/boss/Android/Poc.java) — 75 dòng

Kiểu được nhận diện: Poc.

```java
// dòng 18: public Poc() throws Exception {
// dòng 23: public void reward(Player plKill) {
// dòng 39: public void autoLeaveMap() {
// dòng 49: public void joinMap() {
// dòng 62: public void doneChatE() {
```

## nro.models.boss.Baby (1 tệp)

### [Baby.java](../../SRC/src/nro/models/boss/Baby/Baby.java) — 139 dòng

Kiểu được nhận diện: Baby.

```java
// dòng 19: public Baby() throws Exception {
// dòng 24: public void reward(Player plKill) {
// dòng 100: public void active() {
// dòng 108: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack
```

## nro.models.boss.Black_Goku (1 tệp)

### [BlackGoku.java](../../SRC/src/nro/models/boss/Black_Goku/BlackGoku.java) — 173 dòng

Kiểu được nhận diện: BlackGoku.

```java
// dòng 25: public BlackGoku() throws Exception {
// dòng 30: public void reward(Player plKill) {
// dòng 93: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 121: public void autoLeaveMap() {
// dòng 136: public void joinMap() {
// dòng 144: public void attack() {
```

## nro.models.boss.Boss_Manager (16 tệp)

### [AnTromManager.java](../../SRC/src/nro/models/boss/Boss_Manager/AnTromManager.java) — 18 dòng

Kiểu được nhận diện: AnTromManager.

```java
// dòng 11: public static AnTromManager gI() {
```

### [BossManager.java](../../SRC/src/nro/models/boss/Boss_Manager/BossManager.java) — 452 dòng

Kiểu được nhận diện: BossManager.

```java
// dòng 116: public static BossManager gI() {
// dòng 123: public BossManager() {
// dòng 129: public List<Boss> getBosses() {
// dòng 133: public void addBoss(Boss boss) {
// dòng 137: public void removeBoss(Boss boss) {
// dòng 141: public void loadBoss() {
// dòng 167: public void createBoss(int bossID, int total) {
// dòng 173: public Boss createBoss(int bossID) {
// dòng 373: public Boss getBoss(int id) {
// dòng 384: public void showListBoss(Player player) {
// dòng 423: public Boss getBossById(int bossId) {
// dòng 427: public boolean checkBosses(Zone zone, int BossID) {
// dòng 431: public Player findBossClone(Player player) {
// dòng 435: public Boss getBossById(int bossId, int mapId, int zoneId) {
// dòng 440: public void run() {
```

### [BrolyManager.java](../../SRC/src/nro/models/boss/Boss_Manager/BrolyManager.java) — 14 dòng

Kiểu được nhận diện: BrolyManager.

```java
// dòng 7: public static BrolyManager gI() {
```

### [ChristmasEventManager.java](../../SRC/src/nro/models/boss/Boss_Manager/ChristmasEventManager.java) — 14 dòng

Kiểu được nhận diện: ChristmasEventManager.

```java
// dòng 7: public static ChristmasEventManager gI() {
```

### [FinalBossManager.java](../../SRC/src/nro/models/boss/Boss_Manager/FinalBossManager.java) — 14 dòng

Kiểu được nhận diện: FinalBossManager.

```java
// dòng 7: public static FinalBossManager gI() {
```

### [GasDestroyManager.java](../../SRC/src/nro/models/boss/Boss_Manager/GasDestroyManager.java) — 43 dòng

Kiểu được nhận diện: GasDestroyManager.

```java
// dòng 11: public static GasDestroyManager gI() {
// dòng 19: public void run() {
```

### [HalloweenEventManager.java](../../SRC/src/nro/models/boss/Boss_Manager/HalloweenEventManager.java) — 13 dòng

Kiểu được nhận diện: HalloweenEventManager.

```java
// dòng 7: public static HalloweenEventManager gI() {
```

### [HungVuongEventManager.java](../../SRC/src/nro/models/boss/Boss_Manager/HungVuongEventManager.java) — 14 dòng

Kiểu được nhận diện: HungVuongEventManager.

```java
// dòng 7: public static HungVuongEventManager gI() {
```

### [LunarNewYearEventManager.java](../../SRC/src/nro/models/boss/Boss_Manager/LunarNewYearEventManager.java) — 14 dòng

Kiểu được nhận diện: LunarNewYearEventManager.

```java
// dòng 7: public static LunarNewYearEventManager gI() {
```

### [OtherBossManager.java](../../SRC/src/nro/models/boss/Boss_Manager/OtherBossManager.java) — 46 dòng

Kiểu được nhận diện: OtherBossManager.

```java
// dòng 13: public static OtherBossManager gI() {
// dòng 21: public void run() {
```

### [RedRibbonHQManager.java](../../SRC/src/nro/models/boss/Boss_Manager/RedRibbonHQManager.java) — 43 dòng

Kiểu được nhận diện: RedRibbonHQManager.

```java
// dòng 11: public static RedRibbonHQManager gI() {
// dòng 19: public void run() {
```

### [SkillSummonedManager.java](../../SRC/src/nro/models/boss/Boss_Manager/SkillSummonedManager.java) — 43 dòng

Kiểu được nhận diện: SkillSummonedManager.

```java
// dòng 11: public static SkillSummonedManager gI() {
// dòng 19: public void run() {
```

### [SnakeWayManager.java](../../SRC/src/nro/models/boss/Boss_Manager/SnakeWayManager.java) — 43 dòng

Kiểu được nhận diện: SnakeWayManager.

```java
// dòng 11: public static SnakeWayManager gI() {
// dòng 19: public void run() {
```

### [TreasureUnderSeaManager.java](../../SRC/src/nro/models/boss/Boss_Manager/TreasureUnderSeaManager.java) — 43 dòng

Kiểu được nhận diện: TreasureUnderSeaManager.

```java
// dòng 11: public static TreasureUnderSeaManager gI() {
// dòng 19: public void run() {
```

### [TrungThuEventManager.java](../../SRC/src/nro/models/boss/Boss_Manager/TrungThuEventManager.java) — 14 dòng

Kiểu được nhận diện: TrungThuEventManager.

```java
// dòng 7: public static TrungThuEventManager gI() {
```

### [YardartManager.java](../../SRC/src/nro/models/boss/Boss_Manager/YardartManager.java) — 14 dòng

Kiểu được nhận diện: YardartManager.

```java
// dòng 7: public static YardartManager gI() {
```

## nro.models.boss.Boss_mini (6 tệp)

### [AnTrom.java](../../SRC/src/nro/models/boss/Boss_mini/AnTrom.java) — 221 dòng

Kiểu được nhận diện: AnTrom.

```java
// dòng 30: public AnTrom() throws Exception {
// dòng 48: public Zone getMapJoin() {
// dòng 54: public Player getPlayerAttack() {
// dòng 59: public int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 76: public void attack() {
// dòng 125: public void moveTo(int x, int y) {
// dòng 132: public void die(Player plKill) {
// dòng 139: public void reward(Player plKill) {
// dòng 167: public void active() {
// dòng 178: public void joinMap() {
// dòng 188: public void joinMap2() {
// dòng 214: public void leaveMap() {
```

### [MatTroi.java](../../SRC/src/nro/models/boss/Boss_mini/MatTroi.java) — 215 dòng

Kiểu được nhận diện: MatTroi.

```java
// dòng 33: public MatTroi() throws Exception {
// dòng 49: public void die(Player plKill) {
// dòng 54: private void applyEffect(Player player) {
// dòng 61: private void checkGlobalEffects() {
// dòng 80: private void updateOdo() {
// dòng 97: public void attack() {
// dòng 125: public void reward(Player plKill) {
// dòng 146: public void joinMap() {
// dòng 155: public void joinMap2() {
// dòng 181: public void leaveMap() {
// dòng 189: public void active() {
// dòng 201: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [Odo.java](../../SRC/src/nro/models/boss/Boss_mini/Odo.java) — 245 dòng

Kiểu được nhận diện: Odo.

```java
// dòng 48: public Odo() throws Exception {
// dòng 65: public int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 79: private void updateOdo() {
// dòng 106: private void regenHp() {
// dòng 121: public void attack() {
// dòng 158: public void moveTo(int x, int y) {
// dòng 165: public void reward(Player plKill) {
// dòng 197: public void active() {
// dòng 208: public void joinMap() {
// dòng 213: public void joinMap2() {
// dòng 239: public void leaveMap() {
```

### [RongNhi.java](../../SRC/src/nro/models/boss/Boss_mini/RongNhi.java) — 62 dòng

Kiểu được nhận diện: RongNhi.

```java
// dòng 30: public RongNhi() throws Exception {
// dòng 35: public void joinMap() {
// dòng 41: public void reward(Player plKill) {
// dòng 54: public void autoLeaveMap() {
```

### [SoiHecQuyn.java](../../SRC/src/nro/models/boss/Boss_mini/SoiHecQuyn.java) — 199 dòng

Kiểu được nhận diện: SoiHecQuyn.

```java
// dòng 29: public SoiHecQuyn() throws Exception {
// dòng 34: public void joinMap() {
// dòng 89: public void chatM() {
// dòng 105: public void active() {
// dòng 110: public void autoLeaveMap() {
// dòng 117: public void leaveMap() {
// dòng 125: public void NhatXuong() {
// dòng 131: public boolean KiemTraNhatXuong() {
// dòng 136: public void attack() {
// dòng 174: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 179: public void reward(Player plKill) {
```

### [Virut.java](../../SRC/src/nro/models/boss/Boss_mini/Virut.java) — 213 dòng

Kiểu được nhận diện: Virut.

```java
// dòng 30: public Virut() throws Exception {
// dòng 46: public void die(Player plKill) {
// dòng 51: private void applyEffect(Player player) {
// dòng 58: private void checkGlobalEffects() {
// dòng 77: private void updateOdo() {
// dòng 94: public void attack() {
// dòng 122: public void reward(Player plKill) {
// dòng 143: public void joinMap() {
// dòng 152: public void joinMap2() {
// dòng 178: public void leaveMap() {
// dòng 186: public void active() {
// dòng 198: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

## nro.models.boss.Broly (2 tệp)

### [Broly.java](../../SRC/src/nro/models/boss/Broly/Broly.java) — 202 dòng

Kiểu được nhận diện: Broly.

```java
// dòng 20: public Broly() throws Exception {
// dòng 47: public void active() {
// dòng 55: public void joinMap() {
// dòng 65: public void joinMap2() {
// dòng 111: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 138: public void attack() {
// dòng 176: public void die(Player plKill) {
// dòng 180: private void tangChiSo() {
// dòng 189: public void leaveMap() {
```

### [SuperBroly.java](../../SRC/src/nro/models/boss/Broly/SuperBroly.java) — 180 dòng

Kiểu được nhận diện: SuperBroly.

```java
// dòng 22: public SuperBroly(Zone zone, int x, int y) throws Exception {
// dòng 52: public void reward(Player plKill) {
// dòng 59: public void active() {
// dòng 64: public void joinMap() {
// dòng 84: public void autoLeaveMap() {
// dòng 94: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 123: public void attack() {
// dòng 160: private void tangChiSo() {
// dòng 169: public void leaveMap() {
```

## nro.models.boss.Cell (9 tệp)

### [SieuBoHung.java](../../SRC/src/nro/models/boss/Cell/SieuBoHung.java) — 225 dòng

Kiểu được nhận diện: SieuBoHung.

```java
// dòng 40: public SieuBoHung() throws Exception {
// dòng 45: protected void resetBase() {
// dòng 50: public void callCellCon() {
// dòng 74: public void recoverHP() {
// dòng 79: public void reward(Player plKill) {
// dòng 143: public void active() {
// dòng 151: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 189: public void joinMap() {
// dòng 195: public void autoLeaveMap() {
// dòng 208: public void mc() {
```

### [XenBoHung.java](../../SRC/src/nro/models/boss/Cell/XenBoHung.java) — 104 dòng

Kiểu được nhận diện: XenBoHung.

```java
// dòng 26: public XenBoHung() throws Exception {
// dòng 31: public void reward(Player plKill) {
// dòng 48: public void active() {
// dòng 56: private void hapThu() {
// dòng 80: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [XENCON1.java](../../SRC/src/nro/models/boss/Cell/XENCON1.java) — 86 dòng

Kiểu được nhận diện: XENCON1.

```java
// dòng 22: public XENCON1() throws Exception {
// dòng 27: public void reward(Player plKill) {
// dòng 44: public void joinMap() {
// dòng 55: public void doneChatE() {
// dòng 69: public void leaveMap() {
// dòng 78: public void autoLeaveMap() {
```

### [XENCON2.java](../../SRC/src/nro/models/boss/Cell/XENCON2.java) — 86 dòng

Kiểu được nhận diện: XENCON2.

```java
// dòng 22: public XENCON2() throws Exception {
// dòng 27: public void reward(Player plKill) {
// dòng 44: public void joinMap() {
// dòng 55: public void doneChatE() {
// dòng 69: public void leaveMap() {
// dòng 78: public void autoLeaveMap() {
```

### [XENCON3.java](../../SRC/src/nro/models/boss/Cell/XENCON3.java) — 86 dòng

Kiểu được nhận diện: XENCON3.

```java
// dòng 22: public XENCON3() throws Exception {
// dòng 27: public void reward(Player plKill) {
// dòng 44: public void joinMap() {
// dòng 55: public void doneChatE() {
// dòng 69: public void leaveMap() {
// dòng 78: public void autoLeaveMap() {
```

### [XENCON4.java](../../SRC/src/nro/models/boss/Cell/XENCON4.java) — 85 dòng

Kiểu được nhận diện: XENCON4.

```java
// dòng 22: public XENCON4() throws Exception {
// dòng 27: public void reward(Player plKill) {
// dòng 43: public void joinMap() {
// dòng 54: public void doneChatE() {
// dòng 68: public void leaveMap() {
// dòng 77: public void autoLeaveMap() {
```

### [XENCON5.java](../../SRC/src/nro/models/boss/Cell/XENCON5.java) — 86 dòng

Kiểu được nhận diện: XENCON5.

```java
// dòng 22: public XENCON5() throws Exception {
// dòng 27: public void reward(Player plKill) {
// dòng 43: public void joinMap() {
// dòng 54: public void doneChatE() {
// dòng 68: public void leaveMap() {
// dòng 77: public void autoLeaveMap() {
```

### [XENCON6.java](../../SRC/src/nro/models/boss/Cell/XENCON6.java) — 86 dòng

Kiểu được nhận diện: XENCON6.

```java
// dòng 22: public XENCON6() throws Exception {
// dòng 27: public void reward(Player plKill) {
// dòng 43: public void joinMap() {
// dòng 54: public void doneChatE() {
// dòng 68: public void leaveMap() {
// dòng 77: public void autoLeaveMap() {
```

### [XENCON7.java](../../SRC/src/nro/models/boss/Cell/XENCON7.java) — 85 dòng

Kiểu được nhận diện: XENCON7.

```java
// dòng 22: public XENCON7() throws Exception {
// dòng 26: public void reward(Player plKill) {
// dòng 43: public void joinMap() {
// dòng 54: public void doneChatE() {
// dòng 68: public void leaveMap() {
// dòng 77: public void autoLeaveMap() {
```

## nro.models.boss.Cold (1 tệp)

### [Cooler.java](../../SRC/src/nro/models/boss/Cold/Cooler.java) — 127 dòng

Kiểu được nhận diện: Cooler.

```java
// dòng 25: public Cooler() throws Exception {
// dòng 30: public void reward(Player plKill) {
// dòng 92: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 111: public void joinMap() {
// dòng 118: public void autoLeaveMap() {
```

## nro.models.boss.Frieza (1 tệp)

### [Fide.java](../../SRC/src/nro/models/boss/Frieza/Fide.java) — 56 dòng

Kiểu được nhận diện: Fide.

```java
// dòng 20: public Fide() throws Exception {
// dòng 25: public void reward(Player plKill) {
// dòng 41: public void joinMap() {
// dòng 47: public void autoLeaveMap() {
```

## nro.models.boss.Golden_fireza (6 tệp)

### [DeathBeam1.java](../../SRC/src/nro/models/boss/Golden_fireza/DeathBeam1.java) — 155 dòng

Kiểu được nhận diện: DeathBeam1.

```java
// dòng 32: public DeathBeam1() throws Exception {
// dòng 36: public void reward(Player plKill) {
// dòng 49: public void joinMap() {
// dòng 62: public void active() {
// dòng 67: public void autoLeaveMap() {
// dòng 85: public void moveTo(int x, int y) {
// dòng 92: public void afk() {
// dòng 103: public void moveToPlayer(Player pl) {
// dòng 116: public void attack() {
// dòng 138: public void leaveMap() {
// dòng 146: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 151: public void setDie() {
```

### [DeathBeam2.java](../../SRC/src/nro/models/boss/Golden_fireza/DeathBeam2.java) — 155 dòng

Kiểu được nhận diện: DeathBeam2.

```java
// dòng 32: public DeathBeam2() throws Exception {
// dòng 37: public void joinMap() {
// dòng 50: public void active() {
// dòng 54: public void reward(Player plKill) {
// dòng 67: public void afk() {
// dòng 78: public void autoLeaveMap() {
// dòng 96: public void moveTo(int x, int y) {
// dòng 103: public void moveToPlayer(Player pl) {
// dòng 116: public void attack() {
// dòng 139: public void leaveMap() {
// dòng 147: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 152: public void setDie() {
```

### [DeathBeam3.java](../../SRC/src/nro/models/boss/Golden_fireza/DeathBeam3.java) — 156 dòng

Kiểu được nhận diện: DeathBeam3.

```java
// dòng 32: public DeathBeam3() throws Exception {
// dòng 36: public void reward(Player plKill) {
// dòng 49: public void joinMap() {
// dòng 62: public void active() {
// dòng 67: public void afk() {
// dòng 78: public void autoLeaveMap() {
// dòng 96: public void moveTo(int x, int y) {
// dòng 103: public void moveToPlayer(Player pl) {
// dòng 116: public void attack() {
// dòng 139: public void leaveMap() {
// dòng 147: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 152: public void setDie() {
```

### [DeathBeam4.java](../../SRC/src/nro/models/boss/Golden_fireza/DeathBeam4.java) — 156 dòng

Kiểu được nhận diện: DeathBeam4.

```java
// dòng 32: public DeathBeam4() throws Exception {
// dòng 37: public void joinMap() {
// dòng 49: public void reward(Player plKill) {
// dòng 62: public void active() {
// dòng 67: public void afk() {
// dòng 78: public void autoLeaveMap() {
// dòng 96: public void moveTo(int x, int y) {
// dòng 103: public void moveToPlayer(Player pl) {
// dòng 116: public void attack() {
// dòng 139: public void leaveMap() {
// dòng 147: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 152: public void setDie() {
```

### [DeathBeam5.java](../../SRC/src/nro/models/boss/Golden_fireza/DeathBeam5.java) — 156 dòng

Kiểu được nhận diện: DeathBeam5.

```java
// dòng 32: public DeathBeam5() throws Exception {
// dòng 36: public void reward(Player plKill) {
// dòng 49: public void joinMap() {
// dòng 62: public void active() {
// dòng 67: public void afk() {
// dòng 78: public void autoLeaveMap() {
// dòng 96: public void moveTo(int x, int y) {
// dòng 103: public void moveToPlayer(Player pl) {
// dòng 116: public void attack() {
// dòng 139: public void leaveMap() {
// dòng 147: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 152: public void setDie() {
```

### [GoldenFrieza.java](../../SRC/src/nro/models/boss/Golden_fireza/GoldenFrieza.java) — 223 dòng

Kiểu được nhận diện: GoldenFrieza.

```java
// dòng 36: public GoldenFrieza() throws Exception {
// dòng 41: public void reward(Player plKill) {
// dòng 55: public void active() {
// dòng 60: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 89: public void autoLeaveMap() {
// dòng 96: public void joinMap() {
// dòng 112: public void attack() {
// dòng 182: public void setBom() {
// dòng 216: public void leaveMap() {
```

## nro.models.boss.MajinBuu_12h (9 tệp)

### [BuiBui.java](../../SRC/src/nro/models/boss/MajinBuu_12h/BuiBui.java) — 151 dòng

Kiểu được nhận diện: BuiBui.

```java
// dòng 29: public BuiBui() throws Exception {
// dòng 33: public void reward(Player plKill) {
// dòng 95: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 129: public void afk() {
// dòng 142: public void die(Player plKill) {
```

### [BuiBui2.java](../../SRC/src/nro/models/boss/MajinBuu_12h/BuiBui2.java) — 234 dòng

Kiểu được nhận diện: BuiBui2.

```java
// dòng 35: public BuiBui2() throws Exception {
// dòng 40: public void reward(Player plKill) {
// dòng 102: private void slowPlayerInMap() {
// dòng 111: public void attack() {
// dòng 148: public void chatM() {
// dòng 177: public void afk() {
// dòng 191: public void die(Player plKill) {
// dòng 201: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [Cadic.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Cadic.java) — 324 dòng

Kiểu được nhận diện: Cadic.

```java
// dòng 44: public Cadic() throws Exception {
// dòng 49: public void joinMap() {
// dòng 60: public void doneChatS() {
// dòng 66: public void active() {
// dòng 71: public Player getPlayerAttack() {
// dòng 90: private void petrifyPlayersInTheMap() {
// dòng 100: public void afk() {
// dòng 113: public void die(Player plKill) {
// dòng 123: public void attack() {
// dòng 169: public void moveTo(int x, int y) {
// dòng 176: public void moveToPlayer(Player pl) {
// dòng 183: public void chatM() {
// dòng 208: public void autoLeaveMap() {
// dòng 225: public void reward(Player plKill) {
// dòng 288: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 317: public void leaveMap() {
```

### [Drabura.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Drabura.java) — 246 dòng

Kiểu được nhận diện: Drabura.

```java
// dòng 39: public Drabura() throws Exception {
// dòng 44: public void joinMap() {
// dòng 54: public void active() {
// dòng 59: public Player getPlayerAttack() {
// dòng 73: private void petrifyPlayersInTheMap() {
// dòng 83: public void afk() {
// dòng 96: public void die(Player plKill) {
// dòng 106: public void attack() {
// dòng 149: public void reward(Player plKill) {
// dòng 212: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [Drabura2.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Drabura2.java) — 192 dòng

Kiểu được nhận diện: Drabura2.

```java
// dòng 28: public Drabura2() throws Exception {
// dòng 33: public void joinMap() {
// dòng 44: public void reward(Player plKill) {
// dòng 107: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 150: public void rest() {
// dòng 172: public void autoLeaveMap() {
// dòng 179: public void afk() {
// dòng 185: public void leaveMap() {
```

### [Drabura3.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Drabura3.java) — 227 dòng

Kiểu được nhận diện: Drabura3.

```java
// dòng 35: public Drabura3() throws Exception {
// dòng 40: public void joinMap() {
// dòng 50: private void petrifyPlayersInTheMap() {
// dòng 60: public void reward(Player plKill) {
// dòng 123: public void autoLeaveMap() {
// dòng 130: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 154: public void afk() {
// dòng 163: public void die(Player plKill) {
// dòng 173: public void attack() {
// dòng 208: public void moveTo(int x, int y) {
// dòng 215: public void moveToPlayer(Player pl) {
// dòng 220: public void leaveMap() {
```

### [Goku.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Goku.java) — 324 dòng

Kiểu được nhận diện: Goku.

```java
// dòng 43: public Goku() throws Exception {
// dòng 48: public void joinMap() {
// dòng 61: public void doneChatS() {
// dòng 67: public void active() {
// dòng 72: public Player getPlayerAttack() {
// dòng 91: private void petrifyPlayersInTheMap() {
// dòng 101: public void afk() {
// dòng 114: public void die(Player plKill) {
// dòng 124: public void autoLeaveMap() {
// dòng 141: public void attack() {
// dòng 187: public void moveTo(int x, int y) {
// dòng 194: public void moveToPlayer(Player pl) {
// dòng 201: public void chatM() {
// dòng 226: public void reward(Player plKill) {
// dòng 289: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 317: public void leaveMap() {
```

### [Mabu.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Mabu.java) — 218 dòng

Kiểu được nhận diện: Mabu.

```java
// dòng 33: public Mabu() throws Exception {
// dòng 38: public void reward(Player plKill) {
// dòng 100: public void joinMap() {
// dòng 109: private void petrifyPlayersInTheMap() {
// dòng 123: public void attack() {
// dòng 158: public void autoLeaveMap() {
// dòng 162: public void rest() {
// dòng 181: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 209: public void leaveMap() {
```

### [Yacon.java](../../SRC/src/nro/models/boss/MajinBuu_12h/Yacon.java) — 206 dòng

Kiểu được nhận diện: Yacon.

```java
// dòng 35: public Yacon() throws Exception {
// dòng 40: public void reward(Player plKill) {
// dòng 102: public void attack() {
// dòng 142: public void afk() {
// dòng 155: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 198: public void die(Player plKill) {
```

## nro.models.boss.MajinBuu_14h (2 tệp)

### [Mabu2H.java](../../SRC/src/nro/models/boss/MajinBuu_14h/Mabu2H.java) — 184 dòng

Kiểu được nhận diện: Mabu2H.

```java
// dòng 34: public Mabu2H() throws Exception {
// dòng 39: public void joinMap() {
// dòng 47: private void eatPlayersInTheMap() {
// dòng 62: private void petrifyPlayersInTheMap() {
// dòng 74: public void attack() {
// dòng 119: public void reward(Player plKill) {
// dòng 127: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 165: public void die(Player plKill) {
```

### [SuperBu.java](../../SRC/src/nro/models/boss/MajinBuu_14h/SuperBu.java) — 135 dòng

Kiểu được nhận diện: SuperBu.

```java
// dòng 27: public SuperBu() throws Exception {
// dòng 32: public void joinMap() {
// dòng 41: public void attack() {
// dòng 77: public void reward(Player plKill) {
// dòng 85: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 113: public void die(Player plKill) {
```

## nro.models.boss.Nappa (3 tệp)

### [Kuku.java](../../SRC/src/nro/models/boss/Nappa/Kuku.java) — 53 dòng

Kiểu được nhận diện: Kuku.

```java
// dòng 19: public Kuku() throws Exception {
// dòng 24: public void joinMap() {
// dòng 30: public void reward(Player plKill) {
// dòng 45: public void autoLeaveMap() {
```

### [MapDauDinh.java](../../SRC/src/nro/models/boss/Nappa/MapDauDinh.java) — 50 dòng

Kiểu được nhận diện: MapDauDinh.

```java
// dòng 17: public MapDauDinh() throws Exception {
// dòng 22: public void joinMap() {
// dòng 28: public void reward(Player plKill) {
// dòng 42: public void autoLeaveMap() {
```

### [Rambo.java](../../SRC/src/nro/models/boss/Nappa/Rambo.java) — 53 dòng

Kiểu được nhận diện: Rambo.

```java
// dòng 19: public Rambo() throws Exception {
// dòng 24: public void joinMap() {
// dòng 30: public void reward(Player plKill) {
// dòng 45: public void autoLeaveMap() {
```

## nro.models.boss.Tau_PayPay (1 tệp)

### [TaoPaiPai.java](../../SRC/src/nro/models/boss/Tau_PayPay/TaoPaiPai.java) — 55 dòng

Kiểu được nhận diện: TaoPaiPai.

```java
// dòng 15: public TaoPaiPai() throws Exception {
// dòng 20: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

## nro.models.boss.ban_do_kho_bau (1 tệp)

### [TrungUyXanhLo.java](../../SRC/src/nro/models/boss/ban_do_kho_bau/TrungUyXanhLo.java) — 94 dòng

Kiểu được nhận diện: TrungUyXanhLo.

```java
// dòng 24: public TrungUyXanhLo(Zone zone, int level, int dame, int hp) throws Exception {
// dòng 44: public void reward(Player plKill) {
// dòng 55: public void joinMap() {
// dòng 61: public void doneChatS() {
// dòng 67: public void afk() {
// dòng 78: public void die(Player plKill) {
// dòng 86: public void leaveMap() {
```

## nro.models.boss.cumber (1 tệp)

### [Cumber.java](../../SRC/src/nro/models/boss/cumber/Cumber.java) — 165 dòng

Kiểu được nhận diện: Cumber.

```java
// dòng 26: public Cumber() throws Exception {
// dòng 31: public void reward(Player plKill) {
// dòng 84: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 112: public void autoLeaveMap() {
// dòng 127: public void joinMap() {
// dòng 135: public void attack() {
```

## nro.models.boss.dai_hoi_vo_thuat (14 tệp)

### [ChanXu.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/ChanXu.java) — 63 dòng

Kiểu được nhận diện: ChanXu.

```java
// dòng 19: public ChanXu(Player player) throws Exception {
// dòng 25: public void attack() {
```

### [ChaPa.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/ChaPa.java) — 15 dòng

Kiểu được nhận diện: ChaPa.

```java
// dòng 11: public ChaPa(Player player) throws Exception {
```

### [JackyChun.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/JackyChun.java) — 15 dòng

Kiểu được nhận diện: JackyChun.

```java
// dòng 11: public JackyChun(Player player) throws Exception {
```

### [LiuLiu.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/LiuLiu.java) — 15 dòng

Kiểu được nhận diện: LiuLiu.

```java
// dòng 11: public LiuLiu(Player player) throws Exception {
```

### [ODo.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/ODo.java) — 15 dòng

Kiểu được nhận diện: ODo.

```java
// dòng 11: public ODo(Player player) throws Exception {
```

### [Pocolo.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/Pocolo.java) — 15 dòng

Kiểu được nhận diện: Pocolo.

```java
// dòng 11: public Pocolo(Player player) throws Exception {
```

### [PonPut.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/PonPut.java) — 15 dòng

Kiểu được nhận diện: PonPut.

```java
// dòng 11: public PonPut(Player player) throws Exception {
```

### [SoiHecQuyn.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/SoiHecQuyn.java) — 15 dòng

Kiểu được nhận diện: SoiHecQuyn.

```java
// dòng 11: public SoiHecQuyn(Player player) throws Exception {
```

### [TauPayPay.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/TauPayPay.java) — 15 dòng

Kiểu được nhận diện: TauPayPay.

```java
// dòng 11: public TauPayPay(Player player) throws Exception {
```

### [The23rdMartialArtCongress.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/The23rdMartialArtCongress.java) — 144 dòng

Kiểu được nhận diện: The23rdMartialArtCongress.

```java
// dòng 22: public The23rdMartialArtCongress(BossType DHVT, int id, BossData data) throws Exception {
// dòng 28: public void checkPlayerDie(Player pl) {
// dòng 33: public void afk() {
// dòng 39: protected void goToXY(int x, int y, boolean isTeleport) {
// dòng 50: public void attack() {
// dòng 74: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 79: public void joinMap() {
// dòng 86: protected void immortalMp() {
// dòng 91: public void update() {
// dòng 126: protected void notifyPlayeKill(Player player) {
// dòng 131: public void die(Player plKill) {
// dòng 136: public void leaveMap() {
```

### [ThienXinHang.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/ThienXinHang.java) — 44 dòng

Kiểu được nhận diện: ThienXinHang.

```java
// dòng 15: public ThienXinHang(Player player) throws Exception {
// dòng 21: public void attack() {
// dòng 34: private void phanThan() {
```

### [ThienXinHangClone.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/ThienXinHangClone.java) — 101 dòng

Kiểu được nhận diện: ThienXinHangClone.

```java
// dòng 22: public ThienXinHangClone(int id, Player player) throws Exception {
// dòng 36: public void attack() {
// dòng 61: public void update() {
```

### [Xinbato.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/Xinbato.java) — 15 dòng

Kiểu được nhận diện: Xinbato.

```java
// dòng 11: public Xinbato(Player player) throws Exception {
```

### [Yamcha.java](../../SRC/src/nro/models/boss/dai_hoi_vo_thuat/Yamcha.java) — 15 dòng

Kiểu được nhận diện: Yamcha.

```java
// dòng 11: public Yamcha(Player player) throws Exception {
```

## nro.models.boss.doanh_trai (6 tệp)

### [NinjaAoTim.java](../../SRC/src/nro/models/boss/doanh_trai/NinjaAoTim.java) — 169 dòng

Kiểu được nhận diện: NinjaAoTim.

```java
// dòng 24: public NinjaAoTim(Zone zone, Clan clan, int dame, int hp) throws Exception {
// dòng 50: public void reward(Player plKill) {
// dòng 94: public void joinMap() {
// dòng 100: public void doneChatS() {
// dòng 105: public void active() {
// dòng 110: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 152: public void die(Player plKill) {
// dòng 160: public void leaveMap() {
```

### [NinjaClone.java](../../SRC/src/nro/models/boss/doanh_trai/NinjaClone.java) — 109 dòng

Kiểu được nhận diện: NinjaClone.

```java
// dòng 23: public NinjaClone(Zone zone, Boss boss, int dame, int hp, int id) throws Exception {
// dòng 47: public void reward(Player plKill) {
// dòng 59: public void joinMap() {
// dòng 66: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 92: public void die(Player plKill) {
// dòng 100: public void leaveMap() {
```

### [RobotVeSi.java](../../SRC/src/nro/models/boss/doanh_trai/RobotVeSi.java) — 135 dòng

Kiểu được nhận diện: RobotVeSi.

```java
// dòng 21: public RobotVeSi(Zone zone, int id, int dame, int hp) throws Exception {
// dòng 45: public void reward(Player plKill) {
// dòng 67: public void joinMap() {
// dòng 73: public void active() {
// dòng 78: public void doneChatS() {
// dòng 84: public void afk() {
// dòng 94: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 119: public void die(Player plKill) {
// dòng 127: public void leaveMap() {
```

### [TrungUyThep.java](../../SRC/src/nro/models/boss/doanh_trai/TrungUyThep.java) — 215 dòng

Kiểu được nhận diện: TrungUyThep.

```java
// dòng 27: public TrungUyThep(Zone zone, int dame, int hp) throws Exception {
// dòng 51: public void reward(Player plKill) {
// dòng 93: public void active() {
// dòng 98: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 123: public void joinMap() {
// dòng 129: public void doneChatS() {
// dòng 133: private void goToXY(int x, int y, boolean isTeleport) {
// dòng 147: private void goToPlayer(Player pl, boolean isTeleport) {
// dòng 152: public void attack() {
// dòng 186: private int getY(int x) {
// dòng 198: public void die(Player plKill) {
// dòng 206: public void leaveMap() {
```

### [TrungUyTrang.java](../../SRC/src/nro/models/boss/doanh_trai/TrungUyTrang.java) — 177 dòng

Kiểu được nhận diện: TrungUyTrang.

```java
// dòng 26: public TrungUyTrang(Zone zone, int dame, int hp) throws Exception {
// dòng 52: public void reward(Player plKill) {
// dòng 75: public void joinMap() {
// dòng 81: public void active() {
// dòng 86: public void doneChatS() {
// dòng 91: private void goToXY(int x, int y, boolean isTeleport) {
// dòng 101: private void goToPlayer(Player pl, boolean isTeleport) {
// dòng 106: public void attack() {
// dòng 135: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 160: public void die(Player plKill) {
// dòng 168: public void leaveMap() {
```

### [TrungUyXanhLo.java](../../SRC/src/nro/models/boss/doanh_trai/TrungUyXanhLo.java) — 157 dòng

Kiểu được nhận diện: TrungUyXanhLo.

```java
// dòng 21: public TrungUyXanhLo(Zone zone, int dame, int hp) throws Exception {
// dòng 46: public void reward(Player plKill) {
// dòng 88: public void active() {
// dòng 93: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 118: public void joinMap() {
// dòng 124: public void doneChatS() {
// dòng 130: public void afk() {
// dòng 141: public void die(Player plKill) {
// dòng 149: public void leaveMap() {
```

## nro.models.boss.event.Halloween (3 tệp)

### [BiMa.java](../../SRC/src/nro/models/boss/event/Halloween/BiMa.java) — 116 dòng

Kiểu được nhận diện: BiMa.

```java
// dòng 19: public BiMa() throws Exception {
// dòng 24: public void reward(Player plKill) {
// dòng 30: public void halloween(Player player) {
// dòng 37: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 65: public void attack() {
// dòng 99: public void joinMap() {
// dòng 108: public void autoLeaveMap() {
```

### [Doi.java](../../SRC/src/nro/models/boss/event/Halloween/Doi.java) — 116 dòng

Kiểu được nhận diện: Doi.

```java
// dòng 19: public Doi() throws Exception {
// dòng 24: public void reward(Player plKill) {
// dòng 30: public void halloween(Player player) {
// dòng 37: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 65: public void attack() {
// dòng 99: public void joinMap() {
// dòng 108: public void autoLeaveMap() {
```

### [MaTroi.java](../../SRC/src/nro/models/boss/event/Halloween/MaTroi.java) — 116 dòng

Kiểu được nhận diện: MaTroi.

```java
// dòng 19: public MaTroi() throws Exception {
// dòng 24: public void reward(Player plKill) {
// dòng 30: public void halloween(Player player) {
// dòng 37: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 65: public void attack() {
// dòng 99: public void joinMap() {
// dòng 108: public void autoLeaveMap() {
```

## nro.models.boss.event_hung_vuong (2 tệp)

### [SonTinh.java](../../SRC/src/nro/models/boss/event_hung_vuong/SonTinh.java) — 211 dòng

Kiểu được nhận diện: SonTinh.

```java
// dòng 32: public SonTinh() throws Exception {
// dòng 37: public void reward(Player plKill) {
// dòng 46: public void afk() {
// dòng 71: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 98: public void joinMap() {
// dòng 107: public void autoLeaveMap() {
// dòng 117: public void active() {
// dòng 122: public Player getPlayerAttack() {
// dòng 142: public void attack() {
// dòng 189: public void moveTo(int x, int y) {
// dòng 196: public void moveToPlayer(Player pl) {
// dòng 203: public void leaveMap() {
```

### [ThuyTinh.java](../../SRC/src/nro/models/boss/event_hung_vuong/ThuyTinh.java) — 209 dòng

Kiểu được nhận diện: ThuyTinh.

```java
// dòng 31: public ThuyTinh() throws Exception {
// dòng 36: public void reward(Player plKill) {
// dòng 47: public void afk() {
// dòng 69: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 96: public void joinMap() {
// dòng 105: public void autoLeaveMap() {
// dòng 115: public void active() {
// dòng 120: public Player getPlayerAttack() {
// dòng 140: public void attack() {
// dòng 187: public void moveTo(int x, int y) {
// dòng 194: public void moveToPlayer(Player pl) {
// dòng 201: public void leaveMap() {
```

## nro.models.boss.event_noel (1 tệp)

### [OngGiaNoel.java](../../SRC/src/nro/models/boss/event_noel/OngGiaNoel.java) — 175 dòng

Kiểu được nhận diện: OngGiaNoel.

```java
// dòng 23: public OngGiaNoel() throws Exception {
// dòng 28: public void joinMap() {
// dòng 78: public void chatM() {
// dòng 93: private void giftBox() {
// dòng 116: public void active() {
// dòng 121: public void autoLeaveMap() {
// dòng 128: public void leaveMap() {
// dòng 136: public void attack() {
// dòng 168: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 173: public void reward(Player plKill) {
```

## nro.models.boss.event_tet (1 tệp)

### [LanCon.java](../../SRC/src/nro/models/boss/event_tet/LanCon.java) — 222 dòng

Kiểu được nhận diện: LanCon.

```java
// dòng 24: public LanCon() throws Exception {
// dòng 29: public void joinMap() {
// dòng 79: public void chatM() {
// dòng 95: public void autoLeaveMap() {
// dòng 102: public void leaveMap() {
// dòng 111: public void attack() {
// dòng 142: public void afk() {
// dòng 182: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 214: private boolean isPlayerChatThang(Player plAtt) {
// dòng 220: public void reward(Player plKill) {
```

## nro.models.boss.event_trung_thu (3 tệp)

### [KhiDot.java](../../SRC/src/nro/models/boss/event_trung_thu/KhiDot.java) — 69 dòng

Kiểu được nhận diện: KhiDot.

```java
// dòng 16: public KhiDot() throws Exception {
// dòng 21: public void reward(Player plKill) {
// dòng 28: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 53: public void joinMap() {
// dòng 61: public void autoLeaveMap() {
```

### [NguyetThan.java](../../SRC/src/nro/models/boss/event_trung_thu/NguyetThan.java) — 207 dòng

Kiểu được nhận diện: NguyetThan.

```java
// dòng 32: public NguyetThan() throws Exception {
// dòng 37: public void reward(Player plKill) {
// dòng 45: public void afk() {
// dòng 67: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 94: public void joinMap() {
// dòng 103: public void autoLeaveMap() {
// dòng 113: public void active() {
// dòng 118: public Player getPlayerAttack() {
// dòng 138: public void attack() {
// dòng 185: public void moveTo(int x, int y) {
// dòng 192: public void moveToPlayer(Player pl) {
// dòng 199: public void leaveMap() {
```

### [NhatThan.java](../../SRC/src/nro/models/boss/event_trung_thu/NhatThan.java) — 205 dòng

Kiểu được nhận diện: NhatThan.

```java
// dòng 32: public NhatThan() throws Exception {
// dòng 37: public void reward(Player plKill) {
// dòng 43: public void afk() {
// dòng 65: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 92: public void joinMap() {
// dòng 101: public void autoLeaveMap() {
// dòng 111: public void active() {
// dòng 116: public Player getPlayerAttack() {
// dòng 136: public void attack() {
// dòng 183: public void moveTo(int x, int y) {
// dòng 190: public void moveToPlayer(Player pl) {
// dòng 197: public void leaveMap() {
```

## nro.models.boss.khi_gas (2 tệp)

### [DrLychee.java](../../SRC/src/nro/models/boss/khi_gas/DrLychee.java) — 158 dòng

Kiểu được nhận diện: DrLychee.

```java
// dòng 27: public DrLychee(Zone zone, Clan clan, int level, int dame, int hp) throws Exception {
// dòng 50: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 86: public void reward(Player plKill) {
// dòng 95: private void dropCt(int x) {
// dòng 123: public void joinMap() {
// dòng 130: public void die(Player plKill) {
// dòng 138: public void leaveMap() {
```

### [Hatchiyack.java](../../SRC/src/nro/models/boss/khi_gas/Hatchiyack.java) — 148 dòng

Kiểu được nhận diện: Hatchiyack.

```java
// dòng 27: public Hatchiyack(Zone zone, Clan clan, int level, int dame, int hp) throws Exception {
// dòng 49: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 85: public void reward(Player plKill) {
// dòng 94: private void dropCt(int x) {
// dòng 122: public void joinMap() {
// dòng 129: public void die(Player plKill) {
// dòng 137: public void leaveMap() {
```

## nro.models.boss.luyen_tap_tu_dong (10 tệp)

### [Karin.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/Karin.java) — 73 dòng

Kiểu được nhận diện: Karin.

```java
// dòng 17: public Karin(Player player) throws Exception {
// dòng 23: public void joinMap() {
// dòng 32: public boolean chatS() {
// dòng 52: public void bayLungTung() {
// dòng 64: public void afk() {
```

### [KhiBubbles.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/KhiBubbles.java) — 45 dòng

Kiểu được nhận diện: KhiBubbles.

```java
// dòng 13: public KhiBubbles(Player player) throws Exception {
// dòng 19: public void afk() {
// dòng 26: public boolean chatS() {
```

### [MrPoPo.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/MrPoPo.java) — 70 dòng

Kiểu được nhận diện: MrPoPo.

```java
// dòng 17: public MrPoPo(Player player) throws Exception {
// dòng 23: public void joinMap() {
// dòng 32: public void afk() {
// dòng 39: public boolean chatS() {
// dòng 59: public void bayLungTung() {
```

### [TauPayPay.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/TauPayPay.java) — 130 dòng

Kiểu được nhận diện: TauPayPay.

```java
// dòng 23: public TauPayPay(Player player) throws Exception {
// dòng 29: public void joinMap() {
// dòng 53: public boolean chatS() {
// dòng 73: public void leaveMap() {
// dòng 88: public void buffPea() {
// dòng 93: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 125: public void afk() {
```

### [ThanVuTru.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/ThanVuTru.java) — 58 dòng

Kiểu được nhận diện: ThanVuTru.

```java
// dòng 14: public ThanVuTru(Player player) throws Exception {
// dòng 20: public void joinMap() {
// dòng 29: public boolean chatS() {
// dòng 49: public void afk() {
```

### [ThuongDe.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/ThuongDe.java) — 83 dòng

Kiểu được nhận diện: ThuongDe.

```java
// dòng 20: public ThuongDe(Player player) throws Exception {
// dòng 26: public void joinMap() {
// dòng 35: public boolean chatS() {
// dòng 56: public void afk() {
// dòng 66: public void leaveMap() {
```

### [ToSuKaio.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/ToSuKaio.java) — 49 dòng

Kiểu được nhận diện: ToSuKaio.

```java
// dòng 18: public ToSuKaio(Player player) throws Exception {
// dòng 24: public void joinMap() {
// dòng 34: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 39: public void active() {
```

### [TrainingBoss.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/TrainingBoss.java) — 223 dòng

Kiểu được nhận diện: TrainingBoss.

```java
// dòng 36: public TrainingBoss(BossType LuyenTap, int id, BossData data) throws Exception {
// dòng 42: public void checkPlayerDie(Player pl) {
// dòng 54: public void active() {
// dòng 62: public void hutMau() {
// dòng 65: public void tanHinh() {
// dòng 69: public void bayLungTung() {
// dòng 73: public void buffPea() {
// dòng 82: protected void goToXY(int x, int y, boolean isTeleport) {
// dòng 92: protected void goToXY(int x, int y) {
// dòng 101: public void attack() {
// dòng 129: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 134: public void joinMap() {
// dòng 142: protected void immortalMp() {
// dòng 147: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 180: protected void notifyPlayeKill(Player player) {
// dòng 185: public void die(Player plKill) {
// dòng 196: public void leaveMap() {
```

### [Whis.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/Whis.java) — 148 dòng

Kiểu được nhận diện: Whis.

```java
// dòng 18: public Whis(Player player) throws Exception {
// dòng 30: public void joinMap() {
// dòng 47: public boolean chatS() {
// dòng 66: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 87: public void die(Player plKill) {
// dòng 118: public boolean chatE() {
// dòng 137: public void afk() {
```

### [Yajiro.java](../../SRC/src/nro/models/boss/luyen_tap_tu_dong/Yajiro.java) — 54 dòng

Kiểu được nhận diện: Yajiro.

```java
// dòng 14: public Yajiro(Player player) throws Exception {
// dòng 20: public void joinMap() {
// dòng 29: public void afk() {
// dòng 36: public boolean chatS() {
```

## nro.models.boss.ma_vuong_picolo (1 tệp)

### [Pocolo.java](../../SRC/src/nro/models/boss/ma_vuong_picolo/Pocolo.java) — 327 dòng

Kiểu được nhận diện: Pocolo.

```java
// dòng 40: public Pocolo(Zone zone, Player player, int dame, int hp) throws Exception {
// dòng 82: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 117: public void laze() {
// dòng 139: public void autoLeaveMap() {
// dòng 173: public void afk() {
// dòng 210: public void chatM() {
// dòng 232: public void attack() {
// dòng 270: public void reward(Player plKill) {
// dòng 274: public void joinMap() {
// dòng 284: public void die(Player plKill) {
// dòng 292: public void leaveMap() {
// dòng 302: public void mc() {
```

## nro.models.boss.nhan_ban (1 tệp)

### [NhanBan.java](../../SRC/src/nro/models/boss/nhan_ban/NhanBan.java) — 131 dòng

Kiểu được nhận diện: NhanBan.

```java
// dòng 27: public NhanBan(Player player, BossData bossData) throws Exception {
// dòng 34: public void reward(Player plKill) {
// dòng 43: public void active() {
// dòng 51: public void joinMap() {
// dòng 58: private void goToXY(int x, int y, boolean isTeleport) {
// dòng 68: private void goToPlayer(Player pl, boolean isTeleport) {
// dòng 73: public void attack() {
// dòng 102: public void die(Player plKill) {
// dòng 111: public void autoLeaveMap() {
// dòng 123: public void leaveMap() {
```

## nro.models.boss.sieu_hang (2 tệp)

### [Rival.java](../../SRC/src/nro/models/boss/sieu_hang/Rival.java) — 51 dòng

Kiểu được nhận diện: Rival.

```java
// dòng 13: public Rival(Player player, Player rival) throws Exception {
// dòng 19: public static BossData bossData(Player player) {
```

### [SuperRank.java](../../SRC/src/nro/models/boss/sieu_hang/SuperRank.java) — 347 dòng

Kiểu được nhận diện: SuperRank.

```java
// dòng 29: public SuperRank(BossType SuperRank, int id, BossData data) throws Exception {
// dòng 35: public void checkPlayerDie(Player pl) {
// dòng 40: public void afk() {
// dòng 46: protected void goToXY(int x, int y, boolean isTeleport) {
// dòng 57: public void attack() {
// dòng 102: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 107: public void joinMap() {
// dòng 135: protected void immortalHpMp() {
// dòng 141: public void update() {
// dòng 176: protected void notifyPlayeKill(Player player) {
// dòng 181: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 326: public void die(Player plKill) {
// dòng 331: public void leaveMap() {
```

## nro.models.boss.tieu_doi_sat_thu (5 tệp)

### [SO1.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu/SO1.java) — 88 dòng

Kiểu được nhận diện: SO1.

```java
// dòng 17: public SO1() throws Exception {
// dòng 22: public void moveTo(int x, int y) {
// dòng 30: public void reward(Player plKill) {
// dòng 46: protected void notifyJoinMap() {
// dòng 54: public void doneChatS() {
// dòng 59: public void autoLeaveMap() {
// dòng 69: public void joinMap() {
// dòng 75: public void doneChatE() {
```

### [SO2.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu/SO2.java) — 87 dòng

Kiểu được nhận diện: SO2.

```java
// dòng 17: public SO2() throws Exception {
// dòng 22: public void moveTo(int x, int y) {
// dòng 30: public void reward(Player plKill) {
// dòng 45: protected void notifyJoinMap() {
// dòng 53: public void doneChatS() {
// dòng 58: public void autoLeaveMap() {
// dòng 68: public void joinMap() {
// dòng 74: public void doneChatE() {
```

### [SO3.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu/SO3.java) — 87 dòng

Kiểu được nhận diện: SO3.

```java
// dòng 17: public SO3() throws Exception {
// dòng 22: public void moveTo(int x, int y) {
// dòng 30: public void reward(Player plKill) {
// dòng 45: protected void notifyJoinMap() {
// dòng 53: public void doneChatS() {
// dòng 58: public void joinMap() {
// dòng 64: public void autoLeaveMap() {
// dòng 74: public void doneChatE() {
```

### [SO4.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu/SO4.java) — 82 dòng

Kiểu được nhận diện: SO4.

```java
// dòng 17: public SO4() throws Exception {
// dòng 22: public void moveTo(int x, int y) {
// dòng 30: public void reward(Player plKill) {
// dòng 46: protected void notifyJoinMap() {
// dòng 54: public void joinMap() {
// dòng 60: public void autoLeaveMap() {
// dòng 70: public void doneChatE() {
```

### [TDT.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu/TDT.java) — 96 dòng

Kiểu được nhận diện: TDT.

```java
// dòng 20: public TDT() throws Exception {
// dòng 24: private void bodyChangePlayerInMap() {
// dòng 35: public void moveTo(int x, int y) {
// dòng 43: public void reward(Player plKill) {
// dòng 59: protected void notifyJoinMap() {
// dòng 67: public void attack() {
// dòng 77: public void joinMap() {
// dòng 83: public void doneChatS() {
// dòng 88: public void autoLeaveMap() {
```

## nro.models.boss.tieu_doi_sat_thu_namek (5 tệp)

### [SO1_NM.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu_namek/SO1_NM.java) — 107 dòng

Kiểu được nhận diện: SO1_NM.

```java
// dòng 20: public SO1_NM() throws Exception {
// dòng 25: public void moveTo(int x, int y) {
// dòng 33: public void reward(Player plKill) {
// dòng 65: protected void notifyJoinMap() {
// dòng 73: public void doneChatS() {
// dòng 78: public void autoLeaveMap() {
// dòng 88: public void joinMap() {
// dòng 94: public void doneChatE() {
```

### [SO2_NM.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu_namek/SO2_NM.java) — 106 dòng

Kiểu được nhận diện: SO2_NM.

```java
// dòng 20: public SO2_NM() throws Exception {
// dòng 25: public void moveTo(int x, int y) {
// dòng 33: public void reward(Player plKill) {
// dòng 64: protected void notifyJoinMap() {
// dòng 72: public void doneChatS() {
// dòng 77: public void autoLeaveMap() {
// dòng 87: public void joinMap() {
// dòng 93: public void doneChatE() {
```

### [SO3_NM.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu_namek/SO3_NM.java) — 106 dòng

Kiểu được nhận diện: SO3_NM.

```java
// dòng 19: public SO3_NM() throws Exception {
// dòng 24: public void moveTo(int x, int y) {
// dòng 32: public void reward(Player plKill) {
// dòng 64: protected void notifyJoinMap() {
// dòng 72: public void doneChatS() {
// dòng 77: public void joinMap() {
// dòng 83: public void autoLeaveMap() {
// dòng 93: public void doneChatE() {
```

### [SO4_NM.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu_namek/SO4_NM.java) — 100 dòng

Kiểu được nhận diện: SO4_NM.

```java
// dòng 19: public SO4_NM() throws Exception {
// dòng 24: public void moveTo(int x, int y) {
// dòng 32: public void reward(Player plKill) {
// dòng 64: protected void notifyJoinMap() {
// dòng 72: public void joinMap() {
// dòng 78: public void autoLeaveMap() {
// dòng 88: public void doneChatE() {
```

### [TDT_NM.java](../../SRC/src/nro/models/boss/tieu_doi_sat_thu_namek/TDT_NM.java) — 91 dòng

Kiểu được nhận diện: TDT_NM.

```java
// dòng 19: public TDT_NM() throws Exception {
// dòng 24: public void moveTo(int x, int y) {
// dòng 32: public void reward(Player plKill) {
// dòng 64: protected void notifyJoinMap() {
// dòng 72: public void joinMap() {
// dòng 78: public void doneChatS() {
// dòng 83: public void autoLeaveMap() {
```

## nro.models.boss.trai_dat (6 tệp)

### [BIDO.java](../../SRC/src/nro/models/boss/trai_dat/BIDO.java) — 100 dòng

Kiểu được nhận diện: BIDO.

```java
// dòng 19: public BIDO() throws Exception {
// dòng 24: public void moveTo(int x, int y) {
// dòng 32: public void reward(Player plKill) {
// dòng 64: protected void notifyJoinMap() {
// dòng 72: public void joinMap() {
// dòng 78: public void autoLeaveMap() {
// dòng 88: public void doneChatE() {
```

### [BOJACK.java](../../SRC/src/nro/models/boss/trai_dat/BOJACK.java) — 79 dòng

Kiểu được nhận diện: BOJACK.

```java
// dòng 20: public BOJACK() throws Exception {
// dòng 25: public void reward(Player plKill) {
// dòng 57: public void joinMap() {
// dòng 63: public void autoLeaveMap() {
// dòng 73: public void doneChatS() {
```

### [BUJIN.java](../../SRC/src/nro/models/boss/trai_dat/BUJIN.java) — 102 dòng

Kiểu được nhận diện: BUJIN.

```java
// dòng 20: public BUJIN() throws Exception {
// dòng 25: public void moveTo(int x, int y) {
// dòng 33: public void reward(Player plKill) {
// dòng 65: protected void notifyJoinMap() {
// dòng 73: public void autoLeaveMap() {
// dòng 83: public void joinMap() {
// dòng 89: public void doneChatE() {
```

### [KOGU.java](../../SRC/src/nro/models/boss/trai_dat/KOGU.java) — 102 dòng

Kiểu được nhận diện: KOGU.

```java
// dòng 20: public KOGU() throws Exception {
// dòng 25: public void moveTo(int x, int y) {
// dòng 33: public void reward(Player plKill) {
// dòng 65: protected void notifyJoinMap() {
// dòng 73: public void autoLeaveMap() {
// dòng 83: public void joinMap() {
// dòng 89: public void doneChatE() {
```

### [SUPER_BOJACK.java](../../SRC/src/nro/models/boss/trai_dat/SUPER_BOJACK.java) — 86 dòng

Kiểu được nhận diện: SUPER_BOJACK.

```java
// dòng 19: public SUPER_BOJACK() throws Exception {
// dòng 24: public void moveTo(int x, int y) {
// dòng 32: public void reward(Player plKill) {
// dòng 64: protected void notifyJoinMap() {
// dòng 72: public void joinMap() {
// dòng 78: public void autoLeaveMap() {
```

### [ZANGYA.java](../../SRC/src/nro/models/boss/trai_dat/ZANGYA.java) — 102 dòng

Kiểu được nhận diện: ZANGYA.

```java
// dòng 20: public ZANGYA() throws Exception {
// dòng 25: public void moveTo(int x, int y) {
// dòng 33: public void reward(Player plKill) {
// dòng 65: protected void notifyJoinMap() {
// dòng 73: public void joinMap() {
// dòng 79: public void autoLeaveMap() {
// dòng 89: public void doneChatE() {
```

## nro.models.boss.vo_dai_hat_mit (6 tệp)

### [BongBang.java](../../SRC/src/nro/models/boss/vo_dai_hat_mit/BongBang.java) — 78 dòng

Kiểu được nhận diện: BongBang.

```java
// dòng 16: public BongBang(Player player) throws Exception {
// dòng 21: public void attack() {
// dòng 47: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 51: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [DeathOrAliveArena.java](../../SRC/src/nro/models/boss/vo_dai_hat_mit/DeathOrAliveArena.java) — 188 dòng

Kiểu được nhận diện: DeathOrAliveArena.

```java
// dòng 23: public DeathOrAliveArena(BossType NgocRong, int id, BossData data) throws Exception {
// dòng 29: public void checkPlayerDie(Player pl) {
// dòng 33: public void hutMau() {
// dòng 37: public void tanHinh() {
// dòng 41: public void bayLungTung() {
// dòng 46: public void afk() {
// dòng 52: protected void goToXY(int x, int y, boolean isTeleport) {
// dòng 63: public void attack() {
// dòng 89: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 94: public void joinMap() {
// dòng 103: protected void immortalMp() {
// dòng 108: public void update() {
// dòng 143: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 170: protected void notifyPlayeKill(Player player) {
// dòng 175: public void die(Player plKill) {
// dòng 180: public void leaveMap() {
```

### [Dracula.java](../../SRC/src/nro/models/boss/vo_dai_hat_mit/Dracula.java) — 99 dòng

Kiểu được nhận diện: Dracula.

```java
// dòng 19: public Dracula(Player player) throws Exception {
// dòng 25: public void hutMau() {
// dòng 40: public void attack() {
// dòng 67: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 71: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [NguoiVoHinh.java](../../SRC/src/nro/models/boss/vo_dai_hat_mit/NguoiVoHinh.java) — 102 dòng

Kiểu được nhận diện: NguoiVoHinh.

```java
// dòng 20: public NguoiVoHinh(Player player) throws Exception {
// dòng 27: public void attack() {
// dòng 53: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 57: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 84: public void tanHinh() {
```

### [ThoDauBac.java](../../SRC/src/nro/models/boss/vo_dai_hat_mit/ThoDauBac.java) — 80 dòng

Kiểu được nhận diện: ThoDauBac.

```java
// dòng 16: public ThoDauBac(Player player) throws Exception {
// dòng 22: public void attack() {
// dòng 48: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 52: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

### [VuaQuySaTang.java](../../SRC/src/nro/models/boss/vo_dai_hat_mit/VuaQuySaTang.java) — 80 dòng

Kiểu được nhận diện: VuaQuySaTang.

```java
// dòng 18: public VuaQuySaTang(Player player) throws Exception {
// dòng 24: public void attack() {
// dòng 50: protected void goToPlayer(Player pl, boolean isTeleport) {
// dòng 54: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

## nro.models.boss.yardrat (19 tệp)

### [CHIENBINH0.java](../../SRC/src/nro/models/boss/yardrat/CHIENBINH0.java) — 26 dòng

Kiểu được nhận diện: CHIENBINH0.

```java
// dòng 10: public CHIENBINH0() throws Exception {
// dòng 15: protected void init() {
```

### [CHIENBINH1.java](../../SRC/src/nro/models/boss/yardrat/CHIENBINH1.java) — 25 dòng

Kiểu được nhận diện: CHIENBINH1.

```java
// dòng 10: public CHIENBINH1() throws Exception {
// dòng 15: protected void init() {
```

### [CHIENBINH2.java](../../SRC/src/nro/models/boss/yardrat/CHIENBINH2.java) — 25 dòng

Kiểu được nhận diện: CHIENBINH2.

```java
// dòng 10: public CHIENBINH2() throws Exception {
// dòng 15: protected void init() {
```

### [CHIENBINH3.java](../../SRC/src/nro/models/boss/yardrat/CHIENBINH3.java) — 26 dòng

Kiểu được nhận diện: CHIENBINH3.

```java
// dòng 10: public CHIENBINH3() throws Exception {
// dòng 15: protected void init() {
```

### [CHIENBINH4.java](../../SRC/src/nro/models/boss/yardrat/CHIENBINH4.java) — 26 dòng

Kiểu được nhận diện: CHIENBINH4.

```java
// dòng 10: public CHIENBINH4() throws Exception {
// dòng 15: protected void init() {
```

### [CHIENBINH5.java](../../SRC/src/nro/models/boss/yardrat/CHIENBINH5.java) — 26 dòng

Kiểu được nhận diện: CHIENBINH5.

```java
// dòng 10: public CHIENBINH5() throws Exception {
// dòng 15: protected void init() {
```

### [DOITRUONG5.java](../../SRC/src/nro/models/boss/yardrat/DOITRUONG5.java) — 25 dòng

Kiểu được nhận diện: DOITRUONG5.

```java
// dòng 10: public DOITRUONG5() throws Exception {
// dòng 15: protected void init() {
```

### [TANBINH0.java](../../SRC/src/nro/models/boss/yardrat/TANBINH0.java) — 25 dòng

Kiểu được nhận diện: TANBINH0.

```java
// dòng 10: public TANBINH0() throws Exception {
// dòng 15: protected void init() {
```

### [TANBINH1.java](../../SRC/src/nro/models/boss/yardrat/TANBINH1.java) — 25 dòng

Kiểu được nhận diện: TANBINH1.

```java
// dòng 10: public TANBINH1() throws Exception {
// dòng 15: protected void init() {
```

### [TANBINH2.java](../../SRC/src/nro/models/boss/yardrat/TANBINH2.java) — 25 dòng

Kiểu được nhận diện: TANBINH2.

```java
// dòng 10: public TANBINH2() throws Exception {
// dòng 15: protected void init() {
```

### [TANBINH3.java](../../SRC/src/nro/models/boss/yardrat/TANBINH3.java) — 25 dòng

Kiểu được nhận diện: TANBINH3.

```java
// dòng 10: public TANBINH3() throws Exception {
// dòng 15: protected void init() {
```

### [TANBINH4.java](../../SRC/src/nro/models/boss/yardrat/TANBINH4.java) — 25 dòng

Kiểu được nhận diện: TANBINH4.

```java
// dòng 10: public TANBINH4() throws Exception {
// dòng 15: protected void init() {
```

### [TANBINH5.java](../../SRC/src/nro/models/boss/yardrat/TANBINH5.java) — 25 dòng

Kiểu được nhận diện: TANBINH5.

```java
// dòng 10: public TANBINH5() throws Exception {
// dòng 15: protected void init() {
```

### [TAPSU0.java](../../SRC/src/nro/models/boss/yardrat/TAPSU0.java) — 24 dòng

Kiểu được nhận diện: TAPSU0.

```java
// dòng 9: public TAPSU0() throws Exception {
// dòng 14: protected void init() {
```

### [TAPSU1.java](../../SRC/src/nro/models/boss/yardrat/TAPSU1.java) — 25 dòng

Kiểu được nhận diện: TAPSU1.

```java
// dòng 10: public TAPSU1() throws Exception {
// dòng 15: protected void init() {
```

### [TAPSU2.java](../../SRC/src/nro/models/boss/yardrat/TAPSU2.java) — 25 dòng

Kiểu được nhận diện: TAPSU2.

```java
// dòng 10: public TAPSU2() throws Exception {
// dòng 15: protected void init() {
```

### [TAPSU3.java](../../SRC/src/nro/models/boss/yardrat/TAPSU3.java) — 25 dòng

Kiểu được nhận diện: TAPSU3.

```java
// dòng 10: public TAPSU3() throws Exception {
// dòng 15: protected void init() {
```

### [TAPSU4.java](../../SRC/src/nro/models/boss/yardrat/TAPSU4.java) — 25 dòng

Kiểu được nhận diện: TAPSU4.

```java
// dòng 10: public TAPSU4() throws Exception {
// dòng 15: protected void init() {
```

### [Yardart.java](../../SRC/src/nro/models/boss/yardrat/Yardart.java) — 267 dòng

Kiểu được nhận diện: Yardart.

```java
// dòng 36: public Yardart(BossType Yardart, int id, BossData... data) throws Exception {
// dòng 41: public void respawn() {
// dòng 47: public void joinMap() {
// dòng 80: protected void init() {
// dòng 85: public void reward(Player plKill) {
// dòng 95: protected int getRangeCanAttackWithSkillSelect() {
// dòng 109: public void attack() {
// dòng 208: public void moveToPlayer(Player pl) {
// dòng 215: public void moveTo(int x, int y) {
// dòng 224: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
```

## nro.models.boss_con_duong_ran_doc (3 tệp)

### [CADICH.java](../../SRC/src/nro/models/boss_con_duong_ran_doc/CADICH.java) — 208 dòng

Kiểu được nhận diện: CADICH.

```java
// dòng 34: public CADICH(Zone zone, Clan clan, int dame, int hp) throws Exception {
// dòng 58: public void reward(Player plKill) {
// dòng 73: public void afk() {
// dòng 84: public void joinMap() {
// dòng 90: public void die(Player plKill) {
// dòng 98: public void leaveMap() {
// dòng 113: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 142: public void attack() {
```

### [NADIC.java](../../SRC/src/nro/models/boss_con_duong_ran_doc/NADIC.java) — 135 dòng

Kiểu được nhận diện: NADIC.

```java
// dòng 28: public NADIC(Zone zone, Clan clan, int dame, int hp) throws Exception {
// dòng 49: public void reward(Player plKill) {
// dòng 58: public void afk() {
// dòng 69: public void joinMap() {
// dòng 75: public void die(Player plKill) {
// dòng 89: public void leaveMap() {
// dòng 99: public void attack() {
```

### [SAIBAMEN.java](../../SRC/src/nro/models/boss_con_duong_ran_doc/SAIBAMEN.java) — 175 dòng

Kiểu được nhận diện: SAIBAMEN.

```java
// dòng 33: public SAIBAMEN(Zone zone, Clan clan, int id, int dame, int hp) throws Exception {
// dòng 53: public void reward(Player plKill) {
// dòng 62: public void afk() {
// dòng 82: public void joinMap() {
// dòng 88: public void die(Player plKill) {
// dòng 96: public void leaveMap() {
// dòng 106: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 130: public void setBom(Player plAtt) {
```

## nro.models.clan (3 tệp)

### [Clan.java](../../SRC/src/nro/models/clan/Clan.java) — 500 dòng

Kiểu được nhận diện: Clan.

```java
// dòng 81: public Clan() {
// dòng 94: public boolean canUpdateClan(Player player) {
// dòng 103: public ClanMember getLeader() {
// dòng 114: public byte getRole(Player player) {
// dòng 123: public boolean isLeader(Player player) {
// dòng 132: public boolean isDeputy(Player player) {
// dòng 142: public void addSMTNClan(Player plOri, long param) {
// dòng 158: public void sendMessageClan(ClanMessage cmg) {
// dòng 189: public void addClanMessage(ClanMessage cmg) {
// dòng 198: public ClanMessage getClanMessage(int clanMessageId) {
// dòng 207: public List<ClanMessage> getCurrClanMessages() {
// dòng 219: public void sendMyClanForAllMember() {
// dòng 227: public void sendFlagBagForAllMember() {
// dòng 235: public void addMemberOnline(Player player) {
// dòng 239: public void removeMemberOnline(ClanMember cm, Player player) {
// dòng 253: public Player getPlayerOnline(int playerId) {
// dòng 269: public void addClanMember(ClanMember cm) {
// dòng 274: public void addClanMember(Player player, byte role) {
// dòng 281: public void removeClanMember(ClanMember cm) {
// dòng 286: public byte getCurrMembers() {
// dòng 290: public List<ClanMember> getMembers() {
// dòng 294: public ClanMember getClanMember(int memberId) {
// dòng 303: public void reloadClanMember() {
// dòng 312: public void insert() {
// dòng 373: public void update() {
// dòng 430: public void deleteDB(int id) {
// dòng 442: public void updatethanhTichBDKB(int clanId) {
// dòng 453: public void updatethanhTichBDKBForLeader() {
// dòng 467: public void updatethanhTichKhiGasForLeader() {
// dòng 478: public void updatethanhTichCDRDForLeader() {
// dòng 489: public void updateThongTinLeader(int clanId) {
```

### [ClanMember.java](../../SRC/src/nro/models/clan/ClanMember.java) — 74 dòng

Kiểu được nhận diện: ClanMember.

```java
// dòng 45: public ClanMember() {
// dòng 48: public ClanMember(Player player, Clan clan, byte role) {
// dòng 65: public int getNumDateFromJoinTimeToToday() {
// dòng 69: public void dispose() {
```

### [ClanMessage.java](../../SRC/src/nro/models/clan/ClanMessage.java) — 44 dòng

Kiểu được nhận diện: ClanMessage.

```java
// dòng 33: public ClanMessage(Clan clan) {
// dòng 40: public void dispose() {
```

## nro.models.combine (32 tệp)

### [CheTaoCuonSachCu.java](../../SRC/src/nro/models/combine/CheTaoCuonSachCu.java) — 110 dòng

Kiểu được nhận diện: CheTaoCuonSachCu.

```java
// dòng 26: public static void showCombine(Player player) {
// dòng 44: public static void cheTaoCuonSachCu(Player player) {
// dòng 66: private static int getItemQuantity(Player player, int itemId) {
// dòng 71: private static String formatRequirement(String itemName, int currentQuantity, int requiredQuantity) {
// dòng 76: private static String formatSuccessRate(int quantityTrangSachCu, int quantityBiaSach) {
// dòng 82: private static boolean hasSufficientSpace(Player player) {
// dòng 86: private static void processSuccess(Player player, Item trangSachCu, Item biaSach) {
// dòng 101: private static void processFailure(Player player, Item trangSachCu, Item biaSach) {
```

### [CheTaoDuiDuc.java](../../SRC/src/nro/models/combine/CheTaoDuiDuc.java) — 84 dòng

Kiểu được nhận diện: CheTaoDuiDuc.

```java
// dòng 21: public static void showInfoCombine(Player player) {
// dòng 52: public static void CheTaoDuiDuc(Player player) {
```

### [CheTaoTrangBiThienSu.java](../../SRC/src/nro/models/combine/CheTaoTrangBiThienSu.java) — 173 dòng

Kiểu được nhận diện: CheTaoTrangBiThienSu.

```java
// dòng 21: public static void showInfoCombine(Player player) {
// dòng 140: public static void CheTaoTS(Player player) {
```

### [ChuyenHoaTrangBi_Ngoc.java](../../SRC/src/nro/models/combine/ChuyenHoaTrangBi_Ngoc.java) — 203 dòng

Kiểu được nhận diện: ChuyenHoaTrangBi_Ngoc.

```java
// dòng 17: public static void chuyenHoaTrangBiNgoc(Player player) {
// dòng 98: public static void thucHienChuyenHoa(Player player) {
// dòng 146: private boolean shouldSkipOption(ItemOption io) {
// dòng 153: private static boolean isCheckTrungTypevsGender(Item item, Item item2) {
// dòng 157: private static boolean isTrangBiGoc(Item item) {
// dòng 161: private static boolean isTrangBiChuyenHoa(Item item) {
// dòng 165: private static boolean isDoLuongLong(Item item) {
// dòng 171: private static boolean isDoJean(Item item) {
// dòng 176: private static boolean isDoZelot(Item item) {
// dòng 181: private static boolean isDoThanXD(Item item) {
// dòng 185: private static boolean isDoThanTD(Item item) {
// dòng 189: private static boolean isDoThanNM(Item item) {
// dòng 193: private static boolean isBaseStatOption(int id) {
// dòng 197: private static boolean isIgnoredOption(int id) {
```

### [ChuyenHoaTrangBi_Vang.java](../../SRC/src/nro/models/combine/ChuyenHoaTrangBi_Vang.java) — 196 dòng

Kiểu được nhận diện: ChuyenHoaTrangBi_Vang.

```java
// dòng 17: public static void chuyenHoaTrangBiVang(Player player) {
// dòng 98: public static void thucHienChuyenHoa(Player player) {
// dòng 146: private static boolean isCheckTrungTypevsGender(Item item, Item item2) {
// dòng 150: private static boolean isTrangBiGoc(Item item) {
// dòng 154: private static boolean isTrangBiChuyenHoa(Item item) {
// dòng 158: private static boolean isDoLuongLong(Item item) {
// dòng 164: private static boolean isDoJean(Item item) {
// dòng 169: private static boolean isDoZelot(Item item) {
// dòng 174: private static boolean isDoThanXD(Item item) {
// dòng 178: private static boolean isDoThanTD(Item item) {
// dòng 182: private static boolean isDoThanNM(Item item) {
// dòng 186: private static boolean isBaseStatOption(int id) {
// dòng 190: private static boolean isIgnoredOption(int id) {
```

### [Combine.java](../../SRC/src/nro/models/combine/Combine.java) — 46 dòng

Kiểu được nhận diện: Combine.

```java
// dòng 26: public Combine() {
// dòng 30: public void clearItemCombine() {
// dòng 34: public void clearParamCombine() {
// dòng 43: public void dispose() {
```

### [CombineService.java](../../SRC/src/nro/models/combine/CombineService.java) — 737 dòng

Kiểu được nhận diện: CombineService.

```java
// dòng 64: private CombineService() {
// dòng 69: public static CombineService gI() {
// dòng 82: public void showInfoCombine(Player player, int[] index) {
// dòng 173: public void startCombine(Player player) {
// dòng 255: public void startCombineVip(Player player, int n) {
// dòng 274: public void openTabCombine(Player player, int type) {
// dòng 302: public void sendAddItemCombine(Player player, int npcId, Item... items) {
// dòng 324: public void sendEffectOpenItem(Player player, short icon1, short icon2) {
// dòng 340: public void sendEffectCombineItem(Player player, byte type, short icon1, short icon2) {
// dòng 387: private int getTempIdItemC0(int gender, int type) {
// dòng 433: private String getNameItemC0(int gender, int type) {
// dòng 483: public boolean CheckSlot(Item trangBi, int starEmpty) {
// dòng 499: public void sendEffSuccessVip(Player player, int iconID) {
// dòng 511: public void sendEffectSuccessCombine(Player player) {
// dòng 531: public void sendEffFailVip(Player player) {
// dòng 542: public void sendEffectFailCombine(Player player) {
// dòng 562: public void reOpenItemCombine(Player player) {
// dòng 591: public void sendEffectCombineDB(Player player, short icon) {
// dòng 607: private String getTextTopTabCombine(int type) {
// dòng 667: private String getTextInfoTabCombine(int type) {
```

### [CombineSystem.java](../../SRC/src/nro/models/combine/CombineSystem.java) — 354 dòng

Kiểu được nhận diện: CombineSystem.

```java
// dòng 13: public static boolean isTrangBiPhaLeHoa(Item item) {
// dòng 21: public static boolean isDaPhaLe(Item item) {
// dòng 25: public static int getGemEpSao(int star) {
// dòng 61: public static int getOptionDaPhaLe(Item daPhaLe) {
// dòng 99: public static int getParamDaPhaLe(Item daPhaLe) {
// dòng 137: public static int getGoldPhaLeHoa(int star) {
// dòng 162: public static float getRatioPhaLeHoa(int star) {
// dòng 195: public static int getGemPhaLeHoa(int star) {
// dòng 227: public static boolean isCoupleItemNangCapCheck(Item trangBi, Item daNangCap) {
// dòng 245: public static int getCountDaNangCapDo(int level) {
// dòng 267: public static int getCountDaBaoVe(int level) {
// dòng 271: public static int getGoldNangCapDo(int level) {
// dòng 293: public static double getTileNangCapDo(int level) {
// dòng 325: private static boolean rollSuccess(Player player, Item.ItemOption optionStar) {
```

### [CuongHoaLoSaoPhaLe.java](../../SRC/src/nro/models/combine/CuongHoaLoSaoPhaLe.java) — 201 dòng

Kiểu được nhận diện: CuongHoaLoSaoPhaLe.

```java
// dòng 20: public static void showInfoCombine(Player player) {
// dòng 75: public static void cuongHoaLoSaoPhaLe(Player player) {
```

### [DanhBongSaoPhaLe.java](../../SRC/src/nro/models/combine/DanhBongSaoPhaLe.java) — 133 dòng

Kiểu được nhận diện: DanhBongSaoPhaLe.

```java
// dòng 22: public static void showInfoCombine(Player player) {
// dòng 61: public static void danhBongSaoPhaLe(Player player) {
// dòng 113: private static int saoPhaLeLapLanhId(int saoPhaLeCap1Id) {
```

### [DoiSachTuyetKy.java](../../SRC/src/nro/models/combine/DoiSachTuyetKy.java) — 159 dòng

Kiểu được nhận diện: DoiSachTuyetKy.

```java
// dòng 25: public static void showCombine(Player player) {
// dòng 53: private static String formatNote(String note) {
// dòng 57: private static String formatRequirement(String itemName, int current, int required) {
// dòng 62: public static void doiSachTuyetKy(Player player, boolean useConDau) {
// dòng 91: private static void processSuccess(Player player, Item cuonSachCu, Item kimBamGiay, Item conDau, boolean useConDau) {
// dòng 108: private static void processFailure(Player player, Item cuonSachCu, Item kimBamGiay) {
// dòng 130: private static Item createSach(byte gender, boolean useConDau) {
```

### [EpSaoTrangBi.java](../../SRC/src/nro/models/combine/EpSaoTrangBi.java) — 169 dòng

Kiểu được nhận diện: EpSaoTrangBi.

```java
// dòng 18: public static void showInfoCombine(Player player) {
// dòng 80: public static void epSaoTrangBi(Player player) {
```

### [GiamDinhSach.java](../../SRC/src/nro/models/combine/GiamDinhSach.java) — 90 dòng

Kiểu được nhận diện: GiamDinhSach.

```java
// dòng 18: public static void showInfoCombine(Player player) {
// dòng 53: public static void giamDinhSach(Player player) {
```

### [HocTuyetKy.java](../../SRC/src/nro/models/combine/HocTuyetKy.java) — 117 dòng

Kiểu được nhận diện: HocTuyetKy.

```java
// dòng 22: public static void showInfoCombine(Player player) {
// dòng 63: public static void hocTuyetKy(Player player) {
// dòng 97: private static int getSkillIdByGender(int gender) {
// dòng 108: private static void appendRequirement(StringBuilder text, String label, long current, long required) {
```

### [HoiPhucSach.java](../../SRC/src/nro/models/combine/HoiPhucSach.java) — 84 dòng

Kiểu được nhận diện: HoiPhucSach.

```java
// dòng 17: private static int getGem(int param) {
// dòng 25: public static void showInfoCombine(Player player) {
// dòng 55: public static void hoiPhucSach(Player player) {
```

### [LamPhepNhapDa.java](../../SRC/src/nro/models/combine/LamPhepNhapDa.java) — 103 dòng

Kiểu được nhận diện: LamPhepNhapDa.

```java
// dòng 22: public static void showInfoCombine(Player player) {
// dòng 51: public static void lamphepnhapda(Player player) {
// dòng 99: private static boolean isValidCombination(Item item1, Item item2) {
```

### [NangCapBongTai.java](../../SRC/src/nro/models/combine/NangCapBongTai.java) — 148 dòng

Kiểu được nhận diện: NangCapBongTai.

```java
// dòng 31: public static void showInfoCombine(Player player) {
// dòng 97: public static void nangCapBongTai(Player player) {
```

### [NangCapBongTai3.java](../../SRC/src/nro/models/combine/NangCapBongTai3.java) — 152 dòng

Kiểu được nhận diện: NangCapBongTai3.

```java
// dòng 30: public static void showInfoCombine(Player player) {
// dòng 96: public static void nangCapBongTai(Player player) {
```

### [NangCapSachTuyetKy.java](../../SRC/src/nro/models/combine/NangCapSachTuyetKy.java) — 115 dòng

Kiểu được nhận diện: NangCapSachTuyetKy.

```java
// dòng 23: public static void showInfoCombine(Player player) {
// dòng 61: public static void nangCapSachTuyetKy(Player player) {
// dòng 97: private static boolean hasRequiredItems(Player player) {
```

### [NangCapSaoPhaLe.java](../../SRC/src/nro/models/combine/NangCapSaoPhaLe.java) — 147 dòng

Kiểu được nhận diện: NangCapSaoPhaLe.

```java
// dòng 23: public static void showInfoCombine(Player player) {
// dòng 69: public static void nangCapSaoPhaLe(Player player) {
// dòng 126: private static int getSaoPhaLeCap2Id(int saoPhaLeCap1Id) {
```

### [NangCapVatPham.java](../../SRC/src/nro/models/combine/NangCapVatPham.java) — 264 dòng

Kiểu được nhận diện: NangCapVatPham.

```java
// dòng 22: public static void showInfoCombine(Player player) {
// dòng 126: public static void nangCapVatPham(Player player) {
```

### [NangChiSoBongTai.java](../../SRC/src/nro/models/combine/NangChiSoBongTai.java) — 136 dòng

Kiểu được nhận diện: NangChiSoBongTai.

```java
// dòng 27: public static void showInfoCombine(Player player) {
// dòng 91: public static void nangChiSoBongTai(Player player) {
```

### [NangChiSoBongTai3.java](../../SRC/src/nro/models/combine/NangChiSoBongTai3.java) — 154 dòng

Kiểu được nhận diện: NangChiSoBongTai3.

```java
// dòng 27: public static void showInfoCombine(Player player) {
// dòng 100: public static void nangChiSoBongTai(Player player) {
// dòng 151: private static byte randomOpt() {
```

### [NhapNgocRong.java](../../SRC/src/nro/models/combine/NhapNgocRong.java) — 51 dòng

Kiểu được nhận diện: NhapNgocRong.

```java
// dòng 15: public static void showInfoCombine(Player player) {
// dòng 35: public static void nhapNgocRong(Player player) {
```

### [PhaLeHoaTrangBi.java](../../SRC/src/nro/models/combine/PhaLeHoaTrangBi.java) — 215 dòng

Kiểu được nhận diện: PhaLeHoaTrangBi.

```java
// dòng 16: public static void showInfoCombine(Player player) {
// dòng 42: private static void QuyTrinh(Player player, Item item, int star) {
// dòng 65: public static float getRatio(int star) {
// dòng 90: private static int getFakeRatio(int star) {
// dòng 115: public static void phaLeHoa(Player player, int... numm) {
```

### [PhanRaSach.java](../../SRC/src/nro/models/combine/PhanRaSach.java) — 69 dòng

Kiểu được nhận diện: PhanRaSach.

```java
// dòng 19: public static void showInfoCombine(Player player) {
// dòng 47: public static void phanRaSach(Player player) {
```

### [PhanRaTrangBiKichHoat.java](../../SRC/src/nro/models/combine/PhanRaTrangBiKichHoat.java) — 118 dòng

Kiểu được nhận diện: PhanRaTrangBiKichHoat, PhanRaTrangBi.

```java
// dòng 26: public static void showInfoCombine(Player player) {
// dòng 54: public static void ThucHienPhanRa(Player player) {
// dòng 101: private static boolean isValidItem(Item item) {
```

### [RandomCollection.java](../../SRC/src/nro/models/combine/RandomCollection.java) — 39 dòng

Kiểu được nhận diện: RandomCollection.

```java
// dòng 18: public RandomCollection() {
// dòng 22: public RandomCollection(Random random) {
// dòng 26: public RandomCollection<E> add(double weight, E result) {
// dòng 35: public E next() {
```

### [TaiTaoCapsuleKichHoat.java](../../SRC/src/nro/models/combine/TaiTaoCapsuleKichHoat.java) — 133 dòng

Kiểu được nhận diện: TaiTaoCapsuleKichHoat.

```java
// dòng 28: public static void showInfoCombine(Player player) {
// dòng 70: public static void thucHienTaiTao(Player player) {
// dòng 120: private static void removeItem(Player player, int itemId, int quantityToRemove) {
```

### [TaoDaHematite.java](../../SRC/src/nro/models/combine/TaoDaHematite.java) — 91 dòng

Kiểu được nhận diện: TaoDaHematite.

```java
// dòng 22: public static void showInfoCombine(Player player) {
// dòng 54: public static void taoDaHematite(Player player) {
```

### [TaoDaMai.java](../../SRC/src/nro/models/combine/TaoDaMai.java) — 83 dòng

Kiểu được nhận diện: TaoDaMai.

```java
// dòng 22: public static void showInfoCombine(Player player) {
// dòng 51: public static void CheTaoDuiDuc(Player player) {
```

### [TaySach.java](../../SRC/src/nro/models/combine/TaySach.java) — 53 dòng

Kiểu được nhận diện: TaySach.

```java
// dòng 17: public static void showInfoCombine(Player player) {
// dòng 29: public static void taySach(Player player) {
```

## nro.models.consts (21 tệp)

### [AppearType.java](../../SRC/src/nro/models/consts/AppearType.java) — 9 dòng

Kiểu được nhận diện: AppearType.

### [BossStatus.java](../../SRC/src/nro/models/consts/BossStatus.java) — 12 dòng

Kiểu được nhận diện: BossStatus.

### [BossType.java](../../SRC/src/nro/models/consts/BossType.java) — 22 dòng

Kiểu được nhận diện: BossType.

### [Cmd_message.java](../../SRC/src/nro/models/consts/Cmd_message.java) — 582 dòng

Kiểu được nhận diện: Cmd_message.

### [ConstAccount.java](../../SRC/src/nro/models/consts/ConstAccount.java) — 10 dòng

Kiểu được nhận diện: ConstAccount.

### [ConstAchievement.java](../../SRC/src/nro/models/consts/ConstAchievement.java) — 24 dòng

Kiểu được nhận diện: ConstAchievement.

### [ConstDailyGift.java](../../SRC/src/nro/models/consts/ConstDailyGift.java) — 8 dòng

Kiểu được nhận diện: ConstDailyGift.

### [ConstEvent.java](../../SRC/src/nro/models/consts/ConstEvent.java) — 12 dòng

Kiểu được nhận diện: ConstEvent.

### [ConstFont.java](../../SRC/src/nro/models/consts/ConstFont.java) — 18 dòng

Kiểu được nhận diện: ConstFont.

### [ConstIgnoreName.java](../../SRC/src/nro/models/consts/ConstIgnoreName.java) — 6 dòng

Kiểu được nhận diện: ConstIgnoreName.

### [ConstItem.java](../../SRC/src/nro/models/consts/ConstItem.java) — 1,264 dòng

Kiểu được nhận diện: ConstItem.

### [ConstMap.java](../../SRC/src/nro/models/consts/ConstMap.java) — 167 dòng

Kiểu được nhận diện: ConstMap.

### [ConstMob.java](../../SRC/src/nro/models/consts/ConstMob.java) — 114 dòng

Kiểu được nhận diện: ConstMob.

### [ConstNpc.java](../../SRC/src/nro/models/consts/ConstNpc.java) — 373 dòng

Kiểu được nhận diện: ConstNpc.

### [ConstPlayer.java](../../SRC/src/nro/models/consts/ConstPlayer.java) — 24 dòng

Kiểu được nhận diện: ConstPlayer.

### [ConstRatio.java](../../SRC/src/nro/models/consts/ConstRatio.java) — 31 dòng

Kiểu được nhận diện: ConstRatio.

### [ConstSuperRank.java](../../SRC/src/nro/models/consts/ConstSuperRank.java) — 23 dòng

Kiểu được nhận diện: ConstSuperRank.

### [ConstTask.java](../../SRC/src/nro/models/consts/ConstTask.java) — 6,356 dòng

Kiểu được nhận diện: ConstTask.

### [ConstTaskBadges.java](../../SRC/src/nro/models/consts/ConstTaskBadges.java) — 66 dòng

Kiểu được nhận diện: ConstTaskBadges.

```java
// dòng 24: public static String getNameById(int id) {
```

### [ConstTournament.java](../../SRC/src/nro/models/consts/ConstTournament.java) — 43 dòng

Kiểu được nhận diện: ConstTournament.

### [SocketType.java](../../SRC/src/nro/models/consts/SocketType.java) — 6 dòng

Kiểu được nhận diện: SocketType.

## nro.models.daily_Giftcode (2 tệp)

### [DailyGiftData.java](../../SRC/src/nro/models/daily_Giftcode/DailyGiftData.java) — 18 dòng

Kiểu được nhận diện: DailyGiftData.

```java
// dòng 8: public DailyGiftData() {
// dòng 14: public String toString() {
```

### [DailyGiftService.java](../../SRC/src/nro/models/daily_Giftcode/DailyGiftService.java) — 37 dòng

Kiểu được nhận diện: DailyGiftService.

```java
// dòng 7: public static boolean checkDailyGift(Player player, byte id) {
// dòng 16: public static void updateDailyGift(Player player, byte id) {
// dòng 25: public static void addAndReset(Player player) {
```

## nro.models.data (5 tệp)

### [DataGame.java](../../SRC/src/nro/models/data/DataGame.java) — 507 dòng

Kiểu được nhận diện: DataGame.

```java
// dòng 44: public static void sendVersionGame(MySession session) {
// dòng 68: public static void updateData(MySession session) {
// dòng 101: public static void updateMap(MySession session) {
// dòng 135: public static void updateSkill(MySession session) {
// dòng 212: public static void sendDataImageVersion(MySession session) {
// dòng 220: public static void sendEffectTemplate(MySession session, int id, int... idtemp) {
// dòng 248: public static void sendBgItemVersion(MySession session) {
// dòng 262: public static void sendItemBGTemplate(MySession session, int id) {
// dòng 280: public static void sendDataItemBG(MySession session) {
// dòng 299: public static void sendIcon(MySession session, int id) {
// dòng 319: public static void sendSmallVersion(MySession session) {
// dòng 334: public static void requestMobTemplate(MySession session, int id) {
// dòng 356: public static void sendTileSetInfo(MySession session) {
// dòng 370: public static void sendMapTemp(MySession session, int id) {
// dòng 388: public static void sendHeadAvatar(Message msg) {
// dòng 400: public static void sendImageByName(MySession session, String imgName) {
// dòng 423: public static void sendVersionRes(ISession session) {
// dòng 436: public static void sendSizeRes(MySession session) {
// dòng 454: public static void sendRes(MySession session) {
// dòng 494: public static void sendLinkIP(MySession session) {
```

### [ItemData.java](../../SRC/src/nro/models/data/ItemData.java) — 115 dòng

Kiểu được nhận diện: ItemData.

```java
// dòng 12: public static void updateItem(MySession session) {
// dòng 19: private static void updateItemOptionItemplate(MySession session) {
// dòng 38: private static void updateItemTemplate(MySession session, int count) {
// dòng 66: private static void updateItemTemplate(MySession session, int start, int end) {
// dòng 95: private static void updateItemArrHead2FTemplate(MySession session) {
```

### [LocalManager.java](../../SRC/src/nro/models/data/LocalManager.java) — 196 dòng

Kiểu được nhận diện: LocalManager.

```java
// dòng 39: public static LocalManager gI() {
// dòng 46: public static Connection getConnection() throws SQLException {
// dòng 50: public void release(Connection con) {
// dòng 54: public static void close() {
// dòng 58: private static void loadProperties() {
// dòng 101: public static LocalResultSet executeQuery(final String query) throws Exception {
// dòng 122: public static LocalResultSet executeQuery(final String query, final Object... objs) throws Exception {
// dòng 137: public static int executeUpdate(final String query) throws Exception {
// dòng 151: public static int executeUpdate(String query, final Object... objs) throws Exception {
// dòng 179: private static HikariConfig createConfig(String poolName, String databaseName) {
```

### [LocalResultSet.java](../../SRC/src/nro/models/data/LocalResultSet.java) — 64 dòng

Kiểu được nhận diện: LocalResultSet.

```java
// dòng 63: public void close();
```

### [ResultSetImpl.java](../../SRC/src/nro/models/data/ResultSetImpl.java) — 365 dòng

Kiểu được nhận diện: ResultSetImpl.

```java
// dòng 16: public ResultSetImpl(final ResultSet rs) throws Exception {
// dòng 55: public void dispose() {
// dòng 72: public boolean next() throws Exception {
// dòng 81: public boolean first() throws Exception {
// dòng 90: public boolean gotoResult(final int index) throws Exception {
// dòng 102: public boolean gotoFirst() throws Exception {
// dòng 111: public void gotoBeforeFirst() {
// dòng 116: public boolean gotoLast() throws Exception {
// dòng 125: public int getRows() throws Exception {
// dòng 133: public byte getByte(final int column) throws Exception {
// dòng 144: public byte getByte(final String column) throws Exception {
// dòng 155: public int getInt(final int column) throws Exception {
// dòng 166: public int getInt(final String column) throws Exception {
// dòng 177: public float getFloat(final int column) throws Exception {
// dòng 188: public float getFloat(final String column) throws Exception {
// dòng 199: public double getDouble(final int column) throws Exception {
// dòng 210: public double getDouble(final String column) throws Exception {
// dòng 221: public long getLong(final int column) throws Exception {
// dòng 232: public long getLong(final String column) throws Exception {
// dòng 243: public String getString(final int column) throws Exception {
// dòng 254: public String getString(final String column) throws Exception {
// dòng 265: public Object getObject(final int column) throws Exception {
// dòng 276: public Object getObject(final String column) throws Exception {
// dòng 287: public boolean getBoolean(final int column) throws Exception {
// dòng 302: public boolean getBoolean(final String column) throws Exception {
// dòng 317: public Timestamp getTimestamp(final int column) throws Exception {
// dòng 328: public Timestamp getTimestamp(final String column) throws Exception {
// dòng 339: public short getShort(final int column) throws Exception {
// dòng 350: public short getShort(final String column) throws Exception {
// dòng 361: public void close() {
```

## nro.models.database (7 tệp)

### [EventDAO.java](../../SRC/src/nro/models/database/EventDAO.java) — 62 dòng

Kiểu được nhận diện: EventDAO.

```java
// dòng 29: public static void loadInternationalWomensDayEvent() {
// dòng 45: public static void save() {
```

### [HistoryTransactionDAO.java](../../SRC/src/nro/models/database/HistoryTransactionDAO.java) — 101 dòng

Kiểu được nhận diện: HistoryTransactionDAO.

```java
// dòng 15: public static void insert(Player pl1, Player pl2,
// dòng 85: public static void deleteHistory() {
```

### [MrBlue.java](../../SRC/src/nro/models/database/MrBlue.java) — 1,350 dòng

Kiểu được nhận diện: MrBlue.

```java
// dòng 60: public static Player login(MySession session, AntiLogin al) {
// dòng 152: public static Player loadById(long id) {
// dòng 175: private static Player loadPlayer(LocalResultSet rs, boolean isOffline) throws Exception {
// dòng 1337: public static List<OptionCard> loadOptionCard(JSONArray json) {
```

### [PlayerDAO.java](../../SRC/src/nro/models/database/PlayerDAO.java) — 1,134 dòng

Kiểu được nhận diện: PlayerDAO.

```java
// dòng 37: public static boolean createNewPlayer(int userId, String name, byte gender, int hair) {
// dòng 352: public static void updatePlayer(Player player) {
// dòng 1065: public static boolean checkLogout(Connection con, Player player) {
// dòng 1091: public static boolean subvnd(Player player, int num) {
// dòng 1111: public static boolean MuaThanhVien(Player player, int num) {
// dòng 1131: public static void LogAddPoint(String name, int id, int point, String type) {
```

### [ShopDAO.java](../../SRC/src/nro/models/database/ShopDAO.java) — 133 dòng

Kiểu được nhận diện: ShopDAO.

```java
// dòng 20: public static List<Shop> getShops(Connection con) {
// dòng 49: private static void loadShopTab(Connection con, Shop shop) {
// dòng 76: private static void loadItemShop(Connection con, TabShop tabShop) {
// dòng 112: private static void loadItemShopOption(Connection con, ItemShop itemShop) {
```

### [SuperRankDAO.java](../../SRC/src/nro/models/database/SuperRankDAO.java) — 278 dòng

Kiểu được nhận diện: SuperRankDAO.

```java
// dòng 21: public static List<SuperRankBuilder> getPlayerListInRankRange(int rank, int limit) {
// dòng 45: public static List<SuperRankBuilder> getPlayerListInRank(int rank, int limit) {
// dòng 66: public static int random(int rank) {
// dòng 83: public static SuperRankBuilder readData(LocalResultSet rs) throws Exception {
// dòng 135: public static void updatePlayer(Player player) {
// dòng 156: public static void loadData(Player player) {
// dòng 188: public static int getRank(int playerId) {
// dòng 199: public static int getCurrentHighestRank() {
// dòng 211: public static void insertData(Player player) {
// dòng 245: public static void updateData(Player player) {
```

### [TraningDAO.java](../../SRC/src/nro/models/database/TraningDAO.java) — 38 dòng

Kiểu được nhận diện: TraningDAO.

```java
// dòng 8: public static void updatePlayer(Player player) {
```

## nro.models.event (5 tệp)

### [Event.java](../../SRC/src/nro/models/event/Event.java) — 58 dòng

Kiểu được nhận diện: Event.

```java
// dòng 12: public void init() {
// dòng 20: public void npc() {
// dòng 25: public void createNpc(int mapId, int npcId, int x, int y) {
// dòng 30: public void boss() {
// dòng 35: public void createBoss(int bossId, int... total) {
// dòng 50: public void itemMap() {
// dòng 55: public void itemBoss() {
```

### [EventManager.java](../../SRC/src/nro/models/event/EventManager.java) — 61 dòng

Kiểu được nhận diện: EventManager.

```java
// dòng 30: public static EventManager gI() {
// dòng 37: public void init() {
```

### [NoiBanh.java](../../SRC/src/nro/models/event/NoiBanh.java) — 84 dòng

Kiểu được nhận diện: NoiBanh.

```java
// dòng 15: public NoiBanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 20: public void openBaseMenu(Player player) {
// dòng 27: public void confirmMenu(Player pl, int select) {
```

### [VuaHung.java](../../SRC/src/nro/models/event/VuaHung.java) — 348 dòng

Kiểu được nhận diện: VuaHung.

```java
// dòng 39: public VuaHung(int mapId, int status, int cx, int cy, int tempId, int avatar) {
// dòng 44: public void openBaseMenu(Player player) {
// dòng 96: public void confirmMenu(Player pl, int select) {
// dòng 216: private void NhanQuaThuong(Player player) {
// dòng 242: private void NhanQuaVip(Player player) {
// dòng 268: private void BanhDay(Player player) {
// dòng 286: private void BanhChung(Player player) {
// dòng 304: private int randomBanhDay() {
// dòng 309: private int randomBanhChung() {
// dòng 314: private void DoiDuaHau(Player player, int select) {
// dòng 339: private static int getItemQuantity(Player player, int itemId) {
// dòng 344: private static String formatRequirement(String itemName, int currentQuantity, int requiredQuantity) {
```

### [XeNuocMia.java](../../SRC/src/nro/models/event/XeNuocMia.java) — 175 dòng

Kiểu được nhận diện: XeNuocMia.

```java
// dòng 21: public XeNuocMia(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 26: public void openBaseMenu(Player player) {
// dòng 33: public void confirmMenu(Player pl, int select) {
// dòng 90: private void MuaNuocMia(Player player) {
// dòng 122: private void MuaNuocMiaX10(Player player) {
// dòng 166: private static int getItemQuantity(Player player, int itemId) {
// dòng 171: private static String formatRequirement(String itemName, int currentQuantity, int requiredQuantity) {
```

## nro.models.event_list (8 tệp)

### [Christmas.java](../../SRC/src/nro/models/event_list/Christmas.java) — 12 dòng

Kiểu được nhận diện: Christmas.

```java
// dòng 9: public void boss() {
```

### [Default.java](../../SRC/src/nro/models/event_list/Default.java) — 14 dòng

Kiểu được nhận diện: Default.

```java
// dòng 10: public void boss() {
```

### [Halloween.java](../../SRC/src/nro/models/event_list/Halloween.java) — 18 dòng

Kiểu được nhận diện: Halloween.

```java
// dòng 9: public void npc() {
// dòng 13: public void boss() {
```

### [HungVuong.java](../../SRC/src/nro/models/event_list/HungVuong.java) — 13 dòng

Kiểu được nhận diện: HungVuong.

```java
// dòng 10: public void boss() {
```

### [InternationalWomensDay.java](../../SRC/src/nro/models/event_list/InternationalWomensDay.java) — 17 dòng

Kiểu được nhận diện: InternationalWomensDay.

```java
// dòng 9: public void init() {
// dòng 15: public void npc() {
```

### [LunarNewYear.java](../../SRC/src/nro/models/event_list/LunarNewYear.java) — 17 dòng

Kiểu được nhận diện: LunarNewYear.

```java
// dòng 9: public void npc() {
// dòng 14: public void boss() {
```

### [TopUp.java](../../SRC/src/nro/models/event_list/TopUp.java) — 11 dòng

Kiểu được nhận diện: TopUp.

```java
// dòng 8: public void npc() {
```

### [TrungThu.java](../../SRC/src/nro/models/event_list/TrungThu.java) — 13 dòng

Kiểu được nhận diện: TrungThu.

```java
// dòng 9: public void boss() {
```

## nro.models.ievent (1 tệp)

### [IEvent.java](../../SRC/src/nro/models/ievent/IEvent.java) — 19 dòng

Kiểu được nhận diện: IEvent.

## nro.models.interfaces (11 tệp)

### [IBoss.java](../../SRC/src/nro/models/interfaces/IBoss.java) — 73 dòng

Kiểu được nhận diện: IBoss.

### [IEventBoss.java](../../SRC/src/nro/models/interfaces/IEventBoss.java) — 17 dòng

Kiểu được nhận diện: IEventBoss.

### [IKeySessionHandler.java](../../SRC/src/nro/models/interfaces/IKeySessionHandler.java) — 7 dòng

Kiểu được nhận diện: IKeySessionHandler.

```java
// dòng 5: public void sendKey(ISession var1);
```

### [IMessage.java](../../SRC/src/nro/models/interfaces/IMessage.java) — 83 dòng

Kiểu được nhận diện: IMessage.

```java
// dòng 9: public int read() throws IOException;
// dòng 11: public int read(byte[] var1) throws IOException;
// dòng 13: public int read(byte[] var1, int var2, int var3) throws IOException;
// dòng 15: public boolean readBoolean() throws IOException;
// dòng 17: public byte readByte() throws IOException;
// dòng 19: public short readShort() throws IOException;
// dòng 21: public int readInt() throws IOException;
// dòng 23: public long readLong() throws IOException;
// dòng 25: public float readFloat() throws IOException;
// dòng 27: public double readDouble() throws IOException;
// dòng 29: public char readChar() throws IOException;
// dòng 31: public String readUTF() throws IOException;
// dòng 33: public void readFully(byte[] var1) throws IOException;
// dòng 35: public void readFully(byte[] var1, int var2, int var3) throws IOException;
// dòng 37: public int readUnsignedByte() throws IOException;
// dòng 39: public int readUnsignedShort() throws IOException;
// dòng 41: public void write(byte[] var1) throws IOException;
// dòng 43: public void write(int var1) throws IOException;
// dòng 45: public void write(byte[] var1, int var2, int var3) throws IOException;
// dòng 47: public void writeBoolean(boolean var1) throws IOException;
// dòng 49: public void writeByte(int var1) throws IOException;
// dòng 51: public void writeBytes(String var1) throws IOException;
// dòng 53: public void writeChar(int var1) throws IOException;
// dòng 55: public void writeChars(String var1) throws IOException;
// dòng 57: public void writeDouble(double var1) throws IOException;
// dòng 59: public void writeFloat(float var1) throws IOException;
// dòng 61: public void writeInt(int var1) throws IOException;
// dòng 63: public void writeLong(long var1) throws IOException;
// dòng 65: public void writeShort(int var1) throws IOException;
// dòng 67: public void writeUTF(String var1) throws IOException;
// dòng 69: public BufferedImage readImage() throws IOException;
// dòng 71: public void writeImage(BufferedImage var1, String var2) throws IOException;
// dòng 73: public DataOutputStream writer();
// dòng 75: public DataInputStream reader();
// dòng 77: public byte[] getData();
// dòng 79: public void cleanup();
// dòng 81: public void dispose();
```

### [IMessageHandler.java](../../SRC/src/nro/models/interfaces/IMessageHandler.java) — 9 dòng

Kiểu được nhận diện: IMessageHandler.

```java
// dòng 7: public void onMessage(ISession var1, Message var2) throws Exception;
```

### [IMessageSendCollect.java](../../SRC/src/nro/models/interfaces/IMessageSendCollect.java) — 17 dòng

Kiểu được nhận diện: IMessageSendCollect.

```java
// dòng 9: public Message readMessage(ISession var1, DataInputStream var2) throws Exception;
// dòng 11: public byte readKey(ISession var1, byte var2);
// dòng 13: public void doSendMessage(ISession var1, DataOutputStream var2, Message var3) throws Exception;
// dòng 15: public byte writeKey(ISession var1, byte var2);
```

### [INetwork.java](../../SRC/src/nro/models/interfaces/INetwork.java) — 26 dòng

Kiểu được nhận diện: INetwork.

```java
// dòng 5: public INetwork init();
// dòng 7: public INetwork start(int var1) throws Exception;
// dòng 9: public INetwork setAcceptHandler(ISessionAcceptHandler var1);
// dòng 11: public INetwork close();
// dòng 13: public INetwork dispose();
// dòng 15: public INetwork setDoSomeThingWhenClose(IServerClose var1);
// dòng 17: public INetwork randomKey(boolean var1);
// dòng 19: public INetwork setTypeSessioClone(Class var1) throws Exception;
// dòng 21: public ISessionAcceptHandler getAcceptHandler() throws Exception;
// dòng 23: public boolean isRandomKey();
// dòng 25: public void stopConnect();
```

### [IPVP.java](../../SRC/src/nro/models/interfaces/IPVP.java) — 24 dòng

Kiểu được nhận diện: IPVP.

### [IServerClose.java](../../SRC/src/nro/models/interfaces/IServerClose.java) — 6 dòng

Kiểu được nhận diện: IServerClose.

```java
// dòng 4: public void serverClose();
```

### [ISession.java](../../SRC/src/nro/models/interfaces/ISession.java) — 45 dòng

Kiểu được nhận diện: ISession.

```java
// dòng 9: public ISession setSendCollect(IMessageSendCollect var1);
// dòng 11: public ISession setMessageHandler(IMessageHandler var1);
// dòng 13: public ISession setKeyHandler(IKeySessionHandler var1);
// dòng 15: public ISession startSend();
// dòng 17: public ISession startCollect();
// dòng 19: public ISession start();
// dòng 21: public String getIP();
// dòng 23: public boolean isConnected();
// dòng 25: public long getID();
// dòng 27: public void sendMessage(Message var1);
// dòng 29: public void doSendMessage(Message var1) throws Exception;
// dòng 31: public void disconnect();
// dòng 33: public void dispose();
// dòng 35: public int getNumMessages();
// dòng 37: public void sendKey() throws Exception;
// dòng 39: public byte[] getKey();
// dòng 41: public boolean sentKey();
// dòng 43: public void setSentKey(boolean var1);
```

### [ISessionAcceptHandler.java](../../SRC/src/nro/models/interfaces/ISessionAcceptHandler.java) — 9 dòng

Kiểu được nhận diện: ISessionAcceptHandler.

```java
// dòng 5: public void sessionInit(ISession var1);
// dòng 7: public void sessionDisconnect(ISession var1);
```

## nro.models.intrinsic (2 tệp)

### [Intrinsic.java](../../SRC/src/nro/models/intrinsic/Intrinsic.java) — 44 dòng

Kiểu được nhận diện: Intrinsic.

```java
// dòng 16: public Intrinsic() {
// dòng 20: public Intrinsic(Intrinsic intrinsic) {
// dòng 31: public String getDescription() {
// dòng 38: public String getName() {
```

### [IntrinsicPlayer.java](../../SRC/src/nro/models/intrinsic/IntrinsicPlayer.java) — 17 dòng

Kiểu được nhận diện: IntrinsicPlayer.

```java
// dòng 10: public IntrinsicPlayer() {
// dòng 14: public void dispose() {
```

## nro.models.item (2 tệp)

### [Item.java](../../SRC/src/nro/models/item/Item.java) — 552 dòng

Kiểu được nhận diện: Item, ItemOption.

```java
// dòng 34: public boolean isNotNullItem() {
// dòng 38: public String getName() {
// dòng 42: public Item() {
// dòng 47: public Item(short itemId) {
// dòng 53: public String getInfo() {
// dòng 61: public String getContent() {
// dòng 65: public void dispose() {
// dòng 84: public ItemOption() {
// dòng 87: public ItemOption(ItemOption io) {
// dòng 92: public ItemOption(int tempId, int param) {
// dòng 97: public ItemOption(Template.ItemOptionTemplate temp, int param) {
// dòng 102: public String getOptionString() {
// dòng 106: public void dispose() {
// dòng 111: public String toString() {
// dòng 119: public String getOptionString(int param) {
// dòng 130: public short getId() {
// dòng 134: public byte getType() {
// dòng 138: public boolean isSKH() {
// dòng 147: public boolean isVaiTho() {
// dòng 154: public boolean isDTS() {
// dòng 161: public boolean isDTL() {
// dòng 168: public boolean isDHD() {
// dòng 175: public boolean isManhTS() {
// dòng 182: public boolean isGiayMau() {
// dòng 189: public boolean isDaNangCap() {
// dòng 197: public int getOptionParam(int id) {
// dòng 207: public void addOptionParam(int id, int param) {
// dòng 218: public ItemOption getOptionDaPhaLe() {
// dòng 288: public boolean canPhaLeHoa() {
// dòng 292: public Item cloneItem() {
// dòng 306: public String getOptionInfo() {
// dòng 316: public boolean isThanLinh() {
// dòng 323: public boolean isThucAn() {
// dòng 330: public String getOptionInfo(Item item) {
// dòng 351: public String getOptionInfoCuongHoa(Item item) {
// dòng 365: public void subOptionParam(int id, int param) {
// dòng 375: public boolean isDaNangCap1() {
// dòng 383: public String typeName() {
// dòng 400: public String typeHanhTinh() {
// dòng 419: public byte typeIdManh() {
// dòng 439: public String typeNameManh() {
// dòng 456: public String typeDanangcap() {
// dòng 473: public String typeDaMayman() {
// dòng 491: public boolean isDaMayMan() {
// dòng 495: public boolean isCongThucVip() {
// dòng 499: public boolean isDoKyGui() {
// dòng 503: public String getInfoItem() {
// dòng 512: public boolean isHaveOption(int id) {
// dòng 522: public boolean isSachTuyetKy() {
// dòng 526: public boolean isSachTuyetKy2() {
// dòng 530: public ItemOption getOptionById(int id) {
// dòng 542: public boolean hasOption(int optionId, int minParam) {
```

### [ItemTime.java](../../SRC/src/nro/models/item/ItemTime.java) — 279 dòng

Kiểu được nhận diện: ItemTime.

```java
// dòng 115: public ItemTime(Player player) {
// dòng 119: public void update() {
// dòng 276: public void dispose() {
```

## nro.models.managers (8 tệp)

### [GiftCodeManager.java](../../SRC/src/nro/models/managers/GiftCodeManager.java) — 67 dòng

Kiểu được nhận diện: GiftCodeManager.

```java
// dòng 17: public static GiftCodeManager gI() {
// dòng 24: public GiftCode checkUseGiftCode(Player player, String code) {
// dòng 47: public void updateGiftCode(GiftCode giftcode) {
// dòng 55: public void checkInfomationGiftCode(Player p) {
```

### [MyClanTopBanDoKhoBau.java](../../SRC/src/nro/models/managers/MyClanTopBanDoKhoBau.java) — 167 dòng

Kiểu được nhận diện: MyClanTopBanDoKhoBau.

```java
// dòng 35: public static MyClanTopBanDoKhoBau getInstance() {
// dòng 39: public void load2(int idLeader) {
// dòng 71: private Player extractPlayerFromResultSet(ResultSet rs) throws SQLException {
// dòng 91: private void extractDataPoint(String dataPoint, Player player) {
// dòng 98: private void extractItemsBody(String itemsBody, Player player) {
// dòng 118: private Item createItemFromDataObject(Object itemData) {
```

### [PVPManager.java](../../SRC/src/nro/models/managers/PVPManager.java) — 71 dòng

Kiểu được nhận diện: PVPManager.

```java
// dòng 17: public static PVPManager gI() {
// dòng 24: public PVPManager() {
// dòng 30: public void removePVP(PVP pvp) {
// dòng 34: public void addPVP(PVP pvp) {
// dòng 38: public PVP getPVP(Player player) {
// dòng 48: public void run() {
// dòng 52: private void update() {
// dòng 66: public void stop() {
```

### [ShenronEventManager.java](../../SRC/src/nro/models/managers/ShenronEventManager.java) — 73 dòng

Kiểu được nhận diện: ShenronEventManager.

```java
// dòng 20: public static ShenronEventManager gI() {
// dòng 27: private ShenronEventManager() {
// dòng 33: public void run() {
// dòng 46: public void update() {
// dòng 61: public void add(ShenronEvent se) {
// dòng 65: public void remove(ShenronEvent se) {
// dòng 69: public void stop() {
```

### [SuperRankManager.java](../../SRC/src/nro/models/managers/SuperRankManager.java) — 141 dòng

Kiểu được nhận diện: SuperRankManager.

```java
// dòng 23: public static SuperRankManager gI() {
// dòng 30: public SuperRankManager() {
// dòng 36: public void run() {
// dòng 64: public boolean canCompete(Player player) {
// dòng 68: public boolean currentlyCompeting(long playerId) {
// dòng 78: public boolean awaitingCompetition(long playerId) {
// dòng 88: public boolean awaiting(Player player) {
// dòng 98: public boolean SPRCheck(@NonNull Zone zone) {
// dòng 108: public int ordinal(long id) {
// dòng 117: public String getCompeting(long plId) {
// dòng 129: public void addSPR(SuperRank spr) {
// dòng 133: public void removeSPR(SuperRank spr) {
// dòng 137: public void addWSPR(long playerId, long rivalId) {
```

### [TopBanDoKhoBau.java](../../SRC/src/nro/models/managers/TopBanDoKhoBau.java) — 157 dòng

Kiểu được nhận diện: TopBanDoKhoBau.

```java
// dòng 32: public static TopBanDoKhoBau getInstance() {
// dòng 37: public void load() {
// dòng 61: private Player extractPlayerFromResultSet(ResultSet rs) throws SQLException {
// dòng 81: private void extractDataPoint(String dataPoint, Player player) {
// dòng 88: private void extractItemsBody(String itemsBody, Player player) {
// dòng 108: private Item createItemFromDataObject(Object itemData) {
```

### [TopConDuongRanDoc.java](../../SRC/src/nro/models/managers/TopConDuongRanDoc.java) — 152 dòng

Kiểu được nhận diện: TopConDuongRanDoc.

```java
// dòng 28: public static TopConDuongRanDoc getInstance() {
// dòng 32: public void load() {
// dòng 56: private Player extractPlayerFromResultSet(ResultSet rs) throws SQLException {
// dòng 76: private void extractDataPoint(String dataPoint, Player player) {
// dòng 83: private void extractItemsBody(String itemsBody, Player player) {
// dòng 103: private Item createItemFromDataObject(Object itemData) {
```

### [TopKhiGasHuyDiet.java](../../SRC/src/nro/models/managers/TopKhiGasHuyDiet.java) — 153 dòng

Kiểu được nhận diện: TopKhiGasHuyDiet.

```java
// dòng 28: public static TopKhiGasHuyDiet getInstance() {
// dòng 33: public void load() {
// dòng 57: private Player extractPlayerFromResultSet(ResultSet rs) throws SQLException {
// dòng 77: private void extractDataPoint(String dataPoint, Player player) {
// dòng 84: private void extractItemsBody(String itemsBody, Player player) {
// dòng 104: private Item createItemFromDataObject(Object itemData) {
```

## nro.models.map (6 tệp)

### [ItemMap.java](../../SRC/src/nro/models/map/ItemMap.java) — 203 dòng

Kiểu được nhận diện: ItemMap.

```java
// dòng 36: public ItemMap(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
// dòng 55: public ItemMap(Zone zone, ItemTemplate temp, int quantity, int x, int y, long playerId) {
// dòng 74: public ItemMap(ItemMap itemMap) {
// dòng 90: public void update() {
// dòng 135: private boolean findPlayerByID(long id) {
// dòng 144: private void satelliteUpdate() {
// dòng 189: private void reAppearItem() {
// dòng 194: public boolean isNotNullItem() {
// dòng 198: public void dispose() {
```

### [MaBuHold.java](../../SRC/src/nro/models/map/MaBuHold.java) — 18 dòng

Kiểu được nhận diện: MaBuHold.

```java
// dòng 11: public MaBuHold(int slot, Player player) {
```

### [Map.java](../../SRC/src/nro/models/map/Map.java) — 491 dòng

Kiểu được nhận diện: Map.

```java
// dòng 92: public Map(int mapId, String mapName, byte planetId,
// dòng 120: public List<Player> getPlayersInMap() {
// dòng 128: private void initZone(int nZone, int maxPlayer) {
// dòng 170: public void initNpc(byte[] npcId, short[] npcX, short[] npcY) {
// dòng 176: public void addNpc(Npc npc) {
// dòng 180: public void run() {
// dòng 192: public void initMob(byte[] mobTemp, byte[] mobLevel, int[] mobHp, short[] mobX, short[] mobY) {
// dòng 241: public void initMob(List<Mob> mobs) {
// dòng 251: private void initTrapMap() {
// dòng 269: private void initItem() {
// dòng 304: public void initBoss() {
// dòng 341: public short mapIdNextMabu(short mapId) {
// dòng 358: public Npc getNpc(Player player, int tempId) {
// dòng 368: public int yPhysicInTop(int x, int y) {
// dòng 400: public int LastY(int cx, int cy) {
// dòng 420: public boolean tileTypeAt(int x, int y, int type) {
// dòng 429: public int[] moveXY(Player player) {
// dòng 464: private boolean isTileTop(int tileMap) {
// dòng 473: public final void readTileMap(int mapId) {
```

### [TrapMap.java](../../SRC/src/nro/models/map/TrapMap.java) — 27 dòng

Kiểu được nhận diện: TrapMap.

```java
// dòng 16: public void doPlayer(Player player) {
```

### [WayPoint.java](../../SRC/src/nro/models/map/WayPoint.java) — 23 dòng

Kiểu được nhận diện: WayPoint.

### [Zone.java](../../SRC/src/nro/models/map/Zone.java) — 837 dòng

Kiểu được nhận diện: Zone.

```java
// dòng 84: public boolean isFullPlayer() {
// dòng 88: public void addBot(BotAttackplayer bot) {
// dòng 107: public List<BotAttackplayer> getBots() {
// dòng 113: public void removeBot(BotAttackplayer bot) {
// dòng 129: private void udMob() {
// dòng 139: private void udNonInteractiveNPC() {
// dòng 156: private void udItem() {
// dòng 182: public void update() {
// dòng 189: public Zone(Map map, int zoneId, int maxPlayer) {
// dòng 205: public int getNumOfPlayers() {
// dòng 209: public int getNumOfBosses() {
// dòng 213: public boolean isBossCanJoin(Boss boss) {
// dòng 222: public void addPlayer(Player player) {
// dòng 250: private void udPlayer() {
// dòng 259: public void removePlayer(Player player) {
// dòng 268: public ItemMap getItemMapByItemMapId(int itemId) {
// dòng 277: public ItemMap getItemMapByTempId(int tempId) {
// dòng 286: public List<ItemMap> getItemMapsForPlayer(Player player) {
// dòng 307: public Player getPlayerInMap(long idPlayer) {
// dòng 316: public Player getPlayerInMapOffline(Player player, long idPlayer) {
// dòng 325: public void pickItem(Player player, int itemMapId) {
// dòng 422: public synchronized void addItem(ItemMap itemMap) {
// dòng 428: public synchronized void removeItemMap(ItemMap itemMap) {
// dòng 432: public Player getRandomPlayerInMap() {
// dòng 449: public void load_Me_To_Another(Player player) {
// dòng 480: public void load_Another_To_Me(Player player) {
// dòng 508: public void loadBoss(Boss boss) {
// dòng 531: private void infoPlayer(Player plReceive, Player plInfo) {
// dòng 615: public void mapInfo(Player pl) {
// dòng 738: public TrapMap isInTrap(Player player) {
// dòng 748: public void sendBigBoss(Player player) {
// dòng 762: public MaBuHold getMaBuHold() {
// dòng 771: public void setMaBuHold(int slot, int zoneId, Player player) {
// dòng 775: public boolean isKhongCoTrongTaiTrongKhu() {
// dòng 803: public Player findNearestPlayer(Pet pet) {
// dòng 820: public Mob findNearestMob(Pet pet) {
```

## nro.models.map.phoban (6 tệp)

### [BanDoKhoBau.java](../../SRC/src/nro/models/map/phoban/BanDoKhoBau.java) — 271 dòng

Kiểu được nhận diện: BanDoKhoBau.

```java
// dòng 42: public void addZone(Zone zone) {
// dòng 46: public BanDoKhoBau(int id) {
// dòng 52: public void run() {
// dòng 64: public void update() {
// dòng 120: public void openBanDoKhoBau(Player plOpen, Clan clan, byte level) {
// dòng 141: public void sendThanhTichBanDoKhoBau(Player pl) {
// dòng 160: private void init() {
// dòng 213: public void finish() {
// dòng 225: private void kickOutOfBDKB(Player player) {
// dòng 231: public Zone getMapById(int mapId) {
// dòng 240: private void sendTextBanDoKhoBau() {
// dòng 246: private void removeTextBanDoKhoBau() {
// dòng 252: public void dispose() {
```

### [BlackBallWar.java](../../SRC/src/nro/models/map/phoban/BlackBallWar.java) — 148 dòng

Kiểu được nhận diện: BlackBallWar.

```java
// dòng 46: public BlackBallWar(Zone zone) {
// dòng 51: private void start() {
// dòng 56: public void run() {
// dòng 68: public synchronized void update() {
// dòng 84: public void updatePlayer(Player player) {
// dòng 106: private synchronized void win(Player player) {
// dòng 131: private void kickOutOfMap(Player player) {
// dòng 141: private void kickAllPlayersOutOfMap(Zone zone) {
```

### [DestronGas.java](../../SRC/src/nro/models/map/phoban/DestronGas.java) — 259 dòng

Kiểu được nhận diện: DestronGas.

```java
// dòng 46: public DestronGas(int id) {
// dòng 52: public void run() {
// dòng 64: public void update() {
// dòng 123: public void openKhiGasHuyDiet(Player plOpen, Clan clan, byte level) {
// dòng 141: public void sendThanhTichKhiGas(Player pl) {
// dòng 164: private void init() {
// dòng 198: public void finish() {
// dòng 211: private void kickOutOfKGHD(Player player) {
// dòng 217: public Zone getMapById(int mapId) {
// dòng 226: private void sendTextKhiGasHuyDiet() {
// dòng 232: private void removeTextKhiGasHuyDiet() {
// dòng 238: public void dispose() {
```

### [MajinBuu14H.java](../../SRC/src/nro/models/map/phoban/MajinBuu14H.java) — 100 dòng

Kiểu được nhận diện: MajinBuu14H.

```java
// dòng 22: public MajinBuu14H(int id) {
// dòng 28: public void init() {
// dòng 33: public void run() {
// dòng 45: public void update() {
// dòng 60: public MaBuHold getMaBuHold() {
// dòng 73: public Zone getMapById(int mapId) {
// dòng 82: private void finish() {
// dòng 94: private void kickOut(Player player) {
```

### [RedRibbonHQ.java](../../SRC/src/nro/models/map/phoban/RedRibbonHQ.java) — 436 dòng

Kiểu được nhận diện: RedRibbonHQ.

```java
// dòng 48: public RedRibbonHQ(int id) {
// dòng 53: public void addZone(Zone zone) {
// dòng 57: public Zone getMapById(int mapId) {
// dòng 67: public void run() {
// dòng 79: public void openDoanhTrai(Player player) {
// dòng 118: private void init() {
// dòng 212: public void update() {
// dòng 250: public ItemMap NR(Zone zone) {
// dòng 258: public void randomNR() {
// dòng 278: public void finish() {
// dòng 289: private void kickOutOfDT(Player player) {
// dòng 296: private void sendTextDoanhTrai() {
// dòng 302: public void sendTextTimePickDoanhTrai() {
// dòng 308: private void removeTextDoanhTrai() {
// dòng 314: public void updateHPDame() {
// dòng 412: private void dispose() {
```

### [SnakeWay.java](../../SRC/src/nro/models/map/phoban/SnakeWay.java) — 269 dòng

Kiểu được nhận diện: SnakeWay.

```java
// dòng 47: public void addZone(Zone zone) {
// dòng 51: public SnakeWay(int id) {
// dòng 57: public void run() {
// dòng 69: public void update() {
// dòng 107: public void openConDuongRanDoc(Player plOpen, Clan clan, byte level) {
// dòng 124: public void sendThanhTichCDRD(Player pl) {
// dòng 143: private void init() {
// dòng 202: public void finish() {
// dòng 215: private void kickOutOfCDRD(Player player) {
// dòng 221: public Zone getMapById(int mapId) {
// dòng 230: private void sendTextConDuongRanDoc() {
// dòng 236: private void removeTextConDuongRanDoc() {
// dòng 242: public long getNumBossAlive() {
// dòng 246: public void dispose() {
```

## nro.models.map.service (5 tệp)

### [ChangeMapService.java](../../SRC/src/nro/models/map/service/ChangeMapService.java) — 1,140 dòng

Kiểu được nhận diện: ChangeMapService.

```java
// dòng 46: private ChangeMapService() {
// dòng 50: public static ChangeMapService gI() {
// dòng 60: public void openChangeMapTab(Player pl) {
// dòng 115: public void openZoneUI(Player pl) {
// dòng 165: public void changeZone(Player pl, int zoneId) {
// dòng 217: public void changeMapBySpaceShip(Player pl, int mapId, int zone, int x) {
// dòng 235: public void changeMapBySpaceShip(Player pl, Zone zoneJoin, int x) {
// dòng 259: public void changeMapInYard(Player pl, int mapId, int zoneId, int x) {
// dòng 278: public void changeMapInYard(Player pl, Zone zoneJoin, int x) {
// dòng 291: public void changeMap(Player pl, int mapId, int zone, int x, int y) {
// dòng 303: public void changeMap(Player pl, Zone zoneJoin, int x, int y) {
// dòng 315: public void changeMapYardrat(Player pl, Zone zoneJoin, int x, int y) {
// dòng 319: private void changeMap(Player pl, Zone zoneJoin, int mapId, int zoneId, int x, int y, byte typeSpace) {
// dòng 456: public void changeMapWaypoint(Player player) {
// dòng 515: public void resetPoint(Player player) {
// dòng 525: public void finishLoadMap(Player player) {
// dòng 585: private void sendEffectMeToMap(Player player) {
// dòng 624: private void sendEffectMapToMe(Player player) {
// dòng 741: public void spaceShipArrive(Player player, byte typeSendMSG, byte typeSpace) {
// dòng 766: public void goToMap(Player player, Zone zoneJoin) {
// dòng 778: public void exitMap(Player player) {
// dòng 822: public void goToTuongLai(Player player) {
// dòng 832: public void goToDBKB(Player player) {
// dòng 852: public void goToQuaKhu(Player player) {
// dòng 856: public void goToPotaufeu(Player player) {
// dòng 860: private void effectChangeMap(Player player, int seconds, byte type) {
// dòng 875: public Zone checkMapCanJoin(Player player, Zone zoneJoin) {
// dòng 1048: public Zone checkMapCanJoinByYardart(Player player, Zone zoneJoin) {
// dòng 1055: private void checkJoinSpecialMap(Player player) {
// dòng 1072: private void checkJoinMapMaBu(Player player) {
// dòng 1088: public void changeMapNonSpaceship(Player player, int mapid, int x, int y) {
// dòng 1093: public Zone getMapCanJoin(Player player, int mapId) {
// dòng 1119: public Zone getZoneJoinByMapIdAndZoneId(Player player, int mapId, int zoneId) {
// dòng 1131: public Map getMapById(int mapId) {
```

### [ItemMapService.java](../../SRC/src/nro/models/map/service/ItemMapService.java) — 72 dòng

Kiểu được nhận diện: ItemMapService.

```java
// dòng 14: public static ItemMapService gI() {
// dòng 21: public void pickItem(Player player, int itemMapId, boolean isThuHut) {
// dòng 30: public void removeItemMapAndSendClient(ItemMap itemMap) {
// dòng 35: public void sendItemMapDisappear(ItemMap itemMap) {
// dòng 47: public void removeItemMap(ItemMap itemMap) {
// dòng 52: public boolean findItemMapByPlayer(Player player, int tempId) {
// dòng 61: public boolean isBlackBall(int tempId) {
// dòng 65: public boolean isNamecBall(int tempId) {
// dòng 69: public boolean isNamecBallStone(int tempId) {
```

### [MapService.java](../../SRC/src/nro/models/map/service/MapService.java) — 629 dòng

Kiểu được nhận diện: MapService.

```java
// dòng 23: public static MapService gI() {
// dòng 30: public WayPoint getWaypointPlayerIn(Player player) {
// dòng 43: public int[][] readTileIndexTileType(int tileTypeFocus) {
// dòng 72: public int[][] readTileMap(int mapId) {
// dòng 91: public Zone getMapCanJoin(Player player, int mapId, int zoneId) {
// dòng 291: public List<Map> getAllMaps() {
// dòng 295: public Zone getZone(int mapId) {
// dòng 310: public Zone getZoneByMapIDAndZoneID(int mapId, int zoneId) {
// dòng 323: public Map getMapById(int mapId) {
// dòng 332: public Map getMapForCalich() {
// dòng 343: public Zone getMapWithRandZone(int mapId) {
// dòng 356: public String getPlanetName(byte planetId) {
// dòng 375: public List<Zone> getMapCapsule(Player pl) {
// dòng 403: public List<Zone> getMapBlackBall() {
// dòng 411: public List<Zone> getMapMaBu() {
// dòng 419: private void addListMapCapsule(Player pl, List<Zone> list, Zone zone) {
// dòng 430: public void sendPlayerMove(Player player) {
// dòng 444: public boolean isMapOffline(int mapId) {
// dòng 453: public boolean isMapBlackBallWar(int mapId) {
// dòng 457: public boolean isMapMaBu(int mapId) {
// dòng 461: public boolean isHome(int mapId) {
// dòng 465: public boolean isMapYardart(int mapId) {
// dòng 469: public boolean isMapCadic(int mapId) {
// dòng 473: public boolean isMapBossFinal(int mapId) {
// dòng 477: public boolean isMapCold(Map map) {
// dòng 482: public boolean isMapTuongLai(Map map) {
// dòng 487: public boolean isMapCold(int mapId) {
// dòng 491: public boolean isMapDoanhTrai(int mapId) {
// dòng 495: public boolean isMapSieuThanhThuy(int mapId) {
// dòng 499: public boolean isMapRiengTu(int mapId) {
// dòng 503: public boolean isMapNgucTu(int mapId) {
// dòng 507: public boolean isMapHuyDiet(int mapId) {
// dòng 511: public boolean isMapBanDoKhoBau(int mapId) {
// dòng 515: public boolean isMapConDuongRanDoc(int mapId) {
// dòng 519: public boolean isMapKhiGasHuyDiet(int mapId) {
// dòng 523: public boolean isMapHanhTinhThucVat(int mapId) {
// dòng 527: public boolean isMapNguHanhSon(int mapId) {
// dòng 531: public boolean AllMap(int mapId) {
// dòng 535: public boolean shouldChangeMap(int currentMapId, int newMapId) {
// dòng 541: public boolean isMapPhoBan(int mapId) {
// dòng 545: public boolean isskh(int mapId) {
// dòng 551: public boolean isMapTuongLai(int mapId) {
// dòng 557: public boolean isMapEvent8_3(int mapId) {
// dòng 561: public boolean isMapEventHungVuong(int mapId) {
// dòng 565: public boolean isMapUpSKH(int mapId) {
// dòng 571: public boolean isMapMabu2H(int mapId) {
// dòng 575: public boolean isMapUpPorata(int mapId) {
// dòng 579: public boolean isMapLuyenTap(int mapId) {
// dòng 583: public boolean isMapNappa(int mapId) {
// dòng 587: public boolean isMapStar(int mapId) {
// dòng 591: public boolean isMapEventHalloween(int mapId) {
// dòng 595: public boolean isMapEvent1(int mapId) {
// dòng 599: public boolean isMap3Planets(int mapId) {
// dòng 605: public void goToMap(Player player, Zone zoneJoin) {
// dòng 617: public List<Player> getPlayersInMap(int mapId) {
```

### [NpcManager.java](../../SRC/src/nro/models/map/service/NpcManager.java) — 62 dòng

Kiểu được nhận diện: NpcManager.

```java
// dòng 18: public static Npc getByIdAndMap(int id, int mapId) {
// dòng 27: public static Npc getNpc(byte tempId) {
// dòng 36: public static List<Npc> getNpcsByMapPlayer(Player player) {
```

### [NpcService.java](../../SRC/src/nro/models/map/service/NpcService.java) — 108 dòng

Kiểu được nhận diện: NpcService.

```java
// dòng 22: public static NpcService gI() {
// dòng 29: public void createMenuRongThieng(Player player, int indexMenu, String npcSay, String... menuSelect) {
// dòng 33: public void createMenuConMeo(Player player, int indexMenu, int avatar, String npcSay, String... menuSelect) {
// dòng 37: public void createMenuConMeo(Player player, int indexMenu, int avatar, String npcSay, String[] menuSelect, Object object) {
// dòng 42: private void createMenu(Player player, int indexMenu, byte npcTempId, int avatar, String npcSay, String... menuSelect) {
// dòng 70: public void createTutorial(Player player, int avatar, String npcSay) {
// dòng 85: public void createTutorial(Player player, int tempId, int avatar, String npcSay) {
// dòng 100: public int getAvatar(int npcId) {
```

## nro.models.matches (10 tệp)

### [DHVT.java](../../SRC/src/nro/models/matches/DHVT.java) — 40 dòng

Kiểu được nhận diện: DHVT.

```java
// dòng 9: public DHVT(Player p1, Player p2) {
// dòng 14: public void start() {
// dòng 19: public void finish() {
// dòng 24: public void dispose() {
// dòng 29: public void update() {
// dòng 33: public void reward(Player plWin) {
// dòng 37: public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
```

### [LuyenTap.java](../../SRC/src/nro/models/matches/LuyenTap.java) — 51 dòng

Kiểu được nhận diện: LuyenTap.

```java
// dòng 12: public LuyenTap(Player p1, Player p2) {
// dòng 17: protected void changeToTypePK() {
// dòng 25: public void finish() {
// dòng 30: public void dispose() {
// dòng 35: public void update() {
// dòng 39: public void reward(Player plWin) {
// dòng 43: public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
```

### [PKCommeson.java](../../SRC/src/nro/models/matches/PKCommeson.java) — 45 dòng

Kiểu được nhận diện: PKCommeson.

```java
// dòng 11: public PKCommeson(Player p1, Player p2) {
// dòng 16: public void finish() {
// dòng 21: public void dispose() {
// dòng 26: public void update() {
// dòng 30: public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
// dòng 41: public void reward(Player plWin) {
```

### [PVP.java](../../SRC/src/nro/models/matches/PVP.java) — 81 dòng

Kiểu được nhận diện: PVP.

```java
// dòng 18: public PVP(TYPE_PVP type, Player p1, Player p2) {
// dòng 30: public void start() {
// dòng 35: protected void changeToTypePK() {
// dòng 42: private void changeToTypeNonPK() {
// dòng 50: public boolean isInPVP(Player pl) {
// dòng 55: public void lose(Player plLose, TYPE_LOSE_PVP typeLose) {
// dòng 69: public void dispose() {
```

### [PVPService.java](../../SRC/src/nro/models/matches/PVPService.java) — 280 dòng

Kiểu được nhận diện: PVPService.

```java
// dòng 22: public static PVPService gI() {
// dòng 29: public PVPService() {
// dòng 37: public void controllerThachDau(Player player, Message message) {
// dòng 85: private void openSelectGold(Player pl, Player plMap) {
// dòng 101: public void sendInvitePVP(Player pl, byte selectGold) {
// dòng 147: public void sendInvitePVP(Player pl, Player plMap) {
// dòng 180: private void acceptPVP(Player pl) {
// dòng 215: private void acceptPVP2(Player pl) {
// dòng 234: public void openSelectRevenge(Player pl, long idEnemy) {
// dòng 251: public void acceptRevenge(Player pl) {
```

### [ThachDau.java](../../SRC/src/nro/models/matches/ThachDau.java) — 74 dòng

Kiểu được nhận diện: ThachDau.

```java
// dòng 17: public ThachDau(Player p1, Player p2, int goldThachDau) {
// dòng 24: public void start() {
// dòng 33: public void finish() {
// dòng 38: public void dispose() {
// dòng 43: public void update() {
// dòng 47: public void reward(Player plWin) {
// dòng 53: public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
```

### [TOP.java](../../SRC/src/nro/models/matches/TOP.java) — 42 dòng

Kiểu được nhận diện: TOP.

```java
// dòng 38: public void setId_player(int id_player) {
```

### [TraThu.java](../../SRC/src/nro/models/matches/TraThu.java) — 69 dòng

Kiểu được nhận diện: TraThu.

```java
// dòng 14: public TraThu(Player p1, Player p2) {
// dòng 19: public void start() {
// dòng 38: public void finish() {
// dòng 43: public void update() {
// dòng 48: public void reward(Player plWin) {
// dòng 53: public void sendResult(Player plLose, TYPE_LOSE_PVP typeLose) {
```

### [TYPE_LOSE_PVP.java](../../SRC/src/nro/models/matches/TYPE_LOSE_PVP.java) — 7 dòng

Kiểu được nhận diện: TYPE_LOSE_PVP.

### [TYPE_PVP.java](../../SRC/src/nro/models/matches/TYPE_PVP.java) — 10 dòng

Kiểu được nhận diện: TYPE_PVP.

## nro.models.matches.dai_hoi_vo_thuat (9 tệp)

### [DeathOrAliveArena.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/DeathOrAliveArena.java) — 275 dòng

Kiểu được nhận diện: DeathOrAliveArena.

```java
// dòng 58: public void update() {
// dòng 116: public void ready() {
// dòng 124: public void toTheNextRound() {
// dòng 154: public void setBoss(Boss boss) {
// dòng 158: public void setTime(int time) {
// dòng 162: public void setTimeWait(int timeWait) {
// dòng 166: private void die() {
// dòng 175: private void timeOut() {
// dòng 184: private void champion() {
// dòng 193: public void leave() {
// dòng 203: private void reward() {
// dòng 207: private void traThuongHatMit(boolean playerWin) {
// dòng 250: public void endChallenge() {
// dòng 270: public void addBinhChon(Player pl) {
```

### [DeathOrAliveArenaService.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/DeathOrAliveArenaService.java) — 95 dòng

Kiểu được nhận diện: DeathOrAliveArenaService.

```java
// dòng 22: public static DeathOrAliveArenaService gI() {
// dòng 29: public void startChallenge(Player player) {
// dòng 64: public void sendTypePK(Player player, Player boss) {
// dòng 76: public Zone getMapChallenge(int mapId) {
```

### [SuperRank.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/SuperRank.java) — 245 dòng

Kiểu được nhận diện: SuperRank.

```java
// dòng 36: public SuperRank(Player player, long rivalId, Zone zone) {
// dòng 58: private void updateZoneInfo(Player p1, Player p2) {
// dòng 66: private void start() {
// dòng 75: public void run() {
// dòng 89: private void update() {
// dòng 112: private void handleMatchStart() {
// dòng 133: private boolean isPlayerAliveInZone() {
// dòng 138: private void handleWin() {
// dòng 150: private void handleLose() {
// dòng 161: private void finishCombat() {
// dòng 174: private void updateResults(boolean playerWon) {
// dòng 209: private void resetZoneInfo() {
// dòng 219: public void dispose() {
// dòng 242: public void shutdown() {
```

### [SuperRankBuilder.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/SuperRankBuilder.java) — 28 dòng

Kiểu được nhận diện: SuperRankBuilder.

```java
// dòng 24: public void dispose() {
```

### [SuperRankService.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/SuperRankService.java) — 236 dòng

Kiểu được nhận diện: SuperRankService.

```java
// dòng 26: public static SuperRankService gI() {
// dòng 33: public void competing(Player player, long id) {
// dòng 86: public void topList(Player player, int type) {
// dòng 126: public Player loadPlayer(long id) {
// dòng 138: public Player getPlayer(long id) {
// dòng 142: public String textInfo(Player pl) {
// dòng 155: public String textInfoNew(Player pl) {
// dòng 180: public String textStatus(SuperRankBuilder srb) {
// dòng 189: public String textReward(int rank) {
// dòng 203: public int reward(int rank) {
// dòng 217: public Zone getZone(int mapId) {
```

### [The23rdMartialArtCongress.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/The23rdMartialArtCongress.java) — 252 dòng

Kiểu được nhận diện: The23rdMartialArtCongress.

```java
// dòng 54: public void update() {
// dòng 137: public void toTheNextRound() {
// dòng 191: public void die() {
// dòng 200: private void timeOut() {
// dòng 207: private void champion() {
// dòng 212: public void leave() {
// dòng 223: private void reward() {
// dòng 229: public void endChallenge() {
```

### [The23rdMartialArtCongressService.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/The23rdMartialArtCongressService.java) — 76 dòng

Kiểu được nhận diện: The23rdMartialArtCongressService.

```java
// dòng 15: public static The23rdMartialArtCongressService gI() {
// dòng 22: public void startChallenge(Player player) {
// dòng 47: public static void setTimeout(Runnable runnable, int delay) {
// dòng 57: public void sendTypePK(Player player, Player boss) {
// dòng 69: public Zone getMapChallenge(int mapId) {
```

### [WorldMartialArtsTournament.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/WorldMartialArtsTournament.java) — 295 dòng

Kiểu được nhận diện: WorldMartialArtsTournament.

```java
// dòng 30: public WorldMartialArtsTournament(Player player_1, Player player_2, Zone zone) {
// dòng 40: private void init() {
// dòng 51: public void run() {
// dòng 65: private void update() {
// dòng 117: private void timeOut() {
// dòng 137: private void die() {
// dòng 154: private void leaveMap() {
// dòng 171: private boolean fallOutCheck(Player pl) {
// dòng 175: private void fallOut() {
// dòng 191: public void finish() {
// dòng 251: private void thongBao() {
// dòng 268: private void npcChat() {
// dòng 285: public void dispose() {
```

### [WorldMartialArtsTournamentService.java](../../SRC/src/nro/models/matches/dai_hoi_vo_thuat/WorldMartialArtsTournamentService.java) — 211 dòng

Kiểu được nhận diện: WorldMartialArtsTournamentService.

```java
// dòng 17: public static int getTournament() {
// dòng 41: public static int getNextTournamentTime() {
// dòng 49: public static String sayText() {
// dòng 55: public static void menu(Npc npc, Player player) {
// dòng 80: public static void confirm(Npc npc, Player player, int select) {
// dòng 130: public static boolean regCheck(Player player) {
// dòng 134: public static void dangky_huy(Npc npc, Player player) {
```

## nro.models.matches.giai_dau (3 tệp)

### [DeathOrAliveArenaManager.java](../../SRC/src/nro/models/matches/giai_dau/DeathOrAliveArenaManager.java) — 63 dòng

Kiểu được nhận diện: DeathOrAliveArenaManager.

```java
// dòng 17: public static DeathOrAliveArenaManager gI() {
// dòng 25: public void run() {
// dòng 36: public void update() {
// dòng 47: public void add(DeathOrAliveArena vdst) {
// dòng 51: public void remove(DeathOrAliveArena vdst) {
// dòng 55: public DeathOrAliveArena getVDST(@NonNull Zone zone) {
```

### [The23rdMartialArtCongressManager.java](../../SRC/src/nro/models/matches/giai_dau/The23rdMartialArtCongressManager.java) — 75 dòng

Kiểu được nhận diện: The23rdMartialArtCongressManager.

```java
// dòng 19: public static The23rdMartialArtCongressManager gI() {
// dòng 27: public void run() {
// dòng 39: public void update() {
// dòng 50: public void add(The23rdMartialArtCongress mc) {
// dòng 54: public void remove(The23rdMartialArtCongress mc) {
// dòng 58: public The23rdMartialArtCongress getMC(@NonNull Zone zone) {
// dòng 67: public boolean plCheck(Player player) {
```

### [WorldMartialArtsTournamentManager.java](../../SRC/src/nro/models/matches/giai_dau/WorldMartialArtsTournamentManager.java) — 265 dòng

Kiểu được nhận diện: WorldMartialArtsTournamentManager.

```java
// dòng 49: public static WorldMartialArtsTournamentManager gI() {
// dòng 56: public WorldMartialArtsTournamentManager() {
// dòng 63: public void run() {
// dòng 91: public void updateChatText() {
// dòng 110: public void update() {
// dòng 190: public void sendWaitNotify() {
// dòng 204: public void reward(Player pl) {
// dòng 217: public void addWMAT(WorldMartialArtsTournament wmat) {
// dòng 221: public void removeWMAT(WorldMartialArtsTournament wmat) {
// dòng 225: public boolean checkPlayer(long id) {
// dòng 229: public Zone getZoneTournament() {
// dòng 249: public WorldMartialArtsTournament getWMAT(@NonNull Zone zone) {
// dòng 258: public Player getPlayerById(long id) {
```

## nro.models.minigame (5 tệp)

### [ChonAiDay_Gem.java](../../SRC/src/nro/models/minigame/ChonAiDay_Gem.java) — 142 dòng

Kiểu được nhận diện: ChonAiDay_Gem.

```java
// dòng 25: public static ChonAiDay_Gem gI() {
// dòng 32: public void addPlayerVIP(Player pl) {
// dòng 38: public void addPlayerNormar(Player pl) {
// dòng 44: public void removePlayerVIP(Player pl) {
// dòng 48: public void removePlayerNormar(Player pl) {
// dòng 53: public void run() {
// dòng 127: private void resetPlayers(List<Player> players) {
// dòng 137: private void resetChonAiDay() {
```

### [ChonAiDay_Gold.java](../../SRC/src/nro/models/minigame/ChonAiDay_Gold.java) — 121 dòng

Kiểu được nhận diện: ChonAiDay_Gold.

```java
// dòng 25: public static ChonAiDay_Gold gI() {
// dòng 32: public void addPlayerVIP(Player pl) {
// dòng 38: public void addPlayerNormar(Player pl) {
// dòng 44: public void removePlayerVIP(Player pl) {
// dòng 48: public void removePlayerNormar(Player pl) {
// dòng 53: public void run() {
// dòng 106: private void resetPlayers(List<Player> players) {
// dòng 116: private void resetChonAiDay() {
```

### [ConSoMayManData.java](../../SRC/src/nro/models/minigame/ConSoMayManData.java) — 13 dòng

Kiểu được nhận diện: ConSoMayManData.

### [ConSoMayManGem.java](../../SRC/src/nro/models/minigame/ConSoMayManGem.java) — 263 dòng

Kiểu được nhận diện: ConSoMayManGem.

```java
// dòng 34: private ConSoMayManGem() {
// dòng 37: public static ConSoMayManGem gI() {
// dòng 45: public void run() {
// dòng 73: public void newData(Player player, int point) {
// dòng 106: public void ramdom1SoLe(Player player) {
// dòng 148: public void ramdom1SoChan(Player player) {
// dòng 190: public String strNumber(int id) {
// dòng 200: public String strFinish(int id) {
// dòng 223: public void ResetGame(int result) {
```

### [ConSoMayManGold.java](../../SRC/src/nro/models/minigame/ConSoMayManGold.java) — 271 dòng

Kiểu được nhận diện: ConSoMayManGold.

```java
// dòng 37: private ConSoMayManGold() {
// dòng 40: public static ConSoMayManGold gI() {
// dòng 48: public void run() {
// dòng 76: public void newData(Player player, int point) {
// dòng 110: public void ramdom1SoLe(Player player) {
// dòng 153: public void ramdom1SoChan(Player player) {
// dòng 196: public String strNumber(int id) {
// dòng 206: public String strFinish(int id) {
// dòng 232: public void ResetGame(int result) {
```

## nro.models.mob (5 tệp)

### [BigBoss.java](../../SRC/src/nro/models/mob/BigBoss.java) — 28 dòng

Kiểu được nhận diện: BigBoss.

```java
// dòng 12: public BigBoss(Mob mob) {
// dòng 17: public void update() {
```

### [Mob.java](../../SRC/src/nro/models/mob/Mob.java) — 1,278 dòng

Kiểu được nhận diện: Mob.

```java
// dòng 65: public Mob(Mob mob) {
// dòng 87: public Mob() {
// dòng 98: public void setTiemNang() {
// dòng 102: public boolean isDie() {
// dòng 106: public void setDie() {
// dòng 111: public void addTemporaryEnemies(Player pl) {
// dòng 117: public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
// dòng 223: public long getTiemNangForPlayer(Player pl, long dame) {
// dòng 256: public void update() {
// dòng 314: public boolean isBigBoss() {
// dòng 321: public void attack() {
// dòng 342: public Player getPlayerCanAttack() {
// dòng 367: private Player getFirstPlayerCanAttack() {
// dòng 394: private void mobAttackPlayer(Player player) {
// dòng 434: private void sendMobAttackMe(Player player, int dame) {
// dòng 449: private void sendMobAttackPlayer(Player player) {
// dòng 463: public void hoiSinh() {
// dòng 469: public int lvMob() {
// dòng 480: public void sendMobHoiSinh() {
// dòng 499: public void hoi_hp(int hp) {
// dòng 521: public void sendEffect(int Effect) {
// dòng 541: private void sendMobDieAffterAttacked(Player plKill, int dameHit) {
// dòng 557: private void hutItem(Player player, List<ItemMap> items) {
// dòng 581: private List<ItemMap> mobReward(Player player, ItemMap itemTask, Message msg) {
// dòng 603: public List<ItemMap> getItemMobReward(Player player, int x, int yEnd) {
// dòng 1073: private ItemMap dropItemTask(Player player) {
// dòng 1100: private void sendMobStillAliveAffterAttacked(int dameHit, boolean crit) {
// dòng 1116: public void hoiSinhMobPhoBan() {
// dòng 1133: public void hoiSinhMobTayKarin() {
// dòng 1149: public void sendSieuQuai(int type) {
// dòng 1161: public void sendDisable(boolean bool) {
// dòng 1173: public void sendDoneMove(boolean bool) {
// dòng 1185: public void sendFire(boolean bool) {
// dòng 1197: public void sendIce(boolean bool) {
// dòng 1209: public void sendWind(boolean bool) {
// dòng 1221: public void sendMobMaxHp(int maxHp) {
// dòng 1233: private void phanSatThuong(Player plTarget, long dame) {
// dòng 1264: public void startDie() {
```

### [MobEffectSkill.java](../../SRC/src/nro/models/mob/MobEffectSkill.java) — 200 dòng

Kiểu được nhận diện: MobEffectSkill.

```java
// dòng 12: public MobEffectSkill(Mob mob) {
// dòng 20: public void update() {
// dòng 54: public boolean isHaveEffectSkill() {
// dòng 58: public void startStun(long lastTimeStartBlind, int timeBlind) {
// dòng 64: private void removeStun() {
// dòng 82: public void setThoiMien(long lastTimeThoiMien, int timeThoiMien) {
// dòng 88: public void removeThoiMien() {
// dòng 107: public void setStartBlindDCTT(long lastTimeBlindDCTT, int timeBlindDCTT) {
// dòng 113: public void removeBlindDCTT() {
// dòng 132: public void setTroi(long lastTimeAnTroi, int timeAnTroi) {
// dòng 138: public void removeAnTroi() {
// dòng 158: public void removeSocola() {
// dòng 172: public void setSocola(long lastTimeSocola, int timeSocola) {
// dòng 185: public void removeBinh() {
// dòng 190: public void setBinh(Player plAtt, long lastTimeBinh, int timeBinh) {
```

### [MobMe.java](../../SRC/src/nro/models/mob/MobMe.java) — 151 dòng

Kiểu được nhận diện: MobMe.

```java
// dòng 15: public MobMe(Player player) {
// dòng 31: public void update() {
// dòng 38: public void attack(Player pl, Mob mob, boolean miss) {
// dòng 76: public void spawn() {
// dòng 90: public void goToMap(Zone zone) {
// dòng 98: private void removeMobInMap() {
// dòng 110: public void mobMeDie() {
// dòng 123: public synchronized void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
// dòng 147: public void dispose() {
```

### [MobPoint.java](../../SRC/src/nro/models/mob/MobPoint.java) — 41 dòng

Kiểu được nhận diện: MobPoint.

```java
// dòng 12: public MobPoint(Mob mob) {
// dòng 16: public int getHpFull() {
// dòng 20: public void setHpFull(int hp) {
// dòng 24: public int gethp() {
// dòng 28: public void sethp(int hp) {
// dòng 36: public int getDameAttack() {
```

## nro.models.mob_bigboss (9 tệp)

### [GaChinCua.java](../../SRC/src/nro/models/mob_bigboss/GaChinCua.java) — 97 dòng

Kiểu được nhận diện: GaChinCua.

```java
// dòng 13: public GaChinCua(Mob mob) {
// dòng 18: public void attack() {
```

### [GauTuongCuop.java](../../SRC/src/nro/models/mob_bigboss/GauTuongCuop.java) — 205 dòng

Kiểu được nhận diện: GauTuongCuop.

```java
// dòng 21: public GauTuongCuop(Mob mob) {
// dòng 29: public void update() {
// dòng 36: public void attack() {
// dòng 98: public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
// dòng 109: public void setDie() {
// dòng 134: public int getDameAttack() {
// dòng 138: private void rewardContributors() {
```

### [Hirudegarn.java](../../SRC/src/nro/models/mob_bigboss/Hirudegarn.java) — 230 dòng

Kiểu được nhận diện: Hirudegarn.

```java
// dòng 17: public Hirudegarn(Mob mob) {
// dòng 22: public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
// dòng 28: public void update() {
// dòng 162: public void attack() {
```

### [MayDoSucManh.java](../../SRC/src/nro/models/mob_bigboss/MayDoSucManh.java) — 101 dòng

Kiểu được nhận diện: MayDoSucManh.

```java
// dòng 29: public MayDoSucManh(Mob mob) {
// dòng 35: public void setDie() {
// dòng 41: public void moveTo(int x, int y) {
// dòng 47: public void injured(Player plAtt, long damage, boolean dieWhenHpFull) {
// dòng 62: public void updateMilestone(Player pl, long realDamage) {
// dòng 94: private void startHealthRegeneration() {
```

### [NguaChinLmao.java](../../SRC/src/nro/models/mob_bigboss/NguaChinLmao.java) — 96 dòng

Kiểu được nhận diện: NguaChinLmao.

```java
// dòng 13: public NguaChinLmao(Mob mob) {
// dòng 18: public void attack() {
```

### [Piano.java](../../SRC/src/nro/models/mob_bigboss/Piano.java) — 106 dòng

Kiểu được nhận diện: Piano.

```java
// dòng 13: public Piano(Mob mob) {
// dòng 18: public void attack() {
```

### [RobotBaoVe.java](../../SRC/src/nro/models/mob_bigboss/RobotBaoVe.java) — 45 dòng

Kiểu được nhận diện: RobotBaoVe.

```java
// dòng 10: public RobotBaoVe(Mob mob) {
// dòng 15: public void update() {
// dòng 23: public void attack() {
```

### [VoiChinNga.java](../../SRC/src/nro/models/mob_bigboss/VoiChinNga.java) — 96 dòng

Kiểu được nhận diện: VoiChinNga.

```java
// dòng 13: public VoiChinNga(Mob mob) {
// dòng 18: public void attack() {
```

### [VuaBachTuoc.java](../../SRC/src/nro/models/mob_bigboss/VuaBachTuoc.java) — 63 dòng

Kiểu được nhận diện: VuaBachTuoc.

```java
// dòng 11: public VuaBachTuoc(Mob mob) {
// dòng 16: public void update() {
// dòng 24: public void attack() {
```

## nro.models.network (12 tệp)

### [Collector.java](../../SRC/src/nro/models/network/Collector.java) — 76 dòng

Kiểu được nhận diện: Collector.

```java
// dòng 18: public Collector(ISession session, Socket socket) {
// dòng 23: public Collector setSocket(Socket socket) {
// dòng 32: public void run() {
// dòng 54: public void setCollect(IMessageSendCollect collect) {
// dòng 58: public void setMessageHandler(IMessageHandler handler) {
// dòng 62: public void close() {
// dòng 71: public void dispose() {
```

### [KeyHandler.java](../../SRC/src/nro/models/network/KeyHandler.java) — 24 dòng

Kiểu được nhận diện: KeyHandler.

```java
// dòng 9: public void sendKey(ISession session) {
```

### [Message.java](../../SRC/src/nro/models/network/Message.java) — 247 dòng

Kiểu được nhận diện: Message.

```java
// dòng 21: public Message(int command) {
// dòng 25: public Message(byte command) {
// dòng 31: public Message(byte command, byte[] data) {
// dòng 38: public DataOutputStream writer() {
// dòng 43: public DataInputStream reader() {
// dòng 48: public byte[] getData() {
// dòng 53: public void cleanup() {
// dòng 72: public void dispose() {
// dòng 81: public int read() throws IOException {
// dòng 86: public int read(byte[] b) throws IOException {
// dòng 91: public int read(byte[] b, int off, int len) throws IOException {
// dòng 96: public boolean readBoolean() throws IOException {
// dòng 101: public byte readByte() throws IOException {
// dòng 106: public short readShort() throws IOException {
// dòng 111: public int readInt() throws IOException {
// dòng 116: public long readLong() throws IOException {
// dòng 121: public float readFloat() throws IOException {
// dòng 126: public double readDouble() throws IOException {
// dòng 131: public char readChar() throws IOException {
// dòng 136: public String readUTF() throws IOException {
// dòng 141: public void readFully(byte[] b) throws IOException {
// dòng 146: public void readFully(byte[] b, int off, int len) throws IOException {
// dòng 151: public int readUnsignedByte() throws IOException {
// dòng 156: public int readUnsignedShort() throws IOException {
// dòng 161: public void write(byte[] b) throws IOException {
// dòng 166: public void write(int b) throws IOException {
// dòng 171: public void write(byte[] b, int off, int len) throws IOException {
// dòng 176: public void writeBoolean(boolean v) throws IOException {
// dòng 181: public void writeByte(int v) throws IOException {
// dòng 186: public void writeBytes(String s) throws IOException {
// dòng 191: public void writeChar(int v) throws IOException {
// dòng 196: public void writeChars(String s) throws IOException {
// dòng 201: public void writeDouble(double v) throws IOException {
// dòng 206: public void writeFloat(float v) throws IOException {
// dòng 211: public void writeInt(int v) throws IOException {
// dòng 216: public void writeLong(long v) throws IOException {
// dòng 221: public void writeShort(int v) throws IOException {
// dòng 226: public void writeUTF(String str) throws IOException {
// dòng 231: public BufferedImage readImage() throws IOException {
// dòng 240: public void writeImage(BufferedImage image, String format) throws IOException {
```

### [MessageSendCollect.java](../../SRC/src/nro/models/network/MessageSendCollect.java) — 101 dòng

Kiểu được nhận diện: MessageSendCollect.

```java
// dòng 16: public Message readMessage(ISession session, DataInputStream dis) throws Exception {
// dòng 43: public byte readKey(ISession session, byte b) {
// dòng 52: public void doSendMessage(ISession session, DataOutputStream dos, Message msg) throws Exception {
// dòng 94: public byte writeKey(ISession session, byte b) {
```

### [MyKeyHandler.java](../../SRC/src/nro/models/network/MyKeyHandler.java) — 15 dòng

Kiểu được nhận diện: MyKeyHandler.

```java
// dòng 9: public void sendKey(ISession session) {
```

### [MySession.java](../../SRC/src/nro/models/network/MySession.java) — 161 dòng

Kiểu được nhận diện: MySession.

```java
// dòng 68: public MySession(Socket socket) {
// dòng 74: public void sendKey() throws Exception {
// dòng 79: public void sendSessionKey() {
// dòng 94: public void login(String username, String password) {
```

### [Network.java](../../SRC/src/nro/models/network/Network.java) — 183 dòng

Kiểu được nhận diện: Network.

```java
// dòng 30: public static Network gI() {
// dòng 37: private Network() {
// dòng 41: public INetwork init() {
// dòng 52: public INetwork start(int port) throws Exception {
// dòng 79: public INetwork close() {
// dòng 94: public INetwork dispose() {
// dòng 102: public INetwork setAcceptHandler(ISessionAcceptHandler handler) {
// dòng 108: public void run() {
// dòng 149: public INetwork setDoSomeThingWhenClose(IServerClose serverClose) {
// dòng 155: public INetwork randomKey(boolean isRandom) {
// dòng 161: public boolean isRandomKey() {
// dòng 166: public INetwork setTypeSessioClone(Class clazz) throws Exception {
// dòng 172: public ISessionAcceptHandler getAcceptHandler() throws Exception {
// dòng 180: public void stopConnect() {
```

### [QueueHandler.java](../../SRC/src/nro/models/network/QueueHandler.java) — 62 dòng

Kiểu được nhận diện: QueueHandler.

```java
// dòng 18: public QueueHandler(@NonNull ISession session) {
// dòng 27: public void run() {
// dòng 43: public void addMessage(Message msg) {
// dòng 52: public void close() {
// dòng 58: public void dispose() {
```

### [Sender.java](../../SRC/src/nro/models/network/Sender.java) — 103 dòng

Kiểu được nhận diện: Sender.

```java
// dòng 22: public Sender(@NonNull ISession session, @NonNull Socket socket) {
// dòng 37: public Sender setSocket(@NonNull Socket socket) {
// dòng 49: public void run() {
// dòng 66: public synchronized void doSendMessage(Message message) throws Exception {
// dòng 70: public void sendMessage(Message msg) {
// dòng 79: public void setSend(IMessageSendCollect sendCollect) {
// dòng 83: public int getNumMessages() {
// dòng 87: public void close() {
// dòng 97: public void dispose() {
```

### [Session.java](../../SRC/src/nro/models/network/Session.java) — 192 dòng

Kiểu được nhận diện: Session.

```java
// dòng 32: public static ISession gI() throws Exception {
// dòng 39: public Session(Socket socket) {
// dòng 59: public ISession start() {
// dòng 66: public ISession startSend() {
// dòng 72: public ISession startCollect() {
// dòng 78: public ISession setSendCollect(IMessageSendCollect collect) {
// dòng 85: public ISession setMessageHandler(IMessageHandler handler) {
// dòng 91: public ISession setKeyHandler(IKeySessionHandler handler) {
// dòng 97: public void sendMessage(Message msg) {
// dòng 104: public void doSendMessage(Message msg) throws Exception {
// dòng 109: public void sendKey() throws Exception {
// dòng 120: public void setSentKey(boolean sent) {
// dòng 125: public boolean sentKey() {
// dòng 130: public boolean isConnected() {
// dòng 135: public long getID() {
// dòng 140: public String getIP() {
// dòng 145: public byte[] getKey() {
// dòng 150: public int getNumMessages() {
// dòng 155: public void disconnect() {
// dòng 175: public void dispose() {
```

### [SessionFactory.java](../../SRC/src/nro/models/network/SessionFactory.java) — 20 dòng

Kiểu được nhận diện: SessionFactory.

```java
// dòng 10: public static SessionFactory gI() {
// dòng 17: public ISession cloneSession(Class clazz, Socket socket) throws Exception {
```

### [SessionManager.java](../../SRC/src/nro/models/network/SessionManager.java) — 70 dòng

Kiểu được nhận diện: SessionManager.

```java
// dòng 14: public static SessionManager gI() {
// dòng 21: public void putSession(ISession session) {
// dòng 25: public void removeSession(ISession session) {
// dòng 29: public List<ISession> getSessions() {
// dòng 33: public void cleanupSessions() {
// dòng 42: public void startCleanupThread() {
// dòng 58: public ISession findByID(long id) throws Exception {
// dòng 67: public int getNumSession() {
```

## nro.models.npc (8 tệp)

### [BaseMenu.java](../../SRC/src/nro/models/npc/BaseMenu.java) — 29 dòng

Kiểu được nhận diện: BaseMenu.

```java
// dòng 14: public void openMenu(Player player) {
```

### [DuaHauEgg.java](../../SRC/src/nro/models/npc/DuaHauEgg.java) — 96 dòng

Kiểu được nhận diện: DuaHauEgg.

```java
// dòng 21: public DuaHauEgg(Player player, long lastTimeCreate, long timeDone) {
// dòng 27: public static void createDuaHauEgg(Player player) {
// dòng 31: public void sendDuaHauEgg() {
// dòng 61: public int getSecondDone() {
// dòng 66: public void openEgg() {
// dòng 84: public void subTimeDone(int d, int h, int m, int s) {
// dòng 89: public void dispose() {
// dòng 93: public void plant() {
```

### [IAtionNpc.java](../../SRC/src/nro/models/npc/IAtionNpc.java) — 10 dòng

Kiểu được nhận diện: IAtionNpc.

### [MabuEgg.java](../../SRC/src/nro/models/npc/MabuEgg.java) — 91 dòng

Kiểu được nhận diện: MabuEgg.

```java
// dòng 21: public MabuEgg(Player player, long lastTimeCreate, long timeDone) {
// dòng 27: public static void createMabuEgg(Player player) {
// dòng 31: public void sendMabuEgg() {
// dòng 48: public int getSecondDone() {
// dòng 53: public void openEgg(int gender) {
// dòng 72: public void destroyEgg() {
// dòng 83: public void subTimeDone(int d, int h, int m, int s) {
// dòng 88: public void dispose() {
```

### [MagicTree.java](../../SRC/src/nro/models/npc/MagicTree.java) — 319 dòng

Kiểu được nhận diện: MagicTree.

```java
// dòng 60: public MagicTree(Player player, byte level, byte currPeas, long lastTimeHarvest, boolean isUpgrade, long lastTimeUpgrade) {
// dòng 72: public void update() {
// dòng 97: public void loadMagicTree() {
// dòng 129: public void openMenuTree() {
// dòng 157: public void harvestPea() {
// dòng 180: public void showConfirmUpgradeMagicTree() {
// dòng 185: public void showConfirmUnuppgradeMagicTree() {
// dòng 190: public void upgradeMagicTree() {
// dòng 205: public void unupgradeMagicTree() {
// dòng 214: public void fastRespawnPea() {
// dòng 235: public void fastUpgradeMagicTree() {
// dòng 257: private byte getMaxPea() {
// dòng 261: private short getSecondPerPea() {
// dòng 265: private int getSecondPea() {
// dòng 272: private int getSecondUpgrade() {
// dòng 276: private String getTextMenuUpgrade() {
// dòng 295: private long getTimeUpgrade() {
// dòng 302: public void dispose() {
// dòng 306: public int addPeaHarvest(byte level, int quantity) {
```

### [NonInteractiveNPC.java](../../SRC/src/nro/models/npc/NonInteractiveNPC.java) — 256 dòng

Kiểu được nhận diện: NonInteractiveNPC.

```java
// dòng 23: public void initNonInteractiveNPC() {
// dòng 28: public short getHead() {
// dòng 33: public short getBody() {
// dòng 38: public short getLeg() {
// dòng 42: public void joinMap(Zone z, Player player) {
// dòng 48: public void update() {
// dòng 82: private void move() {
// dòng 98: private void init() {
```

### [Npc.java](../../SRC/src/nro/models/npc/Npc.java) — 185 dòng

Kiểu được nhận diện: Npc.

```java
// dòng 36: public Npc(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 47: public void initBaseMenu(String text) {
// dòng 59: public void createOtherMenu(Player player, int indexMenu, String npcSay, String... menuSelect) {
// dòng 77: public void createOtherMenu(Player player, int indexMenu, String npcSay, String[] menuSelect, Object object) {
// dòng 97: public void openBaseMenu(Player player) {
// dòng 119: public void npcChat(Player player, String text) {
// dòng 132: public void npcChat(Zone zone, String text) {
// dòng 145: public void npcChat(String text) {
// dòng 160: public boolean canOpenNpc(Player player) {
```

### [NpcFactory.java](../../SRC/src/nro/models/npc/NpcFactory.java) — 743 dòng

Kiểu được nhận diện: NpcFactory.

```java
// dòng 104: public static Npc createNPC(int mapId, int status, int cx, int cy, int tempId) {
// dòng 235: public void openBaseMenu(Player player) {
// dòng 242: public void confirmMenu(Player player, int select) {
// dòng 255: public static void createNpcRongThieng() {
// dòng 258: public void confirmMenu(Player player, int select) {
// dòng 316: public static void createNpcConMeo() {
// dòng 319: public void confirmMenu(Player player, int select) {
```

## nro.models.npc_list (59 tệp)

### [Appule.java](../../SRC/src/nro/models/npc_list/Appule.java) — 62 dòng

Kiểu được nhận diện: Appule.

```java
// dòng 19: public Appule(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 24: public void openBaseMenu(Player player) {
// dòng 41: public void confirmMenu(Player player, int select) {
```

### [Babiday.java](../../SRC/src/nro/models/npc_list/Babiday.java) — 87 dòng

Kiểu được nhận diện: Babiday.

```java
// dòng 19: public Babiday(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 24: public void openBaseMenu(Player player) {
// dòng 53: public void confirmMenu(Player player, int select) {
```

### [BaHatMit.java](../../SRC/src/nro/models/npc_list/BaHatMit.java) — 457 dòng

Kiểu được nhận diện: BaHatMit.

```java
// dòng 33: public BaHatMit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 38: public void openBaseMenu(Player player) {
// dòng 132: public void confirmMenu(Player player, int select) {
```

### [Bardock.java](../../SRC/src/nro/models/npc_list/Bardock.java) — 40 dòng

Kiểu được nhận diện: Bardock.

```java
// dòng 15: public Bardock(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 20: public void openBaseMenu(Player player) {
// dòng 30: public void confirmMenu(Player player, int select) {
```

### [Bill.java](../../SRC/src/nro/models/npc_list/Bill.java) — 162 dòng

Kiểu được nhận diện: Bill.

```java
// dòng 29: public Bill(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 34: public void openBaseMenu(Player player) {
// dòng 52: public void confirmMenu(Player player, int select) {
// dòng 114: private void doiNhanhItem(Player player) {
```

### [BoMong.java](../../SRC/src/nro/models/npc_list/BoMong.java) — 108 dòng

Kiểu được nhận diện: BoMong.

```java
// dòng 25: public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 30: public void openBaseMenu(Player player) {
// dòng 42: public void confirmMenu(Player player, int select) {
```

### [Bulma.java](../../SRC/src/nro/models/npc_list/Bulma.java) — 62 dòng

Kiểu được nhận diện: Bulma.

```java
// dòng 19: public Bulma(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 24: public void openBaseMenu(Player player) {
// dòng 41: public void confirmMenu(Player player, int select) {
```

### [BulmaTuongLai.java](../../SRC/src/nro/models/npc_list/BulmaTuongLai.java) — 63 dòng

Kiểu được nhận diện: BulmaTuongLai.

```java
// dòng 22: public BulmaTuongLai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 27: public void openBaseMenu(Player player) {
// dòng 42: public void confirmMenu(Player player, int select) {
```

### [Calick.java](../../SRC/src/nro/models/npc_list/Calick.java) — 101 dòng

Kiểu được nhận diện: Calick.

```java
// dòng 26: public Calick(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 30: private void changeMap() {
// dòng 47: public void openBaseMenu(Player player) {
// dòng 74: public void confirmMenu(Player player, int select) {
```

### [Cargo.java](../../SRC/src/nro/models/npc_list/Cargo.java) — 52 dòng

Kiểu được nhận diện: Cargo.

```java
// dòng 18: public Cargo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 23: public void openBaseMenu(Player pl) {
// dòng 38: public void confirmMenu(Player player, int select) {
```

### [ChiChi.java](../../SRC/src/nro/models/npc_list/ChiChi.java) — 108 dòng

Kiểu được nhận diện: ChiChi.

```java
// dòng 23: public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 28: public void openBaseMenu(Player player) {
// dòng 46: public void confirmMenu(Player player, int select) {
```

### [Cui.java](../../SRC/src/nro/models/npc_list/Cui.java) — 191 dòng

Kiểu được nhận diện: Cui.

```java
// dòng 28: public Cui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 33: public void openBaseMenu(Player pl) {
// dòng 76: public void confirmMenu(Player player, int select) {
```

### [DaiThienSu.java](../../SRC/src/nro/models/npc_list/DaiThienSu.java) — 29 dòng

Kiểu được nhận diện: DaiThienSu.

```java
// dòng 14: public DaiThienSu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 19: public void openBaseMenu(Player player) {
// dòng 24: public void confirmMenu(Player player, int select) {
```

### [DauThan.java](../../SRC/src/nro/models/npc_list/DauThan.java) — 77 dòng

Kiểu được nhận diện: DauThan.

```java
// dòng 16: public DauThan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 28: public void confirmMenu(Player player, int select) {
```

### [Dende.java](../../SRC/src/nro/models/npc_list/Dende.java) — 104 dòng

Kiểu được nhận diện: Dende.

```java
// dòng 23: public Dende(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 28: public void openBaseMenu(Player player) {
// dòng 51: public void confirmMenu(Player player, int select) {
```

### [DocNhan.java](../../SRC/src/nro/models/npc_list/DocNhan.java) — 44 dòng

Kiểu được nhận diện: DocNhan.

```java
// dòng 16: public DocNhan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 42: public void confirmMenu(Player player, int select) {
```

### [DrDrief.java](../../SRC/src/nro/models/npc_list/DrDrief.java) — 225 dòng

Kiểu được nhận diện: DrDrief.

```java
// dòng 29: public DrDrief(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 34: public void openBaseMenu(Player pl) {
// dòng 68: public void confirmMenu(Player player, int select) {
```

### [DrMyuu.java](../../SRC/src/nro/models/npc_list/DrMyuu.java) — 41 dòng

Kiểu được nhận diện: DrMyuu.

```java
// dòng 14: public DrMyuu(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 19: public void openBaseMenu(Player player) {
// dòng 31: public void confirmMenu(Player player, int select) {
```

### [DuaHau.java](../../SRC/src/nro/models/npc_list/DuaHau.java) — 53 dòng

Kiểu được nhận diện: DuaHau.

```java
// dòng 24: public DuaHau(int mapId, int status, int cx, int cy, int tempId, int avatar) {
// dòng 29: public void openBaseMenu(Player player) {
// dòng 44: public void confirmMenu(Player player, int select) {
```

### [GhiDanh.java](../../SRC/src/nro/models/npc_list/GhiDanh.java) — 200 dòng

Kiểu được nhận diện: GhiDanh.

```java
// dòng 28: public GhiDanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 33: public void openBaseMenu(Player pl) {
// dòng 68: public void confirmMenu(Player player, int select) {
```

### [GiuMaDauBo.java](../../SRC/src/nro/models/npc_list/GiuMaDauBo.java) — 139 dòng

Kiểu được nhận diện: GiuMaDauBo.

```java
// dòng 26: public GiuMaDauBo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 31: public void openBaseMenu(Player player) {
// dòng 39: public void confirmMenu(Player player, int select) {
```

### [GokuSSJ.java](../../SRC/src/nro/models/npc_list/GokuSSJ.java) — 53 dòng

Kiểu được nhận diện: GokuSSJ.

```java
// dòng 16: public GokuSSJ(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 36: public void confirmMenu(Player player, int select) {
```

### [GokuSSJ2.java](../../SRC/src/nro/models/npc_list/GokuSSJ2.java) — 49 dòng

Kiểu được nhận diện: GokuSSJ2.

```java
// dòng 19: public GokuSSJ2(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 24: public void openBaseMenu(Player player) {
// dòng 32: public void confirmMenu(Player player, int select) {
```

### [Jaco.java](../../SRC/src/nro/models/npc_list/Jaco.java) — 60 dòng

Kiểu được nhận diện: Jaco.

```java
// dòng 16: public Jaco(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 37: public void confirmMenu(Player player, int select) {
```

### [Karin.java](../../SRC/src/nro/models/npc_list/Karin.java) — 195 dòng

Kiểu được nhận diện: Karin.

```java
// dòng 24: public Karin(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 29: public void openBaseMenu(Player player) {
// dòng 62: public void confirmMenu(Player player, int select) {
```

### [Kibit.java](../../SRC/src/nro/models/npc_list/Kibit.java) — 69 dòng

Kiểu được nhận diện: Kibit.

```java
// dòng 17: public Kibit(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 22: public void openBaseMenu(Player player) {
// dòng 46: public void confirmMenu(Player player, int select) {
```

### [KyGui.java](../../SRC/src/nro/models/npc_list/KyGui.java) — 40 dòng

Kiểu được nhận diện: KyGui.

```java
// dòng 10: public KyGui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 15: public void openBaseMenu(Player player) {
// dòng 24: public void confirmMenu(Player pl, int select) {
```

### [LinhCanh.java](../../SRC/src/nro/models/npc_list/LinhCanh.java) — 101 dòng

Kiểu được nhận diện: LinhCanh.

```java
// dòng 15: public LinhCanh(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 20: public void openBaseMenu(Player player) {
// dòng 83: public void confirmMenu(Player player, int select) {
```

### [LyTieuNuong.java](../../SRC/src/nro/models/npc_list/LyTieuNuong.java) — 391 dòng

Kiểu được nhận diện: LyTieuNuong.

```java
// dòng 15: public LyTieuNuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 20: public void openBaseMenu(Player player) {
// dòng 33: public void confirmMenu(Player player, int select) {
// dòng 131: private String getResult(String playerChoice, String machineChoice) {
// dòng 143: private void xửLýLựaChọnMiniGame_Gem(Player player) {
// dòng 181: private void xửLýLựaChọnMiniGame_Gold(Player player) {
// dòng 219: private void xửLýConSoMayManGem(Player player, int select) {
// dòng 241: private void xửLýConSoMayManGold(Player player, int select) {
// dòng 263: private void xửLýChonAiDay(Player player, int select, String time) {
// dòng 284: private void xửLýChonAiDayVang(Player player, int select, String time) {
// dòng 298: private void xửLýChonAiDayGem(Player player, int select, String time) {
// dòng 312: private void xửLýThuong1TrieuVang(Player player) {
// dòng 332: private void xửLýVIP10TrieuVang(Player player) {
// dòng 352: private void xửLýThuong10NgocXanh(Player player) {
// dòng 372: private void xửLýVIP100NgocXanh(Player player) {
```

### [MrPoPo.java](../../SRC/src/nro/models/npc_list/MrPoPo.java) — 98 dòng

Kiểu được nhận diện: MrPoPo.

```java
// dòng 17: public MrPoPo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 22: public void openBaseMenu(Player player) {
// dòng 37: public void confirmMenu(Player player, int select) {
```

### [OngGohan.java](../../SRC/src/nro/models/npc_list/OngGohan.java) — 46 dòng

Kiểu được nhận diện: OngGohan.

```java
// dòng 20: public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 25: public void openBaseMenu(Player player) {
// dòng 35: public void confirmMenu(Player player, int select) {
```

### [OngMoori.java](../../SRC/src/nro/models/npc_list/OngMoori.java) — 36 dòng

Kiểu được nhận diện: OngMoori.

```java
// dòng 10: public OngMoori(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 15: public void openBaseMenu(Player player) {
// dòng 25: public void confirmMenu(Player player, int select) {
```

### [OngParagus.java](../../SRC/src/nro/models/npc_list/OngParagus.java) — 36 dòng

Kiểu được nhận diện: OngParagus.

```java
// dòng 10: public OngParagus(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 15: public void openBaseMenu(Player player) {
// dòng 25: public void confirmMenu(Player player, int select) {
```

### [Osin.java](../../SRC/src/nro/models/npc_list/Osin.java) — 292 dòng

Kiểu được nhận diện: Osin.

```java
// dòng 22: public Osin(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 27: public void openBaseMenu(Player player) {
// dòng 113: public void confirmMenu(Player player, int select) {
```

### [Potage.java](../../SRC/src/nro/models/npc_list/Potage.java) — 57 dòng

Kiểu được nhận diện: Potage.

```java
// dòng 12: public Potage(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 17: public void openBaseMenu(Player player) {
// dòng 34: public void confirmMenu(Player player, int select) {
```

### [QuaTrung.java](../../SRC/src/nro/models/npc_list/QuaTrung.java) — 93 dòng

Kiểu được nhận diện: QuaTrung.

```java
// dòng 14: public QuaTrung(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 19: public void openBaseMenu(Player player) {
// dòng 38: public void confirmMenu(Player player, int select) {
```

### [QuocVuong.java](../../SRC/src/nro/models/npc_list/QuocVuong.java) — 111 dòng

Kiểu được nhận diện: QuocVuong.

```java
// dòng 16: public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 28: public void confirmMenu(Player player, int select) {
```

### [QuyLaoKame.java](../../SRC/src/nro/models/npc_list/QuyLaoKame.java) — 585 dòng

Kiểu được nhận diện: QuyLaoKame, RewardItem, KOLQuestData.

```java
// dòng 38: public RewardItem(int itemId, int quantity) {
// dòng 53: public KOLQuestData(int questType, int itemId, int requiredQuantity, List<RewardItem> rewards, String description) {
// dòng 86: public QuyLaoKame(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 91: public void openBaseMenu(Player player) {
// dòng 114: public void confirmMenu(Player player, int select) {
// dòng 155: private void handleBaseMenu(Player player, int select) {
// dòng 175: private void handleTalk(Player player) {
// dòng 205: private void handleTradeRuacon(Player player) {
// dòng 212: private void learnSkill(Player player) {
// dòng 229: private void handleMenu12(Player player, int select) {
// dòng 250: private void handleMenu0(Player player, int select) {
// dòng 277: private void handleSkillLearning(Player player) {
// dòng 295: private void handleClanMapChange(Player player) {
// dòng 303: private void handleClanDissolution(Player player) {
// dòng 309: private void handleTreasureMap(Player player) {
// dòng 317: private void handleMenu4(Player player, int select) {
// dòng 323: private void handleMenuOpenedDBKB(Player player, int select) {
// dòng 341: private void handleMenuOpenDBKB(Player player, int select) {
// dòng 359: private void handleMenuAcceptGoToBDKB(Player player, int select) {
// dòng 365: private void handleKOLQuest(Player player, boolean isVIP) {
// dòng 439: private void handleKOLQuestRewardConfirm(Player player, int select, boolean isVIP) {
// dòng 540: private List<Item.ItemOption> getPetBackpackOptions(int itemId) {
// dòng 573: private String formatRewardDetails(List<RewardItem> rewards) {
```

### [Rong1Sao.java](../../SRC/src/nro/models/npc_list/Rong1Sao.java) — 81 dòng

Kiểu được nhận diện: Rong1Sao.

```java
// dòng 13: public Rong1Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 18: public void openBaseMenu(Player player) {
// dòng 29: public void confirmMenu(Player player, int select) {
```

### [Rong2Sao.java](../../SRC/src/nro/models/npc_list/Rong2Sao.java) — 8 dòng

Kiểu được nhận diện: Rong2Sao.

```java
// dòng 4: public Rong2Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
```

### [Rong3Sao.java](../../SRC/src/nro/models/npc_list/Rong3Sao.java) — 8 dòng

Kiểu được nhận diện: Rong3Sao.

```java
// dòng 4: public Rong3Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
```

### [Rong4Sao.java](../../SRC/src/nro/models/npc_list/Rong4Sao.java) — 8 dòng

Kiểu được nhận diện: Rong4Sao.

```java
// dòng 4: public Rong4Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
```

### [Rong5Sao.java](../../SRC/src/nro/models/npc_list/Rong5Sao.java) — 8 dòng

Kiểu được nhận diện: Rong5Sao.

```java
// dòng 4: public Rong5Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
```

### [Rong6Sao.java](../../SRC/src/nro/models/npc_list/Rong6Sao.java) — 8 dòng

Kiểu được nhận diện: Rong6Sao.

```java
// dòng 4: public Rong6Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
```

### [Rong7Sao.java](../../SRC/src/nro/models/npc_list/Rong7Sao.java) — 8 dòng

Kiểu được nhận diện: Rong7Sao.

```java
// dòng 4: public Rong7Sao(int mapId, int status, int cx, int cy, int tempId, int avartar) {
```

### [RongOmega.java](../../SRC/src/nro/models/npc_list/RongOmega.java) — 120 dòng

Kiểu được nhận diện: RongOmega.

```java
// dòng 13: public RongOmega(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 18: public void openBaseMenu(Player player) {
// dòng 63: public void confirmMenu(Player player, int select) {
```

### [RuongDo.java](../../SRC/src/nro/models/npc_list/RuongDo.java) — 25 dòng

Kiểu được nhận diện: RuongDo.

```java
// dòng 8: public RuongDo(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 13: public void openBaseMenu(Player player) {
// dòng 20: public void confirmMenu(Player player, int select) {
```

### [Santa.java](../../SRC/src/nro/models/npc_list/Santa.java) — 111 dòng

Kiểu được nhận diện: Santa.

```java
// dòng 16: public Santa(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 50: public void confirmMenu(Player player, int select) {
```

### [Tapion.java](../../SRC/src/nro/models/npc_list/Tapion.java) — 48 dòng

Kiểu được nhận diện: Tapion.

```java
// dòng 13: public Tapion(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 18: public void openBaseMenu(Player player) {
// dòng 29: public void confirmMenu(Player player, int select) {
```

### [ThanVuTru.java](../../SRC/src/nro/models/npc_list/ThanVuTru.java) — 180 dòng

Kiểu được nhận diện: ThanVuTru.

```java
// dòng 17: public ThanVuTru(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 22: public void openBaseMenu(Player player) {
// dòng 44: public void confirmMenu(Player player, int select) {
```

### [ThuongDe.java](../../SRC/src/nro/models/npc_list/ThuongDe.java) — 167 dòng

Kiểu được nhận diện: ThuongDe.

```java
// dòng 17: public ThuongDe(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 22: public void openBaseMenu(Player player) {
// dòng 54: public void confirmMenu(Player player, int select) {
```

### [ToriBot.java](../../SRC/src/nro/models/npc_list/ToriBot.java) — 371 dòng

Kiểu được nhận diện: ToriBot.

```java
// dòng 23: public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 28: public void openBaseMenu(Player player) {
// dòng 66: public void confirmMenu(Player pl, int select) {
// dòng 109: private void BuyVip(Player pl, int vipLevel, int cost) {
// dòng 150: private void VIP1(Player pl) {
// dòng 199: private void VIP2(Player pl) {
// dòng 253: private void VIP3(Player pl) {
// dòng 310: private void VIP4(Player pl) {
```

### [ToSuKaio.java](../../SRC/src/nro/models/npc_list/ToSuKaio.java) — 170 dòng

Kiểu được nhận diện: ToSuKaio.

```java
// dòng 16: public ToSuKaio(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 21: public void openBaseMenu(Player player) {
// dòng 34: public void confirmMenu(Player player, int select) {
// dòng 68: private void showLimitPowerMenu(Player player) {
// dòng 74: private void showLimitPowerMyself(Player player) {
// dòng 94: private void handleLimitPowerMyselfOptions(Player player, int select) {
// dòng 117: private void handleLimitPowerPet(Player player, int select) {
// dòng 144: private void handleAutoTrainingMenu(Player player) {
// dòng 153: private void showAutoTrainingRegistrationMenu(Player player) {
// dòng 159: private void handleAutoTrainingRegistration(Player player, int select) {
```

### [TrongTai.java](../../SRC/src/nro/models/npc_list/TrongTai.java) — 55 dòng

Kiểu được nhận diện: TrongTai.

```java
// dòng 11: public TrongTai(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 16: public void openBaseMenu(Player player) {
// dòng 35: public void confirmMenu(Player player, int select) {
```

### [TruongLaoGuru.java](../../SRC/src/nro/models/npc_list/TruongLaoGuru.java) — 138 dòng

Kiểu được nhận diện: TruongLaoGuru.

```java
// dòng 27: public TruongLaoGuru(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 32: public void openBaseMenu(Player player) {
// dòng 51: public void confirmMenu(Player player, int select) {
// dòng 61: private void handleBaseMenu(Player player, int select) {
// dòng 70: private void handleSkillLearningMenu(Player player) {
// dòng 88: private void handleSkillLearning(Player player, int select) {
// dòng 109: private void learnSkill(Player player) {
```

### [Uron.java](../../SRC/src/nro/models/npc_list/Uron.java) — 25 dòng

Kiểu được nhận diện: Uron.

```java
// dòng 8: public Uron(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 13: public void openBaseMenu(Player pl) {
// dòng 20: public void confirmMenu(Player player, int select) {
```

### [Vados.java](../../SRC/src/nro/models/npc_list/Vados.java) — 14 dòng

Kiểu được nhận diện: Vados.

```java
// dòng 7: public Vados(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 12: public void confirmMenu(Player player, int select) {
```

### [VuaVegeta.java](../../SRC/src/nro/models/npc_list/VuaVegeta.java) — 137 dòng

Kiểu được nhận diện: VuaVegeta.

```java
// dòng 26: public VuaVegeta(int mapId, int status, int cx, int cy, int tempId, int avartar) {
// dòng 31: public void openBaseMenu(Player player) {
// dòng 50: public void confirmMenu(Player player, int select) {
// dòng 60: private void handleBaseMenu(Player player, int select) {
// dòng 69: private void handleSkillLearningMenu(Player player) {
// dòng 87: private void handleSkillLearning(Player player, int select) {
// dòng 108: private void learnSkill(Player player) {
```

### [Whis.java](../../SRC/src/nro/models/npc_list/Whis.java) — 262 dòng

Kiểu được nhận diện: Whis.

```java
// dòng 26: public Whis(int mapId, int status, int cx, int cy, int tempId, int avatar) {
// dòng 31: public void openBaseMenu(Player player) {
// dòng 50: public void confirmMenu(Player player, int select) {
// dòng 72: private void handleBaseMenu(Player player, int select) {
// dòng 94: private void handleCheTaoTrangBiThienSu(Player player, int select) {
// dòng 109: private void showSkillLearningMenu(Player player, Item biKiep) {
// dòng 146: private void handleHocTuyetKy(Player player, int select) {
// dòng 190: private void learnNewSkill(Player player, Item sach, int skillId, int iconSkill) {
// dòng 216: private void upgradeSkill(Player player, Item sach, Skill currentSkill) {
// dòng 240: private void sendLearnSkillEffect(Player player, int iconSkill, int usedBk) throws IOException {
```

## nro.models.player (27 tệp)

### [Achievement.java](../../SRC/src/nro/models/player/Achievement.java) — 98 dòng

Kiểu được nhận diện: Achievement.

```java
// dòng 24: public Achievement(Player player) {
// dòng 29: public void add(AchievementQuest achievement) {
// dòng 33: public AchievementQuest get(int index) {
// dòng 37: public long getCompleted(int index) {
// dòng 56: public boolean isFinish(int index, long maxCount) {
// dòng 61: public boolean isRecieve(int index) {
// dòng 66: public boolean canReward(int index) {
// dòng 72: public void done(int index, long completed) {
// dòng 78: public void doneNotAdd(int index, long completed) {
// dòng 84: public void reward(int index) {
// dòng 90: public void dispose() {
```

### [Charms.java](../../SRC/src/nro/models/player/Charms.java) — 147 dòng

Kiểu được nhận diện: Charms.

```java
// dòng 12: public Charms(Player player) {
// dòng 35: public void addTimeCharms(int itemId, int min) {
// dòng 143: public void dispose() {
```

### [DropItem.java](../../SRC/src/nro/models/player/DropItem.java) — 43 dòng

Kiểu được nhận diện: DropItem.

```java
// dòng 21: public DropItem(Player player) {
// dòng 25: public void update() {
// dòng 40: public void dispose() {
```

### [EffectSkill.java](../../SRC/src/nro/models/player/EffectSkill.java) — 263 dòng

Kiểu được nhận diện: EffectSkill.

```java
// dòng 140: public EffectSkill(Player player) {
// dòng 144: public void removeSkillEffectWhenDie() {
// dòng 187: public void update() {
// dòng 252: public boolean isHaveEffectSkill() {
// dòng 256: public void dispose() {
```

### [EffectSkin.java](../../SRC/src/nro/models/player/EffectSkin.java) — 513 dòng

Kiểu được nhận diện: EffectSkin.

```java
// dòng 46: public EffectSkin(Player player) {
// dòng 83: public void update() {
// dòng 111: private void updateCTHaiTac() {
// dòng 154: private void updateXenHutXungQuanh() {
// dòng 211: private void updateOdo() {
// dòng 242: private void updateXinbato() {
// dòng 310: private void updateThoBulma() {
// dòng 336: private void updateTanHinh() {
// dòng 349: private void updateHoaDa() {
// dòng 370: private void updateLamCham() {
// dòng 393: private void updateXChuong() {
// dòng 406: private void updateMaPhongBa() {
// dòng 446: private void updateTrainArmor() {
// dòng 498: private void updateVoHinh() {
// dòng 510: public void dispose() {
```

### [Enemy.java](../../SRC/src/nro/models/player/Enemy.java) — 9 dòng

Kiểu được nhận diện: Enemy.

### [FightMabu.java](../../SRC/src/nro/models/player/FightMabu.java) — 50 dòng

Kiểu được nhận diện: FightMabu.

```java
// dòng 22: public FightMabu(Player player) {
// dòng 26: public void changePoint(byte pointAdd) {
// dòng 37: public void changePercentPoint(byte pointAdd) {
// dòng 47: public void clear() {
```

### [Friend.java](../../SRC/src/nro/models/player/Friend.java) — 20 dòng

Kiểu được nhận diện: Friend.

### [Fusion.java](../../SRC/src/nro/models/player/Fusion.java) — 35 dòng

Kiểu được nhận diện: Fusion.

```java
// dòng 21: public Fusion(Player player) {
// dòng 25: public void update() {
// dòng 31: public void dispose(){
```

### [GiftCode.java](../../SRC/src/nro/models/player/GiftCode.java) — 35 dòng

Kiểu được nhận diện: GiftCode.

```java
// dòng 16: public GiftCode() {
// dòng 20: public void add(String code) {
// dòng 24: public boolean isUsedGiftCode(String code) {
// dòng 28: public void dispose() {
```

### [IDMark.java](../../SRC/src/nro/models/player/IDMark.java) — 150 dòng

Kiểu được nhận diện: IDMark.

```java
// dòng 133: public boolean isBaseMenu() {
// dòng 137: public void dispose() {
```

### [Inventory.java](../../SRC/src/nro/models/player/Inventory.java) — 151 dòng

Kiểu được nhận diện: Inventory.

```java
// dòng 39: public Inventory() {
// dòng 48: public int getGem() {
// dòng 52: public int getParam(Item it, int id) {
// dòng 61: public boolean haveOption(List<Item> l, int index, int id) {
// dòng 69: public void subGem(int num) {
// dòng 73: public void subGold(int num) {
// dòng 77: public void addGold(int gold) {
// dòng 84: public void dispose() {
// dòng 126: public void checkAndUpdateMeRongBadges(Player player) {
// dòng 148: private boolean isPermanent(Item item) {
```

### [ItemEvent.java](../../SRC/src/nro/models/player/ItemEvent.java) — 150 dòng

Kiểu được nhận diện: ItemEvent.

```java
// dòng 39: public ItemEvent(Player player) {
// dòng 43: public boolean canDropTatVoGiangSinh(int maxCount) {
// dòng 55: public boolean canDropRemainingChuongDongCount(int maxCount) {
// dòng 67: public boolean canDropRemainingBanhQuyCount(int maxCount) {
// dòng 79: public boolean canDropRemainingCaTuyetCount(int maxCount) {
// dòng 91: public boolean canDropRemainingKeoDuongCount(int maxCount) {
// dòng 103: public boolean canDropRemainingKeoNguoiTuyetCount(int maxCount) {
// dòng 115: public boolean canDropHoaHong(int maxCount) {
// dòng 127: public boolean canDropBinhNuoc(int maxCount) {
// dòng 139: public boolean canDropManhVo(int maxCount) {
```

### [KOLProgressData.java](../../SRC/src/nro/models/player/KOLProgressData.java) — 26 dòng

Kiểu được nhận diện: KOLProgressData.

```java
// dòng 17: public KOLProgressData() {
```

### [LearnSkill.java](../../SRC/src/nro/models/player/LearnSkill.java) — 19 dòng

Kiểu được nhận diện: LearnSkill.

```java
// dòng 13: public LearnSkill()
```

### [Location.java](../../SRC/src/nro/models/player/Location.java) — 15 dòng

Kiểu được nhận diện: Location.

### [NewPet.java](../../SRC/src/nro/models/player/NewPet.java) — 118 dòng

Kiểu được nhận diện: NewPet.

```java
// dòng 22: public NewPet(Player master, short h, short b, short l) {
// dòng 34: public short getHead() {
// dòng 39: public short getBody() {
// dòng 44: public short getLeg() {
// dòng 48: public void joinMapMaster() {
// dòng 66: private void moveIdle() {
// dòng 77: public void update() {
// dòng 91: public void followMaster() {
// dòng 95: private void followMaster(int dis) {
// dòng 111: public void dispose() {
```

### [NewSkill.java](../../SRC/src/nro/models/player/NewSkill.java) — 182 dòng

Kiểu được nhận diện: NewSkill.

```java
// dòng 25: public NewSkill(Player player) {
// dòng 57: public byte getTypePaint() {
// dòng 74: public byte getTypeItem() {
// dòng 88: private void update() {
// dòng 94: public void setSkillSpecial(byte dir, short _xPlayer, short _yPlayer, short _xObjTaget, short _yObjTaget) {
// dòng 128: public void sonPhiPhai() {
// dòng 134: public void closeSkillSpecial() {
// dòng 146: private void close() {
// dòng 159: public void start(int leep) {
// dòng 165: public void run() {
// dòng 177: public void dispose() {
```

### [NPoint.java](../../SRC/src/nro/models/player/NPoint.java) — 2,040 dòng

Kiểu được nhận diện: NPoint.

```java
// dòng 43: public NPoint(Player player) {
// dòng 213: public void calPoint() {
// dòng 220: public int getRealTlNeDon() {
// dòng 228: private void setPointWhenWearClothes() {
// dòng 472: private void addOption(ItemOption io) {
// dòng 652: private void setSpeed() {
// dòng 658: private void setOutfitFusion() {
// dòng 671: private void setDameTrainArmor() {
// dòng 705: public void setBasePoint() {
// dòng 720: private void setLtdb() {
// dòng 724: private void setThoBulma() {
// dòng 730: private void setTinhNhatNguyetAn() {
// dòng 736: private void setHpHoi() {
// dòng 757: private void setMpHoi() {
// dòng 778: private void setHpMax() {
// dòng 923: private void setHp() {
// dòng 927: private void setMpMax() {
// dòng 1029: private void setMp() {
// dòng 1033: public int getHP() {
// dòng 1037: public void setHP(long hp) {
// dòng 1045: public int getMP() {
// dòng 1049: public void setMP(long mp) {
// dòng 1057: private void setDame() {
// dòng 1208: public void setDame(long dame) {
// dòng 1216: private void setDef() {
// dòng 1225: private void setCrit() {
// dòng 1242: private void resetPoint() {
// dòng 1299: public void addHp(long hp) {
// dòng 1310: public void addMp(long mp) {
// dòng 1322: public void setHp(long hp) {
// dòng 1330: public void setMp(long mp) {
// dòng 1338: private void setIsCrit() {
// dòng 1350: public int getDameAttack(boolean isAttackMob) {
// dòng 1541: public int getCurrPercentHP() {
// dòng 1548: public int getCurrPercentMP() {
// dòng 1552: public void setFullHpMp() {
// dòng 1557: public void subHP(long sub) {
// dòng 1565: public void subMP(long sub) {
// dòng 1576: public long calSucManhTiemNang(long tiemNang) {
// dòng 1660: public long calSubTNSM(long tiemNang) {
// dòng 1675: public short getTileHutHp(boolean isMob) {
// dòng 1683: public short getTiLeHutMp() {
// dòng 1687: public int subDameInjureWithDeff(long dame) {
// dòng 1697: public boolean canOpenPower() {
// dòng 1701: public long getPowerLimit() {
// dòng 1728: public long getPowerNextLimit() {
// dòng 1755: public int getHpMpLimit() {
// dòng 1782: public int getDameLimit() {
// dòng 1809: public short getDefLimit() {
// dòng 1836: public byte getCritLimit() {
// dòng 1863: public void powerUp(long power) {
// dòng 1868: public void tiemNangUp(long tiemNang) {
// dòng 1872: public void increasePoint(byte type, short point) {
// dòng 1942: private boolean doUseTiemNang(long tiemNang) {
// dòng 1955: public long getFullTN() {
// dòng 1980: public void update() {
// dòng 2025: public void dispose() {
// dòng 2036: public long calPercent(long param, int percent) {
```

### [Pet.java](../../SRC/src/nro/models/player/Pet.java) — 1,177 dòng

Kiểu được nhận diện: Pet.

```java
// dòng 65: public Pet(Player master) {
// dòng 70: public void changeStatus(byte status) {
// dòng 84: public void joinMapMaster() {
// dòng 97: public void goHome() {
// dòng 120: private String getTextStatus(byte status) {
// dòng 153: public void fusionGogeta(boolean porata) {
// dòng 189: public void fusion2(boolean porata) {
// dòng 210: public void fusion3(boolean porata) {
// dòng 232: public void fusion(boolean porata) {
// dòng 258: public void unFusion() {
// dòng 269: private void fusionEffect(int type) {
// dòng 286: private void moveIdle() {
// dòng 300: private void masterDoesNotAttack() {
// dòng 314: public void update() {
// dòng 573: public void askPea() {
// dòng 598: private boolean useSkill3() {
// dòng 666: private boolean useSkill4() {
// dòng 699: private boolean useSkill5() {
// dòng 758: private void increasePoint() {
// dòng 767: public void followMaster() {
// dòng 783: private void followMaster(int dis) {
// dòng 798: public short getAvatar() {
// dòng 814: public short getHead() {
// dòng 850: public short getBody() {
// dòng 889: public short getLeg() {
// dòng 928: private Player findPlayerAttack() {
// dòng 947: private boolean cantAttack(Player player) {
// dòng 954: private Mob findMobAttack() {
// dòng 970: private void updatePower() {
// dòng 997: public void openSkill2() {
// dòng 1036: public void openSkill3() {
// dòng 1060: public void openSkill4() {
// dòng 1077: public void openSkill5() {
// dòng 1097: private Skill getSkill(int indexSkill) {
// dòng 1101: public void transform() {
// dòng 1126: public boolean canAttack() {
// dòng 1137: public void petSay(Player player) {
// dòng 1168: public void dispose() {
```

### [Player.java](../../SRC/src/nro/models/player/Player.java) — 1,901 dòng

Kiểu được nhận diện: Player.

```java
// dòng 358: public Player() {
// dòng 393: public boolean isDie() {
// dòng 408: public void sendMessage(Message msg) {
// dòng 414: public boolean isPl() {
// dòng 419: public void run() {
// dòng 428: public void start() {
// dòng 432: public void update() {
// dòng 620: public void autoSendBadges() {
// dòng 676: public String percentGold(int type) {
// dòng 701: public String percentGem(int type) {
// dòng 728: public int getHat() {
// dòng 732: public byte getAura() {
// dòng 747: public byte getEffFront() {
// dòng 824: public short getHead() {
// dòng 878: public short getBody() {
// dòng 938: public short getLeg() {
// dòng 998: public short getFlagBag() {
// dòng 1023: public short getMount() {
// dòng 1047: public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
// dòng 1223: public void setTemporaryEnemies(Player pl) {
// dòng 1229: protected void setBom(Player plAtt) {
// dòng 1233: public void setDie() {
// dòng 1237: protected void setDie(Player plAtt) {
// dòng 1306: public void setClanMember() {
// dòng 1315: public boolean isAdmin() {
// dòng 1319: public void setJustRevivaled() {
// dòng 1324: public boolean isActive() {
// dòng 1328: public void sendNewPet() {
// dòng 1614: private void fixBlackBallWar() {
// dòng 1639: public void move(int _toX, int _toY) {
// dòng 1649: public long getDuaHauStartTime() {
// dòng 1653: public void setDuaHauStartTime(long duaHauStartTime) {
// dòng 1657: public void dispose() {
// dòng 1832: public String getLastChatMessage() {
// dòng 1836: public void setLastChatMessage(String message) {
// dòng 1840: public boolean hasEffect(Player player, int effectId) {
// dòng 1845: public void spreadEffectToNearbyPlayers() {
// dòng 1872: public void sendTextTimeDaiLyGift() {
// dòng 1881: public Player(String name) {
// dòng 1886: public void setName(String newName) {
// dòng 1890: public void resetName() {
// dòng 1897: public boolean isMabuPet(Player player) {
```

### [PlayerEvent.java](../../SRC/src/nro/models/player/PlayerEvent.java) — 34 dòng

Kiểu được nhận diện: PlayerEvent.

```java
// dòng 18: public PlayerEvent(Player player) {
// dòng 22: public void addEventPoint(int num) {
// dòng 26: public void subEventPoint(int num) {
// dòng 30: public void update() {
```

### [RewardBlackBall.java](../../SRC/src/nro/models/player/RewardBlackBall.java) — 92 dòng

Kiểu được nhận diện: RewardBlackBall.

```java
// dòng 41: public RewardBlackBall(Player player) {
// dòng 49: public void reward(byte star) {
// dòng 57: public void getRewardSelect(byte select) {
// dòng 70: private void getReward(int star) {
// dòng 89: public void dispose() {
```

### [Satellite.java](../../SRC/src/nro/models/player/Satellite.java) — 36 dòng

Kiểu được nhận diện: Satellite.

```java
// dòng 22: public void update() {
```

### [SetClothes.java](../../SRC/src/nro/models/player/SetClothes.java) — 239 dòng

Kiểu được nhận diện: SetClothes.

```java
// dòng 16: public SetClothes(Player player) {
// dòng 44: public void setup() {
// dòng 80: private void setupSKT() {
// dòng 182: private void setDefault() {
// dòng 204: public boolean checkSetGod() {
// dòng 221: public boolean checkSetDes() {
// dòng 236: public void dispose() {
```

### [SuperRank.java](../../SRC/src/nro/models/player/SuperRank.java) — 66 dòng

Kiểu được nhận diện: SuperRank.

```java
// dòng 29: public SuperRank(Player player) {
// dòng 35: public void history(String text, long lastTime) {
// dòng 44: public void reward() {
// dòng 58: public void dispose() {
```

### [Traning.java](../../SRC/src/nro/models/player/Traning.java) — 38 dòng

Kiểu được nhận diện: Traning.

## nro.models.player_badges (4 tệp)

### [Badges.java](../../SRC/src/nro/models/player_badges/Badges.java) — 14 dòng

Kiểu được nhận diện: Badges.

### [BadgesData.java](../../SRC/src/nro/models/player_badges/BadgesData.java) — 46 dòng

Kiểu được nhận diện: BadgesData.

```java
// dòng 17: public BadgesData() {
// dòng 23: public BadgesData(int id, long time, boolean isuse) {
// dòng 29: public BadgesData(Player player, int id, int days) {
// dòng 42: public String toString() {
```

### [BadgesService.java](../../SRC/src/nro/models/player_badges/BadgesService.java) — 25 dòng

Kiểu được nhận diện: BadgesService.

```java
// dòng 13: public static void turnOnBadges(Player player, int id) {
```

### [BagesTemplate.java](../../SRC/src/nro/models/player_badges/BagesTemplate.java) — 75 dòng

Kiểu được nhận diện: BagesTemplate.

```java
// dòng 24: public static int findIdItemByIdIdEffect(int idEffect) {
// dòng 33: public static int fineIdEffectbyIdItem(int idItem) {
// dòng 42: public static BagesTemplate fineBadgesbyIdItem(int idItem) {
// dòng 51: public static List<Integer> listEffect(Player player) {
// dòng 63: public static List<Item.ItemOption> sendListItemOption(Player player) {
```

## nro.models.player_system (3 tệp)

### [AntiLogin.java](../../SRC/src/nro/models/player_system/AntiLogin.java) — 56 dòng

Kiểu được nhận diện: AntiLogin.

```java
// dòng 16: public boolean canLogin() {
// dòng 26: public void wrong() {
// dòng 38: public void reset() {
// dòng 45: public String getNotifyCannotLogin() {
```

### [GiftCode.java](../../SRC/src/nro/models/player_system/GiftCode.java) — 32 dòng

Kiểu được nhận diện: GiftCode.

```java
// dòng 25: public boolean isUsedGiftCode(Player player) {
// dòng 29: public boolean timeCode() {
```

### [Template.java](../../SRC/src/nro/models/player_system/Template.java) — 288 dòng

Kiểu được nhận diện: Template, ItemOptionTemplate, ItemTemplate, MobTemplate, NpcTemplate, MapTemplate, SkillTemplate, Part, PartDetail, HeadAvatar, FlagBag, BgItem, ArrHead2Frames, TileSetInfo, TileSetInfoData, WaitSuperRank, AchievementTemplate, AchievementQuest.

```java
// dòng 24: public ItemOptionTemplate() {
// dòng 27: public ItemOptionTemplate(int id, String name, int type) {
// dòng 68: public ItemTemplate() {
// dòng 71: public ItemTemplate(short id, byte type, byte gender, String name, String description, short iconID, short part, boolean isUpToUp, int strRequire) {
// dòng 132: public MapTemplate() {
// dòng 170: public Part() {
// dòng 183: public PartDetail(int iconId, byte dx, byte dy) {
// dòng 196: public HeadAvatar(int headId, int avatarId) {
// dòng 235: public TileSetInfo(List<TileSetInfoData> tileSetInfoData) {
// dòng 245: public TileSetInfoData(int type, int[] data) {
// dòng 256: public WaitSuperRank(long playerId, long rivalId) {
// dòng 269: public AchievementTemplate(String info1, String info2, int money, long maxCount) {
// dòng 282: public AchievementQuest(long completed, boolean isRecieve) {
```

## nro.models.radar (3 tệp)

### [Card.java](../../SRC/src/nro/models/radar/Card.java) — 64 dòng

Kiểu được nhận diện: Card.

```java
// dòng 21: public Card() {
// dòng 30: public Card(byte m, List<OptionCard> o) {
// dòng 35: public Card(short i, byte a, byte ma, byte le, List<OptionCard> o) {
// dòng 43: public Card(short i, byte a, byte ma, byte le, List<OptionCard> o, byte u) {
// dòng 53: public String toString() {
```

### [OptionCard.java](../../SRC/src/nro/models/radar/OptionCard.java) — 30 dòng

Kiểu được nhận diện: OptionCard.

```java
// dòng 15: public OptionCard(int i, int p, byte a) {
// dòng 22: public String toString() {
```

### [RadarCard.java](../../SRC/src/nro/models/radar/RadarCard.java) — 45 dòng

Kiểu được nhận diện: RadarCard.

```java
// dòng 31: public RadarCard() {
```

## nro.models.server (11 tệp)

### [AutoMaintenance.java](../../SRC/src/nro/models/server/AutoMaintenance.java) — 94 dòng

Kiểu được nhận diện: AutoMaintenance.

```java
// dòng 23: public static AutoMaintenance gI() {
// dòng 31: public void run() {
```

### [AutoUpdate_NhanQuaFree.java](../../SRC/src/nro/models/server/AutoUpdate_NhanQuaFree.java) — 52 dòng

Kiểu được nhận diện: AutoUpdate_NhanQuaFree.

```java
// dòng 24: public void start() {
// dòng 37: public void run() {
// dòng 45: public void execute() {
```

### [Client.java](../../SRC/src/nro/models/server/Client.java) — 199 dòng

Kiểu được nhận diện: Client.

```java
// dòng 42: private Client() {
// dòng 46: public static Client gI() {
// dòng 53: public void put(Player player) {
// dòng 69: private void remove(MySession session) {
// dòng 85: private void remove(Player player) {
// dòng 129: public void kickSession(MySession session) {
// dòng 136: public Player getPlayer(long playerId) {
// dòng 140: public Player getPlayerByUser(int userId) {
// dòng 144: public Player getPlayer(String name) {
// dòng 148: public void close() {
// dòng 159: private void update() {
// dòng 176: public Player getPlayerByID(int playerId) {
// dòng 187: public void run() {
```

### [Command.java](../../SRC/src/nro/models/server/Command.java) — 314 dòng

Kiểu được nhận diện: Command.

```java
// dòng 43: public static Command gI() {
// dòng 50: private Command() {
// dòng 55: private void initAdminCommands() {
// dòng 70: private void initParameterizedCommands() {
// dòng 245: public void chat(Player player, String text) {
// dòng 255: public boolean check(Player player, String text) {
```

### [Controller.java](../../SRC/src/nro/models/server/Controller.java) — 942 dòng

Kiểu được nhận diện: Controller.

```java
// dòng 68: public static Controller gI() {
// dòng 76: public void onMessage(ISession s, Message _msg) {
// dòng 719: public void messageNotLogin(MySession session, Message msg) {
// dòng 740: public void messageNotMap(MySession _session, Message _msg) {
// dòng 821: public void messageSubCommand(MySession _session, Message _msg) {
// dòng 849: private void sendThongBaoServer(Player player) {
// dòng 857: public void createChar(MySession session, Message msg) {
// dòng 902: public void login2(MySession session, Message msg) {
// dòng 906: public void sendInfo(MySession session) {
// dòng 937: public void finishUpdate(Player player) {
```

### [Maintenance.java](../../SRC/src/nro/models/server/Maintenance.java) — 75 dòng

Kiểu được nhận diện: Maintenance.

```java
// dòng 18: private Maintenance() {
// dòng 21: public static Maintenance gI() {
// dòng 28: public void startCountdown() {
// dòng 36: public void startSeconds(int seconds) {
// dòng 44: public void startImmediately() {
// dòng 53: public void run() {
// dòng 70: private void sendRemainingTime() {
```

### [Manager.java](../../SRC/src/nro/models/server/Manager.java) — 1,168 dòng

Kiểu được nhận diện: Manager, MapBgDataManager.

```java
// dòng 127: public static Manager gI() {
// dòng 134: public static boolean hasNewTopScores() {
// dòng 138: public static void resetTopFlags() {
// dòng 150: public static byte[] getBgItem(int mapId) {
// dòng 161: public static boolean isExactly11AM() {
// dòng 168: private Manager() {
// dòng 183: private void initMap() {
// dòng 244: public static void loadPart() {
// dòng 288: private void loadDatabase() {
// dòng 979: public static List<TOP> realTop(String query, Connection con) {
// dòng 1028: public void loadProperties() throws IOException {
// dòng 1081: private int[][] readTileIndexTileType(int tileTypeFocus) {
// dòng 1114: private int[][] readTileMap(int mapId) {
// dòng 1132: public static Clan getClanById(int id) throws Exception {
// dòng 1141: public static void addClan(Clan clan) {
// dòng 1145: public static int getNumClan() {
// dòng 1150: public static MobTemplate getMobTemplateByTemp(int mobTempId) {
// dòng 1159: public static byte getNFrameImageByName(String name) {
```

### [MenuController.java](../../SRC/src/nro/models/server/MenuController.java) — 70 dòng

Kiểu được nhận diện: MenuController.

```java
// dòng 23: public static MenuController gI() {
// dòng 30: public void openMenuNPC(MySession session, int idnpc, Player player) {
// dòng 47: public void doSelectMenu(Player player, int npcId, int select) throws IOException {
```

### [ServerLog.java](../../SRC/src/nro/models/server/ServerLog.java) — 71 dòng

Kiểu được nhận diện: ServerLog.

```java
// dòng 20: public static void logCombine(String name, String itemname, int param) {
// dòng 36: public static void logItemDrop(String name, String item) {
// dòng 52: public static String toTimeString(Date date) {
// dòng 61: public static void logAdmin(String name, int quantity) {
```

### [ServerManager.java](../../SRC/src/nro/models/server/ServerManager.java) — 333 dòng

Kiểu được nhận diện: ServerManager.

```java
// dòng 68: public void init() {
// dòng 74: public static ServerManager gI() {
// dòng 82: public static void main(String[] args) {
// dòng 100: public static void logException(Class<?> clazz, Exception e) {
// dòng 105: public void run() {
// dòng 148: private void startTopUpdater() {
// dòng 158: private boolean shouldUpdateTop() {
// dòng 166: private void stopTopUpdater() {
// dòng 173: private void updateTop() {
// dòng 197: public void activeServerSocket() {
// dòng 201: public void sessionInit(ISession is) {
// dòng 213: public void sessionDisconnect(ISession session) {
// dòng 228: private boolean canConnectWithIp(String ipAddress) {
// dòng 245: public void disconnect(MySession session) {
// dòng 257: public void resetNhanQuaHangNgay() {
// dòng 277: public void close() {
// dòng 300: private static void activeCommandLine() {
```

### [ServerNotify.java](../../SRC/src/nro/models/server/ServerNotify.java) — 96 dòng

Kiểu được nhận diện: ServerNotify.

```java
// dòng 32: private ServerNotify() {
// dòng 37: public static ServerNotify gI() {
// dòng 45: public void run() {
// dòng 65: private void ThongBao(String text) {
// dòng 76: public void notify(String text) {
// dòng 80: public void sendNotifyTab(Player player) {
```

## nro.models.services (22 tệp)

### [AchievementService.java](../../SRC/src/nro/models/services/AchievementService.java) — 138 dòng

Kiểu được nhận diện: AchievementService.

```java
// dòng 21: public static AchievementService gI() {
// dòng 28: public void openAchievementUI(Player player) {
// dòng 51: public void confirmAchievement(Player player, byte select) {
// dòng 80: public String numberToString(long num) {
// dòng 84: public String regex(Player player, String text) {
// dòng 89: public void checkDoneTask(Player player, int aId) {
// dòng 100: public void checkDoneTaskKillMob(Player player, Mob mob) {
// dòng 115: public void checkDoneTaskUseSkill(Player player) {
// dòng 129: public void checkDoneTaskFly(Player player, int length) {
```

### [ChatGlobalService.java](../../SRC/src/nro/models/services/ChatGlobalService.java) — 209 dòng

Kiểu được nhận diện: ChatGlobalService, ChatGlobal.

```java
// dòng 30: private ChatGlobalService() {
// dòng 36: private void start() {
// dòng 40: public static ChatGlobalService gI() {
// dòng 47: public void chat1(Player player, String text) {
// dòng 52: public void ThongBaoRoiDo(Player player, String text) {
// dòng 56: public void ThongBaoDapDo(Player player, String text) {
// dòng 60: public void chatVip(Player player, String text) {
// dòng 64: public void chat(Player player, String text) {
// dòng 99: public void run() {
// dòng 125: private void chatGlobal(ChatGlobal chat) {
// dòng 144: public void chat(Player pl, SystemColor text) {
// dòng 188: public ChatGlobal(Player player, String text) {
// dòng 204: private void dispose() {
```

### [ClanService.java](../../SRC/src/nro/models/services/ClanService.java) — 1,070 dòng

Kiểu được nhận diện: ClanService.

```java
// dòng 66: private ClanService() {
// dòng 69: public static ClanService gI() {
// dòng 76: public Clan getClanById(int id) throws Exception {
// dòng 80: private Clan getClanById(int l, int r, int id) throws Exception {
// dòng 102: public List<Clan> getClans(String name) {
// dòng 126: public void getClan(Player player, Message msg) {
// dòng 153: public void clanMessage(Player player, Message msg) {
// dòng 173: public void clanDonate(Player plGive, Message msg) {
// dòng 219: public void joinClan(Player player, Message msg) {
// dòng 236: public void clanRemote(Player player, Message msg) {
// dòng 259: public void clanInvite(Player player, Message msg) {
// dòng 279: private void sendInviteClan(Player player, int playerId) {
// dòng 315: private void acceptJoinClan(Player player, int clanId) {
// dòng 353: private void acceptAskJoinClan(Player player, int clanMessageId) {
// dòng 422: private void cancelAskJoinClan(Player player, int clanMessageId) {
// dòng 450: private void askForPea(Player player) {
// dòng 476: private void askForJoinClan(Player player, int clanId) {
// dòng 526: private void changeInfoClan(Player player, byte imgId, String slogan) {
// dòng 537: private void createClan(Player player, byte imgId, String name) {
// dòng 583: public void sendListClan(Player player, String name) {
// dòng 607: public void sendListMemberClan(Player player, int clanId) {
// dòng 642: public void sendMyClan(Player player) {
// dòng 710: public void sendClanId(Player player) {
// dòng 726: public void showMenuLeaveClan(Player player) {
// dòng 732: public void showMenuNhuongPc(Player player, int playerId) {
// dòng 742: public void changeSlogan(Player player, String slogan) {
// dòng 754: public void changeFlag(Player player, int imgId) {
// dòng 788: public void leaveClan(Player player) {
// dòng 825: public void catChuc(Player player, int memberId) {
// dòng 851: public void kickOut(Player player, int memberId) {
// dòng 887: private void removeClanPlayer(int plId) {
// dòng 904: public void phongPho(Player player, int memberId) {
// dòng 929: public void phongPc(Player player, int memberId) {
// dòng 957: public void chat(Player player, String text) {
// dòng 975: private void checkDoneTaskJoinClan(Clan clan) {
// dòng 983: public void close() {
// dòng 1029: public int capsule(Clan clan) {
```

### [EffectSkillService.java](../../SRC/src/nro/models/services/EffectSkillService.java) — 611 dòng

Kiểu được nhận diện: EffectSkillService.

```java
// dòng 35: private EffectSkillService() {
// dòng 39: public static EffectSkillService gI() {
// dòng 46: public void sendEffectUseSkill(Player player, byte skillId) {
// dòng 61: public void sendEffectPlayer(Player plUseSkill, Player plTarget, byte toggle, byte effect) {
// dòng 81: public void sendEffectMob(Player plUseSkill, Mob mobTarget, byte toggle, byte effect) {
// dòng 97: public void removeUseTroi(Player player) {
// dòng 110: public void removeAnTroi(Player player) {
// dòng 118: public void setAnTroi(Player player, Player plTroi, long lastTimeAnTroi, int timeAnTroi) {
// dòng 123: public void setUseTroi(Player player, long lastTimeTroi, int timeTroi) {
// dòng 129: public void setThoiMien(Player player, long lastTimeThoiMien, int timeThoiMien) {
// dòng 135: public void removeThoiMien(Player player) {
// dòng 140: public void startStun(Player player, long lastTimeStartBlind, int timeBlind) {
// dòng 147: public void removeStun(Player player) {
// dòng 152: public void setSocola(Player player, long lastTimeSocola, int timeSocola) {
// dòng 159: public void removeSocola(Player player) {
// dòng 164: public void sendMobToSocola(Player player, Mob mob, int timeSocola) {
// dòng 179: public void setBlindDCTT(Player player, long lastTimeDCTT, int timeBlindDCTT) {
// dòng 185: public void removeBlindDCTT(Player player) {
// dòng 190: public void setStartHuytSao(Player player, int tiLeHP) {
// dòng 195: public void removeHuytSao(Player player) {
// dòng 202: public void startUseSkillMonkey(Player player) {
// dòng 214: public void finishUseMonkey(Player player) {
// dòng 224: public void setIsMonkey(Player player) {
// dòng 244: public void monkeyDown(Player player) {
// dòng 260: public void setIntrinsic(Player player, int skillId, int cooldown, long lastTimeUseSkill) {
// dòng 267: public void releaseCooldownSkillByIntrinsic(Player player) {
// dòng 273: public void setIsBinh(Player plAtt, Player player, int time) {
// dòng 286: public void BinhDown(Player player) {
// dòng 293: public void setIsHalloween(Player player, int outFit, int time) {
// dòng 304: public void removeHalloween(Player player) {
// dòng 311: public void startUseMafuba(Player player, int time) {
// dòng 319: public void finishUseMafuba(Player player) {
// dòng 342: public void setIsStone(Player player, int time) {
// dòng 356: public void removeStone(Player player) {
// dòng 362: public void setIsLamCham(Player player, int time) {
// dòng 371: public void removeLamCham(Player player) {
// dòng 379: public void setIsTanHinh(Player player, int time) {
// dòng 386: public void removeTanHinh(Player player) {
// dòng 391: public void setDameBuff(Player player, int time, int tiLe) {
// dòng 398: public void removeDameBuff(Player player) {
// dòng 402: public void setMabuHold(Player player, MaBuHold MabuHold) {
// dòng 412: public void removeMabuHold(Player player) {
// dòng 420: public void setPKCommeson(Player player, int time) {
// dòng 427: public void removePKCommeson(Player player) {
// dòng 432: public void setPKSTT(Player player, int time) {
// dòng 439: public void removePKSTT(Player player) {
// dòng 444: public void setChibi(Player player, int time) {
// dòng 460: public void removeChibi(Player player) {
// dòng 470: public void startCharge(Player player) {
// dòng 477: public void stopCharge(Player player) {
// dòng 484: public void setStartShield(Player player) {
// dòng 490: public void removeShield(Player player) {
// dòng 495: public void breakShield(Player player) {
// dòng 501: public void sendEffectBlindThaiDuongHaSan(Player plUseSkill, List<Player> players, List<Mob> mobs, int timeStun) {
// dòng 525: public void sendEffectStartCharge(Player player) {
// dòng 540: public void sendEffectCharge(Player player) {
// dòng 555: public void sendEffectStopCharge(Player player) {
// dòng 568: public void sendEffectEndCharge(Player player) {
// dòng 582: public void sendEffectMonkey(Player player) {
// dòng 597: public void setIsBodyChangeTechnique(Player player) {
// dòng 607: public void removeBodyChangeTechnique(Player player) {
```

### [FlagBagService.java](../../SRC/src/nro/models/services/FlagBagService.java) — 105 dòng

Kiểu được nhận diện: FlagBagService.

```java
// dòng 21: public static FlagBagService gI() {
// dòng 28: public void sendIconFlagChoose(Player player, int id) {
// dòng 47: public void sendIconEffectFlag(Player player, int id) {
// dòng 65: public void sendListFlagClan(Player pl) {
// dòng 84: public FlagBag getFlagBag(int id) {
// dòng 93: public List<FlagBag> getFlagsForChooseClan() {
```

### [FriendAndEnemyService.java](../../SRC/src/nro/models/services/FriendAndEnemyService.java) — 316 dòng

Kiểu được nhận diện: FriendAndEnemyService.

```java
// dòng 37: public static FriendAndEnemyService gI() {
// dòng 44: public void controllerFriend(Player player, Message msg) {
// dòng 63: public void controllerEnemy(Player player, Message msg) {
// dòng 98: private void reloadFriend(Player player) {
// dòng 117: private void reloadEnemy(Player player) {
// dòng 136: private void openListFriend(Player player) {
// dòng 161: private void openListEnemy(Player player) {
// dòng 186: private void makeFriend(Player player, int playerId) {
// dòng 209: private void removeFriend(Player player, int playerId) {
// dòng 229: private void removeEnemy(Player player, int playerId) {
// dòng 239: public void chatPrivate(Player player, Message msg) {
// dòng 254: public void acceptMakeFriend(Player player, int playerId) {
// dòng 273: public void goToPlayerWithYardrat(Player player, Message msg) {
// dòng 297: public void addEnemy(Player player, Player enemy) {
```

### [GiftCodeService.java](../../SRC/src/nro/models/services/GiftCodeService.java) — 92 dòng

Kiểu được nhận diện: GiftCodeService.

```java
// dòng 22: public static GiftCodeService gI() {
// dòng 29: public void giftCode(Player player, String code) {
```

### [IntrinsicService.java](../../SRC/src/nro/models/services/IntrinsicService.java) — 162 dòng

Kiểu được nhận diện: IntrinsicService.

```java
// dòng 25: public static IntrinsicService gI() {
// dòng 32: public List<Intrinsic> getIntrinsics(byte playerGender) {
// dòng 43: public Intrinsic getIntrinsicById(int id) {
// dòng 52: public void sendInfoIntrinsic(Player player) {
// dòng 65: public void showAllIntrinsic(Player player) {
// dòng 84: public void showMenu(Player player) {
// dòng 90: public void showConfirmOpen(Player player) {
// dòng 95: public void showConfirmOpenVip(Player player) {
// dòng 100: private void changeIntrinsic(Player player) {
// dòng 117: public void doinoitai(Player player) {
// dòng 126: public void open(Player player) {
// dòng 145: public void openVip(Player player) {
```

### [InventoryService.java](../../SRC/src/nro/models/services/InventoryService.java) — 1,212 dòng

Kiểu được nhận diện: InventoryService.

```java
// dòng 33: public static InventoryService gI() {
// dòng 40: private void __________________Tìm_kiếm_item_____________________________() {
// dòng 44: public Item findItem(List<Item> list, int tempId) {
// dòng 56: public Item findItemBody(Player player, int tempId) {
// dòng 60: public Item findItemBag(Player player, int tempId) {
// dòng 64: public Item findItemBox(Player player, int tempId) {
// dòng 68: public boolean isExistItem(List<Item> list, int tempId) {
// dòng 76: public boolean isExistItemBody(Player player, int tempId) {
// dòng 80: public boolean isExistItemBag(Player player, int tempId) {
// dòng 84: public boolean isExistItemBox(Player player, int tempId) {
// dòng 88: private void __________________Sao_chép_danh_sách_item__________________() {
// dòng 92: public List<Item> copyList(List<Item> items) {
// dòng 100: public List<Item> copyItemsBody(Player player) {
// dòng 104: public List<Item> copyItemsBag(Player player) {
// dòng 108: public List<Item> copyItemsBox(Player player) {
// dòng 112: private void __________________Vứt_bỏ_item______________________________() {
// dòng 116: public void throwItem(Player player, int where, int index) {
// dòng 142: private void __________________Xoá_bỏ_item______________________________() {
// dòng 146: public void removeItem(List<Item> items, int index) {
// dòng 151: public void removeItem(List<Item> items, Item item) {
// dòng 165: public void removeItemBag(Player player, int index) {
// dòng 169: public void removeItemBag(Player player, Item item) {
// dòng 173: public void removeItemBody(Player player, int index) {
// dòng 177: public void removeItemPetBody(Player player, int index) {
// dòng 181: public void removeItemBox(Player player, int index) {
// dòng 185: private void __________________Giảm_số_lượng_item_______________________() {
// dòng 189: public void subQuantityItemsBag(Player player, Item item, int quantity) {
// dòng 193: public void subQuantityItemsBody(Player player, Item item, int quantity) {
// dòng 197: public void subQuantityItemsBox(Player player, Item item, int quantity) {
// dòng 201: public void subQuantityItem(List<Item> items, Item item, int quantity) {
// dòng 215: private void __________________Sắp_xếp_danh_sách_item___________________() {
// dòng 219: public void sortItems(List<Item> list) {
// dòng 241: public Item finditemBongHoa(Player player, int soluong) {
// dòng 250: public void sortItemv2(List<Item> items) {
// dòng 263: private void __________________Thao_tác_tháo_mặc_item___________________() {
// dòng 267: private Item putItemBag(Player player, Item item) {
// dòng 278: private Item putItemBox(Player player, Item item) {
// dòng 289: public Item putItemBody(Player player, Item item) {
// dòng 397: public void itemBagToBody(Player player, int index) {
// dòng 412: public void itemBodyToBag(Player player, int index) {
// dòng 437: public void itemBagToPetBody(Player player, int index) {
// dòng 461: public void itemPetBodyToBag(Player player, int index) {
// dòng 474: public void itemBoxToBodyOrBag(Player player, int index) {
// dòng 518: public void itemBagToBox(Player player, int index) {
// dòng 544: public void itemBodyToBox(Player player, int index) {
// dòng 558: private void __________________Gửi_danh_sách_item_cho_người_chơi________() {
// dòng 562: public void sendItemBags(Player player) {
// dòng 605: public void sendItemBody(Player player) {
// dòng 649: public void sendItemBox(Player player) {
// dòng 689: public void openBox(Player player) {
// dòng 700: private void __________________Thêm_vật_phẩm_vào_danh_sách______________() {
// dòng 704: private boolean addItemSpecial(Player player, Item item) {
// dòng 735: public boolean addItemBag(Player player, Item item) {
// dòng 817: public boolean addItemBox(Player player, Item item) {
// dòng 821: public boolean addItemList(List<Item> items, Item itemAdd) {
// dòng 883: public static boolean checkListsEqual(List<ItemOption> list1, List<ItemOption> list2) {
// dòng 897: private void __________________Kiểm_tra_điều_kiện_vật_phẩm______________() {
// dòng 907: private int[] isItemIncrementalOption(Item item) {
// dòng 919: private void __________________Kiểm_tra_danh_sách_còn_chỗ_trống_________() {
// dòng 923: public byte getCountEmptyBag(Player player) {
// dòng 927: public byte getCountEmptyListItem(List<Item> list) {
// dòng 937: public byte getIndexBag(Player pl, Item it) {
// dòng 947: public boolean finditemWoodChest(Player player) {
// dòng 961: public int getParam(Player player, int idoption, int itemID) {
// dòng 974: public void subParamItemsBag(Player player, int itemID, int idoption, int param) {
// dòng 995: public boolean findItemBongTaiCap2(Player player) {
// dòng 1008: public boolean findItemBongTaiCap3(Player player) {
// dòng 1022: public boolean findItemBongTai(Player player) {
// dòng 1036: public boolean findItemSkinQuyLaoKame(Player player) {
// dòng 1055: public boolean findItemNTK(Player player) {
// dòng 1071: public boolean findItemTVC(Player player) {
// dòng 1087: public boolean findItemTatVoGiangSinh(Player player) {
// dòng 1096: public boolean findSenzu(Player player) {
// dòng 1105: public boolean fullSetThan(Player player) {
// dòng 1115: public boolean x99ThucAn(Player player) {
// dòng 1120: public boolean canOpenBillShop(Player player) {
// dòng 1124: public boolean optionCanUpgrade(int id) {
// dòng 1128: public int getIndexItem(Player player, List<Item> items, Item item) {
// dòng 1137: public int getIndexItemBag(Player player, Item item) {
// dòng 1141: public int getIndexItemBody(Player player, Item item) {
// dòng 1145: public int getIndexItemBox(Player player, Item item) {
// dòng 1149: private void handleOption210(Item item) {
// dòng 1183: public boolean findItem(Player player, int id) {
// dòng 1197: private void checkOption231(Item item) {
```

### [ItemService.java](../../SRC/src/nro/models/services/ItemService.java) — 1,175 dòng

Kiểu được nhận diện: ItemService.

```java
// dòng 27: public static ItemService gI() {
// dòng 34: public short getItemIdByIcon(short IconID) {
// dòng 43: public Item createItemNull() {
// dòng 48: public Item createItemFromItemShop(ItemShop itemShop) {
// dòng 60: public Item copyItem(Item item) {
// dòng 74: public Item createNewItem(short tempId) {
// dòng 78: public Item createNewItem(short tempId, int quantity) {
// dòng 89: public Item otpts(short tempId, int quantity) {
// dòng 119: public Item createItemSetKichHoat(int tempId, int quantity) {
// dòng 130: public void OpenSKH(Player player, int itemUseId, int select) throws Exception {
// dòng 193: public void OpenVeTangNgoc(Player player, int itemUseId, int select) throws Exception {
// dòng 256: public Item createItemSKH(int itemId, int skhId) {
// dòng 269: public int optionItemSKH(int typeItem) {
// dòng 284: public int pagramItemSKH(int typeItem) {
// dòng 297: public int[] getOptionIdsBySKH(int skhId) {
// dòng 330: public int randomSKHId(byte gender) {
// dòng 350: public Item createItemFromItemMap(ItemMap itemMap) {
// dòng 356: public ItemOptionTemplate getItemOptionTemplate(int id) {
// dòng 360: public Template.ItemTemplate getTemplate(int id) {
// dòng 364: public int getPercentTrainArmor(Item item) {
// dòng 386: public boolean isTrainArmor(Item item) {
// dòng 405: public boolean isOutOfDateTime(Item item) {
// dòng 424: public void OpenItem736(Player player, Item itemUse) {
// dòng 468: public void OpenItem648(Player player, Item itemUse) {
// dòng 513: public Item caitrang2011(boolean rating) {
// dòng 526: public Item caitrangChristmas(boolean rating) {
// dòng 542: public Item phuKien2011(boolean rating) {
// dòng 557: public Item phuKienChristmas(boolean rating) {
// dòng 572: public Item vanBay2011(boolean rating) {
// dòng 582: public Item daBaoVe() {
// dòng 588: public Item randomRac() {
// dòng 597: public Item randomRac2() {
// dòng 610: public Item vanBayChrimas(boolean rating) {
// dòng 620: public byte optionRac(short itemId) {
// dòng 637: public Item vatphamsk(boolean hsd) {
// dòng 660: public List<Item.ItemOption> getListOptionItemShop(short id) {
// dòng 670: public int randTempItemDoSao(int gender) {
// dòng 703: public int randTempItemKichHoat(int gender) {
// dòng 722: public int randDoSao(int gender) {
// dòng 749: public int[] randOptionItemKichHoat(int gender) {
// dòng 827: public ItemMap randDoTL(Zone zone, int quantity, int x, int y, long id) {
// dòng 956: public ItemMap randDoTLBoss(Zone zone, int quantity, int x, int y, long id) {
// dòng 1088: public Item DoThienSu(int itemId, int gender) {
// dòng 1137: public void AddOption(ItemMap item, int skhId) {
// dòng 1141: public void Option(Item item, int skhId) {
// dòng 1145: private void Option_All(List<ItemOption> item, int skhId) {
// dòng 1151: public int ID(int skhId) {
```

### [ItemTimeService.java](../../SRC/src/nro/models/services/ItemTimeService.java) — 297 dòng

Kiểu được nhận diện: ItemTimeService.

```java
// dòng 25: public static ItemTimeService gI() {
// dòng 32: public void sendAllItemTime(Player player) {
// dòng 130: public void turnOnTDLT(Player player, Item item) {
// dòng 157: public void turnOffTDLT(Player player, Item item) {
// dòng 170: public void sendCanAutoPlay(Player player) {
// dòng 181: public void sendTextDoanhTrai(Player player) {
// dòng 193: public void sendTextTimePickDoanhTrai(Player player) {
// dòng 204: public void sendTextBanDoKhoBau(Player player) {
// dòng 216: public void sendTextXinbato(Player player) {
// dòng 220: public void sendTextConDuongRanDoc(Player player) {
// dòng 232: public void sendTextKhiGasHuyDiet(Player player) {
// dòng 244: public void sendTextTimeKeoBuaBao(Player player, int time) {
// dòng 248: public void removeTextDoanhTrai(Player player) {
// dòng 252: public void removeTextBanDoKhoBau(Player player) {
// dòng 256: public void removeTextConDuongRanDoc(Player player) {
// dòng 260: public void removeTextKhiGasHuyDiet(Player player) {
// dòng 264: public void removeTextTime(Player player, byte id) {
// dòng 268: public void sendTextTime(Player player, byte id, String text, int seconds) {
// dòng 281: public void sendItemTime(Player player, int itemId, int time) {
// dòng 293: public void removeItemTime(Player player, int itemTime) {
```

### [NpcService.java](../../SRC/src/nro/models/services/NpcService.java) — 120 dòng

Kiểu được nhận diện: NpcService.

```java
// dòng 21: public static NpcService gI() {
// dòng 28: public void createMenuRongThieng(Player player, int indexMenu, String npcSay, String... menuSelect) {
// dòng 32: public void createMenuConMeo(Player player, int indexMenu, int avatar, String npcSay, String... menuSelect) {
// dòng 36: public void createMenuConMeo(Player player, int indexMenu, int avatar, String npcSay, String[] menuSelect, Object object) {
// dòng 41: private void createMenu(Player player, int indexMenu, byte npcTempId, int avatar, String npcSay, String... menuSelect) {
// dòng 65: public void createTutorial(Player player, int avatar, String npcSay) {
// dòng 80: public void createTutorial(Player player, int tempId, int avatar, String npcSay) {
// dòng 95: public int getAvatar(int npcId) {
// dòng 104: public void createBigMessage(Player player, int avatar, String npcSay, byte type, String select, String confirn) {
```

### [OpenPowerService.java](../../SRC/src/nro/models/services/OpenPowerService.java) — 67 dòng

Kiểu được nhận diện: OpenPowerService.

```java
// dòng 19: private OpenPowerService() {
// dòng 23: public static OpenPowerService gI() {
// dòng 30: public boolean openPowerBasic(Player player) {
// dòng 48: public boolean openPowerSpeed(Player player) {
```

### [PetService.java](../../SRC/src/nro/models/services/PetService.java) — 352 dòng

Kiểu được nhận diện: PetService.

```java
// dòng 20: public static PetService gI() {
// dòng 27: public void createNormalPet(Player player, int gender, byte... limitPower) {
// dòng 41: public void createNormalPet(Player player, byte... limitPower) {
// dòng 55: public void createMabuPet(Player player, int gender, byte... limitPower) {
// dòng 69: public void createMabuPet(Player player, byte... limitPower) {
// dòng 83: public void createUubPet(Player player, byte... limitPower) {
// dòng 97: public void createJirenPet(Player player, byte... limitPower) {
// dòng 111: public void createKidBeerPet(Player player, byte... limitPower) {
// dòng 125: public void changeNormalPet(Player player, int gender) {
// dòng 136: public void changeNormalPet(Player player) {
// dòng 147: public void changeMabuPet(Player player) {
// dòng 158: public void changeUubPet(Player player) {
// dòng 169: public void changeKidBeerPet(Player player) {
// dòng 180: public void changeJirenPet(Player player) {
// dòng 191: public void changeMabuPet(Player player, int gender) {
// dòng 202: public void changeNamePet(Player player, String name) {
// dòng 229: private int[] getDataPetNormal() {
// dòng 239: private int[] getDataPetMabu() {
// dòng 249: private int[] getDataPetUub() {
// dòng 259: private int[] getDataPetKidBeer() {
// dòng 269: private int[] getDataPetJiren() {
// dòng 279: private void createNewPet(Player player, boolean isMabu, boolean isUub, boolean isKidBeer, boolean isJiren, byte... gender) {
// dòng 331: public static void Pet2(Player pl, int h, int b, int l) {
```

### [PlayerService.java](../../SRC/src/nro/models/services/PlayerService.java) — 350 dòng

Kiểu được nhận diện: PlayerService.

```java
// dòng 37: public PlayerService() {
// dòng 40: public static PlayerService gI() {
// dòng 47: public void sendTNSM(Player player, byte type, long param) {
// dòng 61: public void sendMessageAllPlayer(Message msg) {
// dòng 70: public void sendMessageIgnore(Player plIgnore, Message msg) {
// dòng 79: public void sendInfoHp(Player player) {
// dòng 91: public void sendInfoMp(Player player) {
// dòng 103: public void sendInfoHpMp(Player player) {
// dòng 111: public void hoiPhuc(Player player, long hp, long mp) {
// dòng 122: public void sendInfoHpMpMoney(Player player) {
// dòng 148: public void playerMove(Player player, int x, int y) {
// dòng 207: public void sendCurrentStamina(Player player) {
// dòng 219: public void sendMaxStamina(Player player) {
// dòng 231: public void changeAndSendTypePK(Player player, int type) {
// dòng 236: public void changeTypePK(Player player, int type) {
// dòng 240: public void sendTypePk(Player player) {
// dòng 252: public void banPlayer(Player playerBaned) {
// dòng 268: public void hoiSinh(Player player) {
// dòng 300: public void hoiSinhMaBu(Player player) {
// dòng 329: public void dailyLogin(Player player) {
```

### [RadarService.java](../../SRC/src/nro/models/services/RadarService.java) — 127 dòng

Kiểu được nhận diện: RadarService.

```java
// dòng 23: public static RadarService gI() {
// dòng 30: public void sendRadar(Player pl, List<Card> cards) {
// dòng 75: public void Radar1(Player pl, short id, int use) {
// dòng 88: public void RadarSetLevel(Player pl, int id, int level) {
// dòng 101: public void RadarSetAmount(Player pl, int id, int amount, int max_amount) {
// dòng 115: public void sendAura(Player pl, int id_Aura, int id_Eff_Set_Item) {
```

### [RewardService.java](../../SRC/src/nro/models/services/RewardService.java) — 1,762 dòng

Kiểu được nhận diện: RewardService.

```java
// dòng 25: private RewardService() {
// dòng 29: public static RewardService gI() {
// dòng 37: public List<Item> getListItemLuckyRound(Player player, int num, boolean vip) {
// dòng 176: public Item itemRand(Item item, boolean success) {
// dòng 184: public void rewardLancon(Player player) {
// dòng 215: public void initChiSoItem(Item item) {
// dòng 220: private static void SetClothes(int tempId, int type, List<ItemOption> list) {
// dòng 1011: public void initBaseOptionClothes(Item itemid, int tempId, int type, List<ItemOption> list) {
// dòng 1672: public void initActivationOption(int gender, int type, List<ItemOption> list) {
// dòng 1681: public Item rewardCapsuleTet(Player player) {
```

### [Service.java](../../SRC/src/nro/models/services/Service.java) — 2,920 dòng

Kiểu được nhận diện: Service.

```java
// dòng 65: public static Service gI() {
// dòng 72: public void showYourNumber(Player player, String Number, String result, String finish, int type) {
// dòng 95: public void SendImgSkill9(short SkillId, int IdAnhSKill) {
// dòng 109: public void sendPetFollow(Player player, short smallId) {
// dòng 135: public void sendBadgesPlayer(Player player, int sec, int idImg) {
// dòng 149: public void sendChibi(Player player) {
// dòng 180: public void sendHaveChibiFollowToAllMap(Player pl) {
// dòng 190: public void ClosePanel(Player pl) {
// dòng 205: public void sendChibiFollowToMe(Player me, Player pl) {
// dòng 228: public void showListTop(Player player, List<TOP> tops) {
// dòng 264: public void showListTopTraiDat(Player player, List<TOP> tops) {
// dòng 301: public void showListTopNamek(Player player, List<TOP> tops) {
// dòng 338: public void showListTopXayda(Player player, List<TOP> tops) {
// dòng 375: public void showTopClanBDKB(Player player) {
// dòng 404: public void showMyTopClanBDKB(Player player) {
// dòng 433: public void showTopClanKhiGas(Player player) {
// dòng 462: public void showTopClanCDRD(Player player) {
// dòng 491: public void sendMessAllPlayer(Message msg) {
// dòng 495: public void sendMessAllPlayerIgnoreMe(Player player, Message msg) {
// dòng 499: public void sendMessAllPlayerInMap(Zone zone, Message msg) {
// dòng 518: public void sendMessAllPlayerInMap(Player player, Message msg) {
// dòng 555: public void sendMessAnotherNotMeInMap(Player player, Message msg) {
// dòng 590: public void Send_Info_NV(Player pl) {
// dòng 606: public void Send_Info_NV_do_Injure(Player pl) {
// dòng 622: public void sendInfoPlayerEatPea(Player pl) {
// dòng 638: public void reload_HP_NV(Player pl) {
// dòng 655: public void resetPoint(Player player, int x, int y) {
// dòng 671: public void clearMap(Player player) {
// dòng 682: public boolean isMessageContainsThang(String text) {
// dòng 686: public void chat(Player player, String text) {
// dòng 709: public void chatJustForMe(Player me, Player plChat, String text) {
// dòng 721: public Npc getNpc(Player player) {
// dòng 734: public void Transport(Player pl) {
// dòng 750: public void Transport(Player pl, int type) {
// dòng 766: public void point(Player player) {
// dòng 802: public String name(Player player) {
// dòng 821: public void player(Player pl) {
// dòng 928: public Message messageNotLogin(byte command) throws IOException {
// dòng 934: public Message messageNotMap(byte command) throws IOException {
// dòng 940: public Message messageSubCommand(byte command) throws IOException {
// dòng 946: public void addSMTN(Player player, byte type, long param, boolean isOri) {
// dòng 1011: public String get_HanhTinh(int hanhtinh) {
// dòng 1024: public List<String> ListCaption(int gender) {
// dòng 1051: public String getCurrStrLevel(Player pl) {
// dòng 1055: public int getCurrLevel(Player pl) {
// dòng 1102: public void hsChar(Player pl, int hp, int mp) {
// dòng 1135: public void charDie(Player pl) {
// dòng 1166: public void attackMob(Player pl, int mobId, boolean isMobMe, int masterId) {
// dòng 1187: public void Send_Caitrang(Player player) {
// dòng 1210: public void setNotMonkey(Player player) {
// dòng 1223: public void sendFlagBag(Player pl) {
// dòng 1236: public void sendThongBaoOK(Player pl, String text) {
// dòng 1251: public void sendThongBaoOK(MySession session, String text) {
// dòng 1263: public void sendThongBaoAllPlayer(String thongBao) {
// dòng 1275: public void sendBigMessage(Player player, int iconId, String text) {
// dòng 1289: public void sendThongBaoFromAdmin(Player player, String text) {
// dòng 1293: public void sendThongBao(Player pl, String thongBao) {
// dòng 1306: public void sendThongBao(List<Player> pl, String thongBao) {
// dòng 1315: public void sendThongBaoToAnotherNotMe(Player me, String text) {
// dòng 1324: public void sendMoney(Player pl) {
// dòng 1342: public void sendToAntherMePickItem(Player player, int itemMapId) {
// dòng 1356: public void openFlagUI(Player pl) {
// dòng 1387: public void changeFlag(Player pl, int index) {
// dòng 1427: public void sendFlagPlayerToMe(Player me, Player pl) {
// dòng 1441: public void chooseFlag(Player pl, int index) {
// dòng 1456: public void attackPlayer(Player pl, int idPlAnPem) {
// dòng 1466: public void releaseCooldownSkill(Player pl) {
// dòng 1486: public void sendTimeSkill(Player pl) {
// dòng 1505: public void sendTimeSkill(Player pl, Skill skill) {
// dòng 1522: public void releaseCooldownSkill(Player pl, Skill skill) {
// dòng 1539: public void dropItemMap(Zone zone, ItemMap item) {
// dòng 1555: public void dropItemMapForMe(Player player, ItemMap item) {
// dòng 1571: public void showInfoPet(Player pl) {
// dòng 1655: public void sendSpeedPlayer(Player pl, int speed) {
// dòng 1668: public void setPos(Player player, int x, int y) {
// dòng 1685: public void setPos2(Player player, int x, int y) {
// dòng 1700: public void setPos0(Player player, int x, int y) {
// dòng 1717: public void getPlayerMenu(Player player, int playerId) {
// dòng 1740: public void hideWaitDialog(Player pl) {
// dòng 1752: public void chatPrivate(Player plChat, Player plReceive, String text) {
// dòng 1795: public void changePassword(Player player, String oldPass, String newPass, String rePass) {
// dòng 1819: public void switchToCreateChar(MySession session) {
// dòng 1830: public void sendCaption(MySession session, byte gender) {
// dòng 1846: public void sendHavePet(Player player) {
// dòng 1858: public void sendWaitToLogin(MySession session, int secondsWait) {
// dòng 1870: public void sendMessage(MySession session, int cmd, String path) {
// dòng 1882: public void sendNangDong(Player player) {
// dòng 1894: public void setClientType(MySession session, Message msg) {
// dòng 1914: public void dropSatellite(Player pl, Item item, Zone map, int x, int y) {
// dòng 1941: public void mabaove(Player player, int mbv) {
// dòng 1958: public void sendEffPlayer(Player pl, Player plReceive, int idEff, int layer, int loop, int loopCount) {
// dòng 1976: public Message showInputText(String title, int maxLength) throws IOException {
// dòng 1983: public void sendEffAllPlayer(Player pl, int idEff, int layer, int loop, int loopCount) {
// dòng 2000: public void sendEffCaiTrang(Player pl, int costumeId) {
// dòng 2038: public void removeEffAllPlayer(Player pl) {
// dòng 2051: public void removeEffPlayer(Player pl, int idEff) {
// dòng 2065: public void sendEffPlayer(Player pl) {
// dòng 2087: public void sendEffAllPlayerMapToMe(Player pl) {
// dòng 2098: public void Send_Body_Mob(Mob mob, int type, int idIcon) {
// dòng 2118: public void sendPlayerVS(Player pVS1, Player pVS2, byte type) {
// dòng 2138: public void sendPVB(Player pVS1, Player pVS2, byte type) {
// dòng 2164: public void sendPVP(Player p1, Player p2) {
// dòng 2177: public void exitMap(Player player, long playerExitMapId) {
// dòng 2189: public void SendPowerInfo(Player player) {
// dòng 2207: public void SendPercentPowerInfo(Player player) {
// dòng 2225: public void SendMabu(Zone zone, int percent) {
// dòng 2240: public void callNhanBan(Player player) {
// dòng 2280: public void sendBigBoss(Zone zone, int action, int size, int id, int dame) {
// dòng 2300: public void sendBigBoss2(Zone zone, int action, Mob bigboss) {
// dòng 2317: public void sendBigBoss2(Player player, int action, Mob bigboss) {
// dòng 2334: public void sendMabuHold(Player player, int action, short x, short y) {
// dòng 2354: public void sendMabuHoldToMe(Player player, Player plReceive, int action, short x, short y) {
// dòng 2369: public void sendEffMabuHoldAllPlayerMapToMe(Player pl) {
// dòng 2382: public void sendEffMabuEat(Player player, Player plTarget) {
// dòng 2396: public void sendMabuEat(Player player, Player plTarget) {
// dòng 2424: public void sendMabuAttackSkill(Player player) {
// dòng 2451: public Message messageReadOpt(byte command) throws IOException {
// dòng 2457: public void sendMessageServer(String data) {
// dòng 2468: public void sendHideNpc(Player player, int npcId, boolean isHide) {
// dòng 2481: public void sendTopRank(Player pl) {
// dòng 2497: public void moveFast(Player pl, int x, int y) {
// dòng 2512: public void dropAndPickItemDNC(Player pl, int itemId) {
// dòng 2519: public void dropAndPickItem(Player pl, int itemId, int quantity) {
// dòng 2525: public void sendTypePK(Player player, Player boss) {
// dòng 2539: public void playerInfoUpdate(Player pl, Player plR, String plName, int plHead, int plBody, int plLeg) {
// dòng 2592: public void sendLoginFail(MySession session, boolean isLoggingIn) {
// dòng 2604: public void stealMoney(Player pl, int stealMoney) {//danh cho boss an trom
// dòng 2616: public void LogicEffect(Player pl, int id, int layer, int loop, int loopcount, int stand, int times) {
// dòng 2636: private void removeEffectChar(Player pl, int id) {
// dòng 2650: public void sendServerMessage(Player player, String text) {
// dòng 2665: public void sendServerAlert(Player player, String text) {
// dòng 2680: public void sendDialogMessage(Player pl, String text) {
// dòng 2695: public void sendDialogMessage(MySession session, String text) {
// dòng 2710: public void updatePlayerTotalDamage(Player pl) {
// dòng 2724: public void updatePlayerPointMayDam(Player pl) {
// dòng 2736: public void sendDanhQuaiNhanNgoc(Player player) {
// dòng 2742: public static void sendIcon(MySession session, int id) {
// dòng 2758: public void sendInfoCharMoiToMe(Player plReceive, BotAttackplayer bot) {
// dòng 2834: public void sendAppear(BotAttackplayer bot, Player player) {
// dòng 2842: public void sendDisappear(BotAttackplayer bot, Player player) {
// dòng 2862: public void sendPlayerInfo(Player player) {
// dòng 2897: public void loadMob(Player p, GauTuongCuop gauTuong) {
```

### [SkillService.java](../../SRC/src/nro/models/services/SkillService.java) — 1,269 dòng

Kiểu được nhận diện: SkillService.

```java
// dòng 40: public static SkillService gI() {
// dòng 47: public boolean useSkill(Player player, Player plTarget, Mob mobTarget, int status, Message msg) {
// dòng 121: private void useNewSkillNotFocus(Player player, Player plTarget, Mob mobTarget, int status, byte skillId, Short dx, Short dy, byte dir, Short x, Short y) {
// dòng 148: public void updateSkillSpecial(Player player) {
// dòng 257: public void sendCurrLevelSpecial(Player player, Skill skill) {
// dòng 274: public void newSkillNotFocus(Player player, int status) {
// dòng 339: public void learSkillSpecial(Player player, byte skillID) {
// dòng 359: public void useSkillAttack(Player player, Player plTarget, Mob mobTarget) {
// dòng 599: public void useSkillAlone(Player player) {
// dòng 789: private void useSkillBuffToPlayer(Player player, Player plTarget) {
// dòng 845: private void phanSatThuong(Player plAtt, Player plTarget, long dame) {
// dòng 883: private void hutHPMP(Player player, long dame, Player pl, Mob mob) {
// dòng 903: private void playerAttackPlayer(Player plAtt, Player plInjure, boolean miss) {
// dòng 962: private void playerAttackMob(Player plAtt, Mob mob, boolean miss, boolean dieWhenHpFull) {
// dòng 1005: public void sendPlayerPrepareSkill(Player player, int affterMiliseconds) {
// dòng 1023: public void sendPlayerPrepareBom(Player player, int affterMiliseconds) {
// dòng 1041: public boolean canUseSkillWithMana(Player player) {
// dòng 1072: public boolean canUseSkillWithCooldown(Player player) {
// dòng 1077: public void affterUseSkill(Player player, int skillId) {
// dòng 1105: private void setMpAffterUseSkill(Player player) {
// dòng 1126: private void setLastTimeUseSkill(Player player, int skillId) {
// dòng 1191: private boolean canHsPlayer(Player player, Player plTarget) {
// dòng 1214: public boolean canAttackPlayer(Player p1, Player p2) {
// dòng 1222: public boolean canAttackPlayer2(Player p1, Player p2) {
// dòng 1245: private void sendPlayerAttackMob(Player plAtt, Mob mob) {
// dòng 1261: public void selectSkill(Player player, int skillId) {
```

### [SubMenuService.java](../../SRC/src/nro/models/services/SubMenuService.java) — 168 dòng

Kiểu được nhận diện: SubMenuService, SubMenu.

```java
// dòng 30: private SubMenuService() {
// dòng 33: public static SubMenuService gI() {
// dòng 40: public void controller(Player player, int playerTarget, int menuId) {
// dòng 139: public void showSubMenu(Player player, SubMenu... subMenus) {
// dòng 162: public SubMenu(int id, String caption1, String caption2) {
```

### [TaskService.java](../../SRC/src/nro/models/services/TaskService.java) — 1,650 dòng

Kiểu được nhận diện: TaskService.

```java
// dòng 48: public static nro.models.services.TaskService gI() {
// dòng 55: public TaskMain getTaskMainById(Player player, int id) {
// dòng 72: public void sendTaskMain(Player player) {
// dòng 105: public void sendNextTaskMain(Player player) {
// dòng 125: public void sendUpdateCountSubTask(Player player) {
// dòng 139: public void sendNextSubTask(Player player) {
// dòng 152: public void sendInfoCurrentTask(Player player) {
// dòng 157: public boolean checkDoneTaskTalkNpc(Player player, Npc npc) {
// dòng 255: public void checkDoneTaskJoinClan(Player player) {
// dòng 261: public void checkDoneTaskGetItemBox(Player player) {
// dòng 267: public void checkDoneTaskPower(Player player, long power) {
// dòng 299: public void checkDoneTaskUseTiemNang(Player player) {
// dòng 305: public void checkDoneTaskUseItem(Player player, Item item) {
// dòng 312: public void checkDoneTaskGoToMap(Player player, Zone zoneJoin) {
// dòng 344: public void checkDoneTaskPickItem(Player player, ItemMap item) {
// dòng 365: public void checkDoneTaskFind7Stars(Player player) {
// dòng 369: public void checkDoneTaskNangCS(Player player) {
// dòng 378: public void checkDoneTaskConfirmMenuNpc(Player player, Npc npc, byte select) {
// dòng 394: public void checkDoneTaskKillBoss(Player player, Boss boss) {
// dòng 476: public void checkDoneTaskKillMob(Player player, Mob mob) {
// dòng 616: private boolean doneTask(Player player, int idTaskCustom) {
// dòng 1064: private void npcSay(Player player, int npcId, String text) {
// dòng 1071: public void loadTask() {
// dòng 1085: private void rewardDoneTask(Player player) {
// dòng 1119: private void addDoneSubTask(Player player, int numDone) {
// dòng 1134: private int transformMapId(Player player, int id) {
// dòng 1165: private int transformNpcId(Player player, int id) {
// dòng 1186: private String transformName(Player player, String text) {
// dòng 1234: private boolean isCurrentTask(Player player, int idTaskCustom) {
// dòng 1238: public int getIdTask(Player player) {
// dòng 1246: public SideTaskTemplate getSideTaskTemplateById(int id) {
// dòng 1253: public void changeSideTask(Player player, byte level) {
// dòng 1270: public void removeSideTask(Player player) {
// dòng 1275: public void paySideTask(Player player) {
// dòng 1349: public void checkDoneSideTaskKillMob(Player player, Mob mob) {
// dòng 1415: public void checkDoneSideTaskPickItem(Player player, ItemMap item) {
// dòng 1424: private void notifyProcessSideTask(Player player) {
// dòng 1472: public ClanTaskTemplate getClanTaskTemplateById(int id) {
// dòng 1479: public void changeClanTask(Npc npc, Player player, byte level) {
// dòng 1494: public void removeClanTask(Player player) {
// dòng 1500: public void payClanTask(Player player) {
// dòng 1530: public void checkDoneClanTaskKillMob(Player player, Mob mob) {
// dòng 1596: public void checkDoneClanTaskPickItem(Player player, ItemMap item) {
// dòng 1605: private void notifyProcessClanTask(Player player) {
```

### [TrainingService.java](../../SRC/src/nro/models/services/TrainingService.java) — 180 dòng

Kiểu được nhận diện: TrainingService.

```java
// dòng 33: public static TrainingService gI() {
// dòng 40: public Player getNonInteractiveNPC(Zone zone, int id) {
// dòng 49: public int getNpc(int BossId) {
// dòng 70: public void luyenTapEnd(Player pl, int bossID) {
// dòng 76: public Boss callBoss(Player pl, int bossID, boolean isThachDau) {
// dòng 118: public int getTnsmMoiPhut(Player player) {
// dòng 130: public void tangTnsmLuyenTap(Player player, long tnsm) {
// dòng 139: public void tnsmLuyenTapUp(Player player) {
```

## nro.models.services.shenron (5 tệp)

### [Shenron_Event.java](../../SRC/src/nro/models/services/shenron/Shenron_Event.java) — 275 dòng

Kiểu được nhận diện: Shenron_Event.

```java
// dòng 60: public void update() {
// dòng 88: public void reSummonShenron() {
// dòng 93: public void sendBlackGokuhesShenron() {
// dòng 97: public void showConfirmShenron(byte select) {
// dòng 108: public void activeShenron(boolean appear, byte type) {
// dòng 133: public void confirmWish() {
// dòng 258: public void shenronLeave() {
```

### [Shenron_Manager.java](../../SRC/src/nro/models/services/shenron/Shenron_Manager.java) — 69 dòng

Kiểu được nhận diện: Shenron_Manager.

```java
// dòng 25: public static Shenron_Manager gI() {
// dòng 33: public void run() {
// dòng 45: public void update() {
// dòng 61: public void add(Shenron_Event se) {
// dòng 65: public void remove(Shenron_Event se) {
```

### [Shenron_Service.java](../../SRC/src/nro/models/services/shenron/Shenron_Service.java) — 89 dòng

Kiểu được nhận diện: Shenron_Service.

```java
// dòng 33: public static Shenron_Service gI() {
// dòng 40: public void openMenuSummonShenron(Player pl, int type) {
// dòng 46: public void summonShenron(Player player) {
// dòng 79: private boolean checkShenronBall(Player pl) {
```

### [SummonDragon.java](../../SRC/src/nro/models/services/shenron/SummonDragon.java) — 556 dòng

Kiểu được nhận diện: SummonDragon.

```java
// dòng 94: private SummonDragon() {
// dòng 125: private void active() {
// dòng 132: public static SummonDragon gI() {
// dòng 139: public void openMenuSummonShenron(Player pl, byte dragonBallStar) {
// dòng 145: public void summonShenron(Player pl) {
// dòng 188: private void reSummonShenron() {
// dòng 193: private void sendBlackGokuhesShenron(Player pl) {
// dòng 214: private void activeShenron(Player pl, boolean appear, byte type) {
// dòng 238: private boolean checkShenronBall(Player pl) {
// dòng 274: private void sendNotifyShenronAppear() {
// dòng 289: public void confirmWish() {
// dòng 470: public void showConfirmShenron(Player pl, int menu, byte select) {
// dòng 519: public void reOpenShenronWishes(Player pl) {
// dòng 539: public void shenronLeave(Player pl, byte type) {
```

### [SummonDragonNamek.java](../../SRC/src/nro/models/services/shenron/SummonDragonNamek.java) — 321 dòng

Kiểu được nhận diện: SummonDragonNamek.

```java
// dòng 43: public static SummonDragonNamek gI() {
// dòng 50: private SummonDragonNamek() {
// dòng 80: private void active() {
// dòng 87: public void summonNamec(Player pl) {
// dòng 101: private void reSummonShenron() {
// dòng 106: private void activeShenron(Player pl, boolean appear, byte type) {
// dòng 128: private void sendNotifyShenronNamekAppear() {
// dòng 144: public void confirmWish() {
// dòng 280: public void showConfirmShenron(Player pl, int menu, byte select) {
// dòng 302: public void sendBlackGokuhesNamec(Player pl) {
// dòng 306: public void shenronLeave(Player pl, byte type) {
```

## nro.models.services_dungeon (10 tệp)

### [BlackBallWarService.java](../../SRC/src/nro/models/services_dungeon/BlackBallWarService.java) — 198 dòng

Kiểu được nhận diện: BlackBallWarService.

```java
// dòng 34: public static BlackBallWarService gI() {
// dòng 41: private BlackBallWarService() {
// dòng 45: public void addMapBlackBallWar(int id, Zone zone) {
// dòng 49: public void dropBlackBall(Player player) {
// dòng 74: public void joinMapBlackBallWar(Player player) {
// dòng 91: public boolean pickBlackBall(Player player, Item item) {
// dòng 128: public void xHPKI(Player player, byte x) {
// dòng 157: public void xDame(Player player, byte x) {
// dòng 186: public void changeMap(Player player, byte index) {
```

### [DestronGasService.java](../../SRC/src/nro/models/services_dungeon/DestronGasService.java) — 95 dòng

Kiểu được nhận diện: DestronGasService.

```java
// dòng 23: public static DestronGasService gI() {
// dòng 32: private DestronGasService() {
// dòng 39: public void addMapKhiGasHuyDiet(int id, Zone zone) {
// dòng 43: public void openKhiGasHuyDiet(Player player, byte level) {
```

### [MajinBuu14HService.java](../../SRC/src/nro/models/services_dungeon/MajinBuu14HService.java) — 59 dòng

Kiểu được nhận diện: MajinBuu14HService.

```java
// dòng 22: public static MajinBuu14HService gI() {
// dòng 31: private MajinBuu14HService() {
// dòng 38: public void addMapMaBu2H(int id, Zone zone) {
// dòng 47: public void joinMaBu2H(Player player) {
```

### [MajinBuuService.java](../../SRC/src/nro/models/services_dungeon/MajinBuuService.java) — 126 dòng

Kiểu được nhận diện: MajinBuuService.

```java
// dòng 28: public static MajinBuuService gI() {
// dòng 35: public Npc getNpcOsin(Player player) {
// dòng 44: public Npc getNpcBabiday(Player player) {
// dòng 53: public Npc getNpcBabiday(Zone zone) {
// dòng 62: public void joinMapMabu(Player player) {
// dòng 79: public void xuongTangDuoi(Player player) {
// dòng 83: public void goHome(Player player) {
// dòng 100: public void update(Player player) {
```

### [NgocRongNamecService.java](../../SRC/src/nro/models/services_dungeon/NgocRongNamecService.java) — 460 dòng

Kiểu được nhận diện: NgocRongNamecService.

```java
// dòng 32: public static NgocRongNamecService gI() {
// dòng 53: public boolean isMapNRNM(int mapId) {
// dòng 58: public void initNgocRongNamec(byte type) { //type 0: INIT NGOC RONG, type 1: INIT HOA THACH NGOC RONG
// dòng 93: public void reInitNgocRongNamec() {
// dòng 130: public void removeStoneNrNamec() {
// dòng 144: public void doneDragonNamec() {
// dòng 157: public void reInitNrNamec(long time) {
// dòng 162: public boolean isSameMapNrNamec() {
// dòng 166: public boolean isSameZoneNrNamec() {
// dòng 170: public boolean canCallDragonNamec(Player p) {
// dòng 189: public void teleportToNrNamec(Player p) {
// dòng 211: public String getDis(Player pl, int id, short temp) {
// dòng 254: public String getDisPlayer(Player pl, int id, short temp) {
// dòng 297: public String getDisStone(Player pl, int id, short temp) {
// dòng 340: public byte findIndex(int id) {
// dòng 350: public List<Integer> findIndex(int start, int stop) {
// dòng 365: public boolean pickNamekBall(Player player, Item item) {
// dòng 410: public void run() {
// dòng 431: public void dropNamekBall(Player pl) {
// dòng 449: public void menuCheckTeleNamekBall(Player pl) {
```

### [RedRibbonHQService.java](../../SRC/src/nro/models/services_dungeon/RedRibbonHQService.java) — 73 dòng

Kiểu được nhận diện: RedRibbonHQService.

```java
// dòng 22: public static RedRibbonHQService gI() {
// dòng 31: private RedRibbonHQService() {
// dòng 38: public void addMapDoanhTrai(int id, Zone zone) {
// dòng 42: public void joinDoanhTrai(Player pl) {
```

### [SnakeWayService.java](../../SRC/src/nro/models/services_dungeon/SnakeWayService.java) — 88 dòng

Kiểu được nhận diện: SnakeWayService.

```java
// dòng 25: public static SnakeWayService gI() {
// dòng 34: private SnakeWayService() {
// dòng 41: public void addMapConDuongRanDoc(int id, Zone zone) {
// dòng 45: public void openConDuongRanDoc(Player player, byte level) {
```

### [SuperDivineWaterService.java](../../SRC/src/nro/models/services_dungeon/SuperDivineWaterService.java) — 144 dòng

Kiểu được nhận diện: SuperDivineWaterService.

```java
// dòng 29: public static SuperDivineWaterService gI() {
// dòng 36: public void addZone(Zone zone) {
// dòng 40: public void joinMapThanhThuy(Player player) {
// dòng 68: public void init(Zone zone, Player player) {
// dòng 99: public void update(Player player) {
```

### [TrainingService.java](../../SRC/src/nro/models/services_dungeon/TrainingService.java) — 185 dòng

Kiểu được nhận diện: TrainingService.

```java
// dòng 34: public static TrainingService gI() {
// dòng 41: public Player getNonInteractiveNPC(Zone zone, int id) {
// dòng 50: public int getNpc(int BossId) {
// dòng 71: public void luyenTapEnd(Player pl, int bossID) {
// dòng 77: public Boss callBoss(Player pl, int bossID, boolean isThachDau) {
// dòng 116: public int getTnsmMoiPhut(Player player) {
// dòng 135: public void tangTnsmLuyenTap(Player player, long tnsm) {
// dòng 144: public void tnsmLuyenTapUp(Player player) {
```

### [TreasureUnderSeaService.java](../../SRC/src/nro/models/services_dungeon/TreasureUnderSeaService.java) — 79 dòng

Kiểu được nhận diện: TreasureUnderSeaService.

```java
// dòng 23: public static TreasureUnderSeaService gI() {
// dòng 32: private TreasureUnderSeaService() {
// dòng 39: public void addMapBanDoKhoBau(int id, Zone zone) {
// dòng 43: public void openBanDoKhoBau(Player player, byte level) {
```

## nro.models.services_func (8 tệp)

### [BuyBackService.java](../../SRC/src/nro/models/services_func/BuyBackService.java) — 37 dòng

Kiểu được nhận diện: BuyBackService.

```java
// dòng 20: public static BuyBackService gI() {
// dòng 27: public void addItem(Player player, Item item) {
```

### [EffectMapService.java](../../SRC/src/nro/models/services_func/EffectMapService.java) — 89 dòng

Kiểu được nhận diện: EffectMapService.

```java
// dòng 18: private EffectMapService() {
// dòng 22: public static EffectMapService gI() {
// dòng 29: public void sendEffectMapToPlayer(Player player, int id, int layer, int loop, int x, int y, int delay) {
// dòng 49: public void sendEffectMapToAllInMap(Zone zone, int id, int layer, int loop, int x, int y, int delay) {
// dòng 69: public void sendEffectMapToAllInMap(Player player, int id, int layer, int loop, int x, int y, int delay) {
```

### [Input.java](../../SRC/src/nro/models/services_func/Input.java) — 759 dòng

Kiểu được nhận diện: Input, SubInput.

```java
// dòng 86: private Input() {
// dòng 90: public static Input gI() {
// dòng 97: public void doInput(Player player, Message msg) {
// dòng 583: public void createForm(Player pl, int typeInput, String title, SubInput... subInputs) {
// dòng 603: public void createForm(ISession session, int typeInput, String title, SubInput... subInputs) {
// dòng 622: public void createFormChangePassword(Player pl) {
// dòng 628: public void createFormGiveItem(Player pl) {
// dòng 632: public void createFormGetItem(Player pl) {
// dòng 636: public void createFormGiftCode(Player pl) {
// dòng 640: public void createFormMBV(Player pl) {
// dòng 644: public void createFormBangHoi(Player pl) {
// dòng 648: public void createFormFindPlayer(Player pl) {
// dòng 652: public void createFormFindPlayer1(Player pl) {
// dòng 657: public void createFormNapThe(Player pl, byte loaiThe) {
// dòng 662: public void createFormTangRuby(Player pl) {
// dòng 667: public void createFormTradeGold(Player pl) {
// dòng 671: public void createFormTradeGem(Player pl) {
// dòng 675: public void createFormChangeName(Player pl, Player plChanged) {
// dòng 680: public void createFormChangeNameByItem(Player pl) {
// dòng 684: public void createFormChooseLevelBDKB(Player pl) {
// dòng 688: public void createFormChooseLevelCDRD(Player pl) {
// dòng 692: public void createFormChooseLevelKGHD(Player pl) {
// dòng 696: public void createFormSenditem1(Player pl) {
// dòng 705: public void createFormBanSLL(Player pl) {
// dòng 709: public void createFormGiaiTanBangHoi(Player pl) {
// dòng 713: public void createFormConSoMayMan_Gold(Player pl) {
// dòng 717: public void createFormConSoMayMan_Gem(Player pl) {
// dòng 721: public void createFormTrongDua(Player player) {
// dòng 725: public void createFormBotQuai(Player pl) {
// dòng 730: public void createFormBotBoss(Player pl) {
// dòng 735: public void createFormBotItem(Player pl) {
// dòng 743: public void createFormBotAttackPlayer(Player pl) {
// dòng 753: public SubInput(String name, byte typeInput) {
```

### [LuckyRound.java](../../SRC/src/nro/models/services_func/LuckyRound.java) — 188 dòng

Kiểu được nhận diện: LuckyRound.

```java
// dòng 35: public static LuckyRound gI() {
// dòng 42: public void openCrackBallUI(Player pl, byte type) {
// dòng 64: public void openCrackBallVipUI(Player pl, byte type) {
// dòng 86: public void readOpenBall(Player player, Message msg) {
// dòng 113: private void openBallByGem(Player player, byte count) {
// dòng 130: private void openBallByGold(Player player, byte count) {
// dòng 147: private void openBallByTicket(Player player, byte count) {
// dòng 167: private void sendReward(Player player, List<Item> items) {
// dòng 185: private void addItemToBox(Player player, List<Item> items) {
```

### [MiniGame.java](../../SRC/src/nro/models/services_func/MiniGame.java) — 31 dòng

Kiểu được nhận diện: MiniGame.

```java
// dòng 16: private MiniGame() {
// dòng 21: public static MiniGame gI() {
```

### [Trade.java](../../SRC/src/nro/models/services_func/Trade.java) — 454 dòng

Kiểu được nhận diện: Trade.

```java
// dòng 52: public Trade(Player pl1, Player pl2) {
// dòng 67: public void openTabTrade() {
// dòng 91: public void addItemTrade(Player pl, byte index, int quantity) {
// dòng 168: private void sendUpdateGoldTrade(Player pl) {
// dòng 189: private void removeItemTrade(Player pl, byte index) {
// dòng 205: private void removeItemTrade2(Player pl, byte index) {
// dòng 220: private boolean isItemCannotTran(Item item) {
// dòng 256: public void cancelTrade() {
// dòng 264: private void closeTab() {
// dòng 279: public void dispose() {
// dòng 292: public void lockTran(Player pl) {
// dòng 346: public void acceptTrade() {
// dòng 353: private void startTrade() {
// dòng 412: private void sendNotifyTrade(byte status) {
// dòng 445: public void addItemBot(Item it) {
// dòng 449: public void update() {
```

### [TransactionService.java](../../SRC/src/nro/models/services_func/TransactionService.java) — 229 dòng

Kiểu được nhận diện: TransactionService.

```java
// dòng 42: private TransactionService() {
// dòng 45: public static TransactionService gI() {
// dòng 53: public void controller(Player pl, Message msg) {
// dòng 180: private void sendInviteTrade(Player plInvite, Player plReceive) {
// dòng 204: public void cancelTrade(Player player) {
// dòng 211: public boolean check(Player player) {
// dòng 216: public void run() {
```

### [UseItem.java](../../SRC/src/nro/models/services_func/UseItem.java) — 2,831 dòng

Kiểu được nhận diện: UseItem.

```java
// dòng 80: private UseItem() {
// dòng 84: public static UseItem gI() {
// dòng 91: public void getItem(MySession session, Message msg) {
// dòng 143: public Item finditem(Player player, int iditem) {
// dòng 152: public void doItem(Player player, Message _msg) {
// dòng 255: private void useItem(Player pl, Item item, int indexBag) {
// dòng 943: public void openRuongGo(Player player) {
// dòng 1079: public int calculateRequiredEmptySlots(int level) {
// dòng 1127: public void RadaNgocRong(Player player, int itemUseiD) {
// dòng 1164: public void RadaNgocRongVip(Player player, int itemUseiD) {
// dòng 1202: public void KhoBauHaiTac(Player player, int itemUseiD) {
// dòng 1240: private int randClothes(int level) {
// dòng 1248: private void changePet(Player player, Item item) {
// dòng 1261: private void changePetRamdom(Player player, Item item) {
// dòng 1297: private void eatGrapes(Player pl, Item item) {
// dòng 1314: private void PhaoBong(Player pl, Item item) {
// dòng 1341: private void PhaoBongVip(Player pl, Item item) {
// dòng 1372: private void KemTraiCay(Player player, Item item) {
// dòng 1415: private void QuaThieuNhi(Player player, Item item) {
// dòng 1461: private void ThucAnChoThan(Player player, Item item) {
// dòng 1491: private void openCSKB(Player pl, Item item) {
// dòng 1520: private void useItemTime(Player pl, Item item) {
// dòng 1706: private void controllerCallRongThan(Player pl, Item item) {
// dòng 1725: private void learnSkill(Player pl, Item item) {
// dòng 1773: private void useTDLT(Player pl, Item item) {
// dòng 1781: private void usePorata2(Player pl) {
// dòng 1792: private void usePorata3(Player pl) {
// dòng 1804: private void usePorata(Player pl) {
// dòng 1816: private void openCapsuleUI(Player pl) {
// dòng 1821: public void choseMapCapsule(Player pl, int index) {
// dòng 1857: public void eatPea(Player player) {
// dòng 1903: private void upSkillPet(Player pl, Item item) {
// dòng 1950: public void OpenHopThanlinh(Player player, int itemUseiD) {
// dòng 2006: public void OpenHopQuaThuong(Player player, int itemUseiD) {
// dòng 2038: public void OpenTrungRongNhi(Player player, int itemUseiD) {
// dòng 2077: private int randomInRange(int min, int max) {
// dòng 2081: public void OpenHopQuaVip(Player player, int itemUseiD) {
// dòng 2113: public void OpenHopQuaGokuDay(Player player, int itemUseiD) {
// dòng 2145: public void OpenHopQuaGokuDayVangNgoc(Player player, int itemUseiD) {
// dòng 2177: public void OpenHopQuaGokuDayVip(Player player, int itemUseId) {
// dòng 2217: public void OpenHopQuaCadic(Player player, int itemUseiD) {
// dòng 2249: private void ItemManhGiay(Player pl, Item item) {
// dòng 2261: private void XuongCho(Player pl, Item item) {
// dòng 2352: public void NoelItemBox(Player pl, Item item) {
// dòng 2444: public void HopQuaChinhChu(Player pl, Item item) {
// dòng 2485: public void HopQuaNheNhang(Player pl, Item item) {
// dòng 2526: public void BlackGokuItemBoxEventNoel(Player pl, Item item) {
// dòng 2576: public void C5(Player pl, Item item) {
// dòng 2603: public void C7(Player pl, Item item) {
// dòng 2630: public void ChuLunBox(Player pl, Item item) {
// dòng 2662: public void NonNoelDo(Player pl, Item item) {
// dòng 2691: private void CapsuleKichHoat(Player pl, Item item) {
// dòng 2695: private void MapRiengTu(Player pl, Item item) {
// dòng 2699: private void VeTangNgoc(Player pl, Item item) {
// dòng 2703: public void LoaTheGioi(Player pl, Item item) {
// dòng 2707: public void TuiVang(Player pl, Item item) {
// dòng 2723: private void ItemSieuThanThuy(Player pl, Item item) {
// dòng 2746: public void UseCard(Player pl, Item item) {
// dòng 2792: private void RuongNgocRong(Player pl, Item item) {
```

## nro.models.shop (12 tệp)

### [ItemShop.java](../../SRC/src/nro/models/shop/ItemShop.java) — 75 dòng

Kiểu được nhận diện: ItemShop.

```java
// dòng 33: public ItemShop() {
// dòng 37: public ItemShop(ItemShop itemShop) {
// dòng 51: public void dispose() {
// dòng 63: public byte getLevelSkill() {
```

### [Shop.java](../../SRC/src/nro/models/shop/Shop.java) — 91 dòng

Kiểu được nhận diện: Shop.

```java
// dòng 27: public Shop() {
// dòng 31: public Shop(Shop shop, Player player) {
// dòng 59: public Shop(Shop shop) {
// dòng 70: public ItemShop getItemShop(int temp) {
// dòng 81: public void dispose() {
```

### [ShopService.java](../../SRC/src/nro/models/shop/ShopService.java) — 1,299 dòng

Kiểu được nhận diện: ShopService.

```java
// dòng 52: public static ShopService gI() {
// dòng 59: public void opendShop(Player player, String tagName, boolean allGender) {
// dòng 100: private Shop getShop(String tagName) throws Exception {
// dòng 109: private Shop resolveShop(Player player, Shop shop, boolean allGender) {
// dòng 116: private Shop resolveShopBua(Player player, Shop s) {
// dòng 170: private void openShopType0(Player player, Shop shop) {
// dòng 225: private void openShopDanhHieu(Player player, Shop shop) {
// dòng 281: private void openShopType1(Player player, Shop shop) {
// dòng 328: private void openShopType3(Player player, Shop shop) {
// dòng 372: private void openShopType4(Player player, String tagName, List<Item> items) {
// dòng 417: private void openShopType8(Player player, String tagName, List<Item> items) {
// dòng 462: public void takeItem(Player player, byte type, int tempId) {
// dòng 498: private boolean subMoneyByItemShop(Player player, ItemShop is) {
// dòng 533: private boolean subMoneyByItemShopV2(Player player, ItemShop is) {
// dòng 575: private void buyItemBua(Player player, int itemTempId) {
// dòng 596: private void learnKyNang(Player pl, ItemShop is) {
// dòng 660: public void buyItem(Player player, int itemTempId) {
// dòng 894: private boolean checkAutoTrainPurchase(Player player, int itemTempId) {
// dòng 907: private void updateAutoTrainPurchase(Player player, int itemTempId) {
// dòng 923: private void buyDanhHieu(Player pl, ItemShop is) {
// dòng 948: private void changeDanhHieu(Player pl, ItemShop is) {
// dòng 969: private boolean subIemByItemShop(Player pl, ItemShop itemShop) {
// dòng 1011: public void showConfirmSellItem(Player pl, int where, int index) {
// dòng 1065: public void sellItem(Player pl, int where, int index) {
// dòng 1132: private void getItemSideBoxLuckyRound(Player player, List<Item> items, byte type, int index) {
// dòng 1178: private void buyItemDaBan(Player player, List<Item> items, byte type, int index) {
// dòng 1217: private void buyItemHD(Player player, int itemTempId) {
// dòng 1287: private Item buyMagicPean(Player player, int[][] listDauThan, Item item) {
```

### [ShopTab.java](../../SRC/src/nro/models/shop/ShopTab.java) — 51 dòng

Kiểu được nhận diện: ShopTab.

```java
// dòng 23: public static void loadItem() {
// dòng 34: private static String q() {
// dòng 48: public static void main(String[] args) {
```

### [TabShop.java](../../SRC/src/nro/models/shop/TabShop.java) — 61 dòng

Kiểu được nhận diện: TabShop.

```java
// dòng 24: public TabShop() {
// dòng 28: public TabShop(TabShop tabShop, int gender) {
// dòng 41: public TabShop(TabShop tabShop) {
// dòng 51: public void dispose() {
```

### [TabShopDanhHieu.java](../../SRC/src/nro/models/shop/TabShopDanhHieu.java) — 56 dòng

Kiểu được nhận diện: TabShopDanhHieu.

```java
// dòng 21: public TabShopDanhHieu(TabShop tabShop, Player player) {
```

### [TabShopHangDoc.java](../../SRC/src/nro/models/shop/TabShopHangDoc.java) — 54 dòng

Kiểu được nhận diện: TabShopHangDoc.

```java
// dòng 18: public TabShopHangDoc(TabShop tabShop, Player player) {
// dòng 45: public int idDauCanBuy(Player player) {
```

### [TabShopHocKynang.java](../../SRC/src/nro/models/shop/TabShopHocKynang.java) — 40 dòng

Kiểu được nhận diện: TabShopHocKynang.

```java
// dòng 18: public TabShopHocKynang(TabShop tabShop, Player player) {
```

### [TabShopMuaAvatar.java](../../SRC/src/nro/models/shop/TabShopMuaAvatar.java) — 53 dòng

Kiểu được nhận diện: TabShopMuaAvatar.

```java
// dòng 18: public TabShopMuaAvatar(TabShop tabShop, Player player) {
// dòng 44: public int idDauCanBuy(Player player) {
```

### [TabShopSanta.java](../../SRC/src/nro/models/shop/TabShopSanta.java) — 53 dòng

Kiểu được nhận diện: TabShopSanta.

```java
// dòng 18: public TabShopSanta(TabShop tabShop, Player player) {
// dòng 44: public int idDauCanBuy(Player player) {
```

### [TabShopSoHuu.java](../../SRC/src/nro/models/shop/TabShopSoHuu.java) — 47 dòng

Kiểu được nhận diện: TabShopSoHuu.

```java
// dòng 20: public TabShopSoHuu(TabShop tabShop, Player player) {
```

### [TabShopUron.java](../../SRC/src/nro/models/shop/TabShopUron.java) — 60 dòng

Kiểu được nhận diện: TabShopUron.

```java
// dòng 17: public TabShopUron(TabShop tabShop, Player player) {
// dòng 51: public int idDauCanBuy(Player player) {
```

## nro.models.shop_ky_gui (3 tệp)

### [ConsignItem.java](../../SRC/src/nro/models/shop_ky_gui/ConsignItem.java) — 41 dòng

Kiểu được nhận diện: ConsignItem.

```java
// dòng 26: public ConsignItem() {
// dòng 29: public ConsignItem(int i, short id, int plId, byte t, int gold, int gem, int q, byte isUp, List<ItemOption> op, boolean b) {
```

### [ConsignShopManager.java](../../SRC/src/nro/models/shop_ky_gui/ConsignShopManager.java) — 48 dòng

Kiểu được nhận diện: ConsignShopManager.

```java
// dòng 20: public static ConsignShopManager gI() {
// dòng 33: public void save() {
```

### [ConsignShopService.java](../../SRC/src/nro/models/shop_ky_gui/ConsignShopService.java) — 528 dòng

Kiểu được nhận diện: ConsignShopService.

```java
// dòng 30: public static ConsignShopService gI() {
// dòng 37: private List<ConsignItem> getItemKyGui2(Player pl, byte tab, byte to, byte max) {
// dòng 50: private List<ConsignItem> getItemKyGui(Player pl, byte tab, byte... max) {
// dòng 75: private List<ConsignItem> getItemKyGui() {
// dòng 82: private boolean isKyGui(Item item) {
// dòng 105: private boolean SubThoiVang(Player pl, int quatity) {
// dòng 115: public void buyItem(Player pl, int id) {
// dòng 165: public ConsignItem getItemBuy(int id) {
// dòng 174: public ConsignItem getItemBuy(Player pl, int id) {
// dòng 183: public void openShopKyGui(Player pl, byte index, int page) {
// dòng 235: public void upItemToTop(Player pl, int id) {
// dòng 250: public void StartupItemToTop(Player pl) {
// dòng 270: public void claimOrDel(Player pl, byte action, int id) {
// dòng 323: public List<ConsignItem> getItemCanKiGui(Player pl) {
// dòng 334: public boolean itemCanConsign(Item it) {
// dòng 348: public int getMaxId() {
// dòng 366: public byte getTabKiGui(Item it) {
// dòng 378: public void KiGui(Player pl, int id, int money, byte moneyType, int quantity) {
// dòng 441: public void openShopKyGui(Player pl) {
```

## nro.models.skill (3 tệp)

### [NClass.java](../../SRC/src/nro/models/skill/NClass.java) — 47 dòng

Kiểu được nhận diện: NClass.

```java
// dòng 22: public SkillTemplate getSkillTemplate(int tempId) {
// dòng 30: public List<Skill> getSkills(int tempId) {
// dòng 38: public SkillTemplate getSkillTemplateByName(String name) {
```

### [PlayerSkill.java](../../SRC/src/nro/models/skill/PlayerSkill.java) — 110 dòng

Kiểu được nhận diện: PlayerSkill.

```java
// dòng 21: public PlayerSkill(Player player) {
// dòng 26: public Skill getSkillbyId(int id) {
// dòng 37: public void sendSkillShortCut() {
// dòng 65: public byte getIndexSkillSelect() {
// dòng 86: public byte getSizeSkill() {
// dòng 96: public void dispose() {
```

### [Skill.java](../../SRC/src/nro/models/skill/Skill.java) — 99 dòng

Kiểu được nhận diện: Skill.

```java
// dòng 76: public Skill() {
// dòng 80: public Skill(Skill skill) {
// dòng 96: public void dispose() {
```

## nro.models.task (10 tệp)

### [BadgesTask.java](../../SRC/src/nro/models/task/BadgesTask.java) — 52 dòng

Kiểu được nhận diện: BadgesTask.

```java
// dòng 22: public BadgesTask() {
// dòng 29: public boolean isDone() {
// dòng 33: public int getPercentProcess() {
// dòng 45: public String toString() {
```

### [BadgesTaskService.java](../../SRC/src/nro/models/task/BadgesTaskService.java) — 79 dòng

Kiểu được nhận diện: BadgesTaskService.

```java
// dòng 17: public static void createAndResetTask(Player player) {
// dòng 29: public static void updateDoneTask(Player player) {
// dòng 44: public static void updateCountBagesTask(Player player, int id, int amount) {
// dòng 56: public static int sendPercenBadgesTask(Player player, int idBadgesReward) {
// dòng 69: public static int sendDay(Player player, int id) {
```

### [BadgesTaskTemplate.java](../../SRC/src/nro/models/task/BadgesTaskTemplate.java) — 23 dòng

Kiểu được nhận diện: BadgesTaskTemplate.

```java
// dòng 16: public BadgesTaskTemplate() {
```

### [ClanTask.java](../../SRC/src/nro/models/task/ClanTask.java) — 98 dòng

Kiểu được nhận diện: ClanTask.

```java
// dòng 37: public void reset() {
// dòng 53: public ClanTask() {
// dòng 57: public void renew() {
// dòng 64: public boolean isDone() {
// dòng 68: public String getName() {
// dòng 75: public String getLevel() {
// dòng 92: public int getPercentProcess() {
```

### [ClanTaskTemplate.java](../../SRC/src/nro/models/task/ClanTaskTemplate.java) — 19 dòng

Kiểu được nhận diện: ClanTaskTemplate.

```java
// dòng 15: public ClanTaskTemplate() {
```

### [SideTask.java](../../SRC/src/nro/models/task/SideTask.java) — 98 dòng

Kiểu được nhận diện: SideTask.

```java
// dòng 37: public void reset() {
// dòng 53: public SideTask() {
// dòng 57: public void renew() {
// dòng 64: public boolean isDone() {
// dòng 68: public String getName() {
// dòng 75: public String getLevel() {
// dòng 92: public int getPercentProcess() {
```

### [SideTaskTemplate.java](../../SRC/src/nro/models/task/SideTaskTemplate.java) — 19 dòng

Kiểu được nhận diện: SideTaskTemplate.

```java
// dòng 15: public SideTaskTemplate() {
```

### [SubTaskMain.java](../../SRC/src/nro/models/task/SubTaskMain.java) — 35 dòng

Kiểu được nhận diện: SubTaskMain.

```java
// dòng 23: public SubTaskMain() {
// dòng 26: public SubTaskMain(SubTaskMain stm) {
```

### [TaskMain.java](../../SRC/src/nro/models/task/TaskMain.java) — 41 dòng

Kiểu được nhận diện: TaskMain.

```java
// dòng 26: public TaskMain() {
// dòng 30: public TaskMain(TaskMain taskMain) {
```

### [TaskPlayer.java](../../SRC/src/nro/models/task/TaskPlayer.java) — 28 dòng

Kiểu được nhận diện: TaskPlayer.

```java
// dòng 17: public TaskPlayer() {
// dòng 22: public void dispose() {
```

## nro.models.utils (9 tệp)

### [FileIO.java](../../SRC/src/nro/models/utils/FileIO.java) — 110 dòng

Kiểu được nhận diện: FileIO.

```java
// dòng 19: public static byte[] readFile(String url) {
// dòng 32: public static ByteArrayOutputStream loadFile(String url) {
// dòng 54: public static void writeFile(String url, byte[] ab) {
// dòng 69: public static void writeFile(String url, String content) {
// dòng 92: public static List<File> getFiles(String folderPath) {
```

### [FileRunner.java](../../SRC/src/nro/models/utils/FileRunner.java) — 18 dòng

Kiểu được nhận diện: FileRunner.

```java
// dòng 13: public static void runBatchFile(String batchFilePath) throws IOException {
```

### [Functions.java](../../SRC/src/nro/models/utils/Functions.java) — 46 dòng

Kiểu được nhận diện: Functions.

```java
// dòng 18: public static boolean isSpam(Player player, String text) {
// dòng 22: public static int maxint(long n) {
// dòng 26: public static String generateRandomCharacters(int quantity) {
// dòng 38: public static void sleep(long l) {
```

### [Logger.java](../../SRC/src/nro/models/utils/Logger.java) — 193 dòng

Kiểu được nhận diện: Logger.

```java
// dòng 95: public static void log(String text) {
// dòng 99: public static void logln(String text) {
// dòng 103: public static void log(String color, String text) {
// dòng 107: public static void logln(String color, String text) {
// dòng 111: public static void success(String text) {
// dòng 114: public static void ThongBao(String text) {
// dòng 118: public static void successln(String text) {
// dòng 122: public static void warning(String text) {
// dòng 126: public static void warningln(String text) {
// dòng 130: public static void error(String text) {
// dòng 134: public static void errorln(String text) {
// dòng 138: public static void primary(String text) {
// dòng 142: public static void primaryln(String text) {
// dòng 146: public static void logException(Class<?> clazz, Exception ex, String... log) {
// dòng 173: public static void fileLog(String playerName, String string) {
// dòng 185: private static void writeFile(String filePath, String content) throws IOException {
```

### [SkillUtil.java](../../SRC/src/nro/models/utils/SkillUtil.java) — 324 dòng

Kiểu được nhận diện: SkillUtil.

```java
// dòng 28: public static Skill createSkill(int tempId, int level) {
// dòng 40: public static SkillTemplate findSkillTemplate(int tempId) {
// dòng 51: public static List<Skill> findSkills(int tempId) {
// dòng 62: public static List<Skill> findPointSkill(int skillId) {
// dòng 68: public static Skill createEmptySkill() {
// dòng 74: public static Skill createSkillLevel0(int tempId) {
// dòng 81: public static boolean isUseSkillDam(Player player) {
// dòng 88: public static boolean isUseSkillChuong(Player player) {
// dòng 93: public static int getTimeMonkey(int level) { //thời gian tồn tại khỉ v
// dòng 97: public static int getPercentHpMonkey(int level) { //tỉ lệ máu khỉ cộng thêm v
// dòng 101: public static int getPercentDameMonkey(int level) { //tỉ lệ dam khỉ cộng thêm v
// dòng 105: public static int getTimeStun(int level) { //thời gian choáng thái dương hạ san v
// dòng 109: public static int getTimeSocola() {
// dòng 113: public static int getTimeShield(int level) { //thời gian tồn tại khiên v
// dòng 117: public static int getTimeTroi(int level) { //thời gian trói v
// dòng 121: public static int getTimeDCTT(int level) { //thời gian choáng dịch chuyển tức thời v
// dòng 125: public static int getTimeThoiMien(int level) { //thời gian thôi miên
// dòng 129: public static int getRangeStun(int level) { //phạm vi thái dương hạ san
// dòng 133: public static int getRangeBom(int level) { //phạm vi tự sát
// dòng 137: public static int getRangeQCKK(int level) { //phạm vi quả cầu kênh khi
// dòng 141: public static int getPercentHPHuytSao(int level) { //tỉ lệ máu huýt sáo cộng thêm v
// dòng 145: public static int getPercentTriThuong(int level) { //tỉ lệ máu hồi phục trị thương v
// dòng 149: public static int getPercentCharge(int level) { //tỉ lệ hp ttnl
// dòng 153: public static int getTempMobMe(int level) {
// dòng 158: public static int getTimeSurviveMobMe(int level) { //thời gian trứng tồn tại
// dòng 162: public static long getHPMobMe(long hpMaxPlayer, int level) {
// dòng 167: public static Skill getSkillbyId(Player player, int id) {
// dòng 176: public static boolean upSkillPet(List<Skill> skills, int index) {
// dòng 198: public static byte getTempSkillSkillByItemID(int id) {
// dòng 248: public static Skill getSkillByItemID(Player pl, int tempId) {
// dòng 298: public static void setSkill(Player pl, Skill skill) {
// dòng 312: public static byte getTyleSkillAttack(Skill skill) {
```

### [StringUtil.java](../../SRC/src/nro/models/utils/StringUtil.java) — 19 dòng

Kiểu được nhận diện: StringUtil.

```java
// dòng 7: public static String randomText(int length) {
```

### [SystemMetrics.java](../../SRC/src/nro/models/utils/SystemMetrics.java) — 88 dòng

Kiểu được nhận diện: SystemMetrics.

```java
// dòng 16: private static String formatMemory(double used, double total) {
// dòng 20: private static String formatPercentage(double percentage) {
// dòng 24: private static double getUsedMemoryGB() {
// dòng 32: private static double getTotalMemoryGB() {
// dòng 38: private static double getRAMUsagePercentage() {
// dòng 46: private static double getCPUUsagePercentage() {
// dòng 52: private static double getHeapUsedMemoryGB() {
// dòng 59: private static double getHeapMaxMemoryGB() {
// dòng 66: private static double getHeapUsagePercentage() {
// dòng 74: public static String ToString() {
```

### [TimeUtil.java](../../SRC/src/nro/models/utils/TimeUtil.java) — 269 dòng

Kiểu được nhận diện: TimeUtil.

```java
// dòng 38: public static String convertMillisecondToMinute(long time) {
// dòng 42: public static String convertMillisecondToHour(long time) {
// dòng 46: public static String convertMillisecondToDay(long time) {
// dòng 50: public static long diffDate(Date d1, Date d2, byte type) {
// dòng 72: public static boolean isTimeNowInRange(String fromTime, String toTime, String format) throws Exception {
// dòng 84: public static int getCurrDay() {
// dòng 88: public static int getCurrHour() {
// dòng 92: public static int getCurrMin() {
// dòng 96: public static String convertTime(int totalSeconds) {
// dòng 120: public static String getTimeLeft(long startTime, int targetSeconds) {
// dòng 126: public static String getTimeLeft(long lastTime) {
// dòng 134: public static int getMinLeft(long lastTime, int targetSeconds) {
// dòng 140: public static int getSecondLeft(long lastTime, int targetSeconds) {
// dòng 145: public static String getDateLeft(long lastTime, int targetSeconds) {
// dòng 150: public static String convertTimeNow(long lastTime) {
// dòng 155: public static long getTime(String timeStr, String format) throws Exception {
// dòng 164: public static String getTime(long millis) {
// dòng 179: public static String getTimeNow(String format) {
// dòng 184: public static String getTimeBeforeCurrent(int subtractMillis, String format) {
// dòng 189: public static String formatTime(Date time, String format) {
// dòng 194: public static String formatTime(long millis, String format) {
// dòng 199: public static boolean isMabuOpen() {
// dòng 205: public static boolean isMabu14HOpen() {
// dòng 211: public static boolean is21H() {
// dòng 217: public static long getStartTimeBlackBallWar() {
// dòng 224: public static boolean isBlackBallWarOpen() {
// dòng 232: public static boolean isBlackBallWarCanPick() {
// dòng 239: public static long getSecondsUntilCanPick() {
// dòng 251: public static boolean checkTime(long time) {
// dòng 255: public static String convertTimeMS(int totalSeconds) {
// dòng 261: public static int getCurrMinute() {
// dòng 265: public static int getCurrSecond() {
```

### [Util.java](../../SRC/src/nro/models/utils/Util.java) — 599 dòng

Kiểu được nhận diện: Util.

```java
// dòng 40: public static int createIdBossClone(int idPlayer) {
// dòng 44: public static boolean contains(String[] arr, String key) {
// dòng 48: public static String numberToMoney(long power) {
// dòng 63: public static String convertSecondsToTime(long totalSeconds) {
// dòng 78: public static String convertMilliseconds(long milliseconds) {
// dòng 86: public static byte getHead(byte gender) {
// dòng 97: public static String msToThang(long ms) {
// dòng 134: public static String powerToString(long power) {
// dòng 149: public static int getDistance(int x1, int y1, int x2, int y2) {
// dòng 153: public static int getDistance(Player pl1, Player pl2) {
// dòng 157: public static int getDistance(Player pl, Npc npc) {
// dòng 161: public static int getDistance(Player pl, Mob mob) {
// dòng 165: public static int getDistance(Mob mob1, Mob mob2) {
// dòng 169: public static int nextInt(int from, int to) {
// dòng 173: public static int nextInt(int max) {
// dòng 177: public static long nextLong(long from, long to) {
// dòng 181: public static long nextLong(long max) {
// dòng 185: public static int nextInt(int[] percen) {
// dòng 196: public static int getOne(int n1, int n2) {
// dòng 200: public static String replace(String text, String regex, String replacement) {
// dòng 204: public static boolean isTrue(long ratioPercentage, long totalPercentage) {
// dòng 209: public static boolean isTrue(float ratioPercentage, long totalPercentage) {
// dòng 217: public static boolean isTrue(long ratioPercentage, long totalPercentage, int accuracy) {
// dòng 221: public static int highlightsItem(boolean highlights, int value) {
// dòng 226: public static boolean isTrue(float ratioPercentage, long totalPercentage, int accuracy) {
// dòng 234: public static String tinhgio(long ms) {
// dòng 263: public static String format(double power) {
// dòng 267: public static boolean haveSpecialCharacter(String text) {
// dòng 274: public static boolean canDoWithTime(long lastTime, long miniTimeTarget) {
// dòng 302: public static char removeAccent(char ch) {
// dòng 310: public static String removeAccent(String str) {
// dòng 318: public static Object[] addArray(Object[]... arrays) {
// dòng 332: public static int randomBossId() {
// dòng 340: public static boolean isAfterMidnight(long currenttimemillis) {
// dòng 349: public static boolean isTimeDifferenceGreaterThanNDays(long setTime, int nDays) {
// dòng 356: public static String formatNumber(long j) {
// dòng 398: public static void threadPool(Runnable task) {
// dòng 411: public static String formatBytes(long bytes) {
// dòng 423: public static void setTimeout(Runnable runnable, int delay) {
// dòng 433: public static String addSlashes(String input) {
// dòng 445: public static boolean isNumeric(String str) {
// dòng 457: public static String roundToTwoDecimals(double num) {
// dòng 463: public static String dvp(long j) {
// dòng 505: public static String dvp2(double n) {
// dòng 518: public static synchronized boolean compareDay(Date now, Date when) {
// dòng 529: public static String mumberToBlue(long number) {
// dòng 534: public static String toDateString(Date date) {
// dòng 545: public static String mumberTo(long number) {
// dòng 550: public static String numberFormat(long number) {
// dòng 555: public static ItemMap spl(Zone zone, int tempId, int quantity, int x, int y, long playerId) {
// dòng 588: public static boolean isSameDay(long time1, long time2) {
```
