# Đã sửa và triển khai danh hiệu — 04/10/2026

Server PID 13264, cổng 14445 hoạt động. JAR mới SHA-256 `244fc099202ca8c99b9714c81399bade728bb101a7dc7c7e2db8c60ca89cc089`. Bản cũ được lưu tại `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-badge-repair-20261004-175132.jar`, SHA-256 `be1056f6e334f6cda9d5fea0c556ab7ae70ddc42b8e96fe5418e52b442fd5e14`. Đã bảo trì lưu dữ liệu và dừng server trước khi thay JAR.

## Kết quả

- 105/105 ca kiểm tra mã nguồn và 105/105 ca chạy trên JAR mới đạt; thêm 9/9 ca KOL đạt.
- Cả Game1/Game2 của client Unity biên dịch thành công với compiler và cấu hình Unity 2022.3.62f2. Chưa chạy kiểm thử thao tác giao diện trên tài khoản thật sau cập nhật.
- Probe JVM xác nhận 19 mẫu danh hiệu/19 nhiệm vụ, ba mẫu thiếu và mapping thưởng đã đủ; KOL SD/HP/KI +10%. Kiểm tra dữ liệu và ảnh của 19 hiệu ứng đủ zoom 1–4.

## Thay đổi

1. Danh hiệu hết hạn không còn cộng chỉ số ngay cả trước lượt dọn định kỳ; gộp bản ghi trùng tất cả loại, chỉ một danh hiệu hoạt động; từ chối chọn danh hiệu không sở hữu/còn hạn mà không tắt danh hiệu đang dùng.
2. Cộng chỉ số chí mạng của Mẹ Rồng/Gõ đầu trẻ và né đòn của Ve chai/Móc sạch túi; giữ HP/KI lớn khi đổi. Buff đẹp chỉ cộng một lần cho bản thân và tác dụng lên người ở gần trong phạm vi 120.
3. Bò Mộng chỉ cộng một lượt khi trả thành công nhiệm vụ Siêu khó. Đã mở đủ lựa chọn Dễ/Bình thường/Khó/Rất khó/Siêu khó trong menu Bò Mộng vì trước đây chỉ có ba mức đầu.
4. Rương gỗ chỉ cộng khi mở thành công cấp 12; nâng đồ chỉ đếm lên +7; Ăn Trộm đếm một lần; Ve chai đếm nhặt thành công; Trùm ước rồng đếm hoàn tất điều ước 1 sao, không đếm gọi lại; X-mas đếm điểm sự kiện kiếm được.
5. Cao thủ siêu hạng nhận tiến độ ngay khi Top 1. Nước anh bao: thắng Xinbato đại hội 23 đủ 5 lần. KOL: hạ Mặt Trời một lần. Fan cứng: hoàn thành 7 nhiệm vụ KOL thường hoặc VIP tại Quy Lão.
6. Mẹ Rồng giữ điều kiện đủ 7 loại pet vĩnh viễn đang sở hữu; không tính bản trùng/đồ có hạn/đồ đệ tử đang mặc. Tất cả danh hiệu được cấp 30 ngày.
7. Tiến độ theo ngày Việt Nam thống nhất cho online qua nửa đêm và đăng nhập lại; ngày tiến độ lưu trong dữ liệu nhiệm vụ để không reset lặp khi đăng nhập. Mẹ Rồng/Fan cứng tính theo trạng thái sở hữu/chuỗi KOL hiện có.
8. Bổ sung ảnh và dữ liệu hiệu ứng Kẻ thao túng sói/Nước anh bao/Thánh ở dơ. Client xóa/thay/gia hạn hiệu ứng theo thông báo server ngay trong lượt cập nhật tiếp theo; gửi thời hạn 15 giây, làm mới mỗi 10 giây để không nhấp nháy.
9. Danh sách shop tạo bản sao riêng từng người, không sửa options shop chung; chỉ số/mô tả dùng mẫu danh hiệu đã chuẩn hóa.

## Dữ liệu cấu hình và giới hạn

Các mẫu/mapping thiếu được chuẩn hóa khi server nạp cấu hình, không sửa trực tiếp database người chơi hay dump SQL. Bản ghi danh hiệu trùng/hết hạn được sửa khi người chơi online; trạng thái sẽ được lưu bởi cơ chế lưu nhân vật hiện có. Không xóa các danh hiệu đã nhận trước đây chỉ dựa trên suy đoán chúng được nhận từ lỗi đếm cũ.

Mở lại Play mode/client để nạp thay đổi phía client nếu Unity chưa reload script. Chi tiết ca kiểm tra ở `.analysis/gold-economy-check/test.log`, `.analysis/badge-repair/kol-regression.log`; snapshot runtime ở `.analysis/badge-repair/badge-config-live.json`.
