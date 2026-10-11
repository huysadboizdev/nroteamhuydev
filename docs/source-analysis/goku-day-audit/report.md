# Kiểm tra hộp Goku Day 1591 / 1594 — 10/10/2026

**Trạng thái:** Các lỗi dưới đây đã được sửa và triển khai; xem [bằng chứng bản sửa](../../goku-day-fix-20261010.md). Nội dung lỗi là kết quả kiểm tra trước sửa; `audit.json` và `sql-snapshot.json` hiện phản ánh database sau sửa. Bản chụp trước sửa được giữ trong thư mục backup của deployment.

Chỉ kiểm tra, không sửa source, SQL, tài nguyên, JAR hay dữ liệu người chơi.

## Phạm vi và bằng chứng

- Hai hộp đi chung hàm `OpenHopQuaGokuDayVangNgoc` ở `SRC/src/nro/models/services_func/UseItem.java:2296`. Đã đối chiếu bytecode trong JAR `SRC/20.jar`; pool, điều kiện ô trống và nhánh option93 khớp source.
- SQL đọc trực tiếp từ database `team2026`, gồm item template, part và head_avatar. Lưu bản chụp các dòng liên quan trong `sql-snapshot.json`.
- 126 ID ảnh duy nhất, gồm icon hộp, icon cải trang, avatar và các frame part; kiểm tra 504 PNG ở `SRC/data/icon/x1..x4`, giải mã ảnh bằng Pillow và kiểm tra alpha.
- Binary `SRC/data/update_data/part` giải mã hết 2137 part, số lượng khớp SQL. Các part của sáu phần thưởng khớp SQL về type, số frame, ID ảnh và offset.
- Bốn dump/seed đều lưu item1593 ở bộ1467/1471/1472; part1471/1472 có trong cả bốn file. Import lại bộ SQL hiện có vẫn mang bộ này.
- Client Unity không đóng gói các file `Assets/Resources/res/xN/smallimage/Small<ID>.png` của 126 ảnh này. `Game1/SmallImage.cs` và `Game2/SmallImage.cs` tải ảnh từ server khi không có local resource. Việc thiếu bản local không tự nó là lỗi; chưa kiểm tra cache RMS đang nằm trong tiến trình Unity.

## Phần thưởng chung

| ID | Tên | Head / Body / Leg | Kết quả |
|---:|---|---|---|
|1588|CT Goku|0 / 1 / 2|Part và PNG hợp lệ; head0 là ID hợp lệ, server chấp nhận|
|1589|CT Goku SSJ|1454 / 1450 / 1451|Không thấy thiếu/hỏng PNG hoặc sai type/frame|
|1595|CT Goku SSJ2|1455 / 1450 / 1451|Không thấy thiếu/hỏng PNG hoặc sai type/frame|
|1587|CT Goku SSJ3|1449 / 1450 / 1451|Không thấy thiếu/hỏng PNG hoặc sai type/frame|
|1593|CT Goku SSJ God|1467 / 1471 / 1472|Sai hình thân/chân, nhiều frame trong suốt|
|1590|CT Goku SSJ Blue|1457 / 1459 / 1460|Không thấy thiếu/hỏng PNG hoặc sai type/frame|

## Các lỗi xác định

### 1. Goku SSJ God ghép hình tuần lộc

`item_template.id=1593` có head1467 (Goku tóc đỏ), body1471 và leg1472. Ảnh mà body/leg tham chiếu là hình tuần lộc, đã kiểm tra trực quan cả x1–x4; ví dụ icon13278 và13285. Đây là bộ part sai danh tính nhân vật dù đúng type1/type2. Hình trong screenshot phù hợp với dữ liệu này.

Đường dẫn cần đối chiếu:

- `SRC/data/icon/x1/13278.png` … `SRC/data/icon/x4/13278.png` (body frame1).
- `SRC/data/icon/x1/13285.png` … `SRC/data/icon/x4/13285.png` (leg frame0).
- Các ảnh leg khác:13279,13280,13281,13282,13283,13284,13287.
- `SRC/data/update_data/part`, SQL `part.id IN (1471,1472)` và `item_template.id=1593`.
- `SRC/sql/team2026.seed.sql`, `SRC/sql/nro1.seed.sql`, `SRC/sql/nro1.sql`, `team2026.sql`.

Tài liệu cũ `docs/source-analysis/resource-consistency-fix.md:13` ghi đổi1593 sang1471/1472. Kết quả hiện tại cho thấy việc kiểm tra đúng loại part chưa đủ để kiểm tra đúng nhân vật.

### 2. Bộ God thiếu nội dung animation

- Body1471: 16/17 frame dùng icon2955 hoàn toàn trong suốt; chỉ frame1 có ảnh13278.
- Leg1472: frame9–13 (5/14 frame) dùng icon2955 trong suốt.
- SQL và binary đều chứa các frame này. Client `Char.cs` chọn frame theo `CharInfo`, nên các tư thế dùng frame trong suốt không vẽ được phần tương ứng. Cần kiểm tra lại animation sau khi sửa bộ đúng.
- Icon2955 cũng xuất hiện ở frame cuối của các bộ khác; không tự động coi mọi tham chiếu2955 là lỗi. Lỗi của bộ God là nhiều frame thân/chân dùng placeholder thay cho bộ animation nhân vật.

### 3. Nhánh hiếm trả cải trang hạn0 ngày

`UseItem.java:2315` thêm option `(93,0)`. SQL option93 là `Hạn sử dụng # ngày`; `ItemService.isEquippedOutOfDateTime` coi đồ này có thời hạn và hết hạn sau khi sang ngày mới, không phải vĩnh viễn. Cả hai nhánh đều thêm option93: nhánh hiếm0 ngày, nhánh còn lại3 ngày. Nếu thiết kế là có tỷ lệ nhận đồ vĩnh viễn, hiện tại không có nhánh đó.

### 4. Tỷ lệ thực tế là1%, không phải0,5%

`int randomOption=(int)(Math.random()*100)` chỉ có100 giá trị0..99. Điều kiện `<0.5` chỉ nhận giá trị0, tức1%. Nếu cần0,5%, cách lấy số/so sánh hiện tại không đạt tỷ lệ đó.

### 5. Thông báo ô trống không khớp điều kiện

`UseItem.java:2297` yêu cầu `getCountEmptyBag(player)>1`, tức ít nhất2 ô; thông báo ở dòng2325 lại yêu cầu5 ô. Hai hộp cùng chịu lỗi thông báo này.

## Những phần chưa thấy lỗi

Icon hộp1591=11006 và1594=11201 tồn tại, giải mã được và có nội dung ở cả x1–x4. Không có PNG thiếu/hỏng trong504 file đã kiểm tra; không có part thiếu/sai type hoặc SQL/binary lệch ở các bộ được xét. Các tên hộp giống nhau và dùng cùng pool/chỉ số là hành vi hiện có, chưa đủ bằng chứng để coi là lỗi thiết kế.

Chi tiết toàn bộ đường dẫn, kích thước, SHA256, alpha và frame tham chiếu: `audit.json`. Chưa thử mở hộp hoặc thay đồ trực tiếp trên tài khoản; không tiêu hao vật phẩm người chơi.
