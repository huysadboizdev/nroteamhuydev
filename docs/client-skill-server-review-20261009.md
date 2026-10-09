# Rà soát server cho lỗi skill khi client tăng tốc x5+ (2026-10-09)

Phạm vi: `708cb8979` → `d70b13e23` và diff chưa commit của `SkillService.java`; truy vết thêm Controller, EffectSkillService, PlayerSkill, EffectSkill, Player, ChangeMapService, Pet và Service. Chỉ đọc source; không chỉnh server, không build/thay JAR. Áp dụng systematic-debugging (truy vết nguyên nhân, đối chiếu lịch sử) và nguyên tắc review nêu vị trí, hậu quả, cách sửa. Không có GitNexus index/tool được sử dụng; đối chiếu bằng git/rg và đọc đầy đủ các nhánh liên quan. Chưa chạy tái hiện trên server đang hoạt động, các nhận định dưới đây là kết quả phân tích luồng code.

## Lịch sử và giới hạn quy trách nhiệm

`git log --all` hiện chỉ có hai commit, cùng tác giả Huydev:

- `708cb8979`, 2026-10-05 18:36:31 +07:00, `first commit`.
- `d70b13e23`, 2026-10-08 12:23:16 +07:00, `chore: sync Teamobi2026 source and resources`.

Không có chứng cứ từ git để gán từng thay đổi cho Claude hoặc một AI cụ thể. Client không có lịch sử git tương ứng nên không suy diễn nguồn tác giả client.

Diff hai commit trong EffectSkillService chỉ đổi phép nhân HP biến khỉ sang long. PlayerSkill/EffectSkill không thay đổi. Các nhánh chuẩn bị TU_SAT/QCKK/MAKANKOSAPPO nêu dưới đây có từ baseline `708cb8979`; chúng không phải regression mới do `d70b13e23`. Async TU_SAT là thay đổi **chưa commit** hiện tại. Thay đổi notice/ID skill trong commit mới không tự sửa cơ chế chuẩn bị hoặc thời gian chờ.

## Phát hiện có căn cứ

1. **P1 — TU_SAT bị hủy im lặng khi client gửi hoàn tất quá sớm.** `SRC/src/nro/models/services/SkillService.java:749`–752: request thứ hai trước 1500 ms đặt `lastTimeUseThisSkill` về thời điểm hiện tại, xóa `prepareTuSat`, rồi return mà không nổ và không báo hủy/cooldown cho client. Chuẩn bị ban đầu gửi 2000 ms ở dòng 747. Nếu client kết thúc bằng số frame và frame chạy nhanh x5+, việc kết thúc dưới 1500 ms giải thích trực tiếp hiện tượng “gồng nhưng không nổ”; điều kiện thời gian server là bằng chứng, phần tăng tốc frame cần xác nhận bên client. Sửa client dùng thời gian thực cho hoàn tất; nếu sửa server, request sớm nên bị bỏ qua trong khi vẫn giữ cast đang chờ, hoặc trả phản hồi hủy rõ ràng, thay vì tự bắt đầu cooldown.

2. **P1 — Async TU_SAT có thể lên lịch nhiều vụ nổ cho cùng một cast.** `SkillService.java:754`–807 (diff chưa commit): `prepareTuSat` vẫn true trong 1500 ms chờ; lần gọi tiếp theo tiếp tục schedule một task mới vì cooldown chỉ cập nhật trong callback dòng 797. Task dùng đảo boolean ở dòng 763 nên task thứ hai còn có thể bật chuẩn bị lại. Không có future/token/pending guard. Sửa bằng một cast duy nhất có trạng thái pending, task ID chống lặp và chuyển trạng thái một lần; xử lý trên vòng update/game scheduler hiện hữu thay vì tạo một executor/thread mỗi request. Đây là vấn đề server AI/boss/pet; nhánh này không chạy với player thường.

3. **P1 — Async TU_SAT đọc skill đang chọn và thế giới có thể đã thay đổi.** `SkillService.java:764`, 766, 775–797: callback đọc `bomber.playerSkill.skillSelect.point`, `zone`, HP, rồi `affterUseSkill(bomber, TU_SAT)`. Tuy truyền template ID TU_SAT, mana ở dòng 1225–1243 và cooldown ở 1304–1305 vẫn lấy `skillSelect` hiện tại. AI Pet gán chọn skill ở `Pet.java:610`, nhiều nhánh tấn công ở 385–531; Boss gán ngẫu nhiên ở `Boss.java:770`. Vì callback chạy muộn trên thread riêng, có thể dùng cấp skill khác, trừ mana hoặc khóa cooldown của skill khác; TU_SAT vẫn sẵn sàng. Không kiểm tra chết, đổi map, bị dispose hoặc zone null trước gây sát thương. `ChangeMapService.java:940` đặt zone null; `PlayerSkill.java:107` xóa skillSelect khi dispose. Sửa chụp đúng Skill/zone/cast ID từ lúc chuẩn bị, hủy khi lifecycle thay đổi, tính chi phí/cooldown trên Skill đó, đồng bộ với trạng thái player. Không giữ nguyên callback này chỉ bằng việc đổi ID notice.

