# Shop Santa KOL VIP và quà VIP 1–4

> Khi triển khai, dùng executing-plans và test-driven-development, hoàn thành từng mục rồi xác minh trước khi cập nhật server.

**Mục tiêu:** bán vé nhiệm vụ KOL VIP bằng ngọc xanh tại Santa và đồng bộ quà VIP mới với mô tả, túi đồ, hồng ngọc, dữ liệu lưu.

**Kiến trúc:** shop Santa dùng item1825 sẵn có, tab17/type_sell1. ToriBot lấy danh sách quà từ một catalog dùng chung cho menu, kiểm tra sức chứa và cấp quà. Giữ cơ chế thanh toán VND/receipt hiện có; không tạo món mới trùng thẻ radar.

**Công nghệ:** Java17, MariaDB team2026, PNG x1–x4, client Unity.

## Dữ liệu đã kiểm tra trực tiếp

| Nội dung | ID | Tên trong item_template |
|---|---:|---|
| Vé KOL VIP | 1825 | Vé Nhệm Vụ KOL Vip |
| Capsule hồng ngọc | 722 | Capsule hồng ngọc |
| Cuốn sách cũ | 1283 | Cuốn sách cũ |
| Bùa giám định | 1284 | Bùa giám định |
| Con dấu | 1794 | Con dấu |
| Thẻ Rồng thần Namek | 1204 | Mảnh Rồng thần Namếc |
| Thẻ Oozaru | 1791 | Mảnh khỉ Oorazu |
| Thẻ Oozarun1 | 1792 | Mảnh khỉ Oorazu 1 |
| Thẻ Oozarun2 | 1793 | Mảnh khỉ Oorazu 2 |

Các thẻ1204/1791/1792/1793 là TYPE33, cùng ID với radar. UseItem.UseCard tra radar bằng item.template.id. Vì tên khác và lỗi chính tả Oorazu, tìm tên Thẻ Oozaru ở item_template không ra. Không dùng item1771 của bộ res moi: ở server hiện tại đó là Rồng nhí1 sao. Không dùng Con dấu1851 nguồn khác: server dùng1794 và DoiSachTuyetKy đã tra1794.

Vé1825 chưa có trong item_shop. KOL VIP đang yêu cầu vé trong hành trang; không cần sửa điều kiện thành VIP ToriBot. Capsule722 mở nhận1–100 hồng ngọc và1 vé tặng,80% nhận1–5; x10 capsule không có nghĩa10 hồng ngọc cố định. Đã xác minh36 PNG của9 icon ở đủ bốn zoom, không thiếu/hỏng.

## Bảng quà yêu cầu

| Quà thay/thêm | VIP1 | VIP2 | VIP3 | VIP4 |
|---|---:|---:|---:|---:|
| Hồng ngọc trực tiếp thay thỏi vàng | 200 | 500 | 700 | 1.000 |
| Capsule722 thay10 phiếu giảm giá459 | 10 | 10 | 10 | 10 |
| Cuốn sách cũ1283 | 1.000 | 2.000 | 3.000 | 4.000 |
| Con dấu1794 | 100 | 100 | 100 | 100 |
| Bùa giám định1284 | 100 | 100 | 100 | 100 |
| Thẻ/mảnh thêm | Namek1204×1.000 | Oozaru1791×1.000 | Oozarun1 1792×1.000 | Oozarun2 1793×1.000 |

Người dùng đã chốt: VIP1–4 nhận200/500/700/1.000 hồng ngọc trực tiếp; vé1825 bán giá1.000 ngọc xanh/vé. “Sách vũ VIP2” được hiểu là Cuốn sách cũ1283 theo các cấp khác; không có item riêng tên này trong kết quả truy vấn.

Giữ các quà cũ ngoài thỏi vàng/phiếu giảm giá: thú cưỡi, búa, cải trang, đá bảo vệ,10 thẻ đội trưởng vàng VIP3,20 mảnh Namek VIP4. Thẻ mới là bổ sung theo yêu cầu “thêm”. Giá VIP hiện tại50k/100k/150k/200k VND, thời hạn30 ngày/gói và tối đa4 lần mua/mùa được giữ.

## Task1: Vé Santa và SQL

Files: SRC/sql/migrations/20261006_santa_kol_vip_rewards.sql (mới), team2026.sql, SRC/sql/nro1.sql, SRC/src/nro/models/data/DataGame.java nếu đổi tên/mô tả template.

