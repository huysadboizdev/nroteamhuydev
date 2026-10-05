# Kế hoạch option, cửa hàng hồng ngọc Santa và chưởng PvP

**Trạng thái:** đã triển khai SQL và JAR, server đang chạy; 46 bộ kiểm thử Java trên mã nguồn và JAR đều đạt, Unity đạt 950 kiểm tra. Chưa thực hiện gameplay đăng nhập trực tiếp hoặc chụp UI; các mục nghiệm thu trực tiếp bên dưới còn để mở.

**Mục tiêu:** ID 711, 710, 1710, 1869 có đúng option người dùng cung cấp; bán ở cửa hàng hồng ngọc Santa với điều kiện tiền nhiệm; x3/x4/x5 chưởng chỉ tác dụng lên người chơi thật; hiển thị đúng trên client.

**Kiến trúc đề xuất:** lưu bộ option mặc định trong bảng SQL riêng `item_default_option`, nạp vào catalog server và áp dụng qua ItemService; sao chép đúng bộ này vào item_shop_option cho cửa hàng thật `SANTA_HONG_NGOC`. Cửa hàng dùng giao thức shop đặc biệt hiện có để hiện icon hồng ngọc, nhưng thanh toán bằng ví ruby. Sửa có phạm vi cơ chế chưởng, TN/SM đệ tử và giáp tập luyện; kiểm chứng tài nguyên trước khi gán hình dáng.

**Công nghệ:** MySQL/MariaDB, Java server, Unity C# client Game1/Game2, dữ liệu part và PNG server.

## Bộ option đã chốt theo thông tin người dùng

| Vật phẩm | ID | Option id:param |
|---|---:|---|
| Jacky Chun | 711 | `50:23`, `77:21`, `103:21`, `159:4`, `160:50` |
| Quy lão Kame | 710 | `50:22`, `77:20`, `103:20`, `159:3`, `160:35` |
| Android 21 Tiến Sĩ | 1710 | `50:24`, `77:22`, `103:22`, `159:5`, `160:60` |
| Giáp tập luyện cấp 5 | 1869 | `77:15`, `103:15`, `94:5`, `47:50000`, `9:1000`, `251:0` (option mới dự kiến) |

Mã option và tên tương ứng đã tồn tại trong item_option_template: 50 sức đánh %, 77 HP %, 103 KI %, 159 nhân chưởng mỗi phút, 160 TN/SM đệ tử khi sư phụ mặc, 94 giảm sát thương %, 47 giáp trực tiếp, 9 phút hiệu lực.

Mô tả cần lưu: 711 `Cải trang thành Jacky Chun`; 710 `Cải trang thành Quy lão Kame`; 1710 `Cải trang thành Android 21 Tiến Sĩ` (hiện SQL là `VPSK`).

Giữ tên `Cải trang` của 711/710 và `CT Android 21 Tiến Sĩ` của 1710. Theo bổ sung của người dùng, giáp cấp 5 **mặc giảm 40%, cởi tăng 50% sức đánh**.

Ghi cùng một dòng vào description vật phẩm và tên option mới: **`Khi mặc vào sẽ đánh yếu hơn bình thường 40%, khi cởi ra sẽ tăng sức đánh 50%`**. Option dự kiến `251:0`: database hiện có ID cao nhất 250, ID 251 chưa được dùng. Kiểm tra lại trước migration, không ghi đè option nếu phát sinh trùng. Kiểm tra độ dài cột description và NAME, mở rộng có phạm vi nếu cần để không cắt chuỗi. Option này hiển thị giải thích hiệu ứng; tính toán thực tế do cơ chế giáp phụ trách, không cộng lần thứ hai.

## Kết quả khảo sát và phạm vi kiểm chứng

