# Tinh Ấn, Nhật Ấn, Nguyệt Ấn Implementation Plan

> **For agentic workers:** Use executing-plans to implement this plan task-by-task in the current session. Steps use checkbox syntax for tracking.

**Goal:** Đồ ấn thường rơi từ boss/quái có bonus riêng random 1–5%, bộ +15%; đồ Thần Linh có bonus riêng random 1–7%, bộ Thần Linh đủ 5 món cùng ấn +20%.

**Architecture:** SQL lưu cấu hình hai cấp đồ × ba loại ấn và mapping 13 template Thần Linh; Java sinh dòng chỉ số riêng đúng một lần khi tạo đồ rơi có ấn. NPoint áp dụng dòng phần trăm thông thường ngay khi mặc; điều kiện bộ kiểm tra riêng 5 ô áo/quần/găng/giày/rada. Giá trị random được lưu bằng cơ chế itemOptions hiện có.

**Tech Stack:** Java 17, MySQL/MariaDB, Ant/NetBeans, các test Java main hiện có.

## Quy tắc nghiệm thu

| Ấn | Marker | Dòng thêm | Roll mỗi món | Bộ |
| --- | --- | --- | --- | --- |
| Tinh | 34 | 50: sức đánh | 1–5% | +15% sức đánh |
| Nguyệt | 35 | 103: KI | 1–5% | +15% KI |
| Nhật | 36 | 77: HP | 1–5% | +15% HP |

| Thần Linh có ấn | Marker | Dòng thêm random | Bộ 5 món Thần Linh cùng ấn |
| --- | --- | --- | --- |
| Thần Tinh Ấn | 34 | 50: sức đánh +1–7% | +20% sức đánh |
| Thần Nguyệt Ấn | 35 | 103: KI +1–7% | +20% KI |
| Thần Nhật Ấn | 36 | 77: HP +1–7% | +20% HP |

13 template Thần Linh 555–567 dùng mapping SQL; giữ nguyên chỉ số nền và dùng lại marker. Nhẫn 561 dùng chung cho ba hành tinh. Không cần tạo 39 template mới. 7% là tối đa; chọn số nguyên random từ 1 đến 7, gồm cả hai đầu.

Bộ pha đồ thường/Thần Linh: bonus riêng vẫn theo từng món; mức bonus bộ pha chưa được quy định. Phải chốt trước khi viết kiểm tra bộ pha; không tự áp 20%. Kiểm tra bộ 20% phải xác nhận cả 5 món thuộc cấp divine cùng ấn.

Bonus riêng có hiệu lực ngay khi mặc. Giữ dòng tên ấn và thêm dòng phần trăm có hiển thị theo ví dụ được xác nhận. Mỗi lần sinh dòng bonus chọn ngẫu nhiên đều một số nguyên 1..5 bằng `Util.nextInt(1, 5)`; đây là cách phân phối đề xuất vì người dùng không quy định trọng số.

Đủ 5 món cùng ấn mới bật bộ; tháo một món tắt bộ nhưng các món còn mặc vẫn có bonus riêng. Không cộng thẻ/cải trang/phụ kiện vào số món, không đếm option lặp nhiều lần trên một món. Giữ công thức phần trăm hiện có: không đổi toàn server sang cộng tuyến tính.

## Ranh giới cần giữ khi triển khai

Yêu cầu mới đã chốt: đồ có ấn thường tăng nền random 5–10%, Thần Linh tăng nền random 5–15%; Thần Linh có dòng bonus riêng random 1–7%, không cố định 7%. SQL `equipment_seal_base_rule` lưu hai khoảng nâng nền. Loader phải đọc và xác thực bảng này bên cạnh 6 dòng quy tắc và 13 mapping.

Nâng nền, bonus riêng, bonus bộ là ba bước độc lập. Nâng nền tính trên chỉ số gốc của cùng template trước phẩm chất và trước bonus ấn; không nhân lại trên món đã được nâng nền. Đề xuất một roll nền cho mỗi món, dùng cho các chỉ số nền được catalog xác định. Giữ riêng option phần trăm, yêu cầu sức mạnh, hạn dùng, số sao, cấp nâng và marker; không nhân toàn bộ itemOptions.

