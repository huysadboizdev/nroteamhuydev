# Dọn đồ có hạn trong hành trang

Phạm vi đã được người dùng duyệt: NPC Chi Chi có nút bỏ toàn bộ đồ có option 93 trong hành trang, cả 0 ngày và còn hạn. Không gỡ hạn để thành vĩnh viễn. Không xóa đồ đang mặc, đồ đệ tử hoặc đồ trong rương nhà bằng chức năng này.

Luồng được người dùng cập nhật: mở menu → hỏi “Bạn có muốn bỏ đồ có hạn sử dụng trong hành trang không?” → hai nút Có / Không. Không hiển thị danh sách hay số lượng. Xóa cả chồng ở ô được chụp lúc mở xác nhận. Server vẫn lưu snapshot kèm đối tượng, vị trí, số lượng, template, createTime và options; thay đổi danh sách hoặc dữ liệu sẽ hủy lần xác nhận. Hết hiệu lực sau 120 giây; mỗi xác nhận chỉ dùng một lần. Không dọn khi đang giao dịch, thoát game, dữ liệu chưa tải đủ hoặc có nghiệp vụ đang chờ đối soát.

Xóa dưới khóa player. Lưu toàn bộ nhân vật bằng luồng PlayerDAO hiện có để không tạo bản sao từ việc chuyển đồ giữa túi/rương/trang bị chưa lưu. Nếu lưu chưa thành công, trả lại đúng đối tượng và vị trí cũ. Chỉ báo thành công sau khi bản ghi nhân vật đã được cập nhật. Ghi log player ID, slot, template, quantity, createTime, options và thời gian.

Hết hạn trang bị là luồng riêng: ngày lịch Việt Nam UTC+7, option 93 bằng 0 còn dùng hết ngày; âm sau khi trừ ngày mới thì gỡ bỏ. Kiểm tra khi đăng nhập và mỗi giây khi online, kể cả đứng trong nhà, cho sư phụ và đệ tử. Cập nhật chỉ số, hình dạng và companion liên quan khi gỡ đồ. Không quét hành trang/rương nhà trong vòng kiểm tra trang bị này; giữ nguyên cơ chế hết hạn khi tải hành trang/rương nhà đang có.

Không thay client, SQL hay tài nguyên hình ảnh. Build và kiểm thử source/JAR, backup JAR cũ trước khi thay, SHA-256 và metadata, bảo trì có lưu dữ liệu, khởi động và kiểm tra cổng server. Workspace không có Git nên không commit.
