# Gỡ tạm Siêu Cấp và sửa điều ước Namek — 07/10/2026

Theo lựa chọn của người dùng: chỉ gỡ điều ước bột mỳ, giữ hai điều ước ngọc rồng 3 sao và Hổ mặp vàng. Không thay đổi EXP.

## Hổ mặp vàng 942

Kiểm thử trên JAR hiện tại và sau các sửa chặn Siêu Cấp: trang bị vào ô pet, chỉ số HP/KI/sức đánh, triệu hồi pet đi theo, khôi phục khi đang mặc, gọi lại và tháo đều qua. Parts 966/967/968 và các ảnh được tham chiếu có đủ x1–x4. Hổ được giữ lại. Chưa quan sát thủ công bằng Unity trong lượt này. Bằng chứng chi tiết tại tiger-pet-audit-20261007.md.

## Siêu Cấp 1015

Có icon 9650, nhưng server chưa có xử lý triệu hồi. Hai client chỉ nhận boolean rồng thường/Namek từ packet -83, rồi vẽ effect 17 hoặc 25. DataEffect_1015 không phải bằng chứng về rồng Siêu Cấp; ảnh effect tương ứng thiếu ở cả bốn zoom. Vì vậy tạm chặn phát mới qua ItemService, factory shop, mua hàng, sử dụng và giftcode; SQL ẩn shop 1015 và ghi mô tả tạm ngưng. Giữ ID mẫu và khả năng đọc đồ cũ vì catalog dùng chỉ số theo ID; không xóa tài sản người chơi hoặc tài nguyên có thể dùng chung. SQL live không có shop/giftcode phát 1015 tại thời điểm rà soát. Kiểm thử RED xác nhận bản cũ vẫn tạo được 1015; GREEN xác nhận chặn tạo/dùng/mua/gift, vẫn đọc được đồ cũ và tạo Hổ 942. Migration chạy hai lần trên schema thử, ID 942/1015/1016 và dữ liệu Hổ vẫn giữ nguyên.

## Namek

Bản sửa đã qua kiểm tra chức năng và SQL riêng: menu hai lựa chọn; chặn index bột mỳ cũ; xác minh người gọi, khu, thời hạn; giữ phiên khi túi không đủ chỗ hoặc giao dịch đang chờ. Chuẩn bị thưởng cho mọi thành viên trước khi thay túi, khóa người chơi theo ID. Thành viên offline cập nhật riêng items_bag bằng giao dịch với receipt theo phiên/người chơi để không phát lại khi thử lại sau lỗi commit. Chụp thành viên bang lúc gọi rồng, chặn gọi lần hai trước reset cuộc săn và giữ xử lý pending qua bảo trì.

Bằng chứng kỹ thuật: .analysis/dragon-resource-audit.md, .analysis/disabled-dragon-tests/, .analysis/disabled-dragon-sql-test.log và SRC/test/nro/models/services/shenron/NamekWishTest.java. Toàn bộ source Java và JAR candidate đã qua 223 nhóm hồi quy; Namek SQL trên candidate cuối cũng qua.
Rà soát bổ sung phát hiện việc publish thay thế Item đang có; đã tái hiện RED và sửa để giữ nguyên danh sách túi, Item và các option đang có, cập nhật đúng quantity của stack và chỉ thêm Item mới vào ô trống. Kiểm thử giữ identity và stack đã qua. Hai lượt review không còn phát hiện nghiêm trọng trong phạm vi sửa.

## Triển khai

Đã bảo trì có lưu dữ liệu, chờ server dừng, tạo snapshot SQL, chạy hai migration idempotent và đồng bộ bốn SQL exports. JAR cũ: 20-before-dragon-cleanup-20261007-203134.jar. SHA-256 cũ: df410a864f13cbdb586f2a34685b57fe43813f7015f3c9a4519754e6e445028b. SHA-256 mới: 0b5021ec6996d7cf319661a034816fe17d74c7fd7d3dfbd31032c6a430a3e3dd. Metadata: .analysis/dragon-cleanup-deployment-latest.json. Server đã mở lại cổng 14445 với PID 11032. Kết quả kiểm tra sau cài tại .analysis/dragon-cleanup-installed-verification.log.

Khôi phục dùng bảo trì lưu dữ liệu rồi restore JAR backup; giữ dữ liệu người chơi và các receipt hiện tại, không ghi đè dữ liệu mới bằng snapshot cũ. Chưa thao tác gọi rồng hoặc quan sát Hổ bằng client Unity trong lượt này.

Xác minh sau cài: đủ 223 nhóm kiểm thử PASS trên JAR đang dùng; hash JAR mới, backup và snapshot khớp metadata. SQL giữ nguyên Hổ 942 và item kế tiếp 1016, không có shop 1015 đang bán, mô tả UTF-8 đúng; bảng receipt và player dùng InnoDB. Server PID 11032 vẫn lắng nghe cổng 14445. Các cảnh báo tile thiếu giống phiên cũ đã được báo ở tác vụ rương/phó bản, không phát sinh từ thay đổi điều ước.
