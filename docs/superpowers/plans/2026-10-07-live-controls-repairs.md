# Live controls repairs implementation plan

**Goal:** EXP thay trực tiếp và giữ sau restart, thông báo đúng kênh thế giới, giftcode nhận một lần theo tài khoản, boss chết được dọn và boss reload tách khỏi quái.

**Architecture:** Giữ hàng đợi LiveSync transaction/receipt. EXP lưu SQL rồi publish RAM; receipt đọc setting hiện tại. Giftcode dùng ledger unique(account_id, canonical_code), trao thưởng/quota/claim trong một transaction và recovery khi commit chưa xác định. Boss override HP/sức đánh theo ID/giai đoạn từ bảng riêng, chỉ đọc tại lần spawn mới.

**Tech Stack:** Java17, PHP, MySQL/MariaDB, Unity protocol hiện có.

- [x] Hồi quy và sửa TauPayPay cleanup, Whis/arena/Xên tính HP an toàn, clamp sát thương long.
- [x] EXP long và nhân chống tràn. Thêm server_runtime_setting, nạp startup và web hiển thị setting. Kiểm thử rollback/replay/restart.
- [x] SEND_NOTICE gửi packet92 tên hệ thống/ngoại hình hợp lệ; không trừ ngọc hoặc yêu cầu admin online.
- [x] Giftcode canonical trim ASCII uppercase; ledger tài khoản/backfill legacy. Khóa player→GiftManager→SQL; kiểm tra ngày/quota/túi; inventory/bag/history cùng transaction. Recovery chặn login/save/gameplay ghi đè. Kiểm thử duplicate/reload/recreate/rollback/uncertain commit.
- [x] boss_live_override và BossLiveReload snapshot immutable; RELOAD_BOSS/RELOAD_ALL thay snapshot, Boss.initBase áp HP/damage lần spawn kế tiếp. Web riêng quản lý override. Không reset boss/map/timer đang chạy.
- [x] Tổng hợp migration/seedSQL và chạy idempotent trên schema cô lập. Dry-run legacy trước bảo trì.
- [x] PHP/JS/Java classes/candidate đạt; rà soát giao dịch/pending logout độc lập.
- [x] Bảo trì lưu dữ liệu, xác nhận dừng; backup JAR/SQL/web + SHA256; migration/install/restart. Probe chỉ đọc và kiểm thử JAR đã cài.

Không thử broadcast/thưởng/đổi EXP tùy ý trên người chơi thật. Lần cài JAR này cần bảo trì; đổi EXP/reload sau đó không cần bảo trì. Không commit/push các thay đổi khác đang có.

Deployment evidence: docs/2026-10-07-live-controls-deployment.md. Specialized boss override limits are recorded there and shown in the web UI.
