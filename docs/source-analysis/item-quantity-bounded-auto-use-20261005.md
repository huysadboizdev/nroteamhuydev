# Số lượng vật phẩm và Auto sử dụng — 05/10/2026

Đã sửa Game1/Game2 của client PRJ_2Tab_550K. Tooltip chung chỉ hiện “Số lượng” nếu quantity > 1; giáp cấp 5 dùng cùng quy tắc. Không coi dòng số lượng là một item option và không xóa option chỉ số trong SQL.

Nhấn Auto sử dụng: nhập số lần/cái cần dùng từ 1 đến số vật phẩm cùng ID và cùng option đang có, tiếp theo nhập khoảng nghỉ giữa mỗi lần, đơn vị giây. 0 giây là nhanh nhất khi server đã xác nhận lượt trước; đậu giữ tối thiểu 1 giây theo cơ chế hiện có. Auto chỉ bật khi đã nhập đủ hai bước. Hủy hoặc nhập sai không tự kích hoạt. Tổng vật phẩm và giới hạn lượt dùng dùng long, không tràn khi nhiều stack cộng lại vượt int.

Mỗi request là một lần dùng; vật phẩm thông thường tiêu hao một cái. Auto không gửi một yêu cầu dùng hàng loạt. Chỉ giảm số lượt còn lại khi túi được server cập nhật và xác nhận tiêu hao, tự dừng khi đủ số lần đã chọn. Giữ timeout khi không có kết quả, một request đang chờ toàn hệ thống, cooldown đậu, theo ô túi mới, kiểm tra cùng option, giới hạn buff và chỗ trống nhận thưởng. Kiểm tra lại số vật phẩm trước khi bắt đầu. Màn nhập thứ hai mở ở frame kế tiếp để callback bàn phím mobile không đóng mất nó.

Kiểm thử: 74 kiểm tra scheduler trên code AutoItem thật với fixture cả Game1/Game2, gồm 1/2 lượt, chưa xác nhận, thời gian đúng mốc, nhập sai/hủy, túi thay đổi, chuyển bước mobile, số lượng lớn và hồi quy. Unity biên dịch cả hai namespace thành công, 14 kiểm tra tooltip thật với 0/1/2/50000/int.MaxValue và giáp cấp 5.

Chưa kiểm tra gameplay đăng nhập trực tiếp hoặc đóng gói bản executable/APK. Các script đã cập nhật trong project client; không thay SQL/JAR hoặc bảo trì server vì thay đổi này chỉ ở client.

Backup bốn file: `.analysis/bounded-auto-20261005/*before`; hash và đường dẫn gốc trong `client-manifest.json`. Diff `client-changes.diff`, RED `red.log`, GREEN `test-results.json`, Unity `unity-result.txt` và `unity.log`. Khôi phục bằng sao chép bản trước về đúng đường dẫn đã ghi, đối chiếu SHA-256.