- Database team2026 chưa có shop/option shop cho bốn ID; item_template không có cột option. Không có căn cứ để chỉ INSERT vào item_option_template rồi coi như đã gắn option cho vật phẩm.
- ItemService.createNewItem không tự gán các bộ option yêu cầu. createItemFromItemShop lấy option từ bản shop. Cần đường mặc định và đường cập nhật vật phẩm cũ.
- NPoint đã đọc 50/77/103/94/47/159/160 từ option vật phẩm đang mặc.
- EffectSkin.updateXChuong và NPoint đã có nhánh nhân sát thương Kamejoko/Antomic/Masenko. Hiện hàm getDameAttack chỉ nhận boolean đánh quái, chưa có kiểm tra mục tiêu người chơi thật nên có thể nhân lên quái/boss. SkillService.playerAttackPlayer cũng xử lý Boss kế thừa Player; không coi mọi đối tượng Player là người chơi.
- NPoint hiện tính bonus TN/SM đệ tử bằng `tn / 100 * (bonus + 100)` rồi cộng tiếp vào TN/SM. Công thức cộng bonus cần thành phần tăng thêm đúng `tn * bonus / 100`, dùng phép tính long an toàn và giữ chính sách các bonus khác. Cần lần theo Service/Pet để xác nhận cả tiềm năng và sức mạnh nhận đúng, không chỉ số hiển thị.
- ItemService.getPercentTrainArmor và isTrainArmor trong mã nguồn và SRC/20.jar thiếu 1869: hiện trả 0/false. Cần tách mức giảm khi mặc (40%) và mức tăng khi cởi (50%) riêng cho 1869; giáp cấp 1–4 giữ giá trị hiện tại.
- ID 1710 có head/body/leg=-1. Candidate 1710/1713/1714 có đúng loại part 0/1/2 nhưng chưa xác nhận hình là Tiến Sĩ.
- Đã kiểm tra 102 ID ảnh gồm icon vật phẩm, avatar và khung part hiện tại/ứng viên: đủ 408 path PNG x1–x4 trên server. Chưa kiểm tra gameplay client; không kết luận đủ tài nguyên client chỉ từ kết quả này.
- Dữ liệu khảo sát: `docs/source-analysis/costume-options-resource-preplan.json`.

## Các cách thực hiện

1. **Đề xuất: catalog mặc định từ SQL + xử lý server.** Có nguồn chỉ số rõ ràng, không cần thêm cửa hàng, hỗ trợ tạo vật phẩm mới và bổ sung vật phẩm cũ. Cần migration và JAR mới.
2. **Chỉ gắn option vào shop.** Sau bổ sung yêu cầu Santa, cách này đủ cho đồ mua mới nhưng không tự giải quyết đồ từ quà/admin hoặc đồ đã có; đề xuất catalog SQL kết hợp bản shop đồng bộ.
3. **Hardcode option trong Java.** Ít cấu trúc SQL hơn nhưng chỉ số không nằm trong SQL như yêu cầu, và mỗi lần đổi chỉ số phải dựng JAR.

## Bước 1 — Chốt dữ liệu và lưu khả năng khôi phục

- [x] Lưu snapshot có thời điểm của bốn item_template, các dòng option liên quan và các vật phẩm người chơi bị tác động; không xuất password/token.
- [x] Kiểm tra schema/database đang dùng theo SRC/Config.properties, so với hai dump và metadata JAR; giữ bản ghi SHA-256.
- [x] Cập nhật ghi chú chỉ số với nguồn là thông tin do người dùng cung cấp, tách với tình trạng thực tế trong SQL.

## Bước 2 — Migration chỉ số trong SQL

**Tệp dự kiến:** `SRC/sql/migrations/20261004_costume_training_armor_options.sql`, hai dump `team2026.sql`, `SRC/sql/nro1.sql`.

