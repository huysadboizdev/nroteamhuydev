# Tắt thông báo boss 12h và Mabư 14h Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ẩn toàn bộ boss thuộc sự kiện 12h và Mabư 14h khỏi bảng boss hiện tại/lịch sử, thông báo màn hình và dòng thông báo chạy phía dưới.

**Architecture:** Lọc tại server bằng semantic boss ID để dùng chung cho cả hai tab client. Chặn ghi sự kiện mới và lọc kết quả đọc trước phân trang để lịch sử SQL/outbox cũ cũng bị ẩn. Giữ dữ liệu lịch sử trong kho; không cần xóa SQL. Mabu/SuperBu của 14h được xác định theo ID sự kiện, không theo tên hiển thị có thể trùng boss khác.

**Tech Stack:** Java 17; main-based Java regression tests; client Unity C# chỉ dùng để đối chiếu protocol/UI.

---

## Kết quả kiểm tra source ngày 21/10/2026

- `ThienSuCraftStats.apply`: đồ chế tạo mới tăng chỉ số chính ngẫu nhiên 1–30%; tính sau chỉ số nền và thưởng hành tinh. Có làm tròn số nguyên, giới hạn tối đa 30%.
- `ItemService.DoThienSu` + `Util.highlightsItem`: thưởng hành tinh 10% cho áo/găng Xayda, quần Trái Đất, giày/nhẫn Namek. Công thức VIP dùng factoryGender riêng; không mặc định rằng mọi món đều nhận 10%.
- Dòng phụ chế tạo: có thể không ra; nếu ra thì 1–3 dòng khác nhau, mỗi dòng 1–7%, chọn từ sức đánh, HP, KI, giáp, sức đánh chí mạng. Không phải mặc đủ bộ là tự cộng một mức % cố định.
- Chỉ báo cáo mức hiện có; chưa có yêu cầu thay bonus Thiên Sứ.
- `BossAnnouncementPolicy` đã chứa danh sách tắt boss 12h và điều kiện Mabư 14h; `Boss.noticePublic/noticeLookup` đã kiểm tra policy.
- Lỗ hổng bảng: `BossNoticeService.boardEligible` có biểu thức `noticeLookup() || suppressedNotice(b)` nên cố ý thu thập cả boss bị tắt; `rows()` không lọc chúng khi gộp tracker và lịch sử SQL.
- Dòng chạy dưới dùng `ServerNotify.notify` (packet 93); thông báo boss màn hình dùng packet 115. Các callback tiêu diệt riêng của hai nhóm đã có kiểm tra `suppressedNotice`; cần kiểm thử lại toàn tuyến, không chỉ đổi danh sách UI.
- `BossNoticeRow` lưu semantic ID nhưng không lưu map ID/type. Trong source hiện tại `BossID.MABU` và `BossID.SUPERBU` chỉ được dùng để tạo `MajinBuu_14h.Mabu2H/SuperBu`; có thể lọc lịch sử bằng ID mà không đổi schema/protocol. Không dùng tên “Mabư” để lọc vì có boss giải đấu khác.

## Task 1: Tập trung quy tắc ẩn đúng nhóm sự kiện

**Modify:** `SRC/src/nro/models/services/BossAnnouncementPolicy.java`.
**Test:** `SRC/test/nro/models/services/BossNoticePolicyTest.java`.

- [ ] Thêm kiểm tra thất bại cho đủ 11 semantic IDs: DRABURA, DRABURA_2, DRABURA_3, BUI_BUI, BUI_BUI_2, YA_CON, MABU_12H, GOKU, CADIC, MABU, SUPERBU; thêm đối chứng Fide và Mabư giải đấu phải được giữ.
- [ ] Thêm hàm thuần `hiddenTimedMabuNoticeId(int id)` với tập ID trên; hàm không phụ thuộc zone hoặc tên boss.
- [ ] Dùng hàm này trong `suppressedNoticeId` để chặn hai nhóm kể cả lúc boss chưa có zone hoặc đã rời map. Giữ các quy tắc hiện có cho nhóm boss khác.
- [ ] Chạy lại kiểm tra policy; xác nhận mọi dạng biến hình của Mabư 14h vẫn dùng semantic ID sự kiện.

## Task 2: Lọc cả bảng hiện tại và lịch sử cũ

**Modify:** `SRC/src/nro/models/services/BossNoticeService.java`.
**Test:** tạo `SRC/test/nro/models/services/TimedMabuNoticeTest.java`.

- [ ] Tạo fixture bộ nhớ: dữ liệu sống, lịch sử tracker, lịch sử SQL giả lập; mỗi nguồn có Goku/Cadic, Mabư 12h/14h và một boss đối chứng.
- [ ] Kiểm tra thất bại: hai nhóm biến mất ở cả kind 0/1, category 1/2, request legacy/V2 và push; các boss đối chứng còn nguyên.
- [ ] Sửa `boardEligible`: trả false nếu `hiddenTimedMabuNoticeId(lookupSemanticId(b))`; không mở rộng thay đổi sang các nhóm ngoài yêu cầu.
- [ ] Lọc kết quả `rows()` bằng `hiddenTimedMabuNoticeId(r.semanticBossId())` trước sort/limit/phân trang. Với schema cũ, chỉ dùng `bossId` làm fallback khi đó là BossID tĩnh đã biết; không lấy ID mạng ngẫu nhiên làm semantic ID.
- [ ] Kiểm tra trang cuối bị rỗng được clamp đúng và tổng số trang tính sau khi lọc.
- [ ] Kiểm tra restart/nạp lại outbox không làm các dòng đã ẩn xuất hiện lại. Không sửa/xóa dữ liệu database thật để đạt kết quả.

