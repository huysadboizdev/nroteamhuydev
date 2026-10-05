# Sửa lệnh admin, dịch chuyển tới boss, hồi phục và hoàn thành tiến độ KOL

**Goal:** Sửa các lỗi admin đã kiểm tra; `boss` mở bảng boss và Thách đấu tới đúng boss; `hs` gỡ khống chế/hồi đầy HP/KI; `kol` đủ tiến độ nhiệm vụ hiện tại. Mọi chức năng này chỉ dành cho tài khoản admin.

**Architecture:** Router so khớp token hoàn chỉnh. Một service thao tác admin kiểm tra quyền/session ngay trước thay đổi. Bảng boss giữ ánh xạ từ lựa chọn tới đúng đối tượng boss; thao tác hồi phục dùng hàm gỡ hiệu ứng và gửi packet hiện có. KOL cập nhật tiến độ theo catalog, giữ luồng nhận thưởng có biên nhận hiện tại.

**Tech Stack:** Java21, server JAR SRC/20.jar, MariaDB/MySQL, packet client hiện có. Workspace không có Git; dùng backup source/hash và JAR có metadata thay cho commit/worktree.

## Hành vi chốt và giả định

- `boss`: mở ngay bảng boss đang được phép hiển thị. Nút Thách đấu đưa admin tới đúng map, khu và tọa độ hiện tại của boss đã chọn. Boss chưa xuất hiện, chết, rời map hoặc lựa chọn hết hạn: thông báo rõ và yêu cầu mở lại bảng.
- `hs`: gỡ choáng, trói, thôi miên, mù, hóa đá, làm chậm, hóa socola, phong ấn bình/Mafuba và giữ của Ma Bư; kết thúc hoạt ảnh khóa thao tác nếu có. Hồi HP/KI tới mức tối đa sau khi tính lại chỉ số; đang chết thì hồi sinh tại chỗ. Giữ reset hồi chiêu của hs cũ. Buff có lợi như khiên/biến khỉ được bảo toàn khi không phải nguyên nhân khóa thao tác.
- `kol`: theo trả lời người dùng, chỉ đủ tiến độ nhiệm vụ **hiện tại**, nhận thưởng qua menu hiện có. Chọn nhánh bằng `player.kolMenuVip` mà menu Quy Lão đang lưu: sau mở KOL VIP thì áp dụng nhiệm vụ VIP hiện tại; sau mở KOL thường thì áp dụng nhiệm vụ thường. Khi chưa chọn menu, mặc định KOL thường. Thông báo nêu nhánh và nhiệm vụ đã hoàn thành, nhắc mở lại menu để nhận.
- `kol` không tự nâng stage/đánh dấu đã nhận thưởng, không tự cộng VND/điểm. VIP vẫn cần vé1825 để nhận thưởng như hiện tại. Counter đang dùng chung giữa hai nhánh: khi hai nhánh cùng nhiệm vụ, đủ counter sẽ tự đủ điều kiện ở cả hai; không đổi cấu trúc dữ liệu KOL để tách counter.
- Quyền dùng `account.is_admin` → `session.isAdmin` → `player.isAdmin()`, đúng cơ chế hiện tại. Kiểm tra ở router, service và lúc bấm Thách đấu/form/menu. Tài khoản thường không được mở bảng admin, gỡ hiệu ứng, hồi phục hay sửa KOL bằng lệnh này. Không coi VIP/KOL là quyền admin.
- Phạm vi còn bao gồm lỗi trong báo cáo `docs/source-analysis/admin-command-audit-2026-10-04.md`: Ban, buff tiền/vật phẩm, cú pháp/chỉ số, đổi tên, nhiệm vụ và thông báo form.

## 1. Backup và kiểm thử trước sửa

- [x] Backup `Command.java`, `Controller.java`, `BossManager.java`, `Input.java`, `EffectSkillService.java`, `KolQuestService.java`, `PlayerService.java`, `Client.java`, `TaskService.java`, `NpcFactory.java` và source khác thực sự cần sửa; lưu hash JAR và hai SQL dump.
- [x] Giữ nguyên quyền tài khoản. DB hiện có3/3 tài khoản admin; đây là phát hiện cần quản trị xác nhận, không tự hạ quyền.
- [x] Chuyển các tình huống trong `.analysis/admin-audit/AdminAudit.java` thành assertion hành vi đúng, chạy RED trên JAR cũ. Dùng fixture/session và schema DB riêng cho các thao tác cấp đồ/ban/đổi tên/thưởng.
- [x] Thêm kiểm thử RED cho boss/Thách đấu, hs, kol, quyền admin; không chạy các lệnh làm thay tài sản trên production để thử.

## 2. Sửa router và lớp quyền

Files: `Command.java`; dự kiến mới `services/AdminCommandService.java`.

