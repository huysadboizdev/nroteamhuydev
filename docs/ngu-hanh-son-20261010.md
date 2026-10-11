# Ngũ Hành Sơn – 10/10/2026

Đường Tăng (NPC49) dẫn vào khu tại Aru `(672,432)`, Mori `(689,432)` và Kakarot `(478,408)`. Mỗi làng đặt thêm guide cách NPC bên phải của nhóm cũ 82px, giữ nguyên vị trí Daishinkan và các NPC khác.

Ba map 122 → 123 → 124 đi tiếp sang phải, quay lại sang trái; NPC làng đưa đến mép trái map122 `(60,288)`. Giữ nguyên 6/4/4 quái và tài nguyên quái 50/56/57. Map122 giữ loại offline nên có 1 khu; map123/124 có 10 khu. TNSM cả ba map x5, tính qua arithmetic chống tràn, áp dụng cả đệ tử trong luồng thưởng hiện có. Các giới hạn sức mạnh và giảm thưởng theo cấp hiện có vẫn áp dụng.

Map124 có Đường Tăng `(960,288)` và Tôn Ngộ Không `(1080,288)`. Hai vị trí trên nền đứng hợp lệ. Cả hai có menu về làng theo hành tinh nhân vật. Client Game1/Game2 vẽ Ngộ Không nhô đầu khỏi đá có bùa phong ấn. Sau phản hồi ảnh hiển thị, đã thay chân dung4520 bằng sprite đầu4468 (19×19 ởzoom1), thay khối chữ nhật bằng ảnh đá11 có sẵn (49×37 ởzoom1), sao chép nguyên ảnh đủ4zoom vào `Assets/Resources/res/x{zoom}/mainimage/ngu_hanh_rock.png`. Bùa nhỏ6×18, tên căn theo chiều cao38. Cần Unity dừng Play để nạp lại script/resource rồi Play lại hoặc build client mới; EXE cũ chưa được đóng gói lại.

| NPC | Cải trang | ID | Giá | Chỉ số |
|---|---|---|---|---|
| Ngộ Không | Ngộ Không Trái Đất / Namec / Xayda | 544/545/546 | 200 Hồng Đào | Random, hạn dùng 3–7 ngày |
| Ngộ Không | Ngộ Không đệ tử | 547 | 150 Hồng Đào Chín | Random, hạn dùng 3–7 ngày |
| Đường Tăng | Bát Giới đệ tử | 548 | 150 Hồng Đào Chín | Random, hạn dùng 3–7 ngày |
| Đường Tăng | Urôn Trư Bát Giới | 1698 | 300 Hồng Đào Chín | Random, hạn dùng 3–7 ngày |

Từ bản cập nhật random: cả 6 cải trang cho sư phụ/đệ có sức đánh 1–30% (option50), giảm sát thương 1–50% (94), HP/KI/TNSM/vàng rơi/tốc độ chạy độc lập 1–100% (77/103/101/100/114); chống lạnh (106), hạn dùng random 3–7 ngày (93), nhãn sự kiện2026 (174). Mỗi lần đổi ra bộ chỉ số riêng; giá giữ nguyên. Đồ đã cấp không bị sửa lại. Random đều trong khoảng, không cấp cố định chỉ số tối đa. Đào541/542 rơi theo logic cũ: tỉ lệ cơ bản20% mỗi lần xét rơi ở khu, chọn ngẫu nhiên một loại. Cần một ô túi trống trước khi đổi. Menu có xác nhận, chặn gọi xa và xác nhận lặp; giao dịch kiểm đủ tổng số đào kể cả nhiều stack, trao đồ thành công rồi trừ đúng số đào dưới khóa chủ nhân, chặn các giao dịch/đồng bộ đang chờ. Nhãn đệ tử dựa trên mô tả SQL sẵn có; không thay quy tắc trang bị toàn server.

