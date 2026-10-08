# Kết quả triển khai Super Broly và Mabu mập

Ngày 08/10/2026, giờ Bangkok. Bản mới đã cài sau bảo trì lưu dữ liệu, xác nhận JVM cũ dừng rồi mới thay JAR. Server khởi động lại lúc 11:20:23, PID 4172, mở cổng 14445.

## Hành vi đã triển khai

- Super Broly: trứng item 568 có tỷ lệ 5%, bình Killis item 1795 có tỷ lệ 3% khi chủ thưởng đủ điều kiện. Giữ phần thưởng đệ thường hiện có.
- Mabu mập mới: map 52, một boss toàn server, HP 300 triệu, outfit 297/298/299. Không ảnh hưởng Mabư cũ và võ đài cá nhân map 129.
- Bốn chiêu Cell cấp 7: Kamejoko, Dịch chuyển tức thời, Galick, Thái dương hạ san; thêm Khiên năng lượng cấp 7, 45 giây, cooldown 100 giây; không có Tái tạo năng lượng.
- Đòn gây sát thương lấy 10% HP tối đa của từng người bị đánh, xuyên giáp/khiên/Giáp Xên. Giữ né và miễn nhiễm hồi sinh. Người chơi đánh Mabu không bị giới hạn thường trực; cả đường phản sát thương đã bỏ giới hạn 1% của boss chung. Khiên đang bật mới giảm damage theo luật khiên hiện có.
- Mabu: trứng 30%, bình 15%; mỗi lần hạ có một trang bị theo 60% cấp 9–12 có ấn / 30% Thần Linh / 10% Thần Linh có ấn. Thần Linh dùng chỉ số gốc, đúng hành tinh; mỗi món có ấn chỉ có một ấn.
- Hai món hiếm roll độc lập. Điều kiện bình được chốt từ trước khi phát thưởng: trứng trong túi/rương cá nhân hoặc đang ấp, hoặc đệ hiện tại là Mabu. Không tiêu hao trứng/đệ, không yêu cầu 40 tỷ sức mạnh/3.000 Killis để rơi. Bình mới có option 250 = 0 trước khi xuất hiện trên map; điều kiện sử dụng bình vẫn giữ như trước.
- Đệ kết liễu được tính cho chủ; đồ dưới đất có chủ riêng. Túi đầy cần dọn túi để nhặt trước khi đồ hết hạn. Không phát thưởng hai lần do callback hoặc lỗi gửi packet.
- Mabu thoại bảo vệ 2 giây rồi chiến đấu tối đa 20 phút. Khu trống liên tục 5 phút thì rút sớm. Chết/rút xong nghỉ 10 phút; thiếu khu thử lại sau 10 giây. Cleanup thất bại giữ thế hệ cũ để retry, không spawn đè. Rút không rơi quà.
- Mabu nằm trong bảng Boss, không phải Boss nhiệm vụ. Thông báo xuất hiện và chi tiết cả hai tab không hiện số khu. BOSS2 giữ layout và trả slot khu -1; BOSS1 giữ nguyên; SQL nội bộ vẫn lưu khu để chẩn đoán.

## Bằng chứng

- `python .analysis/build_mabu_release.py --db`: 93 nhóm kiểm thử và 3 nhóm SQL trên schema riêng đều đạt, source hash ổn định.
- Kiểm thử RED trước sửa: thiếu boss/vòng đời, client còn hiện khu, BOSS2 chưa mask khu và phản sát thương còn cap. Sau sửa các kiểm thử đạt.
- Kiểm thử tích hợp dùng constructor, initBase, ChangeMapService và SkillService thật: vào map, cast khiên 45 giây, khóa cooldown, phá khiên, hết khiên, đòn kết liễu, một trang bị, callback lặp và tháo boss khỏi manager/zone.
- Kiểm thử lỗi tháo map chứng minh manager vẫn gỡ boss, giữ đối tượng đang mắc trong zone, không thay thế trước cleanup, retry gỡ đầy đủ.
- JAR staging đã chạy lại cả 93 nhóm. Bản cài có SHA-256 đúng JAR staging; toàn bộ classes nguồn đã biên dịch đối chiếu byte-for-byte với JAR cài.
- Audit live SQL và binary: toàn bộ 2.099 part khớp; 107 icon liên quan với 428 PNG x1–x4, dữ liệu và ảnh hiệu ứng khiên, skill paint, projectile/dart và map 52 hợp lệ. Report `.analysis/mabu-release-resource-review.json`.
- Unity Win64 build thành công, 0 lỗi, 64 warning. Model và bảng boss Game1/Game2 đã chạy kiểm thử, gồm dữ liệu cũ có số khu vẫn không hiện khu.
- Probe chỉ đọc trên JVM mới: một Mabu ACTIVE, HP 300.000.000/300.000.000, map 52, đúng 5 skill, khiên đang hoạt động 45.000 ms, một notice trong category 2, Broly/Super không trùng khu. Snapshot `.analysis/mabu-release-runtime.txt`; số lượng boss có thể đổi khi đến thời hạn.

Chưa thử săn bằng hai tài khoản người chơi đăng nhập thực tế. Kiểm thử tích hợp dùng fixture cô lập dữ liệu và packet, không ghi quà hoặc nhân vật thử nghiệm vào database game. Audit tài nguyên và build không thay thế kiểm tra trận đánh trên client có đồ họa.

Log khởi động có cảnh báo tile map cũ 78/95/101/121/125/130/134 và một `ConcurrentModificationException` trong `MajinBuu14H.finish` khi danh sách zone đang được khởi tạo. Đây là đường phó bản cũ không thay đổi trong đợt này; server vẫn chạy và probe Mabu đạt. Không ghi nhận exception từ Mabu mới trong lần kiểm tra này. Không tuyên bố toàn bộ log server sạch lỗi.

## Bản cài và khôi phục

- JAR đang chạy: `SRC/20.jar`.
- SHA-256 mới: `e22f297599110b39c3f7faf16a11185c08e2c79b2b85016faf7db40cdaf1581b`.
- Backup trước cài: `SRC/jar-backup/20-before-mabu-release-20261008-111943.jar`.
- SHA-256 backup: `8fee2a88ed81d53ea97e3862cb1164aedf78bb6076f7602626558770a59bbd11`.
- Metadata đầy đủ: `.analysis/mabu-release-deployment-latest.json` và `SRC/jar-backup/mabu-release-20261008-111943.json`.
- Đã chụp SQL sau khi server cũ lưu xong; không chạy migration trong đợt này. Chỉ `20.jar` là app JAR ở thư mục chạy; thư viện và tool JAR giữ nguyên.
- Client: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/PC-Mabu-20261008-111232/NRO-Boss.exe`. Source client gốc cũng đã cập nhật phần ẩn khu.
- Khôi phục: bảo trì lưu dữ liệu, xác nhận JVM dừng, backup JAR hiện tại với tên mới rồi khôi phục JAR trên. Giữ dữ liệu người chơi/biên nhận hiện tại; không ghi đè bằng snapshot SQL cũ.

Kế hoạch thời hạn cho toàn bộ boss khác vẫn là tài liệu riêng, chưa áp dụng đồng loạt trong đợt này.
