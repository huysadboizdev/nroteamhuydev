# Kế hoạch dự thảo: Nâng SKH tại Bà Hạt Mít

> Quy tắc hiện tại theo giải thích mới nhất: mỗi món thành công 50%, xịt 50%, độc lập cho mọi lượt 1–5 món; đủ năm có thể nhận 0–5 SKH. Menu hiện đủ Thường/VIP/Đóng. Xem [kết quả Thường](../../source-analysis/skh-thuong.md) và [kế hoạch đã thực thi](2026-10-02-nang-skh-thuong-restore.md). Các đoạn nhận cố định ba món và câu chưa triển khai bên dưới là lịch sử, không còn dùng cho gameplay.

> Cập nhật nhánh VIP: quy tắc đã được người dùng xác định trong lượt sau. Xem [kế hoạch Nâng SKH VIP](2026-10-02-nang-skh-vip.md). Các câu “VIP chưa có quy tắc/chưa mở” phía dưới là trạng thái lịch sử. Mapping và tác dụng set cũng đã được sửa riêng; xem [tác dụng SKH hiện tại](../../source-analysis/skh-effects.md).

Ngày: 02/10/2026. Trạng thái: quy tắc SKH Thường đã được người dùng xác định; chờ duyệt triển khai kế hoạch tổng thể, chưa sửa source gameplay. VIP chưa có quy tắc.

## Điều chỉnh mới nhất của người dùng — thay thế quy tắc xịt trước đó

- Nâng đủ năm món: nếu hên nhận năm món SKH; nếu xịt vẫn nhận ba món SKH. Không còn áp dụng mô hình 50% độc lập có thể nhận 0–2 món ở lượt năm món.
- SKH đầu ra thuộc hành tinh của đồ thần linh nguyên liệu. Rada dùng chung cần suy ra hành tinh từ các món không phải rada trong lượt; lượt chỉ có rada cần quy tắc riêng đã đề xuất theo nhân vật. Không áp dụng mặc định mọi món theo hành tinh nhân vật khi nguyên liệu thuộc hành tinh khác.
- Bảng ưu tiên loại SKH 30/30/30/5/5 và phí 500 triệu/món vẫn là các quy tắc trước đó, chưa bị người dùng thay đổi.
- Cần xác định lượt năm món có thể ra bốn món không, xác suất nhận ba/bốn/năm món, và cách tính kết quả khi chỉ chọn 1–4 món. Không tự suy ra thuật toán xác suất mới từ quy tắc cũ.
- Các phần bên dưới mô tả quy tắc xịt 50% độc lập cũ cần được viết lại sau khi chốt phân bố mới; không dùng chúng để triển khai.
- Nguồn đã kiểm tra: database cấu hình `team2026` trong `SRC/Config.properties`; bản dump đọc đối chiếu là `team2026.sql` ở root workspace. `SRC/sql/nro1.sql` là bản dump khác. Source tải `item_option_template` và `item_template` từ MySQL; sửa dump không tự cập nhật database đang chạy.

Đối chiếu bổ sung: bảng tạo đồ hiện có **5 lựa chọn SKH cho mỗi hành tinh**: ba bộ cơ bản, Gohan dùng chung và một bộ riêng (Kaio/Nail/Cađic M). Có 13 tên bộ phân biệt vì Gohan dùng chung ba hành tinh. Người dùng đã chọn random độc lập trong đủ năm bộ ở nhánh Thường, ưu tiên ba bộ cụ thể trên mỗi hành tinh. `randomSKHId` có mảng năm phần tử nhưng chỉ chọn index 0–2. Gohan có option 233/234 và có trong nguồn tạo đồ, nhưng chưa thấy xử lý hiệu ứng tương ứng ở SetClothes/NPoint/luồng rơi Mob; phải xác minh và hoàn thiện hiệu ứng nếu đưa vào nâng mới.

| Hành tinh | Năm lựa chọn hiện được khai báo |
| --- | --- |
| Trái Đất | Thiên Xin Hăng, Kirin, Sôngôku, Gohan, Thần Vũ Trụ Kaio |
| Namek | Picolo, Ốc tiêu, Pikkoro Daimao, Gohan, Nail chiến binh Namếc |
| Xayda | Kakarot, Ca Đíc, Nappa, Gohan, Cađic M |

## Mục tiêu và cách hiểu đề xuất

