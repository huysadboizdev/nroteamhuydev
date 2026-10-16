# Rà lại đánh tay và tự đánh quái — 16/10/2026

## Phạm vi và giới hạn bằng chứng

Đối chiếu source server Teamobi2026 và client PRJ_2Tab_550K (Game1/Game2), cùng triệu chứng trong video `NRO BEAR 2026-10-16 11-30-37.mp4` đã xem ở lượt trước.

Video cho thấy hoạt ảnh và các lần không hiện sát thương; không chứa mã phản hồi mạng của từng đòn. Không đủ bằng chứng để quy mọi cú hụt trong video cho OUT_OF_RANGE hoặc COOLDOWN. Các lỗi dưới đây được tái hiện bằng mã xử lý thực tế trong kiểm tra cô lập. Chưa kiểm tra trận đánh trực tiếp bằng client/server đang chạy với source mới.

## Các lỗi đã tái hiện và sửa ở lượt rà này

1. Bấm lại khi chưa hồi chiêu xóa lượt đánh lại đang chờ, trước khi kiểm tra xem có được bắt đầu hoạt ảnh mới hay không. Chuyển thao tác hủy sang sau các điều kiện cho phép bắt đầu đòn mới.
2. Đòn đánh tay thành công không áp dụng thời gian hồi còn lại mà server trả. Dùng cùng hàm SetUsedSkill(skill, retryAfterMs) như tự đánh để đồng bộ theo phản hồi.
3. Lượt đánh lại còn tồn tại sau đổi map, chết hoặc mất kết nối. Bổ sung kiểm tra trạng thái và map, đồng thời giữ kiểm tra đúng đối tượng chiêu/quái.
4. Đường AutoAttack thường có thể gửi trong lúc hoạt ảnh đánh tay chưa gửi đòn. Chặn tự gửi khi còn hoạt ảnh hoặc projectile.
5. AutoAttack thường kiểm tra chủ yếu khoảng cách ngang, bỏ qua khoảng cách dọc; GameScr.isAttack dùng hình chữ nhật theo dx/dy, không khớp vòng tròn tầm cận chiến của server. Dùng MassacreAttackRange sẵn có cho bước áp sát trước hoạt ảnh và trước tự gửi, đồng bộ tọa độ trước gói đánh. Các chiêu đánh xa tiếp tục dùng dx/dy theo helper.
6. getMobCastRetryAfterMs chỉ nhìn skillSelect. Đổi chiêu sau khi gửi đòn có thể làm thời gian hồi của chiêu vừa dùng bị trả thành 0. Tra chiêu đã sở hữu theo skillTemplateId khi chiêu đang chọn khác ID yêu cầu.
7. Hoàn KI/thể lực theo phỏng đoán ở callback từ chối có thể cộng chồng lên cập nhật tài nguyên server đã nhận. Server gửi HP/MP và thể lực chính xác trước phản hồi từ chối; client không cộng hoàn lại lần nữa. Khi chưa gửi được gói, chỉ hoàn tài nguyên cục bộ một lần cho hoạt ảnh đó.
8. Phản hồi COOLDOWN/REJECTED từng kéo tọa độ người chơi về snapshot server. Giới hạn việc sửa tọa độ này cho OUT_OF_RANGE.

Không thay giá trị cooldown cấu hình ba chiêu. Thời gian hồi phản hồi dùng đúng điều kiện `coolDown - 50` đã tồn tại trong canUseSkillWithCooldown; không thêm thời gian chờ cố định mới.

## Kiểm chứng

### Client

`Tests/ManualMobAttackChecks.ps1` trích các đoạn code thực tế từ Char, ModFunc và điều kiện áp sát trong GameScr để thực thi cùng fixture không cần Unity. Không sao chép lại thuật toán đang kiểm tra. Chạy cả Game1 và Game2.

Đã quan sát thất bại trước sửa cho: bấm sớm làm mất lượt retry; không hủy khi đổi map/chết/disconnect; không đồng bộ cooldown đòn được nhận; cộng hoàn chồng cập nhật tài nguyên; tự đánh cạnh tranh đòn tay; tự gửi ngoài tầm; bước áp sát chấp nhận điểm chéo ngoài vòng tròn; hoàn tài nguyên hai lần khi gói không gửi được. Chạy lại sau sửa: PASS.

Các script hiện có đã chạy lại và PASS:

- MobAttackResultChecks.ps1 (điều chỉnh phép kiểm tra source cho callback mới).
- MassacreRangeChecks.ps1.
- MassacreAnimationGateChecks.ps1.
- OutgoingMessageQueueChecks.ps1: 160.000 phần tử, FIFO, không trùng/mất.

Source client được biên dịch bằng Roslyn của Unity với các reference/define từ response file của dự án; 406 source runtime, không lỗi biên dịch (64 warning). Chỉ tạo assembly kiểm tra trong `.analysis/mob-combat-review-20261016`, không mở Unity batchmode và không tạo EXE/APK/WebGL.

### Server

Biên dịch source Java vào `.analysis/mob-combat-review-20261016/classes` bằng Java 21 và thư viện sẵn có; không đóng gói JAR.

MobPunchDeliveryTest chạy luồng thật useSkillForMob → useSkillAttack → playerAttackMob → Mob.injured. Fixture đặt sát thương cố định 100 và thay network/ghi nhận EXP bằng bộ đếm, không kết nối SQL. Với 30 đòn sẵn sàng cho mỗi Dragon/Demon/Galick, cả 90 đòn đều trừ HP, gửi packet -9 và gọi cộng EXP đúng một lần. Mỗi lần thử trùng ngay sau đòn hợp lệ bị trả COOLDOWN và không gây sát thương lần hai. Đây là kiểm tra xử lý đòn, không phải đo tốc độ đánh ngoài thực tế hay kiểm chứng toàn bộ công thức EXP/sát thương trang bị.

Test cũng kiểm tra 101px ngoài tầm, hết thể lực, thiếu MP, đổi chiêu sau gửi, và cập nhật tài nguyên khi server từ chối. Ca đổi chiêu đã thất bại trước khi sửa tra cooldown; sau sửa toàn bộ PASS.

SkillCastLifecycleTest: mob-cast-range, mob-cast-owned, mob-retry-active, mob-retry-expired, serialized-use, boss-cast-range, boss-cast-owned đều PASS. Fixture đánh quái được bổ sung thể lực 100 để thỏa điều kiện đòn hợp lệ.

## Trạng thái triển khai

Chỉ sửa source và kiểm tra cô lập. Chưa thay SRC/20.jar, chưa dừng server, chưa build APK/EXE/JAR. Vì phản hồi COOLDOWN và đồng bộ tài nguyên liên quan cả server lẫn client, cần sử dụng hai phía cùng phiên bản khi thực hiện lần build/cập nhật được yêu cầu sau này.

Đã đọc trực tiếp class trong SRC/20.jar: có useSkillForMob/attackMobConfirmed/sendMobAttackResult, chưa có COOLDOWN. Điều này xác nhận JAR tại thư mục chạy có giao thức xác nhận cũ, chưa chứa toàn bộ chỉnh sửa lần này; không thể đánh giá source mới chỉ bằng việc chơi tiếp bản hiện có. Không xác nhận được process đang chạy bằng CIM do quyền truy cập bị từ chối.
## Tr?ng th?i tri?n khai

Theo x?c nh?n c?a ng??i d?ng: build JAR + client Windows v? c?i JAR trong workspace hi?n t?i.