Không tự chọn boss/map, tỷ lệ sinh ấn, item hợp lệ hoặc cấp lại bonus cho đồ cũ. Kiểm kê thực tế ở nhiệm vụ 1; nếu không có nguồn sinh ấn, phần thêm nguồn rơi mới phụ thuộc vào cấu hình được người dùng chốt. Không làm cho mọi đồ boss/quái đều có ấn.

Giới hạn 5% chỉ áp dụng dòng sinh thêm của đồ thường; đồ Thần Linh sinh thêm dòng random 1–7%. Không ép các option 50/77/103 khác xuống 5%, không thay đổi nâng cấp/sao/chuyển hóa hiện có trong kế hoạch này. Đồ đã lưu có roll giữ nguyên qua thao tác và restart.

### Nhiệm vụ 1: Kiểm kê nguồn đồ rơi và dữ liệu

**Đọc:** `SRC/src/nro/models/mob/Mob.java`, `SRC/src/nro/models/boss/**/*.java`, `SRC/src/nro/models/services/ItemService.java`, `SRC/src/nro/models/map/ItemMap.java`, `SRC/src/nro/models/services/Service.java`, `SRC/src/nro/models/map/service/ItemMapService.java`.

- [ ] Đọc luồng `randDoTL`, `randDoTLBoss`, các nhánh quái/boss và nơi thêm option vào ItemMap. Ghi rõ nguồn nào tạo marker 34/35/36, tránh nhầm ID template 34/35/36 với ID option.
- [ ] Truy vấn chỉ đọc marker trong đồ người chơi bằng parser cấu trúc lưu thực tế; đối chiếu với `item_shop_option`, `item_default_option` và bytecode JAR đang dùng. Không in dữ liệu tài khoản cá nhân ra báo cáo.
- [ ] Xác định điểm sinh bonus sau khi marker đã được gắn, trước khi gửi đồ rơi tới client. `ItemMap` constructor có options rỗng nên không roll ở constructor. Kiểm tra các luồng gửi trực tiếp không đi qua `Service.dropItemMap`.
- [ ] Ghi danh sách call site cụ thể vào note. Nếu không có nguồn rơi ấn, trình danh sách item/boss/map và tỷ lệ dự kiến để người dùng chốt trước khi thêm nguồn mới.
- [ ] Kiểm tra các mẫu option lấy từ shop có chia sẻ đối tượng hay không: `getListOptionItemShop` dùng `addAll`, nên phải sao chép từng ItemOption khi sửa option đồ rơi.

### Nhiệm vụ 2: Cấu hình SQL

**Tạo:** `SRC/sql/migrations/20261005_equipment_seal_bonus.sql`.
**Đồng bộ:** `team2026.sql`, `SRC/sql/nro1.sql`.
**Tạo:** `SRC/src/nro/models/item/EquipmentSealRules.java`.
**Sửa:** `SRC/src/nro/models/server/Manager.java`, cạnh bước load cấu hình item hiện có.
**Test:** `SRC/test/nro/models/item/EquipmentSealSqlTest.java`.

- [ ] Viết test sáu cấu hình: ordinary min=1/max=5/set=15, divine min=1/max=7/set=20; cấu hình sai option, sai range hoặc thiếu loại ấn bị từ chối với lỗi chỉ rõ dòng.
- [ ] Chạy test trước khi viết loader để thấy thất bại đúng nguyên nhân.
- [x] Tạo và áp bảng cấu hình riêng bằng migration có thể chạy lại; đồng bộ hai dump. Đã chạy lại migration và kiểm tra SQL: 6 dòng quy tắc, 13 mapping, 5 type trang bị, 0 dòng cấu hình sai. Loader Java và các test runtime vẫn chưa thực hiện.

