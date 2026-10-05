# Nâng SKH VIP — kết quả triển khai 2026-10-02

Đã bổ sung [Nâng SKH Thường](skh-thuong.md); submenu hiện đủ Thường/VIP/Đóng. JAR mới nhất bao gồm cả hai nhánh và đã chạy 36 lượt test; các con số 28 lượt bên dưới mô tả lần kiểm chứng VIP đầu tiên.

Đã nối tính năng tại Bà Hạt Mít map 5 → Nâng SKH → Nâng SKH VIP. Server nhận 1–5 món hủy diệt trước, rồi cùng số món thần phía sau; ghép theo thứ tự tương ứng. Phôi tùy hành tinh/loại nhưng không trùng loại trang bị. Các món chính được phép trùng loại hoặc khác hành tinh nhân vật.

Mỗi cặp thu 1.000.000.000 vàng, tối đa 5.000.000.000 vàng/lượt, dùng long. Thành công độc lập 50%: thay món hủy diệt bằng SKH mới và dùng phôi. Xịt: chỉ dùng phôi, giữ đúng instance hủy diệt cùng toàn bộ option/sao/cấp nâng. Phí thu cả cặp xịt.

Đầu ra giữ loại và hành tinh của hủy diệt; riêng nhẫn chung 656 sinh rada có set theo hành tinh người chơi 0/1/2. Không lấy hành tinh phôi, không coi gender=3 của rada là Xayda. Preview ghi hành tinh từng cặp và kiểm tra lại khi xác nhận.

Cấp 1–8 có tổng 95%, cấp 9–12 có tổng 5% trong món thành công; random đều cấp trong mỗi nhóm và đều năm set của hành tinh. Chỉ số nền lấy tối đa của đúng template từ shop làng 1/2/3, tạo ItemOption mới, thêm đủ hiệu ứng từ ActivationSet. Quần/giày cấp 1 có HP/KI mà không có hồi phục trong SQL; giữ đúng dữ liệu này. Không kế thừa option nguyên liệu vào món mới.

## Source chính

- `SRC/src/nro/models/combine/NangSKHVip.java`: chọn nguyên liệu, preview, fingerprint, random, phí và commit các slot được chọn.
- `SRC/src/nro/models/item/SKHCapsuleCatalog.java`: danh mục VIP đủ 12 cấp và danh mục capsule cũ giữ nguyên các phần tử/thứ tự cũ.
- `SRC/src/nro/models/services/ItemService.java`: builder max chỉ số; capsule dùng hồ sơ cũ qua catalog chung.
- `Combine.java`, `CombineService.java`, `BaHatMit.java`, `ConstNpc.java`: trạng thái preview, route, tab và menu xác nhận/hủy.
- `TransactionService.isTrading`: kiểm tra giao dịch đang tồn tại mà không khởi tạo worker nền.

Toàn bộ output được tạo trước khi trừ vàng/tiêu hao. Khi ghi túi, chỉ thay slot đồ chính thành công và các slot phôi, tránh ghi đè phần thưởng/đồ nhặt thêm ở ô khác. Xác nhận lặp, cooldown, nguyên liệu đổi/di chuyển/sửa option, hành tinh rada thay đổi, túi thay instance và đang giao dịch đều bị chặn. Đây là bảo vệ luồng VIP trong bộ nhớ; không thay cơ chế lưu database hoặc đồng bộ toàn bộ các luồng hành trang khác.

## Kiểm chứng

Chạy `python .analysis/verify_gold_economy.py`: `COMPILE_EXIT=0`, 28 lượt test `EXIT=0`. Compile toàn bộ source/test Java 17; compiler còn cảnh báo unchecked đã có trong dự án.

- 17 lượt hồi quy kinh tế vàng, pha lê, mapping và hiệu ứng SKH.
- SKHVipCatalogTest: 15 nhóm danh mục, max chỉ số, deep copy, lỗi dữ liệu, bảo toàn capsule cũ và sáu món quần/giày cấp 1.
- SKHVipSqlTest: 900 tổ hợp từ SQL thật = 15 nhóm × 12 cấp × 5 set; đối chiếu chỉ số qua join item_shop → tab_shop → shop và item_shop_option. Bao phủ rada với đủ năm set cho cả ba hành tinh.
- NangSKHVipTest: chín mode validation/random/fees/success/failure/mixed/inventory/replay/menu. Có lượt 5 cặp trộn hành tinh, thiếu vàng một đơn vị, túi đầy, tất cả xịt, thành công/xịt hỗn hợp, hai thread xác nhận và ô ngoài lựa chọn nhận đồ giữa lúc commit.

Đã quan sát các test thất bại trước khi sửa: thiếu tính năng/catalog; quần cấp 1 bị yêu cầu option 27; đồ nhận thêm ở ô ngoài lựa chọn bị ghi đè. Sau sửa, các ca đều đạt. Review yêu cầu và review chất lượng đã kiểm tra lại; lỗi ghi đè túi được khắc phục bằng ghi riêng các slot đã chọn.

Audit JSON đã sửa tên trường: `shop_id` là shop làng thật; `item_shop_id` là ID dòng bán hàng. Không thay dữ liệu SQL.

## Phần cần thử trên môi trường chạy

Chưa có client thật trong workspace để kiểm tra giao diện nhận đủ 2/4/6/8/10 món và giữ thứ tự, hoặc xác minh dữ liệu sau đăng nhập lại. Server và test offline hỗ trợ 10 món. Nếu client giới hạn số ô, cần triển khai giao diện chọn hai bước theo kế hoạch, giữ nguyên tối đa 5 cặp/lượt.

Đã đóng gói và thay `SRC/20.jar` sau khi compile riêng 551 file source production và chạy lại 28 lượt kiểm thử trên chính JAR. Bản cũ được chuyển nguyên vẹn vào `SRC/jar-backup/20-before-skh-vip-20261002-133621.jar`, kiểm tra SHA-256 khớp. Hướng dẫn quay về bản cũ: [jar-backup/README.md](../../SRC/jar-backup/README.md). Không có tiến trình Java chạy lúc thay; chưa khởi động server, chưa sửa database.
