# Triển khai chế tạo Thiên Sứ — 2026-10-07

Đã cài bản đã kiểm thử vào `SRC/20.jar`, khởi động lại server và xác nhận JVM/cổng 14445 cùng catalog thật. Thời gian triển khai theo Asia/Bangkok: 2026-10-07T21:56:54.303830+07:00. JVM mới: 15832.

## Hành vi đã triển khai

- Công thức: 999 mảnh cùng loại + 1 công thức thường hoặc VIP + 1 đá nâng cấp + 1 đá may mắn + 50 hồng ngọc; không thu vàng. Mặc đủ 5 món Hủy Diệt.
- Cả thành công và thất bại theo tỷ lệ đều tiêu món Hủy Diệt đang mặc tương ứng loại đồ tạo ra, đủ nguyên liệu và 50 hồng ngọc. Giữ bốn món còn lại; muốn làm tiếp phải mặc bù đủ bộ. Thành công trả một món vào túi.
- Chỉ số chính đồ mới tăng 1–30% so với nền factory đã gồm ưu thế hành tinh. Chỉ áp dụng đúng option giáp/HP/sức đánh/KI/chí mạng; có làm tròn theo đơn vị nghìn hoặc số nguyên và kẹp trần 30%. Không nhân các dòng phụ thêm 30%.
- Dòng phụ giữ xác suất có bonus và 1–3 dòng khác nhau theo đá cũ; mỗi dòng 1–7%, chọn sức đánh/HP/KI/giảm sát thương/sức đánh chí mạng. Bỏ marker option 15 sai nghĩa.
- Xem yêu cầu và hủy không tạo đồ hoặc trừ tài sản. Bảng xác nhận ghi tên kết quả/hành tinh, tỷ lệ, phí và món Hủy Diệt mất kể cả thất bại. Thiếu điều kiện, túi đầy, nguyên liệu/trang bị đã đổi hoặc lỗi chuẩn bị/nhận đồ không trừ tài sản.
- Trừ túi, trang bị và hồng ngọc dưới cùng khóa người chơi với PlayerDAO; mỗi xác nhận dùng một lần. Cập nhật bộ trang bị, chỉ số, ngoại hình, túi và tiền sau kết quả. Sửa dispatcher để xác nhận cũ không xóa bảng xác nhận mới ngoài khóa.
- Giữ chính sách 80 tỷ sức mạnh/khóa giao dịch và mọi chỉ số/metadata của đồ cũ. Không đổi phó bản/capsule/hỗ trợ nhiệm vụ hoặc giá nguyên liệu trong SQL.

## Kiểm chứng

- Biên dịch toàn bộ source Java17, chạy 229 nhóm kiểm thử trên classes và JAR staging cuối.
- Kiểm thử mới: 15 template và toàn bộ 30 mức bonus; mọi lượt rút may mắn 0–50/các cấp đá; hàm tạo đồ thật có ưu thế hành tinh; công thức/hành tinh/5 ô trang bị; thành công/thất bại; thiếu tài sản/túi đầy/stale; lỗi factory/grant; giao dịch và pending; bấm trùng/đồng thời; hủy; refresh trang bị/chỉ số; metadata đồ không liên quan; dispatcher cleanup dưới khóa.
- Có RED→GREEN cho lỗi preview trừ vàng, sao chép túi làm mất metadata đồ cũ, guard wallet cùng tài khoản, tên kết quả trên preview và cleanup dispatcher ngoài khóa. Log tại `.analysis/thien-su-craft`.
- Rà soát yêu cầu và chất lượng độc lập đã duyệt; lỗi đồng thời tìm thấy trong rà soát đã sửa trước bản cuối.
- INSTALLED THIEN SU CRAFTING PASS 15 groups; backup and snapshot hashes verified
- Agent chỉ đọc trong JVM mới xác nhận các class chế tạo được tải từ JAR hiện hành, phí 50 hồng ngọc, đủ catalog template/options. Không thử bằng tài sản người chơi thật.
- Hai client Game1/Game2 nhận được giao thức/menu/option hiện tại; đủ icon 15 món ở x1–x4. Xem `docs/thien-su-crafting-client-audit-20261007.md`.

Giới hạn: chưa thao tác và kiểm tra hình ảnh/menu trực tiếp trong Unity vì phiên công cụ không có điều khiển desktop/Unity. Các cảnh báo biên dịch test về Unsafe có sẵn trong fixture; không có lỗi biên dịch hoặc kiểm thử.

## Backup và phục hồi

- JAR cũ: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-thien-su-crafting-20261007-215640.jar`.
- SHA-256 cũ: `f06c7bc977378926f01591265690b16b6a0b03c135ac54599d2080727ab7a8cc`.
- SHA-256 mới: `13d4a90762111edce5dc683c49655fcaaefa1c89bbf4bcfed98331c6d603a030`.
- Snapshot SQL sau khi server lưu dữ liệu và dừng: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\.analysis\thien-su-crafting-deploy-20261007-215640\team2026-before-thien-su-crafting.sql`.
- Metadata triển khai: `.analysis/thien-su-crafting-deployment-latest.json` và bản có thời gian tại `SRC/jar-backup`.
- Chỉ còn JAR ứng dụng hiện hành trong thư mục chạy; giữ thư viện/tool cần dùng. Nếu phục hồi, dùng bảo trì có lưu dữ liệu rồi thay bằng JAR backup; giữ dữ liệu người chơi hiện tại, không tự khôi phục SQL snapshot làm mất giao dịch sau triển khai.
