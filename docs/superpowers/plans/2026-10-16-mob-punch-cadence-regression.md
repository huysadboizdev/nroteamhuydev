# Khắc phục delay đấm sau bản xác nhận sát thương

**Yêu cầu:** Giữ nguyên tốc độ hồi chiêu Dragon/Demon/Galick. Người dùng xác nhận đã cập nhật JAR trên server và khởi động lại. Lỗi cần sửa là nhịp client bị kéo theo ACK mạng.

**Nguyên nhân trong source:** Char chặn chiêu đang chờ ACK (timeout 8 giây); tracker và callback đều bắt đầu lại thời gian còn lại tại lúc nhận ACK. AutoAttack còn chặn cố định 500 ms. Tàn sát cộng 25 ms vào cooldown và chỉ ghi cooldown khi ACK đến. hasSendAttack bị xóa trước cả kiểm tra cooldown khi bấm sớm.

**Thiết kế:** Dự đoán hồi chiêu tại lúc bắt đầu hoạt ảnh (đánh tay) hoặc xếp gói gửi thành công (tự đánh/tàn sát). Tiếp tục xác nhận sát thương độc lập bằng sequence. ACCEPTED không thay mốc hồi chiêu. Chỉ phản hồi từ chối mới sửa lịch thử lại; phản hồi cũ của cùng chiêu không sửa đòn mới. Giữ nguyên kiểm tra tầm, tài nguyên và server thực thi sát thương. Không thay dữ liệu cooldown.

Đã xem xét bỏ toàn bộ giao thức xác nhận (mất khả năng sửa đòn bị từ chối) và giảm timeout ACK (vẫn làm nhịp phụ thuộc mạng). Chọn xác nhận bất đồng bộ cùng dự đoán cooldown.

## Các bước thực hiện

- [ ] Bổ sung ca chạy source thực tế của cả Game1/Game2: ba chiêu, cooldown 100/250/500/1000 ms, không ACK, ACK muộn và ACK cũ sau đòn mới, không gửi trước hồi chiêu, không gửi trùng khi bấm sớm; quan sát thất bại trước sửa.
- [ ] Sửa MobAttackResultTracker để ACCEPTED không mở cooldown mới và sequence cũ không sửa lịch mới. Giữ TTL chỉ để dọn bộ nhớ.
- [ ] Sửa Char/ModFunc/PickMobController của hai tab: gỡ chờ pending, ghi cooldown khi bắt đầu/gửi, gỡ ngưỡng 500 ms của tự đánh quái và 25 ms cộng thêm của tàn sát; không đặt lại hasSendAttack khi từ chối hoạt ảnh mới.
- [ ] Chỉ áp dụng sửa tọa độ cho kết quả còn hiệu lực. Đối chiếu tầm đánh và từ chối/tài nguyên vẫn hoạt động.
- [ ] Chạy kiểm tra nhịp mới và các kiểm tra hồi quy liên quan; biên dịch runtime C#.
- [ ] Theo ủy quyền build Windows của tác vụ đang tiếp diễn: build lại client, kiểm tra artifact rồi chuyển vào MODNROVEGETA có backup. Không build lại JAR nếu không thay source server.

**Giới hạn bằng chứng:** Kiểm tra mô phỏng thời gian cần phân biệt với kiểm chứng gameplay trực tiếp trên server. Không suy diễn PASS fixture thành mọi đòn mạng thực tế đều được nhận.


## Verified result

- Before the fix, 100 Game1/Game2 assertions failed across three skills and cooldowns from 100 to 1000 ms.
- After the fix, cadence tests pass for immediate ACK, delayed ACK at 75/250/1200 ms, missing ACK, stale ACK ordering, and the exact server cooldown-minus-50-ms boundary.
- Manual, result, massacre, range, and outgoing queue regressions pass. All 406 runtime C# sources compile with zero errors.
- Unity Windows build: zero errors, 64 warnings. The high-speed integration regression passed 169616 checks; EquipmentAura, BlackPowerAura, NPC image decode, and receive queue checks passed.
- Updated MODNROVEGETA/Windows/NRO.exe and ZIP. Previous files are preserved under names ending backup-20261016-151418-335. Install metadata is in MODNROVEGETA/mob-punch-cadence-install.json.
- No JAR rebuild was needed because server source did not change. The user confirmed the new server JAR is live. Live gameplay was not tested after this client build.

## Follow-up: tolerate punch packet jitter without slowing the client

**Cause:** The client schedules melee swings at `coolDown - 50 ms`. The server also requires `coolDown - 50 ms`, measured from the prior packet's server receipt. Variable transit time between consecutive attack packets can make an otherwise ready swing arrive just inside the server's cooldown boundary, so the animation is visible while the server rejects the cast and emits no mob damage packet.

**Change:** Keep client cadence and configured skill cooldowns unchanged. For confirmed melee casts only, admit the packet up to 50 ms earlier than the client's existing boundary by using a 100 ms server grace. Pass the same grace through the locked skill executor so its second cooldown check cannot undo the confirmed-cast admission. Legacy casts and non-melee skills retain their 50 ms rule.

**Regression:** Extend `SRC/test/nro/models/services/MobPunchDeliveryTest.java` with a confirmed Dragon/Demon/Galick hit at `coolDown - 75 ms` elapsed. It must be accepted, reduce HP, emit exactly one `-9` packet, and still reject an immediate duplicate. Run the test red before the production change and green after; also run `SkillCastLifecycleTest` to cover the shared executor.

**Install:** Build only the changed server class into a uniquely named staging JAR, verify it, check that the local server process and port 14445 are stopped, back up `SRC/20.jar` under a unique timestamp, then install the candidate and record both SHA-256 values plus restore metadata. The remote server at `160.191.245.239:14445` is outside this workspace and cannot be updated from here.
