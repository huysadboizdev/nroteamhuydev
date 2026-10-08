# Hạn lượt boss và bỏ thông tin khu — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use executing-plans khi người dùng duyệt triển khai. Checklist này là đề xuất, chưa sửa code ứng dụng hoặc thay JAR.

**Goal:** Bỏ thông tin khu khỏi thông báo boss và bảo đảm lượt boss kết thúc đúng hạn dù chưa giết hoặc còn sót thành viên.

**Architecture:** Mỗi lượt có một chủ sở hữu, danh sách thành viên và hạn kết thúc chung. Đóng lượt trước khi thu hồi; chỉ bắt đầu thời gian chờ hồi khi toàn bộ thành viên cũ đã được dọn. Phân biệt thu hồi với tiêu diệt để không phát thưởng hoặc gọi dạng tiếp theo khi hết giờ.

**Tech Stack:** Java 17, SQL lịch sử boss, Unity Game1/Game2, kiểm thử Java main và Unity Editor hiện có.

**Trạng thái:** đề xuất để người dùng xem trước ngày 08/10/2026, giờ Bangkok. Chưa triển khai các mốc mới.

## 1. Kết quả kiểm tra hiện tại

Đã rà mã lớp boss và phương thức `autoLeaveMap`/`rest`, lưu bằng chứng tại `.analysis/boss-turn-audit-20261008.json` và `.analysis/boss-data-rest-audit-20261008.json`. Đây là kiểm tra nguồn; các giá trị hồi bên dưới là cấu hình gốc, có thể được giờ hỗ trợ nhiệm vụ thay thế.

| Boss/nhóm | Nếu không giết hoặc còn sót | Chờ hồi hiện tại |
|---|---|---|
| Basil/Lavender/Bergamo; Pi Láp/Mai/Su | Không có hạn tổng. Có người trong khu thì con cuối có thể tồn tại mãi. Khu vắng liên tục 5 phút thì thu hồi cả nhóm. | 10 phút sau khi hạ hết hoặc thu hồi nhóm. Vắng từ đầu: khoảng 5 + 10 = 15 phút tới lượt mới, chưa kể thoại/tick/chờ khu. |
| Black Goku/Cumber | Mốc ban đầu 10–15 phút; có người thì cập nhật lại mốc vắng 5–15 phút. Không phải thời hạn cố định của lượt. | Cấu hình gốc 5 phút sau khi kết thúc toàn bộ dạng. |
| Tiểu đội sát thủ, Fide, Android 19/Kore, Pic/Poc/King Kong, Xên con, Xên võ đài, Cooler, Bojack… | Nhiều lớp có mốc 15 phút nhưng đặt lại thời điểm khi có người trong khu; có thể bị giữ lâu. Thành viên/dạng chưa kết thúc khiến cách hồi phụ thuộc từng chuỗi. | Tiểu đội: 5 phút; Fide/Kore/Android 14/King Kong: 10 phút; Cooler/Xên: 30 phút; Bojack: 15 phút. Boss phụ được gọi theo nhóm, không hồi độc lập theo số 0 trong BossData. |
| Kuku/Mập đầu đinh/Rambo | 15 phút từ lần vào map; đoạn gia hạn theo người chơi đã được comment. | 10 phút sau rời/chết. |
| Broly thường; Mặt Trời/Virut/Ở Dơ/Ăn Trộm | Không có hạn tổng qua `Boss.autoLeaveMap` mặc định (hàm rỗng). | Broly và Mặt Trời cấu hình 10 phút; các mini khác theo cấu hình riêng. Broly chuyển Super là chuyển dạng của lượt đang có. |
| Super Broly | Khu vắng 5 phút thì rời; có người thì gia hạn. | Thu hồi Super và trả khu cho chu kỳ Broly; không tạo thêm Super độc lập. |
| Chuỗi hỗ trợ nhiệm vụ 14:00–17:00 | Chờ đủ các thành viên hoàn tất mới tính hồi. Còn một con có thể giữ chuỗi trong giờ mở. Khi đóng giờ, chuỗi đã giao chiến được thêm tối đa 5 phút. | Mặc định 120 giây khi hoàn tất và còn trong giờ hỗ trợ. |

