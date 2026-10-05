# Kế hoạch Capsule hồng ngọc — Tiểu đội sát thủ Namek

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mỗi boss Tiểu đội sát thủ Namek rơi5 Capsule hồng ngọc chung, mở nhận ruby1–100 theo tỷ lệ80%/20%; giữ cải trang/ngọc rồng chỉ cho killer.

**Architecture:** Dùng item722 hiện có, tách roll/mở/drop capsule vào service nhỏ. Thêm quyền owner giữ đến hết hạn cho riêng drop killer và guard mỗi lượt sống của5boss Namek. KOL pickup hook hiện có được giữ.

**Tech Stack:** Java21, MySQL item_template, hai dump SQL, JAR20 đang dùng, client protocol hiện có.

**Spec:** [Thiết kế đã làm rõ](../specs/2026-10-04-namek-pink-capsule-design.md). Tỷ lệ80/20 và capsule dùng chung đã được người dùng chốt, sau đó yêu cầu triển khai. Workspace không Git: dùng backup có timestamp/SHA-256, không tạo commit giả.

## Trạng thái triển khai — 2026-10-04

- [x] Tasks1–5: source, kiểm thử capsule/quyền nhặt/drop, tên và mô tả SQL đã triển khai; spec và quality review chấp thuận.
- [x] Task6 tự động: candidate và JAR cài đặt đạt58/58 nhóm (14 mới +44 hồi quy), migration chỉ đổi name/description722, backup/MOVE JAR/SHA-256, khởi động PID17612 cổng14445 và probe catalog14/service/owner flag/text đều đạt.
- [x] Metadata và [báo cáo triển khai](../../source-analysis/namek-pink-capsule.md) đã ghi, có hướng dẫn rollback.
- [ ] Gameplay client thực tế: chưa thực hiện. Không dùng test mô phỏng để đánh dấu nghiệm thu chơi thật.

Điều chỉnh kỹ thuật đã kiểm chứng: thêm constructor ItemMap có tham số quyền private để đặt cờ/owner trước khi publish; khóa player riêng cho pickup722; case722 trong UseItem xử lý task thành công dưới khóa player rồi return trước tail refresh chung. Các test bổ sung RED/GREEN xác nhận race đã được sửa. Checklist chi tiết bên dưới giữ nguyên làm tiêu chí đối chiếu; trạng thái trên phản ánh phần đã hoàn thành.

## Task1: Snapshot và kiểm thử roll

**Create:** `SRC/src/nro/models/services/PinkCapsuleService.java`, `SRC/test/nro/models/services/PinkCapsuleTest.java`.

- [ ] Backup các file ở tasks2–5, haiSQLdump và snapshot live rowitem722; ghi hash. Đọc activeJAR/runtimePID/workdir, không thay bản đang chạy.
- [ ] Viết test RED cho API `static int rollRuby(IntUnaryOperator randomBounded)`; randomBounded nhận N, trả0..N-1. Duyệt roll nhóm0..99:80 kết quả thuộc1–5,20 thuộc6–100. Kiểm tra ranh giới group79/80 và số1/5/6/100.
- [ ] Triển khai thuật toán sau, không dùng `Util.nextInt(1,100)` trực tiếp vì sẽ thành phân phối đều:

```java
public static int rollRuby(java.util.function.IntUnaryOperator randomBounded) {
    return randomBounded.applyAsInt(100) < 80
        ? 1 + randomBounded.applyAsInt(5)
        : 6 + randomBounded.applyAsInt(95);
}
```

- [ ] Production gọi `rollRuby(Util::nextInt)` (overload nextInt(max) loại trừ max). Kiểm tra tổng kỳ vọng13 và xác suất từng mức cao bằng nhau; không dùng kiểm thử random không seed làm gate dễ flaky.

## Task2: Mở một capsule

**Modify:** `SRC/src/nro/models/services_func/UseItem.java`.
**Extend:** `PinkCapsuleService.java`, `PinkCapsuleTest.java`.

