# Kiểm tra hết giờ và đăng nhập lại: Mabư / Tapion

Kiểm tra ngày 2026-10-05. Chỉ audit; không sửa source, SQL hoặc thay JAR trong lượt này.

## Kết quả

| Khu vực | Hết giờ khi online | Thoát game gần hết giờ rồi đăng nhập |
|---|---|---|
| Mabư 12h, map 114–120 | `MajinBuuService.update` phát hiện ngoài 12h–13h, bắt đầu chờ khoảng 30 giây, sau đó `DungeonExitService` gọi tàu về nhà 21 + gender. Thất bại thì giữ `goHome` để thử lại. | `MrBlue.loadPlayer` đưa về nhà nếu đăng nhập ngoài giờ mở. Đăng nhập trong giờ mở vẫn có thể ở lại sự kiện; đó là hành vi hiện tại. |
| Mabư 14h, map 127–128 | `MajinBuu14H.update` ngoài 14h–15h gọi finish, tàu về nhà; các tick sau tiếp tục thử người chưa ra. Giải phóng trạng thái bị giữ trong bụng Mabư khi thoát. | `MrBlue.loadPlayer` luôn đưa vị trí đã lưu ở 127/128 về nhà, kể cả đăng nhập trong giờ mở. |
| Map 126 qua Tapion ở thành phố Vegeta 19 | Có lỗi: đoạn đuổi trong `Player.update` đã bị comment. Không có handler timeout khác tìm được cho map 126. | Có lỗi: map 126 không có nhánh reset vị trí hoặc chặn giờ khi load. Lưu ở 126 có thể vào lại 126 ngoài giờ NPC cho vào. |

Map 126 là khu Hirudegarn qua NPC Tapion; SQL live đặt tên `Thành phố Santa`.

## Lỗi và sự khác biệt cần chú ý

- `Player.java:533`: kiểm tra giờ 22h–23h và map 126 nhưng dòng chuyển về 19 bị comment. Kể cả bật lại dòng cũ thì nó dùng `changeMapNonSpaceship`, chưa phải tàu cưỡng chế.
- `Tapion.java:34`: cho vào từ 1h đến trước 23h bằng `Calendar` mặc định, nhưng thông báo yêu cầu quay lại 22h. Lịch vào và thông báo không khớp; cần xác định lịch mong muốn trước sửa.
- `MrBlue.java:297`: reset cho nhiều map sự kiện, nhưng thiếu 126; `MapService.getMapCanJoin` cũng không chặn giờ 126. Đây là đường tránh kiểm tra giờ vào NPC bằng logout/relogin.
- Mabư 12h và 14h cố ý miễn cưỡng chế với nhân vật admin. Nếu thử bằng admin thì không có tàu khi hết giờ là hành vi của source hiện tại.
- Người offline không thể xem hoạt ảnh tàu; biện pháp đúng ở phiên đăng nhập là sửa vị trí trước khi vào map. Với Mabư, source đã có xử lý này.

## Bằng chứng kiểm tra

- Đối chiếu bytes 6 class liên quan với `SRC/20.jar` đang dùng, hash JAR `9f549bc7726e3a5507d180b7ab544a1d6dc64c429ac3497a07a309bf20fd4764`.
- Kiểm thử fixture chạy các phương thức thật trên class đã đối chiếu với JAR: hết giờ Mabư 12h, Mabư 14h, giải phóng người bị giữ và không kéo lại người đã rời map. Cả 4 ca qua, xác nhận packet tàu `-65` và vị trí sau chuyển.
- Test: `.analysis/MabuExitAudit.java`; log: `.analysis/black-ball-verify/mabu-exit-audit.log`.
- Đường đăng nhập được đọc theo `MySession.login -> MrBlue.login/loadPlayer -> getMapCanJoin`. Chưa thực hiện logout/relogin thật bằng client hoặc quan sát hoạt ảnh tàu; không chỉnh giờ server để giả lập đóng sự kiện.
- Đọc danh mục map từ SQL live; không sửa dữ liệu người chơi.

## Hướng sửa map 126

1. Thống nhất lịch mở/đóng và thông báo, dùng mốc UTC+7 qua `TimeUtil`.
2. Khi ngoài giờ, gọi `DungeonExitService.exit` về map 19; giữ kiểm tra origin và thử lại nếu chưa ra.
3. Khi load vị trí map 126 ngoài giờ, chuyển về map 19 trước khi đăng nhập vào map.
4. Chặn vào map 126 ngoài giờ tại service chung, không chỉ NPC Tapion.
5. Test online hết giờ, mất kết nối trước giờ đóng, đăng nhập lại sau giờ đóng, nhân vật chết/bị giữ và không kéo lại người đã dùng Capsule ra ngoài.

Chưa áp dụng các sửa này.
