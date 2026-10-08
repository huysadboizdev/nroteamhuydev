# Tapion / Hirudegarn map 126 — kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Map 126 mở hằng ngày 15:00–16:00 và 22:00–23:00 giờ Việt Nam, có 20 khu, giữ 3 giai đoạn Hirudegarn nhưng bỏ thời gian chờ; hết phiên dùng tàu cưỡng chế về map 19 và chặn đường vào lại.

**Architecture:** Một service riêng quản lý lịch, phiên và vòng đời boss của map 126. NPC, chuyển map, đăng nhập và boss dùng cùng nguồn giờ `Asia/Ho_Chi_Minh`. Tái sử dụng `DungeonExitService.exit` cho tàu cưỡng chế; vòng xử lý nhanh riêng cho boss/đóng phiên, giữ nhịp cập nhật chung của map hiện tại.

**Tech Stack:** Java 17+, scheduler Java, MySQL, bộ kiểm thử Java hiện có và fixture packet tàu.

**Trạng thái:** Đã triển khai source, SQL và JAR theo yêu cầu người dùng. 27 nhóm kiểm thử đạt, runtime và SQL live đã xác nhận. Người dùng đã chọn giữ 3 giai đoạn, bỏ thời gian chờ. Xem `docs/source-analysis/hirudegarn-sessions-2026-10-05.md` về kết quả và giới hạn kiểm chứng; chưa commit/push.

## Quy tắc đã chốt

| Nội dung | Hành vi dự kiến |
|---|---|
| Phiên chiều | Cho vào từ 15:00:00 đến trước 16:00:00 |
| Phiên tối | Cho vào từ 22:00:00 đến trước 23:00:00 |
| Thời gian ở lại | Theo mốc kết thúc phiên; vào 15:55 chỉ còn 5 phút |
| Số khu | Chính xác 20 khu, ID 0–19 |
| Sức chứa | Giữ cấu hình hiện tại 15 người/khu; yêu cầu 50 người trước đó dành cho ngọc rồng sao đen |
| Boss | Một Hirudegarn độc lập mỗi khu; giai đoạn 1 chết → 2, 2 chết → 3, 3 chết → boss mới giai đoạn 1 đầy HP |
| Chờ hồi/chuyển | Bỏ các mốc chờ 5 giây, 15 giây và nhánh cũ 600 giây |
| Hết phiên | Đóng cổng, ngừng combat/phát thưởng/hồi boss và gọi tàu về thành phố Vegeta map 19 |
| Đăng nhập lại | Vị trí lưu ở 126 được đưa về 19 trước khi vào thế giới; muốn vào sự kiện phải qua Tapion trong giờ mở |
| Khởi động lại server | Không cấp thêm một giờ; nếu đang trong phiên, chỉ chạy đến mốc 16:00 hoặc 23:00 |

Đề xuất áp dụng lịch và cưỡng chế cho cả nhân vật admin đang chơi ở 126. Không dùng ngoại lệ admin của Mabư để kéo dài phiên này. Boss, pet và NPC không phải đối tượng gọi tàu.

Đúng mốc 16:00/23:00 là hết quyền ở trong sự kiện, không có 30 giây gia hạn. Packet tàu và hoàn tất chuyển map vẫn phụ thuộc scheduler/mạng. Mục tiêu fixture: xử lý ở lượt nhanh đầu tiên sau mốc đóng (nhịp 100 ms khi không quá tải), không hứa hoàn tất hoạt ảnh trong cùng mili giây.

## Hiện trạng đã kiểm tra

- `SRC/src/nro/models/npc_list/Tapion.java`: đang cho vào từ 1h đến trước 23h, thông báo lại chỉ nói 22h.
- `SRC/src/nro/models/player/Player.java`: đoạn đuổi map 126 bị comment và dùng chuyển không tàu.
- `SRC/src/nro/models/database/MrBlue.java`: vị trí đăng nhập thiếu xử lý map 126.
- `SRC/src/nro/models/mob_bigboss/Hirudegarn.java`: 3 giai đoạn, phát đồ ở mỗi lần chết/chuyển giai đoạn, đang có thời gian chờ 5/15 giây và nhánh 600 giây.
- `SRC/src/nro/models/map/Map.java`: mỗi khu khởi tạo một Hirudegarn từ template. `Map.run()` có scheduler 5.000 ms nhưng không được gọi ở luồng khởi động đang dùng.
- `SRC/src/nro/models/server/Manager.java`: `initMap()` khởi tạo mob/NPC rồi cập nhật Zone theo batch mỗi 1 giây. Luồng nhanh sự kiện phải được đăng ký ở đây sau khi map đã khởi tạo nội dung.
- `SRC/src/nro/models/services/DungeonExitService.java`: đã có tàu cưỡng chế, hồi sinh nếu chết, kiểm tra đúng khu gốc và trả kết quả để thử lại khi thất bại.
- Map 126 hiện có 10 khu, sức chứa 15 người/khu; SQL đặt tên `Thành phố Santa`, NPC vào là Tapion ở map 19.

