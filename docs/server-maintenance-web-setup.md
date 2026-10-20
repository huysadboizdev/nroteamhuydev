# Cài đặt điều khiển bảo trì trên web

Mã nguồn đã có nút **Máy chủ / Bảo trì** trong trang quản trị, nhưng web và server chỉ hoạt động khi cùng truy cập một MySQL database và server đang chạy phiên bản source có coordinator bảo trì.

## Cài schema

Trước khi khởi động server đã cập nhật, áp dụng file `SRC/sql/20261020_server_maintenance.sql` vào database mà cả server game lẫn `webgoc2026` đang dùng. Có thể chạy bằng phpMyAdmin hoặc MySQL CLI, ví dụ:

```powershell
mysql -h <host> -u <user> -p <database> < SRC/sql/20261020_server_maintenance.sql
```

Migration chỉ tạo bảng trạng thái và chèn trạng thái `OPEN` nếu chưa có; không tự chạy từ web hay lúc khởi động Java. Giữ bảng `server_maintenance_state` qua các lần restart để server tiếp tục ở trạng thái bảo trì nếu trước đó đang `MAINTENANCE` hoặc `FAILED`.

## Cấu hình pool kết nối

`SRC/Config.properties` và `SRC/Config.example.properties` hiện đặt pool Hikari tối thiểu 2, tối đa 5 kết nối. Kiểm tra `max_connections` của MySQL VPS còn đủ chỗ cho server và các dịch vụ khác; không chép thông tin đăng nhập trong `Config.properties` vào web hoặc kho mã nguồn. Pool lớn hơn không thay thế việc sửa query chậm hay deadlock.

## Sử dụng

Trong webgoc2026 mở **Máy chủ / Bảo trì**. Khi heartbeat mới, **Bảo trì ngay** chặn đăng nhập, chờ thao tác đang chạy, lưu và ngắt toàn bộ phiên; trạng thái chỉ hoàn tất sau khi server ghi nhận xong. Nếu heartbeat cũ nhưng database còn truy cập được, nút vẫn cho xếp hàng yêu cầu khi trạng thái gần nhất là `OPEN` hoặc `FAILED`; lệnh chưa được server xử lý nên chưa xác nhận việc đá người chơi. Nếu JVM đã treo hoặc SQL không truy cập được, web không thể cưỡng ép ngắt phiên. Nếu lưu thất bại, đăng nhập tiếp tục bị chặn và trang hiển thị `FAILED`. Sau khi xử lý nguyên nhân, gửi lại bảo trì; chỉ dùng **Mở lại server** khi heartbeat mới và trạng thái là `MAINTENANCE`.

Nút này cần JVM và MySQL còn phản hồi. Nó không thể cứu JVM đã deadlock hoặc VPS mất mạng. Chưa kiểm thử luồng trên VPS, chưa áp dụng migration lên database thật, chưa build JAR và chưa triển khai.
