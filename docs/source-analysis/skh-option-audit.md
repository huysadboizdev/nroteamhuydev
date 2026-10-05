# Đối chiếu option SKH với team2026.sql

> Bản audit lịch sử: hiệu ứng đã được triển khai ở lượt tiếp theo. Xem [tác dụng SKH hiện tại](skh-effects.md) để biết kết quả sửa và kiểm chứng.

> Cập nhật mapping 02/10/2026: `ActivationSet.java` là bảng ID chung theo `team2026.sql`. `getOptionIdsBySKH`, `randOptionItemKichHoat`, `Option/AddOption`, `ID`, `Item.isSKH` và `SetClothes` đã dùng bảng này. Đã bổ sung bộ đếm Gohan và reset khi tháo đồ; option 250/252/253/255 không còn được coi là SKH trong bộ đếm vì không thuộc bảng set SQL. Nhận xét bên dưới về thiếu nhận diện/bộ đếm là kết quả trước thay đổi. Hiệu ứng gameplay đã được triển khai tiếp theo, xem liên kết ở trên. Giữ nguyên tỷ lệ random và không sửa đồ cũ trong database.
>
> Kiểm chứng mapping: `python .analysis/verify_gold_economy.py` compile toàn bộ Java với `--release 17`; 10 lượt test đạt tại thời điểm sửa mapping. Test SKH kiểm tra 13 nhóm option, helper tạo đồ, nhận diện tên/hiệu ứng, đếm 5 món và reset về 4 khi tháo, loại option không phải set, cùng 600 lượt random đúng hành tinh và padding `-1`. Lượt triển khai hiệu ứng sau đó đã bổ sung test và đạt 17 lượt. Không khởi động server hoặc thay JAR triển khai.

Ngày 02/10/2026. Phạm vi: 13 bộ khác tên, tương ứng 5 lựa chọn trên mỗi hành tinh. Nguồn chuẩn là `team2026.sql`; kiểm tra source Java, chưa kiểm thử chiến đấu trong game. Cập nhật theo yêu cầu người dùng: đã sửa hai mapping Thiên Xin Hăng/Kirin và chạy test hồi quy offline. Chưa thay đổi hiệu ứng các bộ khác, chưa sửa SQL.

## Kết luận về ghép option

SQL đang đúng: Thiên Xin Hăng dùng 127+139; Kirin dùng 128+140. `ItemService.getOptionIdsBySKH` trước đây trả 127→140 và 128→139; hiện đã sửa thành 127→139 và 128→140. Hai đường khác (`SetClothes.setupSKT`, `ItemService.randOptionItemKichHoat`) cũng dùng đúng cặp theo SQL. Không cần đổi SQL ở hai bộ này.

Sau sửa, cả 13 mapping khớp bảng chuẩn. Test đã tái hiện lỗi 127→140 trước sửa, rồi đạt sau sửa: kiểm tra đồ tạo ra có đúng option và 12 bộ có sẵn bộ đếm được nhận diện đúng khi mặc 5 món. Gohan chưa có bộ đếm nên chưa kiểm thử hiệu ứng mặc Gohan. Cặp option khớp không bảo đảm toàn bộ hiệu ứng gameplay đã được triển khai đúng.

## Bảng toàn bộ 13 bộ

| Hành tinh | Bộ | Option tên | Option hiệu ứng | Mô tả SQL | Tình trạng source |
| --- | --- | --- | --- | --- | --- |
| Trái Đất | Thiên Xin Hăng | 127 | 139 | 5 món x2 thời gian chói mắt | Đã sửa helper khớp SQL; bộ đếm và SkillService đang x2 thời gian stun khi đủ 5 |
| Trái Đất | Kirin | 128 | 140 | 5 món +100% sát thương Quả Cầu Kênh Khi | Đã sửa helper khớp SQL; bộ đếm và NPoint đang nhân sát thương x2 khi đủ 5 |
| Trái Đất | Sôngôku | 129 | 141 | 5 món +100% sát thương Kamejoko | Mapping khớp; NPoint cộng 100% hệ số tăng sát thương |
| Namek | Picolo | 130 | 142 | 5 món +100% sát thương Masenkosappo | Mapping khớp, nhưng NPoint dùng `dameSkill *= 3 / 2`: phép chia nguyên bằng 1, không tăng sát thương |
| Namek | Ốc tiêu | 131 | 143 | 5 món +100% sát thương Liên Hoàn | Mapping khớp; NPoint cộng 100% hệ số tăng sát thương |
| Namek | Pikkoro Daimao | 132 | 144 | 5 món +100% sát thương và bất tử đệ từ Đẻ Trứng | Mapping khớp; NPoint nhánh Đẻ Trứng nhân x4, khác mô tả x2. MobMe có nhánh bỏ hết hạn tồn tại khi đủ 5; không nên diễn giải là đệ tử thông thường bất tử |
| Xayda | Kakarot | 133 | 136 | 5 món +100% sát thương đấm Galick | Mapping khớp; NPoint cộng 100% hệ số tăng sát thương |
| Xayda | Ca Đíc | 134 | 137 | 5 món x5 thời gian hóa khỉ | Mapping khớp; EffectSkillService nhân thời gian x5 |
| Xayda | Nappa | 135 | 138 | 5 món +80% HP | Mapping khớp; NPoint cộng 80% HP tối đa |
| Dùng chung | Gohan | 233 | 234 | 5 món +150% may mắn, +30% vàng rơi từ quái | Mapping khớp, có nguồn sinh đồ; SetClothes chưa đếm Gohan. Chưa tìm thấy xử lý option 233/234 cho hiệu ứng Gohan trong NPoint/Mob; option may mắn 236 ở Mob là cơ chế khác |
| Namek | Nail chiến binh Namếc | 237 | 238,239,240 | 2 món tăng sát thương chí mạng; 4 món giảm hồi Masenko; 5 món tăng sát thương và giảm mạnh hồi Masenko | Mapping khớp; code có +10 vào danh sách tăng sát thương chí mạng từ 2 món, hồi chiêu +20% ở 4/+50% ở 5, tăng hệ số Masenko +80 ở 5. SQL không ghi số cụ thể nên chưa kết luận mức số này sai |
| Xayda | Cađic M | 241 | 242,243,244 | 2 món tăng HP/phạm vi nổ; 4 món tăng nhẹ sát thương nổ; 5 món tăng mạnh sát thương nổ | Mapping khớp; HP +20% từ 2 món, sát thương có nhánh 4/5. Phạm vi +200 chỉ khi `cadicM == 2`, không áp dụng khi mặc 3–5 món; cần kiểm tra quy tắc cộng dồn mong muốn |
| Trái Đất | Thần Vũ Trụ Kaio | 245 | 246,247,248 | 2 món tăng chí mạng; 4 món giảm hao HP/KI Kaioken; 5 món tăng sát thương và giảm mạnh hao HP/KI | Mapping khớp nhưng chí mạng `crit += 10 / 100` luôn cộng 0, điều kiện `>=1` khác ngưỡng 2 món SQL. Có giảm hao HP ở 4/5 và tăng sát thương ở 5; chưa thấy giảm hao KI theo bộ Kaio trong hàm trừ MP |