## Chọn phương án

1. **Service riêng + vòng nhanh riêng cho map 126 — đề xuất:** gom lịch, đóng phiên và hồi boss vào một nơi; không tăng tần suất cập nhật Player/item của cả map.
2. Sửa rải rác NPC, Player, login và boss: ít lớp mới nhưng dễ lệch lịch, bỏ sót đường chuyển map và rơi lại vào lỗi đã audit.
3. Đổi toàn bộ `zone.update()` của map 126 xuống 100 ms: đơn giản hơn nhưng tăng cả nhịp Player, vật phẩm và timer khác khoảng 50 lần; không chọn.

## Phạm vi file

| File | Vai trò |
|---|---|
| Tạo `SRC/src/nro/models/services_dungeon/HirudegarnEventService.java` | Lịch có thể giả lập Clock, định danh phiên theo ngày/mốc mở, nhịp nhanh, đóng phiên, reset boss |
| Sửa `SRC/src/nro/models/npc_list/Tapion.java` | Dùng lịch chung, thông báo 2 khung giờ, từ chối khi hết giờ/đầy khu |
| Sửa `SRC/src/nro/models/map/service/MapService.java` | Chặn chọn map 126 ngoài giờ, tìm khu hữu hạn, bảo vệ đường chuyển trực tiếp |
| Sửa `SRC/src/nro/models/map/service/ChangeMapService.java` | Kiểm tra lại giờ tại lúc chốt chuyển, chống race với đóng phiên |
| Sửa `SRC/src/nro/models/database/MrBlue.java` | Chuẩn hóa vị trí login từ 126 về 19 |
| Sửa `SRC/src/nro/models/player/Player.java` | Xóa đoạn lịch cũ bị comment, không để hai handler cạnh tranh |
| Sửa `SRC/src/nro/models/map/Map.java` và `Zone.java` | Khởi tạo 20 khu, đăng ký vòng nhanh, boss 126 chỉ có một nơi update, snapshot danh sách người chơi an toàn |
| Sửa `SRC/src/nro/models/server/Manager.java` | Đăng ký luồng nhanh từ đường khởi động thực, sau khi mob/NPC map 126 đã khởi tạo; giữ nhịp Zone chung 1 giây |
| Sửa `SRC/src/nro/models/mob_bigboss/Hirudegarn.java` | Chuyển 3 giai đoạn không chờ, mỗi lần chết chỉ thưởng một lần, reset mỗi phiên |
| Tạo `SRC/sql/migrations/20261005_hirudegarn_sessions_zones.sql` | Chỉ cập nhật số khu map 126 lên 20 |
| Đồng bộ `SRC/sql/team2026.seed.sql`, `SRC/sql/nro1.seed.sql`, `team2026.sql`, `SRC/sql/nro1.sql` | Cùng cấu hình khu; hai dump riêng tư tiếp tục nằm ngoài Git |
| Tạo test trong `SRC/test/nro/models/services_dungeon/HirudegarnEventTest.java` | Giờ, vòng đời phiên, chuyển/đăng nhập/cưỡng chế |
| Tạo `SRC/test/nro/models/mob_bigboss/HirudegarnRespawnTest.java` | Giai đoạn, HP, packet, thưởng, race hồi boss với đóng phiên |

## Công việc triển khai

### 1. Lịch và định danh phiên

- [ ] Viết test biên 14:59:59.999, 15:00, 15:59:59.999, 16:00, 21:59:59.999, 22:00, 22:59:59.999, 23:00 và qua ngày.
- [ ] Chạy test trước khi thêm service, xác nhận thiếu hành vi mới.
- [ ] Service nhận `java.time.Clock`; dùng `TimeUtil.VIETNAM_ZONE`. Quy tắc mở: `hour == 15 || hour == 22`, mốc đóng tương ứng 16h/23h cùng ngày.
- [ ] Định danh phiên bằng ngày Việt Nam + giờ bắt đầu; phiên mới reset mỗi boss về giai đoạn đầu, đầy HP, vị trí hợp lệ, sạch hiệu ứng/combat từ phiên cũ.
- [ ] Khi server khởi động giữa phiên, tính mốc đóng từ giờ thực, không từ thời gian khởi động.
- [ ] Kiểm tra lại test và test múi giờ JVM khác UTC+7.

