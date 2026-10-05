# Kế hoạch cưỡng chế thoát phó bản và sửa Jaco–Commeson

> Người dùng đã đồng ý triển khai ngày 05/10/2026. Đã hoàn tất SRC, kiểm thử và cập nhật SQL/JAR sau bảo trì lưu dữ liệu. Kết quả thực tế ghi tại `docs/dungeon-commeson-20261005.md`.

**Mục tiêu:** phó bản hết hạn phải đưa người chơi ra bằng tàu; trận nhân bản Commeson mỗi nhân vật tối đa một lượt/ngày, xử lý đầy đủ thắng, thua, nhặt bình và tự thoát bằng Capsule.

**Thiết kế:** thống nhất đường cưỡng chế thoát nhưng giữ thời lượng và nơi trả của từng phó bản. Trận Commeson có phiên riêng gắn chủ trận, boss, zone và các deadline; SQL giữ lượt ngày và trạng thái để reconnect không tạo lượt mới. Dùng lại hiệu ứng tàu hiện có của client.

**Công nghệ:** Java server, MySQL, giao thức tàu/capsule hiện có. Ngày tính theo giờ Việt Nam, UTC+7, không phải sau 24 giờ kể từ lượt trước.

## 1. Kết quả đọc SRC và SQL

| Phó bản | Thời hạn hiện có | Đường đưa về hiện có |
|---|---|---|
| Bản đồ kho báu | 30 phút; có chờ 60 giây khi hoàn thành hoặc gần hết hạn | Tàu về đảo Kame, map 5 |
| Con đường rắn độc | 30 phút; có chờ 60 giây khi kết thúc | Tàu về map 5 |
| Khí gas hủy diệt | 30 phút; có chờ 60 giây khi boss cuối chết | Tàu về làng Aru, map 0 |
| Doanh trại | 30 phút chiến đấu; khi vào pha nhặt thưởng dùng timer 5 phút | Tàu về nhà theo hành tinh |
| Mabư 12h | Đóng lúc 13h; hiện đếm 30 giây về | Tàu về nhà; đang có ngoại lệ admin |
| Mabư 14h | Đóng lúc 15h | Tàu về nhà; đang có ngoại lệ admin |
| Siêu thần thủy | Hiệu ứng thử thách 15 phút | Có đổi map về nhà khi hiệu ứng hết; chưa dùng đường tàu như các phó bản trên |
| Ngọc rồng sao đen | Sự kiện 20h–21h | Có kick bằng tàu khi đóng sự kiện |
| Jaco / nhân bản | Hiệu ứng đánh nhân bản 5 phút | Chưa có tàu sau nhặt bình; thua chưa được xử lý hồi sinh trong PKCommeson |

Đây là kết quả đọc code, chưa phải kết quả test gameplay khi timer hết. Bản triển khai sẽ kiểm thử cưỡng chế với nhân vật sống, chết, offline và đã đổi map.

Điểm cần sửa hoặc kiểm chứng:

- `BanDoKhoBau`, `SnakeWay`, `DestronGas`, `RedRibbonHQ` gọi finish/dispose rồi vẫn chạy phần update tiếp theo. Cần kết thúc tick ngay sau khi đóng, tránh truy cập clan/boss đã bị dọn.
- Nhiều hàm finish duyệt danh sách người chơi đang thay đổi, hoặc không kiểm tra player.zone trước khi đọc map. Một lỗi trong vòng này có thể chặn việc đón những người còn lại.
- Một số dispose chỉ dọn boss còn sống, có thể bỏ boss chết đang chờ pha cuối; cần dọn toàn bộ boss thuộc phiên, không đụng boss phiên khác.
- `ChangeMapService.changeMap` có điều kiện chặn chuyển nhanh khi đang mang ngọc rồng Namếc. Cưỡng chế thoát cần xử lý vật phẩm mang theo trước, không chỉ gọi hàm rồi mặc định là đã chuyển thành công.
- Potage mới kiểm tra ngày ở menu; `Service.callNhanBan` chưa kiểm tra lại điều kiện/ngày khi tạo boss. Admin hiện tự bỏ qua điều kiện ngày ở menu.
- `BossManager.findBossClone` tìm nhân bản bất kỳ trong khu, không theo chủ trận. Map 140 là map chung, SQL có 10 khu, mỗi khu tối đa 15 người.
- Nhân bản có thể lấy zone mới của người chơi khi join; cần chặn boss xuất hiện ở map khác nếu chủ trận rời map giữa lúc tạo và join.
- `PKCommeson.update` và `finish` đang rỗng. `sendResult` chỉ xử lý RUNS_AWAY, không xử lý DEAD để hồi sinh.
- Bình thưởng là item **638 – Bình chứa Commeson**. Chưa có liên kết giữa sự kiện nhặt đúng bình của trận và timer tàu.
- SQL đã có `player.lasttimepkcommeson`, nhưng chỉ ghi timestamp hiện tại trong bộ nhớ lúc tạo boss rồi lưu qua luồng save chung; cần bảo đảm lượt ngày được lưu trước khi bắt đầu đánh.
- Capsule 193 đang fall-through sang case 194, gọi mở UI hai lần; sửa break và kiểm tra không tiêu thụ vật phẩm hai lần trong luồng thoát.

