# Kế hoạch điều khiển sự kiện trên web — 08/10/2026

**Trạng thái:** kế hoạch để xem trước. Chưa sửa web/source/SQL/JAR/client, chưa bật tắt hoặc xóa đồ thật.

**Mục tiêu:** bật/tắt các sự kiện đủ điều kiện ngay trong game từ control web; thu hồi nguyên liệu/vé/đồ dùng đúng sự kiện, giữ quà vĩnh viễn; dọn rương sau bảo trì có kiểm chứng và khả năng khôi phục.

**Công nghệ hiện có:** PHP control web, MySQL/MariaDB, Java server, Unity client Game1/Game2, hàng đợi LiveSync.

## 1. Quy tắc vận hành

| Thao tác | Kết quả mong muốn |
|---|---|
| Bật | NPC/menu/shop/drop/boss thuộc sự kiện hoạt động theo cấu hình; người đang đứng trên map cũng nhận cập nhật |
| Tắt | Khóa toàn bộ đường phát/dùng/đổi/mua/bán đồ tiêu hao sự kiện; gỡ boss và drop đang nằm đất, ẩn nội dung NPC/menu tương ứng |
| Đồ tiêu hao trong túi | Thu hồi online dưới khóa người chơi; offline được khóa qua policy và dọn trước khi vào game hoặc trong kỳ bảo trì |
| Đồ tiêu hao trong rương | Khóa ngay việc rút/dùng/chuyển; giữ bản lưu tới kỳ bảo trì kế tiếp, rồi xóa nếu vẫn thuộc phiên sự kiện đã tắt |
| Cải trang/pet/quà vĩnh viễn đã nhận | Giữ cả vật phẩm, đồ đang mặc, pet đã kích hoạt và chỉ số của quà; chỉ dừng nguồn phát mới của sự kiện |
| Server tắt/restart | Đọc SQL, tiếp tục đúng trạng thái/job dọn; không bật lại bằng các boolean mặc định hiện tại |

Phân loại dựa vào **vai trò của ID vật phẩm**, không dựa riêng vào tên, loại item hoặc việc có option thời hạn. Nguyên liệu không có hạn sử dụng vẫn có thể phải thu hồi. Đồ hạn dùng là phần thưởng được phân loại rõ vào đồ dùng sự kiện hoặc nhóm giữ, không suy đoán tự động.

Đề xuất chưa cho bật lại cùng sự kiện khi đang chờ/đang dọn rương. Sau dọn xong mới mở phiên mới; tránh đồ mùa mới bị job mùa cũ xóa. Không tự chạy bảo trì khi nhấn Tắt: chờ kỳ bảo trì tiếp theo, web ghi rõ số rương/nhân vật chờ xử lý. Nếu cần đổi quy tắc này, phải chốt trước triển khai.

## 2. Căn cứ từ source hiện tại

- Web đã có `server_command_queue`, request ID chống gửi trùng, receipt, kiểm quyền admin, CSRF và API kiểm kết quả. LiveSync poll khoảng 1,5 giây. Chưa có command sự kiện trong allowlist hiện tại.
- `EventManager` có các boolean mặc định true, nhiều init bị comment. Các nhánh NPC, shop, drop và UseItem chưa được điều khiển tập trung.
- Có 10 tên sự kiện trong hợp các nhóm đã kiểm kê; không phải 10 sự kiện hoàn chỉnh. 20/11, Valentine chưa có luồng tương ứng; TopUp là stub; 8/3 thiếu bảng SQL/luồng tác dụng. Chưa cho web bật những nhóm này chỉ vì có tên.
- Boss Hùng Vương và các manager boss mùa chưa có updater; top kem cập nhật nhầm bảng; đổi quà có rủi ro mất phí khi đầy túi; nồi bánh thiếu kiểm tra số lượng; trồng dưa sai chu kỳ/giới hạn. Các lỗi này phải sửa trước khi công bố sự kiện tương ứng sẵn sàng.
- Item hiện chưa có trường nguồn sự kiện/phiên. Đồ cũ dùng chung ID với nội dung thường không thể phân biệt nguồn chỉ từ ID hoặc createTime.
- Server có `items_bag`, `items_box`, `items_body`, `items_collection`, rương quay thưởng, đồ đã bán, body đệ tử, dữ liệu item event và hệ thống ký gửi/gift/nhận thưởng có cơ chế riêng. Chỉ dọn `items_box` sẽ chưa đủ.
- Production pool SQL có thể chỉ một connection. Không giữ connection rồi gọi helper xin connection khác; thứ tự khóa/borrow/commit phải được kiểm thử với cấu hình này.