- [x] Tạo bảng item_default_option với khóa `(item_id, option_id)` và trường param kiểu INT. Không dùng SMALLINT cho param 50000.
- [x] Thêm option_template mới ID 251 với nguyên văn dòng mặc giảm 40% / cởi tăng 50%; kiểm tra ID chưa dùng, độ dài tên và số template truyền qua byte không vượt 255.
- [x] Upsert 21 cặp option đúng bảng trên; migration chạy lại không tạo bản trùng. Kiểm tra option_id đã tồn tại trước khi áp dụng.
- [x] Cập nhật ba description đúng yêu cầu, giữ các trường ngoài phạm vi.
- [x] Đồng bộ dữ liệu nguồn trong hai dump và chuẩn bị rollback khôi phục snapshot; không xóa catalog của vật phẩm khác.
- [x] SELECT đối chiếu phải trả mỗi cải trang đúng 5 option, giáp cấp 5 đúng 6 option và không trùng. Description và option mới phải đủ nguyên văn dòng yêu cầu.

## Bước 3 — Gắn option vào vật phẩm mới và vật phẩm đã có

**Tệp dự kiến:** `SRC/src/nro/models/server/Manager.java`, `SRC/src/nro/models/services/ItemService.java`; tạo `SRC/src/nro/models/item/ItemDefaultOptions.java` và `SRC/test/nro/models/item/ItemDefaultOptionsTest.java` nếu cần tách catalog.

- [x] Viết kiểm tra bốn ID nhận đúng option, không dùng chung object option giữa các vật phẩm; tạo hai vật phẩm và thay timer một vật phẩm không được làm đổi vật phẩm kia.
- [x] Nạp catalog một lần từ SQL; dùng default ở các đường tạo item. Rà cả đường `new Item`, quà/admin/shop và item load từ DB để không bỏ sót.
- [x] Quy tắc: bộ trên là baseline khi tạo mới. Với vật phẩm cũ chỉ bổ sung option thiếu; giữ option ngoài bộ này và giá trị đã được nâng cấp hợp lệ. Không áp lại timer 1000 phút mỗi lần đăng nhập hay chuyển túi/rương.
- [x] Nếu phát hiện option mục tiêu đã có nhưng khác baseline, ghi audit để phân biệt nâng cấp và cấu hình sai trước khi ghi đè. Cập nhật kho cũ phải có backup và đọc/ghi được JSON lồng chuỗi của body/bag/box/pet.
- [ ] Kiểm tra round-trip SQL → Java → lưu inventory → load lại → packet client; param giáp 50000 phải giữ nguyên.

## Bước 4 — Hiệu ứng thực tế và kiểm tra hồi quy

**Tệp dự kiến:** `NPoint.java`, `EffectSkin.java`, `ItemService.java`, các đường cộng TN/SM trong `Service.java`/`Pet.java` nếu truy vết chỉ ra cần sửa.

