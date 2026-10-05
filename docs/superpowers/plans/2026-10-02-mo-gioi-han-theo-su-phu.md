# Điều chỉnh mở giới hạn theo sức mạnh sư phụ

Người dùng đã yêu cầu triển khai và chọn mở lần lượt từng bậc, không thêm nút mở toàn bộ.

## Quy tắc

- Sư phụ đạt ít nhất 17.999.999.999 sức mạnh là có thể mở tiếp mọi bậc cho bản thân và đệ tử đến tối đa, không cần bản thân/đệ tử đạt trần sức mạnh từng bậc.
- Mỗi lần vẫn tăng đúng một bậc. Giữ nguyên trần bậc và chỉ số gốc; trần cuối 90.010.000.000.
- Phí mở ngay bậc 0–3 giữ 50 triệu vàng/bậc. Mở thường cho bản thân vẫn là lựa chọn cũ, nhưng người dùng có thể chọn mở ngay để không chờ.
- Bậc 4/5: 1 thần linh; bậc 6/7: 1 hủy diệt; bậc 8: 1 thiên sứ. Mỗi bậc này tiêu hao đúng món đã chọn và 500 triệu vàng của sư phụ.
- Mở bản thân: chỉ lấy đồ bản thân đang mặc. Mở đệ tử: cho chọn đồ đang mặc của sư phụ hoặc đệ tử, loại nguyên liệu theo bậc của đệ tử. Đồ trong túi/rương không được tính.
- Menu/confirm ghi rõ nguồn đồ. Xác nhận kiểm tra lại sức mạnh sư phụ, sở hữu đệ tử, bậc, slot/instance/options và vàng; chỉ tiêu hao một lần.

## Các bước

- [x] Viết và chạy test thất bại với JAR cũ: master-gate, master-gear.
- [x] OpenPowerService: thay điều kiện đạt từng trần bằng gate sức mạnh sư phụ; thêm nguồn đồ vào Selection, chọn từ body sư phụ/đệ tử và tiêu hao đúng nguồn. Cập nhật stat/ngoại hình của người mất đồ.
- [x] Quốc Vương: kiểm tra gate cho cả menu dưới và trên 40 tỷ; ghi nguồn đồ và sửa lời thoại, giữ chọn/xác nhận từng bậc.
- [x] Kiểm thử không cần sức mạnh đệ tử, sư phụ giữ nguyên 17.999.999.999 qua mọi lần mở, mất đúng đồ/tiền, đổi đồ/đệ tử/gate sau preview và xác nhận lặp.
- [x] Chạy toàn bộ source/JAR cùng review độc lập, sao lưu và cập nhật server đã được người dùng yêu cầu.

Các yêu cầu trong tài liệu này thay thế điều kiện sức mạnh và nguồn đồ trong kế hoạch Quốc Vương trước đó. Không thay phí hay số món bị tiêu hao.