## 3. Phương án chọn

**Chọn trạng thái SQL bền vững + queue LiveSync + server đối chiếu runtime.** Web lưu yêu cầu và lệnh trong cùng transaction. Server nhận lệnh, kiểm tra phiên/cấu hình, áp dụng, rồi cập nhật kết quả. Restart có thể tiếp tục đối chiếu mà không spawn trùng hoặc mất job dọn.

Hai cách không chọn: chỉ sửa cờ SQL không cập nhật được toàn bộ RAM/game; gọi trực tiếp vào server mà không có journal dễ mất yêu cầu khi mất kết nối hoặc crash.

Thời gian mục tiêu: yêu cầu được nhận trong khoảng 1–3 giây khi worker khỏe. Chỉ báo **Đã bật/Đã tắt** sau khi bước runtime cần thiết hoàn tất. Thu hồi nhiều người chơi xử lý theo batch và có tiến độ; không hứa mọi thao tác toàn server luôn hoàn tất trong vài giây.

## 4. Danh mục sự kiện và SQL

Mỗi sự kiện có mã ổn định, phiên chạy, cấu hình version và danh sách rõ ràng:

- Boss gốc, boss đồng hành/form, số lượng tối đa, map/khu, chu kỳ hồi.
- NPC hoặc menu sự kiện; vị trí, NPC dùng chung hay riêng.
- Item nguyên liệu/vé/đồ dùng cần thu hồi; item quà vĩnh viễn cần giữ.
- Shop, công thức, mob/boss drop, giftcode/phần thưởng chờ nhận liên quan.
- Điểm/top/buff/trạng thái cây hoặc vòng gọi rồng thuộc sự kiện.
- Yêu cầu res/path và mức hỗ trợ client.

Các bảng đề xuất, tên cuối cùng sẽ được đối chiếu schema trước migration:

| Bảng | Nội dung |
|---|---|
| `game_event_catalog` | Tên/mã, cấu hình và trạng thái đủ điều kiện bật |
| `game_event_state` | desired/applied state, revision, run ID, lỗi và thời điểm áp dụng |
| `game_event_item_rule` | Item ID, vai trò consume/keep, phạm vi thu hồi, quy tắc đồ dùng chung |
| `game_event_run` | Lịch sử từng phiên, thời điểm mở/đóng và revision policy đã chốt |
| `game_event_cleanup_job` | Run ID cần dọn, cutoff, tiến độ, lỗi và trạng thái bảo trì |
| `game_event_cleanup_receipt` | Kết quả theo nhân vật/kho, bảo đảm chạy lại không xóa hai lần |
| `game_event_cleanup_backup` | Bản item trước thu hồi và dữ liệu liên quan để phục hồi có chọn lọc |

Queue/receipt admin hiện có tiếp tục dùng; không tạo một hàng lệnh thứ hai nếu không cần. Trạng thái mong muốn và enqueue commit cùng nhau. Config phải dùng revision để hai admin không vô tình ghi đè nhau.

Đồ riêng sự kiện: danh sách ID xác định đủ cho đồ cũ. Đồ dùng chung: các lần cấp mới cần gắn event/run vào serialization và giữ tag qua chuyển kho/ký gửi/gift. Không gộp stack khác nguồn/phiên. Phải cập nhật mọi codec đọc/ghi đang dùng; tag không được làm thay đổi wire client nếu client không cần biết. Đồ cũ không rõ nguồn được báo riêng để phân loại, không xóa bằng suy đoán hoặc dải ID tùy ý.

