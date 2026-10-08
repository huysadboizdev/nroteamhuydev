# Kiểm kê sự kiện SQL + SRC — 08/10/2026

Phạm vi: đọc SRC, SQL đang chạy `team2026` lúc **08:16:07 UTC+7**, đối chiếu snapshot JVM lúc **08:14:06 và 08:15:09 UTC+7**. Không sửa code, SQL, JAR hoặc khởi động lại server. Skills: `gitnexus-exploring`, `systematic-debugging`, `verification-before-completion`; phiên này không có MCP GitNexus nên truy dấu trực tiếp bằng `rg` và đọc source. Số lượng dưới đây có phạm vi cụ thể; không cộng tất cả thành số sự kiện đang chạy.

## Có bao nhiêu?

| Phạm vi | Số lượng | Ý nghĩa |
|---|---:|---|
| Mã chế độ `ConstEvent` | **8** | Halloween, 20/11, Noel, Tết, Hùng Vương, Trung thu, Hè, Valentine; loại mã 0 không sự kiện |
| Lớp `event_list` | **8** | 7 nhóm sự kiện + `Default` |
| Nhóm trong `EventManager` | **7** | Tết, 8/3, Halloween, Noel, Hùng Vương, Trung thu, TopUp |
| Hợp các tên chế độ và nhóm quản lý | **10** | 8 tên chế độ + 8/3 + TopUp; không gồm Default, không ngụ ý có đủ chức năng |
| Lớp có khởi tạo thực tế ở startup | **3** | Default, HungVuong, TopUp; TopUp rỗng |
| Sự kiện mùa có lệnh tạo boss ở startup | **1** | Hùng Vương; đã tạo đối tượng nhưng thiếu vòng cập nhật boss |
| Sự kiện mùa có boss hoạt động qua manager theo luồng SRC hiện tại | **0** | Không có luồng cập nhật seasonal manager |
| NPC sự kiện đang đặt trong SQL trong nhóm kiểm tra | **2 loại / 6 lần đặt** | Hùng Vương (52) trên 183/184/185; Xe nước mía (84) trên 0/7/14 |
| NPC nồi bánh (66) được đặt trong SQL | **0** | Có template và factory nhưng chưa đặt map |
| Bảng SQL `event` / dòng cấu hình runtime | **0 / 0** | Bảng `event` không tồn tại; `server_runtime_setting` rỗng |

Nguồn đếm: `SRC/src/nro/models/consts/ConstEvent.java:3`, `event/EventManager.java:14`, thư mục `event_list`, SQL lưu ở `.analysis/events-audit-20261008.sql` và kết quả `.analysis/events-audit-20261008-sql.txt`. Runtime: `.analysis/ranking-events-runtime-20261008/runtime.txt`; PID 11064 chạy `SRC/20.jar`, SHA-256 theo metadata triển khai `13d4a90762111edce5dc683c49655fcaaefa1c89bbf4bcfed98331c6d603a030`.

## Trạng thái từng nhóm