```sql
-- EQUIPMENT_SEAL_RULES_20261005
-- Configuration only. Runtime/drop support requires the planned Java update.
-- ordinary: random 1..5 per item, 15 percent full set.
-- divine: random 1..7 per item, 20 percent full divine set.
-- Base-stat premium over the unsealed baseline: ordinary 5..10, divine 5..15.
-- Existing item templates/options/player data are preserved.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_rule (
    equipment_tier VARCHAR(16) NOT NULL,
    seal_option_id INT NOT NULL,
    bonus_option_id INT NOT NULL,
    min_percent INT NOT NULL,
    max_percent INT NOT NULL,
    set_percent INT NOT NULL,
    PRIMARY KEY (equipment_tier, seal_option_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_tier_item (
    item_id INT NOT NULL PRIMARY KEY,
    equipment_tier VARCHAR(16) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS equipment_seal_base_rule (
    equipment_tier VARCHAR(16) NOT NULL PRIMARY KEY,
    min_percent INT NOT NULL,
    max_percent INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
START TRANSACTION;
INSERT INTO equipment_seal_rule
    (equipment_tier,seal_option_id,bonus_option_id,min_percent,max_percent,set_percent)
VALUES
    ('ordinary',34,50,1,5,15),
    ('ordinary',35,103,1,5,15),
    ('ordinary',36,77,1,5,15),
    ('divine',34,50,1,7,20),
    ('divine',35,103,1,7,20),
    ('divine',36,77,1,7,20)
ON DUPLICATE KEY UPDATE
    bonus_option_id=VALUES(bonus_option_id),
    min_percent=VALUES(min_percent),
    max_percent=VALUES(max_percent),
    set_percent=VALUES(set_percent);
INSERT INTO equipment_seal_tier_item (item_id,equipment_tier) VALUES
    (555,'divine'),(556,'divine'),(557,'divine'),(558,'divine'),
    (559,'divine'),(560,'divine'),(561,'divine'),(562,'divine'),
    (563,'divine'),(564,'divine'),(565,'divine'),(566,'divine'),(567,'divine')
ON DUPLICATE KEY UPDATE equipment_tier=VALUES(equipment_tier);
INSERT INTO equipment_seal_base_rule (equipment_tier,min_percent,max_percent)
VALUES ('ordinary',5,10),('divine',5,15)
ON DUPLICATE KEY UPDATE
    min_percent=VALUES(min_percent),max_percent=VALUES(max_percent);
COMMIT;
```

- [ ] Loader đọc một snapshot bất biến; xác nhận đủ hai cấp × ba loại, ánh xạ đúng; ordinary min=1/max=5/set=15, divine min=1/max=7/set=20; mapping đúng 555–567 và type 0..4. Item không thuộc mapping dùng cấp ordinary khi đã có ấn. Từ chối cấu hình sai khi khởi động, không âm thầm đổi chỉ số.
- [ ] Chạy migration hai lần trên database test, xác nhận luôn đúng 6 dòng quy tắc và 13 dòng mapping; test loader PASS. Không thêm dòng vào `item_default_option` vì bảng này đang được giới hạn riêng cho một số item.

### Nhiệm vụ 3: Sinh bonus riêng một lần

**Tạo:** `SRC/src/nro/models/item/EquipmentSealBonus.java`.
**Sửa:** call site nguồn rơi xác định ở nhiệm vụ 1, ưu tiên lớp dùng chung `ItemService.java`; chỉ sửa `Service.java` nếu đã chứng minh điểm gửi đồ bao phủ đúng các nguồn.
**Test:** `SRC/test/nro/models/item/EquipmentSealBonusTest.java`.

- [ ] Test ba loại ấn ở cả hai cấp: ordinary roll 1 và 5, không có 0 hoặc 6; divine kiểm tra cả biên 1 và 7, không có 0 hoặc 8. Kiểm tra đủ 13 template, nhẫn 561 dùng chung và cả ba hành tinh.
- [ ] Test đồ không có ấn và item ngoài type 0..4 không nhận bonus; marker giữ nguyên, chỉ số nền giữ nguyên, mẫu shop không bị sửa.
- [ ] Test gọi hook lặp không thêm dòng/roll lại; dữ liệu có nhiều marker khác loại phải bị báo lỗi rõ ràng hoặc bỏ qua có log, không tự chọn loại đầu tiên.
- [ ] Triển khai roll đúng một lần trong nhánh tạo đồ rơi có ấn. Không đặt hook ở tính chỉ số/mặc đồ/đăng nhập/pickup.
- [ ] Chốt cách lưu chống gọi lặp: ưu tiên gọi duy nhất ở nguồn và giữ trạng thái khởi tạo trên ItemMap; không nhận diện “đã roll” chỉ bằng việc có option 50/77/103 vì đồ có thể đã có option đó từ trước.
- [ ] Chạy test PASS; kiểm tra khi có sẵn dòng phần trăm cùng ID, dòng sinh thêm vẫn độc lập, không ghi đè chỉ số cũ và dòng mới của ordinary không vượt 5, divine trong 1–7; không giới hạn các dòng nền khác.