## 5. Giao diện control web

Thêm mục **Sự kiện** vào control hiện có. Mỗi dòng thể hiện tên, trạng thái yêu cầu, trạng thái trong game, phiên, boss đang hoạt động, số NPC, job dọn và lỗi gần nhất.

Trạng thái dễ hiểu: **Tắt / Đang bật / Đang chạy / Đang tắt / Chờ bảo trì dọn rương / Đang dọn / Lỗi / Chưa đủ nội dung**. Không hiển thị “thành công” khi mới gửi lệnh vào queue. Có cảnh báo server không phản hồi dựa trên heartbeat thực tế, không suy ra server online chỉ vì SQL truy cập được.

Các thao tác: Bật, Tắt, Xem vật phẩm sẽ thu hồi/được giữ, Xem tiến độ/lịch sử, Thử lại job lỗi. Khi Tắt, hiển thị cụ thể boss/item và dự kiến số người bị ảnh hưởng, nhắc quà vĩnh viễn giữ nguyên. Preview có thời điểm/revision; số lượng chính xác được ghi ở lúc thực hiện vì inventory có thể thay đổi sau preview.

API POST kiểm admin/CSRF, allowlist và payload event/run/revision; không nhận SQL, đường file hoặc script tùy ý. Nút có request ID để bấm lại không tạo hai lượt. Web không sửa JSON túi/rương của người online trực tiếp.

## 6. Luồng bật không restart

1. Kiểm tra sự kiện đủ boss/map/NPC/recipe/item/res, revision đúng, không có job thu hồi chưa kết thúc.
2. Tạo phiên mới trong SQL và ghi yêu cầu; build cấu hình đầy đủ trước publish.
3. Server chuẩn bị danh sách instance boss/NPC/menu/shop. Theo dõi chính boss con/form thuộc run ID; không gọi `init()` lặp mù vì có thể nhân đôi NPC/boss.
4. Chỉ cho manager cập nhật/spawn sau khi khởi tạo đầy đủ và phiên được phép chạy. Publish trạng thái nhất quán, mở các gate cùng revision; lỗi chuẩn bị thì gỡ phần đã tạo và giữ sự kiện chưa chạy.
5. Gửi boss appearance, NPC visibility và thông báo cho người online. Người vào map sau đọc cùng snapshot. Menu/shop mở từ trước phải revalidate revision khi mua/đổi.
6. Đối chiếu số instance/map và các phần bắt buộc; ghi applied revision và kết quả lệnh. Replay/restart đối chiếu trạng thái mới nhất, không tạo thêm đợt boss cũ.

Client hiện có packet `-73` để ẩn/hiện NPC theo template ID. Packet chỉ đổi NPC đã có trong `vNpc`, **không tự tạo NPC mới**. Với vị trí cố định, chuẩn bị NPC sự kiện trong danh sách map từ đầu và ẩn khi tắt; gửi trạng thái lại sau mỗi lần tải map. Một map không được dùng cùng template ID cho hai NPC cần điều khiển riêng qua packet này. NPC dùng chung như ChiChi chỉ bật/tắt menu hoặc dòng shop sự kiện, không ẩn toàn NPC.

Nếu cần NPC/vị trí động mà client hiện tại không hỗ trợ, phải bổ sung packet/cập nhật client Game1 và Game2 một lần, có kiểm version. Không dùng kick, capsule hoặc restart để che thiếu cơ chế cập nhật. Res hiện hữu dùng đường tải server/client đã kiểm tra; sự kiện thiếu artwork/part bị khóa bật tới khi sửa.

## 7. Luồng tắt

