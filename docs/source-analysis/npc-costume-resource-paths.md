# NPC, cải trang và đường dẫn res còn thiếu — 03/10/2026

Kiểm tra DB đang chạy lúc 2026-10-03T07:18:41.292176+07:00. Binary part khớp DB; kiểm tra file trực tiếp sau các lần sửa. Không sửa source/DB/res/JAR và không dừng server trong lần này.

Thiếu file trên server là bằng chứng về path; chưa tự động chứng minh client crash. Nếu client nhúng sẵn đúng icon hoặc đang có cache thì ảnh vẫn có thể hiển thị. Các danh sách dưới đây không dùng kết quả thiếu zoom trước khi đã bổ sung 3.203 file.

| Nhóm | Đã đối chiếu | Có thiếu file/sai part | Icon thiếu khác nhau |
|---|---:|---:|---:|
| NPC | 93 | 0 | 0 |
| Cải trang | 457 | 11 | 328 |
| Pet | 503 | 2 | 1 |
| Thú cưỡi | 55 | 0 | 0 |
| Ngoại hình source | 30 | 2 | 0 |

Một icon có thể dùng bởi nhiều NPC/cải trang; không cộng các cột icon giữa nhóm để suy ra số ảnh khác nhau. `Ngoại hình source` là từng bộ ba trong các mảng Player, không phải item DB.

**Tất cả icon thiếu trong lần đối chiếu này đều thiếu cả x1, x2, x3, x4**, không chỉ thiếu một zoom. Với 11 cải trang, có 328 ID ảnh khác nhau thiếu: ảnh khung đầu/thân/chân, icon vật phẩm và avatar. Một ID có thể đảm nhiệm nhiều vai trò. Ví dụ item1846 cần `SRC/data/icon/x1/21308.png`, `x2/21308.png`, `x3/21308.png`, `x4/21308.png`; cả bốn chưa có.

## NPC

Các part/icon/avatar được tham chiếu bởi **93 NPC hiện tại đều có file ở cả bốn zoom**. Không tìm thấy lỗi path mới ở nhóm này. Đây là kiểm tra file và cấu trúc, chưa đánh giá hình vẽ/frame bằng client thật.

Heart ID 108 vẫn ẩn theo yêu cầu trước đó. Bộ part gốc 2109–2111 chưa có; template hiện dùng part rỗng hợp lệ. Đây là thiếu bộ ngoại hình gốc đã biết, không được tính là lỗi path mới của template rỗng.

Bunma Rực Rỡ ID 110 đang dùng bộ 1380/1381/1382 đã sửa. Các NPC render đặc biệt (Rương đồ, Đậu thần, Rồng, NPC 77/82...) không bị kết luận sai loại part chỉ từ trường head/body/leg.

## Cải trang (item type 5)

| Item ID | Tên trong DB | Part đầu/thân/chân | Icon thiếu | Loại ảnh thiếu |
|---:|---|---|---|---|
| 1841 | Cải trang Gốc | 1939/1942/1943 | 21488, 21491, 21494–21524 | body, head, head_avatar, icon vật phẩm, leg |
| 1842 | Cải trang Gốc | 1934/1937/1938 | 21451–21473, 21476, 21479–21487 | body, head, head_avatar, icon vật phẩm, leg |
| 1843 | Cải trang Gốc | 1929/1932/1933 | 21412–21416, 21419, 21422–21433, 21436–21450 | body, head, head_avatar, icon vật phẩm, leg |
| 1844 | Cải trang Gốc | 1925/1927/1928 | 21377, 21379, 21381–21411 | body, head, head_avatar, icon vật phẩm, leg |
| 1845 | Cải trang Gốc | 1920/1923/1924 | 21340, 21343, 21346–21376 | body, head, head_avatar, icon vật phẩm, leg |
| 1846 | Cải trang Gốc | 1917/1918/1919 | 21308–21339 | body, head, head_avatar, icon vật phẩm, leg |
| 1847 | Cải trang Gốc | 1912/1915/1916 | 21269, 21272, 21275–21305 | body, head, head_avatar, icon vật phẩm, leg |
| 1848 | Cải trang Gốc | 1907/1910/1911 | 21232, 21235, 21238–21268 | body, head, head_avatar, icon vật phẩm, leg |
| 1849 | Cải trang Gốc | 1901/1902/1903 | 20444–20446, 20452–20464, 20466–20478, 20480–20481 | body, head, head_avatar, icon vật phẩm, leg |
| 1850 | Cải trang Gốc | 1905/1902/1903 | 20448–20450, 20452–20464, 20466–20478, 20480–20481 | body, head, head_avatar, icon vật phẩm, leg |
| 1851 | Broly Nes | 1898/1899/1900 | 20097–20127 | body, head, head_avatar, icon vật phẩm, leg |

