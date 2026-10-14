# Thay client web bằng Scriptsweb

Người dùng xác nhận thay toàn bộ client web bằng `C:/Users/Admin/Downloads/Scriptsweb/Scripts`. Giữ source tham chiếu nguyên trạng, nhập bản sao vào project web; không dùng các lớp Game1/Game2 làm gameplay mới. Scriptsweb sử dụng lớp global và một Main, nên scene chạy một client theo cấu trúc này.

Tái sử dụng tài nguyên Unity hiện có vì Scriptsweb chỉ chứa source và một phần resource. Giao diện/menu/gameplay lấy từ Scriptsweb. Chỉ sửa lớp tích hợp cần để chạy browser: nhận diện thiết bị, WebSocket/gateway local, lưu dữ liệu và nhập liệu, các thread không được WebGL hỗ trợ. Không nhập thêm sửa gameplay của client khác khi chưa được yêu cầu.

Sao lưu Assets/source/editor/scene/settings/tests/tooling cũ có hash trước thay. Dựng source trong staging rồi kiểm tra C# bằng compiler/fixtures độc lập; không gọi Unity BuildPlayer, không tạo APK/EXE/JAR/WebGL release. Bản game đang phục vụ chỉ đổi sang source mới sau khi người dùng yêu cầu buildWebGL.
