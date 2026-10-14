# Bản PRJ WebGL đã cài local

Người dùng yêu cầu cài và chạy để test ngày 14/10/2026, cho phép build bản WebGL lần này.

- Source gameplay/UI: PRJ_2Tab_550K; Scriptsweb chỉ là mẫu.
- Unity 2022.3.62f2, build WebGL: Succeeded, 0 errors, 70 warnings.
- Bản đang phục vụ: ClientWeb/PRJ_2Tab_Web/Builds/WebGL.
- Backup cũ: Builds/WebGL-backup-20261014-083712. SHA-256 backup và bản mới được đối chiếu trước/sau cài; metadata `.analysis/prj-webgl-deployed.json`.
- Gateway chạy cổng 8086, nối tới Java local 127.0.0.1:14445.
- URL: http://localhost:8086/?v=prj-20261014.

Kiểm thử artifact mới qua HTTP: đúng tên file PRJ mới, handshake TCP nhận 7 byte; runtime PC/Android/iOS đều mở được và chuyển cả hai tài khoản; bàn phím browser mobile hoạt động; không có lỗi JavaScript/Unity exception trong các kiểm thử này. Tám lần nhấn nút đổi tab trong game, chuyển lúc giữ chuột và 12 yêu cầu đổi nhanh đạt.

Đã chọn server local và xác minh bằng ảnh menu gốc PRJ v2.4.8: Chơi mới, Đổi tài khoản, Máy chủ NRO, Xóa dữ liệu. Không đăng nhập tài khoản hay vào gameplay bằng thông tin người dùng; phần đó dành cho người dùng test.

Evidence: Tests/evidence/prj-release-20261014/{results.json,login-menu.png,login-menu.log,pc.png,android.png,ios.png} và Tests/evidence/tab-switch-results.json.

Lần chạy Unity đầu bị timeout IPC dịch vụ cấp phép trong sandbox; lần chạy được cấp quyền ngoài sandbox thành công. Không sửa server/JAR hoặc tạo APK/EXE/iOS trong lần này.