- [x] Chưởng: cùng điều kiện, option 159 phải cho hệ số x3/x4/x5 đúng trên một đòn chưởng cơ bản khi đủ 60 giây, chỉ khi người đánh và mục tiêu đều là người chơi thật (`isPl()`), PvP hợp lệ. Không áp cho quái, boss, đệ tử, bot, NPC hoặc đòn đấm; đánh các mục tiêu bị loại không tiêu hao lượt. Lượt kế tiếp không nhân cho đến hết cooldown. Không giữ lượt sau khi tháo/đổi sang cải trang khác; mặc mới bắt đầu chu kỳ 60 giây, không reset bằng đăng nhập/đổi trang bị để lấy lượt ngay.
- [x] Truyền quyền kích hoạt theo mục tiêu từ SkillService.playerAttackPlayer tới NPoint bằng overload/context rõ ràng; các callsite quái hoặc không có mục tiêu mặc định không được phép kích hoạt. Không dùng riêng `isAttackMob=false` làm điều kiện vì boss cũng đi qua đường này. Đặt tiêu hao lượt vào nhánh PvP hợp lệ; kiểm tra đánh trượt/cấm PK không nhân hoặc tiêu hao lượt. Giữ cơ chế isXDame của hiệu ứng khác, không xóa nhánh boss chung để sửa riêng option 159.
- [x] Test ma trận mỗi x3/x4/x5: PvP trúng hợp lệ nhân đúng; quái/boss/bot/pet/NPC không nhân và không mất lượt; PvP kế tiếp vẫn dùng được lượt, sau đó hết lượt; cooldown 59999/60000ms; đổi trang bị không tích trữ lượt.
- [x] Khoanh phạm vi thay đổi cho ba cải trang 710/711/1710. Rà các vật phẩm khác dùng option 159; giữ hành vi của chúng nếu không thuộc yêu cầu này, không vô tình đổi toàn bộ hiệu ứng nhân sát thương trên server.
- [x] TN/SM: với mức gốc 1000, riêng option 160 phải tăng thêm 350/500/600 cho đệ tử tương ứng; sư phụ không nhận bonus này. Tháo cải trang phải hết bonus. Test thêm mức nhỏ dưới 100 để bắt lỗi chia trước rồi nhân, mức lớn để bắt tràn số và cả sức mạnh/tiềm năng qua đường nhận thật.
- [x] Giáp: nhận diện ID 1869 và tách hai mức. Bổ sung hàm lấy mức giảm khi mặc (1869 → 40, cấp khác dùng giá trị hiện có); giữ hàm mức tăng khi cởi (1869 → 50). NPoint dùng đúng hàm ở hai nhánh. Với sức đánh gốc 1000, khi mặc riêng hiệu ứng này còn 600; cởi lúc timer có hiệu lực thành 1500. Bonus HP/KI/giáp/giảm sát thương từ option chỉ cộng lúc mặc; tháo ra không giữ chúng. Option 251 không gây cộng/trừ lần hai.
- [x] Hiệu lực: option 9 bắt đầu 1000 phút; theo cơ chế hiện có tăng khi mặc luyện tập đang đánh, tối đa 10.000 cho cấp 5 (bổ sung 05/10/2026), giảm khi tháo ở túi/rương; hết thời gian không còn bonus +50%. Kiểm tra dùng/chuyển đồ/đăng nhập không tự làm đầy timer.
- [x] Test HP/KI +15%, giáp +50000 và giảm sát thương 5% với công thức thực tế, không chỉ đọc text option. Giữ tương thích giáp cấp 1–4, cải trang khác và bonus đệ tử khác.

## Bước 5 — Res, part và đường dẫn client

**Tệp/phạm vi:** bảng item_template/part/head_avatar, `SRC/data/update_data/part`, `SRC/data/icon/x1..x4`, `SRC/src/nro/models/data/DataGame.java`; client `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1` và `Game2`, `Assets/Resources/res`.

- [x] Bộ 1710/1713/1714 là hình nam Gogeta nên đã loại; xác nhận bộ Tiến Sĩ đúng 1603/1606/1607 từ ảnh icon, PNG và frame client, đã gán vào SQL.
- [x] Đối chiếu head/body/leg, loại part, frame, offset và avatar; tái tạo binary part từ DB nếu có đổi, cập nhật cache version cần thiết theo DataGame hiện tại.
- [x] Rà đường client nhận template/part, tải icon server, atlas và cache ở cả Game1/Game2; path đủ không đồng nghĩa asset đúng hình.
- [ ] Kiểm tra tooltip từng option (đặc biệt 47:50000, 159,160), mô tả mới và hiệu ứng hiển thị.
- [ ] Thử đăng nhập với cache sạch/cũ, mặc/tháo cả ba cải trang, đứng/chạy/bay/đánh/chưởng ở x1–x4, giáp cấp 5, cả ba hệ nhân vật. Điều kiện đạt: không exception/lỗi tải ảnh, không part -1 bị truy cập như index, hình đúng và chỉ số đúng.

## Bước 6 — Cửa hàng hồng ngọc Santa

**Tệp:** `SRC/src/nro/models/npc_list/Santa.java`, `SRC/src/nro/models/shop/ShopService.java`, `SRC/src/nro/models/services/InventoryService.java`, migration/dump SQL; tạo `SRC/test/nro/models/shop/SantaRubyShopTest.java` cho kiểm tra mua hàng. Client Game1/Game2 Panel/Controller chỉ chỉnh nếu kiểm tra giao thức hiện tại cho thấy thiếu nhãn hồng ngọc.

Giá do người dùng xác nhận, không tự đặt:

| Vật phẩm | ID | Giá hồng ngọc | Điều kiện mua |
|---|---:|---:|---|
| Quy lão Kame | 710 | 1000 | Không cần cải trang tiền nhiệm |
| Jacky Chun | 711 | 1500 | Đang sở hữu Quy lão Kame 710 |
| Android 21 Tiến Sĩ | 1710 | 2000 | Đang sở hữu Jacky Chun 711 |
| Giáp tập luyện cấp 5 | 1869 | 2500 | Không cần cải trang tiền nhiệm |

Định nghĩa sở hữu: vật phẩm còn tồn tại, quantity > 0, không hết hạn, thuộc người mua trong đồ đang mặc/hành trang/rương. Không tính món đã bán/ký gửi, đồ đệ tử/người khác hoặc lịch sử từng mua. Món tiền nhiệm được giữ lại; yêu cầu là sở hữu, không phải nguyên liệu đổi. Đây là lựa chọn thiết kế theo yêu cầu “phải có cải trang trước đó”.

- [x] Tạo shop có tag duy nhất `SANTA_HONG_NGOC`, npc_id=39. Dùng type_shop=3 cho packet iconSpec/cost hiện có; hai tab “Cải trang”, “Giáp tập luyện”. Cấp shop/tab/item_shop ID từ dữ liệu thực tế, không ghi đè các ID hiện có; migration chạy lại không tạo shop trùng.
- [x] Bốn item_shop dùng `type_sell=3` (COST_RUBY), đúng giá trên, `icon_spec=7743` (icon hồng ngọc item 861). Gắn đúng 5/5/5/6 option tương ứng; không sinh option mặc định trùng khi cấp vật phẩm từ shop.
- [x] Thêm nút “Cửa hàng hồng ngọc” vào cuối menu Santa để không làm lệch các nút hiện có. Kiểm tra cả menu có/không phiếu giảm giá 459 tại map 5/13/20; không để phiếu này giảm giá cửa hàng mới.
- [x] Trước cấp vật phẩm, kiểm tra đang mở đúng shop, món hợp lệ, type_sell=3, giá dương từ server, điều kiện tiền nhiệm, đủ chỗ túi và đủ `inventory.ruby`. Giá và điều kiện không lấy từ packet client. Thực hiện kiểm tra/trừ tiền/cấp đồ trong khóa player hiện có và phối hợp chặn giao dịch hồng ngọc đang pending.
- [x] Thêm nhánh thanh toán ví riêng theo tag trong shop đặc biệt: không đi vào subIemByItemShop chung để trừ vật phẩm, không đổi từ vàng/ngọc xanh/coupon. Thiếu ruby phải từ chối dù gem/gold đủ. Chỉ giảm ruby khi cấp đồ thành công; lỗi cấp đồ hoàn trả số dư, không âm hoặc phát đồ lặp.
- [x] Cơ sở khảo sát: ShopService.subMoneyByItemShop nhánh shop thường hiện kiểm tra nhầm `inventory.gem < ruby`; không dùng nhánh này cho cửa hàng mới. Kiểm tra logic thanh toán của cửa hàng mới trực tiếp bằng ruby, không chỉ nhìn loại tiền trong SQL.
- [x] Thông báo thiếu tiền ghi rõ “hồng ngọc”; thiếu tiền nhiệm ghi đúng tên cải trang cần có. Hai tab client hiển thị icon ruby, giá và xác nhận mua đúng loại tiền; không hiển thị nhầm ngọc xanh. Khi mở shop vẫn có thể xem đủ món, điều kiện được xác nhận lại ở server lúc mua.
- [x] Test: ruby đủ/gem=0 vẫn mua được; ruby thiếu/gem nhiều vẫn bị từ chối; số dư gem/gold/coupon không đổi; thiếu 710 không mua 711, thiếu 711 không mua 1710, có tiền nhiệm ở body/bag/box mua được; giữ lại tiền nhiệm; giáp mua độc lập; đồ hết hạn không mở khóa; túi đầy/lỗi cấp đồ không mất ruby; packet giả món/giá hoặc request đồng thời không vượt điều kiện.
- [ ] Test tooltip và trang bị đồ thật mua từ Santa có đúng chỉ số, timer 1000 phút ban đầu và đủ 21 option tổng; mở lại/đăng nhập không làm đầy timer của giáp đã dùng.

