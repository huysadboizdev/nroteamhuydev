# Sửa Boss Control Panel — 08/10/2026

Đối chiếu cuối lúc 20:52:35 (UTC+7).

## Đã sửa

- Khôi phục server bằng đúng JAR đã kiểm chứng SHA-256 `92d9da2cb63a37bd8f0838ba72f245139e07fd6187b58b97cb5428f62d36bd81`. Một JVM PID 11188 lắng nghe 14445; launcher tách khỏi phiên tool, task chạy một lần đã xóa. Không thay JAR.
- Thống nhất ID admin dùng trong queue, audit và kiểm tra request retry. `admin_actor_id()` dùng danh tính từ auth guard hoặc session; thiếu ID thì từ chối trước khi lưu. Đã tái hiện audit có actor nhưng queue actor=0 khi chỉ có session; regression test nay PASS.
- Sửa test database eval helper hai lần: bản test chỉ nạp mỗi helper một lần và không reconnect database game. Database thử được xóa trong finally.
- Thông báo sau khi lưu ghi rõ đang chờ ACK, không khẳng định game đã áp dụng.
- Thêm endpoint GET `admin/api/boss_status.php`, kiểm tra quyền admin, no-store, trả runtime/ACK/tuổi heartbeat tính bằng giờ database; nhả session lock sau xác thực.
- Trang boss tự cập nhật heartbeat/ACK mỗi 5 giây. Heartbeat thiếu, cũ hoặc ở tương lai không được coi là đang kết nối. Việc refresh giữ nguyên modal và dữ liệu nháp.

## Xác minh thực tế

- Server có 72 nhóm boss đang được cập nhật; heartbeat cuối 20:52:34, tuổi 1 giây.
- Hai lệnh cũ #34/#35 đã được server kiểm tra và từ chối đúng vì actor_account_id=0. Giữ nguyên lịch sử thất bại; không sửa danh tính hoặc giả ACK cho lệnh cũ.
- Lệnh mới #36–#39 có actor 3728, đều completed / Applied RELOAD_BOSS. Khi xử lý chúng, server stage toàn bộ cấu hình hiện tại từ SQL.
- Smoke qua trang web với boss id:-23/phase 0: sức đánh 10000 → 10001 (revision 6), heartbeat xác nhận đã áp dụng; sau đó trả về 10000 (revision 7), heartbeat xác nhận phục hồi; reset tạo revision 8. PID giữ nguyên 11188.
- Cấu hình thử cuối cùng: hp/damage/defense/critical/cap đều NULL, drop_mode=default, apply_mode=next. Tombstone mặc định được giữ lại cùng audit; không để lại override chỉ số hoặc drop.
- Runtime id:-23 có applied_revision=7 sau reset revision 8 vì reset mặc định dùng chế độ next (áp dụng vào lần xuất hiện tiếp theo); chỉ số hiện tại đã được phục hồi ngay ở revision 7.

## Kiểm tra tự động

- PHP lint tất cả file PHP sửa/thêm: PASS.
- Validation, DB save/reset/rollback/retry/stale revision/queue failure/admin identity/heartbeat: PASS trên DB riêng.
- Queue/catalog contract, 16 API contract kiểm tra authentication/method/CSRF/allowlist/queued: PASS.
- JS syntax và UI contract: PASS.
- Chrome authenticated: catalog 144, lọc/gom mini boss, mở modal/thêm drop: PASS.
- Chrome kiểm tra refresh không reload, giữ draft, mô phỏng mất heartbeat rồi kết nối lại: PASS; không có uncaught JS exceptions.
- Status API: admin HTTP 200, fresh=true; không đăng nhập HTTP 403.
- BossAdminConfigTest/DropServiceTest/HeartbeatTest trên JAR đang chạy: PASS.

## Phạm vi xác minh

Live stats và ACK được thử trên server thật. Drop replacement được kiểm chứng bằng Java tests; phiên này không tổ chức giết boss trong client game. Log startup vẫn có các cảnh báo map thiếu tile vốn có (78,95,101,121,125,130,134), nằm ngoài thay đổi web/queue lần này.

Artifacts: `.analysis/boss-panel-live-smoke.json`, `.analysis/boss-panel-repair-browser.json`, `.analysis/boss-panel-server-start.json`. Session admin thử được xóa khi kết thúc.
