# Thiên Sứ: chỉ số chính +1–30%, dòng phụ +1–7% — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Chế tạo mới cho chỉ số chính vượt nền 1–30% và mỗi dòng phụ tối đa 7%, với 999 mảnh, một công thức, hai loại đá, 50 hồng ngọc và tiêu món Hủy Diệt tương ứng đang mặc kể cả thất bại theo tỷ lệ.

**Architecture:** Tách validator và bộ roll chỉ số của chế tạo Thiên Sứ thành class nhỏ. `showInfoCombine` chỉ xem yêu cầu; `CheTaoTS` thực hiện một lần dưới khóa người chơi, chuẩn bị trạng thái nhận đồ trước khi tiêu tài sản. Không sửa factory chung làm ảnh hưởng các nguồn phát đồ khác hoặc đồ đã sở hữu.

**Tech Stack:** Java 17, SQL catalog hiện có, giao thức combine và client Unity hiện tại.

**Design:** `docs/superpowers/specs/2026-10-07-thien-su-bonus-design.md`.

**Trạng thái:** Đã triển khai và kiểm chứng tự động; báo cáo `docs/thien-su-crafting-deployment-20261007.md`. Chưa kiểm tra thao tác trực tiếp trong Unity (ghi rõ ở bước tương ứng).

## Task 1 — Bộ roll bonus độc lập

**Create:** `SRC/src/nro/models/combine/ThienSuCraftStats.java`, `SRC/test/nro/models/combine/ThienSuCraftStatsTest.java`.

- [x] Viết test đầu ra trước khi sửa code: bonus chính 1 và 30; dòng phụ 1 và 7; 1/2/3 dòng khác nhau; mọi roll chính 1–30 trên 15 template; nhẫn gốc18/19/20/22 không tăng vượt30% hoặc giữ nguyên do roll nhỏ.
- [x] Chạy test xác nhận thiếu hành vi mới; không dùng random không kiểm soát trong test. Truyền nguồn random vào roller để ép từng nhánh.
- [x] Thực hiện đúng option chính theo loại đồ: 47/22/0/23/14. Dùng long cho phép nhân; cộng mức tăng làm tròn và kẹp trần như thiết kế; không đổi options khác.
- [x] Thêm dòng phụ từ danh sách50/77/103/94/5 bằng lấy không hoàn lại; param1–7. Giữ luật xác suất/số dòng theo cấp đá cũ; không tạo option15 giả.
- [x] Test bonus chính chạy trước dòng phụ và chỉ một lần, không làm dòng phụ vượt7; không tăng option21/30/timer/sao. Chạy test lại và kiểm tra toàn bộ miền roll.

## Task 2 — Một công thức và một validator

**Create:** `SRC/src/nro/models/combine/ThienSuCraftRecipe.java`, `SRC/test/nro/models/combine/ThienSuCraftRecipeTest.java`.

**Read:** `SRC/src/nro/models/item/Item.java`, `SRC/src/nro/models/player/SetClothes.java`, `SRC/src/nro/models/services/InventoryService.java`.

- [x] Dùng công thức đã chốt: chính xác 4 nhóm, 1 công thức1071–1073/1084–1086, 999 mảnh1066–1070, 1 đá1074–1078, 1 đá1079–1083, 50 hồng ngọc; không thu vàng. Yêu cầu đủ5món Hủy Diệt đang mặc, tiêu ô tương ứng0/1/2/3/4 theo mảnh1066/1067/1070/1068/1069 cả khi thành công và thất bại theo tỷ lệ.
- [x] Viết test thiếu/đủ nguyên liệu, quantity998/999, hồng ngọc49/50, công thức thường/VIP và gender, cả5loại mảnh, trùng item/slot, vật phẩm không còn trong túi, không đủ ô trống, tháo/đổi bộ Hủy Diệt sau mở bảng. Món Hủy Diệt trong túi không thay được yêu cầu trang bị.
- [x] Xây validator trả kết quả có lỗi rõ ràng, không gọi `findFirst().get()` khi thiếu item; kiểm tra ownership/quantity tại thời điểm xác nhận. Cùng validator cho xem yêu cầu và chế tạo.
- [x] Giữ giá shop/nguyên liệu SQL, không thay80tỷ hoặc khóa giao dịch; đọc catalog thật trong test phù hợp và xác nhận các ID đầy đủ.

## Task 3 — Chọn để xem, bấm để chế tạo

**Modify:** `SRC/src/nro/models/combine/CheTaoTrangBiThienSu.java`, `SRC/src/nro/models/combine/CombineService.java`, `SRC/src/nro/models/npc_list/Whis.java`.

