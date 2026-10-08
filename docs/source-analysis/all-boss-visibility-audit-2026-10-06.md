# Rà soát boss bị tàng hình — 06/10/2026

Trạng thái: kiểm tra chỉ đọc trên server PID 9156, không sửa source ứng dụng, không thay JAR, không dừng server.

## Bằng chứng trực tiếp

Hai mẫu lúc **07:18:24 và 07:18:53, giờ Việt Nam**, quét 2.372 khu và các BossManager đã được khởi tạo. Công cụ không gọi `gI()` để khởi tạo manager mới, không kích hoạt sự kiện hoặc tạo boss.

- Thu được 437 đối tượng `Boss`, thuộc 84 lớp, gồm boss trong manager và boss đồng hành. Số này không phải số loại boss trong toàn bộ game.
- 371 đối tượng ở `ACTIVE` hoặc `CHAT_S`, đã được gán map/khu: **cả 371 thiếu trong cả `zone.bosses` và `zone.humanoids` ở cả hai mẫu**.
- 55 `REST`, 10 `AFK`, 1 `JOIN_MAP` được tách khỏi số lỗi đang hoạt động; boss chờ/chưa spawn không được tự động kết luận tàng hình.
- Hai mẫu không phát hiện deadlock tại thời điểm lấy mẫu. Đây không phải chứng minh server không thể deadlock.

| Nhóm boss hoạt động | Số thiếu đăng ký khu |
|---|---:|
| Yardrat | 180 |
| Mabu 12h | 70 |
| Mabu 14h | 14 |
| Broly | 30 |
| Boss mini | 30 |
| Nappa | 15 |
| Tàu Pảy Pảy | 10 |
| Android | 6 |
| Boss Trái Đất | 5 |
| Xên | 2 |
| Baby | 2 |
| Black Goku | 2 |
| Cold, Frieza, Cumber, tiểu đội sát thủ, tiểu đội sát thủ Namek | 5 |
| **Tổng** | **371** |

Manager tương ứng: YardartManager 180, FinalBossManager 94, BrolyManager 30, BossManager 67.

## Nguyên nhân đã xác nhận

1. `SRC/src/nro/models/boss/Boss.java`, `joinMap()` và `joinMapByZone()` chọn khu bằng cách gán trước `this.zone`.
2. `SRC/src/nro/models/map/service/ChangeMapService.java`, `commitToMap()` trả về thành công ngay khi `player.zone == zoneJoin`, trước `addPlayer()`. `commitEventMap()` có cùng kiểu bỏ qua đăng ký.
3. BossManager vẫn gọi `update()` độc lập với danh sách khu, nên boss có thể đánh dù không được đăng ký vào khu.
4. `Zone.load_Another_To_Me()` lấy hình nhân vật từ `humanoids`. Người vào khu sau, đổi khu rồi quay lại hoặc đăng nhập lại sẽ thiếu hình boss.

**Phân biệt:** `changeMap()` vẫn có đường gọi `load_Me_To_Another()` gửi packet xuất hiện cho người đã đứng trong khu. Vì vậy 371 là số boss có trạng thái đăng ký sai đã đo được, không phải bằng chứng cả 371 đều tàng hình trên mọi client ở mọi thời điểm. Chưa quan sát hình boss trực tiếp trong Unity của người dùng.

## Phạm vi source và ảnh hưởng

Đã lập danh mục 99 phương thức `joinMap*` trong 93 file, sau khi bỏ comment; 44 phương thức vừa gán `zone` vừa gọi `changeMap`. Danh mục là công cụ tìm đường đi, không thay cho phân tích ngữ nghĩa Java. Nhóm dùng `super.joinMap()` chịu lỗi chung; nhóm dùng khu từ constructor hoặc `zoneFinal` cũng cần kiểm thử.

Các đường đặc biệt đã đối chiếu gồm Xên con, Broly/SuperBroly, Yardrat, Mabu 14h, boss luyện tập `TrainingBoss`, Nhân bản, Siêu hạng và võ đài Hạt Mít. Boss luyện tập/đấu riêng có cùng rủi ro source, nhưng OtherBossManager và các manager phó bản không có boss trong hai mẫu: **chưa có bằng chứng runtime cho những lượt chơi chưa diễn ra**. Event manager chưa khởi tạo không bị kích hoạt để kiểm tra.

Ảnh hưởng phụ của `bosses` rỗng:

- Chọn khu tránh boss trong `Boss.joinMap()` không còn nhận biết boss đang tồn tại.
- Một số kiểm tra mở đường phó bản trong `MapService` có thể coi đã hết boss dù boss vẫn sống.
- `Zone.addPlayer()` hiện không chống trùng ở `bosses`, `pets`, `nonInteractiveNPCs`. Sửa bằng cách thêm boss lặp lại mà không sửa tính duy nhất sẽ gây lỗi mới.
- `MapService.goToMap()` còn có đường tự chuyển khu ngoài sao đen/map 126; cần xét phạm vi dùng và hợp nhất kiểm tra admission để không tạo hai hành vi khác nhau.

## Boss lớn dùng hệ Mob

Quét riêng 150 đối tượng trong `zone.mobs`: 20 Hirudegarn, 30 Máy đo sức mạnh, 50 Vua bạch tuộc, 50 Robot bảo vệ. Những đối tượng này không dùng `zone.bosses/humanoids`; không được kết luận lỗi theo tiêu chí của `Boss`.

Source có packet map chứa mob; Hirudegarn có `sendBigBoss()` khi vào map và packet riêng khi đổi giai đoạn. Chưa xác nhận chúng mắc lỗi bỏ qua `addPlayer` này. Cần kiểm thử packet/client khi vào muộn, reconnect và đổi giai đoạn, nhất là giờ mở map 126. Mẫu lúc 07h không chứng minh hoạt động phiên 15h/22h.

## Tệp bằng chứng

- `.analysis/all-boss-visibility/runtime1.tsv`, `runtime2.tsv`: dữ liệu boss.
- Tệp `.managers.txt` kèm mỗi mẫu: thời gian, manager, số khu, deadlock.
- Tệp `.mobs.tsv` kèm mỗi mẫu: boss lớn hệ Mob.
- `.analysis/all-boss-visibility/source-join-inventory.json`: danh mục đường join source.
- `.analysis/all-boss-visibility/summary.json`: số liệu tổng hợp.
- `.analysis/AllBossVisibilityProbe.java`: công cụ chỉ đọc; tệp công cụ không phải JAR ứng dụng.

Kế hoạch sửa: `docs/superpowers/plans/2026-10-06-boss-visibility-membership-fix.md`.
