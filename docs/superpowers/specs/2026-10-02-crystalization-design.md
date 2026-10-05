# Pha lê hóa tối đa 9 sao và đánh giá vàng hành trang

Yêu cầu người dùng: dễ pha lê hóa, tỷ lệ giảm đều từ 100% khi lên 1 sao đến 5% khi lên 9 sao. Kiểm tra khả năng chứa 5.000 tỷ vàng và kiểu dữ liệu hiện tại.

## Thiết kế áp dụng

Giữ trần 9 sao đã có. Với số sao hiện tại s từ 0 đến 8, xác suất lên sao tiếp theo là `100 - 95*s/8` phần trăm. Các giá trị là 100; 88,125; 76,25; 64,375; 52,5; 40,625; 28,75; 16,875; 5. Ngoài khoảng 0..8 trả 0.

Dùng `CombineSystem.getRatioPhaLeHoa` làm nguồn chung; `PhaLeHoaTrangBi.getRatio` ủy quyền vào đó. Menu hiển thị tỷ lệ thật. Khi random, chuyển tỷ lệ sang số nguyên với độ chính xác 0,001% và gọi overload long của `Util.isTrue`, tránh overload float làm mất phần lẻ. Không thay phí vàng/ngọc, luật thất bại hoặc cơ chế dừng sau lần thành công đầu tiên của nút đập 10/100 lần.

Hai cách khác đã cân nhắc: làm tròn tỷ lệ thành phần trăm nguyên (dễ đọc nhưng không chia đều chính xác), hoặc đổi toàn bộ `Util.isTrue(float, ...)` (ảnh hưởng các chức năng ngoài phạm vi). Chọn xác suất chính xác riêng trong pha lê hóa.

## Vàng hành trang

`Inventory.gold` và `LIMIT_GOLD` là long, hiện cap 200.000.000.000. `MrBlue` đọc vàng bằng Long.parseLong; `PlayerDAO` lưu JSON trong `player.data_inventory` kiểu text. 5.000 tỷ = 5.000.000.000.000 vừa long.

Client >=214 được gửi writeLong ở Service.player, Service.sendMoney và PlayerService.sendInfoHpMpMoney. Client thấp hơn nhận int, không biểu diễn được mức này. Chưa có source client để xác nhận UI và parser. Giới hạn per-transaction và chi phí int không đồng nghĩa số dư int. GiftCodeService còn clamp độc lập 2 tỷ; cần xử lý khi thật sự nâng cap để không làm giảm số dư cao khi nhận code.

Trong lượt này sửa pha lê hóa theo yêu cầu; vàng được kiểm tra tương thích, chưa đổi cap khi chưa xác định client. Không thay giao thức client.

## Xác minh

Test main Java không phụ thuộc database: kiểm tra bảng tỷ lệ, biên -1/9/10, trần 9 sao và chuỗi kết quả random với seed cố định. Biên dịch toàn bộ source với --release 17 và Lombok. Không khởi động server hoặc truy cập DB. Không có Git repository để commit.
