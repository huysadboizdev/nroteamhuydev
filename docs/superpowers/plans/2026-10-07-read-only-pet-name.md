# Kế hoạch tên đệ tử mặc định trong admin

> **For agentic workers:** Use executing-plans to implement task-by-task. Chỉ thực hiện sau khi người dùng yêu cầu triển khai kế hoạch.

**Goal:** Tên trong Pet Studio tự hiển thị tên mặc định của loại đệ tử hiện tại; admin không nhập hoặc gửi tên tùy ý.

**Architecture:** Server là nguồn quy tắc tên. PHP hiển thị tên tương ứng loại đệ tử, API không nhận thay đổi tên và server tự gán tên chuẩn khi admin đổi loại. Luồng đổi đệ tử trong game giữ dùng tên mặc định của PetService.

**Tech Stack:** PHP, JavaScript, Java, MySQL JSON và hàng đợi LiveSync hiện có.

## Kết quả kiểm tra

- `admin/player_detail.php:381` đang là input text chỉnh được và có required. Hàm savePet tại dòng 995 luôn gửi pet_name.
- `admin/includes/player_data_helper.php:151` đọc tên lưu trong info[2], chỉ bỏ ký tự $, nên tên rỗng đã lưu vẫn hiện rỗng.
- `admin/includes/admin_command_validator.php:51` hiện cho phép pet_name.
- `SRC/src/nro/models/services/LiveSyncPlayerPatch.java:127` nhận tên tùy ý. Khi sửa pet_type của đệ có sẵn, tên cũ chưa tự đổi theo loại.
- `SRC/src/nro/models/services/PetService.java:218` đã gán tên mặc định khi tạo/đổi trong game: loại 0 Đệ tử, 1 Mabư, 2 Uub, 3 Kid Beer, 4 Kid Jiren.

## Thiết kế đề xuất

Dùng tên theo loại thay vì dùng nguyên chuỗi tên đã lưu hoặc chỉ khóa input. Cách này xử lý cả tên trống và tên cũ không khớp loại. Chỉ khóa input sẽ vẫn hiển thị sai và không bảo vệ API; chỉ sửa PHP sẽ chưa sửa tên khi đổi loại qua server.

Phạm vi yêu cầu là admin: không tự xóa chức năng đổi tên bằng vật phẩm trong game. Màn hình admin luôn hiển thị tên mặc định theo loại. Khi người chơi đổi loại trong game và trang admin được tải lại, tên và loại phải cùng khớp. Không thêm cơ chế tự refresh toàn trang liên tục.

### Task 1: Quy tắc tên và form chỉ đọc

**Files:** `C:/xampp/htdocs/webgoc2026/admin/includes/player_data_helper.php`, `C:/xampp/htdocs/webgoc2026/admin/player_detail.php`, `C:/xampp/htdocs/webgoc2026/tests/admin_pet_form_test.php`.

- [x] Thêm kiểm thử loại 0–4, tên rỗng và tên cũ không khớp. Mong đợi tên hiển thị lần lượt Đệ tử/Mabư/Uub/Kid Beer/Kid Jiren.
- [x] Thêm helper dùng bảng `[0=>'Đệ tử',1=>'Mabư',2=>'Uub',3=>'Kid Beer',4=>'Kid Jiren']`; loại không hợp lệ hiển thị `Không xác định` và không tự đổi dữ liệu SQL.
- [x] Thay ô tên bằng input readonly không required, giá trị lấy từ helper theo type. Xóa pet_name khỏi payload savePet.
- [x] Khi admin chọn loại khác trong form, cập nhật tên xem trước theo cùng bảng; dữ liệu chỉ có hiệu lực sau khi lệnh lưu completed.
- [x] Chạy `C:/xampp/php/php.exe -n tests/admin_pet_form_test.php` và `python tests/admin_inline_syntax.py`, yêu cầu PASS.

### Task 2: Chặn tên tùy ý và đồng bộ khi đổi loại

**Files:** `C:/xampp/htdocs/webgoc2026/admin/includes/admin_command_validator.php`, `SRC/src/nro/models/services/PetService.java`, `SRC/src/nro/models/services/LiveSyncPlayerPatch.java`.

- [x] Thêm kiểm thử API gửi pet_name phải bị từ chối, đồng thời lưu chỉ số không có pet_name vẫn thành công. API trả lỗi rõ `Không được chỉnh tên đệ tử trong admin` trước khi tạo lệnh.
- [x] Tách quy tắc tên hiện tại của PetService thành phương thức dùng chung `defaultPetName(int type)` trả chuỗi có tiền tố $; createNewPet dùng phương thức đó.
- [x] LiveSyncPlayerPatch từ chối pet_name ở đầu xử lý UPDATE_PET, bảo vệ cả lệnh gửi trực tiếp vào hàng đợi.
- [x] Sau khi áp dụng pet_type, nếu payload có pet_type thì gán info[2] bằng tên mặc định của loại mới. Nếu chỉ sửa chỉ số và không có pet_type thì giữ dữ liệu tên SQL hiện có. Đệ mới tạo cũng dùng helper.
- [x] Kiểm thử đổi giữa đủ năm loại, tên cũ/rỗng, trường hợp chỉ sửa chỉ số và không mất skill/body/fusion hoặc điểm đã có.

### Task 3: Kiểm tra và triển khai

**Tests:** `C:/xampp/htdocs/webgoc2026/tests/admin_player_command_test.php`, `SRC/test/nro/models/services/LiveSyncPlayerPatchTest.java`, `SRC/test/nro/models/services/LiveSyncQueueTest.java`.

- [x] Chạy các bài PHP bằng php -n, kiểm tra cú pháp PHP và JS.
- [x] Biên dịch và chạy kiểm thử Java bằng `.analysis/build_web_manager.py`, gồm kiểm thử tên mới và hồi quy hàng đợi một kết nối SQL.
- [x] Kiểm thử dữ liệu trên database cô lập; không đổi đệ tử thật của người chơi để thử.
- [x] Nếu cập nhật JAR: bảo trì lưu dữ liệu, xác nhận server dừng, sao lưu JAR với thời gian riêng, đối chiếu SHA-256, cập nhật và khởi động lại theo AGENTS.md.
- [x] Kiểm tra web hiển thị tên đúng, ô tên không sửa được, yêu cầu giả mạo bị chặn và lưu chỉ số vẫn hoạt động.

Không cần thêm cột SQL, part hoặc res client cho thay đổi này. Không chạy UPDATE toàn bộ tên đệ tử trong SQL; xử lý tên rỗng/cũ ở màn hình và khi đổi loại hợp lệ. Đã triển khai; xem docs/2026-10-07-pet-name-deployment.md.
