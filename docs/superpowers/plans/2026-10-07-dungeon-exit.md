# Thoát phó bản bằng tàu bay — Implementation Plan

**Trạng thái 2026-10-07:** Người dùng yêu cầu giữ nguyên phần thoát phó bản và capsule. Không triển khai kế hoạch này; nội dung phía dưới được lưu làm đề xuất cũ.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khi hết giờ hoặc hoàn thành toàn bộ phó bản, đưa người chơi còn trong lượt đó ra ngoài bằng tàu bay; capsule tự thoát phải an toàn.

**Architecture:** Dùng đường thoát chung DungeonExitService đã có. Đóng quyền vào trước, chốt thưởng một lần, rồi vận chuyển; chỉ thu hồi lượt khi không còn người chơi. Phần thưởng chưa thể vào túi phải có bản ghi bền vững trước khi đưa ra ngoài.

**Tech Stack:** Java server, MySQL/InnoDB, client Unity hiện có; kiểm thử Java theo bộ test hiện tại.

## Trạng thái và bằng chứng đọc code

- Đây là bản kế hoạch để người dùng xem trước, chưa sửa hành vi hoặc cập nhật JAR.
- `DungeonExitService.exit` kiểm tra người chơi còn ở zone nguồn, hồi sinh nếu cần, dùng `forcedDungeonShip`; có cơ chế chọn khu đích khi khu đông.
- Kho báu, Rắn độc, Khí gas có nhánh chờ khoảng 60 giây. Doanh trại có bước gặp Độc Nhãn, rải ngọc rồi cho 5 phút nhặt.
- Capsule 193 hiện trừ vật phẩm khi mở bảng chọn. Capsule 194 không trừ. `mapBeforeCapsule` có thể lưu zone phó bản; danh sách capsule chưa tự lọc toàn bộ trạng thái đóng của lượt.
- Đọc code chưa chứng minh toàn bộ đường đi hoạt động trên Unity. Cần kiểm thử thực tế trước khi kết luận đã sửa.

## Quy tắc đề xuất

1. Hết thời hạn: đánh dấu đóng ngay tại tick kiểm tra, ngừng quái/boss sinh mới, chặn vào lại, tàu bay đưa toàn bộ người còn trong lượt ra điểm thoát hiện có.
2. Hoàn thành sớm: phải là kết thúc toàn bộ chuỗi boss/quái của lượt, không phải hạ một boss trung gian. Chốt thưởng rồi tàu bay đưa ra, bỏ thời gian chờ 60 giây.
3. Doanh trại: đề xuất bỏ bước chạy nhặt ngọc 5 phút. Khi thắng, tạo lượng ngọc theo công thức `randomNR` hiện tại đúng một lần và chia lần lượt cho danh sách người chơi đủ điều kiện có mặt lúc kết thúc, sắp xếp theo player ID. Thay lời thoại Độc Nhãn để phản ánh nhận thưởng tự động. Đây là thay đổi thưởng cần người dùng xem cùng kế hoạch.
4. Đồ đang rơi có chủ: chuyển cho đúng chủ còn đủ điều kiện trong lượt; đồ chung phân phối lần lượt cho danh sách trên. Không gom đồ của người chơi tự vứt: phải đánh dấu nguồn phần thưởng phó bản lúc tạo vật phẩm. Không nhân thưởng vì gọi đóng lại nhiều lần.
5. Túi đầy: lưu thưởng chờ nhận vào InnoDB, khóa duy nhất theo lượt/phần thưởng/người nhận. Cho nhận qua NPC mở phó bản; không giữ người chơi trong phó bản vì đầy túi. Kiểm tra retry, mất kết nối và kết quả commit không rõ trước khi dùng.
6. Capsule được dùng tự ra ngoài khi luật vật phẩm cho phép. Chỉ trừ capsule thường khi chuyển map thành công; hủy bảng chọn hoặc chuyển thất bại không mất vật phẩm. Capsule đặc biệt vẫn dùng nhiều lần.
7. Người đã capsule ra ngoài không bị kéo đi lần nữa. Không cho “Về chỗ cũ” vào lượt hết giờ, đã đóng hoặc đã được cấp lại cho bang khác. Mọi đường thoát dọn timer, trạng thái đặc thù và đệ tử theo cùng hợp đồng.

## Task 1 — Khóa hợp đồng đóng và thoát

**Modify:** `SRC/src/nro/models/services/DungeonExitService.java`, `SRC/src/nro/models/map/service/ChangeMapService.java`, `SRC/src/nro/models/map/phoban/{BanDoKhoBau,SnakeWay,DestronGas,RedRibbonHQ}.java`.

