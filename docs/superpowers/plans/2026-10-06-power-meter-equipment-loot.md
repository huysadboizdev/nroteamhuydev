# Máy đo đứng yên và rơi đồ theo trang bị — kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Máy đo sức mạnh không tự nhảy/quay; đủ set Thần Linh rơi thức ăn tại Tương Lai 20%, Cold 60%; đủ set Hủy Diệt rơi mảnh Thiên Sứ tại Ngục Tù 100%, x1 hoặc x20 khi buff Cỏ bốn lá còn hiệu lực.

**Architecture:** Khóa chuyển động riêng mob 117 ở packet, cấu hình và cả hai client; giữ nguyên cơ chế nhận sát thương, hồi HP và tính điểm. Quy tắc loot riêng tập trung xác định chủ nhân, set đang mặc, vùng map, tỷ lệ và số lượng; thay đúng nhánh cũ trong Mob, không thêm song song gây rơi hai lần.

**Tech Stack:** Java 17 server; Unity C# Game1/Game2; MySQL/MariaDB; binary mob resources x1–x4.

Kế hoạch theo yêu cầu người dùng; chưa sửa src, SQL live, client hoặc JAR cho thay đổi này.

## Kết quả kiểm tra hiện trạng

- `mob_template.id=117`: TYPE=1, range_move=1, speed=1, HP=2.000.000.000. `Zone.sendMapInfo` gửi `is dont move=false` cho mọi mob.
- Máy đo thuộc server `MayDoSucManh extends BigBoss`; `Mob.attack` đã loại 117 khỏi tấn công. Không có lệnh di chuyển tự động riêng trong lớp máy đo.
- Client nhận 117 dưới lớp `Mob` thường: cờ NewBoss chỉ dành cho 77, 82–85. Res 117 x1–x4 có `typeData=0`, không có bảng frame NewBoss; không được đổi máy thành NewBoss.
- `Mob.updateMobStandWait` chuyển sang đi; `updateMobWalk` di chuyển rồi đổi hướng khi vượt range=1, tạo đổi hướng liên tục. `updateInjure` đổi hướng theo người đánh; `startDie` bật vận tốc nảy. Đây là các đường mã cần khóa riêng cho máy đo. Cần xem trực tiếp Unity khi triển khai để phân biệt chuyển tọa độ và frame hình vẽ.
- `Mob.getItemMobReward`: thức ăn 663–667 hiện kiểm tra set thần rồi roll 3/333 (~0,90%), không giới hạn riêng Tương Lai/Cold. Roll cũ qua `isLuckyDrop` có thêm luck và set Gohan.
- Mảnh Thiên Sứ 1066–1070 hiện cần set Hủy Diệt/map155, roll 10% và thêm nhánh tài khoản active 2/555; số lượng luôn 1. Item1229 là Bí kíp tuyệt kỹ, không phải mảnh Thiên Sứ; giữ nhánh này độc lập.
- `checkSetGod/checkSetDes` chỉ kiểm tra ID trong khoảng ở năm ô, chưa đủ an toàn với món sai slot/quantity/hết hạn. `InventoryService.fullSetThan/validDivineBodySlots` đã kiểm tra đúng món, slot, hành tinh, level13, hạn dùng.
- Chuyển pet sang master hiện chỉ nằm trong nhánh Cold; cần chuẩn hóa riêng quy tắc mới để đệ kết liễu ở Tương Lai/Ngục Tù xét bộ đồ của sư phụ.
- Cỏ bốn lá ID1635 có timer 30 phút, các trường `isUseCoBonLa`, `lastTimeUseCoBonLa`, `timeLengthCoBonLa`. Quy tắc x20 phải kiểm tra thời gian thực, không chỉ boolean.
- SQL đã có 5 thức ăn, 5 mảnh; đủ 40 icon PNG x1–x4. Không cần tạo ID mới hoặc part nhân vật cho loot.

## Hành vi dự kiến

| Trang bị đang mặc | Khu vực | Xác suất mỗi quái thường chết | Vật phẩm/số lượng |
|---|---|---:|---|
| Đủ 5 món Thần Linh đúng ô, còn hạn | Tương Lai | 20% | Random 663–667, x1 |
| Đủ 5 món Thần Linh đúng ô, còn hạn | Cold | 60% | Random 663–667, x1 |
| Đủ 5 món Hủy Diệt đúng ô, còn hạn | Hành tinh Ngục Tù 155 | 100% | Random 1066–1070, x1 |
| Như trên, Cỏ bốn lá đang còn thời gian | Ngục Tù 155 | 100% | Random 1066–1070, x20 |

