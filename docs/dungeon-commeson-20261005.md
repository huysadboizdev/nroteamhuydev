# Phó bản và nhân bản Commeson – 05/10/2026

Trạng thái: đã cài SRC/SQL/JAR mới và khởi động lại server sau bảo trì có lưu dữ liệu.

## Quy tắc

- Mỗi nhân vật một trận Commeson bắt đầu thành công/ngày, theo UTC+7. Đi tới Potaufeu không mất lượt. Thua, hết giờ hoặc bỏ trận đã bắt đầu vẫn tính lượt.
- Trận đánh tối đa 5 phút. Nhân bản gắn với chủ trận và khu đã chọn, không đi theo chủ sang bản đồ khác.
- Thua: dọn trận, hồi sinh đầy HP/KI nếu đã chết và tàu đưa về trạm tàu của hành tinh.
- Thắng: một bình chứa Commeson (638), chỉ chủ trận được nhặt. Chờ nhặt tối đa 60 giây từ lúc thắng.
- Nhặt thành công: bắt đầu 60 giây mới để tàu đón. Túi đầy không tính là đã nhặt. Hết hạn chưa nhặt thì dọn bình và đưa về.
- Dùng Capsule ra trước thì hủy đếm giờ. Đếm giờ cũ không kéo người chơi khỏi bản đồ mới.
- Lượt ngày và việc nhận bình lưu SQL; đăng nhập lại hoặc khởi động lại server không tạo lượt hay thưởng trùng.

## Lỗi đã tái hiện

Kiểm thử trên JAR trước cập nhật tái hiện bốn lỗi: Kho báu, Rắn độc và Khí gas tiếp tục chạy tick sau đóng; Doanh trại lỗi khi dọn phiên chưa có clan. Log: `.analysis/dungeon-timeout-red/result.log`.

Rà soát thêm phát hiện lệnh chuyển map trả về void, nên gọi tàu không đồng nghĩa người chơi đã rời map. Các phiên cần giữ trạng thái đang đóng và thử lại những người chưa chuyển thành công. Lỗi của một người không được bỏ qua những người còn lại.

Hai nhánh Capsule thiếu break cần sửa để tránh gửi danh sách chuyển map sai hoặc mở giao diện hai lần. Khôi phục Commeson cần nối vào đăng nhập thật, không nối vào hàm đọc nhân vật offline.

## Thời gian giữ nguyên

Kho báu, Rắn độc và Khí gas giữ 30 phút cùng thời gian chờ cuối hiện có. Doanh trại giữ 30 phút đánh và 5 phút nhặt thưởng từ lúc bắt đầu pha thưởng. Mabư và Sao đen giữ lịch sự kiện; Siêu thần thủy giữ thời hạn thử thách. Dùng lại giao thức tàu của client.

## Kiểm tra và triển khai

Chạy kiểm thử thời gian, kết thúc phiên, lỗi chuyển map, túi đầy, giao dịch SQL, bấm nhiều lần, đăng nhập lại và hồi quy các chức năng đã sửa trước đó. Trước cài JAR phải bảo trì có lưu dữ liệu, xác nhận server dừng, snapshot SQL, chuyển JAR cũ vào `SRC/jar-backup`, kiểm tra SHA-256 và ghi metadata phục hồi.

## Kết quả cuối

- Bản dựng và 61 nhóm kiểm thử đạt; riêng phó bản có 22 tình huống. Bao gồm SQL Commeson thực trong database kiểm thử riêng, giao dịch nhận bình nguyên tử, rollback/mất phản hồi commit, reconnect, giới hạn ngày và chặn giao dịch khi đang đối soát.
- SQL live có bảng `commeson_attempt`, khóa duy nhất theo nhân vật/ngày và mã trận. Server PID 14960 nghe port 14445; kiểm tra trong JVM xác nhận có service mới, 4 danh mục phó bản và đủ bản đồ thử thách/đưa về.
- JAR hiện dùng: `SRC/20.jar`, SHA-256 `0b5ce1a93e05e83c7e4c40e1a9b7278fc18a7a682b63e05b57c7dd0945bf0ae6`.
- JAR cũ: `SRC/jar-backup/20-before-dungeon-commeson-20261005-165811.jar`. SQL trước cập nhật: `.analysis/dungeon-commeson-deploy-20261005-165811/team2026-before-dungeon-commeson.sql`. Metadata: `.analysis/dungeon-commeson-deployment-latest.json`.
- Chưa kiểm thử trực tiếp thao tác đánh trận và quan sát hoạt ảnh tàu bằng client Unity sau cập nhật.
