# Bảo trì sửa truyền template và lỗi client

Đã triển khai ngày09/10/2026 sau yêu cầu bảo trì. Server đã lưu dữ liệu, dừng đúng cơ chế bảo trì, sao lưu JAR/SQL rồi chạy lại PID19696 trên cổng14445.

## Kết quả

- Nguyên nhân được xác nhận qua packet thực từ server: nhóm template bắt đầu cố định tại750 vượt giới hạn65535 byte. Mô tả runtime khác SQL nên chỉ tính từ SQL chưa tái hiện đủ lỗi.
- `ItemData` giờ mã hóa template trước, chọn điểm chia theo byte và xác minh đủ hai gói trước khi gửi. Giữ hai nhóm cache NRitem1/NRitem2. Catalog vượt sức chứa hai gói được từ chối rõ ràng trước khi gửi cập nhật dở dang.
- Probe không đăng nhập, chỉ yêu cầu tài nguyên: nhận đủ2040 ID, phiên bản23. Điểm chia875; kích thước gói57122 và57111 byte. Các món anime/Akatsuki có đúng tên và icon.
- Game1/Game2 chặn ID ảnh âm/vượt giới hạn, quản lý slot cache ảnh, kiểm tra atlas và xử lý phản hồi icon chưa có slot chờ. Panel bỏ qua item không có template. Parser đăng nhập dừng báo lỗi khi template bị thiếu, không tiếp tục đọc ở vị trí lệch. Lỗi tải template được ghi log và phiên bản cache bị đánh dấu chưa hợp lệ.
- Toàn bộ Java và Assembly-CSharp Unity biên dịch đạt. Test tái hiện packet overflow và ID ảnh âm đã fail trước sửa, pass sau sửa; kiểm tra catalog nhỏ, giới hạn vượt hai gói, đủ từng ID, hồi quy anime và ShopDAO/SQL đạt.
- Ảnh đã cài trước đó giữ nguyên hash. SQL option/giá/path giữ nguyên. Các cảnh báo tile map cũ vẫn tồn tại riêng.

## Sao lưu và bằng chứng

JAR trước sửa: `SRC/jar-backup/20-before-res-transfer-fix-20261009-122725.jar`.

JAR hiện tại SHA256: `240e411da132dc579ff8477fbf7c84416171dd83d0788d26e67333bd20e3a14f`.

Metadata: `SRC/jar-backup/res-transfer-fix-deployment-20261009-122725.json`. Bản sao sáu source client trước sửa: `.analysis/res-transfer-fix/client-backup`. Hash và bằng chứng packet: `.analysis/res-transfer-fix/candidate.json`, `live-catalog-result.json`, `live-NRitem1.bin`, `live-NRitem2.bin`. Kết quả full compile: `.analysis/res-transfer-fix/client-compile.log`.

Để dùng source client mới trong Unity: dừng Play, chờ Unity biên dịch, chạy Play rồi đăng nhập lại. Cần kiểm tra hình ảnh trong game thật sau reconnect; kiểm thử hiện xác minh biên dịch, logic bảo vệ và packet tải tài nguyên, chưa xác nhận bằng ảnh chụp giao diện sau sửa.
