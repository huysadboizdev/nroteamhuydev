# Triển khai web local — 13/10/2026

## Có thể test ngay

- URL PC: `http://localhost:8086/`. Tải lại Ctrl+F5.
- Gateway mới PID5480, TCP server hiện có PID9032 ở cổng14445; không dừng hoặc thay JAR server.
- Gateway và config đang phục vụ cùng chọn `127.0.0.1:14445`, khớp template. Đã tái hiện lỗi config trước sửa, test sau sửa pass; gateway đang chạy đã trả packet bắt tay key thực từ server TCP.
- Cơ chế nhận diện thiết bị đã tồn tại được xác minh bằng actual Unity runtime trên Chromium: PC chọn `pc=True/mobile=False`; Android/iOS chọn `pc=False/mobile=True`, đúng ở cả Game1 và Game2. Bàn phím browser mobile cũng pass. Android/iOS là mô phỏng trình duyệt, chưa thay kiểm tra máy thật/Safari.
- Điện thoại cùng LAN dùng `http://<IPv4-LAN-của-PC>:8086/`; địa chỉ TCP127.0.0.1 được gateway giải quyết trên PC, không phải trên điện thoại.

## Source đã sửa, chưa xuất hiện trong game đang phục vụ

Đồng bộ tám file tàn sát và bốn file appearance/hào quang của cả hai tài khoản từ native bằng diff có chọn lọc. Giữ browser WebSocket, coroutine, lưu dữ liệu, điều khiển và kiểm tra resource unsigned của project web.

Review phát hiện core.Send của WebGL xử lý lỗi gửi bằng đóng phiên nhưng trả void. Đã bổ sung kiểm tra kết nối sau gửi để không commit tọa độ/KI/skill/cooldown khi lỗi gửi này xảy ra. Sáu test bổ sung fail trước sửa, pass sau sửa.

Wasm/data hiện vẫn là build11/10. Không build, đóng gói hoặc thay APK/EXE/JAR/WebGL. Vì vậy chưa thể test gameplay mới tàn sát/hào quang trong trình duyệt; cần yêu cầu build WebGL riêng để đưa C# mới vào artifact.

## Xác minh

- Node:18/18 tests pass, gồm config local, device classification, gateway, viewport và branding.
- ResourceDownloadChecks, VirtualControlsChecks, TabSwitchChecks pass cả hai tài khoản.
- Harness trích controller và Service/GameScr/ModFunc thật từ source web: Game1FAILURES=0, Game2FAILURES=0. Add-Type trong bộ nhớ, không tạo release game.
- Browser local acceptance: PC/Android/iOS, hai tài khoản, bàn phím mobile, không phát hiện lỗi JS/Unity thuộc tiêu chí kiểm tra.
- Review source hoàn tất; lỗi gửi thầm lặng đã sửa và review lại, không finding mới trong phạm vi thay đổi.
- Chưa xác nhận đăng nhập/chiến đấu bằng tài khoản game thật; handshake và khởi tạo UI không thay thế kiểm thử gameplay.

## Bằng chứng và khôi phục

- Backup14file và hash trước sửa: `.analysis/web-local-before-20261013/metadata.json`.
- Diff lượt này: `.analysis/web-local-20261013.patch`.
- Hash cuối và kết quả browser: `.analysis/web-local-20261013-verification.json`.
- Browser: `ClientWeb/PRJ_2Tab_Web/Tests/evidence/local-device-20261013/results.json`, ảnh/log từng thiết bị.
- Khôi phục các file từ backup theo đúng relative path trong metadata; với config gateway phải khởi động lại riêng gateway. Không đụng server game hoặc các sửa khác có trước lượt này.
