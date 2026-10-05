# Capsule hồng ngọc và vé tặng — đã triển khai

Server đang chạy bản mới tại `SRC/20.jar`, cổng14445. Đã bảo trì có lưu dữ liệu trước khi thay JAR.

- Capsule722 bỏ option97 (phản30% sát thương), kể cả capsule cũ trong dữ liệu và shop. Chỉ sửa option của722; các vật phẩm khác giữ nguyên.
- Mỗi capsule nhận hồng ngọc và chắc chắn1 vé tặng hồng ngọc2000. Tỷ lệ giữ80% nhận1–5,20% nhận6–100.
- Dùng vé nhập người nhận đang online và số hồng ngọc, rồi xác nhận. Mỗi lần thành công mất1 vé. Số nhập là tổng bị trừ:100 trừ100, nhận90. Phí10% làm tròn lên:11 trừ11, nhận9. Số1 bị từ chối.
- Vé có thể xếp chồng, không giao dịch/ký gửi. Catalog15 để client cập nhật dữ liệu khi đăng nhập lại.
- Chuyển ruby, tiêu hao vé và biên nhận dùng cùng transaction InnoDB. Đối soát commit không rõ kết quả; chống gửi lặp, gửi đồng thời, ghi đè bởi lưu/thoát/đăng nhập lại. Snapshot các ngăn vật phẩm ngăn phục hồi vé sau crash.

## Kiểm chứng

67/67 nhóm kiểm thử đạt trên candidate và JAR đã cài: phân phối thưởng, túi đầy, tràn số, vé, option, form xác nhận/hủy, giao dịch đồng thời, khóa trade/giftcode, rollback từng lần ghi, mất kết nối sau commit, lưu/thoát/đăng nhập và hồi phục. Các bài DB dùng schema riêng.

Runtime probe trên server đang chạy xác nhận mô tả capsule, ticket2000/type27/icon6765/stack/option30, catalog15, shop722 không còn97, phí và phân phối. Kiểm tra DB sau khởi động xác nhận chỉ1 trường inventory có capsule cũ được dọn97; số lượng, số dư và dữ liệu khác được bảo toàn. Server lắng nghe14445.

Chưa thao tác bằng client trò chơi để kiểm tra giao diện và trải nghiệm trực tiếp. Log báo một số map thiếu tile (78,95,101,121,125,130,134); đây là cảnh báo lúc nạp map, ngoài thay đổi này.

## Sao lưu và khôi phục

JAR cũ: `SRC/jar-backup/20-before-ruby-gift-20261004-134343.jar`.

SHA-256 cũ: `ad73e48fae9d02360c1dac0242d71e7932081b336d372783cc2bb8c8de6d7813`.

SHA-256 mới: `3115c50ac2d663dbb66e0b76f8b6db8481557a4b76133c6d3dbdd48d77508b94`.

Metadata: `.analysis/ruby-gift/deployment.json` và JSON cạnh JAR backup. Snapshot DB trước bảo trì: `.analysis/ruby-gift/database-maintenance-before.json`; source/dump cũ có manifest `.analysis/ruby-gift/source-sql-backup.json`. JAR baseline/staging đã chuyển vào `SRC/jar-backup`, chỉ còn JAR ứng dụng đang chạy ngoài backup. Thư viện và JAR công cụ được giữ.

Migration: `SRC/sql/migrations/20261004_ruby_capsule_gift.sql`; hai SQL dump đã cập nhật. Khi rollback phải bảo trì lưu dữ liệu, backup JAR hiện tại, rồi đối chiếu SHA của bản thay. Giữ biên nhận, số dư và vé đã phát sinh; không phục hồi snapshot tài sản cũ đè lên dữ liệu mới. JAR cũ chưa hiểu ticket2000, nên cần quyết định cách xử lý ticket trước khi phục hồi phiên bản cũ.
