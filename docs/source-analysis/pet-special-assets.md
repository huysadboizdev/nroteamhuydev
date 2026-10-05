# Kiểm tra dữ liệu và tài nguyên đệ tử đặc biệt

Ngày kiểm tra: 2026-10-02. Phạm vi: source, hai bản SQL và tài nguyên trong workspace. Không sửa production, không build, không triển khai, không truy cập DB đang chạy.

Cả Uub (type 2), Kid Beer/Beerus (type 3) và Kid Jiren (type 4) đều có mã nguồn, đủ ba part và đủ ảnh PNG mà các part tham chiếu ở x1–x4. Có một khác biệt dữ liệu Uub giữa hai SQL và một ảnh bình Kilis thiếu ở x1. Các lỗi đổi đệ tử phát hiện trong bản nguồn trước cập nhật đã được sửa trong source hiện tại; báo cáo này không khẳng định bản sửa đã triển khai.

Trong báo cáo này, “đệ tử 2” chỉ nhóm đệ tử chiến đấu đặc biệt Uub/Beer/Jiren, được lưu ở `player.pet` với typePet 2/3/4. `PetService.Pet2()` tạo `NewPet` ở `player.newPet`, là thực thể đi theo có hình chỉ định; đó là một cơ chế khác.

## Đối chiếu từng đệ tử

| Đệ tử | typePet | Part đầu/thân/chân | team2026.sql | SRC/sql/nro1.sql | data/update_data/part | Ảnh part x1–x4 |
| --- | ---: | --- | --- | --- | --- | --- |
| Uub | 2 | 946 / 947 / 948 | Có; icon 11625–11655 và 2955 | Có; icon 8586–8616 và 2955 | Ba part khớp team2026.sql; khác nro1.sql | Đủ và đọc được cả hai bộ icon |
| Kid Beer / Beerus | 3 | 1422 / 1423 / 1424 | Có; icon 12793–12823 và 2954/2955 | Giống team2026.sql | Ba part khớp cả hai SQL | Đủ và đọc được |
| Kid Jiren | 4 | 876 / 877 / 878 | Có; icon 8063–8093 và 2955 | Giống team2026.sql | Ba part khớp cả hai SQL | Đủ và đọc được |

Các part có TYPE 0/1/2 tương ứng đầu/thân/chân và lần lượt 3/17/14 bộ `(icon, dx, dy)`. ID part không phải ID vật phẩm: ví dụ item 1422 trong item_template là một vật phẩm khác, không phủ định part 1422 của Beer.

Chênh lệch Uub là chênh lệch nội dung frame và tọa độ, không phải thiếu part. Không nên coi hai SQL là bản thay thế hoàn toàn tương đương cho hình Uub. Bộ update hiện có khớp team2026.sql ở cả chín part đã kiểm tra.

## Nguồn và đường dẫn phục vụ client

- [Pet.java](../../SRC/src/nro/models/player/Pet.java): `getAvatar()` tại khoảng dòng 853; `getHead()/getBody()/getLeg()` trả các part trên theo type 2/3/4 tại khoảng dòng 885/921/960. Các hiệu ứng/cải trang/biến hình có thể thay hình mặc định.
- [PetService.java](../../SRC/src/nro/models/services/PetService.java): `createUubPet`, `createKidBeerPet`, `createJirenPet` đi qua `createPet`; `createNewPet` đặt tên, typePet 2/3/4, sức mạnh khởi tạo 40 tỷ và 9 ô body. Source hiện tại đặt `limitPower = 0`, tiềm năng 0 và tạo body/skills mới cho pet mới.
- [PlayerDAO.java](../../SRC/src/nro/models/database/PlayerDAO.java) dòng 718 lưu typePet; [MrBlue.java](../../SRC/src/nro/models/database/MrBlue.java) dòng 810 đọc typePet, khoảng dòng 872 bổ sung đủ 9 ô body cho các loại này. Đây là bằng chứng code có đường lưu/đọc, chưa phải kiểm thử đăng nhập thực tế.
- [Manager.java](../../SRC/src/nro/models/server/Manager.java) khoảng dòng 295–325 đọc bảng `part` và ghi `data/update_data/part`: short số part, byte type, từng short icon + byte dx + byte dy. `loadPart()` khoảng dòng 244 cũng có đường ghi tương tự. Truy vấn hiện là `select * from part`, không có ORDER BY; luồng binary ghi theo thứ tự kết quả và không ghi ID riêng.
- [DataGame.java](../../SRC/src/nro/models/data/DataGame.java) dòng 73/88 đọc và gửi nguyên `data/update_data/part`; dòng 302 gửi icon từ `data/icon/x{zoomLevel}/{iconId}.png`. Nếu icon không tồn tại thì `sendIcon` trả về, không có fallback x1 trong hàm này.
- Đường thực tế trong workspace: `SRC/data/update_data/part` và `SRC/data/icon/x1`, `x2`, `x3`, `x4`. Đường source tương đối `data/...` yêu cầu thư mục làm việc runtime tương ứng thư mục `SRC` hoặc một deployment có cấu trúc tương đương; audit chưa xác minh cwd của server đang chạy.

File part hiện có 88.121 byte, header khai báo 2.099 part. Hai SQL có 2.087 dòng part trích xuất. Audit giải mã tới các ID cần kiểm tra và đối chiếu từng frame/offset của đủ chín part; không khẳng định toàn bộ binary đồng nhất với toàn bộ SQL.

