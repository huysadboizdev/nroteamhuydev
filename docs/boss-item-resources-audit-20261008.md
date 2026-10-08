# Boss, item và resource — kiểm tra chỉ đọc 2026-10-08

Đối chiếu SQL live team2026, source server, JAR đang dùng và hai namespace Game1/Game2 của Unity client. Không sửa SQL/source/JAR/res, không dừng hoặc khởi động server. JAR SHA-256: `13d4a90762111edce5dc683c49655fcaaefa1c89bbf4bcfed98331c6d603a030`.

## Kết quả toàn catalog

- SQL live: 2.001 item (0–2000), 2.099 part (0–2098), 840 dòng item_shop, 146 img_by_name, 169 map. Mỗi item có một dòng trong `full-item-catalog.csv`, không chỉ đồ Thiên sứ.
- Part binary gửi cho client: 2.099 part giải mã đúng, 88.125 byte, không dư byte, không khác SQL live, không sai số frame. Các lỗi part1999/trùng1949/thiếu1919 của audit cũ đã hết trong dữ liệu hiện tại.
- 144 outfit literal của BossData (bao gồm custom constructor và form), 128 template static đọc trực tiếp từ JAR. Outfit source/JAR khác nhau: 0. Toàn bộ part ID boss tồn tại và icon frame boss có đủ x1–x4; **3 outfit sai loại part theo vị trí head/body/leg**: NGUYETTHAN2058/2059/2060 có type1/2/0; NHATTTHAN2065/2066/2067 type2/0/1; Virut651/778/779 type0/0/1. Đây là sai mapping, không phải thiếu file. Client Char.paintCharBody lấy GameScr.parts theo từng vị trí rồi dùng chỉ số frame thân/chân; cần sửa mapping đúng artwork và thử animation trước khi xác nhận ảnh hưởng hiển thị. Catalog template không chứng minh boss được spawn.
- Rival tạo outfit động từ player.getHead/getBody/getLeg, không phải một bộ ảnh riêng. Luồng này kế thừa đồ và transform của người chơi; `dynamic-boss-outfits.csv` lưu biểu thức.
- Quét **75,368 PNG**: signature, CRC chunk, IEND, kích thước và giải mã pixel. Không có PNG rỗng hoặc giải mã lỗi. 14 PNG dư dữ liệu sau IEND nhưng giải mã pixel được; CSV ghi từng file. `effect/x3/3` là binary 399byte tên trần, không phải route ảnh `ImgEffect_{id}.png`, không tính là PNG hỏng.
- 15.759 icon ID được SQL part/item/skill/avatar/flag/NPC tham chiếu; **633 ID thiếu cả x1–x4**, tương đương 2.532 path. Đây là tham chiếu catalog toàn cục, không đồng nghĩa 633 lỗi boss đang chạy. Bốn folder icon mỗi folder có 16.515 file.
- **12 item thiếu icon**: 673 `abcd`, 1841–1850 `Cải trang Gốc`, 1851 `Broly Nes`. 11 item cuối còn thiếu frame icon outfit. Không item nào trong 12 có dòng item_shop is_sell=1; chưa kiểm tra từng inventory người chơi hoặc reward động nên không kết luận tuyệt đối là không sử dụng. Item673 tên placeholder; 1841–1851 không có literal số riêng trong source (heuristic, không chứng minh không cấp qua dữ liệu).
- Item210 Avatar head17 có SQL part type2 thay vì type0; SANTA_HEAD gán template.head vào player.head. Item1040 Pet Thỏ mập có SQL leg1093 type1 nhưng luồng Pet2 thực tế dùng1092/1093/1094 đúng loại, nên không coi là lỗi vẽ pet hiện tại.
- 3 tên img_by_name thiếu bốn zoom: aura_999_999, mount_42_0, mount_42_1. DataGame.sendImageByName trả độ dài ảnh0; chưa xác nhận người chơi/boss yêu cầu tên đó.

## Đường đi dữ liệu

`Manager.loadPart` đọc SQL có ORDER BY id → `PartDataWriter` kiểm tra ID và số frame rồi ghi `SRC/data/update_data/part` → `DataGame.updateData` gửi file → client GameScr đọc part. Body/leg của item TYPE0/1 dùng template.part; costume TYPE5 dùng head/body/leg. TYPE27 là pet không mặc thông thường; các triplet của loại này chỉ ghi là kiểm tra cấu trúc nếu không có caller thật.

Client cả Game1/Game2 `SmallImage.createImage`: thử GameCanvas.loadImage(`/SmallImage/Small{id}.png`), tiếp theo imageRaw cache ở zoom>1, rồi requestIcon. `ClientResourcePaths.Candidates` thử res/x{zoom} và res gốc, có fallback x4; Image dùng Unity Resources.Load. Server `Controller` nhận request icon → `DataGame.sendIcon` đọc duy nhất `SRC/data/icon/x{session.zoomLevel}/{id}.png`, thiếu file thì return không trả ảnh. Vì vậy thiếu server icon có thể được artwork nhúng/cache che đi; chưa kiểm tra cache thiết bị thật. Các zoom client và server là hai bộ nguồn ảnh khác nhau; không suy ra thiếu file client là thiếu ảnh cuối cùng khi server có thể trả.

Transform ở `transform-outfits.csv`: fusion, thần, Halloween, Mafuba, monkey, stone, socola, Gogeta và mapping pet1040. Có 4 form với part thiếu/sai loại hoặc icon thiếu; chi tiết trong summary JSON. Gogeta isGogeta dùng2100/2101/2102, ngoài SQL part0–2098; cần xác nhận điều kiện activation trước khi đánh giá ảnh hưởng thực tế.

Map và mob chỉ đối chiếu template/path; các map thiếu tile và map boss dùng được lưu `map-resource-paths.csv`, `summary.json`. Dữ liệu SQL live là nguồn chính; không dùng seed cũ để kết luận server đang chạy.

## Bằng chứng và giới hạn

[Tổng hợp JSON](../.analysis/resources-audit-20261008/summary.json), [toàn bộ item](../.analysis/resources-audit-20261008/full-item-catalog.csv), [item có vấn đề](../.analysis/resources-audit-20261008/item-resource-issues.csv), [toàn bộ outfit boss](../.analysis/resources-audit-20261008/all-boss-outfit-configs.csv), [boss mapping sai](../.analysis/resources-audit-20261008/boss-outfit-resource-issues.csv), [toàn bộ part](../.analysis/resources-audit-20261008/full-part-catalog.csv), [icon thiếu](../.analysis/resources-audit-20261008/missing-referenced-icons.csv), [ảnh đã giải mã](../.analysis/resources-audit-20261008/all-resource-images.csv), [transform](../.analysis/resources-audit-20261008/transform-outfits.csv).

Chưa chạy Unity/desktop hoặc phiên client sạch cache; kiểm tra decode không chứng minh sprite đúng mỹ thuật, đúng cắt frame hoặc mọi animation render thành công. Phân loại spawn/reward/đồ bị disable cần kết hợp audit luồng khác; item1015 Siêu rồng giữ template nhưng đã tắt shop/use theo thay đổi trước, không tự coi tài nguyên placeholder của nó là lỗi gameplay.