### Nhiệm vụ 4: Điều kiện bộ đúng 5 món

**Sửa:** `SRC/src/nro/models/player/NPoint.java`.
**Dùng:** `EquipmentSealRules.java`.
**Test:** `SRC/test/nro/models/player/EquipmentSealEffectTest.java`.

- [ ] Test một món Nguyệt roll 5 tăng KI ngay nhưng isNguyetAn=false; lặp tương tự HP và sức đánh.
- [ ] Test 4 món chưa bật bộ; 5 món ordinary cùng ấn bật đúng +15%, 5 món divine cùng ấn bật đúng +20% tương ứng; bộ trộn khác ấn không bật; tháo món tắt bộ và mất riêng dòng của món đó.
- [ ] Test một món lặp marker, cải trang có marker và thẻ có marker đều không bù được món còn thiếu.
- [ ] Thay nguồn đếm marker bằng kiểm tra 5 ô type 0..4 đúng vị trí; mỗi item đếm tối đa một lần. Loại bỏ tăng số đếm bộ từ vòng option thông thường và thẻ.
- [ ] Giữ `addOption` tính option 50/77/103 theo engine hiện có; lấy phần trăm bộ theo cấp đồ từ cấu hình, không áp dụng bonus riêng lần thứ hai.
- [ ] Test chỉ số trước/sau có giá trị cụ thể và kiểm tra làm tròn. Không dùng kỳ vọng +40% cho 5 món 5% cộng bộ nếu engine nhân tuần tự.

### Nhiệm vụ 4A: Tăng chỉ số nền trước bonus riêng

**Tạo:** `SRC/src/nro/models/item/EquipmentSealBaseStats.java`.
**Sửa:** `EquipmentSealRules.java`, `EquipmentSealBonus.java` và các nguồn rơi đã xác định ở nhiệm vụ 1; đặc biệt `ItemService.java:randDoTL/randDoTLBoss`.
**Test:** `SRC/test/nro/models/item/EquipmentSealBaseStatsTest.java`.

- [ ] Loader xác nhận ordinary min=5/max=10, divine min=5/max=15; thiếu cấp hoặc range khác cấu hình chốt bị từ chối rõ ràng.
- [ ] Kiểm kê catalog nền cho áo/quần/găng/giày/nhẫn từng hành tinh, chỉ ra option nền có thể nâng và mức nền trước random phẩm chất. Không lấy option đầu làm nền vì có thể là yêu cầu sức mạnh. Quyết định cách xử lý hồi phục phụ/chí mạng theo loại món, ghi vào note trước khi sửa.
- [ ] Viết test ordinary nền 1000: roll 5 cho 1050, roll 10 cho 1100; divine nền 1000: roll 5 cho 1050, roll 15 cho 1150. Nguồn random có thể điều khiển để kiểm tra chắc chắn cả hai biên.
- [ ] Viết test gọi lại không nhân thêm; đồ không có ấn giữ nguyên; nâng nền không sửa marker, dòng % riêng, hạn dùng, cấp nâng, số sao hoặc yêu cầu sức mạnh. Test chỉ số lớn để tránh overflow và số nhỏ để xác nhận quy tắc làm tròn.
- [ ] Với Thần Linh có ấn, thay nguồn phẩm chất `tiLe=100..115` bằng nền gốc + roll 105..115 đúng một lần; không xếp hai hệ số. Test cực đại: nền 1000 không thành 1322 do nhân 1,15 hai lần.
- [ ] Dùng phép nhân long khi tính nền và làm tròn xuống theo số nguyên của option; không đổi đơn vị K khi nhân. Chỉ roll lúc tạo đồ và lưu kết quả qua itemOptions hiện có.
- [ ] Chạy test PASS cùng các test bonus riêng/bộ/lưu-nạp. Ví dụ giày Nguyệt có cả KI nền đã tăng, dòng KI riêng random và +15%/+20% bộ vẫn phải được tính đúng từng bước.

### Nhiệm vụ 5: Lưu dữ liệu và hồi quy

