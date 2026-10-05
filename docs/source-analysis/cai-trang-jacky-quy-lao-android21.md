# Ghi chú SQL: Jacky Chun, Quy lão Kamê, Android 21 Tiến Sĩ

Ngày kiểm tra: 04/10/2026 (Asia/Bangkok).

## Chỉ số người dùng cung cấp — bộ đã triển khai

Các chỉ số dưới đây được người dùng xác nhận. Bộ mục tiêu đã được áp dụng trong SQL và JAR. Các mục khảo sát ở cuối tài liệu là dữ liệu trước triển khai, giữ lại để đối chiếu.

| Vật phẩm | ID | Sức đánh | HP | KI | Hiệu ứng / chỉ số khác | Option id:param |
|---|---:|---|---|---|---|---|
| Jacky Chun | 711 | +23% | +21% | +21% | x4 chưởng cơ bản mỗi phút; +50% TN/SM cho đệ tử khi sư phụ mặc | 50:23, 77:21, 103:21, 159:4, 160:50 |
| Quy lão Kame | 710 | +22% | +20% | +20% | x3 chưởng cơ bản mỗi phút; +35% TN/SM cho đệ tử khi sư phụ mặc | 50:22, 77:20, 103:20, 159:3, 160:35 |
| Android 21 Tiến Sĩ | 1710 | +24% | +22% | +22% | x5 chưởng cơ bản mỗi phút; +60% TN/SM cho đệ tử khi sư phụ mặc | 50:24, 77:22, 103:22, 159:5, 160:60 |
| Giáp tập luyện cấp 5 | 1869 | Mặc −40%, cởi +50% | +15% | +15% | Giảm 5% sát thương; giáp +50.000; hiệu lực 1000 phút | 77:15, 103:15, 94:5, 47:50000, 9:1000, 251:0 (option đã thêm) |

Mô tả cải trang mục tiêu: `Cải trang thành Jacky Chun`, `Cải trang thành Quy lão Kame`, `Cải trang thành Android 21 Tiến Sĩ`.

Kế hoạch: `docs/superpowers/plans/2026-10-04-costume-options-training-armor.md`. Người dùng bổ sung hiệu ứng giáp cấp 5 **mặc giảm 40%, cởi tăng 50% sức đánh**; thay đề xuất giữ ±50% trước đó.

Dòng cần thêm vào cả **description** và **option** giáp cấp 5: `Khi mặc vào sẽ đánh yếu hơn bình thường 40%, khi cởi ra sẽ tăng sức đánh 50%`. Đã thêm option ID **251**, param **0**, giữ nguyên đầy đủ dòng mô tả trong SQL và client. Cơ chế giáp tính hiệu ứng thật với hai mức riêng, không dùng option hiển thị để cộng lần hai. Tổng bộ mục tiêu thành **21 option**: mỗi cải trang 5, giáp cấp 5 là 6.

### Bổ sung cửa hàng Santa và giới hạn chưởng

Theo yêu cầu mới: Santa có “Cửa hàng hồng ngọc”, chỉ thanh toán ví ruby. Quy lão Kame 710 giá **1000**, Jacky Chun 711 giá **1500**, Android 21 Tiến Sĩ 1710 giá **2000**, giáp cấp 5 1869 giá **2500** hồng ngọc.

Chuỗi điều kiện mua: **710 → 711 → 1710**; phải đang sở hữu món ngay trước đó trong đồ mặc/túi/rương, không tiêu hao món tiền nhiệm. Giáp cấp 5 mua độc lập. Hiệu ứng x3/x4/x5 chưởng của ba cải trang chỉ tác dụng lên **người chơi thật**, không tác dụng lên boss/quái; đánh boss/quái không tiêu hao lượt chưởng này. Các điều kiện chi tiết, kiểm tra loại tiền và ma trận kiểm thử nằm trong kế hoạch.

Trạng thái hiện tại: **đã áp dụng SQL/JAR, server đang chạy và catalog runtime đã xác minh**. Cửa hàng và hiệu ứng đúng bộ chỉ số ở trên; chưa có kiểm thử gameplay đăng nhập trực tiếp.

