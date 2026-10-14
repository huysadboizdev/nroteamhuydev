# Client gốc trên trình duyệt — cập nhật local13/10

Người dùng chọn cách2: giữ giao diện client gốc khi chơi browser. Đã loại bỏ thanh Tài khoản1/Tài khoản2/Toàn màn hình/nhãn PC/iOS/Android khỏi cả HTML đang phục vụ và template. Nút đổi tab gốc Unity còn nguyên; browser bridge chỉ lưu trạng thái và đóng bàn phím khi đổi tài khoản.

Canvas chiếm toàn bộ vùng hiển thị browser theo viewport, giữ tỷ lệ bằng cùng một scale cho width/height; backing buffer vẫn giới hạn1024x600 cho PC hoặc1024x768 mobile. Safe area, WebSocket, tải game và keyboard bridge cần thiết cho điện thoại được giữ. Mobile dọc yêu cầu xoay ngang; thay đổi hướng không tạo lại phiên Unity.

Local: `http://localhost:8086/`, Ctrl+F5 để tải HTML mới. Khi dùng DevTools, chọn thiết bị trước khi tải lại trang để client khởi tạo đúng chế độ.

Kiểm chứng:

-7 tests mới/đổi fail trước sửa; sau sửa Node22/22pass.
-Layout15viewport PC/touch PC/Android/iPhone/iPad pass.
-Orientation4mode pass, giữ tab/input khi xoay.
-Actual Unity hiện có: PC/Android/iOS Chromium giả lập, cả hai tài khoản, không lỗi thuộc tiêu chí JS/Unity kiểm tra.
-Nút đổi tab trong game:8 lần nhấn thực bằng chuột pass; chuyển khi đang giữ chuột và12 yêu cầu nhanh cũng pass.
-KeyboardAndroid/iOS: fullscreen test, Unicode, password/numeric, mở lại bằng touch, submit/close pass.
-Review hai HTML không finding mới. Diff whitespace check không lỗi.

Không build hoặc thay artifact Unity. Hash data/wasm/framework/loader khớp các hash trước lượt sửa shell. Các sửa C# tàn sát/hào quang trước đó vẫn chỉ có trong source, cần buildWebGL khi người dùng yêu cầu để xuất hiện trong game. Chưa xác nhận đăng nhập/chiến đấu với tài khoản thật hoặc Safari/điện thoại thật; không tuyên bố toàn bộ hành vi giống mọi bản phát hành.

Backup chính xác: `.analysis/original-client-shell-before-20261013/metadata.json`. Diff: `.analysis/original-client-shell-20261013.patch`. Verification: `.analysis/original-client-shell-20261013-verification.json`. Ảnh local mới: `ClientWeb/PRJ_2Tab_Web/Tests/evidence/local-device-20261013/pc.png`, `android.png`, `ios.png`.