**Đọc:** `SRC/src/nro/models/services/ItemService.java:createItemFromItemMap`, luồng pickup, serializer/DAO đang lưu itemOptions.
**Test:** `SRC/test/nro/models/item/EquipmentSealPersistenceTest.java`, các test bộ đồ, wire option, item mặc định hiện có.

- [ ] Test đồ rơi → nhặt → lưu/nạp giữ marker và chính xác từng dòng roll; mặc/tháo, rương và giao dịch không thay param.
- [ ] Test option trùng ID được serializer giữ riêng đúng như dữ liệu đồ rơi; nếu bị gộp, giải quyết cách lưu trước khi triển khai, không ghi đè chỉ số có sẵn.
- [ ] Chạy hồi quy `SKHEffectTest`, `ItemOptionWireTest`, `ItemDefaultOptionsTest`, `ChuyenHoaTrangBiTest` theo runner hiện có; xác nhận không ảnh hưởng set kích hoạt khác.
- [ ] Build Java 17 bằng cấu hình Ant trong `SRC/build.xml`; compile test riêng, chạy trên candidate JAR + thư viện `SRC/lib/*`, tránh class production rời che khuất JAR. Mọi nhóm test phải báo PASS và build phải thành công.

### Nhiệm vụ 6: Triển khai sau kiểm chứng

**Đầu ra:** snapshot SQL, migration, candidate JAR, bản backup và metadata trong `SRC/jar-backup`.

- [ ] Backup bảng cấu hình và dữ liệu SQL bị tác động; giữ đường dẫn snapshot và lệnh rollback trong metadata.
- [ ] Bảo trì có lưu dữ liệu, xác nhận server dừng trước thay JAR. Áp migration và đọc lại đủ 6 dòng quy tắc và 13 dòng mapping.
- [ ] Chuyển `SRC/20.jar` cũ vào `SRC/jar-backup` với tên có thời gian duy nhất; so SHA-256 với giá trị ghi trước khi chuyển. Cài candidate làm `SRC/20.jar`, đối chiếu hash candidate/new.
- [ ] Gom JAR ứng dụng dư/staging không dùng trong SRC/.analysis vào jar-backup; không di chuyển SRC/lib hoặc công cụ cần dùng. Ghi metadata gồm thời gian, hash, migration, test và cách khôi phục.
- [ ] Khởi động, xác nhận loader SQL thành công; kiểm tra trong game quần Nguyệt thường 1–5%/bộ +15% và quần Thần Nguyệt 1–7%/bộ Thần Linh +20%; lặp tương tự HP và sức đánh, tháo món và đăng nhập lại giữ roll.
- [ ] Cập nhật note bằng kết quả thực tế. Rollback cần bảo trì/lưu dữ liệu, backup JAR đang dùng trước khi phục hồi; không xóa option đã sinh trên đồ người chơi.

## Trạng thái

## Phần nối: giao dịch và siêu thị ký gửi

**Yêu cầu:** Cả hai cấp đồ có ấn cấm giao dịch trực tiếp, chỉ có dòng ký gửi ngọc (87), thanh toán bằng hồng ngọc/ruby. Không áp chính sách lên mọi đồ Thần Linh không có ấn. Giữ số roll/chỉ số nền trong mua/hủy/lưu-nạp.

**Đã làm SQL:** `SRC/sql/migrations/20261005_equipment_seal_market.sql` tạo `equipment_seal_market_rule`, áp và kiểm tra 3 marker, currency ruby, option khóa 30 và ký gửi 87; hai dump đã đồng bộ. Chưa triển khai runtime.

### Nhiệm vụ 7: Chính sách đồ ấn

**Sửa:** `EquipmentSealRules.java`, `EquipmentSealBonus.java`, `SRC/src/nro/models/services_func/Trade.java`, `SRC/src/nro/models/shop_ky_gui/ConsignShopService.java`.

