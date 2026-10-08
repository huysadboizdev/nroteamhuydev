# Thêm bộ ba Sói và tiểu đội Pi Láp — Implementation Plan

**Trạng thái 08/10/2026:** đã triển khai; probe trên server xác nhận đủ sáu boss ACTIVE với 1.000 HP, bốn chiêu và không Tái tạo năng lượng. Kết quả cuối và giới hạn kiểm chứng: [báo cáo phát hành](2026-10-08-hunting-release-verification.md). Checklist bên dưới giữ nội dung kế hoạch gốc, không coi các kiểm thử thủ công chưa chạy là đã qua.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thêm hai nhóm boss săn gồm Basil/Lavender/Bergamo và Pi Láp/Mai/Su, với HP cố định, sát thương theo từng mục tiêu và mỗi con hạ được chắc chắn rơi trang bị.

**Architecture:** Hai nhóm dùng chung xử lý combat, loot và bộ điều phối vòng đời nhóm. Tính sát thương tại đòn đánh trên từng mục tiêu, không thay `dame` toàn cục của boss. Các boss mới thuộc mục Boss của kế hoạch phân loại thông báo, không thuộc Boss nhiệm vụ và không thay Sói Hẹc Quyn hiện có.

**Tech Stack:** Java 17, SQL template hiện có, Unity 2022.3 C# Game1/Game2 và giao thức hiện có cho boss/ảnh/vật phẩm.

## Các cấu hình đã chốt với người dùng

- Mỗi con **1.000 HP tối đa**. Mỗi đòn gây sát thương hợp lệ lên boss chỉ trừ **1 HP**; không phải số sát thương âm. Đòn né/không hợp lệ không trừ HP.
- Mỗi đòn trúng người chơi gây **5% HP tối đa của chính người đó**, xuyên giáp và khiên. Không lấy HP của người boss đã đánh ở lượt trước, không nhân thêm chí mạng/hệ số kỹ năng sau khi đã tính phần trăm. Giữ quy tắc né và miễn nhiễm ngay sau hồi sinh.
- Bộ chiêu của Xên Hoàn Thiện tại võ đài, đúng `BossesData.SIEU_BO_HUNG_1`: Kamejoko cấp 7 (10 giây), Dịch chuyển tức thời cấp 7 (20 giây), Galick cấp 7 (1 giây), Thái Dương Hạ San cấp 7 (50 giây). Không sao chép phần gọi Xên con hay chuyển cấp của boss gốc.
- Ba thành viên xuất hiện trong cùng khu, nói theo kịch bản nhóm; sau khi kết thúc hội thoại, cả ba mới chuyển PK và hiện trạng thái chiến đấu/máu đỏ. Không kích hoạt từng con sau khi con trước chết.
- Spawn ở **Vách + làng + 3 map tiếp theo**, không dùng lựa chọn bỏ làng:

| Hành tinh | Map ID | Tên |
|---|---|---|
| Trái Đất | 39, 0, 1, 2, 3 | Vách núi Aru, Làng Aru, Đồi hoa cúc, Thung lũng tre, Rừng nấm |
| Namek | 40, 7, 8, 9, 10 | Vách núi Moori, Làng Mori, Đồi nấm tím, Thị trấn Moori, Thung lũng Namek |
| Xayda | 41, 14, 15, 16, 17 | Vực Plant, Làng Kakarot, Đồi hoang, Làng Plant, Rừng nguyên sinh |

- **Mỗi con** bị hạ rơi chắc chắn một món, chọn đúng một nhóm theo tỷ lệ tổng 100%: 60% đồ shop cấp 9–12 có ấn; 30% Thần Linh thường; 10% Thần Linh có ấn. Không chờ hạ hết cả nhóm mới phát trang bị.
- Đồ ấn không phải đồ kích hoạt set 127–135. Dùng Tinh/Nguyệt/Nhật ấn đã có ở option 34/35/36, một ấn cho mỗi món. Không gắn chồng hai hay ba ấn vào cùng một món.

## Kết quả kiểm tra trước triển khai

Audit chỉ đọc `.analysis/audit_hunt_boss.py`; kết quả `.analysis/new-hunt-boss-resource-check.json`.