- [x] Chốt giá vé1.000 ngọc xanh và ruby200/500/700/1.000 cho VIP1–4.
- [ ] Test vé1825 chỉ có1 dòng tab17, currency1/cost đúng; mua dùng ngọc xanh và không trừ hồng ngọc, hết ngọc/túi đầy không mất phí.
- [ ] Migration idempotent thêm vé1825 vào tab17 shopSANTA; tránh trùng dòng khi chạy lại.
- [ ] Sửa tên vé thành “Vé nhiệm vụ KOL VIP”, bổ sung mô tả đúng chức năng; nếu sửa tên thẻ đồng bộ item và radar, giữ ID.
- [ ] Đồng bộ2 SQL dump và tăng vsItem nếu sửa template; xác minh icon15293 đủ zoom.

## Task2: Catalog quà và mô tả VIP

Files: SRC/src/nro/models/npc_list/ToriBot.java; SRC/src/nro/models/services/VipRewardCatalog.java (mới); SRC/test/nro/models/services/VipRewardCatalogTest.java (mới).

- [ ] Test từng VIP cấp đúng số lượng/tên món theo bảng, không còn457/459; mô tả lấy cùng catalog với cấp quà.
- [ ] Catalog giữ các option/HSD quà hiện tại, bổ sung722/1283/1794/1284 và4 loại thẻ theo cấp.
- [ ] Ruby cộng trực tiếp inventory.ruby theo lượng đã chốt, kiểm tra tràn số trước mua và gửi cập nhật tiền cho client.
- [ ] Cập nhật menu ToriBot từ catalog để tên/mô tả và quà thật luôn thống nhất; nút Đóng không kích hoạt BuyVip (hiện confirmMenu case1–4 chưa kiểm tra select).

## Task3: Cấp quà đủ và thanh toán

Files: ToriBot.java; VipRewardCatalog.java; SRC/test/nro/models/npc_list/ToriBotVipRewardTest.java (mới). Đối chiếu InventoryService.java, WalletPurchaseService.java, PlayerDAO.java; chỉ thay phần liên quan khi cần.

- [ ] Test quà có thể cộng dồn vào ô sẵn có, không yêu cầu số ô trống cố định. Sách4.000/thẻ1.000 vừa giới hạn stack hiện tại.
- [ ] Chuẩn bị tất cả item và mô phỏng sức chứa chung cả gói trước gọi PlayerDAO.purchase; không kiểm từng món riêng rồi giả định cả gói vừa túi.
- [ ] Dưới khóa player, chỉ cấp khi đủ toàn bộ quà. Hiện VIP1–4 gọi addItemBag liên tiếp và bỏ qua return, có thể nhận thiếu sau trừ tiền.
- [ ] Test túi thiếu chỗ, lỗi cấp quà, callback bị gọi lại, giao dịch pending, mở lại menu và logout; không cấp trùng/không mất quà khi debit đã commit.
- [ ] Với giao dịch pending, phải giữ hoặc kiểm lại sức chứa lúc reconcile; nếu cần khóa thay đổi hành trang hoặc lưu gói chờ nhận, bảo đảm khoản đã thanh toán có đường nhận đủ quà. Không tuyên bố rollback VND khi chưa xử lý receipt.
- [ ] Lưu kiểm tra ruby/quà/VIP count/time sau logout-login; gửi thông báo thành công sau khi cấp đủ.

## Task4: Triển khai khi người dùng yêu cầu

- [ ] Compile đầy đủ, chạy test VIP mới cùng test wallet/KOL/shop/inventory hiện có.
- [ ] Sao lưu SQL và res nếu cần; bảo trì có lưu dữ liệu, xác minh JVM ứng dụng đã dừng.
- [ ] Chuyển20.jar cũ vào SRC/jar-backup với thời gian riêng; kiểm SHA256, cài JAR đã test, metadata/rollback; gom JAR ứng dụng dư.
- [ ] Khởi động server; probe chỉ đọc xác minh shop vé, item/radar, catalog và hash class; thử mua từng VIP trên tài khoản kiểm thử, xác minh đủ quà và số tiền.

Trạng thái: chỉ kiểm tra và lập kế hoạch, chưa thay SQL/source/JAR đang chạy cho yêu cầu này.
