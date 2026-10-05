# Rà soát và sửa boss — 05/10/2026

Đã biên dịch toàn bộ SRC, rà soát 162 file Java trong thư mục boss và đối chiếu 128 cấu hình BossData với SQL đang dùng: 169 map, 25 skill và 2.001 item. Số file bao gồm lớp cơ sở, cấu hình và manager, không phải 162 loại boss khác nhau.

## Các lỗi đã sửa

| Nhóm | Lỗi và xử lý |
|---|---|
| Tiểu đội sát thủ thường/Namek | Callback đồng đội kích hoạt lại boss cha HP 0. Dùng khóa chung với đòn kết liễu; chặn ACTIVE/AFK/CHAT_S khi chết. |
| Luồng chết chung | Lỗi thưởng/thông báo chặn đổi trạng thái. Phục hồi pha chết, không chạy lại thưởng để tránh nhân đôi drop. |
| Hiệu ứng/chat cuối | Bom, kỹ năng đặc biệt hoặc thiếu người nói làm boss đứng lại. Xử lý chết trước cổng hiệu ứng; giới hạn chat cuối 10 giây, xử lý dữ liệu thiếu và lỗi callback. |
| Boss xuất hiện cùng nhau | Slot null hoặc callback sau khi đổi dạng gây lỗi mảng. Snapshot nhóm, giữ vị trí người nói, kiểm tra giới hạn và bỏ slot null. |
| Manager boss | Một boss lỗi bỏ qua boss sau; xóa khỏi manager khi chưa rời map tạo boss ma. Bắt lỗi từng boss, giữ quản lý để dọn/retry, chặn thời gian nghỉ âm. |
| Majin Buu 12h/luyện tập | Phân biệt AFK chờ hồi sinh sau chết với AFK chiến đấu; đặt timer trước tác vụ có thể lỗi. |
| Whis | So sát thương với HP sau trừ làm chết sớm. Chỉ chết ở HP 0, chặn đánh lại sau chết, bảo vệ phép chia cấp. |
| Luyện tập/Thượng Đế | Xóa zone trước tra NPC; lỗi thông báo chặn dọn. Tra bằng zone cũ, luôn thoát map, REST, gỡ manager và dispose. |
| Đại hội võ thuật/Hạt Mít/Siêu hạng/clone Thiên Xin Hăng | Update riêng thiếu xử lý chết. Nối pha chết chung trước hiệu ứng. |
| Kết quả đấu giải | Dọn boss trước match tick làm mất tín hiệu thắng. Giữ kết quả chết tới khi match tiêu thụ một lần, reset khi respawn. |
| Nguyệt thần/Nhật thần | ID drop 2123/2124 không có trong SQL. Đổi sang 1276 Cải trang Nguyệt thần / 1277 Cải trang Nhật thần; kiểm tra người nhận, reset latch thưởng mỗi spawn. |

## Đối chiếu SQL và tài nguyên

- HP/map/hồi sinh boss được định nghĩa trong BossesData và constructor Java; không có bảng SQL định nghĩa boss. Bản sửa không cần migration SQL.
- Item template đủ ID 0–2000. Các ID drop dạng số trực tiếp đã đối chiếu không còn thiếu; chưa kiểm chứng mọi nhánh drop tính động bằng gameplay.
- 12 cấu hình Đại hội võ thuật ghi map 168, nhưng joinMap lấy zone người tham gia. Đây là placeholder không dùng để lookup map; không chèn map giả vào SQL.
- Startup còn báo thiếu tile map 78,95,101,121,125,130,134. Các ID này không có trong danh sách map của 128 BossData tĩnh đã đối chiếu. Tài nguyên map thiếu vẫn cần bộ tile đúng; bản sửa này chưa bổ sung chúng.
- Chi tiết dữ liệu: `.analysis/boss-full-audit/catalog-audit.json` và `boss-catalog.json`.

## Kiểm chứng

- Build toàn bộ Java thành công; 52 nhóm kiểm thử đạt, gồm shop/ký gửi/hồng ngọc/thành viên/nội tại/thông báo boss và vòng đời boss.
- Kiểm thử callback SO1/SO2 thường và Namek, tranh chấp đòn kết liễu, lỗi thưởng, AFK hồi sinh, chat thiếu, manager lỗi, Whis, slot đồng đội null và bốn update riêng.
- Đã tái hiện lỗi trên JAR trước sửa và kiểm thử đạt sau sửa cho cleanup luyện tập, custom update và match tick sau dispose thật.
- Snapshot sau khởi động bản cuối: 438 boss đang được quản lý, không có boss HP 0 kẹt ACTIVE/AFK/CHAT_S ngoài luồng chờ chết hợp lệ. Cổng 14445 đang chạy PID 12684; SHA-256 JAR, backup và SQL snapshot đã đối chiếu.
- Chưa dùng nhân vật trong game giết lần lượt mọi loại boss. Phạm vi xác nhận là source, SQL, fixture hồi quy và snapshot JVM sau triển khai.

## Triển khai và khôi phục

- Đã bảo trì lưu dữ liệu, kiểm tra server dừng, sao lưu SQL, chuyển JAR cũ vào `SRC/jar-backup` trước cài bản mới.
- JAR chạy: `SRC/20.jar`. SHA-256: `61e8ed62a2bc4a7705a3e727b5c76b80bdb24c8895f711ade3b31f25024a9b2e`.
- Backup gần nhất: `SRC/jar-backup/20-before-boss-full-20261005-153033.jar`. SHA-256: `dfed386cc02ebf89ea9bd416c43dbe63b2c79800a737706d8db77745bb698c23`.
- SQL snapshot: `.analysis/boss-full-deploy-20261005-153033/team2026-before-boss-full.sql`.
- Metadata đầy đủ: `.analysis/boss-full-deployment-latest.json`.
- Khôi phục: bảo trì lưu dữ liệu và dừng server, thay JAR từ backup; giữ dữ liệu người chơi hiện tại, không nhập ngược SQL chỉ để đổi phiên bản JAR.
