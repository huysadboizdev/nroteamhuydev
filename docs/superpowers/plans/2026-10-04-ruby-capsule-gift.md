# Capsule hồng ngọc và vé tặng — kế hoạch dự kiến

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bỏ option phản sát thương trên capsule 722; mở capsule nhận hồng ngọc và vé tặng hồng ngọc; chuyển hồng ngọc giữa người chơi với phí đúng một lần 10%.

**Architecture:** Giữ PinkCapsuleService cho việc mở capsule. Thêm RubyGiftService xử lý riêng việc chuyển hồng ngọc, tách khỏi vé ngọc xanh 718. Input chỉ đọc yêu cầu và gọi service; toàn bộ điều kiện và mutation nằm trong service.

**Tech Stack:** Java 21, JAR ứng dụng hiện hành, MySQL/InnoDB, Gson, cơ chế inventory/player-save hiện có.

Trạng thái: đã kiểm tra source và database read-only ngày 2026-10-04. Đây là kế hoạch theo yêu cầu, chưa sửa source ứng dụng/DB/JAR. Các quy tắc thưởng vé và phí dưới đây là đề xuất chờ người dùng chốt.

## Kết quả kiểm tra

- `SRC/src/nro/models/services/PinkCapsuleService.java`: mở 722 hiện chỉ cộng `inventory.ruby`; 80% nhận 1–5, 20% nhận 6–100; đã khóa player và kiểm tra tràn số.
- `SRC/src/nro/models/services_func/UseItem.java`: 722 gọi PinkCapsuleService; vé 718 mở menu 900 rồi Input tìm người nhận.
- `SRC/src/nro/models/services_func/Input.java`: FIND_PLAYER_GIFT_RUBY thực chất dùng `inventory.gem`, trừ số gửi +10%, rồi chỉ cộng 90% cho người nhận. Ví dụ nhập 100: trừ 110, cộng 90.
- Nhánh TANG_NGOC_HONG kiểm tra ruby nhưng `Inventory.subGem()` trừ gem, cộng đủ ruby cho người nhận, không kiểm tra vé 2000 tồn tại trước khi chuyển, không thu phí. Source còn đặt TANG_NGOC_HONG và SELECT_LUCKYNUMBER cùng giá trị 514: cần cấp mã input riêng.
- Database live: shop item 818/temp_id 722 có option 30/param 1 và option 97/param 30 (row item_shop_option 1282). Quét items_bag/items_box: 1 chồng 722, 1 option 97. Đây là snapshot lúc kiểm tra, cần quét lại trong bảo trì.
- Live catalog có 718,722; không có 2000; max item id 1999. Không coi ID2000 an toàn chỉ dựa max DB: kiểm tra cả client, source và hai dump trước cấp ID.
- Boss Namek `dropShared` hiện tạo capsule không có option phản sát thương; lỗi option có nguồn xác nhận trong cấu hình shop và item đã lưu.
- `InventoryService.addItemList` yêu cầu options giống nhau để gộp stack. `Trade` giữ bản copy túi nên mở/tặng trong lúc giao dịch có thể bị hoàn tác hoặc ghi đè; đã có `TransactionService.isTrading()` để kiểm tra.

## Thiết kế đề xuất

Chọn vé hồng ngọc riêng thay vì đổi ý nghĩa 718. Đổi 718 sẽ tác động vé có sẵn và nguồn cấp từ đổi/nạp ngọc xanh. Một vé dùng chung với menu chọn loại ngọc ít catalog hơn nhưng dễ nhầm loại tiền và làm rộng phạm vi thay đổi.

- Mỗi capsule dùng: trừ 1 capsule, nhận thưởng ruby theo tỷ lệ hiện hành và **1 vé hồng ngọc bảo đảm**. Không phụ thuộc số ruby roll được.
- Vé mới tên “Vé tặng hồng ngọc”, type27, stack được, dùng icon6765 sẵn có; đề xuất ID2000 sau kiểm tra xung đột. Vé không giao dịch/ký gửi; capsule giữ option30 không giao dịch, chỉ bỏ option97.
- Dùng vé: nhập tên người nhận đang online và số hồng ngọc, xem màn hình xác nhận số trừ/phí/số nhận. Thành công tiêu hao 1 vé; thất bại không trừ gì.
- Đề xuất số nhập là tổng ruby người gửi mất: `fee = (amount + 9L) / 10`, `received = amount - fee`. Chỉ thu 1 lần. Gửi100 => mất100/nhận90; gửi11 => mất11/nhận9. Từ chối nếu received<=0 (gửi1).
- Nếu người dùng chọn thu thêm phí: `fee = (amount + 9L)/10`, `debit = amount + fee`, `received = amount`; gửi100 => mất110/nhận100. Không dùng đồng thời hai cách.
- Không cho tự tặng; từ chối người nhận offline/đổi session, thiếu vé, thiếu ruby, số không hợp lệ, số dư âm, tràn số hoặc một trong hai bên đang giao dịch.
- Thông báo luôn ghi “hồng ngọc”, kèm phí và số người nhận thực nhận.
- Xóa option97 chỉ trên item722, giữ option97 của trang bị/vật phẩm khác. Chuẩn hóa capsule cũ lúc load/copy/grant bằng một hàm dùng chung; sửa dữ liệu lưu trong bảo trì để túi/rương nhất quán.

