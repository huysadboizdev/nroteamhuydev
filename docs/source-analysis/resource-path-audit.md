# Dò path và res — 2026-10-02

> Đã xử lý sau audit: xem [báo cáo đồng bộ source/DB/res/JAR](resource-consistency-fix.md). Nội dung dưới đây ghi nhận trạng thái **trước sửa**.

Thời điểm: 2026-10-02T20:39:16.710388+07:00. Phạm vi source/JAR hiện dùng, toàn bộ `SRC/data`, hai dump SQL và truy vấn SELECT các bảng template đang chạy. Không sửa Java/SQL/res/JAR, không dừng server. Không có source/client chạy thử để chứng minh client crash trên từng thiết bị.

## Lỗi ưu tiên cao

### 1. Bộ part bị lệch cấu trúc từ part1999

- SQL đang chạy và cả hai dump: part1999 có TYPE0 nhưng DATA chỉ có 2 frame `[[15288,0,10],[2955,0,0]]`; loại đầu cần 3 frame, thân17, chân14.
- `SRC/data/update_data/part` khai 2.099 part, 88.121 byte. Bộ giải mã đọc tới part2000 gặp type236 tại offset83.538, không phải TYPE0/1/2.
- Kiểm chứng nguyên nhân: chỉ chèn thử một frame 4 byte vào bộ nhớ tại offset83.534 thì giải mã đủ2.099 part, không dư byte. Không ghi file part thử xuống production.
- `Manager.java` ghi mỗi dòng DATA nguyên số frame, không kiểm tra số lượng theo TYPE. `DataGame.updateData()` gửi nguyên bộ này khi cập nhật dữ liệu client. Đây là lỗi dữ liệu có nguy cơ cao làm client lỗi/đóng khi tải part; chưa tái hiện crash bằng client thật.
- NPC77 Quả trứng linh thú và NPC82 Rương Sưu Tầm tham chiếu1999. Lỗi phân tích bộ part có thể ảnh hưởng quá trình tải chung, không chỉ lúc gặp hai NPC.

### 2. SQL trùng/thiếu ID part và NPC vượt giới hạn

- DB hiện tại có2.099 dòng, ID lớn nhất2098 nhưng **ID1949 trùng hai dòng**, không có ID1919. `team2026.sql` giống DB. `nro1.sql` có ID1919 nên không tương đương ở phần này.
- Item1846 `Cải trang Gốc`: chân1919, không có dòng part1919 trong DB/dump team. Binary part không ghi ID riêng và ghi theo kết quả `select * from part` không ORDER BY, vì vậy không thể đồng nhất vị trí mảng với ID khi có gap/trùng/đổi thứ tự.
- NPC108 `Heart`: head/body/leg2109/2110/2111; NPC110 `Bunma Rực Rỡ`:2123/2124/2125. Các part này không có trong hai dump và vượt mảng part hiện tại0–2098; các tham chiếu NPC đã xác nhận bằng SQL đang chạy. Có nguy cơ lỗi truy cập mảng/vẽ NPC nếu client không kiểm tra, chưa có client để chứng minh.
- Các NPC đặc biệt như Rương đồ/Đậu thần/Rồng có cách vẽ riêng; các TYPE khác head/body/leg trong danh sách thô không tự động được coi là lỗi.

### 3. Reader tile map đọc lệch một byte

- File tile có hai byte đầu chiều rộng/chiều cao. `Manager.readTileMap` đọc đúng; `Map.readTileMap` và `MapService.readTileMap` bỏ thêm một byte trước khi đọc kích thước.
- Probe gọi đúng `Map.readTileMap` từ `SRC/20.jar`: nhà21 thực tế32x20 nhưng nhận20x39, `types=null`; nhà22 thực tế32x20 nhưng nhận20x38, `types=null`; nhà23 nhận20x26 thay vì32x20.
- Ít nhất9 map reader yêu cầu số byte lớn hơn file: 21, 22, 24, 25, 30, 39, 40, 138, 183. Các map khác vẫn có thể nhận kích thước sai dù chưa EOF.
- Lỗi EOF bị nuốt nên để trạng thái tile chưa đầy đủ. Hàm vật lý có nhánh bắt lỗi, nên kết quả đã tái hiện là sai kích thước/null tile; không tuyên bố toàn server chắc chắn crash.

### 4. Map có template nhưng thiếu tile

- Thiếu `SRC/data/map/tile_map_data/{id}` cho các map **78, 95, 101, 116, 121, 125, 130, 134**, có trong cả hai dump và đã xác nhận template trong SQL chạy.
- Trong đó78 Lãnh địa Fize và116 Thánh địa Kaio là map có tên; các map còn lại có tên dạng ID, có thể là placeholder, cần kiểm tra luồng đi vào trước khi kết luận mức ảnh hưởng.
- Probe JAR với78/116 cho width/height0, maps/typesnull. Server bỏ lỗi đọc file; client yêu cầu tile không nhận gói data vì `sendMapTemp` return khi thiếu file. Nguy cơ kẹt chuyển map hoặc lỗi địa hình khi đường đi sử dụng map đó.

## Res thiếu hoặc lệch — mức ảnh hưởng phụ thuộc luồng và client cache

