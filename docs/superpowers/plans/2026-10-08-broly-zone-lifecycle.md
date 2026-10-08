# Broly và Super Broly trong cùng khu — Implementation Plan

**Trạng thái 08/10/2026:** đã triển khai, kiểm thử và đưa server vào chạy. Kết quả cuối, giới hạn kiểm chứng và metadata khôi phục: [báo cáo phát hành](2026-10-08-hunting-release-verification.md). Checklist bên dưới là kế hoạch gốc; báo cáo phát hành phân biệt các kiểm thử tự động đã qua với kiểm thử thủ công chưa chạy.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Mỗi khu có tối đa một Broly hoặc Super Broly; Broly chuyển dạng thay thế đúng tại khu/vị trí đang đứng, không sinh Super do một lần rời map thông thường.

**Architecture:** Giữ hai lớp boss hiện tại, bổ sung bộ điều phối vị trí trong `BrolyManager` và trạng thái chuyển dạng rõ ràng. Đặt chỗ cho cả lượt Broly → Super để không có khoảng trống mà một Broly khác chen vào. Chỉ công bố boss mới sau khi đã khởi tạo đủ và được đưa vào khu thành công.

**Tech Stack:** Java 17, server NRO, packet chuyển map hiện có, kiểm thử Java dạng main.

## Kết quả kiểm tra nguồn

- `Broly.active()` dùng `hpMax == 1_500_000`; `tangChiSo()` tăng theo tỷ lệ ngẫu nhiên nên có thể vượt mốc này.
- `Broly.leaveMap()` luôn gọi `new SuperBroly(zone,x,y)`, không phân biệt chết/chuyển dạng/lỗi vào map/thu hồi. Exception bị nuốt.
- `Broly.joinMap2()` kiểm tra khu rồi vào khu ở hai bước; không có đặt chỗ chung với Super. Khi không còn khu trống có nhánh chuyển trạng thái DIE, dẫn tới `leaveMap()` và tạo Super ngoài điều kiện chuyển dạng.
- `SuperBroly.joinMap()` vào khu được truyền trực tiếp; nếu khu null lại gọi cách spawn chung của `Boss`. Chưa xác nhận độc quyền Broly/Super trong khu.
- `event_list/Default.java` tạo 30 Broly. Thời gian nghỉ Broly hiện tại 600 giây; Super có cơ chế rời khi khu không có người khoảng 5 phút. Giữ các lịch này.
- Ảnh người dùng cho thấy Broly 49 và Super Broly 51 ở Đồng Nam Guru, khu 9. Nguồn có các đường gây trùng khu nêu trên; chưa thực hiện test tái hiện đồng thời hay sửa runtime trong lượt lập kế hoạch.

## Quy tắc được đề xuất

1. Khu có Super thì Broly mới chọn khu khác hoặc chờ lượt sau; khu có Broly cũng không nhận thêm Broly/Super độc lập.
2. Broly đang sống, ở trạng thái chiến đấu hợp lệ và HP tối đa đạt **từ 1.500.000 trở lên** thì chuyển dạng đúng một lần.
3. Giữ map, khu, tọa độ x/y; gửi xóa Broly cũ rồi thêm Super. Sau chuyển dạng, danh sách nhân vật và thông báo hiện tại không còn ghi Broly cũ là đang sống.
4. Broly bị hạ trước mốc chuyển dạng thì xử lý chết bình thường, không sinh Super. Rời vì lỗi, bảo trì, thu hồi hoặc khu không hợp lệ cũng không sinh Super.
5. Chuẩn bị Super thất bại: ghi lỗi có ngữ cảnh, giữ/khôi phục Broly hợp lệ và đặt chỗ; không bỏ mất boss hoặc để lặp vô hạn không có log.
6. Broly không nghỉ rồi spawn lại vào chính khu đang được Super giữ. Super chết/rời/dispose mới giải phóng chỗ của lượt đó; không giải phóng chỗ của một boss khác do callback cũ.

## Các cách thực hiện

- **Khuyến nghị:** giữ hai lớp, điều phối đặt chỗ và chuyển dạng qua `BrolyManager`. Ít thay đổi kỹ năng/phần thưởng hiện có.
- Gộp hai dạng vào một boss nhiều cấp: tự nhiên hơn về vòng đời, nhưng phải chuyển các kỹ năng, thưởng pet và xử lý Super hiện tại sang cấu trúc mới; phạm vi lớn hơn.
- Chỉ thêm kiểm tra `getBosses()` trước spawn không đủ: hai luồng có thể cùng nhìn thấy khu trống và vẫn vào chung.

## Task 1: Kiểm thử tái hiện

**Files:** Create `SRC/test/nro/models/services/BrolyLifecycleTest.java`.