## Bước 7 — Đóng gói và cập nhật có lưu dữ liệu

- [x] Chạy các kiểm tra Java và client phù hợp; lưu output, kết quả SQL, manifest res và các file đã thay đổi. Không tuyên bố hiệu ứng/client đạt khi chỉ test text SQL.
- [x] Dựng JAR staging; kiểm tra class/bytecode thay đổi đúng, không dùng JAR cũ.
- [x] Dừng server bằng bảo trì có lưu dữ liệu; xác nhận tiến trình đã dừng trước khi áp dụng các thay đổi live và thay JAR.
- [x] Theo AGENTS.md: chuyển `SRC/20.jar` cũ vào `SRC/jar-backup` tên có thời điểm duy nhất, xác nhận SHA-256; ghi metadata bản cũ/mới, migration và rollback. Không di chuyển thư viện SRC/lib.
- [x] Chỉ giữ `SRC/20.jar` ở thư mục chạy; đã bảo trì lưu dữ liệu, backup có SHA-256, khởi động và xác minh catalog trong JVM tại cổng 14445. Kiểm tra gameplay đăng nhập trực tiếp chưa thực hiện.

## Điều kiện nghiệm thu

Đúng 21 option trong SQL; vật phẩm mới nhận đúng, vật phẩm cũ không mất nâng cấp/timer; HP/KI/sức đánh/giáp/giảm sát thương có tác dụng; x3/x4/x5 đúng mỗi phút chỉ với người chơi thật, không tác dụng/tiêu hao lượt ở boss/quái; +35/+50/+60% đúng TN và SM đệ tử; giáp cấp 5 hoạt động cả timer, mặc -40% / cởi +50%, có nguyên văn dòng mô tả trong cả description và option; Santa bán đúng giá ruby 1000/1500/2000/2500, tiền nhiệm và loại tiền được kiểm tra trên server; ba cải trang hiển thị đúng trên hai tab client; có backup/metadata khôi phục JAR và database.

## Kết quả triển khai 04/10/2026

- SQL live `team2026`: đủ 21 cặp mặc định và 21 cặp shop, một shop Santa; hai dump có migration cập nhật cuối tệp. Cột `description` đã mở rộng từ VARCHAR(75) sang VARCHAR(255) để giữ đủ 76 ký tự mô tả giáp.
- Quét body/bag/box/lucky/sold/pet của 2 player khi server đã dừng: không có món mục tiêu cần backfill. Loader bổ sung option thiếu, giữ timer và option nâng cấp; script backfill JSON lồng chuỗi đã kiểm tra tính lặp an toàn.
- Đã sửa hai lỗi bonus giáp: timer không giảm sau ô trống và cache còn giữ giáp rời khỏi kho. Các trường hợp bán/chuyển, quantity=0, trong rương, thay vật phẩm cùng ID và item khác ở ô giáp có kiểm thử riêng.
- Hình Doctor đã xác nhận; 440 path PNG x1–x4 đủ. Binary part đã đúng nên không cần tạo lại; bổ sung avatar 1603→14241. Game1/Game2 đã sửa nhãn hồng ngọc và xử lý packet ảnh lặp. Version item catalog là 17.
- Hồ sơ: `.analysis/costume-santa-20261004/deployment.json`, `sql-production.json`, `combined-test-results.json`, `runtime-catalog-status.txt`, `unity-result.txt`. Chi tiết client: `docs/source-analysis/costume-client-resource-validation.md`.
- Chưa có chứng nhận gameplay đăng nhập trực tiếp, UI word-wrap, clean/old login cache, save/load tài khoản live hoặc combat trực tiếp giữa hai client; các kiểm thử combat hiện chạy trên đối tượng Java thật với fixture.
