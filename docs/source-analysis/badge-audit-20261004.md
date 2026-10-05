# Kiểm tra toàn bộ danh hiệu — 04/10/2026

**Cập nhật:** Các lỗi trong báo cáo này đã được xử lý trong bản sửa ngày 04/10/2026. Xem `badge-repair-completed.md` để biết kết quả và điều kiện mới đã chốt.

## Phạm vi và bằng chứng

- Đọc trực tiếp cấu hình Manager của server đang chạy: 18 nhiệm vụ, 16 mẫu danh hiệu, 19 món trong mỗi tab danh hiệu của Santa.
- Snapshot: `.analysis/dragon-badge/badge-config-live.json`, thời điểm 17:19:37 giờ Việt Nam.
- Đối chiếu mã nguồn server với JAR ứng dụng đang dùng `SRC/20.jar`, SHA-256 `be1056f6e334f6cda9d5fea0c556ab7ae70ddc42b8e96fe5418e52b442fd5e14`.
- Chạy chương trình offline `GeneralBadgeAudit` với chính JAR này: tái hiện 6 nhóm lỗi. Không thao tác tài khoản thật, không thay cấu hình, không thay JAR trong đợt kiểm tra này.
- Các dòng “lỗi” bên dưới là kết quả đối chiếu mã/cấu hình; 6 tình huống có tái hiện thực thi được ghi riêng. Đây không phải kiểm thử mọi boss, mọi sự kiện và mọi thao tác qua giao diện client.

## Cách nhận và sử dụng chung

Server tự cấp và bật danh hiệu khi nhiệm vụ đủ tiến độ trong lượt cập nhật người chơi. Danh hiệu mới tắt danh hiệu đang dùng trước đó. Thời hạn cấp là **30 ngày**, kể cả Mẹ Rồng; “vĩnh viễn” trong điều kiện Mẹ Rồng chỉ nói về pet.

Đổi danh hiệu tại **Santa → Danh hiệu → Sở hữu**. Tab Danh hiệu hiển thị tiến độ/mở khóa; tab Sở hữu dùng để chọn danh hiệu đã nhận. Mã nhận danh hiệu không trừ tiền tại đây.

Tiến độ nhiệm vụ được tạo lại khi đăng nhập sang ngày mới, kể cả những nhiệm vụ tên không ghi “trong ngày”. Chưa có xử lý tương đương lúc người chơi đang online qua nửa đêm. Vì vậy người online liên tục có thể tích lũy qua ngày, còn người đăng nhập lại bị reset. Mẹ Rồng được tính lại theo đồ hiện có nên không phụ thuộc số đếm cũ.

## Danh sách đầy đủ trong shop

