# Sửa phần hiển thị còn sót sau khi tắt sự kiện

Ảnh người chơi cho thấy Dưa hấu ở nhà và ba menu Top tại Chi Chi còn hiện dù Hùng Vương/Hè đã tắt. SQL xác nhận cả desired/applied đều false; lỗi không nằm ở lệnh web.

Nguyên nhân: NPC đặc biệt Dưa hấu `51` không nằm trong danh sách ẩn/hiện của runtime; `sendDuaHauEgg()` tiếp tục gửi countdown khi đổi map và phát cho cả người cùng map. Chi Chi tạo menu Top cố định, không đọc policy.

Đã sửa bốn source: `EventRuntimeService`, `DuaHauEgg`, `DuaHau`, `ChiChi`. Runtime gửi packet `-73` ẩn/hiện NPC51 khi đổi trạng thái và khi vào map; countdown `-122` chỉ gửi cho chủ cây khi sự kiện bật. Menu/thu hoạch có admission và recheck policy/pending. Chi Chi ẩn ba mục Top khi Hè tắt, giữ Cửa hàng/Bỏ đồ có hạn/Đóng; lưu layout bằng index menu riêng để không lệch thao tác. Các submenu cũ không truy cập được sau OFF.

Đối chiếu client Game1/Game2: packet -73 cập nhật `Npc.isHide`; render bỏ qua NPC đã ẩn. Không cần sửa client hay tạo resource mới. Chưa điều khiển Unity để xác nhận ảnh bằng mắt.

Kiểm thử mới `EventVisibilityTest` tái hiện lỗi trên JAR cũ (RED), qua trên source và JAR mới (GREEN). Toàn bộ 54 nhóm/mode Java và hai suite SQL độc lập qua. Scope đối chiếu metadata trước sửa chỉ gồm bốn file nêu trên.

Đã bảo trì có lưu dữ liệu, backup JAR/SQL và đối chiếu SHA trước thay bản. Server mới PID21644 mở cổng14445. Probe chỉ đọc xác nhận Hùng Vương/Hè OFF, không handle boss mùa, không pending cleanup/logout. Hai job dọn Hùng Vương/Hè đều completed, mỗi job đã xử lý hai người chơi. Quà giữ theo catalog cũ.

Metadata và bản backup cụ thể tại `.analysis/event-control-deployment-latest.json`; trạng thái trước sửa lưu ở `.analysis/event-control-before-visibility-fix.json`. Không restore SQL snapshot đè lên dữ liệu sau thu hồi; dùng backup/receipt khi cần kiểm tra hoặc phục hồi từng thay đổi.