- [ ] Test RED mởstack1/2, item saiID/đã rời bag/quantity0, ruby gầntràn, đồng thời stack1, gửi packet thất bại sau sửaRAM. Xác nhận ruby tăng, gem/VND/điểm không tăng, KOLcounter không tăng/giảm khi dùng.
- [ ] Thêm API `boolean open(Player player, Item requestedItem)`: dưới `synchronized(player)` xác nhận `requestedItem` còn là item trong `inventory.itemsBag`, ID722, quantity>0; rollreward, kiểm tra `0 <= ruby` và `(long)ruby+reward <= Integer.MAX_VALUE`; vượtgiới hạn thì giữ capsule và báo lỗi. Trừ đúng1 bằng InventoryService, cộng ruby, hoàn tất mutation trước `sendItemBags/sendMoney/sendThongBao`. Không tự retry vì gửi packet lỗi.
- [ ] Dispatch `case ConstItem.CAPSULE_HONG` tại switch template.id trong nhánh default của UseItem.useItem, gọi service.open và break; không thay type27 hoặc controllerCapsuleUI của193/194.
- [ ] Kiểm tra autosave inventory JSON đã chứa ruby và stack; nạp lại giữ dữ liệu. Ghi rõ đây là persistence hiện có, không mở rộng crash ledger.

## Task3: Quyền killer giữ đến hết hạn

**Modify:** `SRC/src/nro/models/map/ItemMap.java`, `SRC/src/nro/models/map/Zone.java`.
**Test:** `SRC/test/nro/models/map/NamekDropOwnershipTest.java`.

- [ ] Test RED owner/nonowner lúc0s/46s; item khác vẫn trở thành public ở45s; hết hạn vẫn bị gỡ; capsule public ngay; copy constructor giữ cờ.
- [ ] Thêm `boolean keepOwnerUntilDespawn` mặc địnhfalse và sao chép cờ trong ItemMap(ItemMap). Trong update chỉ gán playerId=-1 khi đủ45s **và cờfalse**; giữ điều kiện despawn50s hiện tại.
- [ ] Khi cờtrue, Zone.pickItem chỉ chấp nhận `itemMap.playerId == player.id`. Khi cờfalse giữ predicate legacy (bao gồm owner alias và-1). Không thay khóa itemMap hay thứ tự pickup/KOLhook.
- [ ] Cờ chỉ được bật cho costume429–433 và dragonball19/20 của drop5bossNamek ởtask4. Không bật global theo itemID vì item cùngID từ nguồnkhác phải giữ behaviorcũ.

## Task4: Drop5 viên chung đúng nhóm

**Modify:** `SRC/src/nro/models/boss/tieu_doi_sat_thu_namek/SO1_NM.java`, `SO2_NM.java`, `SO3_NM.java`, `SO4_NM.java`, `TDT_NM.java`.
**Extend:** `PinkCapsuleService.java`.
**Test:** `SRC/test/nro/models/boss/NamekPinkCapsuleDropTest.java`.

- [ ] Test RED mỗi5class đúng5 itemMap722 qty1 owner-1; rơi thành5điểm hợp lệ tại mapyPhysic; killer/nonkiller đều nhặt; costume/ngọc rồng ownerkiller/cờtrue; vàng77/điểm giữ nguyên. Đội boss thường không nhận dropmới.
- [ ] Thêm API `void dropShared(Boss boss)` tạo vònglặp i0..4, clampx vào map rồi lấy yPhysicInTop tại x đã tính, tạo `new ItemMap(zone,722,1,x,y,-1)` và broadcast `Service.dropItemMap`. Không thêm itemMap lần2 vì constructor đã add vào zone. Không tạo stack5 hoặc item riêng từng người.
- [ ] Mỗi subclass thêm guard reward theo lượt sống: `private boolean pinkRewarded;` và override resetBase gọi `super.resetBase()` rồi resetguard. reward xác thực killer/zone/map, nhận guard trong synchronizedboss rồi nhả trước logic grant; nếu guard đã nhận thì return, ngăn gọi reward lặp cấp cả capsule và đồ cũ. Giữ combatlocks hiện có, không thêm JDBC hoặc lock ngược player→boss.
- [ ] Trong reward hiện có bật keepOwnerUntilDespawn cho it/it1/it2, gọi dropShared đúng1lần, giữ optionshop costume/dropgold/point và các doneChat/chuyểnboss. Test chết→rewardlặp→resetBase→chết: đúng5 rồi10capsule, không tăng nhầm khi reset không chết.
- [ ] KOL5 kiểm thử nonkiller nhặt1/5 capsule tăng1/5; mở ngay không giảm; bagfull và thua tranh nhặt không tăng; 200 capsule đạt nhiệm vụ thường+VIP theo code đang triển khai.