## 2. Quy tắc Commeson đã chốt

1. Jaco đưa tới Potaufeu/map 139; Potage ở map 140 bắt đầu trận. Chỉ đi thăm map không tiêu lượt.
2. Mỗi **nhân vật** một lượt đánh trong một ngày UTC+7. Lượt tính khi trận tạo thành công và bắt đầu; thắng, thua, hết giờ hoặc tự bỏ trận đều đã dùng lượt đó.
3. Bấm nhanh, gọi menu cũ hoặc gọi trực tiếp hàm server không tạo thêm trận. Kiểm tra ở cả NPC và service; giới hạn phải tồn tại qua reconnect và restart.
4. Lỗi tạo trận trước khi bắt đầu không làm mất lượt. Không hoàn lượt khi người chơi đã thực sự vào đánh.
5. Giữ thời gian đánh tối đa **5 phút** hiện tại. Dùng deadline server; hiệu ứng PK chỉ phản ánh thời gian, không phải nguồn quyết định kết quả.
6. Thua hoặc hết 5 phút: kết thúc trận, dọn boss/PK/hiệu ứng, hồi sinh đủ HP/KI nếu đã chết, tàu đưa về trạm tàu hành tinh của người chơi. Không rơi bình.
7. Thắng: rơi đúng **một bình 638**, chỉ chủ trận được nhặt, gắn bình với mã phiên. Giữ các option thưởng hiện tại.
8. Theo trả lời của người dùng: **chờ nhặt tối đa 60 giây từ lúc thắng**. Nếu chưa nhặt vì túi đầy, báo cần chừa ô trống; không tự cộng bình vào túi. Hết 60 giây chưa nhặt thì dọn bình và tàu đưa về.
9. Nếu nhặt thành công trước hạn: chuyển sang pha chờ tàu **60 giây từ lúc nhặt**. Không dùng thời gian bình xuất hiện hoặc thời gian bắt đầu trận làm mốc này.
10. Ví dụ thắng lúc 10:00, nhặt lúc 10:40: tàu về lúc 11:40. Không nhặt: tàu về lúc 11:00.
11. Sau thắng được tự dùng Capsule ra trước. Khi đã ra map khác, hủy timer và dọn phiên; timer cũ không được kéo người chơi khỏi map mới.
12. Nếu dùng Capsule bỏ trận khi đang đánh: coi là bỏ cuộc, không thưởng và không hoàn lượt.
13. Reconnect không có lượt mới. Phiên đã hết hạn phải đưa về điểm an toàn khi đăng nhập. Phiên đã nhặt bình chỉ còn thời gian chờ tàu; không sinh lại boss hay thêm bình.
14. Admin qua luồng NPC áp dụng giới hạn ngày như người chơi. Nếu cần test nhiều lượt, làm bằng lệnh quản trị riêng có ghi log, không tự miễn giới hạn ở menu thường.

## 3. Cách triển khai được đề xuất

**Chọn service quản lý trận và đường thoát chung.** Giới hạn ngày lưu SQL, trạng thái được quyết định dưới khóa phiên/nhân vật, timer được kiểm tra bằng deadline. Cách này xử lý được lúc thắng, nhặt bình, đổi map và reconnect cùng xảy ra.

Hai cách khác không chọn: chỉ sửa NPC/hiệu ứng thì khó bảo vệ lượt ngày và reconnect; tạo bộ phó bản mới hoàn toàn thì thay quá nhiều map/luồng không cần thiết.