Tên như “Cải trang Gốc” có nhiều item ID khác nhau; dùng ID để phân biệt. `head/body/leg = -1` có thể có nghĩa dùng ngoại hình mặc định, không mặc nhiên là thiếu part. Item 210 “Avatar” là entry legacy, không được xem là cải trang ba part chuẩn.

## Pet và thú cưỡi

| Nhóm | Item ID | Tên | Thiếu icon / sai part |
|---|---:|---|---|
| Pet | 673 | abcd | 15313  |
| Pet | 1040 | Pet Thỏ mập |  chân dùng part 1093 TYPE1, cần TYPE2 |

Pet dùng renderer riêng; ví dụ item 1040 Pet Thỏ mập có body/leg cùng 1093 TYPE1. Dòng sai loại part ở nhóm Pet là cần đối chiếu renderer, không khẳng định crash như cải trang TYPE5.

## Ngoại hình từ source

| Bộ | Part đầu/thân/chân | Thiếu icon / sai part |
|---|---|---|
| idOutfitHalloween[1][0] | 2082/2085/2086 |  đầu dùng part 2082 TYPE1, cần TYPE0 |
| idOutfitHalloween[1][2] | 2083/2085/2086 |  đầu dùng part 2083 TYPE2, cần TYPE0 |

`idOutfitHalloween[1][0]` là biến hình Halloween nhóm 1 cho Trái Đất; `[1][2]` là cùng nhóm cho Xayda. Head2082 là TYPE1, head2083 là TYPE2; đây là lỗi tham chiếu loại part, không phải file PNG thiếu. Kiểm tra mảng tại `SRC/src/nro/models/player/Player.java:655` và luồng getHead tại dòng842.

Nhánh Gogeta trong Player.getHead/getBody/getLeg còn trả part **2100/2101/2102**, ngoài bộ 0–2098. NPoint chỉ bật isGogeta với cặp item **2133/2134**; hai item này không có trong DB hiện tại. Do đó đây là nhánh thiếu dữ liệu **chưa thấy kích hoạt được từ item DB hiện tại**, không ghi là crash đang xảy ra.

## Ảnh theo tên

| Tên | Zoom thiếu | Item type 23 dùng mount part tương ứng |
|---|---|---|
| aura_999_999 | 1,2,3,4 | Chưa xác định owner trong DB hiện tại |
| mount_42_0 | 1,2,3,4 | Chưa xác định owner trong DB hiện tại |
| mount_42_1 | 1,2,3,4 | Chưa xác định owner trong DB hiện tại |

Không gán `aura_999_999` cho một cải trang nếu chưa có đường nối trong source/DB. `mount_42_0/1` có khai báo nhưng hiện không có item type23 part42; item802 part42 là lồng đèn TYPE11, không phải thú cưỡi.

## Đường dẫn và dữ liệu chi tiết

- Icon: `SRC/data/icon/x{zoom}/{icon_id}.png` (DataGame.sendIcon).
- Ảnh theo tên: `SRC/data/img_by_name/x{zoom}/{name}.png` (DataGame.sendImageByName).
- [CSV từng file thiếu, tên NPC/item, part và frame](npc-costume-resource-audit/missing-paths.csv).
- [JSON đối chiếu toàn bộ NPC/cải trang/pet/thú cưỡi](npc-costume-resource-audit/audit.json).
- [Snapshot SELECT template hiện tại](npc-costume-resource-audit/live-templates.json).

JAR đang dùng SHA-256 `fdf48ab1643764092a0c3b9cf7dc03b74bcf7e16cb64a2cfa9fed5532cafcb2f`.
