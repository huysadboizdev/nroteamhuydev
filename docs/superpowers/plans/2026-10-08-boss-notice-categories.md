# Tách Boss nhiệm vụ và Boss trong client — Implementation Plan

**Trạng thái 08/10/2026:** đã triển khai server và cả hai client, kiểm thử và dựng Unity thành công. Kết quả cuối: [báo cáo phát hành](2026-10-08-hunting-release-verification.md). Checklist bên dưới giữ nội dung kế hoạch gốc.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Người chơi mở Thông báo để xem riêng Boss nhiệm vụ và Boss; hai nhóm không trùng, có hiện tại/lịch sử và dữ liệu được phân loại từ server.

**Architecture:** Một danh mục phân loại theo ý nghĩa boss, không theo tên hay ID mạng của boss hỗ trợ. Phân loại được ghi vào lượt spawn, outbox và SQL để giữ đúng lịch sử. Client Game1/Game2 có hai mục, mỗi mục giữ bộ lọc hiện tại/lịch sử; dùng capability và packet version mới để tránh làm client cũ đọc sai.

**Tech Stack:** Java 17, MariaDB/MySQL, Unity 2022.3 C#, giao thức boss notice hiện có `-58`, bảng `boss_notice_history`.

## Kết quả kiểm tra

- Hai client `Game1/BossNoticeBoard.cs` và `Game2/BossNoticeBoard.cs` đang có Boss hiện tại/Lịch sử boss lẫn vào trang thông báo; chưa có bộ lọc nhiệm vụ.
- `BossNoticeService.request` chỉ chấp nhận kind 0/1. Packet chưa có category; `BossNoticeRow`, DAO và outbox cũng chưa lưu category.
- `Boss.noticePublic()` loại `questSupport` ngay đầu hàm: boss hỗ trợ 14:00–17:00 hiện không xuất hiện trên bảng này. Không nên chỉ bỏ điều kiện này để phát thông báo xuất hiện toàn server, vì còn ảnh hưởng cơ chế chống spam hỗ trợ.
- `TaskService.checkDoneTaskKillBoss` xác định boss được tính nhiệm vụ qua `questBossId()`, gồm nhiều boss hơn danh sách ROOT_IDS của giờ hỗ trợ.
- Ba test hiện có đã chạy đạt trên JAR hiện tại: `BossNoticeTrackerTest`, `BossNoticeWireTest`, `BossNoticePolicyTest`. Chúng xác minh hành vi cũ, chưa chứng minh tính năng phân loại mới.

## Thiết kế màn hình

Trong Thông báo có ba mục: **Boss nhiệm vụ**, **Boss**, **Thông báo hệ thống**. Mỗi mục boss có lựa chọn **Hiện tại / Lịch sử**, làm mới và chuyển trang.

- Boss nhiệm vụ: toàn bộ boss thực sự được tính nhiệm vụ chính, gồm boss thường và bản hỗ trợ. Hiển thị tên/dạng, map, khu với boss ở map thường, trạng thái và thời gian xuất hiện. Có nhãn Hỗ trợ 14:00–17:00 cho bản hỗ trợ; không cần người chơi ở đúng nhiệm vụ mới được mở xem.
- Boss: các boss còn lại, gồm Broly/Super, mini boss, boss sự kiện đang mở và boss săn thưởng. Loại nhóm nhiệm vụ ở server trước phân trang.
- Danh sách hiện tại chỉ gồm lượt đang sống/đang xuất hiện hợp lệ. Lịch sử có bị hạ/rời/chuyển dạng; không trình bày lượt server trước là đang sống.
- Boss phó bản, luyện tập riêng, boss triệu hồi cá nhân không công khai tọa độ/khu cho toàn server. Muốn đưa vào mục Boss, phải lọc theo lượt/nhân vật được quyền xem; không bỏ các chặn nội dung riêng hiện có chỉ để đủ danh sách.
- Chọn hiển thị khu cho boss nhiệm vụ map thường vì người chơi cần tìm để làm nhiệm vụ. Đây là bổ sung có chủ đích: dữ liệu hiện tại không chứa khu. Không tự dịch chuyển tới boss.

