# Tương thích client chế tạo Thiên Sứ — 2026-10-07

Kiểm tra chỉ đọc hai client Game1/Game2 tại `C:/Users/Admin/Downloads/PRJ_2Tab_550K`. Luồng mới dùng giao thức hiện tại, không cần bổ sung resource hoặc sửa client theo kết quả đọc source.

- `Controller.cs` nhận `-81/action0` để hiện hướng dẫn từ server và `action1` để cập nhật các ô nguyên liệu trong túi. `Service.cs` gửi danh sách chỉ số ô túi; `Panel.cs` không giới hạn cố định số nguyên liệu. Bốn nhóm nguyên liệu trong túi phù hợp; món Hủy Diệt đang mặc do server kiểm tra và tiêu riêng.
- Menu NPC nhận nội dung và lựa chọn từ server; xác nhận ở Whis dùng menu515. Server cần xử lý cả xác nhận và hủy lượt.
- `Controller.cs` xử lý `-37` bằng cách tạo lại danh sách đồ trên người và đặt lại phần ngoại hình mặc định trước khi đọc. Ô rỗng `-1` xóa được món Hủy Diệt đã tiêu. `InventoryService.sendItemBody` đồng bộ đồ trên người và ngoại hình.
- Dòng option được cấp phát và hiển thị theo danh sách động; các ID47/22/0/23/14/50/77/103/94/5 hỗ trợ giá trị mới. HP/KI ở22/23 dùng đơn vị nghìn; option21 dùng đơn vị tỷ với số long. Giữ yêu cầu80tỷ và giao thức IOP32 hiện có.
- SQL có đủ15template1048–1062. Tất cả icon10165–10169/10175–10179/10185–10189 có dữ liệu tại `SRC/data/icon` cho x1–x4; icon chưa cache được yêu cầu qua đường resource hiện tại.
- Cần sửa hướng dẫn cũ trong `CombineService` đang nói chọn cả công thức thường và VIP, cùng mức0–15%. Nội dung mới phải ghi một công thức,999mảnh,hai loại đá,50hồng ngọc, mất món Hủy Diệt tương ứng kể cả thất bại theo tỷ lệ, chỉ số chính+1–30% và mỗi dòng phụ1–7%.

Giới hạn: đây là kiểm tra source/giao thức/resource. Chưa thao tác menu, xem tooltip hoặc kiểm tra ngoại hình trực tiếp trong Unity; phiên công cụ không có điều khiển desktop/Unity. Không dùng tài sản người chơi thật để thử chế tạo.
