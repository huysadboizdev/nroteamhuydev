# Client web lấy từ PRJ_2Tab_550K

Người dùng làm rõ rằng `Downloads/Scriptsweb/Scripts` chỉ là mẫu chuyển client sang web. Nền gameplay/UI phải là `Downloads/PRJ_2Tab_550K`, khớp server Java hiện tại. Lần cài toàn bộ gameplay Scriptsweb trước đó được thay thế theo yêu cầu này.

## Nguồn và phạm vi

Import đầy đủ 402 file C# runtime và tài nguyên từ PRJ, giữ hai namespace Game1/Game2, scene NROL/NROL1 và GUID Main gốc. Scriptsweb không được dùng làm source gameplay. Các lớp cầu nối WebGL nằm riêng trong NroWeb; project PRJ gốc không bị sửa.

Các nhánh browser xử lý WebSocket, nhận diện thiết bị, coroutine, resize, bàn phím HTML và lưu dữ liệu tách hai tài khoản. Giao diện và chức năng gameplay vẫn là PRJ. Chuyển tab xóa trạng thái input cũ; trường nhập và hộp thoại command có quyền nhận input riêng để tránh gửi nhầm nội dung hoặc nhận nhầm nút Xong.

## Khớp server Java

Đối chiếu mã Java trong `SRC/src/nro/models/network` và `services/Service.java`: handshake/XOR, thương lượng `-29/100`, IOP32, INV200, frame dài và snapshot tính năng packet. Giữ toàn bộ capability của PRJ: `IOP32|INV200|BOSS1|BOSS2`.

Controller, Message và ItemOptionWire giữ source PRJ. Bốn Session giữ nhánh native và bổ sung adapter browser. Service thêm kiểm tra sau gửi riêng WebGL để thao tác gửi lỗi không trừ KI, ghi nhận tọa độ hoặc báo đòn đánh thành công.

## Xác minh

- Compiler Roslyn với reference Unity 2022.3/.NET Standard 2.1: 409 source browser, 410 source editor, không lỗi.
- Fixture bốn adapter thật: handshake chia nhỏ/ghép, XOR, ACK tính năng, inventory frame 70 KB, session phụ, queue giới hạn, EOF và reconnect.
- JS bridge: gateway cùng origin, TLS/URL override, bốn socket độc lập.
- Reader/ItemOptionWire thật của cả hai game: uint16 65535, int32 1000000, pickup quantity mở rộng và count inventory 200.
- Fixture combat thật: sáu lỗi silent-disconnect được tái hiện trước sửa; sau sửa Game1/Game2 đều không lỗi.
- Fixture keyboard/save: tab không hoạt động không nhận Done, callback chỉ gọi một lần, hủy khi đổi tab, command cũ không nhận Done của trường nhập mới, save/clear/delete tách tài khoản.
- Sáu method combat/target/skill-layout vẫn khớp PRJ; rà soát 45 file C# thay đổi/thêm và hai JSlib không tìm thấy blocker nghiêm trọng.
- Sau cài, source project đích compile browser/editor không lỗi; 24 kiểm thử Node đạt. Đối chiếu SHA-256 xác nhận 402 file runtime tại project PRJ gốc không bị đổi. Kết quả cuối lưu ở `.analysis/prj-native-web-verification.json`.

Evidence: `.analysis/prj-native-web-tests`, `.analysis/prj-native-web-stage/Tools/validate_shell.py`, `.analysis/PrjWebTanSatHarness.cs` và `.analysis/prj-native-web-stage/native-source-manifest.json`.

## Chưa build

> Trạng thái này thuộc lúc chỉ sửa source. Sau đó người dùng yêu cầu cài/chạy; bản WebGL đã build và cài ngày 14/10/2026. Xem `prj-webgl-local-release-20261014.md`.

Không chạy Unity batchmode/BuildPlayer hay tạo APK/EXE/JAR/WebGL phát hành. DLL phục vụ kiểm tra source/fixture không phải bản game. Artifact đang phục vụ trong Builds/WebGL được giữ nguyên, đối chiếu SHA-256 khi cài source.

Vì chưa build, chưa xác minh runtime mới trong trình duyệt, đăng nhập hoặc gameplay hai tài khoản. Trang local vẫn tải artifact cũ. Sau khi người dùng yêu cầu build cần kiểm tra runtime trên PC/Android/iOS.

Metadata cài đặt và backup: `.analysis/prj-native-web-installed.json` và `.analysis/prj-native-web-correction-backup-*`. Source trước lần sửa này được giữ để khôi phục.