## Danh mục nhiệm vụ

Lấy từ các nhánh thực tế trong `TaskService.checkDoneTaskKillBoss`, không chỉ các root của hỗ trợ: Kuku, Mập đầu đinh, Rambo; Số 4/3/2/1 và Tiểu đội trưởng; Fide các dạng; Android 19 và Dr.Kore; Android 15/14/13; Póc/Píc/King Kong; Xên bọ hung các dạng, Xên con và Siêu bọ hung được tính nhiệm vụ; Drabura/Bui Bui/Ya Con/Mabu 12h theo các nhánh nhiệm vụ đã có.

Boss có phần thưởng săn và đồng thời cộng nhiệm vụ vẫn thuộc nhóm nhiệm vụ để đáp ứng yêu cầu không xuất hiện trùng. Dạng/phiên bản cùng tên không cộng nhiệm vụ không được gộp theo chuỗi tên. Các boss nhiệm vụ ở map riêng chỉ hiện khi đủ quyền xem nội dung đó.

## Các cách thực hiện

- **Khuyến nghị:** danh mục server + lưu category trong spawn/SQL + version giao thức mới. Nhất quán sau restart, phân trang đúng và phân biệt bản hỗ trợ.
- Chỉ lọc tên ở client: dễ lẫn boss cùng tên, sai lịch sử và trang rỗng; không dùng.
- Giữ schema và suy category từ ID lúc đọc: nhẹ hơn nhưng ID mạng hỗ trợ thay đổi và lịch sử cũ thiếu thông tin; chỉ dùng như bước nâng cấp dữ liệu cũ có kiểm soát.

## Task 1: Danh mục và khả năng nhìn thấy

**Files:** Create `SRC/src/nro/models/boss/notice/BossNoticeCategory.java`, `BossNoticeCatalog.java`; Modify `SRC/src/nro/models/boss/Boss.java`, `SRC/src/nro/models/services/BossNoticeService.java`; Test `SRC/test/nro/models/services/BossNoticeCategoryTest.java`.

- [ ] Viết test danh sách ID/dạng lấy từ TaskService, boss hỗ trợ với ID mạng khác, Broly/Super/Mặt Trời và boss sự kiện.
- [ ] Tách quyền được ghi vào bảng khỏi việc phát dòng thông báo xuất hiện toàn server. Boss hỗ trợ được ghi/xem trong nhóm nhiệm vụ nhưng giữ chính sách không spam broadcast.
- [ ] Phân loại theo semantic ID/dạng lúc xuất hiện. Kiểm tra không boss nhiệm vụ nào lọt mục Boss và không boss thường nào bị gắn nhiệm vụ do tên gần giống.
- [ ] Dữ liệu map/khu công khai chỉ có với map thường; nội dung riêng phải có phạm vi người được xem.

## Task 2: Lưu dữ liệu và chuyển đổi lịch sử

**Files:** Modify `SRC/src/nro/models/boss/notice/BossNoticeRow.java`, `BossNoticeTracker.java`, `BossNoticeOutbox.java`, `SRC/src/nro/models/database/BossNoticeDAO.java`; Create `SRC/sql/migrations/20261008_boss_notice_categories.sql`.

- [ ] Bổ sung category, semantic boss ID, dấu hỗ trợ và khu/phạm vi phù hợp. Bảo toàn version của row khi ghi outbox/SQL.
- [ ] Migration cộng thêm cột, không xóa lịch sử. Backfill row cũ từ ID/dạng có thể xác định; row chưa đủ dữ liệu có trạng thái chưa phân loại, không đoán bằng tên.
- [ ] Outbox cũ không có field mới vẫn đọc được; chuyển đổi theo cùng catalog. SQL và outbox cùng quy tắc, tránh mất lượt chưa ghi lúc bảo trì.
- [ ] Test trong schema độc lập: ghi/đọc hai nhóm, bản hỗ trợ, restart, ghi lặp và version cũ không đè dữ liệu mới.