## PNG đã kiểm tra

Đã kiểm tra tồn tại, chữ ký PNG, CRC các chunk, giải nén IDAT; sau đó dùng Pillow đọc toàn bộ pixel. Tổng cộng **507 file đọc thành công**, gồm 504 file ảnh part khác nhau tại bốn mức zoom (tính cả hai bộ Uub và hai icon dùng chung), cùng 3 ảnh bình 15359 tại x2/x3/x4.

Không thiếu hoặc lỗi PNG nào trong các icon part của ba đệ tử. Kiểm tra này chứng minh file ảnh hợp lệ; chưa đánh giá trực quan nhân vật, hoạt ảnh và vị trí frame trong client.

## Bình Kilis và luồng đổi đệ tử

Hai SQL đều có `item_template` ID **1795**, tên **Bình hút năng lượng**, mô tả dùng hút Kilis, type 75 và icon **15359**; `item_option_template` ID **250** là `# Kilis`.

Chuỗi chức năng có trong source:

1. [Hirudegarn.java](../../SRC/src/nro/models/mob_bigboss/Hirudegarn.java) khoảng dòng 97 rơi item 1795 với xác suất `20/100`, option 250 ban đầu 0.
2. [Osin.java](../../SRC/src/nro/models/npc_list/Osin.java) tại map 52 kiểm tra có bình trong túi, mở menu bình và đưa tới map 165. Map 165 có menu bùa hỗ trợ; bùa giá 5 ngọc, thêm 10 phút và tối đa 60 phút theo mã hiện tại.
3. [Mob.java](../../SRC/src/nro/models/mob/Mob.java) khoảng dòng 195 kiểm tra map Cadic; xác suất thêm 1 Kilis vào option 250 của bình là `1/333`, có bùa là `10/333`. Đây là tỷ lệ trong code; lời thoại Osin mô tả “gấp đôi” không khớp tỷ lệ 10 lần này.
4. [UseItem.java](../../SRC/src/nro/models/services_func/UseItem.java) gọi `changePetRamdom` khi dùng 1795. Source hiện tại khóa trên player, kiểm tra bình là instance hợp lệ còn trong túi, số lượng > 0, đúng template 1795, option 250 >= 3000 và đệ Mabư type 1 đạt >= 40.000.000.000 sức mạnh. Hàm chọn ngẫu nhiên type 2/3/4, gọi tương ứng `changeUubPet` / `changeKidBeerPet` / `changeJirenPet`, rồi xóa bình khỏi túi. [Item.java](../../SRC/src/nro/models/item/Item.java) dùng `hasOption` kiểm tra `option.param >= minParam`.

Ảnh bình:

| File | Kết quả |
| --- | --- |
| `SRC/data/icon/x1/15359.png` | **Thiếu** |
| `SRC/data/icon/x2/15359.png` | Có, PNG hợp lệ, đọc pixel được |
| `SRC/data/icon/x3/15359.png` | Có, PNG hợp lệ, đọc pixel được |
| `SRC/data/icon/x4/15359.png` | Có, PNG hợp lệ, đọc pixel được |

Thiếu x1 có thể ảnh hưởng client zoom 1 khi cần tải icon bình và chưa có cache; không phải thiếu ảnh cơ thể của đệ tử.

## Phát hiện trong bản cũ và source hiện tại

Trong bản nguồn trước cập nhật, lưu tại `.analysis/pet-reset-before`, `PetService.changeUubPet`, `changeKidBeerPet`, `changeJirenPet` đặt `player.pet = null` rồi đọc `player.pet.gender`: các helper này có lỗi NullPointerException. Helper Jiren còn gọi creator Beer. Luồng 1795 bản cũ gọi trực tiếp creator trên thread riêng và xóa bình trước khi tạo pet hoàn tất. Đây là các phát hiện của bản cũ, không phải mô tả source hiện tại.

Đã đọc lại source hiện tại: ba helper gọi đúng creator của mình, không đọc pet sau khi đặt null. `createPet` tạo hoàn chỉnh pet mới đồng bộ trong khóa player trước khi xử lý pet cũ; sau đó giải hợp thể nếu cần, đưa pet cũ ra khỏi map, dispose và thay `player.pet`. `createNewPet` trả pet mới với bậc mở giới hạn 0, body/skills mới và chỉ số khởi tạo của loại mới. Không kế thừa đồ, kỹ năng, tiềm năng và bậc mở của pet cũ. Thread riêng chỉ gửi lời chào, giữ reference của pet đã tạo và kiểm tra nó vẫn là pet hiện tại trước khi gửi. `changeJirenPet` nay tạo Jiren đúng loại.

Luồng `UseItem.changePetRamdom` hiện gọi các helper đã sửa và giữ khóa player từ kiểm tra điều kiện tới thay pet và tiêu hao bình. Audit này xác nhận cấu trúc source sau sửa; không thực hiện chuyển đổi trong server thật.

## Giới hạn kết luận

Đã xác minh dump SQL, source và tài nguyên workspace, rồi cập nhật phần source theo bản sửa hiện tại. Chưa truy vấn live DB, chưa xác nhận JAR/server hiện chạy đã có bản sửa, client cache/res tích hợp, kết nối tải icon, hiển thị/animation trong client hay thao tác đổi đệ tử thực tế. Thiếu ảnh bình x1 vẫn là phát hiện chưa xử lý. Báo cáo không chứng nhận các mục chưa kiểm tra và không tuyên bố đã triển khai.
