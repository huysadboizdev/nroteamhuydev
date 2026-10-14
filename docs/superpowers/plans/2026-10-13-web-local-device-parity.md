# Web Local Device Parity Implementation Plan

> Thực hiện trực tiếp trên project local người dùng đang test; sao lưu các file thay đổi và giữ các sửa có sẵn. Không commit, không build release.

**Goal:** Local 8086 kết nối đúng server và tự chọn điều khiển PC/Android/iOS; source gameplay WebGL đồng bộ sửa native.

**Architecture:** Giữ port WebGL; ghép diff tàn sát của native, ghép riêng appearance -90/setter hiệu ứng. Device classifier có sẵn là đầu vào cho cả hai Main.

**Tech Stack:** Unity C#, JavaScript, Node ws, Python, PowerShell.

- [x] Tạo test `Tests/local-config.test.cjs` đọc config runtime/template/gateway, yêu cầu target local trùng nhau; chạy `node --test ../Tests/local-config.test.cjs` trong Gateway và thấy fail trước sửa.
- [x] Sao lưu vào `.analysis/web-local-before-20261013` cùng SHA-256 trước sửa. Ghép tám file tàn sát bằng diff native-before/native-current, đối chiếu từng context; ghép appearance/Char cả hai tài khoản, giữ resource unsigned và các phần web.
- [x] Cấu hình `Gateway/config.json.targets=['127.0.0.1:14445']` và `Builds/WebGL/config.js` tương ứng template; chạy test config và 17 test sẵn có.
- [x] Dùng harness actual tàn sát trích từ source web, Add-Type trong bộ nhớ; chạy resource/virtual controls/tab/effect checks có sẵn.
- [x] Xác định PID đúng gateway8086, khởi động lại riêng gateway ẩn; thử WebSocket handshake tới local TCP. Kiểm thử browser dùng Unity build sẵn cho PC/Android/iOS và hai tab; không tự build game.
- [x] Review diff, ghi kết quả test và giới hạn wasm cũ trong báo cáo, cung cấp URL local.
