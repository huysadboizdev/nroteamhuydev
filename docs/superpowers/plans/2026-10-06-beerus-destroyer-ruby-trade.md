# Beerus: đổi đồ Hủy Diệt bằng nguyên liệu và hồng ngọc

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mỗi lần đổi nhận một món Hủy Diệt thường với đúng một bonus HP/KI/SD 1–5%, trả 99 thức ăn + một món Thần tương ứng + 100 hồng ngọc, không mất phí nếu đổi thất bại.

**Architecture:** Tách giao dịch Beerus khỏi đường mua cũ trong ShopService; kiểm tra, tạo phần thưởng, trừ nguyên liệu và cấp đồ trong một khóa player, có hoàn tác khi cấp lỗi. Dùng các option phần trăm hiện có, không dùng option ấn và không chỉnh chỉ số nền. Giữ catalog SQL làm nguồn giá/hiển thị; xác minh ngữ cảnh shop và giá server khi giao dịch.

**Tech Stack:** Java 17, JDK 21, MySQL/MariaDB `team2026`, packet shop đặc biệt `-44`, bộ test Java main hiện có.

**Trạng thái:** đã triển khai ngày 06/10/2026, server PID18000/cổng14445. Báo cáo kết quả: `docs/source-analysis/beerus-destroyer-deployed-2026-10-06.md`. 59 lượt kiểm thử PASS trên classes, candidate và JAR đã cài; cache15/15 và hash class khớp. Serializer/client được đối chiếu; chưa quan sát UI Unity trực tiếp. Checklist dưới đây giữ mô tả công việc thiết kế ban đầu; bằng chứng thực hiện nằm trong báo cáo và metadata `.analysis/beerus-deployment-latest.json`.

## Quy tắc thiết kế

- Người dùng đã chọn chỉ **một** dòng, không phải ba dòng cùng lúc.
- Giữ option nền của shop, ví dụ găng SD+8.800 vẫn là SD+8.800; thêm dòng độc lập như HP+3%. Không còn roll tăng nền 15%.
- Đề xuất random đều loại dòng HP/KI/SD (mỗi loại 1/3), rồi random đều mức 1/2/3/4/5% (mỗi mức 20%). Đây là phân phối đề xuất vì SQL ấn hiện chỉ định min/max, không có trọng số runtime; không quảng cáo là đã sao chép một bảng xác suất khác.
- Chỉ mở shop khi đang mặc đủ 5 món Thần hợp lệ ở các ô áo/quần/găng/giày/nhẫn và có 99 thức ăn trong hành trang. Kiểm tra lại điều kiện khi bấm OK mở shop và trước **mỗi** giao dịch dưới khóa player.
- Món Thần tiêu hao lấy từ **ô trang bị đang mặc tương ứng**: áo ô 0, quần ô 1, găng ô 2, giày ô 3, nhẫn ô 4. Không lấy đồ Thần trong túi/rương để thay thế; không tự mặc đồ Hủy Diệt vừa nhận.
- Sau lần mua đầu, bộ Thần còn 4 món. Mọi lần mua tiếp, kể cả menu cũ, mua nhiều lần, gửi lại packet hoặc yêu cầu đồng thời, phải báo **“Thiếu 1 món Thần trên người, không được mua.”** và không trừ thêm thức ăn/hồng ngọc. Chỉ được mua lại sau khi người chơi mặc bổ sung đủ bộ Thần.
- 99 thức ăn của **một loại** hợp lệ 663–667; cộng được nhiều ô cùng ID, không trộn các loại khác nhau. Thứ tự chọn loại theo ID, đủ loại nào chọn loại đó; hiện kết quả đổi không phụ thuộc loại thức ăn.
- Chỉ nhận món đúng hệ của người chơi, nhẫn gender 3 dùng chung. Cả 5 ô phải có đúng loại, level 13, quantity>0, đúng hệ và còn hiệu lực; món bị tiêu hao phải đúng ID trong bảng tương ứng. Thiếu bộ được kiểm tra trước điều kiện tiền/thức ăn để lần mua tiếp báo đúng nguyên nhân.
- Không thêm option 34/35/36, 30 hoặc 87 vào phần thưởng chỉ vì mua ở Beerus. Không thay đổi luật ký gửi chung; đồ không ấn vẫn theo luật thường hiện có.
- Không sửa hồi tố đồ đã mua và không thay shop Thiên Sứ; chỉ tiêu hao món Thần đang mặc mà giao dịch hợp lệ yêu cầu.

