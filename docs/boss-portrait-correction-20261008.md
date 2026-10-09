# Sửa đúng ngoại hình boss và xác nhận lưu cấu hình

Kiểm tra trước chỉ xác minh PNG tải được, chưa phát hiện sai nhân vật hoặc PNG hoàn toàn trong suốt. Nguyên nhân: ưu tiên head_avatar thay cho frame part của ngoại hình BossData; boss có đầu rỗng cần dùng frame thân/chân.

Generator hiện chọn frame nhìn thấy được từ đầu thật, rồi thân/chân của đúng outfit. Không dùng head_avatar làm nguồn ưu tiên. Giữ ảnh từng phase và ngoại hình đại diện cho boss sao chép người chơi.

Các trường hợp hồi quy: Ăn Trộm 2254, Ở Dơ 3995, Broly 2893, Super Broly 2924, Rồng Nhí 15070, Virut 7146. Mặt Trời 13489 đúng frame game nên giữ nguyên.

Kiểm chứng:
- PHP resource regression: thất bại trước sửa tại Ăn Trộm, thành công sau sửa.
- Python: 144 boss, mọi phase có pixel không trong suốt; SHA-256 từng PNG web bằng resource game.
- Chrome trang admin thật: 144 boss, 113 ảnh duy nhất tải được, không lỗi JavaScript; đối chiếu các icon bị báo sai và chụp nhóm Broly, mini, Rồng Nhí.
- Phiên admin kiểm thử được xóa sau kiểm tra.

Cấu hình lưu lâu dài ở boss_admin_config / boss_live_override; BossLiveReload.loadStartup được Manager gọi trước khởi tạo boss. applyAtSpawn nạp override khi boss xuất hiện. alive và next quyết định thời điểm áp dụng, không quyết định thời hạn lưu. Sửa/reset tiếp theo thay đổi cấu hình. Không phải sửa cứng mã nguồn Java.

Đọc SQL hiện tại xác nhận Android 13/14/15 giữ HP 150/160/170 triệu, Dame 40.000, mode next, revision 10/11/12. Lần sửa icon này không thay đổi cấu hình boss, JAR hay khởi động lại server; xác nhận luồng khởi động bằng mã nguồn, không thử restart server đang phục vụ.
