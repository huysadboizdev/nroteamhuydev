# Hành trang và rương nhà 200 ô — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mở sẵn 200 ô hành trang và 200 ô rương nhà cho mọi nhân vật cũ/mới, giữ nguyên đồ, tiền, option và các quy tắc chuyển hóa đã sửa.

**Architecture:** Đồng bộ sức chứa, cách đọc chỉ số ô và kích thước gói tin giữa Java server và cả Game1/Game2 của Unity client. SQL lưu sức chứa bằng độ dài mảng JSON; chuyển đổi bằng cách nối ô trống vào cuối mảng, không tạo lại đồ hiện có. Client cũ phải được yêu cầu cập nhật trước khi nhận dữ liệu 200 ô.

**Tech Stack:** Java server, Unity C#, MySQL/MariaDB `team2026`, JSON lồng dạng chuỗi.

**Trạng thái:** Đã triển khai source server/client, migration SQL và JAR chạy ngày 03/10/2026. Đây là kế hoạch gốc; kết quả từng luồng và giới hạn kiểm chứng ghi tại `docs/source-analysis/inventory-200-validation.md`. Bản client Windows đã build, hai tab đã kiểm thử trong Unity; chưa đăng nhập tài khoản người chơi để chơi thủ công.

## Phạm vi đã chốt

- Hành trang 200 ô và rương nhà 200 ô; mở sẵn cho cả nhân vật cũ và mới.
- Không tác động ô trang bị trên người, rương vòng quay, hành trang đệ tử.
- Không thu phí mở ô. Vật phẩm mở rộng 517/518 khi đủ 200 phải bị từ chối và giữ nguyên số lượng.
- Giữ quy tắc chuyển hóa: bậc 11/12, tối thiểu +4; cấm đồ kích hoạt ở cả nguồn và đích; vàng giữ cấp/% và bỏ số sao; ngọc giữ cả sao.

## Kết quả kiểm tra hiện tại

| Thành phần | Hiện tại | Điểm cần sửa |
|---|---|---|
| Server | Trần hành trang 80, rương nhà 100 | `SRC/src/nro/models/player/Inventory.java:22` |
| Nhân vật mới | 30 ô mỗi nơi | Hai vòng khởi tạo trong `SRC/src/nro/models/database/PlayerDAO.java` |
| Nhân vật đang lưu | 2 nhân vật; hành trang 30 và 70; rương đều 30 | Mảng JSON quyết định số ô đang mở |
| Client Game1/Game2 | Mảng động; một số gói đọc unsigned byte, một số đọc signed byte | 200 thành -56; chỉ số ô 199 thành -57 |
| SQL | `items_bag`, `items_box` là TEXT, tối đa 65.535 byte mỗi cột | Không có cột riêng giới hạn số ô |
| Gói tin | Các gói -30/-35/-36 dùng độ dài 2 byte | Payload vượt 65.535 byte làm sai framing |

Bằng chứng dung lượng trong `.analysis/inventory-capacity-audit.json`: hành trang hiện 931/3.070 byte; ước lượng sau nối ô trống là 3.991/5.410 byte. Rương hiện 901 byte, ước lượng 3.961 byte sau nối. Đây là ước lượng cho dữ liệu hiện tại, dùng timestamp 0 cho ô trống, không phải giới hạn của trường hợp 200 món đầy option.

`@@max_allowed_packet` hiện 1 MiB. SQL mode chưa bật STRICT; cần kiểm tra lỗi/cảnh báo khi lưu, không dựa vào khả năng tự cắt TEXT. Không thay SQL mode toàn server trong hạng mục này.

Các lỗi nguồn đã xác nhận:

1. `InventoryService.getCountEmptyBag()` và biến đếm dùng `byte`: 200 ô trống trả về âm.
2. `InventoryService.getIndexBag()` trả về `byte`, vòng lặp dùng `byte`: tràn tại chỉ số 128.
3. `InventoryService.sendItemBags():585` gửi tổng số ô nhưng `continue` khi ô trống, thiếu `writeShort(-1)`. Gói đăng nhập và gói rương có gửi ô trống.
4. `Service.java:827` gửi đăng nhập bằng subcommand 0 của command -30. `Controller.cs` Game1:5471/5506 và Game2:5464/5499 dùng `readByte()` để tạo mảng hành trang/rương.
5. Client -36/-35 đọc số ô bằng unsigned byte nhưng cập nhật một ô bằng signed byte. Gói -43 và `Panel.itemRequest` cũng dùng chỉ số signed.
6. `UseItem`, giao dịch, lựa chọn đồ ghép có đường đọc chỉ số signed. Chỉ sửa trần không sửa các đường này sẽ làm ô 128–199 không dùng được.

## Task 1: Quy tắc sức chứa và dữ liệu nhân vật

