# Bản sửa skill đã cập nhật — 09/10/2026

Server đã cập nhật `SRC/20.jar` và khởi động lại sau bảo trì có lưu dữ liệu. PID đang chạy khi bàn giao: 3148; listener cổng 14445 đã được xác nhận thuộc đúng PID và duy trì trong kiểm tra 30 giây. Probe đọc trong JVM xác nhận mã đang nạp từ `SRC/20.jar` có bộ hẹn nội tại riêng và snapshot bom mới.

Lần khởi động đầu PID 13044 đã ngừng khi kiểm tra lại sau phiên công cụ bị ngắt; log không ghi rõ nguyên nhân dừng. Server được khởi động lại và kiểm tra runtime/listener lần nữa trước bàn giao.

## Nội dung sửa

- Bao gồm các bản sửa gồng/hồi chiêu và biến khỉ đã ghi ở `skill-followup-review-20261009.md`.
- Bộ hẹn nội tại giữ từng skill riêng; kiểm tra đúng đối tượng skill và timestamp lượt dùng, loại timer cũ hoặc skill đã bị thay/xóa. Giữ hành vi hồi MP và packet hiện có.
- Thao tác trả hồi chiêu và ghi timestamp lượt dùng mới dùng chung khóa Player, thứ tự khóa Player → EffectSkill.
- Bom boss/đệ chốt map gốc, cấp skill, sát thương và mục tiêu trước khi nổ; không đọc lại map/skill đang chọn sau khi chốt. Gây sát thương ngoài khóa của người nổ để tránh giữ khóa hai nhân vật đối nghịch.
- Đổi skill và rời map phối hợp bằng cùng khóa Player với thao tác chốt bom.

## Kiểm chứng

Biên dịch toàn bộ server thành công. Kiểm thử trực tiếp JAR cuối: **31 ca PASS** (19 ca vòng đời skill/bom, 8 ca nội tại, 4 nhóm regression HP/khỉ/sát thương boss). Hai ca đồng thời đã RED trước khi sửa và GREEN sau khi sửa. Reviewer độc lập xác nhận không còn lỗi chặn triển khai trong các luồng được rà, và chạy lại hai ca đồng thời thành công.

Client hai tab đã được kiểm thử x1/x2/x5/x10/x20/x50/x100 với 169582 assertions ở lượt trước. Source client không đổi trong bản cập nhật server này. Bản dùng: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/PC-SkillSpeed-20261009-163432.zip`.

## Backup và đối chiếu

- JAR cũ: `SRC/jar-backup/20-before-skill-final-20261009-170729.jar`.
- SHA-256 cũ: `5FDDDFF67ACB8E670EC61D0885833D4EF9F60908898760D802433A0415B462E3`.
- SHA-256 mới: `11D8E8145783E870D35198061D37F3D6D9FA7A579C5D14D730B7BF9B44EBE2EA`.
- SQL sau bảo trì: `.analysis/skill-final-deploy-20261009-170729/database-before-skill-final.sql` (4017188 bytes).
- Metadata triển khai: `.analysis/skill-final-20261009/deployment.json`; bản lưu kèm backup: `SRC/jar-backup/skill-final-20261009-170729.json`.
- Log khởi động hiện tại: `SRC/log/skill-final-20261009-200522.out.log` và `.err.log`; log lần đầu vẫn được giữ.
- Probe: `.analysis/skill-final-20261009/runtime-status.txt`.

Nếu khôi phục: bảo trì lưu dữ liệu, xác nhận server dừng, backup JAR đang dùng với tên mới, rồi khôi phục JAR cũ đã đối chiếu hash. Giữ dữ liệu người chơi hiện tại; không chép đè database bằng snapshot cũ sau khi đã có giao dịch mới.

## Giới hạn

Chưa chơi thử nhân vật trên server thật hoặc đo FPS bằng profiler. Startup vẫn có các cảnh báo tile map thiếu đã có ở bản trước; bản cập nhật này tập trung vào skill. Protocol cũ chưa có cast token để phân biệt tuyệt đối packet cùng skill từ lượt đã hủy.
