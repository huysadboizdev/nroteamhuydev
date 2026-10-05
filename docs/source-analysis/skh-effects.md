# Tác dụng SKH theo team2026.sql

Cập nhật 02/10/2026 sau khi người dùng yêu cầu triển khai hiệu ứng. Bảng ID dùng chung nằm ở `SRC/src/nro/models/item/ActivationSet.java`. Nội dung này thay thế các nhận xét về hiệu ứng chưa triển khai trong những bản audit trước đó.

| Set | Tác dụng trong code |
| --- | --- |
| Thiên Xin Hăng | 5 món: x2 thời gian chói mắt khi dùng skill |
| Kirin | 5 món: x2 sát thương Quả Cầu Kênh Khi |
| Sôngôku | 5 món: +100% sát thương Kamejoko |
| Picolo | 5 món: x2 sát thương Masenkosappo, dùng long rồi giới hạn Integer.MAX_VALUE |
| Ốc tiêu | 5 món: +100% sát thương Liên Hoàn |
| Pikkoro Daimao | 5 món: x2 sát thương sinh vật Đẻ Trứng, không mất HP do bị tấn công và không hết hạn tồn tại. Tháo đồ mất cả bonus sát thương và bảo vệ; sinh vật đã quá hạn sẽ biến mất. Đi theo chủ khi đổi map; chủ chết vẫn thu hồi sinh vật |
| Kakarot | 5 món: +100% sát thương Galick |
| Ca Đíc | 5 món: x5 thời gian hóa khỉ khi dùng skill |
| Nappa | 5 món: +80% HP tối đa |
| Gohan | 5 món: x2.5 xác suất xét rơi vật phẩm ngẫu nhiên của quái thường; x1.3 lượng vàng rơi |
| Nail | Từ 2 món: +10 điểm phần trăm sát thương chí mạng, áp dụng cùng bonus chí mạng sẵn có. 4 món: giảm 20% hồi Masenko. 5 món: +80% sát thương Masenko một lần và giảm 50% hồi chiêu |
| Cađic M | Từ 2 món: +20% HP và +200 phạm vi nổ, giữ tác dụng ở 3–5 món. 4/5 món: cộng sát thương nổ bằng 20%/50% HP tối đa; phép nhân dùng long |
| Kaio | Từ 2 món: +10 điểm chí mạng. 4/5 món: HP tiêu hao Kaioken bằng 5%/3% HP tối đa thay vì 10%; KI tiêu hao bằng 50%/30% chi phí thường. 5 món: +30% sát thương Kaioken |

SQL không ghi số cụ thể cho các bậc Nail/Cađic M/Kaio. Các mức 10/20/30/50/80 và phạm vi +200 giữ theo giá trị đã có trong source; sửa các lỗi khiến chúng không áp dụng hoặc áp dụng lặp. Mốc 2/4/5 món theo mô tả SQL.

## Chính sách Gohan

- Áp dụng trong `Mob.getItemMobReward`, sau các bonus cỏ bốn lá và option 236 đã được tính ở từng nhánh. Nhánh có hai cổng xét rơi chỉ nhân may mắn ở cổng cuối, tránh x2.5 hai lần.
- Giữ phần thập phân khi xét xác suất, ví dụ 1 thành 2.5 thay vì 2; giới hạn xác suất ở 100%.
- Không đổi cách chọn loại đồ hay random phẩm chất/options sau khi đã rơi. Không thay đổi đường thưởng boss riêng và vật phẩm nhiệm vụ rơi bảo đảm.
- Nhân lượng vàng trước khi tạo ItemMap, dùng long để tính rồi giới hạn Integer.MAX_VALUE. Không cộng thêm bonus Gohan ở luồng nhặt vàng.
- Đọc bộ đếm đồ đang mặc mỗi lần xử lý; không lưu bonus trong trạng thái lâu dài. Chỉ đủ 5 món mới có tác dụng.

## Kiểm chứng

`python .analysis/verify_gold_economy.py` compile toàn bộ source/test với Java 17 và chạy 17 lượt test offline: 10 lượt hồi quy có sẵn, 7 chế độ trong `SKHEffectTest`. Các test bao gồm mốc số món, tháo đồ, sát thương, HP/KI, hồi chiêu, phạm vi nổ, giới hạn số nguyên, Đẻ Trứng nhận sát thương/hết hạn/đổi map/chủ chết, lượng vàng và xác suất rơi qua 20.000 lần mỗi cấu hình Gohan 4/5 món với random seed cố định.

Reviewer độc lập không tìm thấy lỗi cần chặn hoàn tất. Chưa kiểm thử bằng client game; chưa thay JAR đang triển khai hoặc khởi động lại server.
