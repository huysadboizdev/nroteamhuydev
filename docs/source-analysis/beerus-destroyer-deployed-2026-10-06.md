# Beerus Hủy Diệt: triển khai ngày 06/10/2026

Chỉ mở shop khi mặc đủ 5 món Thần đúng ô/hệ, còn hiệu lực và có 99 thức ăn cùng loại trong hành trang. Menu map48/index100 đã được nối đúng và kiểm tra lại khi bấm OK.

Mỗi lần mua kiểm tra đủ bộ dưới khóa player, tiêu hao đúng món Thần đang mặc tương ứng, 99 thức ăn trong túi và 100 hồng ngọc. Đồ nhận vào túi; không tự mặc. Sau mua còn 4 món Thần, mọi lần mua tiếp bị chặn với thông báo “Thiếu 1 món Thần trên người, không được mua.” và không trừ thêm gì. Mặc bổ sung đủ bộ mới được mua tiếp. Kiểm tra bảo vệ tài khoản và giao dịch đang chờ được giữ.

Đồ Hủy Diệt giữ chỉ số nền và được thêm đúng một dòng HP/KI/sức đánh 1–5%, chọn đều loại dòng rồi chọn đều phần trăm. Không thêm option ấn 34/35/36, không tăng nền 0–15% và không tự gắn option khóa giao dịch 30/87. Không sửa hồi tố đồ cũ.

Snapshot giữ nguyên trang bị, túi, metadata và ví; lỗi cấp đồ hoàn tác cả ba. Kiểm tra sức chứa sau trừ thức ăn cho phép sử dụng ô túi vừa giải phóng, ô body trống không tính là ô túi.

SQL migration scoped BILL/NPC55, giữ 15 dòng/3 tab/13 loại và options nền. Shop type3, giá100/typeSell3/icon7743 đồng bộ DB, cache runtime và 4 dump/seed. Giá shop Thiên Sứ vẫn10hồng ngọc.

Đã bảo trì có lưu dữ liệu, kiểm tra JVM dừng, snapshot SQL và backup JAR cũ trước khi cài bản mới. Không thực hiện giao dịch thử trên tài khoản người chơi thật. Cache server và SHA-256 class kiểm tra trực tiếp bằng agent chỉ đọc; UI Unity chưa được quan sát trực tiếp.

Kiểm thử: 59 nhóm chạy trên classes, candidate và JAR đã cài đều PASS; gồm giao dịch Beerus, menu, bonus, hồi quy Thiên Sứ/Santa/ruby và chỉ số trang bị. Red logs nằm trong `.analysis/beerus-*`.

Server PID: 18000, cổng14445. JAR SHA-256: `925801550af3a8981521004832066e1ffbe514df39c8a16cb924fd5c9657eac3`. Backup: `SRC/jar-backup/20-before-beerus-20261006-091029.jar`.

Metadata `.analysis/beerus-deployment-latest.json` và file cạnh backup. Rollback: bảo trì lưu dữ liệu, phục hồi JAR backup và rollback-shop.sql trong thư mục deployment; không restore toàn bộ player snapshot.
