# Nhập giftcode tại ba NPC ở nhà

Theo yêu cầu thêm chức năng trực tiếp, bổ sung mục “Nhập giftcode” vào menu Gohan, Moori và Paragus, sau nhận đệ tử và trước Đóng. Các nút nhận ngọc và đệ tử giữ vị trí cũ; Đóng chuyển sang vị trí 3.

Chọn mục mới gọi `Input.gI().createFormGiftCode(player)`, dùng form và `Input.GIFT_CODE` hiện có. Khi gửi code, Input chuyển vào GiftCodeService hiện có; không đổi quy tắc code, phần thưởng, schema DB hoặc luồng Santa.

Sửa lớp cha OngGohan là đủ cho cả ba NPC kế thừa. Kiểm tra đúng map/menu được áp dụng cho mục mới. Test menu, packet form và chuyển code tới service; kiểm tra Đóng/ngoài nhà/sai menu không mở form. Build và test JAR, bảo trì lưu dữ liệu trước khi thay bản đang chạy, backup JAR cũ có timestamp và SHA-256, khởi động lại và kiểm tra cổng/log.
