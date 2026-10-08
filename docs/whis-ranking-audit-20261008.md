# Kiểm tra bảng xếp hạng Whis — 2026-10-08

Phạm vi chỉ đọc mã nguồn, bytecode JAR hiện tại, metadata triển khai và SQL `team2026`. Không sửa nguồn/JAR/SQL, không gọi trận đấu trên nhân vật thật, không dừng server. Skills áp dụng: `gitnexus-exploring`, `systematic-debugging`; GitNexus không có tool trong phiên nên truy vết bằng `rg` và đọc nguồn.

## Hành vi hiện tại

- Mỗi lần hạ Whis, **người ra đòn kết liễu được +1 `thachdauwhis`**. Đây là số trận thắng dùng cho Top 100. Không phải điểm tăng HP/KI/sát thương. `boss/luyen_tap_tu_dong/Whis.java:101–113`.
- Chủ trận được ghi `traning.top = LV vừa hạ`, thời gian trận và thời điểm thắng. Trận tiếp theo LV = `max(1, top + 1)`. Boss có HP gốc 550.000, sát thương gốc 10.000; HP và sát thương nhân LV, giới hạn 2.147.483.647; sát thương người chơi gây ra bị chia LV và có 40% né nếu không xuyên. `Whis.java:33–36,66–84`; `boss/BossesData.java:2479–2493`.
- **Không có cộng chỉ số nhân vật, sức mạnh/tiềm năng, vật phẩm, vàng, ngọc, ruby hoặc thưởng theo hạng** trong đường thắng/top Whis. `sendMoney()` chỉ gửi lại số dư hiện tại (`services/Service.java:1325`). `player/NPoint.java` không tham chiếu `Whis`, `thachdauwhis` hoặc `traning`; các trường `lastTop`/`lastRewardTime` chỉ được lưu/nạp, không có nơi thực hiện nhận thưởng. Tuyệt kỹ ở NPC Whis là giao dịch riêng, không phụ thuộc hạng.
- NPC bản đồ 154 gọi **`services_dungeon.TrainingService`** với `false`; cả hai biến thể TrainingService đều tạo `new Whis(pl)` mà không kiểm tra phí vào, lượt/ngày, cooldown hay trận đang tồn tại. Dòng “Ngươi còn 1 lượt” là chữ cố định (`npc_list/Whis.java:38–40,92`; `services_dungeon/TrainingService.java:78–109`; `services/TrainingService.java:76–110`). `levelLuyenTap` của luyện tự động không được tăng bởi override `Whis.die()`.
- Thắng gọi full `PlayerDAO.updatePlayer(plKill)` trước rồi lưu `data_luyentap` chủ trận bằng `TraningDAO`. `data_luyentap[5..9]` lần lượt là level đã hạ, thời gian trận, lần thắng cuối, lastTop, lastRewardTime (`database/TraningDAO.java:21–31`; `database/MrBlue.java:983–994`). Full save chứa `thachdauwhis` (`database/PlayerDAO.java:1088,1144`).
- Top lấy SQL `SELECT id, thachdauwhis FROM player ORDER BY thachdauwhis DESC LIMIT 100`, hiển thị số trận thắng dưới nhãn “Level” (`server/Manager.java:119,1002–1005`). Nạp lúc khởi động, refresh theo cờ mỗi 3 giây (`server/ServerManager.java:150–194`); không dùng thời gian trận để xử lý hòa.

## Lỗi đã xác định và giới hạn bằng chứng