| Nhân vật | Template cải trang tham chiếu | Head / Body / Leg | Avatar |
|---|---:|---|---:|
| Pi Láp | 635 | 612 / 613 / 614 | 5825 |
| Mai | 636 | 615 / 616 / 617 | 5824 |
| Su | 637 | 618 / 619 / 620 | 5826 |
| Basil | 730 | 745 / 746 / 747 | 6915 |
| Lavender | 731 | 748 / 749 / 750 | 6947 |
| Bergamo | 732 | 751 / 752 / 753 | 6979 |

- Không tìm thấy lớp boss Basil/Lavender/Bergamo/Pi Láp/Mai/Su trong source hiện tại. SQL có đủ hình của sáu nhân vật; source có Sói Hẹc Quyn riêng, không phải bộ ba này.
- **18 part** có type head/body/leg đúng và dữ liệu frame khớp `SRC/data/update_data/part` theo cách đọc của client (3/17/14 frame).
- **247 icon/frame/avatar/item**, tương ứng **988 PNG** ở `SRC/data/icon/x1` đến `x4`, đều tồn tại, có kích thước hợp lệ, CRC chunk đúng và giải nén IDAT được.
- Cả 15 map có `SRC/data/map/tile_map_data/<mapId>`.
- Có **52 mẫu trang bị cấp 9–12** và cả 52 xuất hiện trong shop; đồ Thần Linh có các template **555–567**. Rada cấp 9–12 dùng chung giới tính; nhẫn Thần Linh dùng template 561.
- Option 34/35/36 có tác dụng trong `NPoint`: đủ năm món cùng ấn kích hoạt set. Nhật ấn tăng HP, Nguyệt ấn tăng KI, Tinh ấn tăng sức đánh theo logic hiện có. Không sửa bonus ấn trong lần bổ sung boss.
- Client Game1/Game2 đã đọc icon động từ packet `-67`; server cung cấp qua `DataGame.sendIcon` ở đúng đường `data/icon/x<zoom>/<id>.png`. Giới hạn mảng ảnh được cấp bằng `maxSmallVersion=32767`; phải xác minh mọi icon mới dùng nhỏ hơn giới hạn trước publish.
- Chưa chạy sáu boss thực tế, hoạt ảnh kỹ năng, toàn bộ packet chat/HP/loot hoặc Unity hai tab. Không kết luận “không crash” chỉ từ việc file đầy đủ.

## Cấu hình vận hành đề xuất để duyệt cùng thiết kế

- Một nhóm Sói và một nhóm Pi Láp trên toàn server, mỗi nhóm tự chọn một map trong 15 map trên và một khu hợp lệ. Hai nhóm không cùng chiếm một khu; không chen vào khu Broly/Super đang được đặt chỗ hoặc nhóm khác đang chuẩn bị spawn.
- Nghỉ 10 phút sau khi cả ba đã chết/rời. Thành viên bị hạ không tự hồi sinh riêng giữa lượt còn hai con sống.
- Nếu map/khu hết chỗ, chờ thử lại, không đẩy boss vào khu bất kỳ. Không đặt TTL ngắn khiến người chơi chưa đánh đủ 1.000 hit đã mất lượt; chỉ thu hồi cả nhóm sau 5 phút liên tục khu không còn người, hoặc bảo trì/thu hồi chủ động.
- Vị trí lấy theo chiều rộng và nền map thật; tránh chặn waypoint/NPC, không dùng tọa độ cứng vượt map.
- Một con rơi thêm vàng 10.000–30.000 và capsule hồng ngọc 1–3; đá nâng cấp 220–224 có xác suất 70%, số lượng 1–2; ngọc rồng thường 14–20 có xác suất 30%, số lượng 1. Những tỷ lệ phụ là đề xuất, tách khỏi bảng trang bị 60/30/10 đã được chốt.
- Trang bị chọn theo hành tinh của người được tính công hạ boss, kèm mẫu dùng chung phù hợp. Đồ shop giữ chỉ số gốc từ options thật; đồ Thần Linh thường giữ chỉ số gốc, không tự thêm bonus mới.
- Bảng 60/30/10 bảo đảm có một món trang bị mỗi con, không bảo đảm cả ba loại xuất hiện trong một lượt. Không thêm cơ chế tích lũy bảo hiểm rơi đồ hiếm khi chưa được yêu cầu.

## Task 1: Danh mục boss và loot đúng template

