# Giftcode options, sửa và nhận phần bổ sung — 08/10/2026

Đã triển khai web Giftcode tạo/sửa và server nhận delta. 10 A tăng thành15 A +2 B thì người đã nhận chỉ nhận5 A +2 B. Watermark theo account/code/item; giảm/xóa/thêm lại không phát lại phần cũ, bổ sung không trừ lượt (kể cả code hết lượt nếu còn hạn). Option của món đã phát được giữ cố định. UUID receipt và snapshot người chơi cùng transaction; giữ pending/logout/reconciliation.

Kiểm chứng: 655 source Java build, 8 nhóm test trên classes +8 trên candidate; hai suite SQL Java và SQL PHP trên schema riêng, kiểm tra service nhận delta khi lịch sử đã có code; PHP lint/helper/update, JS4/4, render PHP/escaping, race current-read; review cuối không blocker. Schema test đã xóa. Chưa thử browser/client game thật hoặc phát quà trên tài khoản thật.

Server đã bảo trì lưu dữ liệu, backup SQL/JAR, migration và start lại. PID 15244 / listener14445, startup nạp Giftcode thành công. Chỉ SRC/20.jar ở thư mục chạy.

- JAR đang dùng SHA-256: `08f1308943d49d2bcb1aae7b40f84fa22d96c6db8c0fca09db5ed3c26cfa140d`.
- Backup JAR: `SRC\jar-backup\20-before-giftcode-supplements-20261008-135626.jar`, SHA-256 `08a137803c32317fa32bb1d64c3be5fb7229fad506dbc5e3e6df8121e6760a1b`.
- SQL snapshot: `.analysis\giftcode-supplements-deploy-20261008-135626\team2026-before-giftcode-supplements.sql`.
- Metadata: `.analysis/giftcode-supplements-deployment-latest.json`.
- Kế hoạch hoàn thành: `C:/xampp/htdocs/webgoc2026/docs/superpowers/plans/2026-10-08-giftcode-options-and-supplements.md`.

Khôi phục: dùng bảo trì lưu dữ liệu, backup JAR hiện tại rồi khôi phục JAR trên. Giữ dữ liệu người chơi cùng watermark/receipt mới; không ghi đè quà đã nhận bằng SQL snapshot cũ.
