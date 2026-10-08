# Boss mini và capsule hồng ngọc — 06/10/2026

## Hành vi sau sửa

Ăn Trộm (5), Sói Hẹc Quyn (2), Ở Dơ (5), Mặt Trời (20) tự được tạo khi server khởi động. Không cần gọi NPC hoặc chờ khung giờ. Các boss nghỉ 600 giây sau khi rời map rồi xuất hiện lại; chu kỳ cập nhật có thể làm chậm thêm vài giây.

Ăn Trộm, Sói, Ở Dơ chọn một trong 73 map: 0–20, 24–37, 63–77, 79–84, 92–94, 96–100, 102–110. Ăn Trộm và Ở Dơ vào khu 0. Sói chọn khu có tối đa 10 người và không có Sói khác. Mặt Trời chỉ chọn map 0, 5, 7, 14, khu 0.

Ăn Trộm, Ở Dơ, Mặt Trời rời map sau 15 phút nếu chưa bị giết. Sói ở 100–300 giây; sau cho ăn xương rời map trong khoảng 5 giây rồi nghỉ 10 phút.

Mỗi lần giết hợp lệ rơi 1–5 capsule hồng ngọc (ID 722), mỗi vật phẩm số lượng 1. Quyền nhặt thuộc người giết, hoặc chủ của đệ tử giết boss, cho đến khi vật phẩm biến mất theo thời gian hiện có. Không mở quyền nhặt công cộng sau 45 giây. Một vòng đời chỉ thưởng một lần, gồm cả gọi đồng thời. Capsule không phụ thuộc số vàng Ăn Trộm đã lấy.

Sói kiểm tra xương ID 460 còn hạn, số lượng dương trong hành trang và rương của người đánh/chủ đệ tử. Có xương thì chặn sát thương và hướng dẫn dùng xương; xương trong rương cần lấy ra hành trang. Không có xương thì đánh chết được. Cho ăn gần tối đa 200 đơn vị, còn sống, cùng khu và đủ chỗ nhận quà. Cho ăn cũng rơi 1–5 capsule riêng cho người cho ăn; giữ quà xương cũ. Hai người cho ăn đồng thời không thể thưởng lặp hoặc tiêu hao cả hai xương.

## Lỗi đã xử lý

- Các boss cùng loại dùng chung ID mạng; client tìm nhân vật theo ID đầu tiên nên có thể hiện/nhắm sai boss cùng khu. Cấp ID âm riêng cho từng boss, đồng thời đổi kiểm tra Sói trong khu sang loại đối tượng.
- Mặt Trời dùng nhầm ID Virus trước đó; giờ dùng ID riêng. Kiểm tra bỏng mỗi vòng hoạt động, đồng bộ hiển thị và xử lý 30 giây, không kéo dài hiệu ứng khi đánh lại, xóa bộ đếm khi vào/rời map. Giữ xác suất kết liễu 80% của cơ chế hiện có, không tự đổi cân bằng.
- Ở Dơ đặt 600000 giây nghỉ thay vì 600 giây. Sửa về 10 phút, khởi tạo lại timer và hồi HP ngay cả khi không có mục tiêu còn sống.
- Ăn Trộm có thể sinh HP bằng 0; giới hạn 1–100 HP, chặn sát thương không dương và không xử lý choáng sau chết.
- Sói trước đây luôn miễn sát thương và thiếu lời gọi dùng kỹ năng; sửa theo yêu cầu hai cách nhận thưởng. Bỏ chờ 5 giây trong luồng xử lý dùng đồ, kiểm tra chỗ trống trước tiêu hao xương và bỏ ép kiểu boss võ đài có cùng ID.

Kiểm thử bao gồm quyền nhặt, hết thời gian vật phẩm, chủ đệ tử, thưởng lặp/đồng thời, hồi sinh, ID đồng thời, timer bỏng và toàn bộ luồng dùng xương. Không sửa SQL hoặc client trong đợt này.

Metadata triển khai và SHA-256: `.analysis/mini-boss-deployment-latest.json`. Công cụ kiểm tra runtime chỉ đọc: `.analysis/mini-boss-audit/after.txt`.

## Kết quả triển khai

94 lượt kiểm thử PASS trên classes, candidate và JAR đã cài. Runtime PID 40432 mở cổng 14445, đúng 32 boss (5/2/5/20), ID riêng không trùng, thời gian nghỉ tất cả 600 giây và boss có map đều đăng ký trong khu. Hash của cả 7 class thay đổi khớp JAR đã kiểm thử. Chưa quan sát trực tiếp qua client Unity.

Backup JAR cũ: `SRC/jar-backup/20-before-mini-boss-20261006-100111.jar`. SHA-256 JAR mới: `3018aa4887fc72bde55cb042ae4bd19ab6d86a4d4f0feda570bfc529a2208ec5`. Đã bảo trì lưu dữ liệu trước thay JAR, snapshot SQL sau khi server dừng. Vật phẩm capsule trên đất hết hạn khoảng 50 giây theo cơ chế hiện có; giữ riêng chủ đến lúc hết hạn.

Log khởi động vẫn báo thiếu tile các map 78, 95, 101, 121, 125, 130, 134; chúng không thuộc danh sách xuất hiện của bốn loại boss này.