| Nhóm | Định nghĩa/triển khai | Trạng thái hiện tại |
|---|---|---|
| Halloween | `Halloween.boss`: Bí ma, Ma trơi, Dơi, mỗi loại 10; factory boss có route | `init` bị comment; manager riêng cũng chưa chạy |
| 20/11 | Chỉ tìm thấy mã `ConstEvent` trong phạm vi luồng sự kiện | Không có lớp event_list/init/menu tương ứng trong luồng này |
| Noel | `Christmas.boss`: 30 Ông già Noel | `init` bị comment; manager riêng chưa chạy |
| Tết | `LunarNewYear`: NPC 49 map 0 + 10 Lân con | `init` bị comment; manager riêng chưa chạy |
| Hùng Vương | `HungVuong.boss`: 10 Thủy Tinh; mỗi boss có Sơn Tinh đồng hành; NPC 52 có SQL placement | JVM có **20** đối tượng trong HungVuongEventManager; thiếu vòng cập nhật. NPC đổi quà vẫn nối qua SQL/factory |
| Trung thu | `TrungThu.boss`: 10 Khỉ đột + 10 Nguyệt thần; có Nhật thần companion | `init` bị comment; manager riêng chưa chạy |
| Hè | Không có event_list tương ứng; có Xe nước mía, đồ kem và điểm sự kiện | NPC 84 hoạt động theo map SQL, không chịu cờ hay lịch mùa; khúc mía rơi ở mob, nước đá mua ChiChi |
| Valentine | Chỉ mã `ConstEvent` trong luồng sự kiện kiểm tra | Không có lớp event_list/init tương ứng |
| 8/3 | `InternationalWomensDay.init` gọi EventDAO | `init` bị comment; thiếu bảng SQL event; bốn biến hiệu ứng mặc định 0 và không tìm thấy nơi dùng getter trong SRC |
| TopUp | Lớp TopUp chỉ override `npc()` rỗng | Được init nhưng không thực hiện chức năng event; thanh toán/nạp thẻ bên ngoài lớp này không được tính là bằng chứng TopUp hoạt động |
| Default | 30 Broly | Nhóm nền, không tính sự kiện mùa; runtime BrolyManager có 30 boss |

`Event.init()` gọi npc/boss/itemMap/itemBoss (`event/Event.java:12`). `itemMap`/`itemBoss` ở lớp nền rỗng (`:50`, `:55`) và event_list không override hai hook này: drop/reward hiện nằm rải rác Mob/UseItem/boss, không tự nối vào cờ EventManager. `EVENT_SEVER=0` (`server/ServerManager.java:63`, snapshot JVM) và ConstEvent không được dùng để điều khiển các nhánh trên; không thể nhìn số 0 rồi kết luận NPC nước mía/Hùng Vương đã tắt. Không có lịch ngày lễ trong EventManager.

## Hoạt động song song, không cộng vào sự kiện mùa

Đã truy dấu thêm **3 hệ thống** có từ event hoặc chức năng sự kiện thế giới; đây là phạm vi bổ sung, không phải tổng mọi dungeon/tournament/minigame trong game.

| Hệ thống | Lịch/đường vào | Trạng thái và SQL |
|---|---|---|
| Hirudegarn | Map 126, 20 khu; 15:00–16:00 và 22:00–23:00 Asia/Ho_Chi_Minh; Tapion → ChangeMapService | `services/HirudegarnEventService.java:17`, `:29`; map tick (`map/Map.java:195`), cửa vào/map lookup và normalize saved map đều nối service. Lúc snapshot 08:14 đang ngoài giờ. Không cần bảng event |
| Ngọc rồng đen / BlackBallWar | 20:00–21:00; nhặt từ sau 20:30; UTC+7 | `map/phoban/BlackBallWar.java:34`, `utils/TimeUtil.java:224`; dịch vụ map/bag/reward riêng, không thuộc EventManager |
| Rồng Băng / Shenron event | Dùng ngọc 925–931 → UseItem → NPC xác nhận → Shenron_Service | Menu/summon/reward đã nối, nhưng manager cập nhật sai hệ thống; xem lỗi E03. Không phải rồng Namek/Namek wish |

SQL template map 126 tên **Thành phố Santa**, NPC 53 Tapion; tên map có vẻ cũ nhưng chưa đủ để kết luận lỗi tài nguyên. Các hoạt động DHVT, võ đài sinh tử, siêu hạng, dungeon, minigame có manager riêng được liệt kê trong audit tổng của root, không gộp vào 10 nhóm mùa ở trên.

## Lỗi và bất nhất có bằng chứng

### E01 — Cao: boss Hùng Vương được tạo nhưng không có vòng cập nhật

`Boss.java:285–294` đưa boss mùa vào manager riêng; Hùng Vương vào `HungVuongEventManager`, không nằm list manager mặc định. Constructor `BossManager.java:131` chỉ tạo list. `ServerManager.java:125–134` chạy các manager boss khác, **không chạy bất kỳ manager boss mùa nào**. Tìm toàn SRC không có caller `HungVuongEventManager.updateBosses()/run()` thay thế. Runtime có 20 đối tượng xác nhận init đã chạy; snapshot mới `.analysis/ranking-events-runtime-20261008/runtime-v2.txt` xác nhận **REST=20, boss có zone=0**, không có thread manager mùa. 10 Thủy Tinh + 10 Sơn Tinh giải thích số 20 (`BossesData.java:2309`, `Boss.java:301`).