- Tắt rơi trực tiếp trang bị SKH ở ba map đầu sau làng của mỗi hành tinh.
- Thêm menu Nâng SKH tại Bà Hạt Mít ở Đảo Kamê, map 5, gồm Nâng SKH Thường và Nâng SKH VIP.
- SKH Thường nhận 1–5 món thần linh khác loại: áo, quần, găng, giày, rada/nhẫn.
- Đã chốt: tiêu hao từng món thần linh được chọn, đổi thành trang bị SKH cấp đầu của shop làng tương ứng với loại món và hành tinh. Đồ đầu ra lấy chỉ số nền shop, không kế thừa chỉ số, sao hoặc cấp nâng từ đồ thần linh nguyên liệu.
- Mỗi món random độc lập trong năm SKH của hành tinh. Tỉ lệ đề xuất: mỗi bộ ưu tiên 30%, mỗi bộ còn lại 5%. Năm món có thể trùng loại SKH và không bảo đảm cùng một bộ hoặc đủ năm bộ khác nhau.
- Đề xuất chỉ nhận trang bị cùng hành tinh với nhân vật; nhẫn/rada dùng chung lấy hành tinh nhân vật. Quy tắc này cần được người dùng duyệt.
- Đã chốt phí **500.000.000 vàng mỗi món**, thu một lần theo số nguyên liệu trong lượt. Lượt 1/2/3/4/5 món có phí 0,5/1/1,5/2/2,5 tỉ vàng.
- Đã chốt **50% thành công/50% xịt mỗi món**, roll độc lập. Tất cả món nguyên liệu được chọn đều bị tiêu hao, kể cả món xịt. Phí không hoàn theo số món xịt. Chỉ món thành công nhận đầu ra SKH. Quy tắc VIP chưa được xác định.

## 1. Phạm vi map và việc tắt rơi

`MapService.isMapUpSKH` hiện bao gồm:

| Hành tinh | ID map | Tên map |
| --- | --- | --- |
| Trái Đất | 1, 2, 3 | Đồi hoa cúc, Thung lũng tre, Rừng nấm |
| Namek | 8, 9, 11 | Đồi nấm tím, Thị trấn Moori, Thung lũng Maima |
| Xayda | 15, 16, 17 | Đồi hoang, Làng Plant, Rừng nguyên sinh |

Trong `SRC/src/nro/models/mob/Mob.java`, tắt riêng khối sinh trang bị SKH được điều kiện bằng `isMapUpSKH`. Không đặt cả hàm `isMapUpSKH` thành false vì hàm này còn được dùng cho capsule và đồ có lỗ sao. SKH ở map riêng 164 nằm trong nhánh khác, ngoài phạm vi yêu cầu hiện tại. Đối chiếu toàn bộ nơi tạo option SKH trong luồng rơi quái để không bỏ sót nhánh ở chín map trên.

## 2. Menu và luồng người chơi

Trong `SRC/src/nro/models/npc_list/BaHatMit.java`, nhánh map 5:

1. Thêm mục Nâng SKH vào cuối menu gốc, giữ thứ tự các mục hiện có.
2. Mở submenu bằng mã mới trong `SRC/src/nro/models/consts/ConstNpc.java`, tránh trùng mã menu pha lê/chuyển hóa.
3. Submenu có Nâng SKH Thường, Nâng SKH VIP và Đóng.
4. Thường mở tab combine hiện có qua gói -81: hướng dẫn đặt từ một đến năm món thần linh khác loại.
5. VIP hiển thị Chưa mở nếu người dùng duyệt phương án để trống chức năng này.
6. Trang xác nhận hiển thị số món, hành tinh, quy tắc random và tổng phí đã chốt. Chỉ có Nâng SKH và Từ chối; không ghép nút x10/x100 của pha lê vào luồng này.
7. Nâng thành công cập nhật hành trang, số dư nếu có phí và danh sách vật phẩm chọn.

Trang xác nhận phải cảnh báo rõ: mỗi món thành công 50%, xịt mất đồ thần linh, phí 500 triệu/món và tổng phí lượt này. Kết quả lượt nâng thông báo từng món thành công/xịt và tổng số nhận được. Phí 2,5 tỉ vượt `Integer.MAX_VALUE`, vì vậy lưu/tính phí nâng SKH bằng `long` trong lớp nâng mới (`500_000_000L * count`), không ép về `Combine.goldCombine` hiện là int; không cần đổi toàn bộ cấu trúc phí của các chức năng combine khác.

## 3. Kiểm tra nguyên liệu

