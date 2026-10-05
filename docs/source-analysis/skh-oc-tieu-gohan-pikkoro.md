# Note sửa tiếp: Ốc tiêu, Gohan, Pikkoro Daimao

> Bản audit lịch sử: hiệu ứng đã được triển khai ở lượt tiếp theo. Xem [tác dụng SKH hiện tại](skh-effects.md) để biết kết quả sửa và kiểm chứng.

Nguồn chuẩn: team2026.sql, bảng item_option_template. Ngày 02/10/2026. Đây là đối chiếu source; chưa sửa tác dụng ba bộ và chưa kiểm thử chiến đấu trong game.

## Ốc tiêu — Namek

- Option tên 131: `Set Ốc tiêu`.
- Option hiệu ứng 143: `$(5 món +100% sát thương Liên Hoàn)`.
- Nghĩa mô tả: mặc đủ 5 món Ốc tiêu sẽ tăng 100% sát thương Liên Hoàn, tương ứng gấp đôi trước các phép tính sát thương khác.
- Helper ghép 131→143 đúng; SetClothes đếm option 131 hoặc 143 thành bộ Ốc tiêu và dừng sau một option để không đếm đôi một món.
- NPoint, nhánh Skill.LIEN_HOAN: đủ 5 món đặt percentXDame = 100, phù hợp mức bonus trong SQL.
- Chưa thấy lệch mapping/mức bonus ở các đoạn đã kiểm tra. Test tiếp theo nên so sánh sát thương 4 và 5 món khi các yếu tố khác giữ nguyên, kiểm tra tháo một món mất bonus và rada Ốc tiêu được đếm.
- Không phải bộ Pikkoro Daimao/Đẻ Trứng.

## Gohan — dùng chung 3 hành tinh

- Option tên 233: `Set Gohan`.
- Option hiệu ứng 234: `$(5 món +150% may mắn +30% vàng rơi từ quái)`.
- Nghĩa mô tả: đủ 5 món mới có hai hiệu ứng: tăng 150% may mắn và tăng 30% số vàng rơi từ quái. Không có bonus sát thương được mô tả ở option này.
- Helper ghép 233→234 đúng, và các nguồn tạo SKH đã có Gohan trong bảng.
- SetClothes chưa có biến đếm Gohan hoặc case 233/234. Chưa tìm thấy nhánh hiệu ứng Gohan tương ứng ở NPoint/Mob.
- Mob đang đọc option 236 (+% May mắn) cho một số nhánh drop, đây là cơ chế riêng; chỉ gắn 233/234 không tự làm code 236 hoạt động.
- Cần bổ sung bộ đếm Gohan, reset bộ đếm khi thay đồ, và áp dụng buff đủ 5 món ở các nhánh drop được chọn. Cần xác định rõ +150% may mắn nhân vào xác suất drop nào; SQL không định nghĩa phạm vi drop.
- Vàng: nhân lượng vàng trước khi tạo ItemMap bằng kiểu đủ rộng, tránh tràn số; không cộng lại buff lần hai lúc nhặt. Tách lượng vàng rơi khỏi giới hạn vàng hành trang.
- Kiểm thử 4/5 món, cả 3 hành tinh, rada Gohan, tháo đồ mất buff, tương tác cỏ bốn lá và option 236, tránh buff tồn dư/cộng lặp.

## Pikkoro Daimao — Namek

- Option tên 132: `Set Pikkoro Daimao`.
- Option hiệu ứng 144: `$(5 món +100% sát thương và bất tử đệ từ Đẻ Trứng)`.
- Hiểu theo skill: sinh vật do Đẻ Trứng gọi ra (MobMe), không phải đệ tử thường đi theo người chơi. Tên mô tả SQL có lỗi chữ `đệ từ`, có thể chỉnh chữ thành mô tả rõ sinh vật Đẻ Trứng nếu người dùng muốn.
- Helper ghép 132→144 đúng; SetClothes nhận diện đúng.
- NPoint, nhánh Skill.DE_TRUNG: đủ 5 món đang nhân dameAttack x4, không khớp +100% (x2) trong SQL. MobMe tạo sát thương từ getDameAttack nên đây là đường tính liên quan trực tiếp tới sinh vật Đẻ Trứng.
- MobMe.update: đủ 5 món thì không tự biến mất khi hết timeSurvive. Đây là vô hạn thời gian tồn tại trong nhánh hết hạn.
- MobMe.injured: vẫn trừ HP, mỗi đòn tối đa maxHp/20, và vẫn gọi mobMeDie/dispose khi isDie; không có điều kiện chống chết theo set. Vì vậy chưa phải bất tử khi bị tấn công.
- Nếu lấy mô tả SQL làm chuẩn: cần đổi bonus sát thương về +100% và bổ sung chính sách chống chết cho MobMe khi chủ đủ 5 món. Bất tử phải kiểm tra trạng thái đồ hiện tại, tránh tháo bộ vẫn giữ buff; reset/thu hồi khi rời map hoặc chủ chết theo quy tắc game.
- Kiểm thử 4/5 món, sát thương thực tế x2 sau khi sửa, hết hạn tồn tại, nhận sát thương chí tử, tháo một món, đổi map và chủ chết. Không tự động sửa gameplay trong bước ghi note này.

## Thay đổi đã thực hiện riêng trong lượt này

- ItemService.getOptionIdsBySKH: 127→139 và 128→140, khớp Thiên Xin Hăng/Kirin trong SQL chuẩn.
- SKHOptionMappingTest: kiểm tra 13 mapping, option đồ tạo ra, và 12 bộ đếm hiện có khi mặc đủ 5 món. Gohan chưa có bộ đếm, nên test này không xác nhận tác dụng Gohan.
- Chạy python .analysis/verify_gold_economy.py: compile toàn bộ Java 17 thành công và 10 nhóm test đạt, gồm test SKH mới, vàng/ước rồng và ép sao cũ.
- Chưa build lại JAR chạy server, chưa sửa đồ đã được tạo sai cặp option trong database, chưa thay đổi SQL.
