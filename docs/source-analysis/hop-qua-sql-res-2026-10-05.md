# Kiểm tra hộp quà SQL, res và đường dẫn — 05/10/2026

Đối chiếu DB `team2026.item_template`, dispatcher trong `SRC/20.jar` đang dùng, source Java và client Unity hai tab tại `PRJ_2Tab_550K`. Kiểm tra chỉ đọc: không sửa SQL/JAR/res, không cấp hoặc mở đồ trên tài khoản thật.

## Kết quả

- Có **86 mục tên hộp/rương/capsule**. **62 mục không mở nhận thưởng**, đã tái hiện offline trên JAR thật. **17 hộp/capsule có nhánh mở thưởng**; 7 mục còn lại là capsule di chuyển, máy dò hoặc nguyên liệu/phiếu/mở rộng rương.
- Với 62 mục không mở: gửi đúng luồng sử dụng không tạo thưởng, không trừ hộp, không đổi tiền và không có thông báo lỗi. Callback kiểm tra nhiệm vụ dùng vật phẩm vẫn được gọi; điều đó không chứng minh hộp đã mở.
- Không thấy thiếu icon server ở x1/x2/x3/x4 cho các mục này. Đã kiểm tra 204 file PNG khác nhau: chữ ký, CRC chunk, dữ liệu nén, IEND và kích thước lớn hơn 1×1 đều hợp lệ. Đây là kiểm tra file, không chứng minh mỗi icon vẽ đúng nội dung hộp.
- Cột “có nhánh mở thưởng” xác nhận đường code tồn tại, **chưa chứng minh mọi nhánh random/đầy túi/phần thưởng đều chạy đúng**; không gộp kết quả đó với 62 ca đã tái hiện.

## Các điểm cần lưu ý trước khi đưa vào shop/quà sự kiện

| Nhóm | ID | Kết luận |
| --- | --- | --- |
| Hộp Thần Linh | 1537 / 1775 | Cùng tên và icon 14520; **1537 không mở**, **1775 có xử lý**. 1775 tạo ngẫu nhiên đồ Thần Linh 555–567, không phải engine đồ ấn mới. |
| Hộp sự kiện | 1227 / 1228 / 1776 / 1777 | **1227/1228 không mở**; **1776/1777 có xử lý**. Không dùng lẫn chỉ vì tên/icon tương tự. |
| Quà gói từ menu | 1506 / 1510 / 1511 | 1506 mở menu ghép; `NpcFactory` thực sự tạo 1510/1511 nhưng **hai hộp đầu ra đều không có nhánh mở**. |
| Quà thiếu nhi | 1605 / 1606 / 1607 / 1608 | Chỉ **1608 có xử lý**; ba ID còn lại không mở. |
| Quà Cađíc | 1755 / 1756 / 1757 | Chỉ **1757 có xử lý**. |
| Capsule kích hoạt | 1559 / 1655 | **1559 không mở**; **1655 có menu chọn áo/quần/găng/giày/rada** và callback `OpenSKH`. |
| Capsule Vỡ | 1634 | Không mở trực tiếp; là nguyên liệu tái tạo capsule cùng khoáng 1656 ở Bà Hạt Mít. Không đánh dấu là hộp thưởng hỏng. |
| Phiếu đổi Capsule | 1690 | Chỉ thấy catalog, chưa thấy code đổi đúng ID này. Không tính vào 62 hộp/capsule không mở. |

Các số 1398..1403, 1510, 1559, 1671... xuất hiện trong `Pet2` là mã phần hình của pet; việc trùng số với ID item không phải bằng chứng hộp có xử lý. Các hằng ConstItem cũng có tên cũ lệch SQL ở một số ID; phân loại dựa trên ID và đường gọi thực, không dựa riêng tên hằng.

## Toàn bộ hộp/capsule chưa mở được

Đã thử tất cả ID dưới đây trên dispatcher JAR thật, nhân vật giả và stub network/DAO. “Không mở” nghĩa là không mở thưởng trực tiếp, không kết luận icon hoặc mọi khả năng sử dụng gián tiếp đều vô dụng.

