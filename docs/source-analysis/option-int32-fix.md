# Option số lượng 32-bit — server và client

Đã sửa source server và hai tab Game1/Game2 trong `C:\Users\Admin\Downloads\PRJ_2Tab_550K`. Option param vẫn là int; khi hai bên hỗ trợ IOP32, truyền int32 thay unsigned16. Số100.000 hiển thị đúng100.000 thay34464; phạm vi kiểu dữ liệu vẫn tối đa2.147.483.647, không phải vô hạn.

## Protocol và tương thích

Client thêm `|IOP32` vào chuỗi platform. Server xác nhận bằng command-29/subcommand100/boolean trước gửi item. Client mặc định đọc unsignedshort, chuyển sang int khi nhận xác nhận và reset khi ngắt kết nối. Client mới kết nối server cũ vẫn dùng16-bit nếu không nhận xác nhận. Client cũ kết nối server mới vẫn giữ packet16-bit; giá trị vượt65535 được giới hạn riêng phần gửi cho client cũ để tránh wrap, dữ liệu thật không bị cắt. Client cũ chưa hiển thị được đầy đủ số lớn.

Đã thay22 lần ghi param phía server: đăng nhập, túi, rương, trang bị, pet, shop, ký gửi, giao dịch và radar. Codec giao dịch theo người nhận; sửa lựa chọn width quantity giao dịch theo người nhận tương ứng. Giữ nguyên các trường ID, số option, các short khác và quy tắc scale option213 hiện có.

Phía client:15 lần đọc option mỗi tab dùng codec, handshake, reset kết nối; tránh overflow khi nhân1000 cho option22/23. Không chuyển mảnh bông tai từ option31 sang quantity, không chỉnh giftcode/DB/số dư người chơi trong thay đổi này. Với mảnh đếm theo option31, quantity và param vẫn có ý nghĩa khác nhau như báo cáo trước.

## Kiểm tra

- Java source và JAR:76/76 test, archive kiểm tra đạt.
- Toàn bộ source C# hiện có biên dịch thành công bằng Roslyn. Dùng các reference Unity6 cài trên máy thay đường dẫn Unity2022 đã lưu trong project; bốn reference không tồn tại được bỏ qua vì source không cần chúng. Đây là kiểm tra biên dịch, chưa phải export game Unity2022.
- C# đọc packet tạo bằng codec Java cho cả Game1/Game2:0,1,32767,32768,65535,65536,100000,1000000,INT_MAX; kiểm tra chuỗi hiển thị, trường kế tiếp và packet kết thúc không lệch.
- Kiểm tra nhánh xác nhận hỗ trợ và reset thật của Session_ME trên cả hai tab.
- Probe không đăng nhập tới server đang chạy: phiên cũ không nhận ACK, phiên IOP32 nhận ACK và packet link cũ giữ nguyên.
- Sau bảo trì lưu dữ liệu và thay JAR: server PID15212 mở cổng14445, log khởi động thành công. Các cảnh báo map thiếu tile vẫn là thiếu hụt đã ghi nhận trước đó.

## Triển khai và backup

- JAR đang dùng: `SRC/20.jar`.
- Backup: `SRC/jar-backup/20-before-option-int32-20261003-082514.jar`.
- Hash cũ: `f6c28406070ddbfc2e25dd32a280ca7284664108780333f16d179db67af08417`.
- Hash mới: `ccb1492ae4ce244d9fbc4c7002bbc125d16ab324934758af4bf36087ee430c08`.
- Source client trước sửa: `.analysis/option-int32/client-before`; các hash ghi ở `client-changes.json`.
- Gói file client đã sửa: [client-option-int32-patch.zip](client-option-int32-patch.zip). Source tại PRJ_2Tab_550K đã được sửa trực tiếp; ZIP dùng để lưu/chuyển các file sửa, không phải bản game chạy được.
- Metadata: `.analysis/option-int32/deployment.json`; log build, C# compile và packet tests trong cùng thư mục.

**Cần build lại client bằng Unity2022.3.62f2 của project** để người chơi sử dụng protocol mới. Chưa export EXE/APK/IPA hoặc chạy giao diện Unity thật. Bản compiled DLL trong thư mục kiểm tra không được cài vào client hiện có.
