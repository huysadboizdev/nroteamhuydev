# Thông báo sát thương trên 20 triệu — triển khai08/10/2026

Đã triển khai server. Sát thương thực tế HP mất >20.000.000 do người chơi trực tiếp đánh boss/người/quái phát cùng nội dung trên thế giới (Hệ thống) và info phía dưới, toàn server. Ví dụ: Người chơi abc dùng chiêu Kamejoko với mức sát thương là 25.000.000.

Chống spam mỗi người1 tin/5 giây, toàn server1 tin/giây, tối đa1 tin/cast AOE; bỏ qua hit trong cooldown. Không trừ ngọc/chat cooldown. Không tính overkill, pet/bot/boss, phản dame/độc/miss. Scope snapshot đúng skill thực thi; xử lý normal/newSkill/AOE/Tự sát, MobMe target, SuperBu delegation/Yardart protected HP. Kênh tin dùng packet92/-25 hiện có, không sửa client/SQL schema.

TDD RED trên JAR cũ và combat cũ trước hooks; GREEN policy/combat, BlackGoku thật và quái fixture, giáp/khiên/miss/immunity/overkill, tên chiêu thay đổi/nested/reflection, concurrency rate, exact packet payloads, wallet/chat giữ nguyên. Build656source, 9 nhóm tests trên classes và cùng9 trên candidate: HighDamageNoticeTest/CombatTest, BossNoticeWire/Policy/Tracker, HighHpSkill, GiftCodeRewardDelta, LiveSyncGiftClaim, LiveSyncExpNotice. Review spec/code/ops không blocker.

Triển khai bằng bảo trì lưu dữ liệu, giữ restart helper đến hết thao tác thay JAR, SQL snapshot và JAR backup timestamp/hash. Server khởi động PID 6712 listener14445. Chỉ SRC/20.jar tại thư mục chạy.

- JAR SHA-256: `12218be2a27ebb7e2533589b95bbc05aeaeb5e12bf8adbc91d7fb348cb787ad1`.
- JAR backup: `SRC\jar-backup\20-before-high-damage-20261008-143138.jar`, SHA-256 `08f1308943d49d2bcb1aae7b40f84fa22d96c6db8c0fca09db5ed3c26cfa140d`.
- SQL snapshot: `.analysis\high-damage-deploy-20261008-143138\team2026-before-high-damage.sql`.
- Metadata: `.analysis/high-damage-deployment-latest.json`.

Chưa giao chiến hoặc quan sát bằng client game thật; không gửi broadcast thử vào server đang chạy. Khi rollback dùng bảo trì lưu dữ liệu, backup JAR hiện tại rồi khôi phục backup trên; giữ dữ liệu người chơi mới nhất.
