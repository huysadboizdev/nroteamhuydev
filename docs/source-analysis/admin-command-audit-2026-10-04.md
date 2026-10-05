# Kiểm tra các lệnh admin — 04/10/2026

Đã đối chiếu source với JAR SRC/20.jar, chạy công cụ tái hiện riêng trên JAR thực và truy vấn chỉ đọc DB đang dùng. Kiểm tra cả team2026.sql và SRC/sql/nro1.sql. Không chạy lệnh cấp đồ, Ban, Kick hay bảo trì trên người chơi thật; không thay source ứng dụng, SQL dữ liệu hoặc JAR trong đợt kiểm tra này.

## Lỗi đã tái hiện trên JAR thực bằng fixture

| Vị trí | Lỗi và ảnh hưởng | Sửa đề xuất |
|---|---|---|
| Command.java:256–267 | Không có alias boss; startsWith("b") nhận boss thành b, mở form SEND_ITEM_OP. ban/buff cũng thành b; map/menu thành m; nhiemvu thành n; 123 thành1; toadoXYZ thành toado. | Tách token đầu, so khớp toàn bộ tên lệnh. Đăng ký boss gọi BossManager.showListBoss; giữ b riêng cho buff. |
| Command.java:140–166 | Thông báo hướng dẫn dmg/hpg, nhưng handler đăng ký dm/hp. Nhập theo hướng dẫn luôn lỗi. | Chuẩn hóa hướng dẫn và hỗ trợ alias dm/dmg, hp/hpg. |
| Command.java:140–177 | dm/hp/ki chấp nhận số âm, ghi trực tiếp vào chỉ số gốc. | Parse và kiểm tra miền giá trị trước mutation, thông báo lỗi rõ. |
| Command.java:214–239 | i không kiểm tra addItemBag trả false, vẫn báo SUCCESS khi túi đầy. q âm bỏ qua vòng lặp nhưng cũng báo thành công. | Chặn q<=0, kiểm tra sức chứa và kết quả từng lần cấp; báo đúng số đã cấp/không cấp. |
| Command.java:192–209 | split(" ") không xử lý nhiều khoảng trắng; không kiểm tra đủ hai thành phần optionId:value. | Dùng token khoảng trắng và validate đủ số/option trước tạo vật phẩm. |
| Command.java:214–237 | ID ép short trước kiểm tra catalog: i65536 tạo template0 rồi mới lookup tên bằng ID gốc. Fixture xác nhận cấp sai template; với catalog thật lookup sau đó còn báo lỗi dù đã cấp. | Kiểm tra0<=ID<catalog.size trước cast và trước cấp bất cứ thứ gì. |
| Command.java:218–220 | Hai vật phẩm tạo từ i4322 50:10 có cùng đối tượng ItemOption vì chỉ copy list. Sửa option một món có thể ảnh hưởng món kia trong RAM. | Copy sâu từng option khi tạo mỗi món. |
| Input.java:98–627 | Form GET_IT nhập sai số bị catch(Exception) rỗng ở cuối, người dùng không nhận lỗi. SEND_ITEM_OP/GIVE_IT cũng đi qua catch này. | Validate số trường, parse và thông báo có tên trường; ghi log lỗi ngoài dự kiến. |

upp100 trong bản hiện tại chạy đúng handler upp. Có chồng prefix với up nhưng thứ tự HashMap hiện tại chọn upp trước; không tính là lỗi đã tái hiện. Bộ so khớp token đề xuất loại bỏ sự phụ thuộc vào thứ tự.

## Lỗi xác định qua code và SQL

1. **Ban không khóa tài khoản lâu dài.** PlayerService.java:252 ghi account.ban=0. MrBlue.java:90 chặn khi getBoolean("ban") làtrue. Schema live và hai dump đều dùng ban tinyint/default0. Chuỗi SQL ban=0 có trong bytecode JAR thực. Player.java:461 ngắt phiên sau5giây nhưng lần đăng nhập sau không bị khóa. Đề xuất sửa thành1, kiểm tra rowcount trước thông báo; chưa thực hiện Ban trên tài khoản thật.
2. **Buff ngọc có thể tràn int trước clamp.** Input.java:247/256 cộng gem/ruby+slItemBuff dưới kiểuint rồi mới Math.min(...,2000000000). Ví dụ1.500.000.000+1.000.000.000 tràn thành số âm. slItemBuff âm cũng được nhận. Đề xuất cộng kiểulong, chặn số lượng không dương và clamp hợp lệ; giữ khóa người chơi và kiểm tra giao dịch ruby đang chờ.
3. **Đổi tên không cập nhật chỉ mục người chơi online.** Input.java:424 cập nhật plChanged.name và SQL; Client.players_name vẫn có key cũ, getPlayer(String) lookup đúng map này. Tìm/buff/tặng theo tên mới có thể không thấy, key cũ có thể còn sót sau thoát. Cần cập nhật DB và các chỉ mục tên có kiểm tra phiên và xung đột.
4. **Form buff gửi thông báo nhầm người.** Input.java:279–281 gọi createTutorial(player,...) hai lần; lần hai khi khác ID đáng lẽ gửi pBuffItem. Người nhận không được báo từ nhánh này.
5. **Nhảy nhiệm vụ n thay state trước kiểm tra tồn tại.** Command.java:127 sửa id=idTask-1/index0, rồi gọi sendNextTaskMain. TaskService.sendNextTaskMain gọi rewardDoneTask trước khi chọn nhiệm vụ mới. ID ngoài catalog có thể làm state không hợp lệ; các nhiệm vụ3/4/5/6 còn có nhánh theo giới tính nên không phải luôn nhảy đúng ID nhập. Cần lấy task đích hợp lệ trước rồi gán; tránh vô tình phát thưởng nhiệm vụ cũ.
6. **Các đường cấp đồ chưa đồng nhất validate.** SEND_ITEM_OP/GIVE_IT/GET_IT ép IDshort, nhận quantity/option chưa kiểm tra toàn bộ; GIVE_IT/GET_IT bỏ qua kết quả addItemBag. GIVE_IT thiếu kiểm tra isAdmin ngay tại handler; FIND_PLAYER/CHANGE_NAME và nhiều nhánh menu cũng dựa vào quyền lúc mở form thay vì xác nhận lại lúc thao tác. Đây là thiếu lớp kiểm tra quyền, chưa chứng minh người chơi bình thường có thể tự đặt typeInput/menu và khai thác trực tiếp.

