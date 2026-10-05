# Auto sử dụng: dừng an toàn

Auto ở cả Game1 và Game2 kiểm tra thời gian còn lại cộng với toàn bộ thời gian của lần dùng tiếp. Nếu vượt 500 phút thì dừng trước khi gửi yêu cầu. Áp dụng cả khoảng cách 0 giây và các khoảng cách khác. Không tự bật lại sau khi dừng.

Bùa Santa 1628 mỗi lần cộng 30 phút: từ 0 dùng tối đa 16 lần để đạt 480 phút; lần tiếp sẽ vượt 500 nên không dùng, không trừ item. Các buff 10 phút có thể đạt 500 phút. Server kiểm tra lại giới hạn trong khóa player trước khi thay thời gian hoặc trừ item.

Hộp quà dừng khi không đủ số ô trống theo yêu cầu server, gồm các loại cần 1, 2 hoặc 5 ô. Rương gỗ có kiểm tra số ô riêng theo cấp ở server. Server khóa luồng nhận thưởng, kiểm tra chỗ trống, dùng đúng chồng item đã chọn và chỉ trừ hộp/chìa khóa sau khi thêm thưởng thành công. Hộp quà thiếu nhi không còn chấp nhận hành trang đầy. Rương gỗ giữ lại khi phần thưởng vàng bị chặn; nếu nhận nhiều thưởng bị lỗi giữa chừng, khôi phục số lượng/chỉ số và giữ nguyên đối tượng item, hạn dùng, thông tin giao dịch của các đồ cũ.

Timer nước mía cấp 3 được gửi theo trạng thái nước mía cấp 3, thay cho điều kiện nhầm cấp 2.

Kiểm tra client gồm 42 ca auto và 4 ca nhập chat thực trên PC/mobile ở hai tab, cùng biên dịch toàn bộ mã Unity. Kiểm tra server gồm 132 ca trên nguồn và lặp lại với JAR đóng gói. Không dùng item trên tài khoản online để chạy các ca kiểm thử này.

Metadata triển khai, hash JAR và kiểm tra server thực tế: `.analysis/auto-use-safety/deployment.json`, `.analysis/auto-use-safety/verification.json`. JAR cũ được giữ trong `SRC/jar-backup` trước khi cài bản mới.