Nguồn: database MySQL `team2026` theo `SRC/Config.properties`, hai dump `team2026.sql` và `SRC/sql/nro1.sql`, các migration trong `SRC/sql/migrations`. Khảo sát ban đầu chỉ đọc SQL; ngày 04/10/2026 đã cập nhật theo phê duyệt người dùng. Phần khảo sát dưới đây được giữ như lịch sử trước triển khai.

## Ba cải trang được yêu cầu

| Cải trang | ID item_template | NAME trong SQL | level | icon_id | head | body | leg |
|---|---:|---|---:|---:|---:|---:|---:|
| Jacky Chun | 711 | Cải trang | 1 | 3527 | 356 | 357 | 358 |
| Quy lão Kamê | 710 | Cải trang | 1 | 5175 | 554 | 555 | 556 |
| Android 21 Tiến Sĩ | 1710 | CT Android 21 Tiến Sĩ | 0 | 14241 | -1 | -1 | -1 |

Ba cải trang cần kiểm tra là **711, 710, 1710** theo đính chính của người dùng. ID 678 được giữ ở bảng tra cứu biến thể bên dưới.

## Tất cả thông tin mẫu và các biến thể liên quan

Cả 9 mẫu có chung các giá trị: `TYPE=5`, `gender=3`, `is_up_to_up=0`, `power_require=0`, `gold=0`, `gem=0`. Các mã ghi nguyên bản SQL. `gold/gem` tại item_template không đủ để xác định giá bán shop. Không suy luận level thành độ hiếm.

| ID | NAME nguyên bản | description nguyên bản | level | icon_id | part | head | body | leg |
|---:|---|---|---:|---:|---:|---:|---:|---:|
| 711 | Cải trang | Cải trang thành Jacky Chun | 1 | 3527 | -1 | 356 | 357 | 358 |
| 710 | Cải trang | Cải trang thành Quy lão Kamê | 1 | 5175 | -1 | 554 | 555 | 556 |
| 678 | Cải trang | Cải trang thành Android 21 | 4 | 6400 | -1 | 687 | 688 | 689 |
| 976 | Số 9 Quy lão Kamê | VPSK | 1 | 9395 | -1 | 1043 | 1044 | 1045 |
| 1385 | Cải trang Thầy giáo Quy Lão | VPSK | 0 | 11844 | -1 | 1314 | 1315 | 1316 |
| 1602 | CT Android 21 kid | VPSK | 0 | 13430 | 1485 | 1485 | 1486 | 1487 |
| 1708 | CT Android 21 Thân Thiện | Đi cùng Android  21 Evil+5% giảm sát thương | 0 | 14186 | -1 | -1 | -1 | -1 |
| 1709 | CT Android 21 Evil | Đi Cùng Android  21 Thân Thiện +10% chí mạng | 0 | 14194 | -1 | -1 | -1 | -1 |
| 1710 | CT Android 21 Tiến Sĩ | VPSK | 0 | 14241 | -1 | -1 | -1 | -1 |

`head/body/leg=-1` của 1708, 1709, 1710 là giá trị thật trong database và cả hai dump. SQL này không cung cấp bộ hình dáng cho ba ID đó. Hai khoảng trắng trong description Android 21 được giữ nguyên ở file JSON.

## Toàn bộ option: kết quả kiểm tra trong SQL

**Chưa tìm thấy bộ option `(option_id, param)` gắn với bất kỳ mẫu nào trong 9 ID. Đây là thiếu dữ liệu option trong các nguồn đã kiểm tra, không phải kết luận cải trang không có tác dụng trong game.**

| Nguồn kiểm tra | Kết quả |
|---|---|
| item_template | Không có cột options / option_id / param; chỉ chứa thông tin mẫu. |
| item_shop → item_shop_option → item_option_template | Không có item_shop.temp_id thuộc 9 ID; phép nối không trả về option cho các cải trang. |
| shop_ky_gui.itemOption | Không có item_id thuộc 9 ID. |
| giftcode.detail | Không có vật phẩm thuộc 9 ID. |
| player | Không thấy vật phẩm thuộc 9 ID trong items_body, items_bag, items_box, items_box_lucky_round, items_daban, pet, data_item_event. |
| data_badges | Database không có idItem thuộc 9 ID. |
| Hai dump SQL | Cùng thông tin 9 mẫu, không thấy cấu hình shop hoặc vật phẩm JSON chứa option của chúng. |
| SRC/sql/migrations | Không tìm thấy tên Jacky Chun, Quy lão hoặc Android 21. |