## Task 3: Giao thức và lọc server

**Files:** Modify `SRC/src/nro/models/services/BossNoticeService.java`, `SRC/src/nro/models/network/MySession.java`, `SRC/src/nro/models/services/Service.java`, `SRC/src/nro/models/server/Controller.java` (request `-58` và request ban đầu khi đăng nhập).

- [ ] Capability mới `BOSS2` bên cạnh `BOSS1`; giữ xử lý request/response cũ cho client chỉ có BOSS1. Không tự thêm field vào packet cũ.
- [ ] Hợp đồng mới: request ID, category, view hiện tại/lịch sử, trang; response trả lại các trường đó cùng thời gian server, số trang, cảnh báo và row có category/khu/dấu hỗ trợ.
- [ ] Lọc category, quyền xem và view trước khi đếm/phân trang. Giữ giới hạn 20 row/trang, giới hạn trang và chống spam request.
- [ ] Khi client chuyển nhóm nhanh, response cũ phải bị loại bằng request ID + category + view + trang; không đè danh sách mới.
- [ ] Test wire cũ/mới, invalid category/view/page, logout, request quá nhanh, trang cuối và nhóm không có dữ liệu.

## Task 4: Hai tab client

**Root:** `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts`.

**Files:** Modify `BossNoticeData.cs`; cả `Assembly-CSharp/Game1` và `Game2`: `BossNoticeBoard.cs`, `Panel.cs`, `Controller.cs`, `Service.cs`; Create `Assets/Editor/BossNoticeCategoryTests.cs` và `.meta` do Unity tạo, đặt test trong assembly Editor để không đưa test vào game chạy.

- [ ] Thêm hai lối vào Boss nhiệm vụ/Boss trong trang thông báo; giữ thông báo hệ thống truy cập được.
- [ ] Cả hai nhóm có hiện tại/lịch sử riêng; đổi nhóm reset trang, detail và pending đúng cách. Reset khi logout/đổi tài khoản ở từng tab.
- [ ] Thêm đọc/ghi BOSS2 và khả năng tương thích server cũ: báo chưa hỗ trợ phân loại, không gắn nhãn dữ liệu trộn là Boss nhiệm vụ.
- [ ] Hiển thị tên dạng, map/khu công khai, dấu hỗ trợ; detail/lịch sử giữ người hạ và thời gian như hiện có. Không lẫn nhân vật/đệ tử vào bảng boss.
- [ ] Test nhận response cũ sau chuyển nhóm, hai tab khác tài khoản, server chậm, lỗi packet, dữ liệu rỗng, phân trang và chữ tiếng Việt.

## Task 5: Tích hợp và phát hành

- [ ] Chạy test category/tracker/wire/policy/SQL, hồi quy nhiệm vụ hỗ trợ và toàn bộ suite server; build client cho cả Game1/Game2.
- [ ] Kiểm tra Unity trực tiếp: mỗi nhóm không trùng, bản hỗ trợ chỉ nhóm nhiệm vụ, boss bị hạ/chuyển Broly → Super cập nhật đúng, giờ hỗ trợ kết thúc không còn hiển thị boss đã thu hồi.
- [ ] Kiểm tra boss sự kiện OFF, restart, lịch sử phiên cũ, quyền phó bản/nhân vật riêng và client BOSS1 vẫn đọc đúng.
- [ ] Triển khai sau khi kế hoạch được duyệt: snapshot SQL, bảo trì lưu dữ liệu, backup JAR cũ và SHA-256, migration cộng thêm, JAR mới và client tương thích; xác minh cổng/log và phản hồi trong game. Không thực hiện triển khai trong lượt kiểm tra/lập kế hoạch.