**Create:** `SRC/test/nro/models/combine/ThienSuCraftFlowTest.java`.

- [x] Test `CombineService.showInfoCombine` không thay túi/trang bị/hồng ngọc/vàng/roll, kể cả đủ4nguyên liệu; hủy bảng không tiêu tài sản.
- [x] Chuyển `showInfoCombine` thành preview và tạo menu xác nhận đúng ID515 mà Whis đã xử lý; nội dung nêu phí50hồng ngọc, tỷ lệ, hai khoảng bonus và tên món Hủy Diệt sẽ mất kể cả thất bại theo tỷ lệ.
- [x] Bỏ cả hai công thức phí vàng cũ khỏi luồng này; cùng recipe được dùng xuyên suốt, không còn hai nhánh tiêu khác nhau.
- [x] Dưới khóa người chơi, kiểm tra các guard pending hiện có; chuẩn bị output, túi, trang bị và số dư hồng ngọc trong bản sao bằng helper hiện có. Chỉ công bố sau khi chứng minh nhận đồ hợp lệ; không trừ tài sản nếu nhận đồ lỗi.
- [x] Test thành công trả đúng một món vào túi, không tự mặc; thành công/thất bại theo tỷ lệ đều tiêu đúng nguyên liệu,50hồng ngọc và đúng một món Hủy Diệt đang mặc. Bốn món còn lại và vàng giữ nguyên. Callback trùng, xác nhận đồng thời, túi đầy, trade/pending và item chuyển sau preview không mất tài sản hoặc phát đồ hai lần.
- [x] Sau commit, tính lại set trang bị/chỉ số và gửi body/HP/KI/sức đánh, cập nhật túi/hồng ngọc. Test người chơi phải mặc bù bộ Hủy Diệt để làm lượt tiếp theo.
- [x] Tạo/lưu dấu lượt xác nhận phù hợp với `CombineNew`/IDMark hiện tại, kiểm tra tiêu lượt cả thành công và thất bại, xác nhận mới cho lượt tiếp theo. Chạy test và rà đường lưu PlayerDAO để túi/trang bị/hồng ngọc cùng trạng thái cuối, không thêm đường ghi SQL một phần.

## Task 4 — Tương thích đồ cũ và client

**Test:** `SRC/test/nro/models/item/HighTierEquipmentPowerTest.java`, `SRC/test/nro/models/item/HighTierEquipmentTradeTest.java`, test flow mới.

**Read:** client `Assets/Scripts/Assembly-CSharp/Game1/Panel.cs`, `Game2/Panel.cs`, `Controller.cs`; chỉ sửa client nếu protocol combine hiện có không đủ hiển thị thông tin cần thiết.

- [x] Rà đường grant/login/save và kiểm thử bảo toàn đồ cũ trong túi, metadata và nguồn factory khác; không đổi source login/PlayerDAO hoặc reroll đồ cũ. Chưa chạy vòng đăng nhập/lưu SQL bằng tài khoản thử qua client.
- [x] Test policy yêu cầu80tỷ và khóa giao dịch vẫn đúng trên15ID; recipe không tự tạo SKH hoặc bonus đủ bộ.
- [x] Kiểm tra tooltip/options hiển thị đúng7%, giá trị chính sau tăng và lời hướng dẫn bỏ0–15%; không dùng option15 phản đòn làm số dòng.
- [ ] Thử trực tiếp trong Unity: chưa thực hiện vì không có công cụ điều khiển desktop/Unity. Đã kiểm tra source/giao thức/resource hai client và luồng server tự động; chưa xác nhận hình ảnh/menu thực tế.

## Task 5 — Kiểm chứng và triển khai khi được duyệt

- [x] Build Java đầy đủ, chạy test chế tạo mới và regression combine/equipment/shop/points liên quan; rà soát độc lập các phép tính, guard và tiêu nguyên liệu.
- [x] Dựng JAR staging, chạy kiểm thử trực tiếp trên JAR; ghi SHA-256 và snapshot source để không cài bản khác với bản đã test.
- [x] Nếu cần cài: bảo trì có lưu dữ liệu, xác nhận JVM dừng, chuyển `SRC/20.jar` cũ vào `SRC/jar-backup` tên riêng có thời gian, đối chiếu hash backup/bản mới và ghi metadata. Không di chuyển thư viện hoặc tool JAR còn cần dùng.
- [x] Khởi động, xác nhận cổng/log và kiểm tra chỉ đọc chế tạo/nguồn cấu hình trong JVM; ghi báo cáo cùng hạn chế kiểm chứng. Không tự thay đồ cũ hoặc thay phần phó bản/capsule/hỗ trợ nhiệm vụ đã triển khai.