Tạo lớp `SRC/src/nro/models/combine/NangSKHThuong.java` chịu trách nhiệm kiểm tra và chuyển đổi; điều hướng tại `CombineService.java`.

| Hành tinh | Áo | Quần | Găng | Giày | Rada/nhẫn dùng chung |
| --- | --- | --- | --- | --- | --- |
| Trái Đất | 555 | 556 | 562 | 563 | 561 |
| Namek | 557 | 558 | 564 | 565 | 561 |
| Xayda | 559 | 560 | 566 | 567 | 561 |

Các điều kiện trước khi mở xác nhận và trước khi thực hiện:

- Có 1–5 vật phẩm hợp lệ, không bắt buộc đủ năm.
- Mỗi vật phẩm còn thực sự nằm trong hành trang, có số lượng hợp lệ và được chọn một lần duy nhất.
- Không trùng loại trang bị; hai áo hoặc hai rada bị từ chối dù khác instance.
- Đúng danh sách thần linh và đúng hành tinh theo quy tắc được duyệt.
- Không nhận món đã có SKH để tránh cộng chồng hoặc đổi SKH không chủ ý. Kiểm tra cả nhóm SKH thường và VIP hiện được `SetClothes` nhận diện, không chỉ dựa vào `Item.isSKH()` hiện chỉ quét 127–135.
- Đề xuất từ chối món có hạn sử dụng và các instance bất thường có quantity khác 1.
- Nếu có phí, kiểm tra toàn bộ tổng phí trước khi đổi bất kỳ món nào.

Kiểm tra trùng loại bằng `template.type` 0–4; không kiểm tra trùng chỉ bằng tên hoặc vị trí slot.

### Đồ đầu ra đã đối chiếu shop BUNMA/DENDE/APPULE

| Hành tinh | Áo | Quần | Găng | Giày | Rada |
| --- | --- | --- | --- | --- | --- |
| Trái Đất | 0 — Áo vải 3 lỗ | 6 — Quần vải đen | 21 — Găng vải đen | 27 — Giầy nhựa | 12 — Rada cấp 1 |
| Namek | 1 — Áo sợi len | 7 — Quần sợi len | 22 — Găng sợi len | 28 — Giầy sợi len | 12 — Rada cấp 1 |
| Xayda | 2 — Áo vải thô | 8 — Quần vải thô | 23 — Găng vải thô | 29 — Giầy vải thô | 12 — Rada cấp 1 |

Món thành công: áo thần đổi thành áo cấp đầu, quần thần thành quần cấp đầu, tương tự găng/giày. Nhẫn thần 561 đổi thành rada 12; option SKH của rada dựa vào nhân vật. Số món đầu ra bằng số món thành công. Món xịt bị mất và không có đầu ra. Tạo instance và deep copy các option shop trước khi thêm SKH; `getListOptionItemShop` hiện trả các tham chiếu option dùng chung, không được sửa trực tiếp option lấy từ helper này.

## 4. Random và đồng bộ rada

Bảng SKH Thường đã đối chiếu `item_option_template` và luồng rơi hiện tại, với tỉ lệ đề xuất đáp ứng nhóm ưu tiên của người dùng:

| Hành tinh | SKH | Option tên + hiệu ứng | Tỉ lệ mỗi món |
| --- | --- | --- | --- |
| Trái Đất | Gohan | 233+234 | 30% |
| Trái Đất | Thiên Xin Hăng | 127+139 | 30% |
| Trái Đất | Kirin | 128+140 | 30% |
| Trái Đất | Sôngôku | 129+141 | 5% |
| Trái Đất | Thần Vũ Trụ Kaio | 245+246+247+248 | 5% |
| Namek | Gohan | 233+234 | 30% |
| Namek | Pikkoro Daimao | 132+144 | 30% |
| Namek | Nail | 237+238+239+240 | 30% |
| Namek | Picolo | 130+142 | 5% |
| Namek | Ốc tiêu | 131+143 | 5% |
| Xayda | Ca Đíc | 134+137 | 30% |
| Xayda | Gohan | 233+234 | 30% |
| Xayda | Kakarot | 133+136 | 30% |
| Xayda | Nappa | 135+138 | 5% |
| Xayda | Cađic M | 241+242+243+244 | 5% |

