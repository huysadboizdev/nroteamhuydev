# Hỗ trợ boss nhiệm vụ 14:00–17:00 — Implementation Plan

**Trạng thái 2026-10-07:** Người dùng đã duyệt triển khai phần hỗ trợ nhiệm vụ; phần thoát phó bản/capsule giữ nguyên. Code và ba bộ kiểm thử mục tiêu đã hoàn tất, qua rà soát yêu cầu và chất lượng; đã cài JAR, server chạy lại trên cổng 14445; 226 nhóm kiểm thử trên JAR đã cài qua. Xem `docs/quest-support-deployment-20261007.md`.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tăng số chuỗi boss và giảm thời gian hồi ở map thường từ Tiểu đội sát thủ Nappa đến hết Sên thị trấn trong giờ hỗ trợ hàng ngày.

**Architecture:** Một service quản lý lịch và các chuỗi boss bổ sung có ID thực thể riêng, dùng lại hình ảnh và chiến đấu hiện có. TaskService nhận diện loại nhiệm vụ của boss hỗ trợ qua bảng ánh xạ riêng; boss gốc giữ nhận diện hiện tại. Tách cấu hình lịch/số lượng/hồi khỏi dữ liệu boss gốc.

**Tech Stack:** Java, java.time với Asia/Bangkok (UTC+7), server scheduler/BossManager hiện có, Unity hiện có.

## Phạm vi đã xác nhận

- Người dùng chọn tăng số lượng và giảm hồi boss ở map thường; không dùng khu riêng hoặc NPC bỏ qua nhiệm vụ.
- Khoảng giờ hằng ngày: 14:00 <= giờ hiện tại < 17:00, theo UTC+7, không lệ thuộc timezone hệ điều hành.
- Chuỗi nhiệm vụ chính 20–26. Nhiệm vụ 19 Kuku/Mập Đầu Đinh/Rambo, nhiệm vụ 27 võ đài Sên và 28 Mabu nằm ngoài phạm vi này.
- Code hiện tính nhiều nhiệm vụ bằng `boss.id`, riêng Fide và Xên còn dựa `currentLevel`. Không được tạo bản sao dùng trùng ID mạng hoặc chỉ tăng số gọi constructor rồi cho rằng nhiệm vụ sẽ tự đúng.

| Nhiệm vụ | Boss được hỗ trợ |
|---|---|
| 20 | Số 4 → Số 3 → Số 2 → Số 1 → Tiểu đội trưởng tại Nappa |
| 21 | Fide đủ 3 dạng |
| 22 | Các bước cốt truyện và đánh quái giữ nguyên, không có boss mới để tăng |
| 23 | Android 19, Dr. Kore/Android 20 |
| 24 | Android 15 → 14 → 13 |
| 25 | Póc → Píc → King Kong |
| 26 | Xên Bọ Hung 3 dạng tại thị trấn Ginder |

## Cấu hình khởi điểm đề xuất để người dùng xem

- Trong giờ hỗ trợ: tổng 3 chuỗi độc lập cho mỗi nhóm (1 chuỗi gốc và 2 chuỗi bổ sung); giới hạn tính theo cả nhóm, không tạo 3 bản cho từng thành viên rồi nhân số lượng tiếp.
- Hồi chuỗi sau khi kết thúc: 120 giây. Giữ thời gian chuyển dạng, gọi thành viên và thứ tự chiến đấu đúng boss gốc.
- Phân tán chuỗi bổ sung sang các khu hợp lệ của chính map gốc, ưu tiên khu có người đang ở bước nhiệm vụ tương ứng; tránh dồn cả 3 chuỗi vào một khu.
- Giữ HP/sức đánh, yêu cầu sức mạnh, NPC cốt truyện, số quái và điều kiện nhiệm vụ hiện có.
- Boss bổ sung cho tiến độ nhiệm vụ đúng bước; đề xuất không rơi đồ thương mại hoặc cộng điểm sự kiện/bảng xếp hạng để giờ hỗ trợ không biến thành nguồn farm thưởng. Boss gốc giữ thưởng hiện tại.
- 17:00 dừng tạo/hồi boss bổ sung, khôi phục hồi boss gốc. Boss bổ sung đang giao chiến được tối đa 5 phút để kết thúc, sau đó thu hồi có thông báo. Không cưỡng chế người chơi rời map thường.
- Khởi động server lúc 15:00 phải tự mở hỗ trợ; restart không tạo thừa chuỗi. Thông báo một lần lúc mở/đóng, không spam theo tick.

## Task 1 — Lịch và cấu hình

**Create:** `SRC/src/nro/models/services/QuestSupportSchedule.java`, `SRC/src/nro/models/services/QuestSupportConfig.java`, `SRC/test/nro/models/services/QuestSupportScheduleTest.java`.

