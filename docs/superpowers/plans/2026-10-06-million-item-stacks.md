# Cộng dồn vật phẩm tới một triệu

Yêu cầu đã được người dùng giao triển khai: tất cả vật phẩm có thể cộng dồn nhận vào hành trang được tích tối đa 1.000.000 mỗi ô, không dừng ở 99.

1. Kiểm tra luồng Zone.pickItem → InventoryService.addItemBag → addItemList và giao thức số lượng client. Giữ nguyên quy tắc loại vật phẩm, option và hạn dùng tương thích.
2. Viết kiểm thử thất bại trước: vượt 99 khi túi kín, đúng một triệu, vượt giới hạn tách ô, thiếu chỗ không nhận một phần, không gộp trang bị hoặc option khác, không cắt số lượng cũ vượt giới hạn.
3. Đặt MAX_STACK_QUANTITY=1.000.000 cho vật phẩm cộng dồn. Kiểm tra sức chứa trước; gộp ô tương thích rồi chia phần dư vào các ô trống. Giữ hành vi vật phẩm không cộng dồn.
4. Kiểm thử đường nhặt đồ thực, rương và mua đồ dùng chung hàm nhận; chạy bộ hồi quy server và dựng JAR ứng viên. Client đang nhận số lượng int, không cần đổi giao thức hay SQL.
5. Bảo trì lưu dữ liệu; xác nhận JVM dừng, lưu SQL và JAR cũ có SHA-256; cài ứng viên, chạy lại và đối chiếu lớp runtime với JAR đã kiểm thử.

Stack cũ lớn hơn một triệu được giữ nguyên, không bị cắt hay mất vật phẩm. Tiền tệ và vật phẩm cộng dồn bằng option giữ cơ chế riêng.
