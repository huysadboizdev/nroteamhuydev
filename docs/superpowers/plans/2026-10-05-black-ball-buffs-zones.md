# Kế hoạch đổi buff và giới hạn khu Ngọc Rồng Sao Đen

**Trạng thái:** Đã triển khai source, SQL và JAR vào server ngày 2026-10-05; 15 nhóm test và runtime đã xác nhận. Xem `docs/source-analysis/black-ball-buffs-zones-2026-10-05.md`. Thay đổi Git đang ở workspace, chưa commit/push.

**Mục tiêu:** 3 sao đổi thành +35% KI tối đa; 5 sao đổi thành +35% hút HP; 6 sao đổi thành +20 điểm phần trăm giáp giảm sát thương (`tlGiap`), bỏ +40% KI. Hành tinh 1–3 sao có 2 khu, 4–7 sao có 3 khu, mỗi khu tối đa 50 người chơi.

**Kiến trúc đề xuất:** Giữ cơ chế buff theo thời hạn trong `RewardBlackBall` và tính chỉ số trong `NPoint`. Định nghĩa số khu/sức chứa của sự kiện trong code, đồng bộ SQL để dữ liệu và lúc chạy khớp nhau. Kiểm tra sức chứa tại thời điểm nhận người vào khu, không chỉ lúc chọn khu.

**Công nghệ:** Java 17, MySQL, kiểm thử Java `main` theo cấu trúc hiện có.

## 1. Kết quả cần đạt

| Sao | Item ID | Map ID | Hiệu ứng sau thay đổi | Số khu | Người/khu |
|---|---:|---:|---|---:|---:|
| 1 | 372 | 85 | +21% sức đánh | 2 | 50 |
| 2 | 373 | 86 | +35% HP tối đa | 2 | 50 |
| 3 | 374 | 87 | +35% KI tối đa | 2 | 50 |
| 4 | 375 | 88 | +35% phản sát thương | 3 | 50 |
| 5 | 376 | 89 | +35% hút HP theo sát thương | 3 | 50 |
| 6 | 377 | 90 | +20 điểm phần trăm giáp giảm sát thương | 3 | 50 |
| 7 | 378 | 91 | +14 điểm phần trăm né đòn | 3 | 50 |

- Tổng: 18 khu, sức chứa 900 người chơi; mỗi hành tinh 1–3 sao chứa 100 người, 4–7 sao chứa 150 người.
- Giữ lịch 20h–21h, giờ nhặt từ 20h30, giữ ngọc 5 phút để thắng và buff 22 giờ.
- 3 sao bỏ hút HP; 5 sao bỏ tăng sát thương chí mạng. Các nguồn hút HP/chí mạng từ trang bị và buff khác vẫn giữ.
- 6 sao bỏ +40% KI, cộng 20 vào `tlGiap`. Dùng cơ chế giáp phần trăm hiện có, không tăng `def` thêm 20% và không tạo lớp giảm sát thương độc lập. Khi không có giáp hoặc hiệu ứng khác, sát thương 10.000 còn 8.000. Chịu ảnh hưởng xuyên giáp và đòn `piercing`, tổng giáp bị chặn ở 86% như code hiện tại; do đó không bảo đảm luôn giảm thêm 20% với mọi đòn đánh.
- Khi có cả 3 và 6 sao, KI chỉ được tăng 35% từ 3 sao; 6 sao tăng giáp. Không còn công thức KI +89% của bản kế hoạch trước.
- Sức chứa tính người chơi, không tính quái, boss hoặc đệ tử. Đề xuất áp dụng trần 50 cả với nhân vật admin vào sự kiện, tránh đường bypass vượt trần.

## 2. Hiện trạng và lựa chọn

- `RewardBlackBall.java`: `R3S_1=35`, `R5S_1=35`; giữ hai giá trị này. Đổi `R6S_1` từ 40 thành 20 và đổi ý nghĩa từ KI sang giáp phần trăm.
- `NPoint.setPointWhenWearClothes`: 3 sao cộng `tlHutHp`; 5 sao cộng `tlDameCrit` và `tlSDCM`.
- `NPoint` đang cộng +40% `mpMax` cho 6 sao. `Player.injured` dùng `tlGiap` giảm sát thương, xử lý xuyên giáp và chặn tổng giáp ở 86%.
- `Map.initZone`: map sao đen bị ép `nZone=BlackBallWar.AVAILABLE`, hiện là 5, dù SQL có giá trị khác.
- `map_template` trong dump: cả 7 map đang có `zones=10`, `max_player=15`; số khu thực tế bị code ghi đè.
- `MapService.getZone`: chọn ngẫu nhiên trong vòng `while`; khi mọi khu đầy có thể lặp vô hạn.
- `ChangeMapService.changeZone`: kiểm tra đầy trước chuyển, nhưng cần xác minh lại tại bước thêm người để không vượt 50 khi nhiều người vào đồng thời.
- Mô tả item trong cả hai dump và hai seed còn là phần thưởng cũ, khác code.