- **1505 ID icon** được part/runtime hoặc hai dump tham chiếu thiếu ít nhất một zoom; **633 ID thiếu cảx1–x4**. Theo zoom: x1 thiếu1.502, x2 thiếu664, x3 thiếu664, x4 thiếu636. Đếm ID duy nhất, không phải số dòng SQL/file.
- 56 ID icon của item trong dumpteam thiếu ít nhất một zoom. Ví dụ item1795 Bình hút năng lượng icon15359 thiếux1; cải trang Broly1783 icon14712 thiếux1; Brolyred1784 icon14736 thiếux1; ba Đai lưng1752/1753/1754 thiếu icon14524/14523/14525 ởx4.
- Thư mục `res`: x1 có148 file, x2 có1.413, x3 có1.393, x4 có1.354; x1 thiếu1.272 tên so vớix2. Không có client kèm workspace để biết tên nào đã được nhúng sẵn; đây là lệch bộ res, chưa phải1.272 lỗi crash đã xác nhận.
- Mob thiếu file: 90, 91, 109, 110, 111, 112, 113, 114, 115, 116, 118. Mob118 chỉ thiếux1; các mob90,91,109–116 thiếu cả4zoom. Các mob này không xuất hiện trong trường mobs của map trong dumpteam đã dò; chưa kết luận đang dùng trong game. `requestMobTemplate` không kiểm tra null trước khi ghi và bắt lỗi im lặng, nên request thiếu template không có phản hồi.
- **137 ID ảnh nền** được bảng bg_item_template tham chiếu thiếu ít nhất một zoom; danh sách thô có270 dòng vì tính cả hai dump. Không mặc định tất cả đang được map sử dụng.
- **34 effect data** thiếu ảnh ở ít nhất một zoom; nhiều DataEffect1003–1026/1028 không có ảnh bốn zoom. Cần kiểm tra request/remap `idtemp` trước khi xác định effect đang dùng.
- Ba tên trong bảng img_by_name thiếu cả4zoom: `aura_999_999`, `mount_42_0`, `mount_42_1`. `sendImageByName` vẫn gửi n_frame với độ dài ảnh0; cần kiểm tra aura/thú cưỡi tương ứng khi sửa.
- Helper `Manager.MapBgDataManager.getBgItem` đọc `data/map/item_bg_map_data1/`, thư mục này không có. Luồng gửi map thực tế ở Zone đọc `item_bg_map_data/` có178 file. Không tìm thấy caller helper trong source, nên path lệch này hiện chưa có bằng chứng gây lỗi trong luồng đang dùng.
- Folder `eff_map` có2file; các map khác có fallback ghi số lượng0. Không coi thiếu effectmap của mọi map là lỗi.

## Kiểm tra ảnh

- Quét **72,149 ảnh PNG**, gồm các file res không có đuôi `.png`; kiểm tra chữ ký, CRC từng chunk, IEND và giải mã pixel.
- **Không phát hiện ảnh không giải mã được hoặc file rỗng** trong phạm vi quét. 14ảnh có dữ liệu dư sau IEND; kiểm tra lại bằng Pillow đọc pixel cả14thành công. Đây là ảnh không sạch cấu trúc cuối file, chưa chứng minh là nguyên nhân crash.
- 14file cụ thể ở `audit.json` → png_anomalies; bao gồm icon11120 ở4zoom, aura20/22/23/25 ởx4, một số file resx2/x4. Không tự sửa/cắt dữ liệu dư.
- Chưa giải mã đầy đủ protocol mob/effect/update_data ngoài part và tile, chưa kiểm tra bố trí frame bằng mắt, bộ res nhúng trong client hoặc cache máy người chơi.

## Thứ tự xử lý đề xuất

1. Sửa số frame1999 cùng IDpart1919/1949, bổ sung/điều chỉnh part NPC108/110; dựng lại part từ dữ liệu có thứ tự và kiểm tra toàn bộ trước khi tăng phiên bản data.
2. Sửa reader tile về đúng hai byte kích thước, kiểm tra map21/22/23 và9map EOF; xác định map78/116 có dùng rồi bổ sung tile chuẩn hoặc chặn đường vào khi thiếu.
3. Khôi phục bộ res đúng phiên bản cho các zoom, ưu tiên icon có item/nhân vật đang dùng; không tự tạo file giả để lấp số lượng.
4. Thử client sạch cache ở zoom1–4 với tải data, vào nhà, xem NPC/cải trang và đổi map; thu stacktrace client nếu vẫn crash.

## Bằng chứng

- [Dữ liệu audit](resource-audit/audit.json), [danh sách icon thiếu](resource-audit/missing-icons.csv), [chênh tên file theo zoom](resource-audit/zoom-gaps.csv).
- [Probe part trong bộ nhớ](resource-audit/part-alignment-probe.json), [probe reader JAR](resource-audit/map-reader-probe.log), [SELECT template SQL](resource-audit/live-db-template-results.txt).
- JAR kiểm tra SHA-256 `3bee4687ac28ba8ab68ca6cd569c3c98df70d483002b85bcba2218c7ace51af4`; không thay JAR trong lần audit này.
