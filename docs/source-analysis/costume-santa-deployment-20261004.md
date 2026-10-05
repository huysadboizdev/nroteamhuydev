# Triển khai cải trang và cửa hàng Santa 04/10/2026

Đã triển khai vào `team2026`, cập nhật `SRC/20.jar` và khởi động server cổng 14445. Metadata: `.analysis/costume-santa-20261004/deployment.json`.

| Món | ID | Giá hồng ngọc | Điều kiện |
|---|---:|---:|---|
| Quy lão Kame | 710 | 1000 | Độc lập |
| Jacky Chun | 711 | 1500 | Đang sở hữu 710 |
| Android 21 Tiến Sĩ | 1710 | 2000 | Đang sở hữu 711 |
| Giáp tập luyện cấp 5 | 1869 | 2500 | Độc lập |

Tiền nhiệm trong đồ mặc/túi/rương, còn hạn và quantity dương; không bị thu hồi khi mua. Chỉ trừ ruby, kiểm tra đủ chỗ và hoàn tiền/đồ nếu cấp thất bại. Santa map 5/13/20 giữ các menu cũ và thêm cửa hàng mới ở cuối.

Đủ 21 option mặc định/shop trong SQL. Ba cải trang tăng TN/SM đệ tử 35/50/60%; chưởng x3/x4/x5 mỗi 60 giây chỉ nhân khi PvP hợp lệ giữa người chơi thật. Giáp cấp 5 mặc giảm sức đánh 40%, cởi tăng 50%; timer ban đầu 1000 phút, tích lũy tối đa 10.000 phút, không giữ bonus khi giáp rời kho/hết hạn, không làm đầy timer khi restore. HP/KI +15%, giảm sát thương 5%, giáp +50.000 khi mặc.

Doctor dùng part 1603/1606/1607, avatar 14241. Đủ 440 path PNG x1–x4; part binary khớp SQL. Client Game1/Game2 đã cập nhật nhãn ruby và cache packet icon lặp, giữ nguyên bản gốc có hash trong `client-before`.

46 bộ kiểm thử Java đạt cả trên source và JAR; kiểm thử JDBC mua từ shop SQL thật trên database riêng; Unity 950 kiểm tra đạt; SQL live và catalog trong JVM đã đối chiếu. Chưa thực hiện đăng nhập/gameplay trực tiếp, chụp UI hoặc save/load tài khoản live. Cảnh báo tile map không có trong scope thay đổi này được ghi ở `server-stderr.log`.

## Backup và khôi phục

- JAR cũ: `SRC/jar-backup/20-before-costume-santa-20261004-214921.jar`, SHA-256 `1ac383513e7866bb16838e42ee16da078de510f50b6119fe2316aa51e1f5ff75`.
- JAR mới: `SRC/20.jar`, SHA-256 `63c018179b6a7f78aa2ce88b879e4e5c86666ea468af4900553e776d73d9b6f0`.
- Snapshot catalog trước cập nhật: `.analysis/costume-santa-20261004/catalog-before.sql`; schema gốc: `item-template-schema-before.txt`; snapshot riêng các trường inventory: `inventory-before.json`. Không xuất account/password/token.
- Quét 2 player khi server dừng: 0 vật phẩm mục tiêu, không có inventory live bị chỉnh sửa. Audit: `inventory-backfill-audit.json`.
- Khôi phục cần bảo trì lưu dữ liệu và xác nhận server dừng trước khi thay JAR. JAR hiện tại cũng phải chuyển vào backup tên mới, đối chiếu hash rồi chép lại bản cũ. Đối với SQL, nhập snapshot vào database khôi phục riêng và lấy lại bốn template/option/shop/avatar liên quan; kiểm tra các món hoặc giao dịch phát sinh sau cập nhật trước khi áp dụng, không ghi đè toàn bộ catalog live bằng snapshot cũ. Client khôi phục từ bản gốc và manifest có SHA-256.

Source, client, migration và hash: `server-change-manifest.json`, `client-change-manifest.json`. Hai SQL dump có migration đã đồng bộ cuối tệp. Thư mục chạy chỉ có JAR ứng dụng `20.jar`; thư viện và JAR công cụ được giữ nguyên.

## Cập nhật giáp gọn và cùng tab cải trang

Đã chuyển giáp cấp 5 (1869) vào tab **Cải trang** cùng 710/711/1710; bỏ tab giáp riêng đã trống. Giá vẫn 2.500 hồng ngọc; đủ sáu option và hiệu ứng như cũ. Tooltip Game1/Game2 hiện tên giáp, lời giải thích mặc giảm 40%/cởi tăng 50% đúng một lần, tiếp theo các chỉ số và thời gian còn lại; giáp hiển thị 50.000. Không ép thời gian về 0 theo ảnh mẫu.

Đã backup hai file Panel và dữ liệu cửa hàng, kiểm thử Unity 16 kiểm tra, kiểm thử JDBC SQL mua bốn món cùng tab và migration lặp an toàn. Server đã bảo trì lưu dữ liệu, khởi động lại và xác minh shop trong JVM thật. Không thay JAR trong lần này. Hồ sơ: `.analysis/armor-compact-20261004/deployment.json`, `client-manifest.json`, `runtime-shop-status.txt`; migration: `SRC/sql/migrations/20261004_santa_armor_same_tab.sql`.

## Nâng trần thời gian tích lũy giáp cấp 5 — 05/10/2026

Giáp 1869 vẫn có 1.000 phút khi tạo/mua mới. Khi đang mặc và đánh quái, cơ chế luyện tập hiện có cộng 1 phút mỗi 60 giây có hoạt động đánh trong 30 giây gần nhất; trần riêng cấp 5 đã tăng lên **10.000 phút**. Đứng yên không tích thêm; tháo vào túi/rương vẫn giảm thời gian và hưởng +50% sức đánh khi còn hiệu lực. Các giáp khác giữ trần 1.000 phút. SQL, giá và sáu option không đổi; không nạp lại thời gian của giáp đã có.

Kiểm thử RED tái hiện bị chặn ở 1000; GREEN 46 bộ trên source và JAR. Kiểm tra trực tiếp JVM bằng vật phẩm/player chưa đăng ký xác nhận mặc luyện tập tăng 9999→10000 và không vượt trần; không sửa player live. Server đang nghe cổng 14445.

JAR hiện tại SHA-256 `beed6156faa02d3a6ba89586e55bb544d61a8a5fdbda1c66abdf38524cc20655`; bản trước cập nhật: `SRC/jar-backup/20-before-armor-cap-10000-20261005-080648.jar`, SHA-256 `63c018179b6a7f78aa2ce88b879e4e5c86666ea468af4900553e776d73d9b6f0`. Hồ sơ `.analysis/armor-cap-10000-20261004/deployment.json`, `red.log`, `combined-test-results.json`, `runtime-armor-status.txt`.