| ID | Tên trong SQL | Type | Icon |
| --- | --- | --- | --- |
| 397 | Hộp quà tết | 27 | 2987 |
| 398 | Hộp quà tết | 27 | 2987 |
| 473 | Hộp bánh Trung Thu | 31 | 4126 |
| 571 | Rương Bạc | 27 | 5006 |
| 572 | Rương Vàng | 27 | 5007 |
| 573 | Capsule Bạc | 27 | 5005 |
| 574 | Capsule Vàng | 27 | 5004 |
| 627 | Capsule quà tặng | 27 | 2759 |
| 668 | Hộp quà Tết 2019 | 27 | 2987 |
| 737 | Capsule Trung Thu | 27 | 6984 |
| 758 | Capsule Tết 2024 | 27 | 5004 |
| 796 | Hộp Capsule | 27 | 7222 |
| 818 | Capsule Halloween | 27 | 7356 |
| 869 | Capsule 1 sao | 27 | 7852 |
| 870 | Capsule 2 sao | 27 | 7853 |
| 871 | Capsule 3 sao | 27 | 7854 |
| 915 | Capsule Squid Game | 27 | 8443 |
| 984 | Capsule SEA games | 27 | 9109 |
| 1009 | Rương Tranh ngọc Namếc | 27 | 5007 |
| 1135 | Capsule World Cup | 27 | 10678 |
| 1136 | Capsule World Cup VIP | 27 | 10679 |
| 1173 | Rương Mảnh Thiên Sứ | 27 | 10854 |
| 1184 | Gói quà đặc biệt | 27 | 12397 |
| 1187 | Hộp quà Tết 2024 | 27 | 12398 |
| 1227 | Hộp quà thường | 27 | 11201 |
| 1228 | Hộp quà cao cấp | 27 | 11202 |
| 1314 | Hộp giấy | 27 | 11717 |
| 1356 | Hộp Kẹo Ma Quỷ | 27 | 11743 |
| 1369 | Hộp trà | 27 | 11798 |
| 1370 | Hộp trà hoa cúc | 27 | 11799 |
| 1397 | Rương radar | 29 | 5007 |
| 1398 | Rương giày | 29 | 5007 |
| 1399 | Rương nhẫn | 29 | 5007 |
| 1400 | Rương găng tay | 29 | 5007 |
| 1401 | Rương áo | 29 | 5007 |
| 1402 | Rương quần | 29 | 5007 |
| 1403 | Rương bí kíp tuyệt kỹ | 29 | 5007 |
| 1440 | Rương sao pha lê | 27 | 12180 |
| 1453 | Rương sao pha lê VIP | 27 | 12180 |
| 1501 | Hộp quà Lucky 2024 | 27 | 6983 |
| 1510 | Hộp quà nhẹ nhàng | 27 | 12762 |
| 1511 | Hộp quà chỉn chu | 27 | 12763 |
| 1516 | Hộp Mù Tình Yêu | 27 | 10892 |
| 1537 | Hộp thần linh | 29 | 14520 |
| 1538 | Hộp quà Set kích hoạt 5 sao | 27 | 11201 |
| 1559 | Capsule 1 món kích hoạt | 27 | 7223 |
| 1605 | Hộp quà thiếu nhi | 27 | 11006 |
| 1606 | Hộp quà thiếu nhi | 27 | 11202 |
| 1607 | Hộp quà thiếu nhi | 27 | 11201 |
| 1671 | Rương mùa hè | 27 | 12846 |
| 1695 | Hộp quà tháng 9 | 27 | 11006 |
| 1696 | Hộp quà tháng 9 VIP | 27 | 11202 |
| 1701 | Hộp bánh Trung Thu đặc biệt | 27 | 11717 |
| 1719 | Hộp quà 20/10 | 27 | 12762 |
| 1720 | Hộp quà 20/10 | 27 | 12762 |
| 1725 | Gói quà Halloween | 27 | 11739 |
| 1736 | Hộp quà FA | 27 | 12763 |
| 1738 | Hộp quà FA | 27 | 12763 |
| 1739 | Hộp quà 20-11 | 27 | 12763 |
| 1747 | Hộp quà Black Friday | 27 | 14514 |
| 1755 | Hộp quà Cađíc | 27 | 14520 |
| 1756 | Hộp quà Cađíc | 27 | 14520 |

## Hộp/capsule có nhánh mở thưởng

| ID | Tên trong SQL | Điều kiện/đường xử lý đáng chú ý |
| --- | --- | --- |
| 380 | Viên Capsule kì bí | openCSKB |
| 570 | Rương Gỗ | Rương Gỗ: kiểm tra thời gian mở theo ngày, option cấp rương và ô trống |
| 648 | Hộp quà giáng sinh | NoelItemBox |
| 722 | Capsule hồng ngọc | PinkCapsuleService: cộng ruby, kiểm tra số dư và giao dịch |
| 736 | Hộp quà 5 sao | ItemService.OpenItem736 |
| 1170 | Gói quà | BlackGokuItemBoxEventNoel |
| 1560 | Rương ngọc rồng | RuongNgocRong: cần chìa khóa ID 1561 và ô trống |
| 1591 | Hộp quà Goku Day | OpenHopQuaGokuDayVangNgoc |
| 1592 | Hộp quà Goku Day VIP | OpenHopQuaGokuDay |
| 1594 | Hộp quà Goku Day | OpenHopQuaGokuDayVangNgoc |
| 1608 | Hộp quà thiếu nhi | QuaThieuNhi |
| 1655 | Cápsule Kích hoạt 1 món tự chọn | Menu ConstNpc.CAPSULE_KICH_HOAT → ItemService.OpenSKH |
| 1757 | Hộp quà Cađíc VIP | OpenHopQuaCadic |
| 1775 | Hộp thần linh | OpenHopThanlinh: cần ô trống, ra đồ Thần Linh gốc |
| 1776 | Hộp quà sự kiện | OpenHopQuaThuong |
| 1777 | Hộp quà sự kiện VIP | OpenHopQuaVip |
| 1840 | Hộp quà Goku Day VIP | OpenHopQuaGokuDayVip |

