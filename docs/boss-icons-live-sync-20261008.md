# Boss resource icons và cập nhật Live — 08/10/2026

Kiểm chứng cuối: 21:18:15 UTC+7. Người dùng chọn cả áp dụng ngay trong game và web cập nhật theo game.

## Resource

- 144 mục boss có portrait resource, 111 PNG riêng; có portrait theo phase khi BossData cung cấp nhiều outfit.
- Ánh xạ head part → head_avatar.avatar_id; nếu không có avatar riêng thì dùng frame thật của part loại head. Copy PNG x4, fallback x2/x1; SHA-256 của từng bản copy khớp nguồn.
- Sửa các head của nhóm HuntingTrio trên web theo HuntingTrioCatalog. Nguyệt Thần/Nhật Thần dùng part loại head trong outfit để chọn portrait; không sửa outfit game trong thay đổi này. NhanBan/Rival phụ thuộc người chơi nên dùng portrait đại diện, đánh dấu representative trong metadata.
- Baby, Black Goku, Ăn Trộm, Mặt Trời có 4 URL/ảnh riêng, không rơi về favicon.
- Browser tải được toàn bộ 111 URL ảnh (naturalWidth > 0). Modal đổi ảnh theo phase.

## Áp dụng và đồng bộ

- Mặc định khi mở một lần sửa mới: alive. Vẫn cho chọn next để chờ lần xuất hiện tiếp theo. Các cấu hình next đã lưu trước đây giữ nguyên lựa chọn.
- Reset tạo tombstone nullable với alive để boss đang sống phục hồi ngay về defaults, giữ tỷ lệ HP còn lại, không hồi sinh boss chết.
- Server phát packet hiện có -30/subcommand 9 chứa boss ID, HP hiện tại và HP tối đa sau thay đổi HP Live. Client Game1/Game2 đã có handler này; không yêu cầu protocol mới.
- Status API trả cả cấu hình hiện tại, runtime và ACK. Bảng tự refresh 5 giây, ưu tiên HP tối đa/Dame thực tế từ heartbeat theo phase; số gốc hiển thị riêng. Với nhiều boss cùng loại, đây là mẫu một boss trong heartbeat, không phải tổng HP cả nhóm.
- Modal đang sửa giữ nguyên bản nháp và expected revision; không tự ghi đè dữ liệu người quản trị đang nhập.

## Bằng chứng

- Regression packet: thấy test fail trên JAR cũ vì không gửi packet; PASS sau sửa. Kiểm tra packet chính xác, tỷ lệ HP, gửi một lần, next, dead, detached và HP không đổi.
- Full source compile, test compile, config/drop/heartbeat/arithmetic/wire tests, catalog 5/5 và schema tests: PASS. Chỉ 1 class khác JAR cũ: BossAdminConfig.
- PHP lint, validation, isolated DB save/reset/rollback/idempotency/queue failure/identity/freshness/config status, icon/resource tests, queue/catalog/API contracts, JS syntax/unit: PASS.
- Browser Live thật: lệnh #48 revision 15 đổi boss id:-23 HP 25.000.000 → 25.001.000, Dame 10.000 → 10.001; ACK completed, runtime khớp và bảng đổi không reload. Lệnh #49 reset revision 16: HP/Dame phục hồi ngay 25.000.000/10.000; runtime applied_revision=16. Draft modal giữ nguyên; không uncaught JS errors.
- Browser mô phỏng mất heartbeat và kết nối lại: PASS.
- Một server PID 19236, listener 14445; PID không đổi trong smoke. Heartbeat mới, tuổi 2 giây. Các cấu hình admin khác được giữ nguyên. Boss thử chỉ còn defaults nullable/alive.

## Cài đặt

- Bảo trì đã lưu dữ liệu và server dừng trước thay JAR; SQL snapshot và JAR backup có SHA-256.
- Backup: `SRC/jar-backup/20-before-boss-admin-20261008-211313.jar`, SHA-256 `92d9da2cb63a37bd8f0838ba72f245139e07fd6187b58b97cb5428f62d36bd81`.
- Active: `SRC/20.jar`, SHA-256 `abfbf7fd6bb1d3e02e73de7a97e79d23a00c3eb96b805e70b5976e52f7b7b7d9`.
- Metadata: `.analysis/boss-admin-deployment-latest.json`; snapshot `.analysis/boss-admin-deploy-20261008-211313/team2026-before-boss-admin.sql`. SRC chỉ có 20.jar ứng dụng đang dùng; task khởi chạy một lần và session admin thử đã xóa.
- Kiểm tra lại các test trên JAR đang cài: PASS; SHA active/backup và các ảnh copy đều khớp.

Artifacts: `.analysis/boss-icons-live-browser.json`, `.analysis/boss-icons-live-browser.png`, `.analysis/boss-web-resources-manifest.json`.

Giới hạn: đã thử server thật, web thật và bytes của packet dành cho client; chưa quan sát scene bằng client Unity đang đăng nhập.