### 2. Cổng vào, sức chứa và đăng nhập

- [ ] Test NPC vào đúng giờ, từ chối ngoài giờ; thông báo `Mở cửa 15h–16h và 22h–23h hằng ngày`.
- [ ] Test chọn khu và chuyển map trực tiếp ngoài giờ đều bị chặn. Khi chọn trước 16h nhưng chốt sau 16h, nhân vật phải ở lại map gốc, không phát tàu vào 126.
- [ ] Kiểm tra lại lịch ngay trước thay đổi membership trong thao tác nhập khu; đồng bộ với trạng thái đóng phiên. Dùng lock riêng cho nhập khu, không khóa monitor Zone để tránh deadlock với item rơi khi rời khu cũ.
- [ ] Bảo vệ các đường `MapService.goToMap` và `ChangeMapService.goToMap`, kể cả khi đã truyền sẵn Zone; không chỉ dựa vào kiểm tra NPC.
- [ ] Chọn khu bằng duyệt tối đa 20 khu; đầy tất cả thì trả thất bại và thông báo, không vòng lặp tìm vô hạn. Giữ giới hạn 15/khu, kiểm tra số lượng tại lúc chốt vào.
- [ ] Khi login có map lưu 126, đặt map 19, x trong vùng gần Tapion (mốc x=1000), y=360 trước `getMapCanJoin`. Chính sách này ngăn người thoát phiên chiều tự vào lại phiên tối bằng vị trí cũ.
- [ ] Test đăng nhập lại trước/sau mốc đóng, qua phiên tối/ngày hôm sau; test luôn giữ mốc kết thúc chung khi vào muộn.

### 3. Boss 3 giai đoạn, không thời gian chờ

- [ ] Test mỗi khu có đúng một Hirudegarn và hoạt động độc lập; chết giai đoạn 1/2 phải chuyển giai đoạn tiếp, chết giai đoạn 3 phải trở về giai đoạn đầu đầy HP.
- [ ] Chuyển ở lượt xử lý nhanh tiếp theo, không còn điều kiện chờ 5/15/600 giây; giữ packet đổi hình/HP hiện có để client nhìn thấy đúng giai đoạn.
- [ ] Giữ damage, HP template, tỉ lệ và loại đồ rơi hiện tại. Mỗi lần chết giai đoạn chỉ phát thưởng một lần; đánh đồng thời hoặc tick lặp không nhân đôi đồ.
- [ ] Reset cờ xử lý chết, trạng thái combat và hiệu ứng khi bắt đầu đời boss/giai đoạn mới; không chỉ gán HP. Đối chiếu các cờ của `Mob`, gồm cờ đếm chết, trước khi chọn cách tái sử dụng đối tượng.
- [ ] Dùng vòng nhanh 100 ms riêng chỉ cho boss 126 và việc đóng phiên, đăng ký một lần từ `Manager.initMap()` sau khi mob/NPC đã khởi tạo. Bỏ Hirudegarn 126 khỏi đường `Zone.udMob()` chậm để tránh update hai lần; giữ update Player/item/mob khác theo nhịp Zone thực tế 1 giây. Không gọi toàn bộ `Map.run()` trong Manager vì sẽ thêm vòng cập nhật Zone trùng.
- [ ] Kiểm tra lại giờ dưới cùng cơ chế đồng bộ trước hồi boss/phát thưởng. Tại 16h/23h, dừng gây sát thương, thưởng và hồi boss; không để callback phiên cũ hồi boss hoặc kéo người của phiên mới.
- [ ] Test packet chuyển giai đoạn và boss mới trên client thật để phát hiện yêu cầu thời gian hoạt ảnh riêng của client.

### 4. Tàu cưỡng chế đúng lúc kết thúc

