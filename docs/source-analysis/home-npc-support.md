# NPC nhà nhận ngọc xanh và đệ tử thường

Đã triển khai cho Ông Gohan (map21), Ông Moori (map22), Ông Paragus (map23).

- Ngọc xanh bằng 0: nhận đúng 10.000.000; còn ngọc không nhận; dùng hết được nhận tiếp. Ruby/vàng giữ nguyên.
- Chỉ nhận đệ thường khi pet null; random đều giới tính 0/1/2 qua factory hiện hành, không thay đệ có sẵn. Sức mạnh mới 2.000, bậc 0, tiềm năng 0. Cơ chế luyện HP/KI/sức đánh giữ nguyên.
- Ba NPC dùng chung handler, giữ xử lý nhiệm vụ. Map/template và menu index riêng kiểm tra trước thao tác; cấp thưởng dưới khóa người chơi, gọi DAO lưu ngay và đồng bộ client.

## Kiểm tra thực tế

- TDD: kiểm thử menu/gem/pet đã FAIL đúng chức năng chưa có trước khi sửa.
- 74/74 kiểm thử source và 74/74 trực tiếp candidate JAR PASS.
- 10/10 lượt kiểm thử bổ sung source/JAR PASS, bao gồm 12 request đồng thời và đếm chỉ một lần gọi factory pet.
- Kiểm thử concurrent trên JAR đã cài PASS. Review độc lập không phát hiện lỗi chặn triển khai.
- SQL schema/res client không thay đổi. Fixture offline không gọi lưu DB thật; chưa thử nhận thưởng và đăng nhập lại bằng client thật. DAO hiện bắt lỗi lưu nội bộ, nên thông báo thành công không phải bằng chứng SQL commit; giới hạn này thuộc API lưu hiện có.

## Triển khai

- Dừng bằng bảo trì có lưu; xác minh tiến trình Java dừng trước khi chuyển JAR.
- JAR hiện tại: `SRC/20.jar`, SHA-256 `3bee4687ac28ba8ab68ca6cd569c3c98df70d483002b85bcba2218c7ace51af4`.
- Backup: `SRC/jar-backup/20-before-home-npc-support-20261002-200428.jar`, SHA-256 `dd2af8e9f6ea08a510de9af44e6eb571956868cc25659771a0598cb0bdcb4e3f`.
- Server đã khởi động, PID 12156, nghe cổng14445.
- Chỉ còn app20.jar ở thư mục chạy; không còn staging/app JAR dư trong .analysis; thư viện giữ nguyên.

Thời điểm ghi nhận: 2026-10-02T20:06:40.180366+07:00.
