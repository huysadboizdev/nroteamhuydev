# Khôi phục mapping Uub từ bộ resource gốc

Nguồn đối chiếu được tìm thấy tại `C:/Users/Admin/Downloads/filefolder/res moi/part.sql`, dòng 980–982. Ba dòng Uub đã dùng đúng các icon 8586–8616, thứ tự frame và dx/dy chuẩn. Bản sửa icon trước giữ mapping robot; lần chỉnh đầu +7 tiếp tục sai vì chưa thay frame đứng thân/chân và offset từng động tác.

Khôi phục nguyên dữ liệu part 946/947/948 từ SQL đi kèm resource. Điểm khác chính: đầu [4,7] / [2,5]; đảo lại frame 0/1 của thân về 8588/8589 và chân về 8604/8605; khôi phục toàn bộ dx/dy theo nguồn. Không áp dụng quy tắc dịch đồng loạt. Avatar tiếp tục 8586. Giữ PNG hiện có vì hình đúng Uub và kích thước phù hợp, không ghi đè sprite robot của trang phục khác.

Đã dựng 33 frame theo CharInfo của client, có ảnh `.analysis/uub-audit/original-resource-frames.png`. Verifier so sánh SQL trực tiếp với file resource gốc, rồi với binary server; không chỉ kiểm tra giá trị tự tạo. Tăng phiên bản dữ liệu 13 để hai tab tải lại NR_part.

Migration `SRC/sql/migrations/20261006_uub_original_mapping.sql`. Triển khai theo bảo trì lưu dữ liệu, snapshot SQL, backup JAR timestamp/hash. Metadata `.analysis/uub-original-deployment-latest.json`.

Đã triển khai: PID 48408 mở cổng 14445, runtime vsData=13 và DataGame hash khớp. Ba dòng SQL và part binary khớp nguyên bản với `res moi/part.sql`; kiểm tra 128 PNG hợp lệ. 98 hồi quy PASS trên classes/candidate/JAR đã cài. Backup `SRC/jar-backup/20-before-uub-original-20261006-105923.jar`; JAR mới SHA-256 `08b204b36b0dd72aedb818d8d0a367dc1131c2245b4d74a0497acba600862e5a`. Chưa quan sát trực tiếp Unity sau cập nhật; cần đăng nhập lại để nhận NR_part phiên bản13.