| Danh hiệu / ID vật phẩm | Điều kiện ghi trong cấu hình | Hành vi hiện tại / kết quả |
|---|---|---|
| Đại gia mới nhú / 1289 | Nạp tích lũy 1.000.000 trong ngày | Tiến độ cộng khi **đổi số dư nạp sang vàng/ngọc**, không phải ngay lúc nạp tiền. Chưa thấy lỗi riêng trong ngưỡng mở khóa. |
| Trùm ước rồng / 1290 | Ước Rồng Thần 1 sao 100 lần | Đếm khi rồng xuất hiện, không kiểm tra 1 sao hay hoàn tất điều ước. Luồng gọi lại rồng cũng đi qua điểm đếm này. |
| Trùm săn boss / 1291 | Hạ Cumber, Black Goku, Cooler, Xên tổng 300 lần | Các boss này có điểm cộng, gồm cả các Xên con. Chưa thấy lỗi riêng trong ngưỡng mở khóa. |
| Thánh đập đồ +7 / 1292 | Nâng thành công 5 trang bị lên +7 trong ngày | **Sai cấp:** đếm nâng từ +7 lên +8, không đếm từ +6 lên +7. |
| Cao thủ siêu hạng / 1293 | Top 1 Đại hội Võ đài Siêu hạng | Điểm cộng khi xử lý thưởng Siêu hạng trong lúc rank = 1, chưa cộng ngay khi vừa chiếm Top 1. |
| Nông dân chăm chỉ / 1294 | Hoàn thành 10 nhiệm vụ siêu khó tại Bò Mộng | **Sai độ khó, sai số lượt:** mọi độ khó đều cộng; một lượt trả thưởng thành công cộng 3; hành trang thiếu chỗ vẫn cộng 1 và nhiệm vụ chưa bị tiêu thụ. |
| Kẻ thao túng sói / 1286 | Hạ hoặc cho xương sói 20 lần | Có điểm cộng ở thưởng Sói Héc Quyn và thao tác cho xương, nhưng **thiếu mẫu danh hiệu, reward dùng nhầm ID vật phẩm 1286**. Không mở/hiển thị đúng theo luồng bình thường. |
| Nước anh bao / 1287 | Hoàn thành nhiệm vụ cho Xinbato nước 5 lần | **Thiếu điểm cộng tiến độ**, đồng thời thiếu mẫu danh hiệu và reward dùng ID vật phẩm 1287. |
| Ông thần ve chai / 1295 | Nhặt đồ 500 lần trong ngày | **Đếm sai hành động:** cộng trong xử lý đồ rơi sau khi quái chết, trước khi nhặt; không phụ thuộc nhặt thành công. |
| Bị móc sạch túi / 1296 | Hạ 30 boss Ăn Trộm | **Đếm trùng:** cộng ở `die`, rồi cộng thêm trong `reward` khi boss có vàng trộm; có thể đủ sau 15 lượt loại có vàng. |
| Thánh ở dơ / 1300 | Hạ 30 boss Ở Dơ | Có điểm cộng ở thưởng boss, nhưng **thiếu mẫu danh hiệu, reward dùng ID vật phẩm 1300**. |
| Fan cứng / 1299 | Không có nhiệm vụ tương ứng | Mẫu hiệu ứng 228 có trong cấu hình và shop, nhưng **không có nhiệm vụ mở khóa**. Khác danh hiệu KOL. |
| Gõ đầu trẻ / 1394 | Mở 10 rương gỗ cấp 12 | **Không kiểm tra cấp 12**, cộng trước cả kiểm tra có rương và đủ chỗ. |
| Gõ đầu trẻ / 1392 | Mở 20 rương gỗ cấp 12 | Cùng lỗi như trên; cả ba nhiệm vụ cùng tăng một lượt. |
| Gõ đầu trẻ / 1393 | Mở 30 rương gỗ cấp 12 | Cùng lỗi như trên. |
| X-mas / 1457 | Đạt 500 điểm sự kiện | **Đếm lượt dùng** pháo bông/pháo VIP/kem trái cây/quà thiếu nhi, không đếm tổng điểm sự kiện. Kem không đủ chỗ hoặc không đủ vật phẩm vẫn có thể cộng khi đã vào hàm. |
| Em xinh, em đẹp / 1514 | Nạp tích lũy 2.000.000 trong ngày | Cùng cách cộng số dư đổi vàng/ngọc với Đại gia mới nhú. Chưa thấy lỗi riêng trong ngưỡng mở khóa. |
| Mẹ Rồng / 1790 | Sở hữu đủ 7 loại rồng nhí vĩnh viễn | **Đã sửa điều kiện:** 7 loại khác nhau 1–7 sao trong hành trang, rương nhà hoặc đang mặc của nhân vật chính. Không tính bản trùng, đồ hạn sử dụng kể cả 0 ngày, số lượng 0 hoặc đồ đệ tử đang mặc. Mất một loại sẽ mất quyền bật/hiển thị và chỉ số; khôi phục đủ bộ có thể bật lại nếu danh hiệu chưa hết hạn. Còn lỗi chỉ số chí mạng chung nêu dưới. |
| KOL / 1297 | Tên nhiệm vụ: Fan cứng KOL, mốc 1 | Hiện tại **hạ boss Mặt Trời một lần** để cộng tiến độ; không có kiểm tra fan/KOL trong luồng nhiệm vụ này. Cấu hình chỉ số bất thường. |

## Lỗi chỉ số và quyền sử dụng