Database đang chạy có 2 player và 1 giftcode lúc kiểm tra; mỗi dump có 1 player. Không đưa dữ liệu tài khoản vào ghi chú.

Hai mô tả có ghi hiệu ứng:

- **1708 — Android 21 Thân Thiện:** đi cùng Android 21 Evil, +5% giảm sát thương.
- **1709 — Android 21 Evil:** đi cùng Android 21 Thân Thiện, +10% chí mạng.

Đây là văn bản description; SQL chưa có option_id/param hoặc bằng chứng cơ chế thực thi. Không tự quy đổi thành option cụ thể.

`item_option_template` là danh mục tên option toàn server, không xác định option thuộc cải trang nào. ID dòng item_shop, item_shop_option, part hoặc player trùng số với item_template không có nghĩa chúng là cùng vật phẩm.

Nếu option được tạo lúc mở hộp, nhận thưởng, mua hoặc biến hình trong mã Java, cần kiểm tra riêng mã nguồn; SQL hiện tại chưa đủ để ghi bộ option đó.

## Vị trí mẫu trong hai dump

| ID | team2026.sql dòng | SRC/sql/nro1.sql dòng |
|---:|---:|---:|
| 711 | 5451 | 5418 |
| 710 | 5450 | 5417 |
| 678 | 5418 | 5385 |
| 976 | 5716 | 5683 |
| 1385 | 6125 | 6092 |
| 1602 | 6342 | 6309 |
| 1708 | 6448 | 6415 |
| 1709 | 6449 | 6416 |
| 1710 | 6450 | 6417 |

## Dữ liệu kèm theo

- `cai-trang-jacky-quy-lao-android21.json`: đủ mọi cột mẫu, vị trí dòng trong dump, dữ liệu part (ảnh và offset), head_avatar tìm được và kết quả kiểm tra option.
- `cai-trang-jacky-quy-lao-android21-readonly.sql`: SELECT có thể chạy lại để đối chiếu thông tin và option shop / ký gửi.

## Bổ sung: chỉ số ba cải trang và giáp tập luyện cấp 5

Đã kiểm tra thêm mã nguồn `SRC/src` ngày 04/10/2026. `ItemService.createNewItem` không tự gán bộ HP/KI/sức đánh cho ba ID cải trang; `createItemFromItemShop` sao chép option của bản shop; `getListOptionItemShop` lấy option từ shop. `NPoint` cộng chỉ số từ `item.itemOptions`. Không tìm thấy bộ option gán riêng cho 1710, 710, 711 trong mã nguồn hiện tại.

| Vật phẩm | ID | Sức đánh | HP | KI | Chí mạng / chỉ số khác |
|---|---:|---|---|---|---|
| Jacky Chun | 711 | Chưa có dữ liệu option | Chưa có dữ liệu option | Chưa có dữ liệu option | Chưa có dữ liệu option |
| Quy lão Kamê | 710 | Chưa có dữ liệu option | Chưa có dữ liệu option | Chưa có dữ liệu option | Chưa có dữ liệu option |
| Android 21 Tiến Sĩ | 1710 | Chưa có dữ liệu option | Chưa có dữ liệu option | Chưa có dữ liệu option | Chưa có dữ liệu option |
| Giáp tập luyện cấp 5 | 1869 | Mô tả: mặc -50%, tháo +50%; hàm hiệu ứng trong JAR hiện tại trả 0% cho ID này | Không thấy option | Không thấy option | Không thấy bộ option riêng |

Không gán các số phần trăm thường gặp ở server khác cho ba cải trang. Không có dữ liệu không đồng nghĩa chỉ số bằng 0. Điều kiện đổi Jacky Chun tại `ShopService.java:730` yêu cầu người chơi có Quy lão Kamê; đây không phải option chỉ số.

