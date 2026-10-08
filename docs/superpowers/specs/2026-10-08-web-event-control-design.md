# Thiết kế control sự kiện trên web

Trạng thái: đề xuất để người dùng xem; chưa triển khai.

Yêu cầu đã chốt: bật sự kiện từ web và xuất hiện trong game không restart khi vận hành; tắt thì dừng boss, nguồn drop và đồ dùng sự kiện. **Chỉ thu hồi nguyên liệu/vé/đồ dùng sự kiện; giữ cải trang, pet và quà vĩnh viễn đã nhận.** Đồ cần thu hồi trong rương được khóa ngay, xóa sau kỳ bảo trì tiếp theo nếu sự kiện đang tắt.

Phương án đề xuất: SQL lưu trạng thái mong muốn và phiên sự kiện, dùng queue/receipt của LiveSync để server xử lý. Server có catalog chung về item/boss/NPC/shop/drop, snapshot trạng thái đã áp dụng, cơ chế thu hồi online và job dọn offline/rương sau bảo trì. Web hiển thị riêng yêu cầu và kết quả thực tế. Các sự kiện chưa có nội dung hoặc tài nguyên không đủ bị khóa bật.

Các phương án khác đã cân nhắc: web đổi cờ SQL đơn giản không xử lý RAM/nhánh rải rác; web gửi lệnh trực tiếp không có trạng thái bền vững khó phục hồi sau crash. Queue cộng SQL version được chọn vì tận dụng cơ chế sẵn có và đối chiếu được kết quả.

Không thể bảo đảm tuyệt đối không lỗi chỉ bằng thiết kế. Điều kiện nghiệm thu cụ thể, thử lỗi, quy trình backup/khôi phục và các giới hạn dữ liệu cũ nằm trong [kế hoạch chi tiết](../plans/2026-10-08-web-event-control.md).