**Files:** Create `SRC/src/nro/models/boss/HuntingTrio/HuntingTrioCatalog.java`, `HuntingTrioLoot.java`; Test `SRC/test/nro/models/services/HuntingTrioLootTest.java`.

- [ ] Viết test đủ ba khoảng random: 0–59 đồ shop có ấn, 60–89 thần thường, 90–99 thần có ấn; chứng minh mỗi lần có đúng một trang bị, không hai món và không rỗng.
- [ ] Pool cấp 9–12 lấy từ 52 template đã đối chiếu, gồm áo/quần/găng/giày/rada. Snapshot lựa chọn trong cấu hình, không query SQL mỗi lần boss chết.
- [ ] Pool thần 555–567 theo hành tinh/giới tính và nhẫn 561; gọi routine tạo đồ/default options hiện có, không tạo đồ có template mà thiếu chỉ số gốc.
- [ ] Một option 34/35/36 cho đồ ấn, param theo quy ước hiện có; không dùng option kích hoạt 127–135 thay ấn.
- [ ] Các pool phụ dùng ID thật: capsule 722, ngọc rồng thường 14–20, đá 220–224, vàng template phù hợp cơ chế nhặt vàng hiện tại. Không dùng ngọc Namek/ngọc đen/ngọc siêu cấp thay ngọc thường.
- [ ] Có khóa thưởng một lần mỗi thành viên/lượt. Quy chủ pet về master; không có owner hợp lệ, thu hồi/lỗi spawn không tạo loot. Packet drop lỗi sau khi item đã publish không được tạo thêm item bù gây nhân đôi.
- [ ] Test item nhặt lại giữ đủ options gốc/ấn, túi đầy không làm mất item dưới đất, ngọc/ruby còn quyền nhặt đúng chủ và không cấp thưởng trùng qua callback chết lặp.

## Task 2: Sát thương theo mục tiêu và HP boss

**Files:** Create `SRC/src/nro/models/boss/HuntingTrio/HuntingTrioBoss.java`; Modify `SRC/src/nro/models/services/SkillService.java` tại đường đánh người chơi dùng `BossNoticeService.injure`, hoặc bổ sung policy có kiểu rõ ràng tại wrapper để bao phủ mọi kỹ năng gây sát thương của nhóm; Test `SRC/test/nro/models/services/HuntingTrioCombatTest.java`.

- [ ] Test hai người HP tối đa 100.000 và 2 tỷ: sát thương gốc lần lượt 5.000 và 100 triệu, dù boss đổi mục tiêu liên tiếp hoặc đánh nhiều người.
- [ ] Phép tính `(long)target.nPoint.hpMax * 5 / 100`; không lấy `hp` hiện tại, không gán lại `boss.nPoint.dame` để tránh kỹ năng/luồng khác dùng sai mục tiêu.
- [ ] Tại đòn đã hợp lệ, truyền piercing để bypass giáp/khiên/giáp Xen; không nhân thêm hệ số skill/crit sau policy. Đòn miss/miễn nhiễm vẫn 0. Không thêm chiêu tự kết liễu/timer chết như Mặt Trời.
- [ ] Kiểm thử sát thương sau shield, giáp, giáp Xen, HP gần chết, người vừa hồi sinh, khác map/khu và người đang đăng xuất. Target check phải hợp lệ trước khi áp dụng policy.
- [ ] `HuntingTrioBoss.injured`: damage <=0 hoặc chưa mở chiến đấu thì 0; đòn hợp lệ 1 HP, clamp theo HP còn lại, xử lý death/credit một lần. Có guard plAtt null và skillSelect null; không dereference kiểu Broly hiện tại.
- [ ] Test đánh tay/chưởng/AOE/pet/phản sát thương/tự sát: không có đường vượt giới hạn HP boss hoặc nhân thưởng. Không thay damage boss cũ.

## Task 3: Hai nhóm xuất hiện và mở chiến đấu đồng thời

**Files:** Create `SRC/src/nro/models/boss/HuntingTrio/HuntingTrioGroup.java`, `HuntingTrioService.java`; Modify `SRC/src/nro/models/boss/Boss_Manager/BossManager.java` và `BossID.java` nếu cần semantic ID; dùng allocator ID mạng có kiểm soát riêng hoặc `MiniBossIdentity` tương thích nguồn hiện có.

