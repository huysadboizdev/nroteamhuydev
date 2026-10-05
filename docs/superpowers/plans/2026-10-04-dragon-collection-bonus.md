# Kế hoạch mô tả và bonus bộ 7 Rồng Nhí vĩnh viễn

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Thay mô tả của 7 pet và cộng một lần 10% HP, KI, Sức Đánh khi nhân vật đang sở hữu đủ 7 loại pet vĩnh viễn.

**Architecture:** SQL giữ mô tả vật phẩm; server tự kiểm tra vật phẩm thật đang sở hữu khi tính chỉ số. Bonus không ghi thêm vào option từng pet hoặc chỉ số gốc của nhân vật. Dùng pipeline phần trăm hiện có trong NPoint để tránh cộng dồn sau đăng nhập hoặc tính lại.

**Tech Stack:** Java 21, MySQL, server item-template protocol, client Unity C# hiện có.

---

## Kết quả kiểm tra ngày 04/10/2026

- Database hiện tại và cả `team2026.sql`, `SRC/sql/nro1.sql`: ID 1765–1771 đều có `item_template.description = 'Vật phẩm sự kiện'`. Snapshot chỉ đọc lưu ở `.analysis/dragon-collection-plan/database-before.json`.
- `SRC/src/nro/models/server/Manager.java:625` đọc description từ SQL.
- `SRC/src/nro/models/data/ItemData.java:52` và `:81` truyền description cho client.
- Client `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Panel.cs:1967` và Game2 cùng dòng hiển thị `item.template.description` dưới đường phân cách. Đây là mô tả, không phải item_option_template.
- `SRC/src/nro/models/item/DragonPetCatalog.java` quy định phần thưởng vĩnh viễn không có option 93; pet có hạn có option 93. Không coi option 93 param 0 là vĩnh viễn.
- `SRC/src/nro/models/player/Inventory.java:143` đã kiểm tra pet vĩnh viễn trong bag/box/body để cập nhật danh hiệu Mê Rồng; `InventoryService.addItemBag` cũng có bộ đếm danh hiệu. Đây là tiến độ nhiệm vụ, không phải bằng chứng hiện đang sở hữu đủ bộ; không dùng bộ đếm này cấp bonus.
- `SRC/src/nro/models/player/NPoint.java` chưa có bonus bộ 7 rồng. Danh sách tlHp/tlMp/tlDame áp dụng từng hệ số phần trăm và reset mỗi lần tính lại.

## Quy tắc đề xuất, chờ chốt phạm vi sở hữu

1. Đổi mô tả cả 7 pet thành đúng câu: **Sở hữu 7 Pet Rồng Nhí Vĩnh Viễn sẽ tăng 10% HP, KI, Sức Đánh**.
2. Phải đủ 7 loại khác nhau: 1 sao ID 1771, 2 sao 1770, 3 sao 1769, 4 sao 1768, 5 sao 1767, 6 sao 1766, 7 sao 1765. Bảy con trùng loại không đủ điều kiện.
3. Đề xuất tính trên cùng nhân vật: hành trang + rương + pet đang đeo, không bắt buộc đeo pet để kích hoạt. Không cộng vật phẩm tài khoản khác, đệ tử, vật phẩm đã bán hoặc đang chờ nhận. Câu hỏi phạm vi này đã gửi người dùng; cập nhật kế hoạch nếu người dùng chọn khác.
4. Chỉ tính item thật có template, quantity > 0, danh sách option hợp lệ và không có option 93. Có hạn dù còn 30 ngày cũng không tính.
5. Bonus chỉ cấp cho nhân vật chính, một lần cho cả bộ; không cấp riêng cho từng pet hoặc nhân bản sang đệ tử. Khi đủ bộ, mỗi chỉ số được thêm hệ số 1,10 ở pipeline phần trăm trang bị, trước các hiệu ứng sau đó. Ví dụ HP sau phần trăm trang bị 100.000 thành 110.000 trước các buff/giảm chỉ số khác; giữ cách làm tròn hiện tại.
6. Mất một loại thì mất bonus. Chuyển pet giữa bag/box/body vẫn sở hữu nên giữ bonus. Đăng nhập/tính lại nhiều lần không cộng dồn, không sửa hpg/mpg/dameg và không tự hồi đầy HP/KI.