Lấy một số nguyên trong 0–99 cho mỗi món rồi tra các khoảng 0–29, 30–59, 60–89, 90–94, 95–99. Ba khoảng đầu là ba bộ ưu tiên theo thứ tự bảng ở từng hành tinh; hai khoảng cuối là hai bộ ít ra. Tổng xác suất 100%; nhóm ưu tiên chiếm 90%. Không dùng chuỗi nhiều phép thử 50% vì chúng cho xác suất lệch. Tạo mới từng `ItemOption` cho từng món, tránh chia sẻ danh sách option hoặc sửa template chung. Áp dụng cùng bảng cho lượt có một món, hai món hoặc đủ năm món.

Roll thành công và roll loại SKH là hai bước riêng: mỗi món roll thành công trước; chỉ món thành công mới random SKH theo bảng 30/30/30/5/5. Các tỉ lệ SKH là tỉ lệ trong số món thành công, không phải xác suất nhận đồ trên mọi lần nâng. Không bắt buộc mỗi lượt xịt đúng 2 hoặc 3 món; một lượt vẫn có thể thành công cả năm hoặc xịt cả năm tùy các roll độc lập.

“Picolo Daimao” trong yêu cầu được hiểu là bộ Pikkoro Daimao (option 132), phân biệt với bộ Picolo (option 130). “Ca Đíc” ưu tiên là bộ thường option 134; Cađic M option 241 thuộc nhóm ít ra.

Gohan cần có hiệu ứng thật trước khi đưa vào random với 30%: nhận diện/đếm đúng các món Gohan, đối chiếu mức +150% may mắn và +30% vàng rơi ở option 234 với cách tính drop hiện tại, bổ sung đúng chỗ và dùng phép tính long cho vàng. Test phải kiểm tra hiệu ứng khi đủ năm món, không chỉ kiểm tra tên option.

Nhẫn thần linh nguyên liệu là item 561 và rada cấp 1 đầu ra là item 12; cả hai có `template.gender = 3`, dùng chung. Khi đổi sang rada SKH, bắt buộc lấy `player.gender` 0/1/2; không sử dụng cách mặc định gender 3 thành Xayda của một số helper hiện tại. Nhân vật Trái Đất chỉ nhận rada SKH Trái Đất, Namek chỉ nhận rada SKH Namek, Xayda chỉ nhận rada SKH Xayda. Quy tắc áp dụng cả khi chỉ chọn một nhẫn thần linh.

Không gọi trực tiếp `createItemSKH`/`getOptionIdsBySKH` cho luồng mới trước khi xử lý bất nhất: helper hiện ghép 127+140 và 128+139, trái với tên/hiệu ứng trong database. Ưu tiên bảng đúng riêng cho nâng thường để tránh thay đổi các nguồn SKH khác trong lần sửa này.

## 5. Bảo toàn vật phẩm và chống thao tác lặp

1. Khóa trên inventory của nhân vật trong lúc xác nhận chuyển đổi, dùng cùng cơ chế đã áp dụng cho giao dịch vàng.
2. Xác thực lại map/NPC, loại combine, menu xác nhận, danh sách vật phẩm và số dư tại thời điểm thực hiện.
3. Xác định kết quả thành công/xịt của từng món và tạo đủ instance đầu ra cho các món thành công trước khi trừ phí hoặc tiêu hao nguyên liệu; lỗi kỹ thuật ở một món làm cả lượt bị từ chối trước khi thay dữ liệu. Xịt là kết quả gameplay hợp lệ, không phải lỗi kỹ thuật để hoàn phí cả lượt.
4. Đổi đúng các món nguyên liệu được chọn thành các instance đầu ra tương ứng. Chỉ số đầu ra là chỉ số shop cấp đầu cộng option SKH; không sao chép chỉ số, sao, lỗ sao, cấp nâng hay option khác của nguyên liệu.
5. Đề xuất dùng chính các slot của nguyên liệu để đặt instance đầu ra theo giao dịch một lượt, nên túi đầy vẫn làm được. Nếu đầu ra có thể stack, vẫn đặt từng instance số lượng 1 với option riêng vào slot nguyên liệu; tránh thuật toán addItemBag gộp một phần rồi thất bại gây mất đồ. Bảo đảm loại bỏ các tham chiếu nguyên liệu khỏi combine sau commit.
6. Sau mọi lượt hợp lệ đã xử lý, kể cả xịt toàn bộ, vô hiệu xác nhận cũ/clear lựa chọn. Gói tin gửi lặp hoặc hai yêu cầu đồng thời không được trừ phí hay random thêm lần nữa.
7. Ghi nhận kết quả theo cơ chế log sẵn có: nhân vật, template, loại SKH, số món và phí. Không coi gửi gói mạng thất bại là lý do chạy lại giao dịch đã hoàn tất.

