# Bảng thông báo boss đã triển khai

Vào **Chức năng → Thông báo → Boss hiện tại / Lịch sử boss**, chọn một boss để xem chi tiết.

- Boss bản đồ chung: tên, trạng thái, bản đồ, giờ xuất hiện, người hạ, giờ hạ, số phút đã trôi qua và dự kiến hồi sinh. Không gửi/hiện khu hoặc tọa độ.
- Timer hồi sinh lấy từ trạng thái nghỉ thực tế. Boss có điều kiện/lịch riêng hiện chờ điều kiện/lịch, không đưa giờ chưa chắc chắn.
- Lịch sử 7 ngày bắt đầu từ lần triển khai; giữ sau restart. SQL lỗi được giữ qua outbox rồi replay theo version, không ghi đè sự kiện mới bằng bản cũ.
- Khi server restart, bản ghi sống của phiên trước được đánh dấu phiên trước, bỏ ETA cũ.
- Thông báo hệ thống cũ giữ nguyên. Client cũ không nhận packet mới. Cần bản client mới để xem bảng boss.
- Cả Game1 và Game2 đều được cập nhật; bản PC tiếp tục hỗ trợ paste Ctrl+V/Shift+Insert.

Kiểm chứng: 50 nhóm server PASS, gồm hồi quy ký gửi/shop/thành viên/nội tại và tracker/wire/SQL-outbox/policy boss. Unity build thành công, 0 lỗi; model thời gian, packet thực tế, phản hồi cũ/chuyển trang/reset của cả hai client đều PASS. Chưa xác nhận bằng thao tác trực tiếp trên tài khoản trong game; đã xác nhận server khởi động, port 14445 và các sự kiện xuất hiện thật ghi vào SQL.

JAR cũ đã được chuyển vào jar-backup sau bảo trì có lưu dữ liệu; có snapshot SQL và metadata SHA-256.

Client ZIP: `C:\Users\Admin\Downloads\PRJ_2Tab_550K\Builds\PC-Boss-20261005-140541.zip`
SHA-256 client: `cf9fba9e71ce084431fceb13d631d618839d773839e367563836113f718c4bcd`

JAR mới: `92d13274d81c97349160ca28f8795cfa705fc3b8f3c591faf38e203e4265e401`
Backup JAR: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-boss-board-20261005-140818.jar`
Metadata: `.analysis/boss-board-deployment-latest.json`

Cập nhật theo yêu cầu: bỏ tự tải lại 5 giây. Mở mục boss/đổi tab tải dữ liệu mới; đọc chi tiết và quay lại danh sách không phát request mới. Nút Làm mới và phân trang vẫn hoạt động; đồng hồ tuổi sự kiện dùng thời gian server.