## Task 3: Kiểm tra thông báo màn hình và dòng chạy dưới

**Read/modify nếu kiểm tra phát hiện đường lọt:**

- `SRC/src/nro/models/services/BossNoticeService.java` (`appeared`, `defeated`).
- `SRC/src/nro/models/services/BossScreenNoticePolicy.java`.
- `SRC/src/nro/models/boss/MajinBuu_12h/*.java`.
- `SRC/src/nro/models/boss/MajinBuu_14h/Mabu2H.java`, `SuperBu.java`.

**Tests:** `BossScreenNoticePolicyTest`, `BossScreenNoticeWireTest`, `TimedMabuNoticeTest`.

- [ ] Dùng sink bộ nhớ đếm sự kiện spawn/kill: 11 ID bị tắt phát 0 packet 93 và 0 packet 115; Fide vẫn phát thông báo.
- [ ] Rà callback tiêu diệt riêng, đổi dạng và hồi sinh; bảo đảm tất cả dùng quy tắc chung, không phát chuỗi thông báo trực tiếp vượt qua policy.
- [ ] Đối chiếu hai tab client: `Game1/BossNoticeBoard.cs`, `Game2/BossNoticeBoard.cs` nhận snapshot đã lọc; phần thông báo hệ thống và dòng chạy dưới không được nhận thông báo mới của hai nhóm.
- [ ] Khi kiểm tra bằng client thật, mở lại bảng hoặc đăng nhập lại để loại cache phiên cũ; phân biệt thông báo đã nhận trước cập nhật với sự kiện mới.

## Task 4: Kiểm tra hồi quy và bàn giao

- [ ] Biên dịch source/test vào `.analysis/timed-mabu-notice-check` bằng JDK sẵn có, `--release 17 -encoding UTF-8 -proc:none`, classpath `SRC/lib/*`. Không đóng gói application JAR.
- [ ] Chạy `BossNoticePolicyTest`, `BossScreenNoticePolicyTest`, `BossScreenNoticeWireTest`, `BossNoticeCategoryTest`, `BossNoticeWireTest`, `BossNoticeTrackerTest`, `BossNoticeZonePrivacyTest`, `TimedMabuNoticeTest` bằng classpath chứa thư mục classes/tests vừa biên dịch và `SRC/lib/*`.
- [ ] Kiểm tra mọi nguồn dữ liệu/đường gửi ở Task 2–3, chạy `git diff --check`, rà diff chỉ gồm phạm vi thông báo yêu cầu.
- [ ] Xác nhận boss vẫn spawn, đánh, rơi đồ và tính nhiệm vụ; thay đổi chỉ tác động công bố thông báo.
- [ ] Bàn giao source và kết quả kiểm tra. Chỉ build/cập nhật JAR khi người dùng yêu cầu rõ ở bước triển khai; khi thay JAR phải dừng server an toàn, backup timestamp, SHA-256 và metadata theo AGENTS.md.

## Tiêu chí nghiệm thu

- Bảng như ảnh không còn Goku/Cadic “Cửa ải”, các boss 12h khác hoặc mọi dạng Mabư 14h, ở hiện tại và lịch sử.
- Không có thông báo xuất hiện/tiêu diệt của hai nhóm ở panel màn hình, mục thông báo hệ thống và dòng chạy dưới.
- Boss ngoài phạm vi vẫn hiện đúng; không thay bonus Thiên Sứ, lịch sự kiện, nhiệm vụ, chiến đấu hoặc phần thưởng.

## Cập nhật triển khai theo yêu cầu tiếp theo

- Đã sửa policy cho 11 semantic IDs và lọc cả tracker/lịch sử trước phân trang.
- Đã chặn boss thuộc hai nhóm vào tracker mới; các callback thông báo chung và riêng đều dùng policy chặn.
- Theo yêu cầu bổ sung: bỏ subscription/push ở server. Client Game1/Game2 bỏ cache bảng và bỏ nhận push; mỗi lần mở lấy trang đầu mới, bỏ phản hồi đến khi bảng đã đóng, không tự tải lại khi kết nối lại.
- Bỏ hook mở trùng trong `Panel.show`; giữ yêu cầu tải từ thao tác mở danh sách. Chọn nhóm, chuyển trang và Làm mới là yêu cầu do người dùng chủ động.
- Đã thêm `TimedMabuNoticeTest.java`, `Tests/BossNoticeSnapshotTests.ps1`; cập nhật kiểm tra policy, wire và hành vi bảng.
- Kiểm tra Java dùng classpath giải nén trong `.analysis/timed-mabu-notice-check`, tránh lỗi đọc classpath của javac trong sandbox. Không dùng database thật, không đóng gói application JAR.
- Chưa kiểm chứng giao diện bằng client đang chạy; chưa build APK/EXE/JAR hoặc triển khai bản phát hành cho thay đổi này.