- [ ] Viết test chuẩn bị đủ ba thành viên trước publish, failure ở thành viên thứ 2/3 thu hồi phần đã chuẩn bị, giải phóng chỗ đúng owner/generation.
- [ ] Cùng map/khu, vị trí cạnh nhau trong giới hạn map; ID mạng khác nhau, không đè ID Sói Hẹc Quyn/Broly/boss nhiệm vụ hoặc bản hỗ trợ.
- [ ] Ba con NON_PK/không nhận damage khi nói. Một state machine nhóm quản lý hội thoại, không mỗi con tự mở PK theo chatS riêng.
- [ ] Khi hội thoại hoàn tất, cập nhật trạng thái chiến đấu/flag của cả ba rồi mới cho AI tấn công. Test người vào khu trong hội thoại và ngay sau mở đều nhận đúng PK/HP.
- [ ] Một con chết vẫn giữ hai con còn lại; cả nhóm kết thúc mới bắt đầu cooldown. Callback chết/rời cũ không được thu hồi lượt nhóm mới.
- [ ] Ghi thông báo xuất hiện một lần mỗi nhóm, nhưng tracker lưu từng con. Không sinh ba thông báo toàn server lặp liên tục trong mỗi update.
- [ ] Test thiếu khu, map invalid, map nhỏ, bảo trì, thu hồi, người rời/quay lại, cooldown và cạnh tranh đặt chỗ với Broly/Super.

## Task 4: Resource và hai client

**Files:** Existing `SRC/src/nro/models/data/DataGame.java`; client `Assets/Scripts/Assembly-CSharp/Game1` và `Game2`: `Controller.cs`, `SmallImage.cs`, `GameScr.cs`, `Char.cs`, `Part.cs`; shared `BossNoticeData.cs` chỉ chỉnh khi cần hỗ trợ bảng phân loại.

- [ ] Chạy audit nguồn/resource lại trên bản cuối, thêm giới hạn index avatar/icon/part và kiểm tra frame offsets. Không sửa `.meta` hoặc thay hình có sẵn chỉ vì thêm boss server.
- [ ] Dùng part/portrait đã kiểm tra ở bảng trên; không gửi ảnh theo tên chưa đăng ký trong `img_by_name`.
- [ ] So sánh packet nhân vật, skill/HP/drop với cách đọc hiện tại. Các boss là humanoid, không gửi mob template lớn/BigBoss packet nếu không có decoder tương ứng.
- [ ] Kiểm tra Unity Game1/Game2 với cache ảnh trống: cả sáu outfit và chân dung chat, di chuyển/đấm/chưởng/dịch chuyển/chói mắt, ba HP đỏ cùng lúc, item nằm nền đúng vị trí.
- [ ] Kiểm tra cả x1–x4, hai tài khoản, chuyển khu lúc chat/đánh, ngắt kết nối, người vào khu muộn và nhặt đồ ấn vào túi/mặc đủ năm món.
- [ ] Các boss mới thuộc **Boss**, không phải Boss nhiệm vụ. Tích hợp với `2026-10-08-boss-notice-categories.md`; không gắn nhiệm vụ chỉ vì dùng kỹ năng của Xên.

## Task 5: Kiểm chứng và phát hành

- [ ] Đối chiếu từng yêu cầu đã chốt và duyệt các cấu hình vận hành/loot phụ còn đề xuất trước khi triển khai.
- [ ] Test trước sửa phải thất bại đúng hành vi thiếu, sau sửa đạt; chạy suite loot/combat/group và hồi quy Whis, nhiệm vụ hỗ trợ, food, chest, events, Broly và thông báo boss.
- [ ] Build tất cả source rồi chạy lại tests trên JAR đóng gói. Không kết luận client không crash trước khi build/chạy Unity và đọc log thật.
- [ ] Tuân thủ AGENTS: bảo trì lưu dữ liệu, xác nhận JVM dừng, chuyển `SRC/20.jar` cũ vào `SRC/jar-backup` với thời gian riêng, SHA-256/metadata trước khi cài, giữ thư viện/công cụ cần dùng; kiểm tra cổng và số boss/map/khu sau restart.
- [ ] Lượt hiện tại bổ sung thiết kế vào hai kế hoạch Broly/phân loại thông báo đã gửi, chưa sửa production code, chưa spawn boss, chưa thay JAR.
