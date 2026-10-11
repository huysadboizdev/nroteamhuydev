# Rà soát ba workspace trước khi push

Phạm vi: diff chưa commit của server/Unity; kiểm tra hồi quy rộng cho server và web, biên dịch Unity, SQL migration và tài nguyên. Đây là các kiểm tra tự động và review source, không phải xác nhận toàn bộ thao tác gameplay bằng tài khoản thật.

## Lỗi đã sửa

- Server: phép chia nguyên làm option tốc độ chạy dưới100% không cộng tốc độ. Tính phép nhân trước phép chia, có test các mốc13/25/50/75/99%.
- Server: BossAdminDropService khóa Sói trước chủ nhân, ngược với luồng cho xương. Luồng thưởng Sói hiện khóa chủ nhân trước Sói; test deadlock và chống thưởng lặp đạt.
- Server: template SKH VIP rỗng gây NullPointerException trước kiểm tra hợp lệ. Kiểm tra template tồn tại trước truy cập type.
- Test: cập nhật constructor/singleton GiftCodeService, version catalog động, giới hạn stack1triệu và fixture chuyển hóa có option tường minh thay cho default mới.
- Test Ngũ Hành Sơn dùng fixture tuyến đường được commit để chạy được trên checkout mới, không phụ thuộc file phân tích cục bộ.
- Web: vô hiệu hóa hai trang quản trị cũ nằm dưới static assets. Chúng chứa upload đường dẫn tùy ý và luồng thay đổi dữ liệu không qua admin hiện tại; bản gốc lưu trong .analysis tại máy. HTTP trả403, không còn code upload/SQL.
- Web: thêm ảnh tám boss Zamasu/Heart/Kamin/Oren/Beerus/Whis/Champa/Vados. Head part đối chiếu source và SQL, ảnh PNG lấy từ resource server.
- Git: bỏ qua dump SQL đầy đủ/metadata có dữ liệu tài khoản; giữ seed đã loại dữ liệu riêng và migration trên Git.

## Kiểm chứng

- Toàn bộ source Java17 và223 test source biên dịch thành công;193 trường hợp hồi quy offline/DB tạm đạt. Nhóm gồm NPC, inventory, crafting, boss, đệ, cải trang, TNSM, chuyển map và protocol.
- SQL migration Daishinkan/GokuDay/Ngũ Hành Sơn chạy trên database tạm, kiểm tra replay, bảo toàn NPC/quái và cửa chuyển map.
- Web132 kiểm tra cú pháp/test/security đạt; test database admin boss trên database ngẫu nhiên riêng đạt. HTTP trang chủ200, admin khi chưa đăng nhập403, hai endpoint cũ403.
- Unity2022.3.62f2 biên dịch toàn bộ script ở project kiểm thử riêng. Renderer cả hai tab, rương sưu tầm và resource đá bốn mức zoom đạt; PNG giữ nguyên kích thước, point filtering, không NPOT resize.
- JAR ứng viên được kiểm tra cả classes và đóng gói với các test Ngũ Hành Sơn/GokuDay/stack/giftcode/deadlock Sói/SKH VIP/admin drops. Người dùng đã đồng ý triển khai: bảo trì lưu dữ liệu, sao lưu JAR/hash trước khi cài; server mới mở cổng14445 và agent xác nhận NPC/cửa map. SQL đầy đủ `SRC/sql/team2026-full-20261011-084141.sql` restore đủ75bảng, chỉ giữ trong máy.

Log/JSON chi tiết nằm .analysis/workspace-review-20261010 tại máy. Git không chứa JAR đang chạy, backup JAR, khóa thanh toán, config local hay SQL dữ liệu thật.

## Cảnh báo còn lại

Các ID map121/125/130/134 chỉ có tên là số ID, npcs/mobs/waypoints đều rỗng và thiếu file tile. Cảnh báo này có từ trước; server bỏ qua chúng khi nạp. Không có tài nguyên để khôi phục thành map chơi được nên chưa tự tạo map thay thế. Ba map Ngũ Hành Sơn122/123/124 có tài nguyên và đã được kiểm chứng riêng.