**Test:** các test tại `SRC/test/nro/models/services_dungeon/`; thêm `DungeonImmediateExitTest.java`.

- [ ] Viết test thất bại cho hết giờ, thắng sớm, boss trung gian, thoát trùng, capsule ra trước và khu đích đầy.
- [ ] Chạy test để xác nhận lỗi đúng hành vi mục tiêu.
- [ ] Chuẩn hóa lý do đóng TIMEOUT/COMPLETED; giữ khóa lượt và kiểm tra zone nguồn, không giải phóng lượt còn người.
- [ ] Bỏ nhánh đợi sau khi thực sự hoàn thành, chốt thưởng trước khi gọi đường tàu bay hiện có.
- [ ] Chạy lại timeout, closing, initialization, allocation race và admission tests.
- [ ] Rà Mabu, Ngọc rồng đen, Hirudegarn, Siêu thần thủy bằng hợp đồng tương tự; chỉ áp dụng điểm kết thúc đúng từng loại, tránh cắt chuỗi nhiều giai đoạn.

## Task 2 — Thưởng trước khi rời phó bản

**Create:** `SRC/src/nro/models/services/DungeonRewardSettlementService.java`, `SRC/sql/migrations/20261007_dungeon_pending_rewards.sql`, `SRC/test/nro/models/services_dungeon/DungeonRewardSettlementTest.java`.

**Modify:** `SRC/src/nro/models/map/phoban/RedRibbonHQ.java`, `SRC/src/nro/models/npc_list/DocNhan.java`, `SRC/src/nro/models/map/ItemMap.java`, các điểm tạo đồ thưởng phó bản, NPC mở từng phó bản.

- [ ] Test số lượng ngọc giữ công thức hiện tại, đồ có chủ, đồ không có chủ, đồ tự vứt, túi đầy và gọi kết thúc hai lần.
- [ ] Thêm nhận diện nguồn/lượt vào đồ thưởng, snapshot người đủ điều kiện trước khi vận chuyển.
- [ ] Ghi thưởng theo token lượt và token phần thưởng; phát vào túi hoặc lưu chờ nhận với biên nhận tránh phát hai lần.
- [ ] Test SQL trên database riêng: rollback, offline, retry sau commit không rõ, khởi động lại và nhận lại ở NPC.
- [ ] Cập nhật lời thoại/thông báo, rồi kiểm tra không còn đồ thưởng hợp lệ bị mất khi đóng.

## Task 3 — Capsule và vào lại

**Modify:** `SRC/src/nro/models/services_func/UseItem.java`, `SRC/src/nro/models/map/service/MapService.java`, `SRC/src/nro/models/map/service/ChangeMapService.java`.

**Create:** `SRC/test/nro/models/services_dungeon/DungeonCapsuleExitTest.java`.

- [ ] Test hủy chọn map, chuyển thất bại, dùng thành công, trùng tick cưỡng chế, ngọc Namek đang mang và zone cũ đã tái cấp.
- [ ] Đưa tiêu thụ capsule 193 vào nhánh thành công dưới khóa người chơi; xác minh tương thích bảng chọn hiện tại, không thay ý nghĩa capsule 194.
- [ ] Lọc zone phó bản không hợp lệ trong danh sách và kiểm tra lại lúc thực sự chuyển map.
- [ ] Xóa tham chiếu lượt cũ khi không còn hợp lệ, dọn timer/trạng thái và chạy lại admission tests.

## Task 4 — Nghiệm thu và phát hành sau khi kế hoạch được chấp thuận

- [ ] Build Java và chạy các test liên quan, kiểm tra SQL không dùng database thật làm fixture.
- [ ] Trên Unity thử từng phó bản: hết giờ, thắng sớm, chết, đệ tử, capsule, map đích đông, ngắt mạng và đăng nhập lại; xác nhận hình tàu bay và timer biến mất.
- [ ] Kiểm tra lời thoại và thưởng Doanh trại mới với túi thường/đầy.
- [ ] Nếu cập nhật JAR: bảo trì lưu dữ liệu, xác nhận server dừng, backup `SRC/20.jar` tên có thời gian trong `SRC/jar-backup`, ghi SHA-256/metadata, cài bản mới rồi kiểm tra khởi động và log.
- [ ] Ghi báo cáo kiểm chứng và các giới hạn còn lại; không tự commit các thay đổi khác trong workspace.
