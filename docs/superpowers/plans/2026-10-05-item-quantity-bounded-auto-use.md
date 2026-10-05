# Kế hoạch số lượng vật phẩm và Auto sử dụng có giới hạn

Yêu cầu đã chốt: chỉ hiện “Số lượng” khi quantity > 1. Auto hỏi số lần/cái muốn dùng và khoảng nghỉ giữa các lần, đơn vị giây (người dùng chọn khoảng nghỉ, không chọn tổng thời gian chạy).

1. Sao lưu Panel.cs và AutoItem.cs của cả Game1/Game2, lưu hash. Đây là thay đổi client; không cần thay SQL/JAR.
2. Dùng chung điều kiện quantity > 1 cho tooltip thường và tooltip giáp cấp 5. Giữ các option, mô tả, hiệu ứng và chỉ số thực.
3. Auto mở nhập số lần/cái (1 đến tổng số vật phẩm cùng ID và cùng option đang có), sau đó nhập khoảng nghỉ >= 0 giây. Chỉ bật auto sau khi hoàn tất hai bước; hủy không kích hoạt auto. Chuyển sang bước hai ở frame sau để callback bàn phím mobile không đóng mất hộp nhập mới.
4. Lưu số lần còn lại trên từng cấu hình auto; chỉ giảm sau khi server cập nhật túi xác nhận đã tiêu hao. Đủ số lần thì dừng và thông báo. Giữ một request đang chờ, timeout, khoảng nghỉ, cooldown đậu, kiểm tra chỗ trống/giới hạn buff, loại option và theo vị trí túi mới.
5. Kiểm thử RED/GREEN cấu hình hai bước, giới hạn số lần, chưa xác nhận không trừ, hủy, nhập sai, túi thay đổi, callback mobile; chạy lại hồi quy scheduler cả hai namespace. Biên dịch và kiểm tra tooltip bằng Unity trên clone riêng, không đóng editor người dùng.
6. Cập nhật note và manifest. Chứng nhận biên dịch/kiểm thử fixture, không tự nhận đã kiểm tra gameplay đăng nhập trực tiếp.

Đã hoàn tất chỉnh sửa và xác minh: 74 kiểm tra Auto, Unity biên dịch và 14 kiểm tra tooltip đạt. Backup/diff/manifest nằm trong `.analysis/bounded-auto-20261005`. Gameplay đăng nhập trực tiếp và đóng gói executable/APK chưa thực hiện.
