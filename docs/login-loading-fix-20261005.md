# Sửa treo “Xin chờ” khi đăng nhập — 05/10/2026

Nguyên nhân đã tái hiện: kiểm tra trạng thái Commeson trong `MrBlue.login` đọc `player.id` bằng `getLong("id")`, nhưng cột SQL là `INT`, JDBC trả `Integer`. `ResultSetImpl` ép trực tiếp sang `Long` gây `ClassCastException`. Server chỉ ghi log và trả `null`, không thông báo client, làm màn hình chờ kéo dài.

Đã sửa toàn bộ getter số dùng chuyển đổi `Number`, áp dụng cả tên cột và số thứ tự. SQL NULL trả 0 tương tự getter số JDBC. ID BIGINT vẫn giữ đúng giá trị. Đã bổ sung thông báo khi đăng nhập có exception, nhân vật không tải được hoặc đang chờ đối chiếu Commeson. Client hiện có xử lý thông báo `-26` để tắt `InfoDlg` và trạng thái tải map; không cần sửa client cho lỗi này.

Kiểm chứng:

- Bản JAR cũ tái hiện đúng `Integer cannot be cast to Long` bằng JDBC thật.
- JAR mới qua 62 nhóm kiểm thử, gồm kiểu INT/BIGINT/SMALLINT/TINYINT/DECIMAL/FLOAT/DOUBLE/NULL, đọc theo tên và thứ tự, đọc ID nhân vật thật.
- Kết nối TCP tới server đang chạy: tài khoản mới tới màn hình tạo nhân vật, tạo nhân vật, đăng nhập ID INT, nhận dữ liệu nhân vật và map 39; ngắt rồi đăng nhập lại nhân vật đã lưu cũng thành công.
- Làm hỏng dữ liệu của nhân vật thử để kiểm tra: server trả thông báo `-26`, không chờ vô hạn. Exception của nhân vật thử trong log là chủ động tạo để kiểm chứng nhánh lỗi.
- Tài khoản, nhân vật và dữ liệu phụ của thử nghiệm đã xóa. Không đăng nhập tài khoản của người chơi để thử.
- Runtime phó bản và các map cần thiết vẫn kiểm tra đạt.

Cập nhật `SRC/20.jar` sau bảo trì lưu dữ liệu, xác nhận server dừng và snapshot SQL. JAR cũ: `SRC/jar-backup/20-before-login-numeric-20261005-171643.jar`. Metadata và SHA-256: `.analysis/login-numeric-deployment-latest.json`. Không thay đổi cấu trúc SQL trong lần sửa này.

Đã xác minh giao thức đăng nhập và vào map; chưa thao tác trực tiếp màn hình Unity. Nếu Unity vẫn giữ phiên chờ cũ, tắt Play rồi bật lại để kết nối mới.
