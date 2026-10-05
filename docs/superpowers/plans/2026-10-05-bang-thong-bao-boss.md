# Bảng thông báo boss trong client

Trạng thái: đã triển khai ngày 05/10/2026. Người dùng đã yêu cầu triển khai; dùng phạm vi mặc định boss bản đồ chung, loại boss phó bản/đấu trường/luyện tập riêng. Giữ thông báo hệ thống, thêm Boss hiện tại/Lịch sử boss.

## Yêu cầu hiển thị

Người chơi vào game có thể mở **Chức năng → Thông báo** để biết boss nào đang có, xuất hiện khi nào/ở đâu, boss nào đã bị ai tiêu diệt lúc nào, cách đây bao nhiêu phút và khoảng bao lâu hồi sinh. Địa điểm chỉ là **tên bản đồ**; không hiện khu, số khu hoặc tọa độ. Không gửi zoneId trong dữ liệu dành cho client.

Giữ các thông báo hệ thống đang có; thêm mục/tab **Boss** trong cùng trang. Trong Boss có hai chế độ:

- **Hiện tại:** các boss được theo dõi, trạng thái đang xuất hiện/chờ hồi sinh/chờ điều kiện; thời điểm xuất hiện gần nhất; bản đồ đang xuất hiện hoặc bản đồ lần gần nhất nếu đã bị hạ; người hạ/lúc hạ gần nhất; hồi sinh dự kiến.
- **Lịch sử:** các sự kiện xuất hiện/bị hạ/biến mất theo thứ tự mới nhất trước. Mỗi lần xuất hiện là một lượt riêng; bản ghi bị hạ gắn người hạ, thời điểm, địa điểm đúng lượt đó.

Một dòng danh sách ngắn: **Tên boss • Đang xuất hiện / Đã bị hạ**; chọn dòng để xem chi tiết. Chi tiết gồm tên, trạng thái, bản đồ, “Xuất hiện lúc HH:mm:ss (X phút trước)”, “Bị hạ lúc HH:mm:ss (X phút trước)”, “Người hạ: tên nhân vật”, “Hồi sinh dự kiến: khoảng X phút nữa / lúc HH:mm:ss”. Sự kiện ngày khác kèm ngày để không nhầm. Cần phân biệt bản đồ hiện tại với bản đồ lần hạ gần nhất; chưa biết bản đồ lần tới thì không dùng tên bản đồ cũ như địa điểm lần hồi sinh tới.

Giờ dùng Asia/Bangkok (UTC+7), timestamps server là nguồn chuẩn. Tuổi sự kiện và đếm ngược dùng thời gian server cộng elapsed trên client, không phụ thuộc đồng hồ PC. Mở bảng boss/đổi tab lấy dữ liệu mới; không tự tải lại danh sách định kỳ hoặc phát request khi đang đọc chi tiết/quay lại từ chi tiết. Nút Làm mới và phân trang chỉ tải khi người chơi thao tác, có giới hạn tần suất.

## Điều đã khảo sát

- Client Game1/Game2 Panel dùng mResources.gameInfo, vGameInfo/GameInfo cho Thông báo. Packet50 hiện có: số lượng signed byte, từng dòng shortID + tiêu đề + nội dungUTF; Controller xóa danh sách rồi nạp mới và lưu hasRead theo ID trong RMS.
- ServerNotify.sendNotifyTab được gọi khi đăng nhập; nội dung từ Manager.NOTIFY, hiện ghi count cố định10. Không có lịch sử boss bền vững trong luồng này. Không nối hàng trăm boss vào packet50 hiện tại vì count signed byte, dữ liệu động và ID/read marker sẽ không phù hợp.
- Thông báo chạy trên màn hình dùng packet93 chứa chuỗi, ServerNotify chỉ giữ hàng đợi tạm. Không phân tích chuỗi này để dựng lịch sử: thiếu identity/time/lifecycle và người vào sau không nhận lại.
- Boss.joinMap/notifyJoinMap phát thông báo xuất hiện bằng tên bản đồ. Boss.die nhận plKill, nhưng nhiều subclass override die/joinMap/leaveMap (toàn cây boss có nhiều điểm override). Phải audit luồng chung và override để không bỏ sót hoặc ghi hai lần.
- Boss cơ bản chờ từ lastTimeRest + secondsRest*1000; lastTimeRest được đặt khi leaveMap sau hội thoại cuối, không phải luôn ngay thời điểm bị hạ. Boss nhiều dạng respawn sang level kế tiếp ngay. Boss theo giờ/điều kiện hoặc custom rest không có một công thức hồi sinh chung.

## Quy tắc dữ liệu và thời gian