### Bảng món tương ứng

| Hủy Diệt nhận | Món Thần tiêu hao | Hệ |
|---|---|---|
| 650 áo | 555 áo | Trái Đất |
| 651 quần | 556 quần | Trái Đất |
| 657 găng | 562 găng | Trái Đất |
| 658 giày | 563 giày | Trái Đất |
| 652 áo | 557 áo | Namec |
| 653 quần | 558 quần | Namec |
| 659 găng | 564 găng | Namec |
| 660 giày | 565 giày | Namec |
| 654 áo | 559 áo | Xayda |
| 655 quần | 560 quần | Xayda |
| 661 găng | 566 găng | Xayda |
| 662 giày | 567 giày | Xayda |
| 656 nhẫn | 561 nhẫn | Dùng chung |

## Task 1: Test giao dịch và menu trước sửa

**Create:** `SRC/test/nro/models/shop/BeerusDestroyerTradeTest.java`, `BeerusDestroyerBonusTest.java`, `SRC/test/nro/models/npc_list/BeerusMenuTest.java`.
**Reuse:** `SRC/test/nro/models/services/PinkFixture.java`.

- [ ] Tạo shop fixture BILL/NPC55 với 15 dòng, giá ruby100; trang bị đủ 5 món Thần đúng hệ, bag gồm 99 thức ăn, chỗ trống và số dư ruby.
- [ ] Test tất cả 13 mapping trên, ba hệ và nhẫn. Chạy `takeItem(player,3,id)`, không chỉ gọi helper nội bộ.
- [ ] Chạy bản cũ để xác nhận thất bại ở thiếu món tương ứng, không tiêu hao món Thần, charge-before-validation và menu map48/index100/OK.
- [ ] Test thiếu ruby/đồ/food: body, túi và mọi ví phải giữ nguyên. Test sai ô/sai loại/sai hệ, quantity0, hết hạn; đồ Thần trong túi/rương không bù được ô trang bị thiếu.
- [ ] Test mua lần đầu để lại 4 món Thần; lần tiếp từ shop đang mở phải báo đúng câu thiếu 1 món, không trừ gì dù ví/thức ăn cũng đã thiếu. Mặc bổ sung đúng món Thần rồi mới mua tiếp được.
- [ ] Test túi đầy có/không có ô được giải phóng khi tiêu hao, nhiều ô thức ăn cùng loại, retry và hai yêu cầu đồng thời.

Mẫu assertions cho giao dịch thành công:

```java
check(player.inventory.ruby == rubyBefore - 100, "exact ruby charge");
check(foodAfter == foodBefore - 99, "exact food charge");
check(divineBodyAfter == divineBodyBefore - 1, "one matching equipped divine item consumed");
check(validDivineSlotsAfter == 4, "one equipped divine slot is now empty");
check(rewardAfter == rewardBefore + 1, "one destroyer item granted");
check(player.inventory.gold == goldBefore && player.inventory.gem == gemBefore,
      "other wallets unchanged");
```

## Task 2: Giao dịch riêng có kiểm tra và hoàn tác

**Create:** `SRC/src/nro/models/shop/BeerusDestroyerTrade.java`.
**Modify:** `SRC/src/nro/models/shop/ShopService.java`, `buyItemHD()`.