**Modify:** `SRC/src/nro/models/player/Inventory.java`, `SRC/src/nro/models/database/PlayerDAO.java`, `SRC/src/nro/models/database/MrBlue.java`, `SRC/src/nro/models/services/InventoryService.java`.

**Create test:** `SRC/test/nro/models/player/InventoryCapacityTest.java`.

- [ ] Thêm kiểm thử: nhân vật mới có hai danh sách dài 200; nhân vật cũ 30/70 được nối thành 200; toàn bộ phần tử cũ giữ nguyên giá trị và thứ tự. Chạy kiểm thử trước sửa để xác nhận thất bại vì mặc định 30/trần 80–100.
- [ ] Đổi hai hằng thành `MAX_ITEMS_BAG = 200`, `MAX_ITEMS_BOX = 200`; dùng hằng trong hai vòng tạo ô mới của DAO.
- [ ] Sau giải mã thành công ở loader, nối ô bằng `ItemService.gI().createItemNull()` đến 200. Không dùng cùng một đối tượng Item trống cho nhiều ô.

```java
while (player.inventory.itemsBag.size() < Inventory.MAX_ITEMS_BAG) {
    player.inventory.itemsBag.add(ItemService.gI().createItemNull());
}
while (player.inventory.itemsBox.size() < Inventory.MAX_ITEMS_BOX) {
    player.inventory.itemsBox.add(ItemService.gI().createItemNull());
}
```

- [ ] Dữ liệu không parse được hoặc dài hơn 200: báo lỗi rõ kèm ID nội bộ cho quản trị, không cắt mảng, không ghi danh sách rỗng đè dữ liệu. Chặn vào game nếu dữ liệu không thể biểu diễn an toàn.
- [ ] Đổi trả về và biến đếm của `getCountEmptyBag` sang `int`; đổi `getIndexBag` và biến vòng lặp sang `int`, giữ -1 khi không tìm thấy. Đổi người gọi ở `shop_ky_gui/ConsignShopService.java` sang int.
- [ ] Kiểm thử 200 ô trống cho kết quả 200, ô cuối cho chỉ số 199; vật phẩm 517/518 bị từ chối khi đủ ô và không bị tiêu hao.

## Task 2: Chỉ số ô và kiểm soát thao tác trên server

**Modify:** `SRC/src/nro/models/services_func/UseItem.java`, `SRC/src/nro/models/services_func/Trade.java`, `SRC/src/nro/models/services_func/TransactionService.java`, `SRC/src/nro/models/server/Controller.java`, `SRC/src/nro/models/combine/CombineService.java` và người gọi các phương thức đổi kiểu.

**Create test:** `SRC/test/nro/models/network/InventorySlotWireTest.java`.

- [ ] Tạo fixture gửi chỉ số 0, 127, 128, 199. Thêm payload sai: 200, 254, gói thiếu byte, chọn lặp lại một món, số món vượt danh sách. Xác nhận lỗi đường signed trước sửa.
- [ ] Đọc chỉ số bằng `readUnsignedByte()` và giữ bằng `int`. Action/type/where vẫn giữ ý nghĩa signed hiện có; không thay toàn bộ `readByte()` một cách đại trà.
- [ ] Các đường có sentinel -1 như tiền giao dịch hoặc dùng vật phẩm theo template: chỉ ánh xạ 255 về -1 ở đúng nhánh có sentinel. Ở thao tác ô thông thường 255 là không hợp lệ.

```java
int slot = msg.reader().readUnsignedByte();
if (slot >= player.inventory.itemsBag.size()) {
    return; // Không đổi đồ, tiền hoặc trạng thái ghép.
}
Item selected = player.inventory.itemsBag.get(slot);
```

- [ ] `Trade.addItemTrade/removeItemTrade` nhận int; kiểm tra phạm vi, số lượng dương và không vượt số lượng hiện có trước sửa trạng thái. Không để giá trị tiền sentinel đi vào truy cập mảng.
- [ ] Gói -81: đọc unsigned số món/chỉ số; xác nhận đủ payload, phạm vi và không trùng trước thêm danh sách đồ ghép. Giữ kiểm tra lại item và snapshot khi thực hiện chuyển hóa.
- [ ] Chạy kiểm thử hành động với ô 199: dùng, chuyển hành trang↔rương, bán, ký gửi, giao dịch, ghép/chuyển hóa. Packet sai phải không trừ tiền, không mất/nhân đôi đồ.

## Task 3: Gửi danh sách đủ ô và tương thích client

**Modify server:** `SRC/src/nro/models/services/InventoryService.java`, `SRC/src/nro/models/services/Service.java`, `SRC/src/nro/models/network/MySession.java`, `SRC/src/nro/models/network/MessageSendCollect.java`.