1. **Mẹ Rồng và ba Gõ đầu trẻ không được cộng sức đánh chí mạng từ danh hiệu.** Option 5 có trong cấu hình (Mẹ Rồng +7%, Gõ đầu trẻ +10%) nhưng các vòng xử lý option danh hiệu trong `NPoint` không đưa nó vào `tlSDCM`/danh sách sức đánh chí mạng. Tái hiện với +10 trên nhân vật không mặc đồ cho kết quả `tlSDCM = 0`.
2. **Né đòn của Ông thần ve chai và Bị móc sạch túi tính sai.** Đang dùng `tlNeDon += tlNeDon * param / 100`, ngay sau reset và trước khi đọc trang bị. Né đòn cơ sở 0 thì danh hiệu +3/+5 cũng thành 0. Cần thống nhất cộng điểm phần trăm hoặc tăng tương đối rồi đặt đúng vị trí tính.
3. **KOL có option bất thường `{id:10,param:103}`**, tức “Sát thương chuẩn 103%”, thay vì option KI 103 với giá trị 10 thường đi cùng SD/HP +10. Đây là nghi vấn đảo ID/giá trị; chưa tự sửa cấu hình vì cần chốt chỉ số mong muốn. Các vòng cộng chỉ số danh hiệu hiện không xử lý option 10.
4. **Bản ghi danh hiệu trùng ngoài Mẹ Rồng vẫn cộng chỉ số nhiều lần.** Có thể xảy ra với dữ liệu cũ do lỗi cấp danh hiệu đã từng thêm bản ghi hai lần. Lần sửa trước chỉ loại trùng cho Mẹ Rồng; chưa xử lý các loại khác.
5. **Lọc chỉ số/tab sở hữu chưa kiểm tra hạn trực tiếp.** Một danh hiệu đã hết hạn vẫn trả về option và xuất hiện trong danh sách đến khi lượt `autoSendBadges` loại bản ghi. Không phải được dùng vĩnh viễn; có khoảng trạng thái cũ trước lần cập nhật kế tiếp.
6. **Đổi danh hiệu thông thường chưa xác thực sở hữu/hạn tại hàm bật.** Chọn ID không sở hữu làm tắt danh hiệu đang dùng; hàm shop vẫn có thể báo đổi thành công. Một bản ghi hết hạn chưa dọn cũng có thể được bật trong khoảng này. Cần kiểm tra ở server dù giao diện bình thường chỉ hiển thị đồ sở hữu.
7. **Đổi danh hiệu ép HP/KI/sức đánh về `int`.** Shop gọi `(int) hpMax`, `(int) mpMax`, `(int) dame` dù hệ thống hỗ trợ giá trị lớn hơn. Với số vượt giới hạn int có nguy cơ tràn, sai số khi đổi danh hiệu. Phát hiện qua mã, chưa tái hiện thao tác đổi trên tài khoản thật.

## Kết quả tái hiện offline

Lệnh chạy:

```powershell
& 'C:/Program Files/Java/jdk-21.0.12/bin/java.exe' -cp 'SRC/20.jar;.analysis/dragon-badge/audit-classes' GeneralBadgeAudit
```

Kết quả exit code 0, xác nhận:

- Danh hiệu hết hạn vẫn trả option trước dọn định kỳ.
- Hai bản ghi danh hiệu không phải Mẹ Rồng trả hai bộ option.
- Chọn danh hiệu chưa sở hữu làm tắt danh hiệu hiện tại.
- Ba lần trả thưởng nhiệm vụ Bò Mộng mức dễ thất bại vì thiếu chỗ vẫn cộng ba điểm.
- Gọi mở rương khi không có rương vẫn cộng cả ba nhiệm vụ Gõ đầu trẻ.
- Option chí mạng +10 và né đòn +5 từ danh hiệu cho chỉ số 0 trên nhân vật không trang bị.

## Vị trí mã để sửa

| Nhóm | Mã |
|---|---|
| Cấp, reset, tiến độ | `SRC/src/nro/models/task/BadgesTaskService.java`; `services/PlayerService.java:321`; `database/MrBlue.java:1347` |
| Chọn danh hiệu và chỉ số | `player_badges/BadgesService.java`; `player_badges/BagesTemplate.java`; `player/NPoint.java:247`; `shop/ShopService.java:923` |
| Nâng +7 | `combine/NangCapVatPham.java:174`, `:216` |
| Bò Mộng | `services/TaskService.java:1275` |
| Rương và sự kiện | `services_func/UseItem.java:968`, `:1348`, `:1375`, `:1406`, `:1449` |
| Ăn Trộm | `boss/Boss_mini/AnTrom.java:133`, `:158` |
| Ve chai | `mob/Mob.java:562` |
| Ước rồng | `services/shenron/SummonDragon.java:219` |
| KOL | `boss/Boss_mini/MatTroi.java:126` |
| Nạp/đổi số dư | `services_func/Input.java:164`, `:199` |

## Thứ tự đề xuất khi sửa

1. Chặn sử dụng không sở hữu/hết hạn, bỏ bản ghi trùng mọi danh hiệu, sửa chỉ số chí mạng/né và bỏ ép int khi đổi.
2. Sửa điểm cộng đúng hành động, đúng kết quả thành công cho Bò Mộng, rương gỗ, nâng +7, Ăn Trộm, nhặt đồ, ước rồng và điểm sự kiện.
3. Bổ sung/mapping đúng ba mẫu thiếu; nối nhiệm vụ Xinbato; chốt cách nhận Fan cứng và KOL, chỉ số KOL.
4. Chốt nhiệm vụ nào theo ngày/nhiệm vụ nào tích lũy; áp dụng reset cùng quy tắc cho online qua nửa đêm và đăng nhập lại.

Chưa tự lựa chọn điều kiện mới cho các danh hiệu đang thiếu hoặc cấu hình mâu thuẫn. Các lỗi mới nêu trong báo cáo chưa được đưa vào JAR.
