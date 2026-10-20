# Kế hoạch ổn định server và bảo trì từ web

> Trạng thái: đã triển khai phần source Java và giao diện/API web; chưa build JAR, chưa chạy migration trên database thật, chưa triển khai VPS.
> Chưa có thread dump/VPS để xác nhận chính xác nguyên nhân lag thực tế; sửa các rủi ro có thể tái hiện trong source local.

**Goal:** Khắc phục các lỗi có thể gây nghẽn khi chơi lâu và cho quản trị viên bảo trì ngay từ webgoc2026, lưu dữ liệu trước khi ngắt người chơi.

**Architecture:** Tái sử dụng hàng đợi quản trị SQL hiện có, thêm trạng thái bảo trì bền vững và một bộ điều phối Java chạy độc lập với worker xử lý lệnh. Giữ JVM và kênh điều khiển hoạt động trong chế độ bảo trì để web có thể mở đăng nhập trở lại.

**Tech Stack:** Java, JDBC/HikariCP, MySQL, PHP/mysqli, JavaScript; giữ giao diện quản trị hiện có.

## 1. Phạm vi và giới hạn bằng chứng

- Người dùng báo VPS chơi 40 phút đến 3 giờ thì tất cả cùng xoay, không hiện quái và đăng nhập lại thất bại.
- Source local có hàng đợi Sender không giới hạn, lỗi gửi bị nuốt, hai nơi cập nhật Player, executor không được chủ động quản lý vòng đời, và pool SQL tối đa 1 kết nối.
- Login giữ LiveSyncCommandGate.LIFECYCLE_LOCK trong toàn bộ quá trình tải dữ liệu; lệnh web cũng dùng khóa này. Đây là điểm cần kiểm tra chờ khóa, chưa chứng minh deadlock thực tế.
- Chưa có thread dump, SQL processlist, thông số VPS hoặc xác nhận JAR/config trên VPS khớp local. Không coi các rủi ro trên là nguyên nhân đã xác nhận của sự cố VPS.
- Không tự build JAR/APK/EXE, không triển khai VPS, không kích hoạt bảo trì trên hệ thống thật khi đang kiểm thử.

## 2. Phương án

1. **Đề xuất: bảo trì mềm, giữ JVM.** Chặn đăng nhập, ngừng thao tác thay đổi dữ liệu mới, hoàn tất giao dịch đang xử lý, lưu, ngắt phiên; giữ worker điều khiển để mở lại từ web. Cần quản lý trạng thái rõ ràng nhưng phù hợp việc điều khiển hoàn toàn trên web.
2. **Tận dụng tắt/restart hiện tại.** Ít thay đổi hơn nhưng restart_server.bat tự mở lại sau 300 giây; khi Java đã dừng, hàng đợi web không thể khởi động Java nếu không có dịch vụ quản lý tiến trình riêng.
3. **Dịch vụ ngoài JVM để điều khiển tiến trình.** Có thể can thiệp khi JVM treo nhưng cần thiết lập riêng trên VPS và không bảo đảm lưu dữ liệu khi buộc dừng; ngoài phạm vi lần này.

## 3. Hành vi bảo trì đề xuất

