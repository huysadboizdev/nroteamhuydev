# Kiểm tra chế tạo Thiên Sứ — 2026-10-07

Kiểm tra chỉ đọc source, SQL `team2026` và bytecode JAR đang cài. Không sửa source ứng dụng, SQL, hành trang hoặc JAR; không thực hiện chế tạo trên người chơi thật.

JAR SHA-256: `f06c7bc977378926f01591265690b16b6a0b03c135ac54599d2080727ab7a8cc`.

## Mở chức năng và nguyên liệu

Whis map 154 → Nói chuyện → Chế tạo. `Whis.handleCheTaoTrangBiThienSu` kiểm tra mặc đủ 5 món Hủy Diệt ID 650–662 ở năm ô trang bị trước khi mở bảng. Không thấy kiểm tra lại bộ đang mặc trong hai hàm chế tạo.

Nhánh `CombineService.showInfoCombine` gọi `CheTaoTrangBiThienSu.showInfoCombine` khi gửi danh sách nguyên liệu. Hàm này yêu cầu đúng 4 vật phẩm khác nhóm:

| Nhóm | Số lượng | ID |
|---|---:|---|
| Công thức thường hoặc VIP | 1 | 1071–1073 hoặc 1084–1086 |
| Mảnh của loại đồ muốn tạo | 999 | 1066 áo, 1067 quần, 1070 găng, 1068 giầy, 1069 nhẫn |
| Đá nâng cấp cấp 1–5 | 1 | 1074–1078 |
| Đá may mắn cấp 1–5 | 1 | 1079–1083 |

Thêm 10 triệu vàng và ít nhất một ô túi trống. Cả công thức thường lẫn VIP đều được `isCongThucVip` chấp nhận, không cần đưa cả hai công thức. Hành tinh của công thức quyết định hành tinh đồ; mảnh quyết định loại áo/quần/găng/giầy/nhẫn. Nhánh này trừ vàng, tạo đồ và tiêu nguyên liệu ngay lúc chọn đủ đồ; không chỉ hiện thông tin. Thất bại vẫn tiêu toàn bộ nguyên liệu và 10 triệu vàng.

SQL shop hiện bán công thức thường/VIP, đá nâng cấp và đá may mắn mỗi món 10 hồng ngọc. Không có row shop bán mảnh 1066–1070 trong kết quả kiểm tra.

## Đồ nhận được và chỉ số nền

Có 15 template: áo 1048–1050, quần 1051–1053, găng 1054–1056, giầy 1057–1059, nhẫn 1060–1062; mỗi loại ba hành tinh.

`ItemService.DoThienSu` tạo chỉ số dưới đây trước phần biến đổi trong hàm chế tạo:

| Loại | Chỉ số nền trước ưu thế hành tinh |
|---|---|
| Áo | 2.800–4.000 giáp |
| Quần | 120.000–150.000 HP |
| Găng | 10.350–11.500 sức đánh |
| Giầy | 90.000–130.000 KI |
| Nhẫn | 18–20% chí mạng |

Ưu thế hành tinh nhân 1,1 rồi làm tròn xuống: Trái Đất +10% HP quần; Xayda +10% giáp áo/sức đánh găng; Namek +10% KI giầy và giá trị chí mạng nhẫn. Đây là tăng tương đối cho chỉ số món đó, không phải +10 điểm phần trăm chí mạng. Nhẫn Namek nền tương ứng 19/20/22%.

Đường nhận vào túi gọi `ItemDefaultOptions.complete`: yêu cầu mặc 80 tỷ sức mạnh, không thể giao dịch/ký gửi. Không tự thêm SKH trong factory này: hàm tên `createItemSetKichHoat` ở đây chỉ tạo Item rỗng options trước khi thêm chỉ số. Counter `setDHD` có đếm option21=80 nhưng không có consumer; không thấy bonus riêng khi mặc đủ 5 món Thiên Sứ.

## Bonus ngẫu nhiên

Nhánh `showInfoCombine` sau khi chế tạo thành công có thể thêm 1–3 dòng **khác nhau**, chọn từ:

| Option | Bonus trên mỗi dòng |
|---:|---|
| 50 | Sức đánh +1–3% |
| 77 | HP +1–3% |
| 103 | KI +1–3% |
| 94 | Giảm sát thương +1–3% |
| 5 | Sức đánh chí mạng +1–3% — không phải tỷ lệ chí mạng |