## Các mục không phải hộp thưởng trực tiếp

| ID | Tên | Vai trò |
| --- | --- | --- |
| 193 | Gói 10 viên Capsule | Capsule di chuyển, có xử lý |
| 194 | Viên Capsule đặc biệt | Capsule di chuyển VIP, có xử lý |
| 379 | Máy dò Capsule kì bí | Máy dò capsule, có hiệu ứng thời gian |
| 518 | Mở rộng rương đồ | Mở rộng rương khi nhận vào hành trang |
| 1506 | Hộp đựng quà | Nguyên liệu gói quà; mở menu ghép, không phải hộp thưởng |
| 1634 | Cápsule Vỡ | Nguyên liệu tái tạo capsule tại Bà Hạt Mít |
| 1690 | Phiếu đổi Cápsule | Phiếu đổi capsule; chưa thấy nhánh đổi/sử dụng |

## res và path tải icon

`item_template.icon_id` là ID hình, **khác ID vật phẩm**. Ví dụ item 1537 và 1775 đều dùng icon 14520.

1. Client hai tab: `SmallImage.createImage(iconId)` tìm `/SmallImage/Small<iconId>.png`. `GameCanvas.loadImage` dùng `ClientResourcePaths.Candidates`, rồi `Image.__createImage` gọi Unity `Resources.Load`.
2. Đường file local: `PRJ_2Tab_550K/Assets/Resources/res/x<zoom>/SmallImage/Small<iconId>.png` (Unity bỏ đuôi .png khi load). Có fallback theo `ClientResourcePaths` nên thiếu riêng một file local không đủ kết luận mất hình.
3. Nếu local chưa có hình, client gọi `Service.requestIcon(iconId)`. Server `Controller` chuyển tới `DataGame.sendIcon` và đọc **`SRC/data/icon/x<zoom>/<iconId>.png`**; không đọc từ `data/res` ở nhánh request icon này.
4. Ví dụ đúng: `SRC/data/icon/x1/14520.png` tới `x4/14520.png`. Tất cả ID được rà đều có đủ file server 4 mức zoom, nên các hộp không mở ở danh sách trên thiếu xử lý sử dụng, không phải thiếu file icon.

Không gán nhãn “hộp quà chưa dùng” cho PNG chỉ có tên số mà không có ánh xạ SQL; chưa kiểm kê toàn bộ ảnh mồ côi/ảnh hiệu ứng khác ngoài 86 mục này.

## Cách admin kiểm tra lại

Chat admin `i <ID> 1` để nhận item với option mặc định; dùng nhân vật test và chừa đủ ô trống. Ví dụ `i 1537 1` và `i 1775 1`: hộp 1537 không mở, hộp 1775 có xử lý. Không đổi ID của vật phẩm người chơi thật để thử.

## Bằng chứng và phạm vi

- Script catalog/reference: `.analysis/audit_gift_boxes.py`; kết quả JSON/PNG trong `.analysis/gift-box-audit-20261005/`.
- Probe offline: `.analysis/GiftBoxUseAudit.java`, danh sách `.analysis/gift-box-audit-20261005/unsupported.csv`; fixture `SRC/test/nro/models/services/PinkFixture.java`. Classpath dùng JAR thật, không chèn class production thay thế.
- Probe xác nhận 62 ID không mở/không tiêu thụ/không thưởng, chạy không truy cập SQL hoặc socket thật. Việc nạp danh mục SQL để chọn ID là bước chỉ đọc riêng trước probe.
- Source trọng tâm: `services_func/UseItem.java`, `services/ItemService.java`, `npc/NpcFactory.java`, `services/PinkCapsuleService.java`, `data/DataGame.java`, `combine/TaiTaoCapsuleKichHoat.java`; client `Game1`/`Game2` có cùng tuyến tải icon.
- Bảng CSV đủ 86 mục: `hop-qua-sql-res-2026-10-05.csv`. Chưa sửa bất kỳ hộp nào trong lần kiểm tra này.

JAR SHA-256 khi kiểm tra: `adb03921a029864e4b611eba3d28c65abf341c19983d08007b54c29f14ebc5d5`.

## C?p nh?t h?p1775 sau ki?m k?

H?p1775 ?? ???c s?a v? tri?n khai theo y?u c?u: menu Nam?c/Tr?i ??t/Xayda, nh?n ??5 m?n ch? s? g?c0%, h?p v? ?? kh?ng giao d?ch/k? g?i, b?n50000ruby t?i Santa. Ph?n ki?m k? ph?a tr?n ph?n ?nh tr??c s?a (1775 t?ng m? m?t m?n random); c?c62 ID kh?ng m? v?n ch?a s?a. Chi ti?t trong `hop-than-linh-1775-2026-10-05.md`.