## Task5: Tên và mô tả client

**Modify:** `team2026.sql`, `SRC/sql/nro1.sql`.
**Create:** `SRC/sql/migrations/20261004_namek_pink_capsule.sql`.
**Modify when implementing:** `SRC/src/nro/models/data/DataGame.java`.

- [ ] Kiểm tra có đúng row722 tại cảdump vàlive; cập nhật name/description theo spec, không đụng ID/type/icon hoặc ruby861. Migration:

```sql
UPDATE item_template
SET name='Capsule hồng ngọc',
    description='Mở nhận ngẫu nhiên 1–100 hồng ngọc. 80% nhận 1–5 hồng ngọc.'
WHERE id=722;
```

- [ ] Nếu tên cột thực tế khác, đối chiếu schema trước viết migration; không suy đoán thứ tự INSERT. Backup row trước UPDATE và kiểm tra after chỉ đổi hai cột. Hai dump patch row722 bằng parser/đúng statement, tránh regex thay số722 trong dữ liệu khác.
- [ ] Bump catalog `DataGame.vsItem` từ runtime hiện tại13 lên14 để client tải tên/mô tả mới; xác nhận giá trị tại thời điểm triển khai nếu đã có bản khác. KOLmô tả nhiệm vụ đề xuất gọi “Capsule hồng ngọc (ID722)” nhưng vẫn đếm ID722.
- [ ] Kiểm tra client load name/description/icon; không cần sprite mới vì giữ icon6782. Không sửa currencyruby thành item trong bag.

## Task6: Kiểm chứng, triển khai và rollback

**Evidence:** `.analysis/namek-pink-capsule/`, `docs/source-analysis/namek-pink-capsule.md`.

- [ ] Compile classes mới/thay đổi bằng JDK21, classpath active20.jar +lib/*.jar, output riêng `.analysis/namek-pink-capsule/classes`, testclassesriêng. Chạy test RED đúng hành vi rồi GREEN; không dùng JAR/class cũ làm kết quả mới.
- [ ] Suite mới gồm PinkCapsuleTest, NamekDropOwnershipTest, NamekPinkCapsuleDropTest và KOLbehaviorpickup; hồi quy ownership của đồkhác, KOL9modes/DBtestschema riêng, pet/VIP/stack/inventory. Test giao dịch thật trên schema riêng, không cấp thưởng tài khoản người chơi thật.
- [ ] Đóng gói candidate bằng thay đúng entries activeJAR, không duplicate; SHA256 và chạy suite candidate không dùng classproduction rời. Rà soát source và bản đóng gói trước deploy.
- [ ] Bảo trì có lưu dữ liệu, xác địnhPID/workdir và kiểm tra serverdừng/cổng14445đóng; xử lý đúng JVMfixture còn giữJAR nếu có. BackupSQL/liveitemrow722; áp migration+verify13→14 trong runtimecandidate.
- [ ] **Move** JARcũ vào SRC/jar-backup tên riêng timestamp, đối chiếu SHA256; cài bảnmới, gom appJAR dư SRC/.analysis, giữ lib/toolJAR. Ghi metadataold/new/schema/catalog14/rollback.
- [ ] Startserverhidden, probe catalog14/item722/service mới/TCP. Nghiệm thu gameplay bằng nhân vật test nếu có client: người không đánh nhặtcapsule, ownergiữ costume/ngọc rồng, mởnhậnruby, save/relog. Chưa chơi thì ghi rõ chưa nghiệm thuclient.
- [ ] Rollback phải bảo trì và backupbảnđangdùng trước phục hồi. Không thu hồi ruby/capsule đã nhận. Phục hồi name/description từ itemrowbackup nếu rollbackcatalog; khi cần clientrefresh dùng versionmới tránh cache. Giữ KOLledger/walletreceipt của bảnđangchạy, khôngDROP.

## Tự kiểm tra kế hoạch

Yêu cầu drop→task4; quyền nhặt→task3/4; phân phối→task1; dùng/tiền→task2; KOL→task4; clientSQL→task5; backup/deploy→task6. Đã triển khai sau khi người dùng yêu cầu. Các con số80/20 và5capsulechung đã kiểm chứng; nghiệm thu gameplay client vẫn chưa thực hiện.