### Thông tin Android 21 Tiến Sĩ đã kiểm tra lại

- ID **1710**, NAME **CT Android 21 Tiến Sĩ**, description **VPSK**.
- `TYPE=5`, `gender=3`, `level=0`, `icon_id=14241`.
- `part=-1`, `head=-1`, `body=-1`, `leg=-1`.
- `is_up_to_up=0`, `power_require=0`, `gold=0`, `gem=0`.
- Đã truy vấn lại database: không có bản shop, option shop, ký gửi hoặc data_badges gắn với ID 1710. Không tìm thấy bản vật phẩm ID 1710 trong kho người chơi hoặc giftcode.
- Không tìm thấy chỗ gán option riêng cho ID 1710 trong mã nguồn `SRC/src`. Chưa xác định được con số sức đánh, HP, KI, chí mạng hoặc option đặc biệt của bản Tiến Sĩ.
- Hiệu ứng mô tả của Android 21 Thân Thiện/Evil thuộc ID 1708/1709; không gán chúng cho bản Tiến Sĩ 1710.

### Thông tin giáp tập luyện cấp 5

- ID: **1869**; NAME: **Giáp tập luyện cấp 5**.
- description nguyên bản: `Mặc giảm 50% sức đánh, tháo ra tăng 50% sức đánh`.
- `TYPE=32`, `gender=3`, `level=0`, `icon_id=16897`.
- `power_require=1500000` (1.500.000 sức mạnh); `gold=0`, `gem=0` trong mẫu.
- `part=-1`, `head=-1`, `body=-1`, `leg=-1`, `is_up_to_up=0`.
- Vị trí dump: `team2026.sql:6609`, `SRC/sql/nro1.sql:6576`.
- Không có bản shop hoặc bản ký gửi ID 1869; không thấy vật phẩm này trong các kho người chơi / giftcode đã kiểm tra. Chưa xác định được bộ `(option_id, param)` và thời lượng mặc định.
- Option **9** có tên `Hiệu lực trong # phút`; đây là option mà cơ chế giáp tập luyện dùng để kiểm tra thời gian hiệu lực, không phải bằng chứng ID 1869 đã có option 9 với giá trị cụ thể.

### Hiệu ứng giáp cấp 5 chưa khớp mô tả

Đã đọc bytecode `nro.models.services.ItemService` trực tiếp từ `SRC/20.jar`, đối chiếu với mã nguồn:

- `getPercentTrainArmor` (`ItemService.java:390`) chỉ xử lý 529/534 → 10%, 530/535 → 20%, 531/536 → 30%, 1716 → 40%; **thiếu 1869**, nên trả **0**.
- `isTrainArmor` (`ItemService.java:412`) cũng **thiếu 1869**, nên trả **false**.
- `NPoint.setDameTrainArmor` (`NPoint.java:686`) dùng `getPercentTrainArmor` để giảm sức đánh lúc mặc và tăng lúc tháo khi option 9 còn hiệu lực. Vì hàm trả 0 cho 1869, phần thưởng/phạt sức đánh riêng của giáp này hiện là 0% theo mã xử lý đã kiểm tra.
- `EffectSkin.updateTrainArmor` (`EffectSkin.java:446`) dùng `isTrainArmor` để giảm thời gian hiệu lực khi giáp trong túi/rương; ID 1869 chưa được nhận diện tại bước này.

Kết luận dựa trên SQL, mã nguồn và bytecode; chưa thử trang bị trực tiếp trong game. Chỉ ghi nhận vấn đề, chưa sửa hoặc thay JAR.

SHA-256 JAR đối chiếu: `1AC383513E7866BB16838E42EE16DA078DE510F50B6119FE2316AA51E1F5FF75`.

Dữ liệu đối chiếu: `giap-tap-luyen-cap5-audit.json`; bytecode: `.analysis/armor5-check/ItemService.bytecode.txt`.

## Hồ sơ triển khai đã xác minh

