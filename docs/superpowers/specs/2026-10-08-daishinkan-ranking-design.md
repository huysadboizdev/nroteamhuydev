# Daishinkan tại làng và bảng top

Người dùng duyệt triển khai ngày 2026-10-08 sau khi xem thiết kế trong hội thoại.

- Bỏ Bò Mộng (17) khỏi nhà 21, 22, 23; giữ các nơi khác.
- Thêm Daishinkan tại làng Aru (0), Mori (7), Kakarot (14), một NPC mỗi làng, tránh NPC/cổng hiện có.
- Menu: Top sức mạnh, Top nhiệm vụ, Đóng.
- Bảng sức mạnh dùng số viết tắt Việt Nam (59,4 tỷ; 850 tr; 120 nghìn), không chuyển long sang int.
- Bảng nhiệm vụ chỉ có số nhiệm vụ; thứ tự giảm dần theo nhiệm vụ, bước, tiến độ, ID làm khóa ổn định.
- Top 100 tài khoản người chơi; dùng khung bảng xếp hạng hiện tại, không gửi sai số hàng khi thiếu người chơi.
- Kiểm tra dữ liệu hình thật Daishinkan, không dùng hình Whis và đổi tên giả. Nếu không xác định được tài nguyên, báo rõ phần còn thiếu.
- Bảng top cập nhật từ dữ liệu đã lưu, cache tối đa 60 giây, không tải toàn bộ Player chỉ để dựng bảng.
- Test ngưỡng định dạng, thứ tự, dữ liệu JSON lỗi, packet và vị trí NPC. Build và rà soát trước triển khai.
- Triển khai bảo trì lưu dữ liệu, backup DB và JAR theo AGENTS.md; kiểm tra SHA-256 và khởi động lại.
