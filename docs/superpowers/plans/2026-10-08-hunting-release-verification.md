# Phát hành Broly, bảng boss và hai bộ ba săn đồ

Phạm vi được người dùng duyệt: triển khai cả ba phần. Sáu boss mới không có Tái tạo năng lượng; cơ chế tăng trưởng của Broly giữ nguyên.

## Hành vi đã triển khai

- Broly/Super dùng quyền sở hữu khu theo thế hệ. Khu có Super không nhận Broly khác; Broly sống đạt ít nhất 1.500.000 HP tối đa chuyển sang Super một lần. Chết/rời khu bình thường không sinh Super. Lỗi chuyển dạng khôi phục Broly, dọn Super và đệ tử dở dang.
- Client Game1 và Game2 có bảng Boss nhiệm vụ và Boss riêng, mỗi bảng có hiện tại/lịch sử. Phân loại theo ID nhiệm vụ và dạng boss, không đoán theo tên. BOSS2 có phân loại/khu/hỗ trợ nhiệm vụ; BOSS1 giữ nguyên định dạng. Migration chỉ bổ sung cột và phân loại các ID lịch sử xác định được.
- Basil/Lavender/Bergamo và Pi Láp/Mai/Su: mỗi con 1.000 HP, đòn hợp lệ của người chơi trừ 1 HP; đòn boss trừ 5% HP tối đa của đúng người nhận, xuyên giáp/khiên/giáp Xen. Né và miễn nhiễm vừa hồi sinh vẫn áp dụng.
- Ba thành viên được bảo vệ trong ba câu thoại, cùng mở PK khi nhóm chuyển ACTIVE. Bộ chiêu Xên Hoàn Thiện gồm Kamejoko, dịch chuyển tức thời, Galick và Thái dương hạ san; không có hồi năng lượng.
- Phạm vi: Trái Đất 39/0/1/2/3, Namek 40/7/8/9/10, Xayda 41/14/15/16/17. Mỗi loại có một nhóm toàn server; hạ hết nhóm hồi lại sau 10 phút; khu vắng liên tục 5 phút thu hồi nhóm.
- Mỗi con chết rơi một trang bị: 60% đồ shop cấp 9–12 có ấn, 30% Thần Linh thường, 10% Thần Linh có ấn. Chọn theo hành tinh người được ghi nhận; đệ tử ghi nhận cho chủ. Đồ Thần Linh dùng chỉ số gốc canonical, không lấy các shop đã tăng chỉ số.
- Phụ thưởng mỗi con: vàng 10.000–30.000 và capsule hồng ngọc 1–3; 70% thêm đá 220–224 số lượng 1–2; 30% thêm ngọc rồng thường 14–20. Thưởng phát một lần; chỉ số/ấn được gắn trước khi item công bố trên map.

## Bằng chứng kiểm chứng

- RED trước sửa: lớp/tính năng mới chưa có trong JAR; Broly vượt ngưỡng không chuyển dạng; kiểm thử công bố item trước options và chỉ số đồ thần từ shop phát hiện hành vi sai.
- Biên dịch toàn bộ Java với `--release 17`; chạy suite nguồn và trực tiếp trên JAR ứng viên. Kiểm thử database chạy schema tạm riêng, gồm migration lặp, lịch sử, outbox và chống bản ghi cũ ghi đè phân loại mới.
- Unity 2022.3.62f2: build Windows 64 bit thành công, 0 lỗi biên dịch/build. Kiểm thử thực tế cả hai bảng với BOSS1/BOSS2, dữ liệu cũ, trang/phân loại, reset tài khoản, bootstrap throttle và độc lập giữa hai tab đã qua.
- Audit 18 parts, 247 icon/portrait, 988 PNG x1–x4: có đủ, CRC/giải nén hợp lệ, parts runtime khớp SQL, index tối đa 6.979 trong giới hạn client; đủ tile data của 15 map.
- Smoke khởi động client không đăng nhập: tải assembly và đọc host 127.0.0.1:14445; không có managed exception trong log. Chế độ `-nographics` có cảnh báo shader do Null graphics device; không dùng kết quả này để khẳng định mọi hiệu ứng hình ảnh đã được kiểm thử trong trận.
- Review độc lập phát hiện lỗi cleanup khi exitMap ném exception; đã sửa và kiểm thử cả lỗi exit lẫn detach bị chặn. Lượt mới không ghi đè tham chiếu thế hệ chưa dọn xong.
- Probe server thật phát hiện caller vào map thiếu gán khu nguồn trước khi gọi `ChangeMapService`. Đã đối chiếu với `Boss.joinMap`, bổ sung kiểm thử qua hàm chuyển map thật cho sáu boss, Broly và Super; xác nhận RED đúng NPE ở dòng 472 rồi GREEN sau sửa. Không sửa hàm chuyển map dùng chung. Đồng thời bổ sung guard người đánh/skill null cho cả hai dạng Broly.
- Đã cô lập thông báo boss trong fixture: tracker chỉ ở bộ nhớ, outbox thư mục tạm, DAO từ chối mọi kết nối SQL. 48 bản ghi của tám JVM kiểm thử được sao lưu SQL riêng rồi xóa theo đúng run ID đã xác định; không sửa bảng người chơi. Kiểm thử mới xác nhận thông báo thật không thoát khỏi fixture.
- Bản server cuối SHA-256 `8fee2a88ed81d53ea97e3862cb1164aedf78bb6076f7602626558770a59bbd11`, PID 17436 mở cổng 14445. Probe sau khởi động xác nhận hai nhóm ACTIVE, đủ sáu boss đúng HP/PK/skill/map, Broly chiếm 30 khu và không trùng Broly/Super. Các sự kiện vẫn tắt, không có handle sự kiện hoặc cleanup đang chờ.
- Backup ngay trước bản cuối `20-before-hunting-release-20261008-103721.jar` là bản trung gian có lỗi admission vừa được phát hiện. Nếu cần quay lại trước tính năng, dùng `20-before-hunting-release-20261008-103143.jar` (SHA-256 `2cafb5bf976ef178c5fe6f4594c0f9a043fdcbdd8d2b3f400be91e0989662dc6`) với bảo trì lưu dữ liệu; giữ dữ liệu người chơi hiện tại, không ghi đè bằng SQL snapshot cũ.

Metadata cuối cùng, SHA-256, backup JAR, SQL snapshot, số nhóm test và runtime probe nằm trong `.analysis/hunting-release-deployment-latest.json` cùng `SRC/jar-backup/hunting-release-*.json` sau triển khai. Không đánh dấu các kiểm thử thủ công chưa chạy (hai tài khoản đánh/nhặt thật và xem toàn bộ animation) là đã qua.