- Migration: `SRC/sql/migrations/20261004_costume_training_armor_options.sql`; mặc định và shop đều đủ 5/5/5/6 option.
- Mô tả giáp có đủ dấu `%` cuối dòng; SQL đã mở rộng `description` lên VARCHAR(255).
- Android 21 Tiến Sĩ: head/body/leg **1603/1606/1607**, avatar **14241**, đã kiểm tra ảnh và frame; bộ 1710/1713/1714 trước đó là candidate sai.
- Hiệu ứng 159 chỉ nhân chưởng PvP hợp lệ giữa người chơi thật; không nhân boss/quái/pet/bot/NPC, không tiêu lượt khi trượt hoặc sát thương nhận bằng 0.
- Giáp 1869 mặc giảm 40%, cởi tăng 50% trong thời gian còn hiệu lực; timer không tự đầy khi restore. Bonus cởi yêu cầu vẫn sở hữu giáp trong túi/rương.
- JAR hiện tại: `SRC/20.jar`; SHA-256 `63c018179b6a7f78aa2ce88b879e4e5c86666ea468af4900553e776d73d9b6f0`.
- JAR cũ: `SRC/jar-backup/20-before-costume-santa-20261004-214921.jar`; SHA-256 `1ac383513e7866bb16838e42ee16da078de510f50b6119fe2316aa51e1f5ff75`.
- Xác minh: 46 bộ Java đạt trên source và JAR; SQL thử nghiệm/live đạt; Unity 950 kiểm tra đạt; catalog đọc trực tiếp trong JVM và cổng 14445 đạt. Gameplay đăng nhập trực tiếp chưa thực hiện.

## Cập nhật giáp gọn và cùng tab cải trang

Đã chuyển giáp cấp 5 (1869) vào tab **Cải trang** cùng 710/711/1710; bỏ tab giáp riêng đã trống. Giá vẫn 2.500 hồng ngọc; đủ sáu option và hiệu ứng như cũ. Tooltip Game1/Game2 hiện tên giáp, lời giải thích mặc giảm 40%/cởi tăng 50% đúng một lần, tiếp theo các chỉ số và thời gian còn lại; giáp hiển thị 50.000. Không ép thời gian về 0 theo ảnh mẫu.

Đã backup hai file Panel và dữ liệu cửa hàng, kiểm thử Unity 16 kiểm tra, kiểm thử JDBC SQL mua bốn món cùng tab và migration lặp an toàn. Server đã bảo trì lưu dữ liệu, khởi động lại và xác minh shop trong JVM thật. Không thay JAR trong lần này. Hồ sơ: `.analysis/armor-compact-20261004/deployment.json`, `client-manifest.json`, `runtime-shop-status.txt`; migration: `SRC/sql/migrations/20261004_santa_armor_same_tab.sql`.

## Nâng trần thời gian tích lũy giáp cấp 5 — 05/10/2026

Giáp 1869 vẫn có 1.000 phút khi tạo/mua mới. Khi đang mặc và đánh quái, cơ chế luyện tập hiện có cộng 1 phút mỗi 60 giây có hoạt động đánh trong 30 giây gần nhất; trần riêng cấp 5 đã tăng lên **10.000 phút**. Đứng yên không tích thêm; tháo vào túi/rương vẫn giảm thời gian và hưởng +50% sức đánh khi còn hiệu lực. Các giáp khác giữ trần 1.000 phút. SQL, giá và sáu option không đổi; không nạp lại thời gian của giáp đã có.

Kiểm thử RED tái hiện bị chặn ở 1000; GREEN 46 bộ trên source và JAR. Kiểm tra trực tiếp JVM bằng vật phẩm/player chưa đăng ký xác nhận mặc luyện tập tăng 9999→10000 và không vượt trần; không sửa player live. Server đang nghe cổng 14445.

JAR hiện tại SHA-256 `beed6156faa02d3a6ba89586e55bb544d61a8a5fdbda1c66abdf38524cc20655`; bản trước cập nhật: `SRC/jar-backup/20-before-armor-cap-10000-20261005-080648.jar`, SHA-256 `63c018179b6a7f78aa2ce88b879e4e5c86666ea468af4900553e776d73d9b6f0`. Hồ sơ `.analysis/armor-cap-10000-20261004/deployment.json`, `red.log`, `combined-test-results.json`, `runtime-armor-status.txt`.
