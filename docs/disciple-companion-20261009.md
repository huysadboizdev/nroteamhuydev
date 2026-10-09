# Ô pet đi theo của đệ tử — 09/10/2026

Đã sửa mã nguồn để Uub, Beerus nhí và Jiren nhí dùng pet đi theo tại ô thứ 8 (`itemsBody[7]`). Chỉ chấp nhận vật phẩm có model trong danh mục 69 pet hiện có; không cho vật phẩm tiêu hao cùng `type 27` vào ô pet. Thú cưỡi vẫn giữ điều kiện hiện hành.

Pet đi theo thuộc đệ tử, độc lập với pet của sư phụ. Mặc/đổi/tháo cập nhật chỉ số của đệ; đổi pet thu hồi actor cũ; túi đầy giữ nguyên đồ và actor. Pet được khôi phục từ trang bị khi đệ cập nhật sau đăng nhập. Khi đệ rời map, chết, về nhà hoặc hợp thể, actor đi theo được ẩn/đưa khỏi map và quay lại khi đệ hoạt động. Logout/đổi đệ dọn actor thông qua dispose hiện có.

Điều kiện VIP đọc `typePet`, đồng bộ `type` khi tạo mới. Phụ kiện lưng đọc ô thứ 9 (`itemsBody[8]`) và chỉ loại `11`, tránh dùng vật phẩm pet ở ô thứ 8 làm hình đeo lưng. Sách vẫn dùng chung ô thứ 9 theo thiết kế cũ.

Kiểm chứng:

- Biên dịch toàn bộ Java với `--release 17`: đạt (cảnh báo annotation/unchecked có sẵn).
- `DiscipleCompanionTest`: đã tái hiện lỗi trước sửa; bản cuối đạt điều kiện VIP, vật phẩm không hỗ trợ, thiếu sức mạnh, transfer, cộng chỉ số, đổi pet, túi đầy, đăng nhập, đổi map, hợp thể/về nhà/chết, thoát map và dispose.
- `TigerPetAuditTest`: cả 3 cấu hình HP/KI/sức đánh của pet sư phụ đạt.
- Hai bộ kiểm tra chạy trực tiếp trên JAR ứng viên cuối, không chỉ trên thư mục class.
- JAR ứng viên chỉ đổi 7 class liên quan; các entry còn lại giữ nguyên so với `SRC/20.jar`.

JAR đã kiểm chứng: `SRC/jar-backup/20-disciple-companion-candidate-20261009-083402.jar`.

SHA-256 ứng viên: `d4b8cfb02fee162a4835a8e8663c703579a812f92bc5e94658399dea0a667bf8`.

SHA-256 JAR đang chạy, chưa thay: `abfbf7fd6bb1d3e02e73de7a97e79d23a00c3eb96b805e70b5976e52f7b7b7d9`.

Metadata và log nằm trong `.analysis/disciple-companion`. Bước bảo trì/thay JAR bị duyệt tự động từ chối vì chưa có chấp thuận rõ cho gián đoạn server. Chờ người dùng cho phép bảo trì có lưu dữ liệu và cập nhật; khi được phép, phải kiểm tra server dừng, đưa JAR cũ vào backup riêng có thời gian, đối chiếu SHA-256, cài JAR mới, ghi metadata và xác minh khởi động.

## Tri?n khai ho?n t?t

Ng??i d?ng ?? ??ng ? b?o tr?. Server ?? d?ng khi th?c hi?n thay JAR. Backup: `SRC/jar-backup/20-before-disciple-companion-20261009-083720.jar`. ?? c?i JAR c? SHA-256 n?u tr?n, kh?i ??ng PID 7344 v? x?c minh c?ng 14445. C? hai b? h?i quy ?? ch?y l?i tr?n `SRC/20.jar` v? ??t. Metadata ghi nh?n `installed_and_running`; b?n t? ch?i tri?n khai ? tr?n l? l?ch s? tr??c khi c? ph?p.