- [x] Tách tên lệnh khỏi tham số bằng khoảng trắng, so khớp tên đầy đủ; tránh boss/ban/buff thành b, menu/map thành m,123 thành1. Hỗ trợ nhiều khoảng trắng.
- [x] Đăng ký boss, hs, kol riêng; giữ b, item, getitem, brl và các lệnh cũ. Hỗ trợ alias dm/dmg, hp/hpg; hướng dẫn đúng cú pháp, thêm hướng dẫn ngắn khi nhập thiếu/sai tham số.
- [x] Kiểm tra session/role ở nơi thực thi. Làm mới session không còn quyền thì chặn thao tác; kiểm thử giả mạo packet/typeInput/menu không tạo thay đổi. Quyền trong phiên phải lấy từ trạng thái server, không từ dữ liệu client gửi.
- [x] Kiểm tra quyền lại tại các handler GIVE_IT/GET_IT/SEND_ITEM_OP/FIND_PLAYER/CHANGE_NAME và các nhánh menu admin trước tác động. Kiểm tra số trường input, thông báo parse lỗi; ghi log lỗi ngoài dự kiến thay vì catch rỗng.

## 3. Boss và nút Thách đấu

Files: `BossManager.java`, `Controller.java`, service admin mới; trạng thái lựa chọn theo player/session, tránh ghi DB.

- [x] Tạo snapshot danh sách boss đã lọc một lần để số dòng packet khớp dòng ghi. Không dùng vị trí hiện tại trong ArrayList làm định danh lâu dài.
- [x] Mỗi lựa chọn ánh xạ đến đúng **instance boss** trong snapshot của phiên admin. Boss cùng template/ID xuất hiện ở nhiều khu vẫn phân biệt được. Kiểm tra TTL/phiên/đối tượng còn tồn tại khi bấm.
- [x] Packet Thách đấu chỉ nhận ở menu boss chính xác, không dùng default cho mọi menuType khác. Bảng Broly nếu dùng chung nhánh phải có resolver riêng hoặc cùng cơ chế snapshot.
- [x] Khi bấm: kiểm tra admin, boss chưa chết/rời map, zone/map/location hợp lệ và boss vẫn trong khu; đọc vị trí mới nhất rồi dùng `changeMapYardrat` tới đúng Zone/tọa độ. Không chọn ngẫu nhiên khu bằng map ID.
- [x] Khi boss chuyển khu trong lúc thao tác, kiểm tra lại hoặc báo danh sách đã cũ; không tele nhầm đối tượng. Tránh khóa chéo player/boss gây deadlock.
- [x] Số hàng packet là byte: xác định giới hạn client, giới hạn/trang hóa phù hợp khi vượt dung lượng; không để byte count tràn. Hiển thị thông báo khi danh sách rỗng hoặc packet lỗi.
- [x] Kiểm thử chọn đầu/cuối, nhiều boss cùng ID, thêm/xóa danh sách sau mở, boss chết/rời/chuyển khu, ID không hợp lệ, menu khác, mất quyền, phiên client trước/sau214.

## 4. Lệnh hs

Files: `AdminCommandService.java`, `EffectSkillService.java`; tái sử dụng `Service.hsChar`, `PlayerService.sendInfoHpMp` và packet hiệu ứng.

- [x] Lập bảng hiệu ứng khống chế với hàm remove tương ứng, không gọi chung `removeSkillEffectWhenDie` vì hàm đó còn xóa buff và chưa gỡ đủ anTroi/socola.
- [x] Gỡ trói hai đầu: giải phóng liên kết người trói/người bị trói đúng đối tượng. Gỡ bình/Ma Bư và slot/liên kết còn giữ; làm sạch timer, cờ và packet trạng thái để hiệu ứng không tự quay lại ở tick sau.
- [x] Rà các trạng thái đặc thù đang khóa thao tác (hoạt ảnh biến khỉ/đổi thân...), kết thúc đúng hàm; không đổi PK hay buff không liên quan theo cách reset mù.
- [x] Tính lại hpMax/mpMax sau gỡ debuff; hồi100%. Sống thì cập nhật HP/KI bình thường; chết thì dùng luồng hồi sinh tại chỗ không trừ tiền. Reset hồi chiêu như lệnh hs hiện có.
- [x] Chỉ sửa nhân vật admin đang nhập lệnh; gửi cập nhật cho bản thân và người trong khu. Lỗi packet không lặp mutation/phát tác dụng hai lần.
- [x] Test từng hiệu ứng và nhiều hiệu ứng đồng thời, trói với caster khác, bình/Ma Bư, chết/sống, buff có lợi, gọi lặp, cập nhật tick sau, quyền thường bị từ chối.

## 5. Lệnh kol theo nhiệm vụ hiện tại

Files: `KolQuestService.java`, `AdminCommandService.java`, `QuyLaoKame.java` nếu cần chuẩn hóa lưu nhánh đã chọn.

