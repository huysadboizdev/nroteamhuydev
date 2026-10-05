# Giftcode tại ba NPC ở nhà — 03/10/2026

Đã thêm nút **Nhập giftcode** sau Nhận đệ tử và trước Đóng tại Gohan (nhà 21), Moori (nhà 22), Paragus (nhà 23). Nút mở form giftcode hiện có và gửi mã qua Input.GIFT_CODE tới GiftCodeService. Quy tắc xét code và phần thưởng dùng chung với Santa.

Test kiểm tra menu bốn mục, nội dung packet form, chuyển mã tới đúng service/người chơi ở cả ba nhà, nút Đóng và các guard map/menu. Test mới thất bại trên JAR cũ; bản mới đạt 75/75 bài kiểm tra. Đã chạy lại test giftcode trực tiếp trên JAR đã cài, kết quả PASS. Chưa thử nhận thưởng bằng client thật.

Server đã được đưa vào bảo trì bằng `bt` và chờ thoát trước khi thay JAR. Backup: `SRC/jar-backup/20-before-home-giftcode-20261003-074026.jar`.

- SHA-256 backup: `f001a688213d673fc6b83e94796eb81cc464be58bbbcdf1de40842e14e04add3`.
- SHA-256 JAR mới: `f6c28406070ddbfc2e25dd32a280ca7284664108780333f16d179db67af08417`.
- Server mới PID 16612, cổng 14445; kết nối TCP thành công, log khởi động thành công.

Metadata và log: `.analysis/home-giftcode/deployment.json`, `build.log`, `test-before.log`, `server-console.log`. Các cảnh báo map thiếu tile là các thiếu hụt đã ghi nhận trước đó.
