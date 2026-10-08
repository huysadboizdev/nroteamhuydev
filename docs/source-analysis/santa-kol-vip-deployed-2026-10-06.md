# Santa KOL VIP và quà VIP 1–4

Đã cài src, migration SQL và JAR mới ngày 06/10/2026. Shop Santa `SANTA`, tab 17 bán Vé nhiệm vụ KOL VIP (1825) với giá 1.000 ngọc xanh. `vsItem=19` để client tải lại tên vé.

| VIP | Hồng ngọc trực tiếp | Sách cũ 1283 | Mảnh thẻ x1.000 |
|---|---:|---:|---|
| 1 | 200 | 1.000 | Rồng thần Namek 1204 |
| 2 | 500 | 2.000 | Oozaru 1791 |
| 3 | 700 | 3.000 | Oozarun1 1792 |
| 4 | 1.000 | 4.000 | Oozarun2 1793 |

Mỗi gói nhận thêm 10 capsule hồng ngọc 722 thay phiếu giảm giá, 100 Con dấu 1794 và 100 Bùa giám định 1284. Giữ nguyên các phần quà cũ khác, chỉ số, hạn dùng, điểm mùa, giới hạn lượt mua; VIP4 vẫn có 20 mảnh Namek cũ.

Danh sách dùng chung `VipRewardCatalog` phục vụ mô tả, kiểm tra toàn bộ sức chứa và cấp quà. Túi không đủ trước thanh toán thì từ chối, chưa trừ điểm. Khi thanh toán phải đối soát trễ mà túi đầy hoặc đang giao dịch, toàn bộ quà vật phẩm vào rương phụ Thượng Đế → Quay số → Rương phụ. Nhận từ rương chỉ xóa món khi thêm túi thành công; cho phép cộng dồn dù túi không còn ô trống.

Bảo toàn metadata của vật phẩm cũ khi chuẩn bị túi. Giữ đối soát ví qua logout, khóa đăng nhập đến khi lưu đầy đủ thành công; không cấp lại khi gửi packet thất bại. Registry đối soát nằm trong bộ nhớ tiến trình; khôi phục sau sự cố JVM tắt đột ngột vẫn cần cơ chế giao quà bền vững riêng.

Chín vật phẩm có đủ 36 PNG x1–x4; hai client Game1/Game2 dùng fallback tải icon từ server. Tất cả là vật phẩm hành trang `head/body/leg=-1`, không cần thêm part thân người. SQL migration đã đồng bộ vào hai dump đầy đủ và hai seed được theo dõi trong Git.

Biên dịch toàn bộ src, kiểm thử trên candidate và JAR đã cài. Chứng cứ cuối: `.analysis/vip-rewards-verification.json`; metadata triển khai và hash backup: `.analysis/vip-rewards-deployment-latest.json`, `SRC/jar-backup/vip-rewards-*.json`. Kiểm tra UI trực tiếp trong Unity chưa thực hiện.