1. Đóng gate ngay: không spawn/respawn, không drop, không mua/đổi/craft/gift/nhặt/dùng tiêu hao mới. Kiểm tra lại trạng thái ngay trước khi transaction nhận thưởng commit để chặn callback bắt đầu trước khi tắt.
2. Hủy hoặc hoàn tất các giao dịch đang giữ đồ theo quy trình hiện có; không bỏ receipt tiền, không trừ tiền đã bán rồi tự xóa lịch sử giao dịch. Chặn listing đang bán đồ cần thu hồi và luồng mua lại đồ đã bán.
3. Retire boss riêng theo event/run: bỏ khỏi zone/manager, gửi despawn, không gọi `die()` hoặc reward để gỡ. Gỡ drop nằm đất và hủy callback hồi sinh của phiên cũ. Buff sự kiện dừng; chỉ số/quà vĩnh viễn đã nhận giữ nguyên.
4. Ẩn NPC riêng, bỏ menu/shop sự kiện và vô hiệu request/menu cũ. Không gỡ boss/NPC thường chỉ vì dùng chung lớp hoặc template.
5. Thu hồi đồ tiêu hao trong túi/body online dưới khóa người chơi và transaction, backup trước sửa, lưu rồi mới phát packet. Nhân vật offline được dọn trước khi load vào game bằng gate login và transaction. Không quét mọi tài khoản trong một transaction dài.
6. Đồ trong các rương chuyển trạng thái chờ dọn; chặn rút/gửi/dùng/chuyển/ký gửi cho nhóm này. Đặt lịch job immutable theo run và policy revision; không dựa duy nhất vào boolean hiện tại.
7. Web xác nhận runtime đã dừng và báo riêng tiến độ thu hồi/chờ dọn rương. Một người bị lỗi lưu không được làm cả đợt được đánh dấu hoàn tất; identity đó giữ gate để tránh mất đồ/ghi đè.

Nếu runtime chưa gỡ hết boss do lỗi, trạng thái phải là **Lỗi/Đang tắt**, các nguồn phát vẫn đóng. Nếu có map chỉ dành sự kiện, chọn map ra ngoài và xử lý người trong map bằng luồng chuyển map hiện có; không áp thay đổi này cho phó bản thường đã được yêu cầu giữ nguyên.

## 8. Dọn rương trong bảo trì

Chạy sau khi đã đóng đăng nhập/giao dịch, drain queue và các receipt đang chờ, lưu/kick hết người chơi thành công, và xác nhận không còn writer RAM có thể ghi đè inventory.

1. Chốt các job đủ điều kiện trong kỳ bảo trì; khóa sửa trạng thái sự kiện đó cho tới dọn xong.
2. Snapshot SQL và backup từng inventory bị ảnh hưởng, gồm options/quantity/createTime/expire và tag nguồn. Chốt danh sách cần thu hồi theo version đã lưu, danh sách giữ phải có ưu tiên bảo vệ.
3. Dọn theo batch có checkpoint: rương thường, rương sưu tầm, rương quay thưởng và mọi kho đã kiểm kê. Thu hồi các bản tiêu hao còn ở túi/body/offline nếu chưa dọn trước đó.
4. Ký gửi/vật phẩm chờ nhận/đồ đã bán/gift động được xử lý theo trạng thái nghiệp vụ, không sửa chuỗi JSON bằng replace. Cancel listing tiêu hao theo transaction; không trả lại item đã bị thu hồi qua hòm nhận. Lịch sử giao dịch và receipt vẫn giữ để không replay khoản tiền/quà cũ.
5. Giữ số ô/rương/slot body, thay bằng ô trống đúng codec, không co mảng. Tính lại chỉ số, trạng thái vật phẩm được kích hoạt và bonus rương khi nạp nhân vật sau bảo trì.
6. Lỗi JSON hoặc save: không coi như rương rỗng, không ghi đè dữ liệu lỗi. Ghi lỗi/checkpoint; không mở game bình thường khi cleanup bắt buộc chưa hoàn tất. Có thể chạy lại từ batch chưa commit mà không thu hồi hai lần.
7. Quét hậu kiểm toàn bộ nơi lưu và đường phát: số item tiêu hao thuộc run đã đóng phải bằng 0; quà vĩnh viễn, item thường, tài nguyên tiền và số slot giữ đúng trước/sau. Job chỉ complete khi kiểm chứng đạt.
8. Startup trước listener kiểm job bảo trì chưa hoàn tất và snapshot trạng thái, chỉ mở đăng nhập sau điều kiện bắt buộc đạt. Không đổi luật thu hồi giữa chừng rồi tiếp tục cùng job.

