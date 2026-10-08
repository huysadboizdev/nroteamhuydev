# Sửa nhân vật chết bất thường – 08/10/2026

## Nguyên nhân và phạm vi

- Mặt Trời đặt bỏng nhiệt 30 giây, có 80% xác suất đánh sát thương bằng toàn bộ HP hiện tại. Đây là cơ chế giữ lại theo lựa chọn của người dùng.
- Bộ đếm cũ chỉ lưu ID, tra nhân vật qua `Client` toàn server; không kiểm tra map/khu, lần hồi sinh hay phiên đăng nhập. Vì vậy hiệu ứng cũ có thể tác động sau khi nhân vật đã rời boss. Virut có cùng cách tra ID và được sửa cùng phạm vi.
- Gọi `injured(null, ...)` có thể đưa HP về 0 nhưng bỏ qua nhánh xử lý chết của `Player.injured`. Nay sát thương có nguồn boss và gửi lại HP.
- Biến khỉ nhân đôi HP bằng `int`: 1.073.741.824 HP nhân hai thành số âm, `setHp` đưa về 0. Huýt sáo, trị thương/hồi KI cũng có phép nhân/cộng bằng `int` dễ tràn ở chỉ số cao.

## Thay đổi

- Hiệu ứng giữ đúng đối tượng nhân vật, khu gốc, phiên vào khu của cả nhân vật/boss và mốc hồi sinh.
- Mỗi lần `Zone.removePlayer` tăng số phiên rời khu dưới khóa nhân vật. Rời rồi quay lại cùng khu cũng làm hiệu ứng cũ mất hiệu lực.
- Kiểm tra hiệu ứng và trừ HP dưới cùng khóa nhân vật với thao tác rời khu; hủy khi khác khu, hồi sinh, đăng xuất, boss chết hoặc rời khu. Không tra ID sang nhân vật của phiên đăng nhập mới.
- Virut xóa bộ đếm khi vào/rời map. Không đổi lịch kích hoạt hiện có của Virut trong lần sửa này.
- Giữ xác suất kết liễu 80%, thời gian 30 giây và phạm vi gây bỏng ban đầu của Mặt Trời.
- Các phép tính HP/KI liên quan dùng `long` trước khi nhân/cộng; giữ giới hạn chỉ số hiện có.

## Kiểm chứng

- Test trên JAR cũ: nhân vật khác khu vẫn bị Mặt Trời đánh 31 lần/40 lượt hiệu ứng hết hạn; test thất bại đúng lỗi.
- Test biến khỉ trên JAR cũ: 1.073.741.824 HP thành 0; test thất bại đúng lỗi.
- `MiniBossDelayedDeathTest`: Mặt Trời và Virut, đổi khu, rời/quay lại, hồi sinh, đăng xuất, boss chết và kết liễu hợp lệ trong khu.
- `HighHpSkillTest`: gọi kỹ năng biến khỉ thật với HP 100, 1.073.741.824, 1,5 tỷ, 2 tỷ.
- Biên dịch toàn bộ nguồn, chạy bộ hồi quy và kiểm thử SQL trong schema độc lập; chạy lại hồi quy trên JAR đóng gói trước khi cài.
- Chưa chạy kiểm thử hình ảnh/điều khiển trực tiếp trong Unity. Các kiểm thử này xác minh xử lý server; không khẳng định mọi nguyên nhân chết khác đều đã được loại bỏ.

## Triển khai và khôi phục

- `.analysis/death-fix-deployment-latest.json` lưu hash nguồn/JAR, backup, snapshot SQL và kết quả kiểm thử.
- Dùng bảo trì có lưu dữ liệu, xác nhận server dừng rồi chuyển JAR cũ vào `SRC/jar-backup` với tên thời gian riêng, đối chiếu SHA-256 trước khi cài.
- Không có migration SQL cho bản sửa này. Khi khôi phục, bảo trì lưu dữ liệu rồi phục hồi JAR backup; không phục hồi snapshot SQL cũ đè tiến trình người chơi.
