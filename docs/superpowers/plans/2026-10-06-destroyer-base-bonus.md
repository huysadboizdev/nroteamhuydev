# Khôi phục bonus chỉ số gốc đồ Hủy Diệt

**Trạng thái:** đã triển khai. 63 lượt kiểm thử PASS trên classes, candidate và JAR đã cài; cache shop và hash class khớp. Server PID2508/cổng14445. Báo cáo: `docs/source-analysis/destroyer-base-bonus-2026-10-06.md`. Kế hoạch này thay thế yêu cầu giữ nguyên nền trong kế hoạch Beerus trước đó.

Người dùng yêu cầu giữ lại bonus tăng chỉ số gốc, cùng dòng HP/KI/SD1–5% đã triển khai. Áp dụng cho món mới đổi tại Beerus; không random lại đồ đang sở hữu hoặc thay đổi Thiên Sứ.

Khôi phục phân phối cũ: 1% đạt15%, 14% chọn11–14%, 20% chọn7–10%, 25% chọn4–6%, 40% chọn0–3%. Một mức tăng dùng chung các option nền0/22/23/14/27/28/47 của món; giữ nguyên21/30 và không thêmấn. Sau đó roll một dòng HP/KI/SD độc lập1–5%. Giá, nguyên liệu, đủbộThần và rollback giữ nguyên.

Kiểm thử trước sửa đã tái hiện lỗi thiếu tăng nền. Kiểm thử các mức0–15%, toàn bộ option nền, giữ yêu cầu sức mạnh/khóa giao dịch và dòng bonus riêng; hồi quy đầy đủ. Triển khai bằng bảo trì lưu dữ liệu, backup JARtimestamp/SHA256, không thay SQL; kiểm tra cache/classhash/JAR đã cài.