- [ ] Viết fixture giữ số lần tạo Super, số lần vào/rời khu, callback thất bại và trạng thái đặt chỗ; không phụ thuộc ngẫu nhiên thật.
- [ ] Chạy trên JAR đang dùng, xác nhận các ca thất bại đúng nguyên nhân: HP vượt mốc, leave vì chết/lỗi cũng tạo Super, tranh chấp cùng khu.
- [ ] Thêm ca HP dưới mốc không chuyển; đạt đúng mốc chuyển; hai callback cùng kích chỉ tạo một Super; khu ít hơn ba khu không gây random sai khoảng.

## Task 2: Đặt chỗ trong khu

**Files:** Modify `SRC/src/nro/models/boss/Boss_Manager/BrolyManager.java`; Create `SRC/src/nro/models/boss/Broly/BrolyZoneCoordinator.java`.

- [ ] Đặt chỗ theo đối tượng `Zone`, owner và generation lượt; chỗ gồm đang xuất hiện, đang chuyển dạng hoặc đã có Super. Dùng cơ chế đồng bộ chung cho mọi đường vào của hai dạng.
- [ ] Chọn từ khu 2 trở lên như hiện tại; không có khu phù hợp thì REST/chờ, không đi qua DIE và không tự tạo Super.
- [ ] Giữ nguyên các hạn chế khu đã có boss khác trong bước spawn thường; đây không phải lần thay đổi quy tắc boss khác cùng khu.
- [ ] Không giữ khóa điều phối khi gửi packet, tạo pet, ghi SQL hoặc gọi callback có thể khóa boss/người chơi. Đặt chỗ dưới khóa ngắn, thực hiện bước ngoài khóa rồi xác nhận/hoàn tác theo generation.

## Task 3: Chuyển dạng và thu hồi

**Files:** Modify `SRC/src/nro/models/boss/Broly/Broly.java`, `SRC/src/nro/models/boss/Broly/SuperBroly.java`.

- [ ] Tách `transformToSuper()` khỏi `leaveMap()`; leave chỉ thu hồi boss và cập nhật vòng đời phù hợp.
- [ ] Điều kiện chuyển dạng dùng `>=`, boss còn sống và trạng thái chưa chuyển; xử lý thứ tự với đòn đánh gây chết dưới khóa boss.
- [ ] Chuẩn bị Super đầy đủ trước khi nó được manager update hoặc thông báo xuất hiện. Constructor hiện đăng ký qua `Boss` trước khi gán zone/tọa độ; cần kiểm soát xuất bản đối tượng, không để manager thấy đối tượng khởi tạo dở.
- [ ] Bàn giao chỗ Broly → Super mà không giải phóng giữa hai bước; đảm bảo client nhận remove/add theo thứ tự, không có hai boss trong danh sách khu ở trạng thái hoàn tất.
- [ ] Giữ phần thưởng pet và kỹ năng Super hiện tại. Thu hồi cả pet khi Super rời; callback dispose/lỗi không làm rò đối tượng trong manager.
- [ ] Nếu có trùng Broly/Super từ phiên cũ khi triển khai: kiểm kê từng khu; ưu tiên Super hợp lệ, thu hồi Broly dư không thưởng và không tạo thêm Super; ghi log quyết định. Không dùng tên boss làm khóa.

## Task 4: Thông báo và xác minh

**Files:** Existing `BossNoticeService`, `BossNoticeTracker`; test `BrolyLifecycleTest`, `BossFullLifecycleTest`, `BossNoticeTrackerTest`.

- [ ] Lượt Broly chuyển dạng kết thúc bằng trạng thái chuyển/rời, không ghi bị người chơi hạ. Super có lượt spawn riêng; không giữ Broly cũ trong danh sách đang sống.
- [ ] Test lỗi tạo Super, lỗi vào map, thu hồi/bảo trì, Super chết, Super nghỉ, Broly respawn trong khi Super còn sống, gọi lặp callback và tranh chấp đặt chỗ.
- [ ] Biên dịch toàn bộ nguồn; chạy bộ hồi quy hiện có và test mới trên source lẫn JAR đóng gói.
- [ ] Kiểm tra Unity: kích vượt mốc, thấy remove/add đúng vị trí, chỉ một dạng trong danh sách khu; người vào khu muộn cũng nhận đúng dạng. Kiểm tra không nhận thưởng hai lần.
- [ ] Triển khai sau khi kế hoạch được duyệt: bảo trì lưu dữ liệu, xác nhận server dừng, backup `SRC/20.jar` với thời gian riêng, đối chiếu SHA-256 và metadata, cài rồi kiểm tra cổng/log và boss theo khu. Không commit hoặc thay JAR trong giai đoạn lập kế hoạch.