SQL có đủ 6 cải trang, đào và NPC. Kiểm tra 18 part khớp binary `SRC/data/update_data/part`, 143 icon đủ ảnh4zoom, 12 file quái đủ4zoom. Đường dẫn phục vụ icon: `SRC/data/icon/x{zoom}/{id}.png`; quái: `SRC/data/mob/x{zoom}/{id}`. Client `SmallImage.createImage` thử resource, nếu thiếu gọi `Service.requestIcon` lấy ảnh server. Body470/leg471 của NPC Ngộ Không là phần trong suốt có chủ ý, không phải thiếu ảnh. Sa Ngộ Tĩnh chưa có template/part/ảnh được xác nhận nên chưa đưa vào đổi.

Migration `SRC/sql/migrations/20261010_ngu_hanh_son.sql` áp dụng lại an toàn, đọc cả NPC array và NPC chuỗi JSON cũ, giữ quái và NPC không liên quan. Đã cập nhật4 seed và database đang dùng. Phiên bản map/NPC từ6 lên7 để client làm mới cache khi đăng nhập.

Kiểm chứng: full sourceJava17; actual NPoint x5/ngoài khu/chống tràn; giá, nhiều stack, thiếu đào, sai loại, túi đầy, trao thất bại, giao dịch đồng thời, menu xác nhận lặp/gọi xa; regressions GokuDayGift/MillionItemStack/GiftCodeRewardDelta; import migration2lần; toàn bộ scriptUnity biên dịch ở project riêng và renderer hai tab chạy kiểm thử. Đã đối chiếu NPC/map/quái/menu trong JVM. Chưa có kiểm tra thao tác bằng tài khoản người chơi hoặc ảnh chụp gameplay cuối.

JAR cũ và metadata SHA256 nằm `SRC/jar-backup`. SQL đầy đủ mới nhất xem `.analysis/sql-export-latest.json`; restore độc lập đối chiếu đủ75bảng. Dữ liệu export là thời điểm sao lưu, không thay thế tài nguyên/JAR/client.

## Sửa cửa chuyển map lúc 21:48

Bản trước đặt tuyến tiến sang trái, cửa123→124 ở Y384–408, và công cụ kiểm tra dùng sai tile set chỉ số1 thay vì `tile_id-1=0`. Đã sửa hướng, vùng cửa và độ cao đến theo tile set server. Kiểm thử SQL kiểm nền thật và điểm đến; Java chạy MapService lookup/getMapCanJoin/yPhysicInTop thật. Agent trong JVM đã xác nhận bốn cửa122→123,123→122,123→124,124→123 đều nhận vị trí đi bộ, đích có khu nhận và đúng nền. NPC cuối cũng căn xuốngY288. Server PID2508 đã mở cổng14445 sau cập nhật.

SQL đầy đủ cuối: `SRC/sql/team2026-full-20261010-214830.sql`, đã thử khôi phục đủ75bảng. Bản21:40/21:46 là các snapshot trước khi hoàn tất sửa, không dùng làm bản mới nhất.

## Random cải trang lúc 22:25

Đã kiểm thử100lần đổi mỗi loại (600lần), xác nhận đủ option và đúng trần, chỉ số/hạn dùng có biến thiên. Kiểm thử thanh toán/túi đầy/chặn replay/đồng thời cùng ba bộ hồi quy đều đạt trên classes và JAR ứng viên. JAR cũ đã backup/hash trước khi thay, bảo trì lưu dữ liệu. SQL đầy đủ mới nhất: `SRC/sql/team2026-full-20261010-222524.sql`, restore đủ75bảng. Quy tắc random nằm trong JAR, cần dùng JAR mới cùng dữ liệu/tài nguyên hiện tại.

## Tăng rơi Hồng Đào lên 20%

Cả ba map122/123/124 có tỉ lệ cơ bản20% rơi một quả541 hoặc542, chọn đều giữa hai loại. Áp dụng sư phụ và đệ đánh quái; quyền nhặt của chủ nhân theo ID đệ âm hiện có. Giữ các hệ số may mắn/set Gohan trong isLuckyDrop. Kiểm thử luồng Mob.getItemMobReward thật: 5000lượt cho mỗi map và mỗi loại người đánh, kiểm tra cả map121/125 không rơi đào, đúng số lượng/quyền nhặt. Chỉ cập nhật Mob.class trong JAR; không đổi template SQL.
