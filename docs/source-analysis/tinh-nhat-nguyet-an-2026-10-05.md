# Kiểm tra Tinh Ấn, Nhật Ấn, Nguyệt Ấn — 05/10/2026

## Kết quả kiểm tra

Đã đọc SQL đang chạy ở MySQL `team2026` và mã nguồn trong `SRC/src`. Chưa kiểm chứng bytecode của `SRC/20.jar` hoặc thử nhân vật trong game.

| Bộ | Option đánh dấu trong SQL | Hiệu ứng đủ bộ trong mã nguồn |
| --- | --- | --- |
| Tinh Ấn | 34 | +15% sức đánh |
| Nguyệt Ấn | 35 | +15% KI tối đa |
| Nhật Ấn | 36 | +15% HP tối đa |

Nguồn: `NPoint.java`, các đoạn 781–784, 865–867, 982–983, 1105–1107. Tên option trong database đang chạy khớp hai dump `team2026.sql` và `SRC/sql/nro1.sql`.

`NPoint` đang đếm option 34/35/36 và kích hoạt khi số đếm >=5; không sử dụng param của option để tính phần trăm. Vòng tính đồ mặc duyệt toàn bộ `itemsBody`; các option thẻ đang kích hoạt cũng có nhánh tăng số đếm. Vì vậy cơ chế hiện tại chưa bảo đảm nghiêm ngặt “5 món khác nhau: áo, quần, găng, giày, rada cùng ấn”. Khi triển khai phải đếm từng món trong 5 ô đó, mỗi món tối đa một lần.

Truy vấn database đang chạy: không có dòng option 34/35/36 trong `item_shop_option` và `item_default_option`. Điều này không kết luận đồ đã lưu trong hành trang người chơi có hay không có ấn.

Option thông thường 103 là `KI +#%`; `NPoint.addOption` đưa param trực tiếp vào danh sách tăng KI. Gắn 103=5 vào đồ sẽ tăng KI dù chưa đủ bộ. Danh sách phần trăm đang được áp dụng tuần tự trong tính KI, nên 5 dòng 5% cũng không tự tương đương một lần +25%.

`ItemMap` khởi tạo danh sách option rỗng; từng nhánh rơi đồ thêm option sau đó. Đã thấy quái và nhiều boss lấy chỉ số từ `ItemService.getListOptionItemShop`. Chỉnh tên option trong SQL không tự tạo ấn hay option ẩn khi rơi đồ.

`ItemDefaultOptions.loadFromDatabase` chỉ cho phép các item/option đã khai báo trong `REQUIRED` (710, 711, 1710, 1869). Không thể tùy ý thêm profile trang bị ấn vào bảng này với mã nguồn hiện tại.

## Yêu cầu đã chốt qua trao đổi

Hai hiệu ứng độc lập, không thay thế nhau:

| Loại ấn | Dòng đánh dấu vẫn hiển thị | Bonus riêng từng món | Bonus bộ đủ 5 món |
| --- | --- | --- | --- |
| Tinh Ấn | Tinh ấn (34) | Sức đánh +1–5%, option 50 | +15% sức đánh |
| Nguyệt Ấn | Nguyệt ấn (35) | KI +1–5%, option 103 | +15% KI |
| Nhật Ấn | Nhật ấn (36) | HP +1–5%, option 77 | +15% HP |

- Mỗi món trang bị có ấn rơi từ boss hoặc quái/up nhận một giá trị random nguyên từ 1 đến 5, gồm cả 1 và 5; tối đa 5% cho dòng bonus sinh thêm trên món đó.
- Bonus riêng có tác dụng ngay khi mặc món đó, không cần đủ bộ. Tháo món thì mất phần bonus của món.
- Đủ áo, quần, găng, giày, rada cùng loại ấn mới bật +15% của bộ. Trộn các loại ấn không kích hoạt bộ.
- Ví dụ quần Nguyệt Ấn roll 3%: vẫn hiện Nguyệt ấn và có thêm dòng KI +3%. Chỉ mặc quần đã hưởng 3%; đủ bộ mới thêm hiệu ứng bộ +15% KI.
- Giá trị random được lưu theo món; nhặt, chuyển túi/rương, mặc/tháo, giao dịch, đăng nhập lại không roll lại.
- Từ “option ẩn” ban đầu được hiểu theo ví dụ và xác nhận sau đó là dòng chỉ số bổ sung. Phương án kế hoạch dùng dòng option thông thường có hiển thị; không bổ sung cơ chế che dòng ở client.
- Giới hạn 5% áp dụng dòng sinh thêm, không chặn tổng chỉ số nhân vật, dòng KI/HP/sức đánh khác hoặc hiệu ứng bộ 15%.
- Không hứa tổng 5 món 5% cộng bộ là +40%: engine hiện tại áp dụng nhiều phần trăm tuần tự. Giữ công thức hiện có, xác minh bằng kiểm tra số thực tế.

## Phạm vi kế hoạch và thông tin chưa được quy định

Khuyến nghị bảng cấu hình SQL riêng cho ánh xạ ấn → option riêng → khoảng random → bonus bộ; Java đọc bảng và áp dụng trong luồng đồ rơi. Dùng option 34/35/36 để nhận diện bộ, option 50/103/77 để tăng chỉ số từng món.

