# Original Client Browser Implementation Plan

**Goal:** Bỏ UI web thêm ngoài client, canvas phủ viewport và giữ điều khiển gốc.

**Architecture:** Sửa cả template và index đang phục vụ. Giữ API NroTabs để callback từ nút đổi tab Unity hoạt động; bỏ tham chiếu DOM tab/fullscreen/device. Bounded canvas render cùng tỉ lệ viewport cho PC/mobile, keyboard bridge giữ nguyên.

**Tech Stack:** HTML/CSS/JS, Node tests, Playwright actual Unity hiện có.

- [x] Thêm Tests/client-shell.test.cjs: shell không có nav/tab/fullscreen/device; callback đổi tab không phụ thuộc header DOM. Cập nhật render-resolution.test.cjs kiểm tra full viewport và backing ratio; chạy red.
- [x] Sao lưu template/index/tests sửa; loại header CSS/HTML/handlers, đổi resize để style100% và renderScale=min(1,1024/width,(pc?600:768)/height). Giữ loader filenames và data/wasm nguyên trạng.
- [x] Cập nhật browser tests sử dụng nút Unity hoặc BrowserHost callback thay header đã bỏ; chạy Node và browser layout/orientation/actualUnity PC Android iOS, test nút đổi tab gốc PC.
- [x] Review diff, lưu ảnh local mới và báo rõ các source C# chưa build.
