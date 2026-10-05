# Đệ tử cộng đều HP, KI, sức đánh theo lô

Đã triển khai theo yêu cầu ngày 02/10/2026. Người dùng đã duyệt tăng nhanh khi dư tiềm năng và cho phép cập nhật JAR/server.

## Quy tắc đã duyệt

- Đệ tử chỉ nâng HP, KI và sức đánh; chặn nâng giáp/chí mạng bằng tiềm năng. Giữ chỉ số đã có và hiệu ứng trang bị/set/buff.
- Mỗi lượt cập nhật có tối đa 20 vòng; mỗi vòng nâng cùng số điểm cho các chỉ số đủ điều kiện, tối đa 1.000 điểm mỗi chỉ số. Tối đa 20.000 điểm mỗi chỉ số/lượt cập nhật.
- HP/KI mỗi điểm tăng 20, sức đánh mỗi điểm tăng 1. “Đều” là số điểm nâng, không phải giá trị chỉ số hoặc số tiềm năng chi cho từng loại.
- Chọn lô lớn nhất đủ tổng chi phí bằng tìm kiếm nhị phân, không lặp hàng nghìn lần. Khi không đủ một vòng đều, nâng từng điểm theo con trỏ HP → KI → sức đánh, giữ con trỏ qua lượt cập nhật.
- Bỏ qua chỉ số đã chạm trần hoặc không đủ phí. Không nâng vượt giới hạn, không thông báo lỗi liên tục, chỉ cập nhật thông tin một lần khi có thay đổi.
- Tính phí bằng long, bằng tổng nâng từng điểm: HP/KI = n*(base+1000)+10*n*(n-1); sức đánh = 100*n*base+50*n*(n-1).
- Cộng/trừ tiềm năng dùng khóa sư phụ, cùng khóa với lưu dữ liệu và mở giới hạn. Nhân vật chính giữ cách nâng chỉ số hiện tại.

## Công việc hoàn tất

- [x] Kiểm tra luồng Pet/NPoint và ghi kế hoạch.
- [x] Viết bảy chế độ kiểm thử, xác nhận thất bại trên JAR cũ.
- [x] Triển khai lô cân bằng và kiểm tra phí/trần trong Pet.java, NPoint.java.
- [x] Kiểm thử ít tiềm năng, nhiều tiềm năng, luân phiên, trần, phí, chặn giáp/chí mạng, nhân vật chính và đồng thời.
- [x] Rà soát độc lập yêu cầu và chất lượng; không còn phát hiện cần sửa.
- [x] 56/56 lượt kiểm thử source và 56/56 trên JAR đóng gói; đối chiếu toàn bộ class sản xuất và kiểm tra cấu trúc JAR.
- [x] Bảo trì có lưu dữ liệu, chuyển JAR cũ vào jar-backup, cài JAR mới, đối chiếu SHA-256 và xác nhận server nghe cổng 14445.

Chi tiết triển khai: [pet-auto-point.md](../../source-analysis/pet-auto-point.md).