- Trang admin mới: “Máy chủ / Bảo trì”, nút “Bảo trì ngay” và “Mở lại server”. Nút bảo trì có xác nhận vì sẽ ngắt toàn bộ người chơi.
- Nhấn bảo trì: gửi lệnh có request_id, xác thực admin và CSRF; web hiển thị chờ nhận lệnh, không báo hoàn tất chỉ vì enqueue thành công.
- Trạng thái: OPEN -> DRAINING -> MAINTENANCE; lỗi lưu -> FAILED với đăng nhập vẫn đóng; mở lại hợp lệ -> OPEN.
- DRAINING chặn đăng nhập và thay đổi mới trước khi chụp danh sách phiên. Hoàn tất/đối chiếu giao dịch đang dang dở theo cơ chế hiện có; sau đó xác nhận lưu và ngắt phiên.
- Hoàn tất chỉ khi không còn phiên chơi/phiên chờ đăng nhập cần ngắt, dữ liệu đã lưu, không còn giao dịch chưa đối chiếu thuộc đợt bảo trì.
- Không cưỡng ép kill khi SQL lỗi. Giữ dữ liệu trong JVM, hiển thị thất bại/chưa hoàn tất, cho phép thử lại. Người đã lưu có thể đã bị ngắt; không báo bảo trì hoàn tất nếu còn người chưa lưu.
- Chỉ cho mở lại sau khi lưu/đối chiếu hoàn tất và các vòng cập nhật cần thiết đã sẵn sàng.
- Trạng thái giữ qua restart: server kiểm tra cờ bảo trì trước khi cho đăng nhập. Không tự mở lại sau 5 phút đối với nút bảo trì web.
- Lệnh trùng, bấm liên tiếp, trình duyệt retry, worker đọc lại và restart giữa chừng không tạo hai đợt bảo trì hoặc phát lại lệnh cũ để đá người sau khi đã mở.
- Worker quản trị và heartbeat tiếp tục chạy; không gọi LiveSyncService.stop() từ chính worker của nó. Bộ điều phối không giữ transaction SQL hay khóa chung trong khi chờ drain.
- Tách cờ admission/bảo trì web khỏi Maintenance.isRunning hiện tại: cờ này đang kết thúc vòng chạy Player/phó bản, nên không bật nó rồi giả định đổi lại false sẽ tự phục hồi thread.
- Nút này cần Java và MySQL còn phản hồi; khi JVM deadlock hoặc SQL mất kết nối, web phải hiển thị heartbeat cũ/chờ xử lý. Không hứa nút SQL cứu được JVM đã treo.

## 4. Các bước triển khai

### A. Đo và tái hiện lỗi ổn định

**Files:** Sender.java, Session.java, LocalManager.java, Player.java, Map.java; công cụ kiểm tra mới trong SRC/test hoặc .analysis.

- [x] Thêm fixture lỗi gửi, client đọc chậm, vòng phó bản lỗi và khu lỗi cô lập; các kiểm thử source mới chạy đạt.
- [x] Thêm giới hạn/log nhẹ cho hàng đợi gửi và thời gian chờ connection SQL; hiện tượng VPS cụ thể vẫn chưa thể xác nhận khi chưa có dump.
- [ ] Chuẩn bị hướng dẫn lấy 3 thread dump/SQL processlist trên VPS.

### B. Sửa đường gửi và nhận dữ liệu

**Modify:** SRC/src/nro/models/network/Sender.java, Session.java, Collector.java khi cần.
**Test:** SRC/test/nro/models/network/SenderLifecycleTest.java (mới).

- [x] Giới hạn hàng đợi theo số gói, bytes và tuổi; quá tải/nghẽn đóng socket để collector chạy logout bình thường.
- [x] Watchdog write đóng socket để giải phóng ghi bị treo; không dùng `SO_TIMEOUT` như write timeout.
- [x] Kiểm tra lỗi gửi, broadcast dùng chung Message, queue ordering và disconnect cạnh tranh bằng `SenderLifecycleTest`.

### C. Sửa vòng cập nhật và quản lý thread

**Modify:** Player.java, Zone.java, Controller.java, Map.java và các lớp phó bản BanDoKhoBau/SnakeWay/DestronGas/RedRibbonHQ.
**Test:** bài kiểm tra Player scheduler và dungeon lifecycle mới; chạy lại nhóm dungeon timing hiện có.

- [x] Chặn tick kép của người thật trong map; bảo đảm Player worker chỉ start một lần và dừng ở logout.
- [x] Quản lý vòng đời worker Player/phó bản; vòng phó bản nghỉ cả khi update ném lỗi và lỗi một zone không dừng các zone khác.
- [x] Pause/resume các vòng gameplay được kiểm thử trong thời gian bảo trì.
- [ ] Cô lập lỗi từng khu, đo tick chậm; không tự đổi 5000 ms thành 1000 ms trước khi kiểm tra tác động đến cooldown và tải.

### D. SQL và thứ tự khóa

**Modify:** LocalManager.java, Config.example.properties, MySession.java, LiveSyncService.java, dịch vụ liên quan sau khi tái hiện.
**Test:** nhóm LiveSyncGate/Queue/MutationGate và kiểm thử cạnh tranh với pool nhỏ.

- [x] Login lấy lease admission; maintenance chờ login đang tải xong trước khi lưu/ngắt phiên.
- [x] Thêm JDBC connect/read timeout và Hikari checkout timeout/leak detection, ghi log khi chờ connection lâu.
- [x] Đặt pool cấu hình local/example 2 idle/5 max cho thử nghiệm 10 người.
- [ ] Có VPS/dump để xác nhận lock cycle, giới hạn MySQL và chạy cạnh tranh với pool=1.