Không dùng param của ấn để chứa bonus riêng: mã hiện tại dùng nó làm dấu bộ, dễ gây hiểu nhầm khi xử lý đồ cũ. Không cần ID option mới vì ba dòng phần trăm đã có sẵn.

Người dùng chưa quy định boss/map, danh sách item được rơi ấn hoặc xác suất xuất hiện từng loại ấn. Khi triển khai sẽ kiểm kê luồng hiện có; không tự tăng tỷ lệ rơi hoặc biến toàn bộ đồ thường thành đồ ấn. Kế hoạch trước mắt áp dụng bonus cho đồ rơi đã được nhận diện là có ấn. Nếu chưa có luồng sinh ấn, cần chốt danh sách và tỷ lệ trước phần tạo ấn mới.

Đồ cũ: đề xuất giữ nguyên dữ liệu, áp dụng bonus random cho đồ mới; không cập nhật hàng loạt đồ trong hành trang. Đây là phạm vi đề xuất, chưa phải yêu cầu bổ sung từ người dùng.

Kế hoạch chi tiết: `docs/superpowers/plans/2026-10-05-tinh-nhat-nguyet-an.md`.

## Bổ sung đồ Thần Linh

Theo yêu cầu nối kế hoạch: đồ Thần Linh cũng có ba loại ấn, giữ hai hiệu ứng độc lập.

| Loại đồ Thần Linh | Bonus riêng mỗi món | Đủ 5 món Thần Linh cùng ấn |
| --- | --- | --- |
| Thần Tinh Ấn | Random +1–7% sức đánh, option 50 | +20% sức đánh |
| Thần Nhật Ấn | Random +1–7% HP, option 77 | +20% HP |
| Thần Nguyệt Ấn | Random +1–7% KI, option 103 | +20% KI |

Người dùng đã sửa rõ: 7% là tối đa, vẫn random 1–7% (min=1, max=7). Đồ thường vẫn random 1–5%, bộ 15%. Bonus riêng dùng ngay khi mặc và được lưu theo món.

Đã đối chiếu database: 555–567 là 13 template Thần Linh, gồm đủ áo/quần/găng/giày của ba hành tinh và nhẫn 561 dùng chung (type=4, gender=3). Đề xuất dùng lại template hiện có, thêm ấn theo instance của món, không tạo bản sao template cho từng ấn.

SQL cấu hình gồm `equipment_seal_rule` với 6 dòng theo cấp đồ/loại ấn và `equipment_seal_tier_item` với 13 dòng Thần Linh. Marker 34/35/36 vẫn giữ nguyên; Java phải phân biệt cấp đồ bằng bảng mapping để chọn bộ 15% hay 20%.

Bộ pha đồ thường/Thần Linh chưa được người dùng quy định. Đề xuất vẫn nhận bonus riêng theo từng món; chỉ bộ đủ 5 món Thần Linh cùng ấn mới hưởng 20%. Mức bộ pha cần chốt trước khi viết nhánh tính bộ, không tự áp 20% chỉ vì có một món Thần Linh.

Chưa chỉnh mã Java hoặc JAR. Cấu hình SQL không tự làm đồ rơi có ấn hoặc bật bộ Thần Linh 20% trên JAR hiện tại. Nếu cập nhật JAR, thực hiện bảo trì có lưu dữ liệu, chuyển bản cũ vào `SRC/jar-backup` với tên thời gian riêng, kiểm tra SHA-256 và ghi metadata theo `AGENTS.md`.

## Kết quả setup SQL

Đã áp `SRC/sql/migrations/20261005_equipment_seal_bonus.sql` lên MySQL `team2026` và nối cùng migration vào `team2026.sql`, `SRC/sql/nro1.sql`.

- Kiểm tra trước: hai bảng cấu hình chưa tồn tại; không ghi đè bảng có sẵn hoặc sửa dữ liệu trang bị/người chơi.
- Sau áp và chạy lại: đúng 6 dòng quy tắc (3 ordinary + 3 divine), 13 template divine, đủ 5 type trang bị, 0 dòng quy tắc sai.
- Snapshot hai dump trước sửa, metadata/hash migration và kết quả truy vấn: `.analysis/seal-sql-20261005-093452-230/`.
- Cấu hình đã lưu vào SQL; luồng rơi và hiệu ứng 20% chưa hoạt động cho tới khi hoàn thành phần Java/JAR trong kế hoạch.

## Chốt bổ sung: random tối đa 7% và tăng chỉ số gốc

Người dùng xác nhận Thần Linh tăng chỉ số gốc random 5–15%, vẫn bắt đầu từ 5%. Yêu cầu cuối cùng gồm ba phần độc lập:

| Cấp đồ có ấn | Chỉ số nền hơn đồ gốc | Dòng bonus riêng theo ấn | Bộ đủ 5 món cùng ấn |
| --- | --- | --- | --- |
| Thường | Random +5–10% | Random +1–5% | +15% |
| Thần Linh | Random +5–15% | Random +1–7% | +20% |