1. **P2 — Có thể tạo nhiều Whis cùng LV và kéo lùi level đã vượt.** Admission không giữ khóa/chặn trận trùng. Boss chụp level khi `joinMap`; thắng sau đó gán trực tiếp top thay vì giữ max. Lịch hợp lệ: tạo A/B LV1; hạ A, tạo và hạ C LV2; hạ B còn sống => thắng 3, level tụt từ 2 về 1. Top số thắng vẫn tăng, khó nhất đã vượt và bảng “Level” lệch nhau. Bằng chứng: hai `callBoss()` và `Whis.java:33,102,110`. Fixture lịch trong bộ nhớ xác nhận, chưa thao tác client thật.
2. **P2 — Người khác có thể nhận điểm kết liễu trong khi chủ trận nhận level.** Whis `injured()` không giới hạn người đánh là `playerAtt`; TrainingBoss đổi boss sang `PK_ALL`, `SkillService.canAttackPlayer2()` cho phép mọi người đánh PK_ALL (`TrainingBoss.java:54–58`; `Boss.java:450–452`; `SkillService.java:1263–1272`). `die()` cộng thắng cho `plKill` nhưng level cho `playerAtt`. Fixture owner/outsider xác nhận sự phân tách; chưa thử giao tranh thật.
3. **P2 — Refresh top có thể mất tín hiệu thắng, để bảng cũ tới lần thay đổi tiếp theo.** Cờ dirty được bật trước lưu SQL; updater có thể đọc SQL cũ rồi xóa cờ, trong khi lần thắng mới đang lưu. Updater còn xóa tất cả cờ sau cập nhật và một lần nữa bên ngoài; cờ là boolean thường (`Manager.java:125,137–143`; `Whis.java:103–107`; `ServerManager.java:152–155,189–194`). Fixture lịch đọc-trước-lưu tái hiện cached=0, SQL=1, dirty=false. Đây là race được suy ra từ mã, không kết luận đã xảy ra trên server.

Ngoài ba lỗi trên, hai lần lưu thắng/level không dùng chung transaction. Full save có nhiều gate có thể return sớm và DAO chỉ log exception; `TraningDAO` vẫn có thể ghi level. Mất kết nối/crash giữa hai lần ghi có thể tạo chênh lệch. Đây là rủi ro độ bền dữ liệu, chưa fault-inject và chưa thấy dữ liệu lệch ở SQL hiện tại. UI “1 lượt”, nhãn “Level” và việc không có thưởng top là mô tả hành vi/ý định chưa được xác nhận, không tự coi là lỗi cần sửa.

## SQL và đối chiếu JAR

SQL chỉ chạy SELECT/SHOW bằng mysql local, không hiển thị secret. Snapshot đọc: 2 nhân vật; 0 có `thachdauwhis>0`; max thắng 0; số lệch `thachdauwhis` với `data_luyentap[5]` là 0. Cả hai có level/lastTop/lastRewardTime = 0. `boss_live_override` không có dòng boss ID -364 (WHIS); không tìm thấy ký hiệu riêng `bossWHIS` trong nguồn, thực thể tương ứng là `BossID.WHIS`/`BossesData.WHIS`.

`SRC/20.jar` SHA-256: `13d4a90762111edce5dc683c49655fcaaefa1c89bbf4bcfed98331c6d603a030`, khớp `thien-su-crafting-deployment-latest.json` ngày 2026-10-07 21:56:54 +07. Script kiểm chứng 43 hash nguồn thuộc các tên lớp đã chọn, gồm toàn bộ đường Whis. `javap -c -p` lớp Whis trong JAR xác nhận thứ tự win++, bật cờ, sendMoney, PlayerDAO save, setTop/time/lastTime, TraningDAO save. Không phát hiện source/JAR drift trong phần đã kiểm tra.

Đối chiếu snapshot JVM chỉ đọc do luồng kiểm toán chính thu thập: `.analysis/ranking-events-runtime-20261008/runtime.txt`, lúc **2026-10-08 08:14:06 +07**, PID **11064**, code source `SRC/20.jar`, `Topwhis.count=2`. Đây là kiểm chứng live danh sách đã nạp; không dùng PID triển khai cũ 15832. Hash trên đĩa và code source được kiểm riêng, không thay JAR hoặc restart.

Kiểm chứng độc lập: `python .analysis/whis-audit/verify_readonly.py`. Script kiểm tra hash/source và tái hiện ba lịch bằng mô hình trong bộ nhớ; không chạy Java production/SQL mutation. Mọi đường dẫn nguồn bên trên tính từ `SRC/src/nro/models/`.