Các boss theo sự kiện, phó bản, thử thách cá nhân và diễn cảnh có luật giờ/thoát riêng. Không áp dụng một mốc boss map thường cho toàn bộ các lớp đó. Ví dụ Golden Frieza đóng theo cửa sổ giờ; Death Beam là diễn cảnh ngắn; Goku/Cadic/Drabura có mốc diễn cảnh; Noel/Lân/Sói Hec Quyn có các timer riêng.

## 2. Phương án đề xuất

Ba cách khả thi: chỉ dùng timer khu vắng (hiện tại, có thể bị giữ mãi); thêm hạn tối đa và giữ thu hồi khi vắng (đề xuất); hoặc chạy theo lịch giờ cố định toàn server (khó giữ công bằng cho người vào muộn và đổi nhịp săn đồ).

Chọn **hạn tối đa chung cho từng lượt**, kèm timer vắng hiện có:

| Phạm vi | Hạn tối đa đề xuất | Mốc tính |
|---|---|---|
| Hai bộ ba mới | 20 phút | Từ khi cả ba nói xong, cùng mở chiến đấu. |
| Boss săn map thường: Black Goku/Cumber, Cooler/Bojack, mini, Broly/Super… | 20 phút | Từ khi lượt đầu vào map thành công; chuyển dạng và gọi boss phụ giữ hạn ban đầu. |
| Boss nhiệm vụ map thường và chuỗi hỗ trợ | 15 phút | Từ khi chuỗi vào map thành công, bao gồm các thành viên/dạng tiếp theo. |
| Boss có phiên giờ/sự kiện/phó bản/thử thách/diễn cảnh | Giữ luật phiên riêng | Hạn mới chỉ được dùng sau khi đối chiếu từng loại; thời điểm đóng phiên luôn có quyền kết thúc lượt. |

- Người đứng trong khu, đánh boss, chuyển khu rồi quay lại, giết một thành viên hoặc chuyển dạng đều **không gia hạn hạn tổng**.
- Vắng khu 5 phút ở bộ ba/Super vẫn có thể thu hồi sớm. Timer vắng của boss cũ được giữ theo từng loại; hạn tổng luôn là giới hạn trên.
- Còn một con lúc hết hạn: đóng lượt, thu hồi con còn lại và mọi thành viên phụ thuộc. Không chờ thêm 20 phút cho con cuối.
- Giết hết trước hạn: đóng ngay, bắt đầu thời gian hồi ngay sau khi dọn xong.
- Giữ cooldown gốc của boss cũ. Hai bộ ba giữ 10 phút. Chuỗi hỗ trợ giữ cấu hình 120 giây và chỉ hồi khi cửa sổ 14:00–17:00 còn mở.
- Với hỗ trợ nhiệm vụ, hạn thực tế không vượt quy tắc đóng giờ hiện có: chuỗi đã đánh được tới tối đa 17:05, nếu hạn 15 phút của lượt kết thúc trước thì thu hồi trước.
- Hết cooldown nhưng thiếu map/khu hợp lệ: giữ trạng thái chờ, kiểm tra lại sau 10 giây; không sinh đè boss hoặc khởi tạo lượt dở dang. Thời gian hồi công bố là mốc sớm nhất có thể xuất hiện.

**Ví dụ bộ ba mở chiến đấu lúc 14:00:**

| Tình huống | Đóng lượt | Sớm nhất có lượt mới |
|---|---|---|
| Hạ hết lúc 14:08 | 14:08 | 14:18 |
| Giết hai con, còn một con và vẫn có người | 14:20 | 14:30 |
| Không giết con nào, vẫn có người | 14:20 | 14:30 |
| Từ 14:03 khu vắng liên tục | 14:08 | 14:18 |