## Nguồn kiểm tra chính

- `team2026.sql`: item_option_template, dòng 2297–2314 và 2403–2418.
- `SRC/src/nro/models/services/ItemService.java`: getOptionIdsBySKH dòng 297; randomSKHId dòng 330; randOptionItemKichHoat dòng 749.
- `SRC/src/nro/models/player/SetClothes.java`: setupSKT, đếm tối đa một bộ trên mỗi món, dừng ở option SKH đầu tiên nhận diện được.
- `SRC/src/nro/models/player/NPoint.java`: HP Nappa/Cađic M dòng 790/794; chí mạng Kaio dòng 1237; tính sát thương theo từng skill dòng 1374–1476.
- `SRC/src/nro/models/services/SkillService.java`: hao HP Kaioken dòng 401; chói mắt dòng 605; phạm vi/sát thương nổ dòng 744; hồi Masenko dòng 1174; setMpAffterUseSkill dòng 1105.
- `SRC/src/nro/models/services/EffectSkillService.java`: thời gian hóa khỉ dòng 227.
- `SRC/src/nro/models/mob/MobMe.java`: hết hạn sinh vật Đẻ Trứng dòng 32.

## Những điểm khác cần ghi nhớ trước khi làm nâng SKH

1. `randomSKHId` khai báo năm lựa chọn nhưng chỉ chọn index 0–2 với tỉ lệ 25/35/40, không thể ra Gohan và bộ Kaio/Nail/Cađic M qua helper này. `randOptionItemKichHoat` là một nguồn khác có thể sinh các bộ đó; không kết luận toàn server chỉ sinh ba bộ.
2. `Item.isSKH()` chỉ quét option 127–135, bỏ sót Gohan/Nail/Cađic M/Kaio. Không dùng riêng hàm này để xác thực toàn bộ SKH mới.
3. `SetClothes` còn xử lý các ID cũ 250,252,253,255. Trong SQL chuẩn, 250 là `# Kilis`; 252/253/255 không có trong bảng option. Đây là code dư/không đồng bộ, không thêm chúng vào danh sách 13 bộ trên. Cần rà các nguồn sinh option cũ trước khi sửa/xóa.
4. Đồ có cả hai option thuộc hai bộ khác nhau do helper ghép sai: bộ đếm chọn option SKH đầu tiên rồi break, nên tên/mô tả và hiệu ứng có thể không thống nhất; không thể sửa chỉ bằng đổi mô tả SQL.
5. SQL ghi tên và mô tả. Cơ chế tác dụng nằm trong source Java; cập nhật SQL không tự bổ sung hiệu ứng Gohan hay sửa phép chia nguyên Kaio/Picolo.

## Thứ tự sửa đề xuất khi được yêu cầu triển khai

- Mapping 127→139, 128→140 đã sửa và kiểm thử đồ mới. Đồ đã có cặp sai trong database chưa được tự động chuyển đổi.
- Làm rõ chính sách hiệu ứng theo SQL chuẩn rồi sửa Picolo, Pikkoro Daimao, Kaio và ngưỡng phạm vi Cađic M; thêm test biên 1/2/3/4/5 món.
- Bổ sung nhận diện và tác dụng Gohan ở đúng luồng rơi; test đủ năm món và tháo một món, tránh buff tồn dư.
- Tạo bảng random đúng đủ năm lựa chọn cho nâng mới và nhận diện SKH đủ 13 bộ, không dùng helper chỉ random ba bộ.