- [ ] Trước các thao tác boss của mỗi lượt nhanh, kiểm tra đóng phiên, khóa cổng rồi duyệt đủ 20 khu.
- [ ] Lấy snapshot người chơi an toàn trong từng khu; bắt lỗi theo người/khu để một lỗi không bỏ qua 19 khu còn lại.
- [ ] Với nhân vật thực còn ở khu gốc, gọi `DungeonExitService.exit(player, origin, 19, 1000, 360)`; xác nhận packet tàu `-65` và map sau chuyển là 19.
- [ ] Thất bại thì tiếp tục thử ở lượt nhanh tiếp theo trong thời gian đóng cửa. Không tạo một timeout gia hạn mới; không gọi lặp cho người đã ra map khác.
- [ ] Xóa đoạn cũ bị comment trong Player và các kiểm tra giờ riêng của Tapion.
- [ ] Test nhân vật sống/chết, admin, vừa chuyển khu, đã dùng Capsule ra ngoài, mất kết nối trước đóng cửa; không kéo người đã rời map về 19 lần nữa.
- [ ] Test restart trong giờ mở/đóng và lỗi chuyển map tạm thời rồi hồi phục.

### 5. SQL, hồi quy và xác nhận trên server

- [ ] Đọc lại schema/catalog trước viết migration; chỉ đổi cột số khu của map 126 sang 20, giữ max_player=15, mob/NPC/template khác nguyên cấu hình.
- [ ] Migration dùng transaction, chạy lặp hai lần không thay đổi kết quả lần hai. Xác nhận 20 khu bằng cấu hình source và SQL seed, không để seed cũ tạo lại 10 khu.
- [ ] Chạy test lịch/respawn/exit/entry/login/capacity cùng bộ hồi quy ngọc rồng sao đen, Mabư, DungeonExit và PetFusion hiện có.
- [ ] Giả lập Clock trong test; không đổi đồng hồ hệ điều hành hay giờ của server đang có người chơi.
- [ ] Fixture kiểm tra packet tàu và kết quả map; client thật kiểm tra hình boss/3 giai đoạn/tàu. Báo rõ mục nào đã test fixture, mục nào đã quan sát client.
- [ ] Đo thời gian xử lý vòng nhanh với 20 khu và lượng item rơi tích lũy; xác nhận không tăng nhịp cập nhật các map khác, không có xử lý chết hai lần hay lỗi đồng thời danh sách.

### 6. Build và cập nhật khi được yêu cầu triển khai

- [ ] Build candidate JAR riêng, kiểm tra class đã đổi và SHA-256; không ghi đè `SRC/20.jar` trong lúc build.
- [ ] Bảo trì có lưu dữ liệu, xác nhận mọi JVM chạy `-jar 20.jar` đã dừng và chặn script tự khởi động lại trong lúc thay JAR.
- [ ] Backup DB và riêng cấu hình map 126 trước migration; giữ khả năng trả số khu về cấu hình cũ.
- [ ] Chuyển JAR cũ vào `SRC/jar-backup/20-before-hirudegarn-<timestamp>.jar`; đối chiếu hash, lưu metadata và không ghi đè backup có sẵn.
- [ ] Áp dụng migration, cài candidate thành `SRC/20.jar`, khởi động ẩn; xác nhận cổng chạy, hash JAR, 20 khu và lịch trong runtime.
- [ ] Chỉ giữ JAR ứng dụng hiện hành ở thư mục chạy; bản cũ/staging vào jar-backup, thư viện/công cụ cần thiết không di chuyển.
- [ ] Nếu validation không đạt, dừng có lưu dữ liệu, trả JAR/config map đã backup và xác nhận lại runtime. Không phục hồi toàn bộ DB đè dữ liệu mới phát sinh chỉ để rollback cấu hình map.
- [ ] Ghi tài liệu kết quả và giới hạn kiểm chứng. Commit/push theo phạm vi được người dùng yêu cầu tại lúc triển khai, không gom file riêng tư hoặc sửa đổi không liên quan.

## Tiêu chí nghiệm thu

- [ ] Mở đúng hai phiên, mốc đầu bao gồm và mốc cuối loại trừ, mỗi phiên đúng một giờ theo UTC+7.
- [ ] Vào muộn, đăng nhập lại và restart không kéo dài giờ kết thúc.
- [ ] Có đủ 20 khu, boss từng khu chạy 3 giai đoạn rồi xuất hiện lại đầy HP mà không chờ 5/15 giây.
- [ ] Không có đường vào 126 ngoài giờ hoặc thưởng/hồi boss sau mốc đóng.
- [ ] Tất cả người chơi online còn trong 126 được gọi tàu về 19; lỗi tạm thời có thử lại, offline được chuẩn hóa vị trí lúc login.
- [ ] Giữ cấu hình 15 người/khu, không ảnh hưởng buff/khu sao đen hoặc lịch Mabư đã chạy.
- [ ] Backup, metadata, migration và kiểm chứng JAR đáp ứng AGENTS.md trước khi kết luận đã triển khai.