**Phương án A — đề xuất:** Chính sách khu trong code + SQL đồng bộ. Bảo đảm đúng yêu cầu kể cả database cũ chưa cập nhật, thêm kiểm thử chính sách và nhận người vào khu.

**Phương án B:** Bỏ override số khu trong code, lấy toàn bộ cấu hình từ SQL. Ít chính sách cố định hơn nhưng phụ thuộc migration đã chạy đúng trước khi khởi động server.

**Chỉ sửa SQL không đủ:** Code hiện luôn ép 5 khu.

## 3. Thực hiện theo thứ tự

### Bước 1 — Kiểm thử trước khi sửa

Tạo:

- `SRC/test/nro/models/player/BlackBallBuffTest.java`.
- `SRC/test/nro/models/services_dungeon/BlackBallCapacityTest.java`.

Dùng fixture hiện có và tính chỉ số thật qua `NPoint.calPoint()`. Chạy test để xác nhận thất bại vì hành vi cũ, không phải vì lỗi biên dịch hoặc fixture.

Các ca buff:

- 3 sao còn hạn: KI tăng 35%, hút HP không tăng.
- 5 sao còn hạn: hút HP tăng 35 điểm phần trăm; sát thương chí mạng không tăng.
- Buff hết hạn không cộng chỉ số; tính lại nhiều lần không cộng lặp.
- 6 sao còn hạn: `tlGiap` tăng 20 điểm phần trăm, KI không tăng từ viên này. Khi không có giáp/hiệu ứng khác, đòn 10.000 còn 8.000.
- Kiểm tra 6 sao cùng giáp sẵn có, xuyên giáp, đòn `piercing` và giới hạn giáp 86%; buff hết hạn mất đúng phần tăng của sao đen.
- 3 + 6 sao cùng hoạt động: KI tăng 35%, giáp tăng 20 điểm phần trăm.
- Hồi quy 1/2/4/7 sao vẫn giữ đúng giá trị.

Các ca khu:

- 85–87 có đúng 2 khu; 88–91 có đúng 3 khu; tất cả sức chứa 50.
- Người thứ 50 được vào; người thứ 51 bị từ chối và vẫn ở khu cũ.
- Hai người cùng vào khi có 49 người: chỉ một được vào.
- Tất cả khu đầy: trả kết quả thất bại nhanh, không vòng lặp vô hạn.
- ID khu ngoài danh sách bị từ chối, không tạo thêm khu.
- Boss/đệ tử không làm tăng số người chơi; admin không vượt trần được đề xuất.

### Bước 2 — Đổi buff

Sửa `SRC/src/nro/models/player/NPoint.java`:

```java
// Thay phần cộng hút HP của 3 sao bằng phần cộng hút HP của 5 sao.
if (player.rewardBlackBall.timeOutOfDateReward[4] > System.currentTimeMillis()) {
    tlHutHp += RewardBlackBall.R5S_1;
}
// Bỏ hai dòng tlDameCrit.add(R5S_1) và tlSDCM += R5S_1 của 5 sao.
```

Trong phần tính `mpMax`, thay khối buff KI của 6 sao bằng buff KI của 3 sao:

```java
if (player.rewardBlackBall.timeOutOfDateReward[2] > System.currentTimeMillis()) {
    mpMax += mpMax * RewardBlackBall.R3S_1 / 100L;
}
```

Trong `RewardBlackBall.java`, đổi `R6S_1=20`. Trong `NPoint.setPointWhenWearClothes`, sau reset và trước tính chỉ số trang bị:

```java
if (player.rewardBlackBall.timeOutOfDateReward[5] > System.currentTimeMillis()) {
    tlGiap += RewardBlackBall.R6S_1;
}
```

Kiểm tra sử dụng số `long` và làm tròn theo cách hiện có. Cập nhật chú thích ý nghĩa các hằng trong `RewardBlackBall.java`. Dùng xử lý giáp hiện có trong `Player.injured`, không nhân sát thương thêm lần thứ hai.

### Bước 3 — Số khu và sức chứa

Sửa `BlackBallWar.java` và `Map.java`:

- Thay chính sách 5 khu chung bằng 2 khu cho map 85–87, 3 khu cho map 88–91.
- Đặt `maxPlayer=50` tại nhánh khởi tạo map sao đen.
- Tìm toàn bộ nơi sử dụng `BlackBallWar.AVAILABLE` trước đổi để không còn giả định 5 khu.
- Giữ map khác dùng cấu hình hiện có.

Sửa có phạm vi trong `MapService.java` và `ChangeMapService.java`:

- Với sao đen, chọn trong danh sách khu còn chỗ bằng lượt duyệt hữu hạn; đầy toàn bộ trả `null` và thông báo khu đầy.
- Kiểm tra `getNumOfPlayers() >= maxPlayer` ngay trước khi chuyển người vào khu, dưới khóa của khu đích. Khi từ chối không rời khu cũ.
- Áp dụng thống nhất cho NPC Omega, chuyển khu, vào map theo ID và các đường đăng nhập/chuyển map liên quan sau khi truy vết.
- Kiểm tra thứ tự khóa để không tạo deadlock; không mở rộng sửa các phó bản ngoài phạm vi này.

### Bước 4 — SQL và mô tả

Tạo migration `SRC/sql/migrations/20261005_black_ball_buffs_zones.sql`:

```sql
UPDATE map_template SET zones = 2, max_player = 50 WHERE id IN (85,86,87);
UPDATE map_template SET zones = 3, max_player = 50 WHERE id IN (88,89,90,91);
UPDATE item_template SET description = '+35% KI tối đa cho toàn bang' WHERE id = 374;
UPDATE item_template SET description = '+35% hút HP theo sát thương cho toàn bang' WHERE id = 376;
UPDATE item_template SET description = '+20% giáp giảm sát thương cho toàn bang' WHERE id = 377;
```

Đồng bộ các giá trị này trong `team2026.sql`, `SRC/sql/nro1.sql`, `SRC/sql/team2026.seed.sql`, `SRC/sql/nro1.seed.sql`. Kiểm tra tên cột thực tế trước chạy migration. Đề xuất đồng bộ mô tả 4 viên còn lại với bảng hiệu ứng trên để client không tiếp tục hiện phần thưởng cũ; không thay hiệu ứng của các viên đó.

Migration chạy lại phải cho cùng kết quả. Không chỉnh dữ liệu tài khoản, người chơi hoặc thời hạn buff đang lưu.

### Bước 5 — Xác minh và ghi kết quả

- Biên dịch toàn bộ source Java 17 vào thư mục kiểm tra riêng, không thay JAR đang chạy.
- Chạy hai test mới và các test liên quan tính chỉ số, chuyển map/phó bản.
- Import seed vào database thử nghiệm; chạy migration hai lần và đối chiếu 7 map, 7 mô tả item.
- Kiểm tra diff chỉ bao gồm phạm vi đã chốt; ghi kết quả vào tài liệu release.

Lệnh test dự kiến sau khi biên dịch source và test vào thư mục riêng:

```powershell
java -cp '.analysis/black-ball-verify/tests;.analysis/black-ball-verify/classes;SRC/lib/*' nro.models.player.BlackBallBuffTest
java -cp '.analysis/black-ball-verify/tests;.analysis/black-ball-verify/classes;SRC/lib/*' nro.models.services_dungeon.BlackBallCapacityTest
```

## 4. Triển khai khi được yêu cầu

- Kiểm tra trạng thái server và lịch sự kiện; nếu server chạy, bảo trì có lưu dữ liệu, chờ dừng hoàn toàn trước đổi JAR.
- Sao lưu dữ liệu SQL bị tác động và ghi giá trị trước migration để khôi phục.
- Chuyển JAR cũ vào `SRC/jar-backup` với tên có thời gian, không ghi đè; lưu SHA-256 cũ/mới, metadata build và đường dẫn rollback.
- Cài JAR đã kiểm tra thành `SRC/20.jar`; thư mục chạy chỉ giữ JAR ứng dụng đang dùng.
- Áp dụng migration, khởi động và kiểm tra 18 khu/sức chứa, mô tả item, buff và đăng nhập.
- Chỉ commit/push các tệp được phép theo `.gitignore` khi người dùng yêu cầu lên GitHub.

## 5. Tiêu chí nghiệm thu

- Đúng toàn bộ bảng hiệu ứng và khu ở mục 1.
- 3 sao không hút HP, 5 sao không tăng chí mạng, 6 sao không tăng KI; không cộng buff lặp hoặc sau hết hạn.
- Người thứ 51 bị chặn kể cả khi vào đồng thời; không treo khi toàn bộ khu đầy.
- Source, SQL seed, migration và hành vi lúc chạy thống nhất.
- Có kết quả biên dịch/test, backup và khả năng rollback nếu triển khai.
