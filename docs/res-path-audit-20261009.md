# Kiểm tra lỗi res/path ngày 09/10/2026

Trạng thái: đã xác định nguyên nhân; lượt này chỉ kiểm tra, chưa sửa hoặc bảo trì server.

## Nguyên nhân chính

Log `SRC/log/anime-shop-fix-20261009-120529.err.log` có `IllegalArgumentException: Packet exceeds frame capacity` tại `MessageSendCollect.java:61`, gọi từ `ItemData.updateItemTemplate:88` và `ItemData.updateItem:16`.

`ItemData.updateItem` gửi template theo hai nhóm cố định: 0–749 và 750 đến hết catalog. Gói thứ hai dùng command -28, khung thường chỉ chứa tối đa 65535 byte. Catalog runtime có mô tả được cập nhật sau khi tải SQL, nên tính kích thước chỉ từ SQL chưa đủ. Gói thứ hai bị từ chối trước khi gửi.

Cache PlayerPrefs của Unity (`DefaultCompany/NRO QUEEN`, Tab1) có NRitem0, NRitem100, NRitem1 mang phiên bản22 (hex16), nhưng không có NRitem2 và NRitemVersion. Vì vậy phần template từ750 trở lên chưa được nạp hoàn chỉnh.

Log Unity `C:/Users/Admin/AppData/Local/Unity/Editor/Editor.log` xác nhận:

- `Game1.SmallImage.drawSmallImage:143` → `Panel.paintTopInfo:7590`: `IndexOutOfRangeException`.
- `Game1.Panel.paintInventory:7066`: `NullReferenceException` khi đọc `item.template.iconID`.

`Char.getAvatar` trả -1 nếu chưa có mapping. `SmallImage` chỉ kiểm tra cận trên, chưa chặn ID âm, nên truy cập `smallImg[-1]`. Trong xử lý đăng nhập, template chưa tồn tại gây lỗi đọc hành trang; đoạn đọc item body nuốt exception rồi tiếp tục đọc cùng stream, khiến việc đọc các phần sau (bao gồm avatar) không bảo đảm đúng vị trí. Đây là cơ chế lan lỗi phù hợp với stack trace; chưa ghi lại packet đăng nhập thực tế để xác nhận từng ID bị ảnh hưởng.

## Tài nguyên đã xác minh

- 38 part mới, ID2099–2136: bảy boss mới, các bộ anime/pet và Akatsuki.
- Cả 38 part có số frame đúng loại, tọa độ hợp lệ và ID icon trong giới hạn32767.
- 287 icon được tham chiếu bởi các part này có đủ x1–x4 và giải mã PNG được.
- 688 ảnh boss, 540 ảnh anime, 120 ảnh Akatsuki đều đúng hash manifest khi triển khai.
- `SRC/data/update_data/part` có2137 part, dữ liệu của38 part mới khớp từng frame với SQL.
- Phần gửi version icon server là32767; các ID remap24000–242xx nằm trong giới hạn.

Kết quả máy đọc: `.analysis/res-path-audit/result.json`.

## Hướng sửa

1. Chia catalog theo kích thước byte thực tế, bảo đảm mỗi packet -28 không vượt65535. Với client hiện có, cần giữ đầy đủ hai nhóm cache NRitem1/NRitem2; nếu mở rộng thành nhiều nhóm thì phải cập nhật cách lưu/khôi phục cache ở cả Game1 và Game2.
2. Client chặn ID âm, kiểm tra biên mảng/cache icon và template null; không để lỗi một vật phẩm làm lệch stream đăng nhập. Ghi log lỗi tải template thay vì nuốt exception.
3. Tăng phiên bản item sau sửa, kiểm tra đủ hai nhóm template và avatar trên client thật, rồi xác minh lại hành trang/cửa hàng/các bộ mới.

Các cảnh báo map tile cũ78/95/101/121/125/130/134 là vấn đề riêng, không phải nguyên nhân của stack trace trong ảnh.