**Modify client, cả hai thư mục:** `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1` và `Game2`, các file `Controller.cs`, `Service.cs`, `Panel.cs`, `Session_ME.cs`, `Session_ME2.cs`. Thêm trạng thái capability vào lớp wire hiện có hoặc lớp riêng nhỏ có trách nhiệm tương đương.

- [ ] Sửa ô trống của `sendItemBags` thành:

```java
if (!item.isNotNullItem()) {
    msg.writer().writeShort(-1);
    continue;
}
```

- [ ] Decoder kiểm thử phải đọc đúng 200 ô và hết đúng payload với danh sách rỗng, đầy, xen kẽ; không còn byte thừa/thiếu. Kiểm tra cả đăng nhập -30, rương -35 và hành trang -36, giữ option int32 đã có.
- [ ] Thêm capability `INV200` vào cả hai lời khai báo platform của client; server ghi nhận riêng trong session. Giữ capability `IOP32` hiện có. ACK -29/subcommand 100 hiện gửi một boolean IOP32: nối thêm boolean INV200; client mới chỉ đọc boolean bổ sung khi còn byte, mặc định false nếu thiếu. ACK vẫn dùng framing cũ.
- [ ] Client/server reset capability mỗi lần kết nối. Trước gửi dữ liệu nhân vật 200 ô, server yêu cầu INV200; client cũ nhận thông báo cần cập nhật và kết thúc đăng nhập, không nhận mảng bị cắt. ACK phải được xếp trước gói dữ liệu nhân vật trên cùng luồng gửi.
- [ ] Với session INV200 đã xác nhận, gói -30/-35/-36 luôn dùng framing 3 byte cho mọi kích thước, theo đúng thứ tự byte và XOR của nhánh lớn sẵn có. Client chọn `readMessage2` theo cùng ba command khi đã nhận ACK. Không chọn framing theo size đơn thuần vì bên nhận chưa biết size để chọn decoder.
- [ ] Mọi subcommand -30 trong session INV200 dùng cùng framing; không chỉ đổi subcommand đăng nhập. Đồng bộ cả Session_ME và Session_ME2 khi chúng dùng luồng nhận này. Trường hợp payload null vẫn phải gửi đúng 3 byte độ dài 0 cho command đã đổi.
- [ ] Kiểm thử framing với payload 65.535, 65.536 và 100.000 byte, nhiều gói nối tiếp, có XOR; không lệch khóa hoặc nuốt gói tiếp theo. Đặt giới hạn payload lớn hợp lý và từ chối vượt giới hạn bằng lỗi quản trị trước khi ghi header; không cắt dữ liệu.
- [ ] Đếm ô ở đăng nhập đọc unsigned byte. Chỉ số cập nhật ô -35/-36, -43 đọc unsigned và dùng int. `Panel.itemRequest`, `Service.useItem/getItem/giaodich` nhận chỉ số int; rà toàn bộ người gọi để bỏ biến/cast sbyte gây mất thông tin trước gọi. Byte trên dây vẫn đủ biểu diễn 0–199; sentinel 255 được giữ đúng nhánh.

## Task 4: SQL migration giữ nguyên đồ

**Create:** `SRC/sql/migrations/20261003_inventory_200_schema.sql`, `.analysis/migrate_inventory_200.py`, `.analysis/verify_inventory_200.py`.

**Modify:** `SRC/sql/nro1.sql:9393` và `team2026.sql` trong thư mục gốc (chỉ schema hai cột; dữ liệu seed xử lý bằng migration, không thay toàn mảng thủ công).

- [ ] Migration schema riêng, chỉ hai cột:

```sql
ALTER TABLE player
  MODIFY COLUMN items_bag MEDIUMTEXT NOT NULL,
  MODIFY COLUMN items_box MEDIUMTEXT NOT NULL;
```