- [ ] Khai báo bảng map ID Hủy Diệt -> ID Thần bằng dữ liệu bảng trên; chỉ cho phép ID đã liệt kê, level14/type0–4, đúng hệ.
- [ ] Tạo entry `purchase(Player player,int itemId)` giữ `synchronized(player)` từ kiểm tra đến khi hoàn tất. Kiểm tra tag cả idMark và shop là BILL, NPC55, SPEC_SHOP3, row cost100/typeSell3/icon7743.
- [ ] Kiểm tra lại đủ 5 món Thần đang mặc, rồi kiểm tra đúng ID món tương ứng, thức ăn trong bag và ruby. Không dùng kết quả kiểm tra lúc mở menu làm quyền mua. Rà soát luồng thay trang bị để thay đồ và giao dịch dùng cùng khóa player, tránh thay ô giữa kiểm tra và tiêu hao.
- [ ] Chặn giao dịch ruby đang pending; kiểm tra bảo vệ tài khoản và các guard inventory đang dùng; không gọi đường trừ tiền chung rồi mới kiểm tra nguyên liệu.
- [ ] Chuẩn bị phần thưởng từ bản sao row, hoàn tất option nền, áp dụng bonus một lần; chưa trừ ví/túi trong bước này.
- [ ] Snapshot body, bag bằng bản sao giữ nguyên metadata/options/thời hạn và ví ruby. Kiểm tra sức chứa bag sau tiêu hao thức ăn; ô trống trên body không phải chỗ trống bag. Nếu dùng debit tạm dưới khóa player, hoàn tác trước mọi đường return lỗi và trước gửi thông báo.
- [ ] Trừ ruby100, food99 trong bag và đúng một món Thần ở ô body tương ứng bằng inventory API. Cấp đồ vào bag bằng `addItemBag`; nếu false hoặc exception, phục hồi cả body, bag và ruby, không thông báo thành công.
- [ ] Sau commit cập nhật trang bị, tính lại chỉ số/ngoại hình, gửi body, túi, tiền và thông báo nhận món/bonus. Không tự mặc phần thưởng; không gửi packet trừ đồ thành công trước khi cấp đồ hoàn tất.
- [ ] Cho `buyItemHD()` gọi service mới; xóa toàn bộ nhánh charge-before-check, kiểm tra bất kỳ đồ đang mặc và roll nền 0–15% cũ.
- [ ] Chạy test transaction modes `success`, `funds`, `food`, `divine`, `gender`, `capacity`, `rollback`, `concurrent`, `forged`, `pending`; khi grant bị buộc false/throw phải giữ nguyên mọi nguyên liệu và ví.

## Task 3: Bonus một dòng, không có ấn

**Create:** `SRC/src/nro/models/shop/DestroyerBonusRoll.java`.
**Inspect:** `SRC/src/nro/models/player/NPoint.java`; không cần sửa nếu kiểm thử chứng minh option hiện có hoạt động.

- [ ] Dùng random được inject để kiểm tra cả 15 cặp loại/mức và giới hạn; mặc định dùng bộ random thread-safe của ứng dụng hoặc RandomGenerator.
- [ ] Logic chọn dòng và mức:

```java
int[] optionIds = {77, 103, 50}; // HP, KI, sức đánh
int optionId = optionIds[random.nextInt(3)];
int percent = random.nextInt(1, 6);
item.itemOptions.add(new Item.ItemOption(optionId, percent));
```

- [ ] Chỉ áp dụng lên item mới tạo; xác nhận template shop của 13 loại chưa có 50/77/103. Nếu cấu hình về sau thêm bonus sẵn, từ chối cấu hình không hợp lệ để tránh bonus lặp, thay vì cộng chồng âm thầm.
- [ ] Không thêm 34/35/36, không gọi `SealedEquipment.normalize`, không tăng chỉ số nền và không tăng hiệu ứng set ấn.
- [ ] Test deterministic toàn bộ 15 kết quả: đúng một dòng trong {50,77,103}, param1..5, tất cả option nền giữ nguyên, không có dấu ấn.
- [ ] Test mặc một món bonus và tính point: chỉ stat được roll thay đổi theo cơ chế hiện có, không cần đủ bộ. Không kiểm tra xác suất bằng test ngẫu nhiên dễ chập chờn.

## Task 4: Menu Beerus và giá hiển thị hồng ngọc

**Modify:** `SRC/src/nro/models/npc_list/Bill.java`; kiểm tra/đồng bộ `InventoryService.canOpenBillShop()` và helper kiểm tra đủ bộ Thần.
**Inspect:** `ConstNpc.HUONG_DAN_BILL` hiện nói về giải liên vũ trụ; không thay nội dung giải bằng hướng dẫn mua đồ.

