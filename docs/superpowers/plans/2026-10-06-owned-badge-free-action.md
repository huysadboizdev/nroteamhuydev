# Tab Sở hữu: Nhận miễn phí

**Goal:** Khi nhấn danh hiệu còn hạn trong tab Sở hữu, chỉ hiện nút “Nhận miễn phí”; bỏ giá2ngọc và nút mua20/50/100lần. Nút này kích hoạt danh hiệu người chơi đã sở hữu, không tạo bản mới hoặc gia hạn.

**Architecture:** Sửa bản sao `ItemShop` của `TabShopSoHuu`, đưa cost về0 và dùng typeSell chuẩn của shop thường. Packet -44 hiện có sẽ chứa giá vàng=0/ngọc=0. Client Game1/Game2 đã có nhánh tạo duy nhất Command3000 “Nhận miễn phí” trong `Panel.doFireShop`, do đó ưu tiên sửa server mà không đổi Unity hoặc SQL.

**Tech stack:** Java17/JDK21 server, packetshop -44, client Unity Game1/Game2 hiện tại.

## Kết quả điều tra

- `TabShopSoHuu` sao chép `ItemShop` giữ nguyên giá cấu hình, hiện2ngọc.
- `ShopService.openShopType0` và `openShopDanhHieu` viết hai giá theo cost/typeSell.
- `Panel.doFireShop` khi buyCoin=0, buyGold=0, powerRequire=0, không phải shop đặc biệt, sẽ thêm đúng một nút nhận miễn phí. Nhánh có giá mới thêm các nút mua nhiều lần.
- Server tab45 gọi `changeDanhHieu` trước đường mua vật phẩm; đây là kích hoạt danh hiệu đã sở hữu. Tab44 là nhận/mở khóa danh hiệu.

## Phạm vi và hành vi

Áp dụng tất cả danh hiệu trong tab45 Sở hữu, gồm Mẹ Rồng và Gõ đầu trẻ. Danh sách chỉ có danh hiệu hợp lệ theo điều kiện/thời hạn hiện có. Giữ chỉ số, số ngày còn lại, điều kiện7rồngnhívĩnhviễn của Mẹ Rồng, lựa chọn một dòng Gõ đầu trẻ và cooldown đổi danh hiệu.

Khi nhấn “Nhận miễn phí”, bật danh hiệu đã sở hữu. Nếu đang dùng chính danh hiệu đó, giữ thông báo đang sử dụng hiện tại. Hết hạn hoặc không còn đủ điều kiện thì từ chối. Không trừ vàng/ngọc/ruby, không tạo bản ghi sở hữu mới, không đặt lại thời hạn30ngày. Chỉ thay giá trên bản sao dành riêng cho người mở tab, tránh làm đổi giá catalog/shop khác.

## Task1: Kiểm thử trước khi sửa

- [ ] Thêm `OwnedBadgeFreeActionTest` hoặc mở rộng fixture shop: catalog gốc có cost2/COST_GEM; tab45 sao chép phải có cost0, trong khi catalog và tab44 vẫn giữ giá.
- [ ] Parse packet -44 thật từ `ShopService`: cả hai giá của từng dòng tab45 bằng0; options/chỉ số/thời hạn vẫn đủ và không lệch layout.
- [ ] Tái hiện fixture thất bại trên JAR hiện tại để xác nhận đúng nguyên nhân.

## Task2: Sửa nguồn dữ liệu UI

- [ ] `SRC/src/nro/models/shop/TabShopSoHuu.java`: sau khi copy row, đặt cost=0 và typeSell theo giá vàng chuẩn0 để cả hai trường giá wire đều bằng0.
- [ ] Kiểm tra đường mở shop đang dùng có `Shop(shop,player)` để thay đổi tab45 được áp dụng; nếu cần chốt giá0 riêng tab45 khi serialize, không đặt giá0 cho toànshop hoặc tab44.
- [ ] Không chỉnh catalogSQL và không thay globalitemTemplate/strRequire chỉ để đổi label.

## Task3: Xác minh thao tác kích hoạt

- [ ] Theo Command3000 tới buyItem và `ShopService` tab45; xác nhận luôn đi `changeDanhHieu`, không đi nhánh thu tiền hay thêm item.
- [ ] Kiểm tra số dư vàng/ngọc/ruby, danh sách dataBadges và timeofUseBadges trước/sau: số dư, số bản ghi và thời hạn không đổi; chỉ trạng thái đang sử dụng và chỉ số chiến đấu đổi.
- [ ] Thử danh hiệu đang dùng, hết hạn, mất điều kiện Mẹ Rồng, mở bảng rồi trạng thái sở hữu thay đổi, nhấn lặp; giữ các guard hiện có.

## Task4: Kiểm tra client và hồi quy

- [ ] Trong Unity kiểm tra cả Game1/Game2: dòng tabSở hữu không còn giá2ngọc; bấm mở chi tiết chỉ thấy “Nhận miễn phí”, không có mua20/50/100. Giữ mô tả/chỉ số/thời hạn.
- [ ] Nếu packet0/0 đã đủ, không sửa hoặc buildclient. Chỉ xem xét sửaclient nếu đường thực tế còn tạo nút sai; báo rõ bằng chứng trước khi mở rộng phạm vi.
- [ ] Chạy test shop miễn phí, GoDauTreShopTest và BadgeSystemTest/DragonBadgeTest phù hợp; đối chiếu chỉ class thuộc bản sửa thay đổi, các class vệ tinh/Hirudegarn/ngọc sao đen giữ nguyên.

## Task5: Triển khai khi người dùng yêu cầu thực hiện

- [ ] Buildcandidate; lưu hashsource/class/JAR và kết quả kiểm thử.
- [ ] Bảo trì lưu dữ liệu, xác nhận tất cả JVM20.jar dừng, snapshotSQL.
- [ ] Chuyển JAR cũ sang `SRC/jar-backup` với timestamp riêng, SHA256 và metadata; cài `SRC/20.jar` mới, giữ thư mục chạy chỉ một JAR ứng dụng.
- [ ] Restart, kiểm tra cổng14445, probeclass/wire và xác minhclient; ghi rollback vềbackup. Không chạy SQLmigration nếu không cần.

**Trạng thái cập nhật:** đã triển khai server theo yêu cầu tiếp theo, qua24nhóm kiểm tra và probeJVM. Chỉ classTabShopSoHuu thay đổi; serverPID9156/cổng14445. Chưa thao tác xác nhận hình ảnh trên Unity. Chi tiết: [báo cáo triển khai](../../source-analysis/owned-badge-free-action-2026-10-06.md).
