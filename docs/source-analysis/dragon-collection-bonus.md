# Bonus bộ 7 Pet Rồng Nhí vĩnh viễn

Đã triển khai ngày 2026-10-04T10:19:50.029441+07:00 (UTC+7).

- Phải có đủ 7 ID 1765–1771 khác nhau trong hành trang + rương + đồ đang đeo của nhân vật chính.
- Chỉ tính item quantity > 0, template và options hợp lệ; mọi option 93 kể cả param 0 đều loại trừ.
- Cộng một lần 10% HP, KI, Sức Đánh sau phần trăm trang bị; không cộng dồn khi tính lại hoặc đăng nhập.
- Nhận/mất loại cuối cùng được gửi lại chỉ số; chuyển rương giữ bonus; mất bộ clamp HP/KI về max mới, nhận bộ không hồi máu/KI.
- Không dùng tiến độ danh hiệu; không ghi bonus vào chỉ số gốc hay database player; không cấp riêng cho đệ tử/boss/bot/newPet.
- Đã đổi đúng 7 description trong database và hai dump, giữ nguyên mọi trường khác và dragon_pet_config; catalog 12 → 13.

## Xác minh

32/32 nhóm kiểm thử PASS trên JAR candidate và JAR cài: collection eligibility/stats/refresh, resource equip/login 14/14, dragon stats catalog/effects/combat/loot/egg, VIP 1–4, SKH effects 8 mode, hợp thể 5 mode, chuyển rương 5 mode, inventory capacity. Test collection đã thất bại với bản cũ trước khi triển khai.

Hai lượt review yêu cầu và chất lượng APPROVED. Server đã bảo trì lưu dữ liệu, xác nhận tiến trình dừng và cổng đóng trước thay JAR. Sau khởi động, PID 19668 nghe cổng 14445; runtime xác nhận catalog 13, đúng cả 7 mô tả và tải class bonus.

Source Game1/Game2 đưa description vào popup và splitFontArray theo chiều rộng 170px, hỗ trợ xuống dòng. **Chưa kiểm thử bằng thao tác client thực tế**: popup màn hình hẹp, relog tải cache và nhận/mất pet cuối cùng trong game. Log khởi động có cảnh báo map tile không có/không hợp lệ ở 78, 95, 101, 121, 125, 130, 134; không thuộc thay đổi lần này.

## Backup và rollback

- JAR cũ: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-dragon-collection-20261004-101803.jar`
- SHA-256 JAR cũ: `cb52de0b6b8fdea0f7a721f4c2208bf7ca258c9f7564d47865c61e88db8b26ab`
- SHA-256 JAR mới: `a88ca08e3f934ce931b9c57baf225f00d85835df4ba5a7e46bc4cb3b9a8e9029`
- Snapshot DB: `.analysis/dragon-collection-plan/database-deploy-before.json`
- SQL rollback: `.analysis/dragon-collection-plan/rollback.sql`
- Backup dump và metadata: `.analysis/dragon-collection-plan/sql-file-changes.json`
- Test log: `.analysis/dragon-collection-plan/jar-tests.log`
- Hồ sơ triển khai: `.analysis/dragon-collection-plan/deployment.json` và JSON cùng tên cạnh JAR backup.

Nếu rollback: bảo trì lưu dữ liệu và xác nhận server dừng/cổng đóng; backup riêng bản JAR hiện tại theo AGENTS.md trước khi phục hồi JAR cũ; áp dụng rollback.sql, phục hồi hai dump từ backup tương ứng, xác minh SHA-256 rồi khởi động. Không cần sửa inventory, chỉ số gốc hoặc cấu hình pet. Quy tắc backup JAR tiếp tục lưu trong AGENTS.md.

Trong SRC chỉ còn JAR ứng dụng đang dùng là 20.jar; không còn JAR ứng dụng dư trong SRC/.analysis. runtime-probe.jar và maintenance-agent.jar là công cụ còn cần dùng.
