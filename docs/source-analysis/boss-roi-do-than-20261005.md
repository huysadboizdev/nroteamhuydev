# Kiểm tra boss rơi đồ Thần Linh — 05/10/2026

Phạm vi: đọc SQL đang dùng `team2026` và mã nguồn `SRC/src`. Không sửa SQL, server hoặc JAR; không thử giết boss trên server. Các tỉ lệ dưới đây là tính theo nhánh thực thi trong nguồn, không dựa vào comment.

## Tổng số

- Có **15 loại/lớp boss được viết nhánh rơi đồ thần**: 14 lớp `Boss` và Hirudegarn thuộc `BigBoss`.
- Theo luồng nhận sát thương và thưởng hiện tại, **14 loại có thể rơi khi đánh hạ bình thường**: 13 lớp `Boss` và Hirudegarn. Drabura 2 có nhánh 1% nhưng luồng đánh kết liễu không gọi thưởng.
- Đếm theo lớp boss, không đếm số bản xuất hiện trong các khu hoặc tách mỗi lần biến hình thành một boss riêng. Những tên/giai đoạn chung lớp được ghi cùng dòng.

## Danh sách và tỉ lệ

Tỉ lệ boss thường là xác suất rơi **1 món bất kỳ** trong nhóm 13 mẫu đồ thần, cho mỗi lần gọi `reward(plKill)` hợp lệ. Không phải tỉ lệ riêng của từng món.