- [ ] Loader đọc bảng market, xác nhận đủ 3 marker, direct_trade_allowed=0, trade_lock_option_id=30, consign_option_id=87 và consign_currency=ruby.
- [ ] Test đồ ấn không thể đưa vào giao dịch dù thiếu option 30; kiểm tra lại marker ở bước xác nhận để chặn dữ liệu cũ và request sửa tay. Đồ thường không có ấn giữ quy tắc cũ.
- [ ] Khi sinh đồ ấn gắn đúng một option 30 và 87, bỏ 86; không đổi option nền/roll đã lưu, không sửa mẫu shop dùng chung.
- [ ] Test UI chỉ hiện ký gửi ngọc; gửi request giá vàng hoặc ngọc xanh cho đồ ấn bị từ chối trước khi thu phí/lấy đồ. Ngoại lệ cho phép ký gửi ruby với option 30 chỉ áp đồ type 0..4 có marker hợp lệ theo SQL; các đồ khóa khác vẫn bị chặn.

### Nhiệm vụ 8: Sửa tiền tệ và giao đồ

**Sửa:** `ConsignShopService.java`, `ConsignItem.java`, `ConsignShopManager.java`, `Manager.java`, NPC `KyGui.java`.
**Tạo:** `SRC/sql/migrations/20261005_consignment_currency.sql`, các test trong `SRC/test/nro/models/shop_ky_gui/`.

- [ ] Bổ sung currency rõ ràng cho listing mới; giữ nguyên ý nghĩa gold/gem của listing cũ và snapshot trước migration. Không chỉ đổi inventory.gem thành ruby ở mọi listing.
- [ ] Chốt/sửa nhánh vàng nhất quán: không trừ vàng thường nhưng trả thỏi vàng. Test cùng đơn vị ở đăng giá/mua/nhận tiền và phí. Hiện đã tái hiện giá 100 vàng thành 90 thỏi ở người bán.
- [ ] Test đồ ấn trừ đúng ruby, không trừ gem/gold; người bán nhận ruby theo phí ký gửi hiện hành, dùng long cho phép tính và kiểm tra tràn trước cộng.
- [ ] Test đầy túi khi mua/hủy: không mất tiền/đồ, không đổi trạng thái listing khi addItemBag thất bại. Preflight sức chứa và xử lý kết quả thêm túi, deep-copy option, tránh nhân đôi default.
- [ ] Chặn listing giá sai/đồ hết số lượng/không đủ tiền trước mọi mutation; dùng cùng một predicate eligibility cho UI và endpoint đăng bán.

### Nhiệm vụ 9: Đồng thời và lưu bền

**Sửa:** `ConsignShopManager.java`, `ConsignShopService.java` và DAO lưu nhân vật/listing liên quan.

- [ ] Test hai người mua cùng listing chỉ một người thành công; mua/hủy cạnh tranh, nhận tiền lặp, gửi request đồng thời hoặc trong giao dịch trực tiếp không tạo tiền/đồ dư.
- [ ] Thiết kế thứ tự khóa nhân vật/listing thống nhất với Trade/RubyGift; không dùng lock ngược gây deadlock. Recheck tiền/túi/ownership dưới khóa trước commit.
- [ ] Thay TRUNCATE + INSERT từng dòng bằng cập nhật có transaction; bảo đảm lỗi lưu không xóa toàn shop. Test DB lỗi giữa chừng trên database test, không làm lỗi giả lập trên DB người chơi.
- [ ] Ghi chuyển trạng thái listing và các biến động tiền/đồ trong cùng giao dịch bền hoặc dùng ledger/idempotency theo giao dịch ruby hiện có. Test crash/retry: không bán hai lần, không claim hai lần, khôi phục không nhân đồ.

### Nhiệm vụ 10: UI, phân trang và xác nhận phí

**Sửa:** `ConsignShopService.java`, NPC `KyGui.java`.

- [ ] Sửa phân trang dùng int offset, mỗi trang 20 món, cùng predicate khi đếm và khi gửi. Test 0/1/20/21/40 món, trang 7 và trang cuối; từ chối index/page âm hoặc vượt giới hạn protocol.
- [ ] Test đồ bản thân hiển thị trong tab cá nhân, không làm lệch đếm trang chung; số trang không dư khi tổng chia hết 20.
- [ ] Đồng bộ thông báo tiền tệ/phí với thực tế. Up top phải xác thực lại listing/chủ sở hữu/trạng thái trước thu phí và thao tác; test xác nhận giả/lặp không mất phí hoặc đẩy đồ người khác.
- [ ] Chạy hồi quy inventory200, wire option, ruby gift, Trade và toàn bộ vòng đăng/mua/hủy/nhận tiền trước build/deploy theo nhiệm vụ 6.

