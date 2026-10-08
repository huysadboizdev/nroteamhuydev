# Gộp hiển thị Gõ đầu trẻ — 06/10/2026

Danh sách cũ có ba item1392/1393/1394, ba effect242/243/240, cùng tên và chỉ số. Đã giữ một dòng giữa tab Danh hiệu/Sở hữu mà không xóa catalog hoặc bản ghi sở hữu cũ.

`GoDauTreShopRows` chọn bản đang dùng trước, rồi bản sở hữu còn hạn lâu nhất; khi chưa sở hữu chọn bản có phần trăm nhiệm vụ cao nhất, hòa thì giữ thứ tự shop. Cả hai tab dùng cùng lựa chọn. Giữ nguyên ID effect, chỉ số, nhiệm vụ, thời hạn, kiểm tra sở hữu và chức năng trang bị.

Năm nhóm regression tái hiện lỗi trên JAR cũ và qua trên bản mới: chưa sở hữu, từng ID cũ, nhiều bản đang sở hữu, tiến độ nhiệm vụ khác nhau, bản hết hạn. Tám nhóm BadgeSystemTest cũng qua. Biên dịch lại ba class shop, so sánh mọi entry khác của JAR giống hệt bản vệ tinh đang dùng.

Triển khai 07:03 UTC+7 bằng bảo trì lưu dữ liệu, snapshot SQL và backupJAR trước khi thay. Server PID24720 nghe14445; probe chỉ đọc xác nhận ba class shop khớp bản kiểm thử và nhận đủ legacyIDs. Chưa xác nhận ảnh client sau triển khai; đóng/mở lại bảng danh hiệu để nhận danh sách mới.

Backup: `SRC/jar-backup/20-before-go-dau-tre-20261006-070250.jar`.
SHA256cũ: `e590328c68ae65ebe229ea2d820deeaa25fc36b7970213c7a754c9e7c1692ea4`.
SHA256mới: `fffcf718bb5a81d40014d9addb80d76800d5b5e90bad64555456606493b1fdfa`.
Metadata cạnh backup và `.analysis/go-dau-tre-deployment-latest.json`.

Không chạy migration SQL. Khôi phục bằng bảo trì lưu dữ liệu, dừng JVM rồi phục hồi backupJAR. Chưa commit/pushGitHub.
