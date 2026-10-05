# Nhận ngọc ở nhà — triển khai 03/10/2026

Đã cập nhật `SRC/20.jar` và source. Gohan, Moori, Paragus ở ba nhà dùng chung điều kiện: số dư ngọc xanh từ 0 đến 9.999.999 được cộng thêm 10.000.000; từ 10.000.000 trở lên thì từ chối. Ví dụ 9.000.000 → 19.000.000. Khi tiêu xuống dưới ngưỡng lại được nhận tiếp.

Đã sửa lời thoại và test ba nhà, biên ngưỡng, bấm lặp, bấm đồng thời. Giữ kiểm tra đúng map/menu, khóa theo người chơi, cập nhật DB và gửi tiền về client. Không đổi schema hoặc số dư có sẵn.

- Test trước sửa: tái hiện lỗi nhận ở số dư 1 và bấm đồng thời.
- Test sau sửa và bản JAR: 74/74 đạt; kiểm tra archive đạt.
- Backup: `SRC/jar-backup/20-before-home-gem-claim-20261003-073346.jar`.
- SHA-256 backup: `fdf48ab1643764092a0c3b9cf7dc03b74bcf7e16cb64a2cfa9fed5532cafcb2f`.
- SHA-256 mới: `f001a688213d673fc6b83e94796eb81cc464be58bbbcdf1de40842e14e04add3`.
- Server đã dừng trước khi thay JAR; lần triển khai này không có server đang chạy nên không cần gửi bảo trì.
- Sau cài: PID 1648 mở cổng 14445, kết nối TCP thành công; log tải DB và khởi động thành công. Các cảnh báo 7 map thiếu tile vẫn là các thiếu hụt đã ghi nhận trước đó.
- Đối chiếu bytecode JAR đã cài: kiểm tra dưới ngưỡng và dùng phép cộng.

Metadata: `.analysis/home-gem-claim/deployment.json`. Log kiểm tra: `.analysis/home-gem-claim/build.log`, `test-before.log`, `server-console.log`.

Chưa kiểm tra thao tác nhận ngọc rồi đăng xuất/đăng nhập bằng client thật. Test offline không xác nhận được tương tác client và việc lưu số dư thực tế qua phiên đăng nhập; lời gọi lưu DB hiện có được giữ nguyên.
