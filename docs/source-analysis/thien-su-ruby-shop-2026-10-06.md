# Shop Thiên Sứ: 10 hồng ngọc — 06/10/2026

Đã triển khai lúc 07:42; server PID 5936, cổng 14445.

Toàn bộ 16 mặt hàng trong shop `THIEN_SU` của Whis dùng giá 10 hồng ngọc mỗi đơn vị, `type_sell=3`, icon hồng ngọc 7743. Bao gồm đá nâng cấp cấp 1–5, đá may mắn cấp 1–5, công thức thường và công thức VIP cả ba hệ. Giữ loại shop đặc biệt để client hiện đúng icon hồng ngọc.

`ShopService.subIemByItemShop` nhận riêng ngữ cảnh Thiên Sứ, kiểm tra giá/tiền tệ/icon và trừ ví `inventory.ruby`. Không yêu cầu item hồng ngọc trong túi và không dùng ví ngọc xanh/vàng. Giữ kiểm tra đầy túi trước trừ tiền và khóa player; chặn khi có giao dịch hồng ngọc đang chờ. Không thay phí của thao tác chế tạo trang bị.

Migration: `SRC/sql/migrations/20261006_thien_su_ruby_10.sql`. Đã áp dụng trên DB `team2026`, đồng bộ hai dump và hai seed. Không sửa item options hoặc nguyên liệu của các shop khác.

5 nhóm test PASS: mua từng loại bằng đúng 10 hồng ngọc khi ngọc xanh bằng 0; thiếu hồng ngọc; đầy túi; giá/loại tiền/icon sai; mua lặp tính đúng từng đơn vị. Test wallet đã thất bại trên bản cũ trước sửa. SQL và cache server sau khởi động đều xác nhận 16/16 hàng giá 10, type 3, icon 7743; hash class ShopService khớp bản đã test. Chưa quan sát UI Unity trực tiếp sau cập nhật.

Đã bảo trì có lưu dữ liệu, snapshot SQL sau khi server dừng, backup JAR cũ trước cài. JAR mới chỉ thay ShopService.class; mọi entry khác giữ nguyên byte, bao gồm bản sửa boss và chọn khu trước đó. Thư mục chạy chỉ có `SRC/20.jar`.

JAR SHA-256: `08296c466d1e18e20a898b377adc7f8dffac969015712aa3963e1de4dee2ebe5`. Backup: `SRC/jar-backup/20-before-thien-su-ruby-20261006-074211.jar`.

Metadata: `.analysis/thien-su-ruby-deployment-latest.json` và metadata cạnh backup. Thư mục deployment có `prior-shop-rows.tsv`, `verified-shop-rows.tsv`, `rollback-shop.sql`, snapshot SQL. Rollback bằng bảo trì lưu dữ liệu, phục hồi JAR backup và chỉ chạy rollback-shop.sql cho các giá cũ; không phục hồi toàn bộ player từ SQL snapshot.
