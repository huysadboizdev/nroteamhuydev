# Kiểm tra cải trang — 06/10/2026

Đối chiếu trực tiếp SQL team2026 với SRC/data/update_data/part: 2.099 part khớp. Đã rà 457 item type 5, gồm icon vật phẩm, khung đầu/thân/chân và head_avatar. Item Avatar 210 là entry đặc biệt, không coi là lỗi loại part.

11 cải trang thiếu 328 ID ảnh ở cả x1–x4 (1.312 file PNG): Cải trang Gốc ID 1841–1850 và Broly Nes ID 1851. Không thiếu bản ghi part hoặc sai loại part ở các cải trang chuẩn. Kiểm tra 37.808 PNG hiện có: không có file hỏng.

Nguồn đã kiểm tra: Downloads/filefolder/res moi/res moi/x1–x4, toàn bộ file PNG rời trong Downloads (447.035 file được duyệt), và 66 file nén RAR/ZIP đọc được. Không tìm thấy 328 ID ảnh thiếu.

Part trong res moi là bộ SQL khác: ví dụ part 1898 ở server là đầu Broly, còn nguồn là thân TYPE1. Không thể chép đè theo ID part. 158 lượt tham chiếu part khác bộ nguồn không đồng nghĩa lỗi: cần đối chiếu nhân vật trước khi thay.

Không thay bằng ảnh/part của nhân vật khác. Chưa thay đổi SQL, res, JAR hoặc dừng server. Ảnh người dùng gửi chưa xác định tên/ID cải trang đang lỗi; đã hỏi tên/ID để đối chiếu riêng.

Chi tiết:

- [Danh sách item, part và ảnh thiếu](../../.analysis/costume-audit/audit.json)
- [Kết quả tìm trong file rời](../../.analysis/costume-audit/loose-file-search.json)
- [Kết quả tìm trong 66 file nén](../../.analysis/costume-audit/archive-search.json)
- [Kiểm tra PNG](../../.analysis/costume-audit/png-verification.json)