1. Dùng instance key/lượt spawn riêng, không chỉ tên hoặc bossID: nhiều boss cùng loại có thể cùng xuất hiện. Một lượt có UUID, boss template/family/form, tên lúc xảy ra, mapID/mapName (không zone), appearedAt, defeatedAt, killerID/killerName, status, nextEligibleAt/estimateKind và updatedAt.
2. Người hạ theo attribution server đang dùng cho lượt chết; nếu đệ tử hạ thì ánh xạ về sư phụ theo chính sách reward hiện tại. Tên killer lưu snapshot để lịch sử không đổi khi nhân vật đổi tên. Không có killer hợp lệ thì ghi “Không xác định”, không gán giả cho player gần nhất. Không đổi reward, drop hoặc cách phân chia boss khi thêm tracker.
3. Chỉ ghi xuất hiện sau khi join map thành công; chỉ ghi chết một lần khi có chết thực sự; biến mất hết giờ/admin/despawn phải khác bị tiêu diệt. Chuyển dạng phải hiển thị đúng dạng và chuỗi, không hiểu là boss đã hồi sinh hoàn chỉnh.
4. Với boss cooldown thông thường, ETA cập nhật theo thời điểm nghỉ thật và cấu hình runtime. Khi chưa vào REST sau chết, ghi chờ xác định thời gian hoặc ước tính có nhãn rõ; không coi deathTime+secondsRest là mốc chính xác. ETA là sớm nhất/dự kiến; nếu map chưa join được dù đã tới giờ thì “Đang chờ xuất hiện”, không hiển thị thời gian âm.
5. Boss theo lịch dùng lịch server; boss theo điều kiện/chuỗi không biết mốc thì “Chờ điều kiện”/“Chờ kết thúc chuỗi”/“Chưa xác định”. Không ép mọi boss theo một số phút. Chỉ ghi bản đồ xuất hiện tiếp theo khi đã có dữ liệu thực tế.
6. Lưu lịch sử SQL để người đăng nhập sau vẫn xem. Đề xuất giữ7 ngày, hiển thị20 dòng/trang; job dọn lịch sử theo batch và index, không xóa dữ liệu đang dùng. Runtime status dựng lại khi restart; không coi trạng thái “đang sống” lưu từ phiên server cũ là còn sống. Giữ lịch sử cũ, đồng bộ sự kiện mới từ boss runtime.

## Lộ trình triển khai

1. **Chốt phạm vi và catalog:** audit BossManager/OtherBossManager/FinalBossManager, BossType, map phó bản/isNotifyDisabled và các override. Tạo quy tắc theo dõi công khai; mặc định boss bản đồ chung. Chia nhóm cooldown/lịch/điều kiện/chuỗi, có adapter ETA cho nhóm đặc biệt.
2. **Tracker server:** thêm BossNoticeService và model event/snapshot; hooks xuất hiện/chết/leave/rest/chuyển dạng ở luồng thực tế, dedup theo lượt. Không bắt sự kiện bằng chuỗi thông báo. Mọi snapshot là immutable/bounded; không giữ Player/Boss/Zone trong lịch sử để tránh leak. Luồng boss không chờ truy vấn trang của client. Queue persistence có giới hạn và retry/idempotency, không âm thầm bỏ sự kiện; thiết kế phục hồi khi SQL tạm mất trước khi chốt triển khai.
3. **SQL:** migration bảng boss_spawn_history/event (InnoDB) với khóa UUID và index thời gian/trạng thái/template. DAO ghi idempotent, đọc snapshot/page bounded, lưu timestamps/killer snapshot. Snapshot SQL và migration chạy hai lần kiểm chứng ở DB riêng. Không thêm zone column vào payload công khai.
4. **Protocol:** thêm packet/subcommand riêng sau audit để tránh xung đột, capability handshake client mới; snapshot/paging/delta có revision/eventID/serverNow. DTO chỉ có dữ liệu cần hiển thị và không chứa khu. Client cũ vẫn nhận Thông báo hiện có; số dòng packet50 tính theo danh sách thực tế và giới hạn legacy, không hardcode10 nếu sửa luồng này. Request hợp lệ cho người online, giới hạn page/page size/tần suất, không cho client đọc lịch sử riêng ngoài quyền.
5. **Client cả hai tab Game1/Game2:** giữ Thông báo hệ thống, thêm Boss và Hiện tại/Lịch sử; model riêng không dùng RMS ID của thông báo tĩnh. Đọc snapshot/delta chống packet cũ đè mới. Dùng serverNow + elapsed monotonic khi vẽ tuổi sự kiện/countdown; chặn hiển thị khu ở mọi dòng/detail/filter. Có loading/empty/retry và nút Làm mới; đổi account/server hoặc logout xóa cache, hủy request cũ.
6. **Build và triển khai:** build client ở project copy riêng (Unity đang mở không bị đóng), xuất bản PC mới trong Builds. Server candidate chạy kiểm tra và review trước deploy. Bảo trì lưu dữ liệu, snapshot SQL, áp migration và backup SRC/20.jar theo quy tắc workspace; ghi hash/metadata, cài JAR đã kiểm tra, khởi động/TCP. Cung cấp ZIP client và ghi rõ cần dùng client mới để xem bảng Boss.

## Kiểm chứng trước triển khai

- Lượt xuất hiện/chết thực tế, spawn join thất bại, không killer, pet/master, hai boss cùng tên, hai người đánh chết cùng lúc: mỗi lượt chỉ ghi một lần, không đổi reward/drop.
- Boss nhiều dạng (Fide/Xên...), custom Broly/BlackGoku và boss theo giờ: thời gian/chuỗi đúng; không gán cooldown sai hoặc lịch giả.
- Không khu trong UI hoặc packet: kiểm tra serializer/schema DTO và đọc packet; map cũ và map lần tới không bị nhầm.
- Khi đăng nhập sau, đọc được lịch sử đã lưu; restart không giữ trạng thái sống giả; SQL lỗi/retry không tạo event trùng hoặc bỏ sự kiện.
- Tuổi sự kiện/countdown đúng dù đồng hồ PC sai, qua ngày, tới0 hoặc delta tới muộn; revision cũ không đè revision mới.
- Client Game1/Game2, màn hình PC/hẹp, nhiều dòng/trang, thông báo hiện có và cache đa account/server.
- Spam refresh/phân trang không gây truy vấn nặng, số dòng/payload bounded; phiên offline và request sai bị từ chối. Full build/tests/logs trước claim hoàn tất.

Đã triển khai: lịch sử 7 ngày, 20 dòng/trang, làm mới khi mở bảng/đổi tab hoặc bấm Làm mới; không tự tải lại định kỳ; packet -58 và capability BOSS1, không có zoneId. Lịch sử bắt đầu từ lần cài tính năng này.
