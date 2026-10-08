# Kế hoạch sửa đăng ký khu và hiển thị boss

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Boss đã spawn được đăng ký đúng khu, hiện với người đang đứng trong khu và người vào sau/reconnect, không nhân đôi boss hoặc vượt điều kiện vào map.

**Architecture:** Sửa điểm commit chuyển khu dùng chung trước, thay vì chắp vá riêng Xên. Đăng ký khu phải bổ sung danh sách còn thiếu và không thêm trùng; giữ bộ lọc chủ sở hữu, giới hạn người chơi, giờ sự kiện và thứ tự khóa hiện có. Sau đó xử lý các đường join chuyên biệt có gán trước khu hoặc bỏ qua admission.

**Tech Stack:** Java 17, JDK 21, server NRO, bộ test Java `main` hiện có, JAR đang chạy `SRC/20.jar`.

**Trạng thái:** đã triển khai sửa điểm commit dùng chung, chống đăng ký trùng, chuyển boss giữ nguồn và quy tắc ưu tiên khu 0 vào 07:29 ngày 06/10. 62 nhóm test PASS; hai mẫu sau cập nhật 362/363 boss đã spawn, không còn thiếu/trùng đăng ký. Chi tiết và giới hạn kiểm chứng: `docs/source-analysis/boss-visibility-zone-entry-fix-2026-10-06.md`. Các checkbox dưới đây giữ nội dung kế hoạch ban đầu; không dùng để khẳng định mọi lượt kiểm tra client thủ công đã được thực hiện.

## 1. Tạo test tái hiện trước khi sửa

**Create:** `SRC/test/nro/models/map/BossMembershipTest.java`.
**Reuse:** `SRC/test/nro/models/services/PinkFixture.java`.

- [ ] Viết test `preassigned`: boss đã có `zone` nhưng chưa có trong các danh sách; `goToMap()` phải thêm đúng một lần vào `humanoids` và `bosses`, không tăng số người chơi.
- [ ] Chạy trên JAR hiện tại để xác nhận thất bại vì thiếu đăng ký, không phải vì fixture.
- [ ] Thêm test `repeat`, `partial`, `remove`, `race`: gọi lặp, thiếu một danh sách, rời khu và hai lượt đăng ký đồng thời cùng đối tượng.

Phần tái hiện nhỏ nhất, dùng fixture không gọi constructor BossManager:

```java
PinkFixture.install();
Zone zone = PinkFixture.zone();
Player boss = PinkFixture.player(-901);
boss.isBoss = true;
boss.isPlayer = false;
boss.zone = zone;
PinkFixture.check(ChangeMapService.gI().goToMap(boss, zone), "admitted");
PinkFixture.check(zone.getHumanoids().contains(boss), "boss visible to late entrant");
PinkFixture.check(zone.getBosses().contains(boss), "boss participates in zone checks");
PinkFixture.check(zone.getNumOfPlayers() == 0, "boss does not consume player slot");
```

Test này chứng minh hợp đồng `Player.isBoss`; thêm fixture `SieuBoHung` tạo bằng `PinkFixture.blank()` để kiểm tra cả hành vi lớp Boss thật và callback notice.

Lệnh build/run tối thiểu từ root, sau khi đã tạo class test với `main`:

```powershell
$bossTestRoot = '.analysis/boss-membership-tests'
New-Item -ItemType Directory -Force $bossTestRoot | Out-Null
& 'C:/Program Files/Java/jdk-21.0.12/bin/javac.exe' --release 17 -encoding UTF-8 -cp 'SRC/20.jar;SRC/lib/*' -d $bossTestRoot SRC/test/nro/models/services/PinkFixture.java SRC/test/nro/models/map/BossMembershipTest.java
& 'C:/Program Files/Java/jdk-21.0.12/bin/java.exe' -cp "$bossTestRoot;SRC/20.jar;SRC/lib/*" nro.models.map.BossMembershipTest preassigned
```

Kỳ vọng trước sửa: assertion `boss visible to late entrant` thất bại. Sau sửa: các chế độ in PASS, exit 0; chạy với thư mục class ứng dụng mới đứng trước JAR trong classpath.

## 2. Làm đăng ký khu không thêm trùng

**Modify:** `SRC/src/nro/models/map/Zone.java`, `addPlayer()` và kiểm tra `removePlayer()`.

- [ ] Bảo vệ đăng ký cùng một đối tượng bằng khóa `player`, tương thích `goToMap()` đã giữ khóa này. Không thêm khóa toàn khu bao trùm chuyển pet/rời khu.
- [ ] Giữ nguyên phân loại player/boss/pet/NPC hiện tại; bổ sung `contains` cho ba danh sách còn thiếu, trong cùng khóa đăng ký:

```java
if (player instanceof NonInteractiveNPC && !nonInteractiveNPCs.contains(player)) {
    nonInteractiveNPCs.add(player);
}
if (player.isBoss && !bosses.contains(player)) {
    bosses.add(player);
}
if ((player.isPet || player.isNewPet) && !pets.contains(player)) {
    pets.add(player);
}
```

- [ ] Giữ đăng ký `humanoids`, `players`, `notBosses` đúng một lần. Test partial phải phục hồi từng danh sách độc lập, không chỉ kiểm tra `humanoids` rồi bỏ qua.
- [ ] Test remove phải loại đối tượng khỏi mọi danh sách đã đăng ký. Không tạo thao tác tự thêm lại boss `REST`, `AFK`, chết hoặc dispose.
- [ ] Chạy `repeat`, `partial`, `remove`, `race`; kiểm tra cả pet/new pet/NPC và số người chơi. Rà caller giữ khóa trước khi bổ sung khóa vào remove để tránh đảo thứ tự khóa.

## 3. Sửa commit cùng khu

**Modify:** `SRC/src/nro/models/map/service/ChangeMapService.java`, `commitToMap()` và `commitEventMap()`.

- [ ] Thay nhánh chỉ trả về true bằng đăng ký idempotent trước khi trả về; không gọi `exitMap()` nếu chỉ đang hoàn tất đăng ký vào chính khu đó.

```java
if (player.zone == zoneJoin) {
    boolean missingBoss = player.isBoss && !zoneJoin.getBosses().contains(player);
    zoneJoin.addPlayer(player);
    if (missingBoss && player instanceof nro.models.boss.Boss boss) {
        nro.models.services.BossNoticeService.gI().appeared(boss);
    }
    return true;
}
```

- [ ] Áp dụng cùng nguyên tắc cho `destination` trong `commitEventMap()`, sau các kiểm tra `prepareZone`, admission, capacity và `isOpen`, trong event gate như hiện tại. Không cho trường hợp cùng khu bỏ qua phiên đã đóng.
- [ ] Giữ thứ tự khóa `player -> admission/event gate`; giữ việc cleanup khu cũ và di chuyển pet ngoài event gate.
- [ ] Kiểm tra cập nhật lại cùng khu khi danh sách đã đầy đủ không tăng slot, không tạo notice mới và không xóa trạng thái PK/hiệu ứng.
- [ ] Chạy test preassigned với boss thật, partial, repeat; test map 126 đang mở/đã đóng, sao đen đầy/đã kết thúc, dungeon đang đóng.

## 4. Rà các đường spawn, đổi khu, đổi giai đoạn

**Modify when required by failing fixtures:** `SRC/src/nro/models/boss/Boss.java`; các file trong `.analysis/all-boss-visibility/source-join-inventory.json`; `SRC/src/nro/models/map/service/MapService.java`.
**Create:** `SRC/test/nro/models/boss/BossJoinVisibilityTest.java`.

- [ ] Dùng danh mục 99 phương thức để kiểm tra mọi override và đường kế thừa. Đối với spawn ban đầu gán trước khu, bản sửa commit phải đủ để đăng ký; tránh sửa hàng loạt các override nếu test đã đạt.
- [ ] Với đường di chuyển boss đang có trong khu cũ, dùng biến `Zone destination` thay cho ghi đè `this.zone` trước commit; kiểm tra khu cũ không giữ boss sau chuyển.
- [ ] Sau join bị từ chối, không chuyển sang `CHAT_S/ACTIVE` hoặc đánh người; giữ cơ chế retry/rest phù hợp loại boss. Không thông báo spawn thành công trước đăng ký.
- [ ] Viết fixture riêng cho Xên hoàn thiện/Xên con, Broly/SuperBroly, boss cha/con, các level đổi hình, Yardrat, Mabu 12h/14h, Nhân bản, Siêu hạng, Hạt Mít và TrainingBoss. Kiểm tra leave, death, respawn không để bóng boss cũ hoặc đăng ký trùng.
- [ ] Rà caller `MapService.goToMap()` trước khi hợp nhất sang `ChangeMapService.goToMap()`. Nếu thay đường này, phải test dungeon admission và giữ semantics của caller không tự gửi packet map.
- [ ] Kiểm tra logic chọn khu tránh boss và điều kiện diệt hết boss của phó bản với danh sách boss đã được phục hồi. Giữ chuỗi gọi boss đồng hành và thời gian spawn riêng từng sự kiện.

## 5. Kiểm tra packet và quyền nhìn thấy

