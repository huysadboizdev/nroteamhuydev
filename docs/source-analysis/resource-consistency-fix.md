# Đồng bộ source, DB, SQL và res — 02/10/2026

Đã cài bản sửa vào `SRC/20.jar`. Server đã dừng trước khi sửa DB/thay JAR trong lần triển khai đầu. **Cập nhật sau đó: server đã chạy lại và sửa thêm lỗi chỉ mục template**, xem [kiểm tra runtime](resource-runtime-check.md). Chi tiết hash/phiên bản dưới đây thuộc bản sửa dữ liệu đầu; chưa thử chơi bằng client thật.

## Những phần đã sửa

- Part 1999: bổ sung frame rỗng thứ ba, giữ ảnh 15288 và offset cũ. Binary mới có 2.099 part, 88.125 byte, giải mã hết và đối chiếu từng frame với DB.
- Sửa dòng ID 1949 chứa bộ icon 21326–21338 thành ID 1919. Dòng 1949 chứa bộ 16507–16519 được giữ lại. ID hiện liên tục 0–2098, không trùng.
- Cả hai luồng dựng part của Manager dùng `ORDER BY id` và writer chung: kiểm ID, loại, số frame, phạm vi icon trước khi thay file bằng thao tác atomic. Dữ liệu lỗi không ghi đè file cũ.
- NPC Bunma Rực Rỡ dùng bộ 1380/1381/1382 từ item 1476, avatar 12356 từ head tương ứng.
- NPC Heart được ẩn theo yêu cầu người dùng. Giữ slot ID 108 với bộ rỗng có sẵn 1968/470/471 và avatar 2955, tránh làm lệch chỉ mục NPC của client. `Map.initNpc` không tạo NPC 108. Muốn bật lại cần khôi phục part/avatar gốc, đổi DB và bỏ điều kiện ẩn.
- NPC Khá Bảnh: khôi phục head 1065 và avatar 9494 tương ứng bộ body/leg 1066/1067; head 2028 cũ thực tế là part thân.
- Item 1593 Goku SSJ God: head 1467 giữ nguyên, sửa body/leg thành 1471/1472 đúng loại và bộ ảnh hiện có.
- Ba reader tile của Manager, Map, MapService dùng chung định dạng hai byte kích thước unsigned rồi tới lưới tile. Chấp nhận một byte footer của 34 file hiện có; từ chối file thiếu dữ liệu. Map không có tile hợp lệ không được khởi tạo; các yêu cầu vào map không tồn tại trả về null theo luồng chặn chuyển map hiện có.
- Map 116: template khớp hoàn toàn map 50 ở mọi cột trừ ID, nên khôi phục tile từ map 50. Không sửa tên/địa hình map 78 bằng dữ liệu đoán.
- Sửa helper nền về `data/map/item_bg_map_data/`.
- Bổ sung **3.203 file zoom** từ PNG cùng ID/tên đã có: icon 1.741, res 1.428, effect 20, ảnh theo tên 10, nền 4. Dùng nearest-neighbor, giữ alpha, không ghi đè file đã tồn tại. Đây là các biến thể được scale từ ảnh thật, cần kiểm tra hình/frame trên client. Có x1/15359.png của Bình Kilis.
- Tăng phiên bản data 9→10, map 2→3, item 9→10, res 1→2 để client tải lại dữ liệu. Kiểm thử xác nhận cả bốn phiên bản trong JAR cuối.
- Đồng bộ toàn bộ bốn bảng `part`, `npc_template`, `item_template`, `map_template` trong `team2026.sql` và `SRC/sql/nro1.sql` với DB sau sửa. Không import toàn bộ dump vào DB, không sửa dữ liệu tài khoản/nhân vật đang chạy.

## Dữ liệu gốc còn thiếu

- Map **78, 95, 101, 121, 125, 130, 134** vẫn chưa có tile; bị chặn vào. Template map 78 hiện có dấu hiệu copy làng Aru, chưa đủ bằng chứng để sửa thành địa hình Fize đúng.
- **616 ID icon trong part hiện tại thiếu file ở cả bốn zoom**. Không thay bằng ảnh khác hoặc PNG giả. Con số này chỉ tính part DB hiện tại, khác phạm vi 633 ID của audit cũ. Một phần icon có thể nằm trong ảnh nhúng của client; chưa xác minh đầy đủ ánh xạ atlas.
- Ba ảnh `aura_999_999`, `mount_42_0`, `mount_42_1` chưa có bản gốc. Các mob/effect/nền thiếu cả bốn zoom trong báo cáo cũ cũng chưa tự thay bằng tài nguyên khác.
- Đã khảo sát res nhúng của client Unity trong thư mục bên cạnh và xuất bản phân tích. Không tìm được tile/part Heart cần khôi phục trong chỉ mục đó; không dùng các texture khác phiên bản để ghi đè res đang có.

## Kiểm chứng

- Tái hiện test reader trên JAR cũ: map 0 nhận 24×39 thay vì 52×24.
- JAR mới: **74/74 kiểm thử hồi quy** hiện có pass.
- Hai reader Map/MapService đọc đúng kích thước và từng tile của **180 file**, gồm 34 file có footer. Kiểm tra NPC Heart không được tạo.
- Writer part: thứ tự dòng, số frame, ID trùng/gap, loại sai và giữ nguyên file cũ khi dữ liệu lỗi đều pass.
- Binary part đối chiếu từng ID/type/frame với DB: pass. Hai dump SQL đối chiếu bốn bảng template với DB: pass.
- Kiểm SHA-256, kích thước và giải mã pixel của cả **3.203 PNG mới**: pass.
- SHA-256 JAR cuối đã cài: `f5aaecf4e417a6e82412e2fcb16bc7b032e4022f2f1c382740ea5ef662529ee5`.

## Sao lưu và khôi phục

- JAR cũ: `SRC/jar-backup/20-before-resource-repair-20261002-211843.jar`, SHA-256 `3bee4687ac28ba8ab68ca6cd569c3c98df70d483002b85bcba2218c7ace51af4`.
- Metadata triển khai cuối: `SRC/jar-backup/deployment-resource-repair-20261002-212223.json` và `.analysis/resource-repair/deployment.json`. Bản trung gian trước khi tăng phiên bản item cũng đã được giữ tại `SRC/jar-backup/20-before-resource-repair-20261002-212223.jar`.
- SQL, snapshot template DB và binary part trước sửa: `.analysis/resource-repair/backup-20261002-211026/`.
- SQL đảo sáu thay đổi DB: `.analysis/resource-repair/rollback.sql`. Kiểm tra DB chưa có sửa mới trước khi chạy rollback.
- Danh sách mọi file zoom mới, nguồn và SHA-256: `.analysis/resource-repair/zoom-created.json`. Khi rollback res, chỉ bỏ các file trong danh sách còn đúng hash; map 116 là file tile mới riêng.
- Kết quả xác minh binary/res: `.analysis/resource-repair/verified.json`; log build: `.analysis/resource-repair/build.log`.

Khôi phục phải thực hiện khi server đã dừng bằng bảo trì có lưu dữ liệu. Để khôi phục trọn bộ cần phục hồi JAR, DB template, hai dump SQL, binary part và res tương ứng; phục hồi riêng JAR không đảo các thay đổi DB/res.