## 3. Bỏ khu khỏi thông báo

Trong ảnh, dòng `Khu: 9` do `BossNoticeEntry.Details()` sinh ra. Thông báo chạy toàn server của bộ ba hiện đã chỉ chứa tên nhóm và bản đồ.

- Bỏ dòng khu ở chi tiết **Boss** và **Boss nhiệm vụ**, cả hiện tại/lịch sử, cả Game1/Game2. Giữ tên boss, bản đồ, trạng thái, thời điểm xuất hiện, người hạ và thời gian hồi xác định được.
- Client không hiển thị khu ngay cả khi nhận bản ghi cũ còn chứa zone. BOSS2 vẫn giữ cấu trúc packet nhưng server gửi `zone=-1` cho thông báo; BOSS1 giữ nguyên định dạng.
- Server vẫn giữ khu trong dữ liệu nội bộ để phân biệt lượt, quản lý spawn và dọn đúng boss. Không sửa hệ thống chọn khu của người chơi hoặc danh sách nhân vật trong khu.
- Chỉ thông báo tên nhóm + bản đồ khi xuất hiện; không thêm số khu vào nội dung broadcast.

## 4. Đóng lượt an toàn và xử lý thưởng

- Lượt có ID/thế hệ riêng, trạng thái `ACTIVE → CLOSING → COOLDOWN`; đóng admission/combat trước thao tác thu hồi.
- Đóng lượt tối đa một lần. Đòn đến sau hạn không tạo chết/thưởng; xử lý tranh chấp đòn cuối và timeout bằng cùng khóa của lượt/boss, kiểm tra thời gian tại lúc nhận đòn.
- Thu hồi không gọi đường thưởng/ghi nhiệm vụ/biến hình/gọi đồng đội. Dọn hiệu ứng trễ của boss, đệ tử/phụ thuộc theo ownership; không dọn boss của lượt khác chỉ vì cùng map.
- Con đã bị hạ giữ thưởng và tiến độ nhiệm vụ đã nhận. Không phát thưởng con bị thu hồi. Đồ đã rơi trên đất giữ thời gian tồn tại bình thường; không xóa khi nhóm hết giờ.
- Cập nhật bảng thông báo: boss còn sống bị thu hồi ghi `DEPARTED`, không giả thành `DEFEATED` và không gán người hạ. Con đã bị hạ giữ lịch sử người hạ.
- Chỉ giải phóng khu và bắt đầu cooldown khi thành viên cũ đã rời hẳn zone/manager phù hợp. Nếu cleanup lỗi, giữ tham chiếu và khóa khu, retry; không ghi đè bằng lượt mới.
- Callback/timer của thế hệ cũ không được khởi động lại nhóm cũ, dọn nhóm mới hoặc thưởng thêm. Boss phụ chờ chủ nhóm; bản cooldown không dựa trên thời điểm chết riêng của chủ khi còn con sống.
- Dùng thời gian trôi đều để đo thời hạn; giờ thực dùng cho hiển thị và lịch mở cửa sổ. Sau restart tạo lượt mới, lịch sử lượt trước không hiện như boss đang sống của server mới.

## 5. Công việc triển khai sau duyệt

### Task 1: Cô lập và kiểm thử bỏ thông tin khu

**Files:** client `Assets/Scripts/BossNoticeData.cs`, `Assets/Editor/BossNoticeCategoryTests.cs`, `Tests/BossNoticeBoardTests.cs`; server `SRC/src/nro/models/services/BossNoticeService.java`, `SRC/test/nro/models/services/BossNoticeWireTest.java`.

- [ ] Viết RED: zone dương trong dữ liệu cũ/mới không được tạo dòng `Khu:`; dữ liệu khác và cả hai tab giữ đúng; BOSS2 gửi -1, BOSS1 byte-compatible.
- [ ] Sửa shared details và trường zone khi gửi packet; không xóa zone nội bộ.
- [ ] Chạy model/wire/board tests và build Unity.