- Đủ set nghĩa là áo, quần, găng, giày, rada/nhẫn trong năm ô trang bị của người chơi; đồ trong túi/rương không tính. Thiếu một món, trộn bộ hoặc món hết hạn không đủ điều kiện.
- Một lần là một quái thường chết do người chơi/đệ của họ kết liễu; không rơi theo từng hit, không cấp thêm từ máy đo, không tự thêm vào các boss có thưởng riêng.
- Tỷ lệ 20/60/100 là tỷ lệ cố định của quy tắc này; không nhân luck/Gohan/Cỏ bốn lá vào xác suất. Cỏ chỉ đổi số mảnh x1 thành x20; không đổi số thức ăn.
- Tương Lai theo predicate int hiện dùng: 92–94, 96–100, 102–103. Cold:105–110. Map152 có khác biệt giữa predicate Map và int; kế hoạch dùng vùng Cold đang áp dụng cho loot 105–110, không âm thầm thêm152.
- Mỗi lần trúng roll chọn đều một trong năm loại; người sở hữu item là người chơi/sư phụ, dùng quy tắc nhặt đồ hiện có.

## Phương án và lựa chọn

1. **Khuyến nghị: server + SQL + client đồng bộ riêng 117.** Khóa vị trí/hướng cả lúc đứng, trúng đòn và hồi sinh; giữ đo sát thương. Loot gom riêng thành một service nhỏ. Bao phủ đúng yêu cầu và kiểm thử được.
2. Chỉ sửa SQL TYPE/range/speed: ít thay đổi nhưng không chặn toàn bộ hướng/animation trúng đòn của client, không đủ đáp ứng “không nhảy và xoay”.
3. Thay bằng NPC tĩnh: đứng yên nhưng phải viết lại chọn mục tiêu/nhận sát thương/tính điểm, vượt phạm vi; không chọn.

## Task 1: Khóa máy đo sức mạnh