Không dùng thread ngủ 60 giây cho từng người chơi. Deadline được xử lý bằng tick hiện có; timeout vẫn hoạt động khi người chơi chết. Chỉ phát một thông báo khi đổi pha và các mốc đếm cuối, không spam theo tick.

### Thành phần và file dự kiến

| Thành phần | File |
|---|---|
| Cưỡng chế thoát, kiểm tra origin zone, tàu và hồi sinh | Tạo `SRC/src/nro/models/services_dungeon/DungeonExitService.java` |
| Luật/trạng thái/deadline Commeson | Tạo `SRC/src/nro/models/services_dungeon/CommesonChallengeService.java` |
| Lượt ngày và phục hồi phiên | Tạo `SRC/src/nro/models/database/CommesonAttemptDAO.java` và migration SQL |
| Bắt đầu trận, thông báo lượt ngày | `npc_list/Jaco.java`, `npc_list/Potage.java`, `services/Service.java` |
| Nhân bản theo chủ trận, thưởng một lần, cleanup | `boss/nhan_ban/NhanBan.java`, lookup liên quan trong `Boss_Manager/BossManager.java` |
| Kết quả thắng/thua, cleanup PVP | `matches/PKCommeson.java` |
| Nhận bình thành công | `map/Zone.java`, metadata cần thiết trong `map/ItemMap.java` |
| Timer, rời map, capsule và đăng nhập lại | `player/Player.java`, `map/service/ChangeMapService.java`, `services_func/UseItem.java`, `database/MrBlue.java` |
| Các phó bản có timer | `map/phoban/BanDoKhoBau.java`, `SnakeWay.java`, `DestronGas.java`, `RedRibbonHQ.java`, `MajinBuu14H.java`, `BlackBallWar.java`; `services_dungeon/MajinBuuService.java`, `SuperDivineWaterService.java` |

### Lưu SQL

Đề xuất bảng `commeson_attempt`: mã phiên, player_id, game_day, trạng thái, thời gian bắt đầu, hạn đánh, thời gian thắng, ID bình, thời gian nhặt, hạn tàu, map trả và thời gian kết thúc. Khóa duy nhất `(player_id, game_day)` bảo vệ một lượt/ngày ở tầng SQL.

Giữ `lasttimepkcommeson` để tương thích code cũ; bảng phiên là nguồn chính cho lượt mới. Nếu dữ liệu cũ cho biết hôm nay đã đánh thì không cấp thêm lượt khi chuyển sang bảng mới. Phiên lỗi trước bắt đầu được đánh dấu để retry có kiểm soát, không xóa lịch sử lượt đang đánh.

Các chuyển trạng thái quan trọng phải kiểm tra mã phiên và trạng thái trước đó: STARTING → FIGHTING → WON_WAIT_PICKUP → WON_EXIT_PENDING → CLOSED, hoặc FIGHTING → LOST/TIMED_OUT/LEFT. Một phiên không được vừa thắng vừa thua hay phát thưởng hai lần.

### Đường thoát phó bản

- Giữ thời lượng và điểm trả hiện có trong bảng trên; chỉ chuẩn hóa độ tin cậy và hiệu ứng tàu.
- Chụp danh sách người chơi theo từng instance trước khi đưa về; kiểm tra người đó vẫn ở instance cần đóng.
- Đánh dấu instance đóng để từ chối người mới vào, nhưng giữ danh sách người còn phải đưa ra nếu lần chuyển đầu thất bại.
- Xử lý từng người độc lập: một người lỗi không làm người khác bị kẹt. Không coi gọi hàm changeMap là bằng chứng đã rời instance; kiểm tra zone sau chuyển và retry nếu cần.
- Dọn PK, khóa/bế trong Mabư và vật phẩm sự kiện phải rơi trước khi chuyển; bảo đảm nhân vật chết được hồi sinh theo đường đưa về.
- Chỉ xóa liên kết clan/instance và boss/item sau khi xử lý đúng các người thuộc phiên. Khi đóng tick phải return ngay.
- Login vào map phó bản không còn phiên hợp lệ thì đưa về điểm an toàn, không tạo instance mới.

## 4. Các bước triển khai