### E02 — Cao: bảng xếp hạng kem `Topsukien2` không được nạp/cập nhật

`Manager.java:113` khởi tạo list rỗng; startup `:951–954` chỉ nạp Topsukien/Topsukien1/Whis. `ServerManager.java:185–186` khi cờ **isTopSukien2Changed** bật lại truy vấn/gán **Topsukien1/queryTopsukien1**. Trong khi `UseItem.java:1544` tăng point_sukien2 và cờ đúng, ChiChi hiển thị top2 (`npc_list/ChiChi.java:125`). Runtime `Topsukien2.count=0`. SQL có đủ cột point_sukien2. Sửa hướng đúng là nạp và cập nhật đúng list/query2.

### E03 — Cao: Rồng Băng không có updater của manager thật

`UseItem.java:1878` → `Shenron_Service.java:62–65` tạo `nro.models.services.shenron.Shenron_Event` và thêm vào **Shenron_Manager**. Trong khi startup `ServerManager.java:119` chạy **ShenronEventManager** dùng lớp khác `models.ShenronEvent` (`managers/ShenronEventManager.java:3`). Tìm toàn SRC không thấy luồng/caller chạy Shenron_Manager.update/run. Hết thời gian/chuyển reconnect trong Shenron_Event.update vì vậy không được xử lý định kỳ. Manager cũ tự executor.submit(this) (`:29`) rồi startup lại chạy Thread thứ hai: vòng lặp trùng trên hệ thống cũ. Thông báo cho 5 phút nhưng constant thực tế 60 giây (`Shenron_Event.java:50–53`); lỗi lịch chỉ cần sửa sau khi nối đúng updater.

### E04 — Cao, nhánh chưa đặt NPC: nấu bánh cho phép thiếu số lượng nguyên liệu

`event/NoiBanh.java:48` và `:66` chỉ kiểm tra item khác null, không kiểm tra quantity >= 99/10/1; vẫn cấp bánh sau khi trừ. `InventoryService.java:254–260` trừ xuống <=0 thì xóa item, không từ chối thiếu. Có thể giữ một cái mỗi nguyên liệu và nhận sản phẩm giá 99 nguyên liệu khi NPC được đặt/route tới được. NPC66 hiện không có map SQL, nên không báo như exploit đã xảy ra trên server.

### E05 — Cao: đổi quà/nước mía có thể trừ tài sản mà không cấp phần thưởng

`XeNuocMia.java:96–111`, `:126–150` trừ nguyên liệu + vàng trước, bỏ qua boolean của addItemBag; bản x10 có thể cấp một phần và mất phần còn lại. `VuaHung.java:225–233`, `:251–259`, `:271–277`, `:289–295` tương tự. `InventoryService.java:848`, `:934` trả boolean, có nhánh false vì đầy túi/gate/pending transaction. Không hoàn phí/quà, vẫn báo nhận thành công/tăng điểm. Điều kiện tái hiện đầy túi: nguyên liệu còn stack sau khi trừ nên không giải phóng ô; reward mới không có stack sẵn. `npc/DuaHauEgg.java:69–75` cũng bỏ qua addItemBag rồi reset chu kỳ thu hoạch.

### E06 — Trung bình: giới hạn trồng dưa dùng list toàn server không reset và sai kiểu ID

`VuaHung.java:32` List<Integer>, `:178` add((int)pl.id), nhưng `:54` contains(player.id) với player.id long (`Player.java:90`): boxing Long không bằng Integer. Không tìm thấy clear/remove của nguoiDaTrong. Sau 10 lần trồng, `:51` chặn toàn server cho tới restart, dù menu tính phiên từng giờ (`:47`); ID lớn có thể bị cắt khi cast int. Giới hạn phiên cần lưu/reset theo phiên và cùng kiểu ID.