Phải nối vào shutdown/startup thực tế: hiện `ServerManager.close()` dừng LiveSync, reconcile thưởng, lưu ký gửi rồi `Client.close()`. Cần chứng minh các lần save thành công trước cleanup; log “SUCCESSFULLY MAINTENANCE” một mình chưa đủ.

## 9. Bảo toàn dữ liệu và khôi phục

- Quà vĩnh viễn trong túi/rương/body/pet/collection được giữ dù sự kiện tắt. Trạng thái pet/cải trang không bị reset chỉ vì đến từ sự kiện.
- Item template/res không bị xóa khỏi SQL/file khi tắt: client và nhân vật vẫn cần đọc quà đã nhận, và lần mở sau cần tài nguyên này.
- Backup có actor/command/run/policy/checkpoint và hash để truy vết. Xóa chỉ sau backup commit thành công. Quy định thời gian giữ backup trước khi tự dọn archive.
- Khôi phục theo item/nhân vật/job, không đổ SQL snapshot cũ lên toàn bộ database đang vận hành. Thu hồi đã làm không được âm thầm đảo ngược chỉ vì bật lại sự kiện.
- Phân biệt crash trước/sau SQL commit, before/after publish; chạy lại lệnh dùng receipt và desired revision mới nhất. Toggle cũ không được bật lại sau khi lệnh Tắt mới đã commit.
- Policy đọc từ snapshot bất biến, không lấy mutex sự kiện bên trong `calPoint` hoặc khi player đang khóa. Lệnh tắt đóng admission/version dưới khóa sự kiện ngắn rồi thả; đợi giao dịch đang chạy và dọn từng player ngoài khóa sự kiện/connection đã borrow. Không giữ khóa sự kiện rồi chờ player trong khi đường dùng đồ giữ player rồi chờ sự kiện. Login/logout và mutation từng tài khoản dùng gate hiện có; thứ tự khóa cụ thể phải đối chiếu trade/gift/consign và được kiểm bằng ca đồng thời trước triển khai. Không giữ khóa toàn server suốt quét đồ.

## 10. Chia giai đoạn và file dự kiến

Các tên file mới dưới đây là đề xuất. Không sửa toàn bộ event cùng lúc để đưa nội dung chưa đủ vào trạng thái “đã hỗ trợ”.

| Giai đoạn | Công việc / file chính | Điều kiện kết thúc |
|---|---|---|
| 1. Catalog | Registry/policy trong `SRC/src/nro/models/event`; migration `SRC/sql/migrations`; kiểm kê ID, drop, shop, kho | Có danh sách xóa/giữ, nguồn dùng chung và res lỗi rõ ràng cho từng sự kiện |
| 2. Runtime | `EventRuntimeService`, `EventStateDAO`, handler queue; `EventManager`, `ServerManager`, manager boss mùa và NPC/map | Bật/tắt lặp không spawn trùng; giữ trạng thái qua restart |
| 3. Gate toàn luồng | Mob/UseItem/shop/craft/gift/trade/consign/inventory; NPC consumer riêng | Tắt không phát thêm/nhặt/dùng/chuyển tiêu hao qua đường cũ |
| 4. Thu hồi | `EventItemPolicy`, `EventCleanupService`, codec/DAO/login/maintenance | Online/offline/rương được xử lý đúng, backup/rollback/checkpoint qua thử lỗi |
| 5. Control web | `admin/events.php`, API mới, validator/helper/JS/sidebar và tests tại webgoc2026 | CSRF/quyền/request ID/version đúng; UI theo kết quả runtime |
| 6. Client và thử tổng | Game1/Game2 Controller/NPC nếu cần; resource preflight; test end-to-end | Người đứng trên map thấy thay đổi không reconnect/restart |
| 7. Cài lần đầu | Bảo trì lưu dữ liệu, SQL snapshot, backup JAR timestamp/SHA; update client nếu phát sinh packet mới | Khởi động, queue, client version và cleanup recovery được xác nhận |