## Các bước triển khai

### 1. Snapshot và khóa các quy tắc

- [x] Chốt mỗi capsule cấp 1 vé, 1 vé/lần tặng, người nhận online, cách tính/làm tròn phí đã đề xuất.
- [x] Chụp hash SRC/20.jar, source liên quan, hai dump `team2026.sql`, `SRC/sql/nro1.sql`; snapshot catalog722/718, shop722 và các bản ghi inventory cần sửa. Không đưa thông tin tài khoản vào báo cáo công khai.
- [x] Kiểm tra ID2000/icon6765 và mã input mới chưa sử dụng trong server/client; dùng mã input hồng ngọc riêng, loại bỏ alias514 của nhánh hồng ngọc.

### 2. Bỏ option phản sát thương đúng phạm vi

Files: `SRC/src/nro/models/services/ItemService.java`, `SRC/src/nro/models/database/PlayerDAO.java`, `SRC/src/nro/models/services/InventoryService.java`; migration mới `SRC/sql/migrations/20261004_ruby_capsule_gift.sql` và hai dump.

- [x] Tạo quy tắc chuẩn hóa option của item722: bỏ ID97, bảo toàn quantity/createTime và mọi option khác; áp dụng tại load/grant/copy/shop-option để các nguồn tạo item722 không đưa option97 trở lại.
- [x] Migration xóa option97 của shop có temp_id722 bằng join, không chỉ hardcode row1282. Không xóa template option97 toàn cục.
- [x] Trong bảo trì, parse JSON lồng của items_bag/items_box, chỉ sửa options item722, giữ slot/thời gian/số lượng. Backup từng giá trị gốc; kiểm tra số row, tổng capsule, ruby/gem không đổi. Không dùng replace chuỗi trên toàn inventory.
- [x] Kiểm tra thêm giftcode/reward/mail/ký gửi/shop chứa722; chuẩn hóa nguồn có liên quan, không phát thêm tài sản khi dọn option.

### 3. Cấp vé khi mở capsule

Files: `SRC/src/nro/models/consts/ConstItem.java`, `SRC/src/nro/models/services/PinkCapsuleService.java`, `SRC/src/nro/models/data/DataGame.java`, migration/dumps.

- [x] Bổ sung template vé riêng, constant ID, tên/mô tả; tăng vsItem từ giá trị hiện tại sau kiểm tra, để client tải catalog mới. Kiểm tra giới hạn chiều dài description.
- [x] Trước mutation, tính thưởng, kiểm tra ruby không tràn và khả năng thêm1 vé vào stack hoặc ô trống; nếu capsule cuối cùng sẽ giải phóng ô, cho phép vé dùng ô đó.
- [x] Chuẩn bị trạng thái túi kết quả bằng bản copy; chỉ commit túi/ruby sau khi mọi bước thành công dưới khóa player. Không add vé trực tiếp rồi để lỗi tiêu hao capsule xảy ra sau đó.
- [x] Từ chối mở khi đang giao dịch; không làm mất capsule/ruby khi túi đầy không gộp được vé. Refresh túi/tiền sau commit; lỗi packet không được phát thưởng lần nữa.

### 4. Service tặng hồng ngọc

Create: `SRC/src/nro/models/services/RubyGiftService.java`. Modify: `SRC/src/nro/models/services_func/Input.java`, `SRC/src/nro/models/services_func/UseItem.java`; dùng form riêng hoặc menu riêng nếu cần `SRC/src/nro/models/npc/NpcFactory.java`.