- [ ] Xử lý đúng `indexMenu=100`, select0 tại map48 để mở BILL, bỏ nhánh kiểm tra select100 ở map2.
- [ ] Giữ điều kiện mặc đủ 5 món Thần và có 99 thức ăn trong hành trang để mở catalog; dùng cùng quy tắc kiểm tra hợp lệ với purchase. Kiểm tra ở cả lúc mở lời thoại và lúc bấm OK; không dựa vào menu đã mở để cho phép mua.
- [ ] Lời thoại ghi rõ: mặc đủ bộ Thần để mở/mua; tiêu hao 99 thức ăn cùng loại trong hành trang + món Thần đang mặc tương ứng +100 hồng ngọc; nhận một dòng HP/KI/SD1–5%, không có ấn. Mua xong phải mặc bổ sung đủ bộ Thần để mua tiếp.
- [ ] Giữ các nhánh đổi thức ăn/phiếu ăn, shop sự kiện và về Kaio map154.
- [ ] Test menu map48/100/0 chỉ mở BILL khi đủ bộ và thức ăn trong bag; thiếu 1 món, thức ăn trong rương, hoặc tháo đồ sau khi mở lời thoại đều bị chặn. Cancel không mua; map154 không bị ảnh hưởng.

## Task 5: SQL và packet

**Create:** `SRC/sql/migrations/20261006_beerus_destroyer_ruby_trade.sql`.
**Sync:** `team2026.sql`, `SRC/sql/nro1.sql`, `SRC/sql/team2026.seed.sql`, `SRC/sql/nro1.seed.sql`.

- [ ] Đổi BILL sang SPEC_SHOP3 để client hiển thị icon/nhãn hồng ngọc; không dùng shop thường có nhãn ngọc xanh chung.
- [ ] Áp dụng migration idempotent có scope tag BILL:

```sql
START TRANSACTION;
UPDATE shop SET type_shop=3 WHERE tag_name='BILL' AND npc_id=55;
UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id
SET i.type_sell=3,i.cost=100,i.icon_spec=7743
WHERE s.tag_name='BILL' AND s.npc_id=55;
COMMIT;
```

- [ ] Giữ 15 row, ba tab và option nền; không thêm bonus random cố định vào item_shop_option.
- [ ] Test packet `-44`: shoptype3, giá100 và icon7743 cho toàn bộ dòng; kiểm tra NPC và catalog vẫn có đúng áo/quần/găng/giày/nhẫn.
- [ ] Test nút mua20 lần với dư tiền/thức ăn/đồ Thần trong bag: chỉ lần đầu thành công, 19 lần tiếp bị chặn vì còn 4 món đang mặc, không mất thêm nguyên liệu. Test hai yêu cầu đồng thời có cùng kết quả. Sau khi mặc bổ sung đủ bộ, lần mua mới mới được phép thành công và roll riêng.

## Task 6: Build, hồi quy và triển khai khi được yêu cầu

- [ ] Compile source đầy đủ vào `.analysis/beerus-destroyer-build/classes`; compile tests mới cùng PinkFixture và chạy toàn bộ test/mode Task1–5.
- [ ] Chạy hồi quy `ThienSuRubyShopTest`, `SantaRubyShopTest`, giao dịch ruby/ký gửi và bonus/point trang bị; giữ giá Thiên Sứ10ruby và các bản sửa boss/khu.
- [ ] Tạo candidate JAR, liệt kê chính xác class đổi và hash; test lại trên candidate.
- [ ] Khi người dùng yêu cầu triển khai: bảo trì lưu dữ liệu, xác nhận mọi JVM `-jar20.jar` dừng, snapshot SQL và lưu riêng rows BILL/giá cũ để rollback đúng scope.
- [ ] Backup JAR cũ unique timestamp vào `SRC/jar-backup`, đối chiếu SHA-256; apply migration, đồng bộ SQL nguồn, cài candidate thành `SRC/20.jar` và lưu metadata theo AGENTS.md.
- [ ] Khởi động lại, xác nhận giá/currency cache15row và hash class. Kiểm tra giao dịch trên tài khoản thử đã được chuẩn bị: đúng nguyên liệu, đúng ví và đúng một bonus; không thử mua bằng tài khoản thật của người chơi.
- [ ] Rollback khi cần bằng bảo trì, JAR backup và SQL giá/shop cũ; không restore toàn bộ player snapshot gây mất tiến trình chơi mới.

**Điều kiện hoàn thành:** chỉ mở/mua khi đủ bộ Thần đang mặc và thức ăn trong hành trang; mỗi lần tiêu hao99food+1mónThần đang mặc đúng ô/ID+100ruby; còn4món thì lần mua tiếp báo thiếu1món và không trừ gì; thất bại phục hồi cả body/bag/ví; giữ nền gốc, đúng một bonus1–5%, không có ấn hoặc hiệu ứng set ấn, giá client/SQL/runtime đồng bộ.
