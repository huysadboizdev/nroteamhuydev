# Cửa hàng hồng ngọc và điều kiện mua ký gửi

Santa → Cửa hàng hồng ngọc dùng một tab có sẵn (shop37/tab64). Hộp và nguyên liệu nằm cùng các món cũ; tab Hộp/Trang bị riêng được bỏ sau khi chuyển hộp.

| ID | Vật phẩm | Số lượng mỗi lần mua | Giá hồng ngọc |
| --- | --- | --- | --- |
| 1775 | Hộp đồ Thần Linh | 1 | 50.000 |
| 934 | Mảnh hồn bông tai | 1.000 | 100 |
| 933 | Mảnh vỡ bông tai dùng nâng cấp 2 | 1.000 | 100 |
| 1820 | Mảnh vỡ bông tai cấp 3 | 1.000 | 100 |
| 935 | Đá xanh lam | 1 | 10 |

Mô tả hộp: **Mở ra được set Thần Linh**. Menu chọn vẫn có Namếc, Trái Đất, Xayda. Đồ nhận giữ chỉ số gốc 0% roll thêm, không giao dịch/ký gửi như yêu cầu trước.

Ba loại mảnh dùng option31=1000, quantity=1 vì dịch vụ nâng cấp đọc/trừ option31. Mua lần tiếp theo cộng số mảnh vào option31; không nhân quantity thêm lần nữa. Đá xanh lam dùng quantity=1. Giá được server đối chiếu với danh sách cố định, chỉ trừ ruby. Tiền, hành trang và receipt ruby_material ghi cùng transaction; chưa cấp được đồ thì không trừ tiền. Lỗi commit chưa rõ giữ khóa và đối soát trước khi cho phép giao dịch tiếp.

## Mua ký gửi

Người mua phải có tài khoản đã kích hoạt, đang online và có sức mạnh **từ 17.000.000.000**. Cần đủ ruby theo toàn bộ giá tin đăng và đủ chỗ nhận toàn bộ hàng; một lượt mua lấy cả số lượng của tin. Không mua tin do chính nhân vật đăng, tin đã bán/mất/đổi sau khi hiển thị, tin cũ bằng vàng/ngọc xanh, hoặc món không hợp lệ.

Không mua trong lúc giao dịch trực tiếp, tradeWVP, mua ví, nhận/tặng ruby, nhận thưởng KOL/tiến độ nhiệm vụ, đổi tên hoặc giao dịch trước đang chờ đối soát. Túi đầy vẫn có thể nhận vật phẩm nếu cộng được vào stack tương thích. Người mua không trả phí đăng/lên đầu hay phí bán 10%; phí đó thuộc bên bán.

Điều kiện 17 tỷ được giữ nguyên. Thông báo tài khoản chưa kích hoạt được tách rõ khỏi thông báo nhân vật không sẵn sàng, và hướng dẫn NPC bổ sung điều kiện mua.

Đã kiểm tra transaction ở database riêng và các trường hợp thiếu tiền, đầy túi, mua lặp, mua đồng thời, rollback, receipt/đối soát; chưa có thông báo lỗi cụ thể hoặc phiên lỗi của người dùng nên chưa xác định được nguyên nhân lỗi họ gặp trong game. Không thử mua thật hay sửa tiền/hành trang tài khoản người dùng để kiểm tra.

Migration: `SRC/sql/migrations/20261005_ruby_shop_materials.sql`, chạy sau migration hộp1775. Metadata triển khai: `.analysis/ruby-shop-deployment-latest.json`; log kiểm tra: `.analysis/consign-latest.json`.

## Triển khai đã kiểm chứng

38 nhóm kiểm tra đạt. Đã bảo trì lưu dữ liệu, sao lưu toàn bộ SQL, chuyển JAR cũ vào jar-backup và kiểm tra SHA-256 cả hai bản. Server khởi động lại và TCP localhost:14445 kết nối thành công. SQL thực tế có đúng một tab64; mô tả hộp, giá và số mảnh đúng bảng trên. Hai dump đã nối migration mới.

JAR SHA-256: `d6cbc7b9a24e3d455c3f1705de0c9f850665f71545e173ff2861ecb69d79be98`. Backup: `SRC/jar-backup/20-before-ruby-shop-20261005-121756.jar`. Snapshot: `.analysis/ruby-shop-deploy-20261005-121756/team2026-before-ruby-shop.sql`.