**Create:** `SRC/test/nro/models/map/BossVisibilityWireTest.java`.
**Inspect:** `Zone.load_Me_To_Another()`, `load_Another_To_Me()`, `infoPlayer()`, `sendBigBoss()` và `SRC/src/nro/models/mob_bigboss/`.

- [ ] Dùng player fixture ghi lại `Message(-5)`; xác nhận boss xuất hiện với người đã ở khu, người vào sau và người reconnect, đúng ID/head/body/leg.
- [ ] Hai người trong map offline: chỉ chủ của TrainingBoss nhận hình và đối thủ đúng, người còn lại không thấy boss riêng. Không bỏ bộ lọc offline để chữa hình.
- [ ] Boss `AFK/REST` chưa đến lượt không được lộ ra vì thao tác sửa membership. Test thứ tự packet biến hình và rời khu `-6`.
- [ ] Boss lớn hệ Mob: kiểm tra packet map và packet riêng cho Máy đo sức mạnh, Robot bảo vệ, Vua bạch tuộc, Hirudegarn. Không thêm chúng vào `zone.bosses`.
- [ ] Kiểm tra Hirudegarn đủ ba giai đoạn, vào giữa phiên và reconnect, kết thúc đúng 16h/23h. Sửa packet riêng chỉ nếu test chứng minh thiếu hoặc sai; hiện chưa xác nhận lỗi cùng nguyên nhân ở hệ Mob.

## 6. Build và kiểm tra hồi quy

- [ ] Compile toàn bộ source ứng dụng vào thư mục build riêng; tạo candidate JAR, chưa ghi đè `SRC/20.jar`.
- [ ] Chạy toàn bộ nhóm test membership/join/wire mới, và các test hiện có: `BlackBallCapacityTest`, `DungeonAdmissionTest`, `DungeonClosingBehaviorTest`, `DungeonTimingContractTest`, `DungeonTimeoutRegressionTest`, `HirudegarnEventTest`, `SatelliteRegressionTest`, cùng test boss notice. Dùng đúng các mode khai báo trong từng test; không tự phỏng đoán mode.
- [ ] Chạy test đồng thời join/leave/đổi khu và phiên map 126 đóng trong lúc admission. Yêu cầu không deadlock, không vượt slot, không trùng boss, không mở lại dungeon đã đóng.
- [ ] Đối chiếu entry class của candidate với bản hiện tại; liệt kê chính xác class đổi và SHA-256.

## 7. Cài bản sửa và xác nhận trên server

Đã được người dùng yêu cầu triển khai và đã cài JAR với bảo trì lưu dữ liệu, backup/hash, khởi động lại và hai mẫu runtime. Các lượt quan sát Unity/phó bản/sự kiện đúng giờ vẫn phân biệt với bằng chứng packet và clock fixture.

- [ ] Bảo trì có lưu player/dữ liệu; xác nhận không còn JVM chạy `-jar 20.jar` trước khi thay file.
- [ ] Chuyển JAR cũ vào `SRC/jar-backup/20-before-boss-visibility-<timestamp>.jar`, ghi SHA-256, metadata phiên bản và đường rollback. Không ghi đè backup, không di chuyển thư viện/tools cần dùng.
- [ ] Cài candidate thành `SRC/20.jar`; đối chiếu hash, khởi động server và kiểm tra class thực tế đang chạy. Gom candidate/app JAR dư vào backup theo AGENTS.md.
- [ ] Lấy ít nhất hai mẫu runtime sau khi boss đã spawn: `ACTIVE/CHAT_S/CHAT_E` có khu phải có đúng một đăng ký trong `bosses` và `humanoids`. Loại trừ boss đang nghỉ và boss Mob đúng loại.
- [ ] Vào võ đài Xên map 103: thấy Xên hoàn thiện, nhận đòn, đổi khu rồi quay lại, logout/login vẫn thấy. Kiểm tra tiếp Broly, Yardrat, Mabu và một lượt phó bản/luyện tập; kiểm tra phiên Tapion khi đúng giờ.
- [ ] Ghi lại số test PASS, hash JAR, backup, kết quả mẫu runtime và tình huống client đã kiểm tra. Nếu kết quả sai, dừng bằng bảo trì và rollback backup; không sửa membership hàng trăm boss trực tiếp trên server đang chạy.

**Điều kiện hoàn thành:** không còn mismatch ổn định ở boss đang spawn; không đăng ký trùng; người vào muộn/reconnect thấy boss; boss riêng giữ quyền chủ sở hữu; thời gian sự kiện, số khu và giới hạn người chơi vẫn đúng.