Đồ đầu ra thay vào slot món thành công; slot món xịt được làm trống bằng item-null theo quy ước inventory. Phí trừ một lần mỗi lượt hợp lệ, không hoàn theo số món xịt. Không gọi dispose trước khi log/kết quả đã lấy xong thông tin nguyên liệu và không để các tham chiếu combine cũ được dùng lại sau commit.

### Bổ sung sau rà soát source tích hợp

- `sendItemBags` gọi `sortItems` và thay vị trí slot. Không giữ index từ preview để commit: giữ identity nguyên liệu, xác minh chúng vẫn tồn tại và lấy slot hiện tại khi đang khóa. Commit tất cả đầu ra/món xịt trước khi gọi sendItemBags; không gửi/sắp xếp sau từng món. Clear lựa chọn trước khi reopen tab, vì `reOpenItemCombine` đang ghi số lượng tham chiếu cũ nhưng không ghi index cho món không còn trong túi, có thể tạo gói sai độ dài.
- `Trade.startTrade` thay toàn bộ `inventory.itemsBag` từ snapshot và không dùng khóa inventory của nâng. Chỉ khóa trong NangSKHThuong không đủ. Chặn nâng khi đang giao dịch bằng trạng thái của TransactionService và bảo đảm thao tác bắt đầu/chốt giao dịch không chen giữa validation/commit nâng; chọn cơ chế đồng bộ hoặc cờ lượt nâng chung với entrypoint giao dịch, có test tích hợp. Không giả định khóa mới tự bảo vệ các luồng chưa sử dụng khóa.
- `Controller` đọc số món/index bằng readByte, sau đó CombineService lấy trực tiếp phần tử. Kiểm tra count âm/vượt giới hạn và tính đầy đủ của packet trước khi tạo danh sách, kiểm tra index/instance trùng trước khi mở preview. Gói sai không được để lại danh sách combine đã điền một phần hoặc menu xác nhận cũ còn hiệu lực.
- Phân biệt lần preview với lần thực thi: preview không roll kết quả; chỉ roll sau validation thực thi. Reset trạng thái xác nhận khi đổi loại nâng, thay lựa chọn, chuyển map hoặc đóng menu. Request lặp không được dùng nguyên liệu của một preview mới để trả phí cho preview cũ.
- Chỉ số shop phải lấy đúng shop làng tương ứng và deep copy, không fallback sang đồ không option khi thiếu dữ liệu. Lỗi dữ liệu đầu ra phải bị phát hiện trước khi trừ tiền/tiêu hao đồ.
- Rà thứ tự lưu nhân vật trong lúc commit phí và các slot để tránh save đọc snapshot trung gian; cần test save/load của lượt có cả thành công và xịt. Kiểm tra đồng bộ với save hiện có trước khi quyết định thay đổi DAO.
- Với 50% độc lập, xác suất xịt cả năm là 3,125%; đây là kết quả hợp lệ. UI phải nói rõ có thể mất toàn bộ nguyên liệu, không hứa mỗi lượt chỉ xịt 2–3 món. Tỉ lệ nhận mỗi bộ ưu tiên trên mỗi nguyên liệu là 15%, mỗi bộ ít ra là 2,5%, xịt là 50%.

## 6. Tích hợp cụ thể

- `Mob.java`: ngừng nhánh rơi trực tiếp SKH tại nhóm map yêu cầu.
- `BaHatMit.java`: thêm menu ở map 5 và đường xác nhận dành riêng cho nâng SKH.
- `ConstNpc.java`: thêm mã submenu không trùng.
- `CombineService.java`: thêm loại combine, mô tả tab, preview và thực thi; không chuyển tiếp nâng SKH sang startCombineVip x10/x100.
- `NangSKHThuong.java`: kiểm tra nguyên liệu, chọn SKH, gán rada, tính phí sau khi duyệt và commit kết quả.
- `SRC/test/nro/models/combine/NangSKHThuongTest.java`: test logic thật, chỉ thay mạng/nguồn random khi cần kiểm soát kết quả.
- `SRC/test/nro/models/mob/SKHDropTest.java`: xác minh nhánh SKH bị tắt đúng phạm vi và các nhánh rơi khác vẫn còn.

