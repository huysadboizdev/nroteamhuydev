# Kiểm tra Boss Control Panel — 08/10/2026

Thời điểm đối chiếu database: 20:42:55, UTC+7.

## Kết luận

Các chức năng web đã kiểm tra hoạt động. Chưa xác nhận được việc áp dụng thay đổi vào game hiện tại vì server không lắng nghe cổng 14445, heartbeat dừng từ 17:21:44 và hai lệnh RELOAD_BOSS #34, #35 vẫn pending từ 20:16:22.

## Bằng chứng mới

- Trang admin/bosses.php có session admin thử: HTTP 200, available=true, không có message lỗi; catalog 144 loại, khớp catalog của server. Nhóm: 33 nhiệm vụ, 13 giới hạn dame, 98 bình thường.
- Chrome headless: kiểm tra catalog, gom mini boss Ăn Trộm/Mặt Trời, bộ lọc nhiệm vụ/giới hạn dame, mở modal sửa Black Goku và thêm quy tắc rơi đồ đều PASS. Không submit thay đổi vào database game.
- API tìm vật phẩm và option: HTTP 200, success, có kết quả. API live_sync.php?check_id=35: HTTP 200, xác nhận lệnh vẫn pending.
- PHP lint trang/helper, PHP validation, Node syntax và UI contract: PASS.
- Lưu cấu hình, reset, rollback, retry idempotency, từ chối revision cũ, rollback transaction khi queue lỗi: PASS trên database thử riêng, đã xóa database thử.
- Catalog tests: 5/5 PASS.
- BossAdminConfigTest, BossAdminDropServiceTest, BossAdminHeartbeatTest chạy với SRC/20.jar đang dùng: PASS. Compiler có diagnostic AccessDeniedException khi đóng lombok.jar trong sandbox; các test class đã được tạo và chạy được, không ghi nhận compile hoàn toàn sạch.
- Database live: revision 4; một config id:-32/phase 0 với chỉ số null, apply_mode=next; 72 runtime rows nhưng thời gian cập nhật đã cũ.

## Vấn đề ở bộ test hiện có

tests/admin_boss_admin_db_test.php lỗi Cannot redeclare admin_new_request_id(): test eval live_sync_helper.php rồi boss_admin_helper.php require_once lại cùng file. Đây là lỗi cách nạp của test; trang production nạp qua require_once không gặp lỗi này. Đã kiểm tra bằng bản test tạm chỉ nạp helper một lần trước khi chọn database thử. Bản tạm đã xóa; chưa sửa file test gốc.

## Giới hạn và bước tiếp theo

Cần bật server game và đối chiếu ACK của #34/#35, heartbeat mới, rồi kiểm tra chỉ số/drop trong game để xác nhận toàn bộ luồng Live. Phiên kiểm tra này không khởi động server, thay JAR hoặc thay cấu hình boss live. Session admin thử đã được xóa.