- [x] Lấy stage của nhánh đang chọn từ KolQuestState hợp lệ. Stage8: báo đã xong; state sai: báo lỗi, không tự reset lịch sử.
- [x] Chỉ nâng counter của stage hiện tại đến `max(counter, KolQuestCatalog.required(stage))`. Stage3 phải đủ cả fide1Kills/fide2Kills/fide3Kills; stage khác theo đúng mapping catalog.
- [x] Giữ nguyên mọi counter khác, normalStage/vipStage, lịch sử receipt và số dư. Counter cao hơn yêu cầu không bị hạ. Invalidate menu nhận thưởng cũ để người dùng mở lại đúng menu hiện tại.
- [x] Khóa theo player, xử lý/từ chối rõ khi giao dịch KOL/ruby/mua ví đang chờ đối soát; không ghi đè snapshot pending. Lưu tiến độ bằng KOLProgressData/PlayerDAO đúng cột `player.nhiem_vu_kol`, giữ dữ liệu legacy có liên quan.
- [x] Xác nhận đã lưu trước thông báo thành công; DB lỗi không thông báo thành công giả. Nhận thưởng vẫn qua KolRewardDAO/biên nhận, chống double claim như hiện tại.
- [x] Test đủ7 stage, Fide ba counter, thường/VIP lệch stage, mặc định/nhánh đã chọn, stage8, không véVIP, không tự thưởng, counter lớn, gọi lặp, logout/relogin, DB lỗi/pending và quyền thường.

## 6. Hoàn tất các lỗi admin đã phát hiện

- [x] Ban: sửa account.ban=1, kiểm tra cập nhật đúng account/rowcount rồi mới báo thành công và kick; thử đăng nhập bị chặn trên schema/session test. Không ban tài khoản thật để thử.
- [x] Buff gem/ruby: cộng long trước clamp, chặn số lượng âm/0; giữ khóa/pending guard. Các lệnh chỉ số/up/upp validate miền số và tránh mutation khi parse lỗi.
- [x] Cấp đồ chung: kiểm tra ID catalog trước castshort, option0–250 theo catalog thực, quantity dương có giới hạn theo vật phẩm; copy sâu option. Kiểm tra sức chứa và kết quả cấp, báo đúng số nhận hoặc thất bại; gửi thông báo cho đúng người nhận.
- [x] Đổi tên: cập nhật DB cùng chỉ mục `Client.players_name` dưới khóa phù hợp; kiểm tra uniqueness/phiên/kết quả SQL. Nếu dùng unique index tên, migration phải kiểm tra trùng theo collation và backup; không thêm index theo giả định.
- [x] Nhiệm vụ n: tìm task đích hợp lệ trước khi gán, không vô tình phát thưởng từ task cũ; lỗi không để lại id/index sai.
- [x] Lệnh m: map/khu không hợp lệ hoặc tile chưa nạp phải báo rõ; không im lặng. Menu/người chơi offline/không có đệ tử đều có phản hồi đúng.

## 7. SQL và kiểm chứng phát hành

- [x] Truy vấn chỉ đọc kiểm tra role/schema, item/option, task, map, tên; đối chiếu hai dump. Ba lệnh boss/hs/kol dùng schema hiện có, không cần thêm role mới.
- [x] Nếu phát sinh migration thật (ví dụunique tên), lập riêng SQL có điều kiện, test schema riêng, snapshot trước khi áp dụng và cập nhật hai dump. Mỗi thay đổi SQL phải gắn với lỗi đã chứng minh.
- [x] Build candidate trên JAR hiện tại; đối chiếu các entry thay đổi/source hash, chạy test admin mới và72 nhóm regression đang có (capsule/ruby/Namek/Bojack/KOL/inventory...).
- [x] Xác minh server dừng qua bảo trì có lưu; xử lý vòng tự restart gắn với server. MOVE SRC/20.jar cũ vào SRC/jar-backup tên timestamp duy nhất, kiểm tra SHA backup và bản mới, ghi metadata trước cài.
- [x] Khởi động và kiểm tra runtime/cổng; probe xác nhận class admin được nạp từ SRC/20.jar.
- [x] Kiểm thử packet/fixture client 213/214, boss/hs/kol và quyền tài khoản thường; 116/116 nhóm PASS trên JAR đóng gói.
- [ ] Nghiệm thu bằng giao diện client thật: boss→Thách đấu đúng instance/khu; hs hết khống chế/đầy HPKI; kol nhận thưởng qua menu một lần, VIP cần vé. Checklist riêng trong .analysis/admin-fix/manual-client-checks.md.
- [x] Lưu báo cáo và rollback: bảo trì/backup JAR hiện tại trước phục hồi; giữ tiến độ, số dư và receipt phát sinh, không đè snapshot tài sản cũ.

## Trạng thái

Đã triển khai và cài JAR ngày 04/10/2026 sau backup, rà soát và 116/116 nhóm kiểm thử PASS (72 regression +44 admin). Server hoạt động trên cổng14445; runtime probe PASS. Không migration SQL, không đổi quyền tài khoản. Counter KOL thường/VIP tiếp tục dùng chung theo thiết kế hiện có.

Nghiệm thu gameplay bằng giao diện client thật còn mở; không khẳng định bước này đã làm. Chi tiết kết quả, hash, backup và rollback: docs/source-analysis/admin-commands-release-2026-10-04.md.