- [x] Tách form hồng ngọc khỏi menu900/vé718; chuyển nhánh TANG_NGOC_HONG cũ về service mới hoặc vô hiệu đường cũ sau xác minh callers. Không để đường cũ tiếp tục cộng ruby/trừ gem.
- [x] Xử lý số lượng bằng long, kiểm tra miền int và tính phí bằng số nguyên. Kiểm tra thông tin xác nhận còn đúng trước khi commit.
- [x] Khóa hai Player theo thứ tự ID cố định; dưới khóa kiểm tra lại online/session, không tự tặng, không giao dịch, vé hợp lệ trong túi, quantity>=1, ruby đủ và số dư người nhận không tràn.
- [x] Commit đồng thời ở RAM: trừ đúng ruby của người gửi, cộng đúng ruby người nhận, trừ1 vé. Chỉ sau đó gửi packet và xóa trạng thái form; packet lỗi không chạy lại giao dịch.
- [x] Ghi log giao dịch có mã duy nhất, hai ID nội bộ, amount/fee/net, before/after và vé tiêu hao để đối chiếu.
- [x] Để bảo đảm an toàn khi server crash: bổ sung ledger InnoDB và cập nhật hai snapshot player trong cùng transaction, phối hợp khóa với PlayerDAO autosave/logout; trả lỗi và không commit RAM nếu DB rollback. Test save/logout không ghi đè snapshot vừa chuyển. Không coi gọi updatePlayer hai lần là transaction.
- [ ] Vé718 giữ luồng ngọc xanh riêng; sửa phí kép của luồng718 là đề xuất liên quan, chỉ đưa vào triển khai khi người dùng đồng ý phạm vi này.

### 5. Kiểm thử trước phát hành

Modify: `SRC/test/nro/models/services/PinkCapsuleTest.java`, `SRC/test/nro/models/services/PinkFixture.java`. Create: `SRC/test/nro/models/services/RubyGiftTest.java`, `SRC/test/nro/models/services/RubyGiftDbTest.java`.

- [x] Capsule: biên thưởng1/5/6/100, luôn cấp đúng1 vé, capsulequantity1/stack, túi đầy gộp được/không gộp được, giải phóng ô capsule cuối, tràn ruby, xử lý đồng thời, lỗi packet, đang trade.
- [x] Options: capsule cũ trong túi/rương/shop hết97; không đổi option30/số lượng; trang bị khác vẫn giữ97; stack capsule sau chuẩn hóa gộp đúng.
- [x] Tặng:100/11/1, thiếu ruby/vé, số âm/0/không phải số/quá lớn, tự tặng, offline/đổi session, tràn số người nhận, đang trade; lỗi không trừ gì.
- [x] Đồng thời: một vé nhận hai request, A tặng B và B tặng A, autosave/logout, DB lỗi trước/sau commit, replay cùng mã giao dịch, packet lỗi; tổng ruby hai người chỉ giảm đúng phí.
- [x] DB test trên schema riêng; không dùng tài khoản thật phát thưởng. Chạy test capsule/KOL/inventory/gold/option-wire hiện có trên candidate JAR.
- [ ] Client thực tế: capsule cũ mất dòng phản30%, mở hiện đủ ruby+vé, icon vé hiển thị, form xác nhận đúng loại tiền/phí; đăng nhập lại vẫn giữ số dư/vé.

### 6. Cập nhật an toàn và khôi phục

- [x] Build candidate và kiểm chứng trước khi chạm SRC/20.jar.
- [x] Bảo trì có lưu dữ liệu, xác minh process/cổng server đã dừng; snapshot DB mới nhất rồi chạy migration có kiểm tra điều kiện.
- [x] MOVE JAR cũ vào SRC/jar-backup với timestamp duy nhất; xác nhận SHA-256 backup trùng bản cũ, ghi metadata hash mới/cũ, migration, source, thời gian.
- [x] Cài duy nhất SRC/20.jar trong thư mục chạy; gom app JAR staging/dư hết dùng vào jar-backup, giữ SRC/lib và JAR công cụ cần dùng.
- [x] Khởi động, kiểm tra catalog/service/server và client; ghi báo cáo xác nhận. Rollback cần bảo trì/backup JAR hiện tại; giữ ledger/số dư/vé đã cấp, không phục hồi snapshot tài sản cũ đè lên phát sinh mới.

## Điều kiện nghiệm thu

Capsule722 cũ/mới không còn option phản sát thương; mở mỗi viên nhận ruby+1 vé đúng một lần; người chơi tặng bằng vé riêng, mất đúng10% theo cách đã chốt; gem không đổi khi tặng ruby; request lỗi không mất tài sản; không nhân đôi sau replay/crash; toàn bộ kiểm tra bắt buộc đạt trước cài JAR.

## Kết quả triển khai 04/10/2026

Đã triển khai và kiểm tra server thực tế. JAR cài đặt đạt 67/67 nhóm kiểm thử. Vé dùng ID2000 vì catalog yêu cầu ID liên tục; input tặng62001, xác nhận62002; catalog15. Phí vé ngọc xanh718 giữ nguyên phạm vi cũ. Chưa thao tác bằng client trò chơi để nghiệm thu giao diện; kiểm tra runtime đã xác nhận catalog, icon, option, shop và phí. Báo cáo: [ruby-capsule-gift](../../source-analysis/ruby-capsule-gift.md).