Không có chuyện mọi món đều nhận đủ cả HP/KI/sức đánh, cũng không có đoạn random bonus chỉ số 0–15% như lời hướng dẫn. Mỗi loại chỉ có thể xuất hiện một dòng trong lần random này.

Đá may mắn nâng xác suất có ít nhất một dòng; các tỷ lệ được tính chính xác từ random số nguyên **bao gồm hai đầu mút**, không lấy nhãn biến làm tỷ lệ:

| Cấp đá | Thành công theo cấp đá nâng | Có bonus theo cấp đá may mắn, nếu đã thành công |
|---:|---:|---:|
| 1 | 90,10% | 21,57% |
| 2 | 91,09% | 31,37% |
| 3 | 92,08% | 41,18% |
| 4 | 93,07% | 50,98% |
| 5 | 94,06% | 60,78% |

Code còn thêm option15 param=1/2/3 để đánh dấu số dòng. SQL gọi option15 là “Phản đòn cận chiến+#”, không phải nhãn số dòng bonus; không có nhánh option15 trong NPoint để áp dụng chỉ số này. Không nên hiểu nó là bonus phản đòn có hiệu lực.

## Những lỗi đã xác nhận trong code/JAR

1. Hàm dùng để hiển thị nguyên liệu thực hiện chế tạo và trừ đồ ngay. Luồng chọn item gọi nó trực tiếp, không cần bấm xác nhận chế tạo.
2. Khi thành công, hàm gán `tilemacdinh=100` và tăng **100%** giá trị các option, trừ option0 sức đánh găng và option20 PIN. Do đó giáp áo, HP quần, KI giầy, chí mạng nhẫn bị nhân đôi; găng không nhân đôi. Chỉ số yêu cầu sức mạnh/khóa sau đó được chuẩn hóa về 80 tỷ/không giao dịch lúc vào túi.
3. Nhánh bấm nút `CombineService.startCombine` gọi `CheTaoTS`, là công thức khác: phải có đúng 4 item và 500 triệu vàng, tìm một món Hủy Diệt trong danh sách, tìm mảnh **chỉ cần >=5**, rồi trừ **99 mảnh** và món Hủy Diệt; không xử lý công thức/đá nâng/đá may mắn, không thêm random bonus như nhánh trước.
4. Hai bộ nguyên liệu không tương thích: bộ công thức+mảnh+hai loại đá không chứa Hủy Diệt nên nhánh bấm nút có thể ném `NoSuchElementException` ở `findFirst().get()`. Ngược lại nhánh 500 triệu cho qua từ 5 mảnh nhưng trừ 99, và hàm trừ quantity xóa item khi số lượng <=0.
5. Cả hai nhánh không kiểm tra kết quả `addItemBag` trước khi tiêu nguyên liệu; chưa có transaction bảo vệ đồng bộ túi/vàng cho toàn bộ chế tạo. Đây là phát hiện khi đọc code; không chạy lỗi trên tài khoản thật.

Cần thống nhất một công thức và một bước xác nhận trước khi coi chức năng này ổn định. Tác vụ hiện tại chỉ kiểm tra theo yêu cầu, chưa sửa hành vi.

## Bằng chứng

- `SRC/src/nro/models/npc_list/Whis.java:94` — mở chức năng và kiểm tra bộ Hủy Diệt.
- `SRC/src/nro/models/combine/CombineService.java:84` — luồng chọn nguyên liệu; dòng169 và260 trỏ hai hàm khác nhau.
- `SRC/src/nro/models/combine/CheTaoTrangBiThienSu.java:21` — nhánh 999 mảnh/10 triệu, nhân đôi và bonus.
- `SRC/src/nro/models/combine/CheTaoTrangBiThienSu.java:140` — nhánh 500 triệu/đồ Hủy Diệt/99 mảnh.
- `SRC/src/nro/models/services/ItemService.java:1145` — các chỉ số nền.
- `SRC/src/nro/models/item/ItemDefaultOptions.java:42` — yêu cầu sức mạnh và khóa giao dịch.
- `.analysis/thien-su-combine-bytecode.txt`, `.analysis/thien-su-item-service-bytecode.txt` — bytecode của JAR đã cài, đối chiếu cùng source.
- Đọc trực tiếp SQL item_template 1048–1086, item_option_template và item_shop; không sửa database.