### E07 — Trung bình: menu nói dưa chín theo 6 giờ nhưng cây thực tế 24 giờ

`VuaHung.java:34`, `:54–59` so với `DuaHauEgg.timeStart`; trường timeStart không được gán trong constructor/create/load nên 0. Menu kết luận cây sẵn sàng ngay. `DuaHauEgg.java:13`, `:23–28`, `:60`, `:68` thực tế dùng lastTimeCreate + timeDone=24h để thu hoạch. Không đồng bộ thông tin và trạng thái thật.

### E08 — Trung bình, nhánh tắt: SQL 8/3 thiếu và lỗi bị nuốt

`EventDAO.java:31` SELECT bảng event không tồn tại; catch `:42` rỗng, nên bật lại 8/3 không thấy lỗi rõ mà buff vẫn 0. `save()` UPDATE cùng bảng (`:55`). Hiện init 8/3 bị comment nên chưa có bằng chứng lỗi SQL này phát sinh ở startup hiện tại.

### E09 — Trung bình: cờ bật nhưng init bị comment, TopUp stub, thiếu nguồn nguyên liệu Hùng Vương

Bảy flag true (`EventManager.java:14–26` và runtime) nhưng năm init comment (`:40–59`). TopUp init có chạy nhưng thân rỗng. NPC66 không đặt map. SQL có đủ template nguyên liệu 1220–1222/1544–1549 và quà1776/1777, nhưng không có shop bán các nguyên liệu này trong truy vấn; tìm các ID chính xác trong SRC không thấy drop/cấp ở mob/boss ngoài constants và consumer VuaHung/NoiBanh. Boss Sơn/Thủy Tinh thưởng item422 và điểm, không phải nguyên liệu sính lễ. Không thể kết luận người chơi không có nguyên liệu từ giftcode/admin; đó là nguồn dữ liệu động chưa kiểm kê tại báo cáo này.

### E10 — Trung bình: updater top xóa cờ sau lỗi

`ServerManager.java:193` bắt exception bên trong updateTop; scheduler `:153–154` vẫn resetTopFlags. SQL thất bại có thể làm mất yêu cầu refresh. Cờ đồng thời có thể bị reset trong khi dữ liệu đang tăng; chưa tái hiện race nên không báo mức chắc chắn như E02. Vấn đề thứ tự save/player persistence cần kiểm tra transaction trong audit tổng.

## Dữ liệu item, shop và giới hạn kết luận

Các **26 item template** truy vấn đều có trong SQL (7 ngọc băng, nguyên liệu/quà Hùng Vương, đồ mía); các ID phần thưởng bánh như381–385,1150–1154,1423,1438,1634,1635 nằm trong audit tài nguyên tổng. Shop33/ChiChi/tab58 có 1609 Kem trái cây, 1613 Nước đá, 1631 Pet; is_sell=1. Mob.java:717–742 rơi nguyên liệu1798–1802 và1612 trong AllMap; không kiểm tra cờ mùa. Việc hoạt động Hè trong tháng10 là hành vi hiện tại có bằng chứng, chưa tự kết luận yêu cầu kinh doanh phải tắt.

Các đường grant/craft được kiểm tra qua consumer và addItemBag. `item/DisabledItemRules.java:6` chỉ chặn item1015; các ID sự kiện nêu trên không bị rule này chặn. `ItemService.java:90` áp rule khi tạo item, `UseItem.java:278` áp khi dùng. Sự tồn tại template không chứng minh ảnh/icon/part/skill client đầy đủ; xem audit tài nguyên tổng của root. Không thực hiện giao dịch tiêu hao trong server để thử lỗi, do yêu cầu chỉ kiểm kê. `.analysis/events-audit-20261008-verification.json` lưu lần xác minh số lượng và SHA-256 các source chính/JAR.

Ưu tiên xử lý sau khi người dùng yêu cầu sửa: E01/E02/E03, bảo toàn phí/quà E05, kiểm tra số lượng E04 trước khi đặt NPC66, rồi đồng bộ cấu hình/lịch/SQL và trồng dưa.
