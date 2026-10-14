# Làng Mori: Daishinkan và giao dịch người chơi

## Vị trí NPC

- Database `map_template`, map **7 (Làng Mori)**, NPC **111** đứng tại `(454,432)`.
- Cửa vào nhà Moori có vùng X `384..456`, Y `408..432`: NPC nằm trong vùng cửa.
- Đã chuyển riêng NPC này sang **`(850,432)`**, giữ nguyên các NPC và waypoint khác. Đã đọc lại database xác nhận; map 0 và 14 không đổi.
- Backup trước khi thay đổi: `.analysis/npc-placement-backups/mori-daishinkan-2026-10-14T09-28-07.748421700.json` chứa nguyên giá trị `before_npcs` và `after_npcs`.
- Server khởi tạo danh sách NPC từ database khi khởi động, nên phiên server đang chạy chưa tự nhận vị trí mới.

## Giao dịch bị hủy khi chọn đồ

Người dùng xác nhận mời giao dịch được, nhưng chọn item/đồ thì giao dịch bị hủy.

Trong `Panel.perform`, action `7000` tạo `item4` cho món đồ chỉ có một cái nhưng không gán quantity. C# mặc định quantity bằng 0; client gửi gói `-86`, action `2`, số lượng 0. `TransactionService.controller` phía Java hủy giao dịch khi nhận số lượng này. Lỗi có ở Game1/Game2 của client PRJ và bản web.

- Sửa bốn bản `Panel.cs` để món đồ được chọn có `quantity = 1` trước khi đưa vào bảng và gửi gói tin.
- Server hỗ trợ client cũ gửi 0 **chỉ khi slot hợp lệ còn đúng một món**. Số âm, slot không hợp lệ và số lượng 0 cho stack nhiều món vẫn bị từ chối. Kiểm tra dùng bản sao túi đồ, không sửa dữ liệu người chơi thật.
- `.analysis/trade_client_quantity_regression.py` thực thi đoạn action `7000` lấy từ source: trước sửa thất bại với quantity 0; sau sửa cả hai tab/native và web gửi quantity 1, kể cả slot 150.
- Sửa thêm server bỏ qua action `4` (gỡ đồ): theo dõi chính xác slot gốc của từng món/chồng đồ trong bảng giao dịch, khôi phục bản sao vào đúng slot; giữ nguyên option khi có đồ cùng loại. Bảng đã khóa không được thêm/gỡ đồ.
- Java regression `SRC/test/nro/models/services_func/TradeProtocolTest.java`: **8 kịch bản đạt**: single, zero-stack, negative, invalid-zero, remove, remove-stack, remove-duplicates, locked-remove. Root đã đọc diff và chạy lại cả tám kịch bản độc lập.
- Kiểm tra C# web: 409 source runtime và 410 source editor, 0 lỗi. Client gốc: 402 source runtime, 0 lỗi. Trình kiểm tra chung chưa có reference Android editor nên phần editor client gốc báo thiếu `AndroidPlatformIconKind` trong `ClientBranding.cs`; không thuộc thay đổi giao dịch.

## Trạng thái triển khai

Ban đầu chỉ sửa source và database theo quy tắc không tự build. Sau đó người dùng yêu cầu rõ ràng: **“build thay thế luôn nhé”**.

- Đã build và thay `SRC/20.jar`, chỉ cập nhật các class `Trade` và `TransactionService`, giữ các thành phần còn lại từ JAR đang dùng. Cả 9 ca giao dịch đạt trên candidate và sau khi cài JAR.
- Server cũ PID 13172 được dừng bằng bảo trì của ứng dụng có lưu dữ liệu; giữ tạm script tự restart trong thời gian thay bản. Snapshot database và backup JAR đã đối chiếu SHA-256; script restart được khôi phục.
- Backup JAR: `SRC/jar-backup/20-before-player-trade-20261014-093424.jar`. JAR mới SHA-256 `a02a382fb077e162bb1be060cf2fbb127f353fca6a9aab4c841ec2da1f9d845a`. Metadata: `.analysis/player-trade-deployment-latest.json`; snapshot database trong `.analysis/player-trade-deploy-20261014-093424/`.
- Server mới PID 14492 đã mở cổng 14445. Probe chỉ đọc trong JVM xác nhận map 7/NPC 111 tại `(850,432)`; bằng chứng `.analysis/player-trade-runtime/npc-position.json`.
- WebGL build thành công, 0 lỗi/70 cảnh báo legacy. Đã backup bản web cũ, cài bản `WebGL-prj-tradefix-20261014` và đối chiếu hash. Metadata `.analysis/prj-webgl-deployed.json`.
- Browser fixture trên bản web mới đạt: PC nhấn chuột/lưu tài khoản hai tab; mobile Chromium emulation chạm/HTML keyboard/lưu tài khoản hai tab, không lỗi/dialog. HTTP 200 xác nhận gateway phục vụ đúng build mới.
- Đã mở `http://localhost:8086/?v=WebGL-prj-tradefix-20261014` cho người dùng test. Chưa thực hiện giao dịch thật giữa hai tài khoản; kiểm thử dùng fixture cô lập, không thay inventory người chơi thật.
- Source native PRJ đã sửa, nhưng chưa đóng gói APK/EXE/iOS trong lần triển khai server và web này. Client cũ vẫn được server hỗ trợ cho lỗi số lượng 0 của vật phẩm đơn.
