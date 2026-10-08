# Whis tuần — đã triển khai 08/10/2026

Bảng top Whis tính số trận thắng của tuần. Tuần mới bắt đầu **00:00 thứ Hai, Asia/Ho_Chi_Minh (UTC+7)**. Khi chuyển tuần, chốt ba người có điểm cao nhất tuần vừa kết thúc, rồi bảng tuần mới bắt đầu từ 0. Cấp Whis đã vượt không reset. Tuần đầu chỉ tính các trận từ lúc triển khai; điểm lịch sử `player.thachdauwhis` vẫn giữ riêng.

| Hạng cuối tuần | Bonus trong tuần kế tiếp |
|---|---|
| 1 | +5% sức đánh, HP tối đa, KI tối đa |
| 2 | +4% sức đánh, HP tối đa, KI tối đa |
| 3 | +3% sức đánh, HP tối đa, KI tối đa |

Bonus tự áp dụng, không cần nhận qua NPC; không cộng dồn qua các tuần, không cấp cho người 0 điểm, không cấp cho pet/boss/bot. Tính trên chỉ số tổng sau đồ/hợp thể và giới hạn int hiện có. Người chơi offline vẫn nhận đúng bonus khi đăng nhập trong tuần được thưởng. Nếu server nghỉ qua nhiều tuần, không kéo dài thưởng cũ. Hòa điểm ưu tiên người đạt điểm trước, rồi ID tăng dần. NPC Whis ghi rõ tuần/bonus, bỏ dòng cố định “1 lượt”. Không tự đặt giới hạn một trận/ngày.

**Lần chốt đầu: 00:00 thứ Hai 12/10/2026.** Hiện top và phần thưởng tuần đầu rỗng, chờ người chơi thi đấu.

## Lỗi Whis đã sửa

- Chặn một người tạo nhiều Whis đồng thời, áp dụng cả hai TrainingService; giải phóng token đúng trận khi boss rời map.
- Chỉ chủ trận gây sát thương/nhận điểm; không ghi điểm cho người khác, mob hoặc attacker null. Một boss chết chỉ ghi một trận thắng.
- Giữ cấp đã vượt bằng max, tránh ghi lùi cấp.
- Lưu điểm tuần và cấp trong một transaction; SQL lỗi thì rollback, không báo thắng đã lưu.
- Top dùng snapshot sau commit và refresh ba giây, không phụ thuộc cờ dirty cũ bị reset trước khi lưu.
- Chặn boss update trong cửa sổ superclass đã đăng ký vào manager nhưng constructor chưa gán owner/token.

## SQL, kiểm chứng và startup

Migration [20261008_whis_weekly.sql](../SRC/sql/migrations/20261008_whis_weekly.sql) thêm ba bảng InnoDB `whis_weekly_state`, `whis_weekly_score`, `whis_weekly_reward`. Điểm theo namespace tuần, giữ lịch sử; không reset cấp, không sửa điểm sự kiện khác. Migration cũng được bổ sung vào bốn dump/seed hiện có.

Đã chạy 16 nhóm regression trên compiled classes và cùng 16 nhóm trên candidate JAR, gồm policy/owner guard, rương sưu tầm, pet collection, hợp thể và lifecycle boss. Test SQL bằng schema riêng xác nhận chuyển tuần/năm, hòa điểm, restart, expiry, bỏ qua nhiều tuần, rollback, calPoint đúng 5/4/3% không nhân lặp và gọi Whis.die hai lần chỉ +1 điểm. Schema test đã xóa sau kiểm tra.

Startup lần thử đầu phát hiện pool production chỉ có một connection; phần nạp top được chuyển ra sau `loadDatabase()` để tránh xin connection thứ hai khi đang giữ connection đầu. Bản cuối khởi động thành công với cấu hình pool này, listener **14445**, PID **21004**. Snapshot JVM chỉ đọc xác nhận week_start **2026-10-05**, top/reward rỗng và phần trăm 5/4/3 đúng. Chưa thao tác trận đấu bằng client thật.

## JAR và khôi phục

JAR đang dùng `SRC/20.jar` SHA-256: `5e0dfaf9c3f98540780c15dbc8d6de562ccceb762ecd77537ba7370c25c4fd15`. Source/test hash khớp metadata; 632 source Java. Server đã dừng trước thay JAR, snapshot SQL và các backup được đặt tên riêng theo thời gian, không ghi đè. Chỉ còn JAR ứng dụng đang dùng tại thư mục chạy; các candidate ở `jar-backup`.

Bản chạy ổn trước thay đổi Whis: `SRC/jar-backup/20-before-whis-weekly-20261008-083636.jar`, SHA-256 `13d4a90762111edce5dc683c49655fcaaefa1c89bbf4bcfed98331c6d603a030`. Khi rollback, dùng bảo trì có lưu dữ liệu, backup JAR hiện tại rồi khôi phục bản này; giữ dữ liệu người chơi và bảng Whis mới. Backup ngay trước bản cuối là bản thử gặp lỗi startup, vì vậy metadata ghi riêng đường rollback ổn định.

[Metadata triển khai](../.analysis/whis-weekly-deployment-latest.json), [kiểm chứng JVM](../.analysis/whis-weekly-runtime/runtime.txt), [SQL integration log](../.analysis/whis-weekly-build/database-tests.log).