- JAR m?i ?? c?i t?i `SRC/20.jar`; SHA-256 `d27d05ed9218fd9925b8cde3a8277764ff822089eea68cc2be8e02b833f315e2`.
- Tr??c khi thay ?? x?c nh?n kh?ng c? JVM game ho?c c?ng 14445 ?ang l?ng nghe. Kh?ng kh?i ??ng server sau khi thay.
- JAR c? ?? chuy?n v?o `SRC/jar-backup/20-before-mob-punch-reliability-20261016-144322-403.jar`; SHA-256 `bef522607a0eefb4f9cb799af22b548ed06e72d658736007ea3f42d734db93f9` kh?p b?n g?c.
- ??ng g?i thay ch?nh x?c 5 class bi?n d?ch t? Service/SkillService, so s?nh n?i dung c? 4867 entry c?n l?i v?i JAR c?: gi? nguy?n. Kh?ng ??ng g?i fixture/test.
- Ch?y l?i MobPunchDeliveryTest v? 7 ca SkillCastLifecycleTest tr?n ch?nh JAR trong SRC sau c?i: PASS.
- Ba JAR ?ng d?ng d? trong SRC/build v? .analysis ?? chuy?n v?o jar-backup, ??i chi?u SHA-256; th? vi?n kh?ng di chuy?n.
- Client Windows Unity 2022.3.62f2: `Result=Succeeded`, `Errors=0`, `Warnings=64`; hai scene NROL/NROL1, Mono2x. Th? m?c: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/Distribution/20261016-MobPunchReliabilityFix-144042/Windows`.
- Unity Verify: EquipmentAura, BlackPowerAura (202 checks), NpcImageDecode, HighSpeedSkillRegression (169616 checks), ReceiveQueueBudgetRegression ??u PASS; ki?m tra k? n?ng Game1/Game2 t? x1 ??n x100 ??t.
- Metadata kh?i ph?c: `SRC/jar-backup/mob-punch-deployment-20261016-144322-403.json`.

Client gi? nguy?n ??a ch? `160.191.245.239:14445`. L?n n?y ch? c?i JAR t?i workspace; server t? xa ch?a ???c thay JAR. C?n d?ng hai ph?a c?ng phi?n b?n tr?n server m? client k?t n?i ?? ?p d?ng ??y ?? s?a ??i. Ch?a x?c nh?n tr?n ??nh tr?c ti?p b?ng hai b?n m?i; k?t qu? ki?m tra c? l?p kh?ng thay th? ki?m ch?ng gameplay/m?ng th?c t?.

## Chuy?n client theo y?u c?u

?? chuy?n b?n Windows v? ZIP sang `C:/Users/Admin/Downloads/filefolder/MODNROVEGETA`; ch?y `Windows/NRO.exe`. Th? m?c Windows c? ???c gi? t?i `Windows-backup-20261016-145437-093`. ?? ??i chi?u SHA-256 133 file v? ZIP sau khi chuy?n.


## Mob punch cadence follow-up

The user confirmed the new JAR was running on the server. Inspection found the client waited for attack ACKs, reset cooldown at ACK arrival, kept a 500 ms floor in ordinary mob auto attack, and used full cooldown while the server accepts again at cooldown minus 50 ms. Massacre also added 25 ms. These waits stacked with network latency.

The client now follows the server's cooldown-minus-50-ms rule for manual, ordinary auto, and massacre mob punches. It does not wait for ACK to continue; old or late ACKs cannot reset a newer cooldown or player position. Early manual clicks cannot rearm the already sent animation. PvP keeps its previous gates. Windows client is installed at C:/Users/Admin/Downloads/filefolder/MODNROVEGETA/Windows/NRO.exe. Install metadata is in MODNROVEGETA/mob-punch-cadence-install.json. Cadence regressions and Windows build passed; live gameplay was not tested after this build.

## Follow-up packet jitter fix - 2026-10-16 16:52 (+07:00)

Video review still showed punch animations on Mộc Nhân without damage text. Source tracing found that the client predicts melee readiness at `coolDown - 50 ms`, while the server validates at the same boundary from the prior packet's server receipt time. Packet transit variation can shrink the server-observed interval below that threshold; the server then returns COOLDOWN and never calls `Mob.injured`, even though the client already played the swing.

For confirmed melee mob casts only, `SkillService` now allows a 100 ms grace, giving 50 ms of transit-jitter tolerance beyond the client's current send cadence. The same grace is passed to the locked executor's second cooldown check. Configured cooldowns, client cadence, legacy casts, and non-melee cooldown checks are unchanged. The regression models a request arriving 25 ms inside the old boundary and verifies damage, one `-9` packet, and rejection of an immediate duplicate.

- TDD red: the new case failed before the fix with the expected COOLDOWN result.
- TDD green: `MobPunchDeliveryTest` passes on the installed `SRC/20.jar`; all seven `SkillCastLifecycleTest` modes pass.
- Installed JAR SHA-256: `1b77dff5ddda99ab3e097b213bd96be4d86c17832e3f7b2c563c0dbd7333a5a6`.
- Previous JAR backup: `SRC/jar-backup/20-before-mob-punch-jitter-20261016-165147-647.jar`, SHA-256 `4e04372ef57530b4d6772d10132c3a6281c9fe90b864c35b2212f730e2324d4a`.
- Restore metadata: `SRC/jar-backup/mob-punch-jitter-20261016-165147-647.json`.
- Local port 14445 was not listening. The only Java processes were VS Code language servers. The remote server `160.191.245.239:14445` was not updated, so live gameplay against this JAR remains unverified.