- [ ] **Bước 1 – Kiểm thử cưỡng chế trước sửa.** Tạo `DungeonExitLifecycleTest` và `DungeonTimeoutTest`; dùng clock giả để đi tới đúng mốc timeout, không chờ 30 phút thật. Tái hiện null zone, người chết, đổi map và lỗi một người trong danh sách.
- [ ] **Bước 2 – Sửa đường thoát và các instance.** Thêm DungeonExitService; nối vào các finish/kick hiện có, return sau đóng, cleanup đầy đủ và retry người chưa rời. Chạy lại hai nhóm kiểm thử.
- [ ] **Bước 3 – Lượt ngày và dữ liệu phiên.** Tạo migration và DAO; test cạnh tranh hai lần bắt đầu, ngày mới UTC+7, dữ liệu lasttime cũ, lỗi SQL chưa xác định commit, restart và reconnect.
- [ ] **Bước 4 – Trận nhân bản.** Thêm CommesonChallengeService; NPC/service cùng dùng kiểm tra bắt đầu. Boss gắn chủ trận và zone đã chọn, không gây sát thương cho người ngoài. `PKCommeson` xử lý thắng/thua/hết hạn/bỏ cuộc, không tạo thêm trận trong active tick.
- [ ] **Bước 5 – Bình và timer tàu.** Chỉ xác nhận đã nhặt sau addItemBag thành công với đúng itemMapId và mã phiên. Test túi đầy, bình cũ/khác phiên, nhặt sát giây 60, hai packet nhặt, hai callback kết liễu và hết hạn cùng lúc nhặt.
- [ ] **Bước 6 – Capsule và reconnect.** Sửa fall-through 193; sau khi thực sự rời map thì hủy timer. Khôi phục pha/hạn từ SQL khi đăng nhập, không respawn boss hoặc phát lại bình đã nhận.
- [ ] **Bước 7 – Rà soát và triển khai.** Chạy bộ hồi quy hiện có 53 nhóm, thêm các nhóm phó bản/Commeson. Bảo trì lưu dữ liệu, dừng server, snapshot SQL, chuyển JAR cũ vào jar-backup với timestamp và SHA-256, áp dụng migration, cài JAR mới, khởi động và kiểm tra runtime.

## 5. Tiêu chí nghiệm thu

1. Mọi người còn trong instance được đưa ra khi hết hạn; người chết không kẹt, người đã Capsule đi không bị kéo về lần nữa.
2. Đúng thời hạn phó bản hiện có; không vô tình rút ngắn 5 phút nhặt thưởng của Doanh trại.
3. Nhân vật chỉ có một trận bắt đầu thành công/ngày, kể cả reconnect, restart và bấm NPC liên tục.
4. Thua/hết giờ: hồi sinh nếu cần, không bình, boss biến mất, về trạm tàu; không đánh lại trong ngày.
5. Thắng: chỉ một bình của chủ trận; túi đầy không được tính đã nhặt.
6. Không nhặt: về sau 60 giây từ thắng. Nhặt: về sau 60 giây từ nhặt. Capsule trước hạn: hủy đón.
7. Bình nhặt gần hết hạn và callback sát timeout vẫn chỉ có một kết quả, không nhân đôi bình hoặc đổi map hai lần.
8. Người ngoài không cướp bình hay nhận thưởng từ trận của người khác; boss không xuất hiện theo chủ trận sang map mới.
9. Test trên client: quan sát tàu thật, nhân vật rời map và không còn boss/item/PK của phiên cũ. Kiểm tra map lúc trước/sau deadline, không chỉ thông báo màn hình.

## 6. Quyết định đã có

Người dùng đã chọn chờ nhặt tối đa **1 phút** và đồng ý triển khai. Kế hoạch giữ 5 phút đánh hiện tại và lấy mốc tàu sau nhặt đúng 1 phút. Giới hạn áp dụng theo nhân vật; điểm về cho Commeson là trạm tàu hành tinh, thua được hồi sinh đầy HP/KI nếu đã chết.

## Kết quả triển khai

Đã chạy 61 nhóm kiểm thử; riêng phó bản gồm 22 tình huống. Server PID 14960 nghe cổng 14445, schema lượt ngày và service/bản đồ trong JVM đã xác minh. JAR cũ và snapshot SQL đã lưu theo quy tắc workspace. Chưa kiểm thử trực tiếp hoạt ảnh tàu và trận đánh bằng client Unity sau cập nhật; xem báo cáo kết quả tại `docs/dungeon-commeson-20261005.md`.
