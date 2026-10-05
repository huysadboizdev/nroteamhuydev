# Bỏ ngọc xanh rơi từ Tiểu đội sát thủ Namek

Yêu cầu người dùng: bỏ ngọc xanh rơi từ boss Tiểu đội sát thủ Namek.

Thiết kế: xóa khối tạo ItemMap ID77 trong reward của SO1_NM, SO2_NM, SO3_NM, SO4_NM và TDT_NM. ID77 là Ngọc/type10 trong catalog đang dùng. Giữ capsule722, cải trang, ngọc rồng, điểm sự kiện và guard thưởng mỗi đời. Không sửa DB hoặc số dư ngọc đã nhặt.

- [x] Backup source, đổi kiểm thử NamekDrops thành kiểm tra không có77; chạy RED trên JAR cũ.
- [x] Xóa khối77 trong5 source, compile5 class; package chỉ thay đúng5 entry.
- [x] Kiểm thử thưởng/drop/respawn và bộ regression hiện có trên candidate.
- [x] Bảo trì lưu dữ liệu, xác minh server dừng; MOVE JAR cũ vào backup có timestamp, kiểm tra SHA trước/sau, cài mới.
- [x] Khởi động, kiểm tra runtime/cổng và ghi metadata.

Kết quả: candidate67/67 PASS; JAR đã cài kiểm tra drops/duplicate PASS. Server tự khởi động sau cài JAR; xác minh PID17472, cổng14445, runtime capsule/vé/phí hồng ngọc PASS. Không thay DB.