- [x] Viết test cho 13:59:59, 14:00, 16:59:59, 17:00, đổi ngày, restart giữa giờ và timezone hệ điều hành khác.
- [x] Chạy test thấy thất bại rồi triển khai lịch với Clock truyền được vào test và timezone Asia/Bangkok.
- [x] Cấu hình bật/tắt, 3 chuỗi, hồi 120 giây, thời gian kết thúc trận 300 giây; kiểm tra số âm, 0 hoặc vượt giới hạn bảo vệ.
- [x] Chạy lại test lịch và xác nhận mở/đóng chỉ xảy ra một lần mỗi phiên giờ hỗ trợ.

## Task 2 — Danh tính boss và tiến độ nhiệm vụ

**Create:** `SRC/src/nro/models/boss/QuestSupportIdentity.java`, `SRC/test/nro/models/services/QuestSupportProgressTest.java`.

**Modify:** `SRC/src/nro/models/boss/Boss.java`, `SRC/src/nro/models/services/TaskService.java`, các boss trong `tieu_doi_sat_thu`, `Frieza/Fide.java`, `Android`, `Cell/XenBoHung.java`.

- [x] Test ID thực thể khác nhau, ánh xạ đúng task 20–26, sai bước không cộng, không bỏ qua dạng Fide/Xên và không cộng hai lần cho một lần chết.
- [x] Tách loại boss nhiệm vụ khỏi ID mạng của bản hỗ trợ; ánh xạ có danh sách cho phép rõ ràng, không suy luận từ tên hiển thị.
- [x] Giữ nguyên đường doneTask và các điều kiện đang áp dụng; không sửa trực tiếp index nhiệm vụ.
- [x] Tách phần tiến độ khỏi thưởng phụ của boss bổ sung, kiểm tra không lọt drop/event/badge/rank ngoài chủ đích.
- [x] Chạy lại test boss thường để đảm bảo nhận diện và thưởng gốc vẫn đúng.

## Task 3 — Quản lý các chuỗi bổ sung

**Create:** `SRC/src/nro/models/services/QuestSupportService.java`, `SRC/test/nro/models/services/QuestSupportLifecycleTest.java`.

**Modify:** `SRC/src/nro/models/boss/Boss_Manager/BossManager.java`, `SRC/src/nro/models/server/ServerManager.java` (xác định điểm tick/shutdown đang dùng trước khi sửa), constructor/quan hệ cha-con của các nhóm boss thuộc phạm vi.

- [x] Test đúng 2 chuỗi bổ sung mỗi nhóm, không tăng dần theo tick, quan hệ cha-con không lẫn với chuỗi gốc, không spawn vào map/khu thiếu dữ liệu.
- [x] Tạo tập thực thể riêng cho mỗi chuỗi; cấp ID và đăng ký với đúng manager, có giới hạn toàn cục và snapshot an toàn khi đọc danh sách.
- [x] Áp dụng hồi 120 giây tại nghỉ giữa chuỗi, không rút ngắn chuyển dạng hoặc phá thời gian gọi thành viên.
- [x] Chọn khu hợp lệ có nhu cầu; không tạo khu mới và không điều chuyển người chơi tự động.
- [x] Xử lý mở giờ, đóng giờ, trận đang đánh, restart và shutdown; giải phóng boss/tham chiếu sau thu hồi.
- [x] Test tick đồng thời, đóng giờ trong lúc boss chết và gọi reward sau thu hồi.

## Task 4 — Nghiệm thu và triển khai

- [x] Build Java, chạy schedule/progress/lifecycle và regression boss/task.
- [ ] Trên Unity thử từng chuỗi với tài khoản đúng bước và sai bước; kiểm tra đủ dạng, đủ thứ tự, kill credit và số lượng boss trên map.
- [ ] Đo số boss, tick time và bộ nhớ trong 3 giờ; xác nhận không tích lũy boss qua ngày hoặc restart.
- [x] Kiểm tra mở 14:00, đóng 17:00 và trận được kết thúc tới 17:05 theo đề xuất; ghi rõ đây là ngoại lệ cho trận đang diễn ra.
- [x] Chỉ phát hành sau khi người dùng xem kế hoạch. Khi thay JAR tuân thủ bảo trì, backup có thời gian và SHA-256 theo AGENTS.md.

## Giới hạn kiểm tra hiện tại

Đã triển khai thông số được duyệt: 3 chuỗi/nhóm, hồi 120 giây và thời gian kết thúc trận 300 giây. Kiểm thử sử dụng đồng hồ mô phỏng cho cả 3 giờ; chưa thay thế phép đo bộ nhớ/tick trên server thật trong 3 giờ hoặc thử gameplay trực tiếp trên Unity. Các trường hợp nhận diện nhiệm vụ, chuyển dạng/gọi thành viên, chọn khu thật và đua giữa vào map/thu hồi được kiểm tra tự động.
