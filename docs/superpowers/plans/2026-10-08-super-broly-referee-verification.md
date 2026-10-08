# Super Broly và Trọng Tài — 08/10/2026

## Yêu cầu đã chốt

- Mabu mập tại Đại hội võ thuật phải xuất hiện trong mục Boss.
- Tự phát nổ (skill thứ 5 Xayda, ID 14) gây cố định 10.000 HP lên Super Broly; giữ đấm và phản sát thương, chặn sát thương từ các chiêu khác.
- Trọng Tài ở bản đồ 52 không đổi vị trí khi người chơi đổi khu.

## Thực hiện

- Kiểm tra Mabu đang chạy: một boss, HP tối đa 300 triệu, bản đồ 52, thông báo thuộc nhóm Boss (category 2). Khi boss nghỉ, danh sách đang xuất hiện không hiển thị nó; lịch sử vẫn giữ lượt đã xuất hiện.
- Super Broly kiểm tra loại chiêu trước khi nhận sát thương hoặc tăng chỉ số. Bom hợp lệ gây 10.000 HP, sát thương thực tế không vượt HP còn lại; đòn trượt không gây sát thương. Giữ giới hạn đấm và cách xử lý phản sát thương hiện có.
- Truyền ID chiêu thực thi theo từng đòn qua ngữ cảnh thông báo boss. Chiêu đặc biệt đang chạy lấy ID đã lưu của chính lượt tung chiêu; đổi chiêu chọn không biến nó thành đấm/bom. Đấm hoặc bom thật trong lúc hoạt ảnh đặc biệt còn chạy vẫn hoạt động.
- Ngữ cảnh đòn đánh dùng ThreadLocal, khôi phục ngữ cảnh trước đó trong finally; giữ ghi nhận người hạ boss và kết thúc lượt khi chết.
- Trọng Tài NPC -114 tại tất cả khu bản đồ 52 dùng tọa độ (373, 336). Vị trí di chuyển của trọng tài trong các bản đồ thi đấu riêng giữ quy trình hiện có.

## Kiểm tra trước triển khai

- Biên dịch toàn bộ source; 95 nhóm kiểm tra Java và 3 nhóm kiểm tra SQL trong schema thử riêng đều PASS.
- Kiểm tra bom 10.000 HP, chặn beam/chiêu đặc biệt, đấm/phản sát thương, null/miss/rời khu.
- Kiểm tra đường gọi thực tế playerAttackPlayer: chiêu đặc biệt bị chặn dù đổi sang Galick/Tự phát nổ; đấm và bom thật vẫn được nhận.
- Kiểm tra khởi tạo NPC thực tế ở khu 0, 1, 9 có cùng tọa độ.
- Review độc lập: không phát hiện lỗi nghiêm trọng còn lại sau sửa lỗi đổi chiêu trong lúc tung chiêu đặc biệt.

## Triển khai và giới hạn

Metadata build/deploy nằm trong `.analysis/super-referee-release-*-latest.json`; backup và metadata khôi phục trong `SRC/jar-backup`. Chỉ thay JAR sau khi bảo trì lưu dữ liệu và JVM đã dừng; giữ schema SQL đang dùng. Kiểm tra bản đóng gói trước khi cài và đọc trạng thái server sau khi khởi động.

Không thay đổi tài nguyên hoặc giao thức client trong bản sửa này. Chưa kiểm tra chiến đấu bằng hai tài khoản thật; kiểm tra chiến đấu dùng fixture và đọc trạng thái server không gây sát thương lên người chơi đang online.

## Kết quả sau triển khai

- Server mới PID 14568 mở cổng 14445, khởi động lúc 12:08 ngày 08/10/2026 (Bangkok).
- Probe chỉ đọc xác nhận một Mabu mập ACTIVE, HP 300.000.000, bản đồ 52, thông báo nhóm Boss; Khiên năng lượng đã bật với thời lượng 45 giây.
- Cả 10 khu của bản đồ 52 có Trọng Tài tại (373, 336); Broly/Super không trùng khu.
- Đã kiểm tra lại SHA-256 backup/bản mới và đối chiếu toàn bộ lớp trong JAR với bản biên dịch đã kiểm tra.
- Log khởi động còn cảnh báo thiếu tile cũ ở các bản đồ 78/95/101/121/125/130/134; các bản đồ này không thuộc phần sửa hiện tại.
