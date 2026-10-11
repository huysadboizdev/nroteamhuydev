# Cải trang Tây Du ngẫu nhiên

Theo yêu cầu triển khai: áp dụng cả 6 cải trang đổi bằng đào cho sư phụ và đệ. Mỗi lần đổi random độc lập sức đánh 1–30%, giảm sát thương 1–50%, HP/KI/TNSM/vàng/tốc độ chạy 1–100%; chống lạnh, nhãn sự kiện 2026. Hạn dùng random 3–7 ngày theo câu trả lời người dùng. Giữ giá và thanh toán nguyên tử; chỉ cập nhật đồ đổi mới, đồ đã cấp giữ nguyên.

- Kiểm thử trước: 100 lần đổi mỗi loại, giới hạn và độ biến thiên, hạn dùng, không trừ đào khi lỗi.
- Sửa service tạo option, bỏ percent cố định và cập nhật toàn bộ hội thoại NPC.
- Biên dịch toàn bộ Java và kiểm thử trên classes lẫn JAR ứng viên.
- Bảo trì có lưu dữ liệu, backup JAR và hash, cài JAR đã kiểm chứng, khởi động và kiểm tra runtime.
- Export SQL đầy đủ vào SRC/sql sau cập nhật, kiểm tra restore.