### Task 2: Thời hạn hai bộ ba

**Files:** `SRC/src/nro/models/boss/HuntingTrio/HuntingTrioLifecycle.java`, `HuntingTrioBoss.java`, `HuntingTrioService.java`; tests lifecycle/combat/cleanup/entry hiện có.

- [ ] RED: chưa đánh/giết hai còn một/có người AFK vẫn đóng ở phút 20; thoại không ăn mất thời gian săn; hạ hết sớm hồi đúng sau 10 phút.
- [ ] Thêm hạn ACTIVE cố định; death/timeout/empty cùng đường đóng lượt; con bị thu hồi không có thưởng.
- [ ] Kiểm thử quay lại khu, đòn cuối sát deadline, callback cũ, lỗi cleanup, đủ sáu boss sau real map admission, không tái tạo năng lượng.

### Task 3: Boss cũ và nhóm nhiều thành viên/dạng

**Files:** create `SRC/src/nro/models/boss/BossTurnPolicy.java`, `BossTurnTracker.java`, `SRC/src/nro/models/services/BossTurnService.java`; modify `Boss.java`, `Boss_Manager/BossManager.java`, Broly/Super và hook trong `QuestSupportService.java` khi cần. Tests create `SRC/test/nro/models/services/BossTurnServiceTest.java`.

- [ ] Lập allowlist boss map thường và nhiệm vụ; nhóm theo chủ thực tế, kể cả thành viên chưa được gọi. Đối chiếu trường `BossData.typeAppear`/form/companion; không suy đoán theo tên.
- [ ] RED: một thành viên còn sống, chủ bị hạ sớm, dạng tiếp theo, Broly lên Super, child callback cũ và chuỗi hỗ trợ đang mở/đã đóng.
- [ ] Thêm thời hạn 20/15 phút theo policy, hook vào real admission và tick; không dùng `leaveMap()` chung để vô tình gọi dạng tiếp khi timeout.
- [ ] Giữ cooldown của từng root, giờ hỗ trợ 120 giây và giới hạn 17:05. Chỉ root được bắt đầu lượt sau khi cả nhóm cũ đóng.
- [ ] Kiểm thử lối vào map thật, đệ tử Super, lỗi thoát/packet và thời gian hiển thị trùng quyết định hồi của server.

### Task 4: Boss đặc biệt và kiểm chứng phát hành

- [ ] Kiểm tra riêng boss sự kiện, võ đài/phó bản, diễn cảnh, summon và boss thử thách. Giữ lịch phiên và phạm vi quyền sở hữu; không bật lại sự kiện đã tắt.
- [ ] Mọi fixture dùng thông báo memory/temp outbox và DAO SQL bị khóa. SQL tests dùng schema tạm. Không tạo bản ghi thử trong lịch sử production.
- [ ] Chạy suite server trên source rồi JAR, regression Broly/Whis/events/nhiệm vụ/loot, model/wire và build Unity cả hai tab.
- [ ] Probe read-only thời hạn/nhóm thực tế; kiểm tra bảng hiện tại/lịch sử và thông báo không có khu. Thử các mốc bằng clock kiểm thử, không bắt người dùng chờ 20 phút để xác nhận logic.
- [ ] Nếu triển khai: bảo trì lưu dữ liệu, xác nhận JVM dừng, backup JAR tên thời gian riêng, snapshot SQL nếu cần, SHA-256 và metadata rollback, chỉ giữ `SRC/20.jar` ở thư mục chạy. Restart và kiểm tra cổng/log/nhóm boss.

Không cần migration để chỉ bỏ dòng khu hoặc thêm thời hạn trong bộ nhớ. Nếu cần lưu thêm thời điểm kết thúc vào SQL phải viết migration bổ sung và kiểm thử lặp trước triển khai; không đổi bảng người chơi.