Rà kiểm tra index trong `CombineService.showInfoCombine` vì luồng hiện lấy trực tiếp item theo index client và chưa từ chối chọn lặp. Nâng SKH mới phải kiểm tra danh sách index trước khi dùng; nếu sửa lớp điều hướng chung, bổ sung test tránh ảnh hưởng chức năng combine cũ.

## 7. Kiểm thử và tiêu chí nghiệm thu

- Lượt nâng có 1, 2, 3, 4 và 5 món; số món SKH nhận được bằng số roll thành công. Test cố định roll để tái hiện lượt năm món thành công hai/xịt ba và thành công ba/xịt hai, cùng trường hợp tất cả thành công/tất cả xịt.
- Áo thần đổi thành áo shop cấp đầu đúng hành tinh, găng thần thành găng shop cấp đầu; nhẫn 561 đổi thành rada 12. Đối chiếu toàn bộ 15 ánh xạ đầu ra, chỉ số nền shop và số lượng.
- Cả ba hành tinh random đúng đủ năm SKH được liệt kê; duyệt toàn bộ 100 giá trị roll cho từng hành tinh và xác nhận phân bố chính xác 30/30/30/5/5.
- Mỗi món random độc lập; các kết quả trùng bộ đều hợp lệ. Cùng quy tắc cho lượt có 1–5 món.
- Gohan được nhận diện và hiệu ứng thật được kiểm tra; Kaio/Nail/Cađic M có đủ nhóm option tương ứng.
- Rada đơn lẻ và rada trong bộ đủ năm đều theo hành tinh nhân vật.
- Đúng cặp option 127+139 và 128+140; không cộng nhầm bộ khi mặc năm món.
- Chặn danh sách rỗng, trên năm món, trùng loại, trùng index/instance, index âm/vượt phạm vi, đồ không phải thần linh, đồ khác hành tinh theo quy tắc duyệt, đồ đã SKH và đồ đã được bán/giao dịch sau preview.
- Túi đầy vẫn đổi được qua slot nguyên liệu; từ chối vì dữ liệu không hợp lệ/thiếu phí không làm mất item hoặc tiền. Lượt hợp lệ tiêu hao tất cả nguyên liệu được chọn kể cả món xịt; các món ngoài lựa chọn không đổi.
- Phí là 500_000_000L * count; đủ năm món là 2_500_000_000L. Phí chỉ trừ một lần khi commit lượt nâng, tính bằng long; thiếu phí không tiêu hao nguyên liệu hay random kết quả. Kiểm thử đủ/thiếu một vàng ở cả năm mức phí, xịt toàn bộ vẫn trừ đúng phí và không nhận SKH.
- Duyệt 100 giá trị roll thành công: 0–49 thành công, 50–99 xịt. Xác minh roll loại SKH riêng và xác suất có điều kiện 30/30/30/5/5, không nhầm với 50% thành công.
- Xác nhận lặp/đồng thời chỉ thực hiện một lượt; menu VIP hoặc gói x10/x100 không lọt vào nâng thường.
- Kiểm tra chọn lại vật phẩm sau nâng, lưu/đọc lại nhân vật và mặc đồ để xác minh hiệu ứng SKH.
- Drop tests không phụ thuộc may rủi: dùng nguồn random kiểm soát được hoặc kiểm tra danh sách pool/nhánh SKH đã loại khỏi map.
- Compile toàn bộ source Java 17, chạy lại các test vàng/ước rồng/ép sao hiện có; kiểm tra trực tiếp trong game cho menu, giới hạn số món mà client cho chọn và hiệu ứng hiển thị.

## Các quyết định cần chốt

1. Đã chốt: tiêu hao đồ thần linh, đổi sang đồ SKH cấp đầu ở shop làng theo bảng đầu ra trên.
2. Random độc lập từng món đã được xác định. Tỉ lệ cụ thể được đề xuất là 30/30/30/5/5 theo bảng ưu tiên của người dùng.
3. Đã chốt: 500 triệu vàng/món, 50% thành công độc lập từng món, xịt mất nguyên liệu. Phí thu một lần theo số món và không hoàn khi xịt.
4. VIP hoạt động thế nào, hay tạm hiển thị Chưa mở?
5. Có chấp nhận quy tắc chỉ nhận đồ cùng hành tinh nhân vật, còn rada theo nhân vật?

Kế hoạch này là dự thảo để review, không phải sự chấp thuận triển khai các giả định chưa chốt.
