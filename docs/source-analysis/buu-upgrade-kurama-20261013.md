# Bư hợp thể, bộ đồ nâng cấp và pet Cửu Vĩ — 13/10/2026

## Kết quả server

- Đệ Bư (`typePet=1`): cộng 10% HP, KI, sức đánh của đệ khi đóng góp chỉ số hợp thể. Không hợp thể thì không cộng. VIP loại 2–4 vẫn 20%.
- Đủ 5 món đang mặc: áo, quần, găng, giày, nhẫn. Lấy cấp thấp nhất theo option 72, không yêu cầu cùng bộ hoặc nhóm template. +5/+6/+7/+8 tương ứng +1/+2/+3/+4% HP, KI, sức đánh. Trên +8 giữ mức 4%. Thiếu món hoặc dưới +5 không có bonus. Áp dụng cho chủ và đệ; chỉ số mỗi người tính bonus một lần trước khi cộng phần đệ vào hợp thể.
- Hào quang dùng cùng chính sách cấp với bonus. Giữ hiệu ứng +4 cũ, hỗ trợ đệ có 9 ô đồ. Ảnh server đủ x1/x2/x3/x4: +5/+6 có lớp sau, +7/+8 có lớp trước và sau.
- Cửu Vĩ ID 2019 đã có type 27 và part 2129/2130/2131. UseItem thiếu xử lý companion mới, chỉ liệt kê ID cũ. Sửa bằng CompanionCatalog, chỉ triệu hồi sau khi trang bị thành công. Kiểm thử thay thế/tháo pet và từ chối khi thiếu sức mạnh. Consumable type 27 không thuộc catalog giữ đường xử lý cũ.
- Appearance packet -90 thêm byte cấp hiệu ứng ở cuối. Client cũ bỏ qua phần đuôi; client mới đọc khi còn dữ liệu. Hai bộ Game1/Game2 xóa cache ảnh khi đổi cấp để không giữ hào quang cũ.

## Kiểm chứng

Kiểm thử mới thất bại trên JAR cũ đúng triệu chứng trước khi sửa: Bư thiếu 10%, bộ đồ thiếu 1%, dùng Cửu Vĩ không chuyển vào ô 7, gói tin thay đồ thiếu hiệu ứng. Kiểm thử Unity ban đầu xác nhận cache hiệu ứng cũ; bản sửa qua kiểm thử cả Game1/Game2.

17 nhóm kiểm thử qua trên mã biên dịch, JAR ứng viên và JAR đã cài: Bư các kiểu hợp thể/tách thể/tính lại, đồ cấp 4–9/cấp trộn/thiếu món/đệ, pet VIP, companion lifecycle, Cửu Vĩ use/replace/reject/remove, gói tin hào quang, đồ anime, Killis 30%/60%, Ôsin/map165, thời hạn bùa và dungeon.

Đối chiếu JAR: chỉ NPoint, Player, EquipmentUpgradeBonus, Service, UseItem thay đổi; 4.880 mục giữ nguyên byte. Runtime map165 vẫn type 0, không thuộc sự kiện Mabư; tỷ lệ Killis 30%/60%. Runtime Cửu Vĩ type 27, strengthRequirement=0, part đúng.

## Triển khai

Đã bảo trì lưu dữ liệu và dừng server trước khi thay JAR; snapshot database và xác minh SHA-256. Server khởi động lại PID 9032, cổng 14445.

- JAR hiện hành: `SRC/20.jar`
- SHA-256 mới: `b59aeddb7e4c01ba8e8ffa8346611cd0fd73e5417f671b7b6aad864e11ee8d89`
- Backup: `SRC/jar-backup/20-before-buu-upgrade-20261013-201824.jar`
- Metadata: `.analysis/buu-upgrade-deployment-latest.json`

APK cập nhật hiệu ứng đang được đóng gói trong dự án riêng; không đóng Unity người dùng. Chưa kiểm thử trên điện thoại thật.