- [ ] Giữ charset/collation gốc khi chỉnh schema; MEDIUMTEXT tối đa 16.777.215 byte/cột, không có nghĩa cho phép hơn 200 ô. DDL có implicit commit nên không gộp cùng transaction dữ liệu rồi giả định có thể rollback cả schema.
- [ ] Script có chế độ dry-run mặc định. Parse JSON outer và từng chuỗi item/options; xác nhận mảng, cấu trúc, quantity/options hợp lệ. Lấy định dạng ô trống từ serializer DAO thực, không dùng timestamp 0 ước lượng trong audit làm chuẩn triển khai.
- [ ] Nối đúng `200 - len(old)` chuỗi ô trống; giữ nguyên từng chuỗi item cũ, gồm template, quantity, option trùng, createTime. Không đổi `data_inventory` hay cột khác. Nếu dữ liệu lỗi hoặc >200: dừng và báo hàng cần xử lý, không tự cắt hoặc thay rỗng.
- [ ] Dry-run xuất chỉ ID nội bộ, số ô trước/sau, tổng đồ và SHA-256 dữ liệu trước/sau để đối chiếu; snapshot dữ liệu thật phải nằm trong backup cục bộ bảo vệ, không in đồ/người chơi ra log công khai.
- [ ] Khi server đã dừng và lưu xong: transaction dữ liệu, `SELECT ... FOR UPDATE`; chỉ UPDATE nếu hai giá trị hiện tại khớp snapshot dry-run. Thất bại một hàng rollback transaction. Chạy lại phải không thay nội dung đã đủ 200.
- [ ] Verify sau ghi: mỗi mảng đúng 200; prefix chuỗi cũ giống tuyệt đối; các ô thêm đều trống; tổng đồ/số lượng/options và tiền giữ nguyên. Thử save/load qua DAO thật trên DB kiểm thử với schema sao chép, không chỉ JSON_LENGTH.
- [ ] Dữ liệu 200 món nhiều option phải lưu/đọc đúng; kiểm tra warning SQL và giới hạn gói prepared update (gồm cả hai cột và phần player khác). Chỉ nâng max_allowed_packet nếu payload kiểm thử vượt 1 MiB; ghi cấu hình khởi động tương ứng nếu cần.

## Task 5: UI, kiểm thử tích hợp và phát hành

**Create:** `docs/source-analysis/inventory-200-validation.md` để ghi kết quả thực tế và phiên bản đã kiểm thử.

- [ ] Kiểm tra Unity biên dịch cả Game1/Game2 và tất cả người gọi API đã đổi. Kiểm tra tìm toàn source không còn chỉ số inventory bị giữ trong sbyte hoặc vòng lặp byte.
- [ ] Trên cả hai tab, ở chế độ danh sách và lưới: hiển thị, cuộn, chọn ô đầu/cuối, trang cuối, dùng/chuyển món ô 199; kiểm tra offset đồ trên người trong tab hành trang. Không đổi số ô đồ trên người.
- [ ] Nhặt/quà/shop khi trống 200 phải hoạt động; đầy 200 phải từ chối mà không mất tiền/vật phẩm. Giao dịch lúc đầy, cùng lúc cập nhật đồ, đăng xuất/đăng nhập lại đều giữ đúng item.
- [ ] Chạy hồi quy chuyển hóa gồm 768 tình huống đồ kích hoạt, vàng/ngọc, sao/%/cấp và chống đổi đồ sau preview; giữ các kiểm tra option int32/quantity lớn hiện có. Không sửa quy tắc chuyển hóa trong hạng mục này.
- [ ] Kiểm thử client cũ với server mới: nhận yêu cầu cập nhật rõ ràng; client mới với server cũ không nhận INV200 nên dùng framing cũ. Hai tab kết nối độc lập không dùng nhầm trạng thái capability của nhau.
- [ ] Trước triển khai: build client và JAR staging, đối chiếu test, backup DB/schema và bản client hiện tại. Dừng server bằng bảo trì có lưu dữ liệu; xác nhận PID/port đã dừng trước migration và thay JAR.
- [ ] Chuyển JAR cũ vào `SRC/jar-backup` tên timestamp riêng, so SHA-256, ghi metadata đúng AGENTS.md; cài JAR mới rồi khởi động duy nhất một server. Không di chuyển thư viện hoặc JAR công cụ bảo trì.
- [ ] Phát hành client hỗ trợ INV200 trước mở server cho người chơi; xác nhận số ô/đồ qua đăng nhập thực tế, lưu và khởi động lại. Chỉ đánh dấu hoàn tất sau kiểm thử client chạy được với server thật.
- [ ] Nếu cần phục hồi: bảo trì/lưu và chụp lại DB sau nâng trước mọi thao tác; phục hồi theo bộ JAR/client/schema/dữ liệu đã xác định. Không chỉ trả JAR cũ khi DB còn 200 ô, không khôi phục snapshot cũ làm mất đồ phát sinh sau nâng mà chưa đối chiếu.

## Lệnh kiểm tra khi triển khai

Audit hiện trạng chỉ đọc, có thể chạy lại:

```powershell
python .analysis/audit_inventory_capacity.py
python .analysis/verify_conversion.py
```

Lệnh thứ hai xác nhận hồi quy chuyển hóa; không kiểm chứng nâng ô. Script kiểm thử nâng ô được tạo ở Task 4 phải trả mã thoát 0 chỉ khi server tests, protocol fixtures và SQL round-trip đều đạt; bước Unity/runtime vẫn cần kết quả riêng.

Các tiêu chí bắt buộc trước phát hành: 200/200 cho cũ và mới; ô 128–199 dùng được; không mất đồ sau relog; payload lớn không lệch; client cũ bị chặn trước nhận inventory; đồ kích hoạt vẫn bị chặn chuyển hóa.
