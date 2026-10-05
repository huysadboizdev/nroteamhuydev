# Kiểm tra cộng dồn vật phẩm

Đã đọc danh mục server đang chạy: 2001 mẫu vật phẩm, 936 dòng bán hàng.
Có 408 mẫu đang bán được đánh dấu không cộng dồn. Trong đó 17 mẫu tiêu hao/nguyên liệu được sửa; các trang bị, cải trang và pet có chỉ số riêng giữ từng món.

Danh sách đầy đủ: [nonstack-shop-items.csv](../../.analysis/item-stack/nonstack-shop-items.csv).

## Vật phẩm được sửa

| ID | Vật phẩm | Có trong cửa hàng |
|---|---|---|
| 1072 | Công thức | Có |
| 1073 | Công thức | Có |
| 1085 | Công thức VIP | Có |
| 1086 | Công thức VIP | Có |
| 1143 | Đá bảo vệ (Khóa) | Không; sửa chung khi nhận vật phẩm |
| 1509 | Nơ trang trí | Có |
| 1521 | Thiệp mừng 8-3 | Có |
| 1525 | Túi hạt giống Hoa Hồng | Có |
| 1528 | Chậu đất | Có |
| 1529 | Thuốc tăng trưởng | Có |
| 1532 | Rađa kho báu | Có |
| 1569 | Kho báu hải tặc | Có |
| 1608 | Hộp quà thiếu nhi | Có |
| 1609 | Kem trái cây | Có |
| 1628 | Bùa x2 tn,sm đệ tử | Có |
| 1652 | Loa to thế giới | Có |
| 1653 | Loa to liên vũ trụ | Có |
| 1775 | Hộp thần linh | Có |

## Hành vi và giới hạn

- Giới hạn số lượng hành trang/rương hiện là 2.147.483.647 mỗi ô, đã hỗ trợ 1.000.000. Không hạ giới hạn đang có xuống một triệu.
- Mua tiếp vào ô cộng dồn được phép khi hành trang đầy; không trừ tiền nếu không nhận được vật phẩm.
- Những món khác chỉ số hoặc khác thời điểm hết hạn được giữ riêng; thứ tự hiển thị option không ngăn cộng dồn.
- Bùa Santa x2 đệ tử cộng dồn số lượng khi mua và cộng thời gian khi sử dụng; dùng sau khi bùa cũ hết hạn bắt đầu đủ 30 phút mới.
- Thời gian bùa Santa được đọc bằng số 64 bit khi đăng nhập để không tràn khi cộng thời gian lớn.
- Phiên bản danh mục vật phẩm tăng từ 15 lên 16 để client nhận cờ cộng dồn mới.
- Các giới hạn 99 riêng của ký gửi/chia lô giao dịch và số lượng nguyên liệu công thức không đổi.

## Xác minh

- 113 chế độ kiểm thử trên mã nguồn và cùng 113 chế độ trên JAR đóng gói; gồm mua khi đầy túi, thiếu tiền, trang bị riêng, option khác nhau, hạn dùng khác nhau, bùa Santa và cộng đủ một triệu.
- Chi tiết chạy và triển khai: `.analysis/item-stack/build.log`, `.analysis/item-stack/deployment.json`.
- Chưa thao tác mua bằng tài khoản thật trong giao diện Unity; kiểm tra live danh mục sau khởi động được lưu riêng.
