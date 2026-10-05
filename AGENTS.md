# Quy tắc workspace

## Cập nhật JAR

- Theo yêu cầu người dùng: mỗi lần cập nhật JAR ứng dụng mới, chuyển JAR cũ vào `SRC/jar-backup` trước khi cài bản mới. Đặt tên riêng có thời gian, không ghi đè các backup đã có.
- `SRC/20.jar` là JAR ứng dụng đang dùng. Gom các JAR ứng dụng dư, bản dựng cũ và bản staging không dùng trong `SRC`/`.analysis` vào `SRC/jar-backup`; không di chuyển thư viện trong `SRC/lib` hoặc JAR của công cụ còn cần dùng.
- Kiểm tra server đã dừng trước khi thay JAR; dùng bảo trì có lưu dữ liệu khi cần dừng server. Đối chiếu SHA-256 của bản backup và bản mới; ghi metadata để xác định phiên bản và khôi phục.
- Sau cập nhật, chỉ để JAR ứng dụng đang dùng ở thư mục chạy. Lưu quy tắc này cho mọi lần cập nhật sau.