**Files:**
- Modify: `SRC/src/nro/models/map/Zone.java` (packet map cho mob117).
- Review/modify when needed: `SRC/src/nro/models/mob_bigboss/MayDoSucManh.java` (giữ tọa độ gốc, không thay công thức điểm).
- Modify: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mob.cs` và `Game2/Mob.cs`; `Game1/Controller.cs` và `Game2/Controller.cs` nếu cần giữ nguyên hướng khi tạo/reset mob.
- Create: `SRC/sql/migrations/20261006_power_meter_stationary.sql`.
- Sync: `team2026.sql`, `SRC/sql/nro1.sql`, `SRC/sql/team2026.seed.sql`, `SRC/sql/nro1.seed.sql`.
- Test: `SRC/test/nro/models/mob/PowerMeterStationaryTest.java`; client test/harness mô phỏng đứng, trúng đòn, chết/hồi sinh.

- [ ] Tạo test đỏ: packet117 phải có cờ đứng yên; quái khác giữ cờ cũ. Mô phỏng người đánh từ hai phía, 600 tick, ghi x/y/dir và đảm bảo không thay đổi khi máy sống.
- [ ] SQL riêng `UPDATE mob_template SET TYPE=0,range_move=0,speed=0 WHERE id=117;`; packet dùng điều kiện `mob.tempId == ConstMob.MAY_DO_SUC_MANH` cho cờ đứng yên.
- [ ] Client chỉ nhánh `templateId==117`: cố định x/y tại vị trí tạo máy, hướng khởi tạo ổn định; bỏ phản ứng đổi hướng, đẩy lùi, nhảy chết. Giữ HP bar, damage popup, chọn mục tiêu và nhận trạng thái sống/chết server. Đảm bảo xử lý sau hồi sinh cũng cố định.
- [ ] Không đổi máy thành NewBoss. Nếu sprite đứng có frame nhảy riêng, giữ frame đứng đã đối chiếu từ res117, không sửa animation tất cả mob.
- [ ] Đối chiếu điểm/HP trước và sau, kiểm thử máy ở mọi map đang đặt, đi khu/quay lại, người chơi nhiều phía. Không sửa lỗi điểm khác nếu không được xác nhận nằm trong nhiệm vụ.

## Task 2: Quy tắc đủ set và loot

**Files:**
- Create: `SRC/src/nro/models/services/EquipmentFarmDropService.java`.
- Modify: `SRC/src/nro/models/mob/Mob.java` (thay nhánh food 3/333 và shard cũ).
- Reuse: `InventoryService.fullSetThan`, `InventoryService.isBillIngredientUsable`, `MapService` predicates.
- Test: `SRC/test/nro/models/services/EquipmentFarmDropTest.java`.

- [ ] Test đỏ với nguồn random kiểm soát: roll0–19 rơi food tương lai,20–99 không; Cold0–59 rơi,60–99 không. Ngục Tù đủ set luôn rơi.
- [ ] Xét chủ nhân hợp lệ rồi kiểm tra bộ đang mặc dưới khóa người chơi. Set thần dùng `fullSetThan`; bộ Hủy Diệt tương ứng thần +95 theo từng slot/hành tinh (650–662, level14), cùng kiểm tra item còn hạn/quantity1/type đúng/gender đúng; không dùng chỉ khoảng ID.
- [ ] Xử lý đồ thiếu, null, list dưới5ô, trộn bộ, sai ô, sai hành tinh, quantity0/2 và hết hạn trả không đủ set, không ném exception.
- [ ] Food chọn 663–667 x1 chỉ ở đúng hai vùng. Thay nhánh toàn-map cũ để không rơi thêm lần thứ hai. Shard chọn1066–1070 với quantity=`activeClover ? 20 : 1`, không giữ roll shard cũ.
- [ ] Cỏ active khi boolean bật, duration>0 và `now-lastTimeUseCoBonLa < timeLengthCoBonLa`, đồng thời timestamp không ở tương lai. Kiểm thử trước, đúng, sau thời điểm hết hạn và sau logout/login.
- [ ] Chuẩn hóa đệ đánh theo set/buff/master ở cả ba vùng; kiểm tra người chơi thật, không boss/bot/máy đo; đặt playerId của loot về chủ nhân.
- [ ] Gọi đúng một lần từ luồng chết hiện tại, giữ đồ khác và Bí kíp1229. Test lặp handler/kết liễu đồng thời không sinh thêm lượt thưởng ngoài một lần chết; kiểm tra nguyên tử sẵn có của Mob và bổ sung chặn nếu cần.

## Task 3: Xác minh và triển khai sau khi duyệt

- [ ] Compile Java --release17, chạy test máy, set, vùng, tỷ lệ, buff, pet, món cũ; compile cả hai client Unity, không chỉ kiểm tra chuỗi source.
- [ ] Kiểm tra PNG thực tế 40 ảnh thức ăn/mảnh và 4 resource117, frame đứng, đường server `data/mob/x{zoom}/117` và `data/icon/x{zoom}/{id}.png`.
- [ ] Mô phỏng nhiều quái trên cùng map để xác nhận không thêm scheduler/thread mỗi lần kill hoặc mỗi packet; test ngẫu nhiên biên thay vì chỉ thống kê may rủi.
- [ ] Migration idempotent, không xóa item/mob/map; đồng bộ full dumps và tracked seeds; version dữ liệu mob nếu cần để client cập nhật TYPE/speed/range.
- [ ] Khi cài: bảo trì lưu dữ liệu, xác nhận JVM dừng; SQL snapshot; JAR cũ sang `SRC/jar-backup` tên riêng có thời gian/hash/metadata; cài bản đã test và chỉ giữ JAR đang dùng trong SRC.
- [ ] Khởi động lại, probe đọc template117 và rule version; xác minh server14445. Kiểm tra trực tiếp Unity máy đứng yên trong/sau giao chiến, dùng Cỏ rồi thấy đúng1stack20mảnh thay20itemmap riêng.

**Tiêu chí hoàn thành:** Không thay x/y/dir của máy khi sống, trúng đòn hoặc hồi sinh; đo sát thương vẫn hoạt động. Tỷ lệ và số lượng đúng bảng, thiếu bộ không rơi đồ chuyên biệt, đệ áp dụng set sư phụ. Source/SQL/client/res thống nhất; có chứng cứ compile/test/runtime và backup.
