# Quy tắc workspace

## Cập nhật JAR

- Theo yêu cầu người dùng: mỗi lần cập nhật JAR ứng dụng mới, chuyển JAR cũ vào `SRC/jar-backup` trước khi cài bản mới. Đặt tên riêng có thời gian, không ghi đè các backup đã có.
- `SRC/20.jar` là JAR ứng dụng đang dùng. Gom các JAR ứng dụng dư, bản dựng cũ và bản staging không dùng trong `SRC`/`.analysis` vào `SRC/jar-backup`; không di chuyển thư viện trong `SRC/lib` hoặc JAR của công cụ còn cần dùng.
- Kiểm tra server đã dừng trước khi thay JAR; dùng bảo trì có lưu dữ liệu khi cần dừng server. Đối chiếu SHA-256 của bản backup và bản mới; ghi metadata để xác định phiên bản và khôi phục.
- Sau cập nhật, chỉ để JAR ứng dụng đang dùng ở thư mục chạy. Lưu quy tắc này cho mọi lần cập nhật sau.

## Sử dụng AI Skills

- Trong mọi yêu cầu và tác vụ từ người dùng, BẮT BUỘC chủ động rà soát danh sách skills có sẵn và áp dụng các kỹ năng (AI Skills) phù hợp nhất với ngữ cảnh công việc (ví dụ: `brainstorming`, `writing-plans`, `systematic-debugging`, `gitnexus`, `test-driven-development`, `verification-before-completion`, `9router`...).
- Tuân thủ đúng quy trình của từng skill trước và trong quá trình thực hiện công việc, không bỏ qua quy trình khi thực thi các tác vụ code, phân tích hoặc debug.

## Build theo yeu cau nguoi dung

- Tu 13/10/2026: khong tu build hoac dong goi ban APK, EXE, JAR khi nguoi dung chua yeu cau build ro rang. Chi kiem tra/phan tich/sua source trong pham vi duoc yeu cau; khong tu mo Unity batchmode de tao ban phat hanh. Quy tac nay uu tien hon quy trinh build/deploy cu.
