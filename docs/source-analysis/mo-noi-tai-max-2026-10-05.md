# Mở nội tại max

Trong menu Nội tại chọn **Mở max**, rồi xác nhận **Mở max**. Giá100 hồng ngọc mỗi lượt; sức mạnh tối thiểu10 tỷ. Không yêu cầu thành viên riêng. Giữ hai nhánh Mở Nội Tại/Mở VIP và giá vàng hiện tại (countOpen không đổi khi dùng max).

Random một loại đúng hành tinh hiện tại, gồm nội tại chung. Có thể quay lại loại đang sở hữu. Chỉ số nhận được bằng param_to_1/param_to_2 theo SQL; loại hai thông số đạt max cả hai. Không cộng dồn nội tại. Nội tại vàng rơi từ quái ID23 đạt300%, nội tại ID3 Trái Đất đạt35/35.

Pool dedup theo ID, không có ID0, không phụ thuộc thứ tự hàng SQL. Trái Đất10 loại, Namếc11 loại, Xayda10 loại; mỗi loại xác suất bằng nhau. Bảng intrinsic hiện có8 hàng trùng mỗi ID, giữ nguyên bảng và các nhánh cũ; nhánh max tự tạo pool hợp lệ. Cấu hình trùng mâu thuẫn, rỗng hoặc khác hành tinh bị từ chối trước khi thu tiền.

Menu gồm5 nút: Xem tất cả Nội Tại / Mở Nội Tại / Mở VIP / Mở max / Từ chối. Xác nhận gắn session/planet, hạn120 giây, dùng một lần; đóng hoặc bấm lại không phát sinh lượt quay. Một lần mở menu xác nhận mới là một lượt mua mới. Không cần cập nhật client để hiển thị menu server hiện có.

Ruby và data_intrinsic được lưu cùng inventory/container/receipt trong transaction ConsignDAO action intrinsic_max. Chỉ publish sau commit. SQL lỗi giữ nguyên tiền và nội tại; commit mất ACK khóa pending rồi đối soát. Mở thường/VIP/đổi nội tại cũng tôn trọng pending để không ghi đè kết quả. Đăng xuất chờ đối soát và lưu kết quả đã publish. Giữ đủ8 trường nội tại theo format PlayerDAO gồm countOpen và trạng thái cooldown. Packet112, số dư và chỉ số cập nhật sau thành công; lỗi thông báo không tạo thêm lượt.

46 nhóm kiểm tra đạt trên candidate, gồm SQL thật ở database riêng, dedup/max đúng toàn bộcatalog10/11/10, sức mạnh/phí, rollback/mấtACK, token/session/gender/expiry/cancel/replay, lỗi gửi thông báo, pending mở thường/VIP, logout giữ kết quả, cùng hồi quy cửa hàng/ký gửi/mở thành viên. Review bổ sung không tìm thấy blocker. Chưa thao tác quay bằng tài khoản thật trong client.

Không cần migration schema hoặc sửa thông số max trong SQL. Metadata triển khai: `.analysis/intrinsic-max-deployment-latest.json`.

## Triển khai

Đã cài sau bảo trì lưu dữ liệu và snapshot SQL. SHA-256: `c190cc7f4fe69a62e2e576ea0e5d3aae9b811ce4bebd5002ec53bdcc630975d6`. Backup: `SRC/jar-backup/20-before-intrinsic-max-20261005-130314.jar`. Server đã khởi động và kết nối TCP14445 thành công. Schema player/receipt thực tế đáp ứng dữ liệu và transaction mới. Không thay đổi bảng intrinsic hoặc tự quay cho tài khoản thật.
