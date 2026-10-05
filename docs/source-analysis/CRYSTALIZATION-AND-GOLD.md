# Pha lê hóa và giới hạn vàng — 02/10/2026

Đã sửa hai tệp production: `PhaLeHoaTrangBi.java`, `CombineSystem.java`. Giữ tối đa 9 sao; tỷ lệ menu và lần đập thật dùng chung bảng tuyến tính. Không đổi phí vàng/ngọc hoặc quy tắc nút 10/100 lần dừng sau lần thành công đầu tiên.

| Nâng sao | Tỷ lệ thành công mỗi lần |
| --- | --- |
| 0 → 1 | 100% |
| 1 → 2 | 88,125% |
| 2 → 3 | 76,25% |
| 3 → 4 | 64,375% |
| 4 → 5 | 52,5% |
| 5 → 6 | 40,625% |
| 6 → 7 | 28,75% |
| 7 → 8 | 16,875% |
| 8 → 9 | 5% |

Mỗi mức giảm 11,875 điểm phần trăm. `star` trong code là số sao đang có, nên nhánh `star=8` chính là đập lên 9 sao. Mức ngoài 0..8 trả 0%. Đổi random pha lê hóa sang overload long với denominator 100.000, tránh việc overload float chung cắt bỏ phần thập phân khi tỷ lệ >=1%.

## Vàng hành trang hiện tại

| Vị trí | Kiểu / giới hạn |
| --- | --- |
| `Inventory.gold` | `long`, số dư hành trang |
| `Inventory.LIMIT_GOLD` | `long`, `200_000_000_000L` = 200 tỷ |
| `Inventory.addGold` / `subGold` | Tham số `int`, giới hạn số tiền mỗi lời gọi, không đổi kiểu số dư |
| `Combine.goldCombine` | `int`, chi phí một lần combine |
| `CombineSystem.getGoldPhaLeHoa` | Trả `int`, phí hiện tại 5 triệu..300 triệu mỗi lần |
| `Trade.goldTrade1 / goldTrade2` | `int`; tối đa 10 triệu vàng một lần giao dịch theo hằng hiện có |
| `MrBlue.loadPlayer` | Đọc số dư bằng `Long.parseLong` |
| `PlayerDAO.updatePlayer` | Lưu số dư vào JSON, clamp bằng `Inventory.LIMIT_GOLD` |
| SQL `player.data_inventory` | `text`, chứa JSON inventory; không phải cột INT giữ số dư hành trang |
| Client từ version 214 | Các gói chính dùng `writeLong` |
| Client dưới version 214 | Dùng `writeInt((int) gold)`, số dư lớn có thể tràn/hiển thị sai |

5.000 tỷ = `5_000_000_000_000L`, nằm trong `long` (max 9.223.372.036.854.775.807). Phần server giữ số dư và lưu JSON có khả năng biểu diễn mức này; không cần chuyển cột `data_inventory` sang BIGINT. Không thể xác nhận toàn bộ UI/parser client chỉ từ source server này.

Các bước khi nâng cap thật sự:

1. Đổi `Inventory.LIMIT_GOLD` sang `5_000_000_000_000L`.
2. Đổi clamp `2_000_000_000L` riêng trong `GiftCodeService` về giới hạn chung, để nhận code không kéo số dư lớn xuống 2 tỷ.
3. Kiểm chứng client sử dụng version >=214 và đọc các gói chứa vàng dạng long. Giữ layout gói tin cũ nếu vẫn hỗ trợ client thấp hơn; chỉ đổi writeInt thành writeLong ở nhánh cũ sẽ làm lệch gói.
4. Kiểm tra các đường tăng/giảm vàng, bán thỏi vàng, shop, ký gửi, giao dịch, nhặt vàng, nhận thưởng và save/load ở mức trần. Những khoản int mỗi lần không cần tự động chuyển thành long nếu giới hạn mỗi lần vẫn nhỏ.
5. Chú ý `InventoryService.addItemBag` có nhánh chibi cộng vàng hai lần sau khi kiểm tra giới hạn một lần; cần kiểm thử tránh vượt trần.

Ở lượt này cap chưa đổi: người dùng yêu cầu kiểm tra khả năng phù hợp; phiên bản client chưa xác định. Pha lê hóa đã sửa theo thông số yêu cầu.

## Kiểm tra thay đổi

Lệnh: `python .analysis/verify_crystalization.py`.

- Source cũ: test thất bại đúng yêu cầu, sao thứ nhất expected 100%, actual 50%.
- Source mới: biên dịch 548 tệp production và tệp test với `--release 17`, exit 0.
- Test: 9 mức tỷ lệ, API chung, các biên -1/9/10/Integer.MAX_VALUE, trần 9 sao, 4.500 lượt random deterministic: PASS.
- Không chạy server, không kết nối database, chưa kiểm thử menu trong client game.

Source sửa chưa được đóng gói vào `20.jar`. `run.bat` hiện chạy JAR này, nên cần build/đưa JAR tương ứng vào đường chạy trước khi thay đổi xuất hiện trong game.
