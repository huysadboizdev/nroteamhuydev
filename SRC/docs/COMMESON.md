# Thử thách Commeson

Jaco tại sân bay 24 đưa người chơi tới map 139; Potage tại map 140 mở trận đánh bản sao.
Mỗi nhân vật được mở thành công một trận mỗi ngày theo UTC+7, gồm cả tài khoản quản trị.
SQL ghi RESERVED trước khi tạo bản sao và chuyển FIGHT sau khi tạo thành công; lỗi trước khi bắt đầu chỉ xóa RESERVED để thử lại. RESERVED còn sót sau lỗi SQL/khởi động lại được phục hồi, không xóa lượt FIGHT đã bắt đầu.
`player.lasttimepkcommeson` cũ vẫn được đối chiếu; khóa duy nhất `(player_id, attempt_day)` chống hai phiên cùng mở lượt.

Trận có giới hạn 5 phút. Bản sao chỉ nhận sát thương từ chủ trận, cố định tại khu khởi tạo và có một PKCommeson.
Thua/hết giờ xóa bản sao, bỏ PK và đưa về sân bay 24+giới tính bằng tàu; người chết được hồi sinh đủ HP/KI.
Thắng tạo một bình 638 với option 93:30 và 30:0, giữ chủ sở hữu đến khi biến mất.
Người chơi có 60 giây tính từ chiến thắng để nhặt bình. Hành trang đầy không tính là nhặt.
Nhặt thành công mở một khoảng 60 giây mới trước khi tàu đưa về; không nhặt kịp thì bình bị xóa và tàu đưa về.
Capsule chỉ hủy lịch khi thật sự rời map; rời khi đang đánh được tính thua.

Mỗi lần nhặt ghi `player.items_bag` và biên nhận PICKED trong cùng giao dịch InnoDB.
Lỗi SQL đã xác nhận rollback khôi phục hành trang và bình để thử lại trong thời hạn ban đầu.
Mất phản hồi commit được đối chiếu SQL; trong lúc chưa xác định, giao dịch/vòng cập nhật/lưu nhân vật bị khóa và bộ phục hồi thử lại.
Đăng nhập lại không mở lượt mới, không phát thêm bình đã nhặt và không gia hạn thời gian.
WON/PICKING chưa có giao dịch nhận bình được phục hồi một bình có cùng mã trận và thời hạn cũ; FIGHT bị ngắt chuyển sang lịch về.

Áp dụng `sql/migrations/20261005_commeson_attempt.sql` trước khi chạy JAR có thay đổi này.
Thiếu bảng/lỗi SQL từ chối mở trận hoặc nhận bình. Không dùng dữ liệu trong RAM để cấp thêm lượt.
Kiểm chứng: `.analysis/verify_commeson.py` kiểm tra bản candidate trong `consign-latest.json`; `--source` biên dịch mã hiện tại.
Các bài SQL tạo cơ sở dữ liệu riêng có tên commeson_spec_* rồi xóa; không sửa dữ liệu game.