### E. Điều phối bảo trì Java và SQL

**Create:** SRC/src/nro/models/services/ServerMaintenanceService.java, ServerMaintenanceDAO.java; migration SRC/sql/20261020_server_maintenance.sql.
**Modify:** Maintenance.java, ServerManager.java, Client.java, MySession.java, LiveSyncService.java, LiveSyncCommandDAO.java và điểm chặn mutation cần thiết.
**Test:** SRC/test/nro/models/services/ServerMaintenanceTest.java, ServerMaintenanceDbTest.java (mới).

- [x] Thêm test trạng thái, receipt/lost commit, save failure, retry, resume và restart.
- [x] Lưu phase/revision/request/heartbeat; commit intent trước drain và terminal state sau khi lưu/ngắt.
- [x] Thêm `MAINTENANCE_START` và `MAINTENANCE_OPEN` trên worker riêng, đóng login/mutation, save checked và kick session đúng một lần.
- [x] Heartbeat chạy worker riêng để tiếp tục cập nhật khi drain lâu; gameplay pause/resume được test.
- [ ] Giữ đường bảo trì tắt tiến trình hiện tại tách biệt nếu vẫn cần; không gọi restart_server.bat trong bảo trì mềm từ web.

### F. Web quản trị

**Create:** C:/xampp/htdocs/webgoc2026/admin/server.php, admin/api/server_status.php, admin/assets/js/server-maintenance.js.
**Modify:** admin/includes/sidebar.php, admin/includes/admin_command_validator.php; live_sync helper/API chỉ khi cần.
**Test:** tests/admin_maintenance_api_test.php, admin_maintenance_ui_test.js (mới).

- [x] Thêm trang trạng thái/heartbeat/lịch sử, xác nhận bảo trì, retry cùng request ID và mở lại theo phase.
- [x] POST/CSRF/admin guard; Java xác thực admin lần nữa; không nhận shell/path.
- [x] UI phân biệt queue accepted với kết quả cuối và khóa thao tác khi heartbeat cũ.
- [ ] Migration được dùng chung và có hướng dẫn áp dụng, không tự chạy trên database sản xuất.

### G. Xác minh và bàn giao

- [x] Chạy kiểm thử Java source và PHP/JS contracts; migration/API có test schema giả.
- [ ] Chạy test DB/integration thật: web -> Java -> save/kick -> login chặn -> reopen.
- [ ] Kiểm tra không mất/nhân đôi vàng, vật phẩm, giao dịch và phần thưởng trước/sau bảo trì.
- [ ] Chạy tải 10 phiên mô phỏng ít nhất 4 giờ sau khi có môi trường được phép; so sánh hàng đợi, thread, heap/GC, SQL wait và tick. Tải giả không thay thế xác minh với client thật trên VPS.
- [x] Ghi rõ VPS/thread dump/load test 10 người 4 giờ chưa có; không khẳng định đã hết lag thực tế.
- [x] Bàn giao source, migration và hướng dẫn cấu hình tại `docs/server-maintenance-web-setup.md`; không build/deploy.
- [ ] Khi được yêu cầu thay JAR: dừng bằng bảo trì có lưu; backup SRC/20.jar vào SRC/jar-backup với thời gian riêng; SHA-256, metadata, phương án rollback; không đụng thư viện SRC/lib.

## 5. Tiêu chí chấp nhận

- Lỗi gửi không để phiên nửa sống hoặc hàng đợi tăng vô hạn; thread logout/phó bản không tăng mãi sau các chu kỳ lặp.
- Người thật chỉ có một chủ sở hữu tick, không phá hành vi boss/pet/bot và cooldown.
- Không có lỗi cạnh tranh/khóa đã tái hiện còn tồn tại trong fixture; số liệu VPS vẫn cần để kết luận sự cố thực tế.
- Nút bảo trì không báo xong trước khi lưu và ngắt hết phiên; lỗi lưu không bị biến thành tắt cưỡng bức.
- Mở lại được bằng web, không tự mở sau 5 phút; restart vẫn tôn trọng trạng thái bảo trì.
- Không build, thay JAR hoặc can thiệp VPS trong giai đoạn lập kế hoạch.