## Kết quả SQL

- Quyền được load từ account.is_admin, không từ player. Cột live là tinyint(1) NOT NULL DEFAULT0, khớp hai dump; không có NULL hoặc giá trị ngoài0/1.
- Cả3 tài khoản hiện có is_admin=1. Cần người quản trị xác nhận đây là các tài khoản admin dự kiến; chưa tự hạ quyền bất kỳ tài khoản nào.
- item_template đủ2001 ID liên tục0–2000; item_option_template đủ251 ID liên tục0–250. Không có thiếu ID catalog để giải thích boss mở buff.
- Shop không tham chiếu item/option không tồn tại; không có item_shop_option thiếu shop.
-30 nhiệm vụ chính đều có nhiệm vụ con; không có nhiệm vụ con mồ côi. ID task ngoài0–29 phải bị chặn trước mutation.
-169 map,93 NPC; ID có khoảng trống. Lệnhm cần kiểm tra map thực tồn tại và dữ liệu tile đã nạp, không chỉ kiểm tra min/max. changeMapInYard hiện im lặng nếu getMapCanJoin trả null.
- Không có player thiếu account, account có nhiều nhân vật, hoặc tên nhân vật trùng hiện tại. account_id có unique index; name chưa có unique index. Code kiểm tra tên rồi UPDATE riêng không chống được đổi tên đồng thời; nên đánh giá thêm ràng buộcunique sau kiểm tra collation và dữ liệu khi triển khai sửa.
- Hai dump có đủ bảng liên quan, định nghĩa quyền/ban khớp và có vật phẩm vé2000. Không thấy cần sửa dữ liệu catalog để chữa lỗi định tuyến.

## Lệnh thực tế hiện có

| Nhóm | Lệnh đang đăng ký | Ghi chú |
|---|---|---|
| Menu | a | Menu admin có Ngọc rồng/Đệ tử/Bảo trì/Tìm người/Boss. Muốn xem boss hiện dùng a→Boss. |
| Boss | brl | Danh sách Broly; boss chưa đăng ký. |
| Đồ | item, getitem, b, i ID Q [OPTION:PARAM...] | item tặng qua form, getitem nhận qua form, b buff người khác, i cấp trực tiếp. |
| Chỉ số | dm SỐ, hp SỐ, ki SỐ, up SỐ, upp SỐ | dm/hp khác hướng dẫn dmg/hpg; upp hiện hoạt động khi có đệ tử. |
| Di chuyển | m ID, toado, d | m không báo lỗi khi map không tồn tại. |
| Nhiệm vụ/kỹ năng | n ID, hs | n có mutation/reward phụ; hs reset hồi chiêu. |
| Bot |1,2 |1 menu bot;2 gọi bot tấn công. startsWith hiện còn nhận123 thành1. Chưa gọi bot thật để kiểm chứng gameplay. |

## Thứ tự sửa đề xuất

1. Router theo token, aliasboss/dmg/hpg và hướng dẫn lệnh; chặn lệnh không hợp lệ bằng thông báo.
2. Ban=1 và buff tiền cộnglong; xác nhận quyền tại từng handler, không thay danh sách tài khoản admin.
3. Bộ validate/cấp đồ chung: ID, option, số lượng, sức chứa, copy sâu, kết quả cấp chính xác.
4. Đổi tên cập nhật chỉ mục online/DB, nhiệm vụ lấy state hợp lệ trước khi gán; thông báo cho đúng người.
5. Kiểm thử riêng toàn bộ trường hợp trên và regression trước khi bảo trì/sao lưu/thay JAR. Quyền tài khoản/unique name cần quyết định theo dữ liệu thực, không sửa theo giả định.

Bằng chứng: .analysis/admin-audit/command-reproduction.log, sql-read-only.json, audit-metadata.json, AdminAudit.java. Bộ kiểm tra không thay vật phẩm/số dư trên production. Các lệnh có tác động thật như bảo trì, Ban, Kick, bot và chuyển map chưa được chạy trên người chơi thật; phát hiện tương ứng dựa trên code/SQL và bytecode nếu đã nêu.