Đề xuất thử **Hùng Vương trước**: có NPC/boss thật và luồng đổi quà, nhưng phải sửa các lỗi đã ghi. Sau đó Hè, Halloween, Noel, Tết, Trung thu theo độ sẵn sàng. Nhóm 20/11/Valentine/8/3/TopUp chưa đủ nội dung chỉ hiển thị “Chưa hỗ trợ”. Hirudegarn/ngọc rồng đen/Rồng Băng là hệ thống riêng; nếu đưa vào control thì cần adapter và luật vật phẩm riêng, không tự gộp với sự kiện mùa. Default/Broly thường, Whis tuần, dungeon và hỗ trợ nhiệm vụ không bị ảnh hưởng.

## 11. Điều kiện nghiệm thu

| Nhóm | Ca kiểm thử bắt buộc |
|---|---|
| Runtime | Bật/tắt khi có người đứng trên map; NPC hidden preload; menu cũ; boss con/form đang hồi sinh; drop chưa nhặt; server downtime/restart |
| Đồng thời | Hai admin thao tác trái nhau; request trùng; toggle revision cũ; mua/đổi/craft đúng lúc tắt; người logout/login giữa batch |
| Phạm vi đồ | Bag/box/body/collection/rương quay/body đệ tử/consign/pending reward/đồ đã bán; stack quantity lớn; cùng ID khác nguồn/phiên |
| Không mất quà | Cải trang/pet/quà vĩnh viễn đã nhận giữ đủ options/chỉ số/trạng thái; đồ thường cùng ID dùng chung không bị suy đoán xóa |
| Dữ liệu lỗi | JSON hỏng, SQL timeout, pool max=1, save fail, mất kết nối sau commit, crash giữa batch, map/res/part thiếu |
| Bảo trì | Không còn writer trước dọn; job chưa xong chặn login; chạy lại idempotent; snapshot/backup tồn tại; đối chiếu hậu kiểm bằng 0 item cần thu hồi |
| Client | Game1 và Game2, người mới vào map/người đang ở map, menu/shop đang mở, cache sạch/có cache; không cần restart để toggle |
| Web | Hết quyền/admin bị khóa/CSRF sai/payload sai; server không phản hồi; lỗi runtime không báo thành công; progress và retry đúng |

Ưu tiên test các lỗi có thể mất đồ/nhân bản đồ hoặc ghi đè SQL, sau đó thử hiển thị client. Database test riêng cho thu hồi và fault injection; không dùng tài khoản thật để thử xóa. Chỉ đưa từng sự kiện vào trạng thái đủ điều kiện bật sau khi bộ test liên quan và phiên client thử đạt.

## 12. Kết luận triển khai

Cần **một lần cập nhật server ban đầu** để có engine/gate/cleanup; client chỉ cập nhật nếu kiểm tra cho thấy packet hiện có không đủ. Sau đó thao tác bật/tắt thông thường không restart server. Dọn rương theo yêu cầu vẫn nằm trong kỳ bảo trì kế tiếp.

Kế hoạch giảm rủi ro bằng phạm vi xóa rõ, transaction, version/receipt, backup/checkpoint và các tiêu chí nghiệm thu; không hứa “không lỗi tuyệt đối” khi chưa triển khai và chạy thử. Chưa thực hiện bất kỳ thu hồi hoặc thay đổi sản phẩm nào ở bước lập kế hoạch này.