**Bằng chứng nền:** `docs/source-analysis/ky-gui-sieu-thi-2026-10-05.md`, probe `.analysis/ConsignAuditProbe.java`. Probe hiện tái hiện lỗi, không dùng kết quả đó như kiểm thử PASS của bản sửa.

Đã nối yêu cầu Thần Linh: bonus riêng random 1–7%, bộ cùng ấn 20%; đồ thường giữ 1–5%/15%. Phần SQL cấu hình được triển khai trước, Java/JAR chưa chỉnh. Kết quả áp SQL và metadata được ghi trong note. Nguồn rơi ấn, chính sách đồ cũ và mức bộ pha hai cấp cần được kiểm kê/chốt khi triển khai Java.


## Tiến độ thực tế sau sửa ký gửi 05/10/2026

Đã triển khai/cài runtime cho thanh toán ruby toàn siêu thị, chuẩn hóa và chặn giao dịch đồ ấn, transaction/receipt, phục hồi commit chưa rõ và đăng xuất, phân trang/alias ID, UI/phí. 20 nhóm kiểm tra đạt; server mới đã mở cổng 14445. Bằng chứng và metadata triển khai trong `docs/source-analysis/ky-gui-sieu-thi-2026-10-05.md`.

Các checklist thiết kế phía trên giữ lại để đối chiếu phạm vi: nhánh vàng được đóng hoàn toàn thay vì tiếp tục hỗ trợ; tên migration thực tế là `20261005_consignment_ruby.sql`; marker thị trường cố định trong Java theo SQL đã chốt, chưa có loader chỉnh động. Engine sinh đồ ấn, roll chỉ số và bonus bộ ở nhiệm vụ 1–5 chưa triển khai. Không xem việc hoàn thành ký gửi là hoàn thành engine đồ ấn.


## Chốt nhóm đồ thường: cấp 10, 11, 12

Người dùng chốt đồ thường có ấn chỉ thuộc cấp trang bị 10/11/12 (`item_template.level`), type 0..4. Không phải item ID 10/11/12; không bao gồm đồ cấp thấp khác. Đã bổ sung đủ 39 template ordinary vào `equipment_seal_tier_item`; giữ nguyên 13 template divine 555–567. Migration `20261005_equipment_seal_ordinary_levels.sql` đã áp DB, chạy lại kiểm tra idempotency và nối vào hai dump; mỗi cấp có 13 mẫu.

| Cấp | Trái Đất: áo/quần/găng/giày | Namếc: áo/quần/găng/giày | Xayda: áo/quần/găng/giày | Rada chung |
| --- | --- | --- | --- | --- |
| 10 | 231 / 243 / 255 / 267 | 235 / 247 / 259 / 271 | 239 / 251 / 263 / 275 | 279 |
| 11 | 232 / 244 / 256 / 268 | 236 / 248 / 260 / 272 | 240 / 252 / 264 / 276 | 280 |
| 12 | 233 / 245 / 257 / 269 | 237 / 249 / 261 / 273 | 241 / 253 / 265 / 277 | 281 |

Các mức giữ nguyên: ordinary nền +5–10%, dòng riêng +1–5%, bộ cùng ấn +15%; divine nền +5–15%, dòng riêng +1–7%, bộ cùng ấn +20%. Đồ cấp khác không tự được phân loại ordinary nếu thiếu mapping; engine phải kiểm tra bảng mapping thay vì mặc định mọi đồ không phải divine là ordinary.

Ví dụ admin xem quần vàng Goku Nguyệt Ấn: `i 243 1 6:1000 35:1 103:5`. Đây là đồ chỉ số test cố định, danh sách option truyền vào thay option gốc; lệnh cũ dùng quần ID 6 chỉ minh họa hiển thị, không thuộc nhóm đồ thường được chốt.

Trạng thái hiện tại: khóa giao dịch/ký gửi ruby đã chạy trên JAR; sinh đồ ấn, roll nền/bonus và phân biệt bonus bộ 15%/20% vẫn chưa triển khai engine. Lần bổ sung mapping này không thay JAR hay chỉnh đồ người chơi đã lưu.

Backup/verification: `.analysis/seal-ordinary-levels-20261005-105216`
