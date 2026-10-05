# Chỉ số 7 pet từ Trứng vàng rồng nhí — 04/10/2026

Đã triển khai bộ chỉ số theo yêu cầu người dùng trong database, `team2026.sql`, `SRC/sql/nro1.sql` và source/JAR. Profile được lưu ở bảng `dragon_pet_config`; `Manager` nạp cả 7 dòng vào `DragonPetCatalog` khi khởi động. Trứng vàng rồng nhí ID 1821 cấp đúng một profile khi mở.

## Tên, chỉ số và res

| Tên | ID | Icon | Head / Body / Leg | HP | Sức đánh | KI | Giảm sát thương | Sức đánh chí mạng | Chí mạng | Né đòn | Khác |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Rồng nhí 1 sao | 1771 | 15125 | 1680 / 1681 / 1682 | +22% | +22% | — | 8% | +11% | +8% | — | Chống lạnh |
| Rồng nhí 2 sao | 1770 | 15117 | 1677 / 1678 / 1679 | +22% | +22% | — | 8% | +11% | +8% | — | — |
| Rồng nhí 3 sao | 1769 | 15109 | 1674 / 1675 / 1676 | +18% | — | — | 5% | — | — | 7% | — |
| Rồng nhí 4 sao | 1768 | 15101 | 1671 / 1672 / 1673 | +14% | — | — | — | +7% | +5% | — | — |
| Rồng nhí 5 sao | 1767 | 15093 | 1668 / 1669 / 1670 | — | +18% | — | 5% | — | — | 7% | — |
| Rồng nhí 6 sao | 1766 | 15085 | 1665 / 1666 / 1667 | — | +18% | — | — | +7% | +5% | — | — |
| Rồng nhí 7 sao | 1765 | 15077 | 1662 / 1663 / 1664 | +16% | +16% | +16% | — | — | — | — | +20% May mắn |

Áp dụng chung:

- Không giao dịch: option 30, param 0.
- **10% vĩnh viễn**: không thêm option 93.
- **90% có hạn**: option 93 random đều **1–30 ngày**.
- Mỗi loại rồng có xác suất được chọn như nhau, 1/7. Tỷ lệ vĩnh viễn áp dụng độc lập cho cả bảy loại.
- Không cấp option thừa hoặc trùng; giữ nguyên hình part/icon đã sửa ở lần trước.

Res/path đã kiểm tra lại sau triển khai: **228/228 PNG** ở `SRC/data/icon/x1` đến `x4`; **21/21 part** ID 1662–1682 trong database và `SRC/data/update_data/part`, đối chiếu với cả hai SQL dump. Icon 1771 giữ hình đúng 15125; Ghost Rider 1900 vẫn dùng 16187. Code trang bị và tải lại pet khớp cả 7 bộ part.

## Hiệu ứng thực tế

- Option 77/50/103: tăng HP/sức đánh/KI qua `NPoint`.
- Option 94: giảm sát thương theo cơ chế chiến đấu hiện có; vẫn chịu quy tắc xuyên giáp và giới hạn của game.
- Option 5/14: tăng sức đánh chí mạng/tỷ lệ chí mạng.
- Option 108: né đòn theo cơ chế chiến đấu hiện có.
- Option 106: chống giảm HP/sức đánh tại map lạnh.
- Option 236: đã bổ sung cộng/reset `NPoint.tlLuck` khi trang bị/tháo trang bị. `Mob.isLuckyDrop` nhân tỷ lệ rơi vật phẩm từ quái với `1 + tlLuck/100`. +20% tương đương x1,2; ví dụ xác suất 10% thành 12%. Cộng dồn các option May mắn đang mặc và kết hợp nhân với bonus Gohan x2,5: 10% thành 30% nếu có cả Gohan và +20% May mắn. Xác suất chặn ở tối đa 100%.
- May mắn không tăng số lượng vàng, không đổi tỷ lệ mở trứng hoặc tỷ lệ rơi do boss dùng luồng riêng.

Bộ chỉ số mới áp dụng cho **pet mở từ trứng sau cập nhật**. Không viết lại vật phẩm người chơi đã nhận trước đây. Tên template được cập nhật theo số sao. Những vật phẩm cũ có option 236 khi trang bị cũng nhận hiệu ứng May mắn theo cơ chế mới.

## SQL và source

- Migration: `SRC/sql/migrations/20261004_dragon_pet_profiles.sql`.
- Bảng `dragon_pet_config`: `item_id`, `stars`, `permanent_percent`, `min_days`, `max_days`, `options_json`.
- Source catalog: `SRC/src/nro/models/item/DragonPetCatalog.java`; nạp SQL tại `Manager.java`.
- Phát thưởng: `UseItem.OpenTrungRongNhi`.
- Luck: `NPoint.java`, `Mob.java`.
- Item catalog version **12** để client tự tải tên template mới khi đăng nhập lại.

## Kiểm thử, backup và runtime

- `DragonPetStatsTest`: so sánh toàn bộ option của cả bảy profile; kiểm tra từng hạn 1–30 ngày; duyệt 100 kết quả roll cho đúng 10 lần vĩnh viễn; kiểm tra trang bị/tháo/reset không cộng lặp.
- Combat: nhận sát thương thực tế đúng mức giảm 5/8%, né theo option; rồng 1 sao giữ HP/sức đánh tại map lạnh, rồng 2 sao chịu giảm lạnh bình thường.
- Luck: 1.000 roll có/không Gohan khớp xác suất kỳ vọng, tháo pet trả tỷ lệ cũ.
- Egg: mở 200 trứng với seed cố định, đủ bảy loại, một trứng tiêu hao mỗi lượt, option đúng profile.
- JAR đóng gói và JAR cài đặt đạt **23/23 nhóm kiểm thử**, gồm hồi quy hiệu ứng SKH, hợp thể, quà VIP đã cắt; **14/14 luồng dùng/tải pet**.
- Công cụ đối chiếu DB xác nhận **7/7 profile SQL/source** và **7/7 ánh xạ res**.
- Server bảo trì có lưu dữ liệu, xác nhận dừng trước thay JAR; JAR cũ được chuyển vào `SRC/jar-backup` với tên riêng. SHA-256 và tên backup ở `.analysis/dragon-stats/deployment.json` và JSON cùng tên JAR backup.
- Snapshot DB trước thay đổi: `.analysis/dragon-stats/database-before.json`; SQL hoàn tác: `.analysis/dragon-stats/rollback.sql`.
- Hai SQL dump có backup nguyên byte; vị trí/hash: `.analysis/dragon-stats/sql-file-changes.json`.
- Sau restart, log xác nhận nạp 7 profile từ SQL và lắng nghe cổng 14445. Chưa kiểm thử mở trứng qua client game trực tiếp.
