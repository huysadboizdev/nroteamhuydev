# Thay toàn bộ source client web bằng Scriptsweb

> Đã được thay thế theo giải thích mới của người dùng: Scriptsweb chỉ là mẫu. Nền client phải lấy từ PRJ_2Tab_550K. Xem `prj-native-web-correction-20261014.md`; báo cáo dưới đây chỉ ghi lại lần cài trước đó.

## Phạm vi

Người dùng chọn thay toàn bộ client bằng `Downloads/Scriptsweb/Scripts`. Project đích là `ClientWeb/PRJ_2Tab_Web`; source server và project PC/APK không thuộc lần thay này.

Import 207 file C# gameplay từ Scriptsweb, bỏ AssemblyInfo của project decompile. Toàn bộ Game1/Game2/Management của client web cũ được đưa ra khỏi Assets. Scene duy nhất `NROL.unity` trỏ đến Main gốc của Scriptsweb, GUID `9336ca924b669f14d936b9d3fccdf023`.

## Tích hợp trình duyệt

- Nhận diện PC/Android/iOS, giữ giao diện và gameplay Scriptsweb; shell không có thanh tab bổ sung.
- Kết nối WebSocket qua gateway cùng origin, cấu hình local `127.0.0.1:14445`; xử lý handshake chia nhỏ, XOR, frame lớn và snapshot tính năng của từng packet.
- Controller đọc đúng option ID một byte, giá trị uint16/int32 và túi đồ 200 ô khi server thương lượng tính năng.
- Các tác vụ gameplay dùng thread chuyển sang coroutine riêng cho WebGL; nhánh native của tám file gameplay giữ nguyên sau chuẩn hóa khoảng trắng.
- RMS lưu riêng trong `nro-Scriptsweb`, đồng bộ IndexedDB. Danh sách server đọc trực tiếp từ config, bỏ cache endpoint cũ và yêu cầu HTTP sai.
- Resize giữ zoom/tài nguyên đã nạp, trạng thái gameplay và nội dung ô nhập. Bàn phím HTML hỗ trợ mobile; nút Xong gọi đúng callback chat hoặc xác nhận thường.
- Tài nguyên MenuChat, logo và 16 frame GIF có trong Assets. Không thay các ảnh này bằng ảnh tự tạo.

## Xác minh

- Kiểm tra C# bằng Roslyn với assembly/reference của Unity 2022.3.62f2, .NET Standard 2.1: 214 file browser và 215 file editor, không lỗi. DLL sinh ra chỉ dùng kiểm tra source trong `.analysis/scriptsweb-compiler-validation`; không phải bản phát hành.
- Fixture transport: handshake phân mảnh, XOR rollback, frame ghép/lớn, snapshot thương lượng, secondary session, queue/disconnect/reconnect và gửi lỗi.
- Fixture dùng method Controller thực: đọc option uint16/int32.
- Fixture routine thực: AutoBuyItem, CollectAllThuongDe, DoAutoNoitai; kiểm tra thứ tự lệnh, khoảng chờ và hoàn tất.
- 10 assertions cho parser server và lưu dữ liệu tách biệt.
- Fixture nút Xong tái hiện lỗi cũ trước khi kiểm tra bản sửa: callback chat nhận text một lần; callback thường và action null đúng.
- 21 kiểm thử Node đạt, kiểm tra shell, nhận diện thiết bị, config, giới hạn canvas, scene, tài nguyên và gateway TCP.
- Kiểm tra lại sau cài: compiler browser/editor không lỗi, fixture transport chạy trực tiếp trên source project đích. SHA-256 của 207 file tham chiếu Scriptsweb vẫn khớp manifest gốc.

## Trạng thái source và bản đang chạy

Quy tắc chưa yêu cầu build tiếp tục được áp dụng. Không chạy Unity batchmode, BuildPlayer hay đóng gói bản phát hành. Các file trong `Builds/WebGL` giữ nguyên và được đối chiếu SHA-256 khi cài source.

Vì chưa build WebGL, trình duyệt local vẫn tải artifact client cũ. Kiểm tra source và fixture không chứng minh toàn bộ gameplay/ảnh của Scriptsweb đã chạy trong trình duyệt. Sau khi người dùng yêu cầu build cần kiểm tra runtime mới trên PC/Android/iOS, đặc biệt đăng nhập, chat, điều khiển, menu và tải resource.

Backup và metadata nằm ở `.analysis/scriptsweb-replacement-backup-*`; bản ghi cài đặt là `.analysis/scriptsweb-replacement-installed.json`. Metadata chứa SHA-256 của Assets cũ/mới và artifact WebGL giữ nguyên để khôi phục.

Backup lần này: `.analysis/scriptsweb-replacement-backup-20261014-081552`. Bản ghi xác minh cuối: `.analysis/scriptsweb-replacement-verification.json`.