## Các hướng đã cân nhắc

- **Đề xuất:** quét vật phẩm thật mỗi lần tính chỉ số; dễ xác minh, tối đa 7 bit, không thêm trạng thái sở hữu vào database.
- Dùng tiến độ danh hiệu Mê Rồng: không phù hợp vì có thể đã nhận/trùng loại và không phản ánh bộ hiện còn sở hữu.
- Lưu cờ mở khóa vĩnh viễn vào player: chỉ phù hợp nếu đổi yêu cầu thành “từng sưu tập đủ bộ”; không đúng ý nghĩa đang sở hữu trong câu mô tả.

## Task 1: Mô tả SQL và dữ liệu client

**Files:** Modify `team2026.sql`, `SRC/sql/nro1.sql`, `SRC/src/nro/models/data/DataGame.java`; Create `SRC/sql/migrations/20261004_dragon_collection_description.sql`.

- [x] Snapshot description 7 dòng database và backup hai dump trước khi sửa.
- [x] Tạo migration đúng phạm vi:

```sql
START TRANSACTION;
UPDATE item_template
SET description = 'Sở hữu 7 Pet Rồng Nhí Vĩnh Viễn sẽ tăng 10% HP, KI, Sức Đánh'
WHERE id BETWEEN 1765 AND 1771;
COMMIT;
```

- [x] Sửa trường description của đúng 7 dòng item_template trong cả hai dump. Giữ name/icon/parts và toàn bộ dragon_pet_config hiện có.
- [x] Đổi `DataGame.vsItem` từ 12 thành 13 nếu phiên bản hiện tại vẫn là 12; nếu đã thay đổi thì tăng từ phiên bản thực tế. Client tự tải lại description, không cần sửa Panel hoặc res.
- [x] Kiểm tra live SQL sau migration và hai dump đều có cùng 7 description; xác nhận icon/parts/options/rate/HSD không thay đổi.

## Task 2: Nhận diện bộ vĩnh viễn

**Files:** Create `SRC/src/nro/models/item/DragonPetCollection.java`; Test `SRC/test/nro/models/player/DragonPetCollectionTest.java`.

- [x] Viết test thiếu một loại, trùng loại, pet có hạn, slot rỗng, quantity 0, đủ bộ phân bố trong bag/box/body; chạy để ghi nhận FAIL trước triển khai.
- [x] Tách bộ nhận diện độc lập khỏi bộ đếm danh hiệu. Đề xuất API và thuật toán:

```java
public static boolean hasFullPermanentSet(Inventory inventory) {
    if (inventory == null) return false;
    int mask = 0;
    for (List<Item> slots : Arrays.asList(
            inventory.itemsBag, inventory.itemsBox, inventory.itemsBody)) {
        if (slots == null) continue;
        for (Item item : slots) {
            if (item == null || !item.isNotNullItem() || item.quantity <= 0
                    || item.itemOptions == null) continue;
            int id = item.template.id;
            if (id < 1765 || id > 1771) continue;
            boolean valid = true;
            for (Item.ItemOption option : item.itemOptions) {
                if (option == null || option.optionTemplate == null
                        || option.optionTemplate.id == 93) {
                    valid = false;
                    break;
                }
            }
            if (valid) mask |= 1 << (id - 1765);
        }
    }
    return mask == 0x7f;
}
```

- [x] Test cả option 93 param 0: vẫn không được tính. Test thêm một bộ thứ hai: điều kiện chỉ là true, không tăng số lần bonus.

## Task 3: Cộng chỉ số và cập nhật ngay khi sở hữu thay đổi

**Files:** Modify `SRC/src/nro/models/player/NPoint.java`, `SRC/src/nro/models/services/InventoryService.java`; Test `SRC/test/nro/models/player/DragonPetCollectionTest.java`.

- [x] Dùng fixture của `DragonPetStatsTest` để so sánh HP/KI/damage trước/sau khi có đủ bộ; bổ sung 20 lần calPoint, mất pet, chuyển rương, đăng nhập, đệ tử không được cấp bonus riêng.
- [x] Trong `NPoint.setPointWhenWearClothes`, sau vòng đọc option itemsBody, trước `setBasePoint`, thêm:

```java
if (!player.isPet && !player.isBoss && !player.isBot && !player.isNewPet
        && DragonPetCollection.hasFullPermanentSet(player.inventory)) {
    tlHp.add(10);
    tlMp.add(10);
    tlDame.add(10);
}
```

- [x] Dùng kết quả đã tính làm trạng thái bonus hiện tại trong NPoint, được reset và xác lập lại mỗi lần tính. Tại sendItemBags/sendItemBody/sendItemBox, so sánh điều kiện bộ hiện có với trạng thái lần tính trước; nếu thay đổi thì gọi `Service.gI().point(player)`. Giới hạn nhân vật chính và kiểm tra null. Không gọi cập nhật point bên trong predicate, không gửi gói cập nhật vô điều kiện mỗi lần gửi inventory.
- [x] Kiểm tra các luồng thêm, mở trứng, lấy thưởng, bán/bỏ, chuyển rương, chuyển trang bị để chắc chắn thao tác hoàn tất trước khi gửi inventory. Bổ sung cập nhật point sau mutation ở luồng thiếu gửi inventory; không tính ở giữa tháo item và gắn item.
- [x] Kiểm tra clamp HP/KI hiện tại khi mất bonus: không vượt max mới, không tự hồi khi nhận bonus. Không viết bonus vào dữ liệu player trong DB.

## Task 4: Kiểm thử, triển khai và rollback

- [x] Compile JDK 21 trên classpath active JAR + SRC/lib, chạy test mới từ classes trước active JAR. Ví dụ sau khi tạo test: `java -Dfile.encoding=UTF-8 -cp "../.analysis/dragon-collection-plan/test-classes;../.analysis/dragon-collection-plan/classes;20.jar;lib/*" nro.models.player.DragonPetCollectionTest` từ SRC. Kết quả mong đợi: toàn bộ case nhận diện/chỉ số/cập nhật trạng thái PASS.
- [x] Chạy lại DragonPetStatsTest các mode catalog/effects/combat/loot/egg, DragonPetResourceTest, VIP và các kiểm thử hợp thể đã có. Chạy trên JAR đóng gói để tránh source đúng nhưng JAR thiếu class.
- [ ] Kiểm tra client Game1/Game2 trên popup hẹp: câu mô tả xuống dòng, không bị cắt; relog tải vsItem mới. Kiểm tra số HP/KI/damage hiển thị thay đổi đúng khi nhận/mất loại cuối cùng.
- [x] Chỉ khi chuyển sang thực hiện: bảo trì lưu dữ liệu, xác nhận server dừng/port đóng; backup SRC/20.jar vào SRC/jar-backup tên riêng có thời gian, xác minh SHA-256; cài JAR mới, ghi metadata, khởi động và kiểm tra tải DB/port.
- [x] Rollback gồm JAR backup và 7 description cũ đã snapshot; không cần sửa inventory, chỉ số gốc hoặc dragon_pet_config. Gom application JAR staging dư vào jar-backup, giữ thư viện/tool JAR theo AGENTS.md.

## Phạm vi lần này

Chỉ kiểm tra và ghi kế hoạch. Chưa thay mô tả SQL, chưa thay source ứng dụng, chưa tăng vsItem, chưa cài JAR hoặc dừng server. Quy tắc sở hữu bag/box/body là đề xuất đang chờ phản hồi; kế hoạch phải cập nhật theo lựa chọn trước triển khai.


## Kết quả triển khai

Người dùng đã duyệt triển khai theo kế hoạch, gồm phạm vi bag/box/body. Đã cập nhật source, SQL và JAR, bảo trì lưu dữ liệu và khởi động thành công. 32/32 nhóm kiểm thử PASS; runtime catalog 13 và cả 7 mô tả được xác minh. Kiểm thử client thực tế chưa thực hiện. Chi tiết: [báo cáo triển khai](../../source-analysis/dragon-collection-bonus.md). Các ghi chú “chỉ lập kế hoạch/chờ chốt” ở trên là trạng thái trước khi người dùng duyệt.