| Boss / giai đoạn | Tỉ lệ | Mã nguồn chứa lần random |
|---|---:|---|
| Baby — 3 giai đoạn | 10% (10/100) | `SRC/src/nro/models/boss/Baby/Baby.java:30` |
| Black Goku / Super Black Goku | 5% (5/100) | `SRC/src/nro/models/boss/Black_Goku/BlackGoku.java:37` |
| Xên Hoàn Thiện / Siêu Bọ Hung | 5% (5/100) | `SRC/src/nro/models/boss/Cell/SieuBoHung.java:86` |
| Cooler / Cooler 2 | 5% (5/100) | `SRC/src/nro/models/boss/Cold/Cooler.java:40` |
| Cumber / Super Cumber | 5% (5/100) | `SRC/src/nro/models/boss/cumber/Cumber.java:37` |
| Bui Bui — bản 1, map 115 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/BuiBui.java:42` |
| Bui Bui — bản 2, map 117 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/BuiBui2.java:49` |
| Ca Đít / Cadic — map 119 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/Cadic.java:234` |
| Drabura — bản 1, map 114 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/Drabura.java:158` |
| Drabura — bản 2, map 119 | Có cấu hình 1%; không rơi khi đánh hạ thông thường | `SRC/src/nro/models/boss/MajinBuu_12h/Drabura2.java:53` |
| Drabura — bản 3, gọi cùng Mabư | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/Drabura3.java:69` |
| Gôku — map 119 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/Goku.java:235` |
| Mabư 12h — map 120 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/Mabu.java:47` |
| Ya côn — map 118 | 1% (1/100) | `SRC/src/nro/models/boss/MajinBuu_12h/Yacon.java:49` |
| Hirudegarn — Thành phố Santa, map 126 | 2,5% (1/40) mỗi lần chuyển giai đoạn sau khi bị hạ | `SRC/src/nro/models/mob_bigboss/Hirudegarn.java:106` |

## Đồ có thể rơi và tỉ lệ chọn loại đồ

SQL `item_template` có đủ **13 mẫu**, ID 555–567. Không có bảng/cột boss hoặc drop trong schema hiện tại; tỉ lệ boss và bảng chọn đồ được viết trong Java.

| Loại | ID trong SQL | Tỉ lệ chọn khi boss đã random rơi thành công |
|---|---|---:|
| Nhẫn Thần Linh, dùng chung | 561 | 10% |
| Găng: Trái Đất / Namếc / Xayda | 562 / 564 / 566 | 22,5% |
| Quần: Trái Đất / Namếc / Xayda | 556 / 558 / 560 | 30,375% |
| Áo: Trái Đất / Namếc / Xayda | 555 / 557 / 559 | 27,84375% |
| Giày: Trái Đất / Namếc / Xayda | 563 / 565 / 567 | 9,28125% |

Trong mỗi nhóm áo/quần/găng/giày, ba hành tinh được chọn đều nhau (1/3); không khóa theo hành tinh người đánh. Xác suất riêng một món = tỉ lệ rơi của boss × tỉ lệ chọn loại × 1/3; nhẫn không chia 3. Ví dụ Baby rơi nhẫn là 10% × 10% = **1% mỗi lần thưởng**.

Hàm `ItemService.randDoTLBoss` ở dòng 1011 dùng chuỗi `if / else if` với các lần random mới: 10%, 25%, 45%, 75%, rồi nhánh còn lại. Vì vậy tỉ lệ gộp là bảng trên. Comment ghi găng 15%, quần 20%, áo 30%, giày 25% **không khớp cách chạy thực tế**. `Util.isTrue` ở dòng 204 so sánh random với tử số; các tỉ lệ 10/100, 5/100, 1/100 là đúng trực tiếp.

## Những điều kiện cần lưu ý

1. **Drabura 2:** trong `injured`, dòng 131–134, sát thương lớn hơn hoặc bằng HP bị đổi thành 0 và boss chuyển `AFK`. `afk()` chuyển trạng thái `DIE`; vòng cập nhật `Boss` dòng 381 chỉ chuyển sang chat kết thúc, không gọi `die(plKill)` hoặc `reward`. Vì vậy nhánh rơi 1% đang có nhưng không tới được bằng đòn đánh kết liễu thông thường.
2. **Hirudegarn:** SQL map 126 có mob ID 70, HP 40.000.000; `Map.initMob` tạo đúng lớp Hirudegarn. Chỉ random **áo 555 / 557 / 559**, đều nhau, mỗi áo **0,833333…% mỗi giai đoạn**. Ba lần chuyển giai đoạn 0→1, 1→2, 2→3 đều chạy random, sau khi chết trên 5 giây. Một chu kỳ đủ ba lần có xác suất nhận ít nhất một áo **7,3140625%**, có thể rơi nhiều áo. Đồ có chủ sở hữu `-1`, không gắn riêng người kết liễu.
3. **Option áo Hirudegarn có nguy cơ sai loại:** mã tạo áo cố định rồi lấy toàn bộ option từ một món `randDoTL` random riêng. Món nguồn có thể là quần, găng, giày hoặc nhẫn, nên option sao chép có thể không phù hợp áo. Chỉ ghi nhận, chưa sửa.
4. **Boss nhiều giai đoạn:** cùng một lớp dùng chung hàm thưởng và tỉ lệ; không coi tỉ lệ là một lần duy nhất cho cả chuỗi biến hình. Siêu Bọ Hung có luồng gọi Xên con rồi tự nổ; `Boss.setBom` dòng 842 vẫn gọi `die(plAtt)` và thưởng.
5. **Người nhận:** boss thường tạo đồ với `plKill.id`. Hàm `Boss.die` mặc định chỉ thưởng khi có người kết liễu và không phải bot; một số boss Mabu 12h tự ghi đè `die` nên điều kiện không hoàn toàn giống hàm mặc định.
6. **Quái thường không tính vào tổng boss:** `Mob.java:897` còn có nhánh đồ thần ở map Cold 105–110, đi qua `isLuckyDrop(player,1,50000)` (mức cơ sở 0,002% trước điều chỉnh may mắn của hàm). Hirudegarn ở map 126 nên không hưởng nhánh Cold này.

Chứng cứ truy vấn/source: `.analysis/boss-divine-audit-20261005/sql-source-audit.json`. Tổng số và tỉ lệ tính lại: `.analysis/boss-divine-audit-20261005/verified-rates.json`; script đối chiếu `finalize.py` chỉ đọc SQL.
