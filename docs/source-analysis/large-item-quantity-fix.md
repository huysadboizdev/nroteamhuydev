# Item số lượng lớn — 03/10/2026

Đã rà và sửa server/client để hỗ trợ số lượng tới99.999.999 theo yêu cầu. Quantity và option param vốn đã là int32, đủ phạm vi; không đổi toàn bộ int sang long vì sẽ làm khác protocol/schema. Cộng dồn dùng phép tính long để kiểm tra trước khi lưu int.

## Thay đổi

- Bỏ trần99999 và điều kiện chặn gộp100 triệu rời rạc trong InventoryService. Các item được gộp dùng chung giới hạn int dương (2.147.483.647 mỗi ô), giữ so khớp option và các ngoại lệ ID hiện có.
- Kiểm tra đủ dung lượng trước khi sửa ô: thiếu chỗ thì không nhận một phần. Khi tổng vượt int mỗi ô, điền đầy rồi chuyển phần dư sang ô trống; không wrap sang âm.
- Cộng option1/31 dùng long trung gian; vượt phạm vi int thì trả thất bại trước khi đổi param. Item cùng ID nhưng thiếu option cần cộng không còn bị báo nhận thành công rồi bỏ phần thưởng.
- Sửa hai nhóm option viết tay bị sót ở lần nâng IOP32: số lượng option31 shop phần thưởng và option73/88 bảng cờ. Dùng codec chung theo người nhận; test packet thật cho legacy và int32.
- Nhặt item: giữ số lượng ban đầu trước khi inventory tiêu biến item đầu vào; bổ sung count32 ở cuối packet cho client IOP32, giữ prefix cũ. Sửa trường UTF bị thiếu ở thông báo item thường. Client mới đọc đủ số lượng, không cộng tiền dự đoán lần nữa khi server đã gửi số dư chính thức.
- Client Game1/Game2: lượng lớn viết gọn trong ô nhỏ; tooltip hiển thị quantity chính xác có dấu phân cách hàng nghìn. Option số lượng vẫn hiển thị giá trị đầy đủ qua int32.

Quantity khác option31: mảnh933/934 vẫn dùng option31 làm nguyên liệu nâng cấp, không tự đổi quantity sang param. Ví dụ cấu hình giftcode cho99.999.999 mảnh:

```json
{"id":934,"quantity":1,"options":[{"id":31,"param":99999999}]}
```

Không đổi quy tắc trao giftcode, yêu cầu ô trống của GiftCodeManager, schema DB hoặc các giới hạn tiền/tính năng riêng. Mua nhiều món trong một lần ở shop là luồng riêng: controller hiện không dùng quantity mua thêm; lần này không biến các shop thành mua hàng loạt. Các trường byte dành cho client phiên bản cũ vẫn được giữ để tương thích.

## Kiểm chứng và triển khai

- Test cũ tái hiện lỗi gộp100.000 khi không có ô trống và packet option viết tay.
- Java/source/JAR:78/78 test đạt, gồm stack, param, hai packet thực, overflow, thiếu chỗ và nhặt item.
- Source C# đầy đủ biên dịch đạt; test C# cả hai tab đọc packet Java tới99.999.999, chuỗi option, nhãn ô nhỏ, pickup, handshake/reset và trường tiếp theo không lệch.
- Bảo trì bằng bt, đợi tiến trình thoát rồi thay JAR. Backup: `SRC/jar-backup/20-before-large-item-quantity-20261003-094639.jar`.
- Hash backup: `ccb1492ae4ce244d9fbc4c7002bbc125d16ab324934758af4bf36087ee430c08`.
- Hash mới: `661c3d27f994cb33c74b5a7b5eaeb522c6b001e9232a2e1be0181b0725442bee`.
- Server mới PID3112 nghe cổng14445, log khởi động thành công; probe legacy/IOP32 đạt.

Metadata/log: `.analysis/large-item-quantity/`. Source client đã sửa trực tiếp tại PRJ_2Tab_550K; backup bổ sung ở `client-before` và `client-before-pickup`. [Gói file client đã sửa](client-option-int32-patch.zip) đã cập nhật gồm cả lần nâng option32 trước đó và thay đổi lần này.

Chưa export EXE/APK/IPA hoặc kiểm tra UI Unity thật. Cần build lại client bằng Unity2022.3.62f2 của project; kiểm tra C# sử dụng các reference Unity6 đang cài như đã ghi trong báo cáo option-int32 trước. Chưa xác minh nhận giftcode và lưu/đăng nhập lại bằng client thật; không dùng tài khoản người chơi thật để thử.