4. **P2 — QCKK và MAKANKOSAPPO không thực thi thời gian chuẩn bị server.** `SkillService.java:456`–510: lưu `lastTimePrepareQCKK`/`lastTimePrepareLaze`, gửi 4000/3000 ms, nhưng nhánh hoàn tất không kiểm tra mốc đó. Một request tấn công lặp có thể biến thành hoàn tất ngay; cooldown/mana chỉ cập nhật sau nhánh hoàn tất. Đây là lỗi có sẵn, tương phản với TU_SAT vốn phạt request sớm. Sửa client chỉ gửi hoàn tất một lần theo thời gian thực; nếu sửa server, dùng cast ID và kiểm tra thời gian tối thiểu rõ ràng cho hai skill. Cần xác định thời gian game chuẩn trước khi đổi balance server.

5. **P2 — BIEN_KHI kết thúc dựa trên skillSelect có thể đổi trong lúc chờ.** `EffectSkillService.java:277`–279 giữ trạng thái 1500 ms; `EffectSkill.java:242`–243 gọi finish; `EffectSkillService.java:294`, 301 lấy thời gian/cấp khỉ từ skillSelect hiện tại. `Controller.java:645`–648 vẫn cho packet chọn skill chạy trong thời gian bị khóa cast, và `SkillService.java:1382`–1387 không bảo vệ pending transform. Đổi skill trong 1500 ms có thể áp dụng cấp của skill khác cho biến khỉ. Sửa lưu Skill BIEN_KHI/cấp ở lúc bắt đầu, dùng snapshot khi hoàn tất; không kéo ngắn timer server vì client tăng tốc. Từ nguồn hiện tại, BIEN_KHI vẫn chờ thời gian thực server, không có lý do sửa 1500 ms theo hệ số tốc độ client.

6. **P2 — Thiếu hủy chuẩn bị khi đổi skill/map.** `PlayerSkill.java:57`–63 chỉ lưu boolean và timestamp, không lưu Skill/zone/cast token. `SkillService.java:1382`–1387 chọn skill không xóa pending; `ChangeMapService.java:902`–944 không xóa prepareQCKK/Laze/TuSat. Chọn skill khác rồi trở lại, hoặc đổi map trong lúc gồng, có thể khiến request lần sau đi thẳng nhánh hoàn tất của cast cũ. Chết đã có cleanup ở `Player.java:1311`–1314, nên không ghi nhận “chết không xóa prepare” cho player thường. Sửa cleanup tập trung tại đổi map/chọn skill/lifecycle, hoặc xác minh cast vẫn thuộc đúng skill/zone trước hoàn tất.

7. **P2 — Packet status 20 được xác thực bằng skill cũ trước khi chọn skill được yêu cầu.** `SkillService.java:90`, 100 kiểm tra type/mana/cooldown trên skillSelect trước; dòng 109–111 mới select skillId rồi luôn return false. Skill cũ còn cooldown có thể ngăn cả select; skill cũ type 2 có thể buff trước khi kiểm tra ID mismatch. Áp dụng cho tuyệt kỹ status 20, không khẳng định là nguyên nhân của bốn skill đang sửa. Sửa resolve/validate skillId trước các kiểm tra và chỉ thực thi theo skill đúng của packet.

8. **P2 — Đồng bộ giảm cooldown nội tại chỉ có một slot player.** `SkillService.java:1307` gọi `setIntrinsic`; `EffectSkillService.java:328`–332 ghi đè skillID/cooldown/time trong một bộ biến duy nhất, `EffectSkill.java:245`–246 giải phóng slot sau thời gian giảm. Dùng skill nội tại thứ hai trước khi skill đầu hết timer làm mất timer skill đầu. Server `lastTimeUseThisSkill` vẫn giữ cooldown đầy đủ ở dòng 1304; giảm cooldown thực hiện bằng packet -94 và reset timestamp ở `Service.java:1523`–1533. Sửa giữ timer/readyAt theo từng Skill và đồng bộ thời gian còn lại. Đây là vấn đề có sẵn, chưa chứng minh xuất hiện trong ca x5+.

## Tuyến packet và việc cần giữ nguyên

`Controller.java:517`–524 nhận command -45, đọc status rồi gọi useSkill với target null. Request tấn công có target gọi `Service.attackMob` (`Service.java:1166`–1171) hoặc `attackPlayer` (1457–1464), cả hai gọi useSkill với status -1. Skill đang chọn do command 34 thay đổi (`Controller.java:645`). Packet chuẩn bị/biến khỉ và packet client gửi hoàn tất là hai chiều khác nhau; cần xử lý đúng cả client frame timer lẫn server thời gian thực.

Không cần đổi cooldown chuẩn hoặc số 1500/2000/3000/4000 chỉ để tương thích tốc độ x5+. HP long và ID executingSkill cho notice không có chứng cứ gây lỗi thời gian chờ; không khuyến nghị rollback cả commit sync lớn.

## Kiểm chứng đề nghị trước sửa server

- Ghi timestamps start/request complete/accepted complete tại x1, x5, x10 cho bốn skill; TU_SAT completion phải từ 1500 ms trở lên và gửi đúng một lần.
- QCKK/MAKANKOSAPPO gửi request complete lặp trước và sau thời gian chuẩn: chỉ một lần gây damage, trừ mana/cooldown.
- BIEN_KHI chọn skill khác giữa hai mốc 0/1500 ms: cấp khỉ phải giữ cấp BIEN_KHI.
- Boss/pet TU_SAT gọi lại nhiều lần trong 1500 ms; đổi skill, chết, đổi map hoặc dispose trong lúc chờ: một cast chỉ nổ một lần trong đúng zone, hoặc được hủy.
- Dùng hai skill có giảm cooldown nội tại trước khi timer đầu hết: cả hai phải được giảm đúng.

Các test runtime trên chưa thực hiện trong audit. Không có sửa server hoặc deploy JAR trong báo cáo này.