Tinh tăng sức đánh, Nhật tăng HP, Nguyệt tăng KI. Mức nâng nền áp dụng cho chỉ số nền của món, không phải thêm một dòng % nhân vật. Ví dụ giày gốc 100.000 KI, roll nền 10% thì KI nền thành 110.000; dòng KI +1–5% hoặc +1–7% và hiệu ứng bộ vẫn tách riêng.

Đề xuất roll một mức nâng nền cho món khi sinh đồ có ấn, lưu các chỉ số kết quả; không roll lại hoặc nhân thêm khi nhặt/mặc/lưu-nạp. Không tăng hạn dùng, số sao, cấp nâng, yêu cầu sức mạnh, marker ấn hay các dòng bonus phần trăm. Danh sách option chỉ số nền và cách làm tròn theo từng loại món phải được đối chiếu code/catalog khi triển khai; không tự nhân mọi param.

Đã thấy `randDoTL` và `randDoTLBoss` đang roll `tiLe=100..115`. Khi làm đồ Thần Linh có ấn phải chọn nền gốc trước tỷ lệ này, rồi áp đúng một lần 105..115%; không nhân 105..115% lên đồ đã roll 100..115% vì sẽ vượt trần so với gốc. Đồ không có ấn giữ luồng hiện có.

SQL đã sửa ba dòng divine thành min=1/max=7/set=20, thêm bảng `equipment_seal_base_rule`: ordinary 5..10, divine 5..15. Hai dump và migration đã đồng bộ. Snapshot cấu hình trước sửa và các file trước sửa lưu tại `.analysis/seal-sql-random-20261005-093757-567/`.

## Chốt bổ sung: cấm giao dịch, chỉ ký gửi hồng ngọc

Tất cả đồ có Tinh/Nhật/Nguyệt Ấn, cả thường và Thần Linh, không giao dịch trực tiếp. Chỉ hiện dòng Ký gửi ngọc (87), không có Ký gửi vàng (86), và ký gửi bằng hồng ngọc/ruby. Giữ dòng Không thể giao dịch (30), nhưng Java phải cho phép ký gửi ruby riêng theo chính sách đồ ấn.

Đã tạo và áp bảng `equipment_seal_market_rule` với 3 dòng đúng chính sách, đồng bộ hai dump. Chưa thay Java/JAR nên chính sách mới chưa được thực thi trong game.

Đã kiểm tra siêu thị ký gửi, tái hiện 5 lỗi trên JAR offline; báo cáo chi tiết: `docs/source-analysis/ky-gui-sieu-thi-2026-10-05.md`. Cần xử lý tiền tệ, mất đồ khi đầy túi, đồng thời và lưu bền trước khi bật ký gửi ruby cho đồ ấn.


## Chốt nhóm đồ thường: cấp 10, 11, 12

Người dùng chốt đồ thường có ấn chỉ thuộc cấp trang bị 10/11/12 (`item_template.level`), type 0..4. Không phải item ID 10/11/12; không bao gồm đồ cấp thấp khác. Đã bổ sung đủ 39 template ordinary vào `equipment_seal_tier_item`; giữ nguyên 13 template divine 555–567. Migration `20261005_equipment_seal_ordinary_levels.sql` đã áp DB, chạy lại kiểm tra idempotency và nối vào hai dump; mỗi cấp có 13 mẫu.

| Cấp | Trái Đất: áo/quần/găng/giày | Namếc: áo/quần/găng/giày | Xayda: áo/quần/găng/giày | Rada chung |
| --- | --- | --- | --- | --- |
| 10 | 231 / 243 / 255 / 267 | 235 / 247 / 259 / 271 | 239 / 251 / 263 / 275 | 279 |
| 11 | 232 / 244 / 256 / 268 | 236 / 248 / 260 / 272 | 240 / 252 / 264 / 276 | 280 |
| 12 | 233 / 245 / 257 / 269 | 237 / 249 / 261 / 273 | 241 / 253 / 265 / 277 | 281 |

Các mức giữ nguyên: ordinary nền +5–10%, dòng riêng +1–5%, bộ cùng ấn +15%; divine nền +5–15%, dòng riêng +1–7%, bộ cùng ấn +20%. Đồ cấp khác không tự được phân loại ordinary nếu thiếu mapping; engine phải kiểm tra bảng mapping thay vì mặc định mọi đồ không phải divine là ordinary.

Ví dụ admin xem quần vàng Goku Nguyệt Ấn: `i 243 1 6:1000 35:1 103:5`. Đây là đồ chỉ số test cố định, danh sách option truyền vào thay option gốc; lệnh cũ dùng quần ID 6 chỉ minh họa hiển thị, không thuộc nhóm đồ thường được chốt.

Trạng thái hiện tại: khóa giao dịch/ký gửi ruby đã chạy trên JAR; sinh đồ ấn, roll nền/bonus và phân biệt bonus bộ 15%/20% vẫn chưa triển khai engine. Lần bổ sung mapping này không thay JAR hay chỉnh đồ người chơi đã lưu.

Backup/verification: `.analysis/seal-ordinary-levels-20261005-105216`
