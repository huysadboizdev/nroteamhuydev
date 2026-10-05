# Danh hiệu Mẹ Rồng phải đủ bộ hiện có

Yêu cầu ngày 04/10/2026: chỉ được hiện danh hiệu khi có đủ 7 loại pet rồng nhí vĩnh viễn khác nhau. Phạm vi tính bộ giữ giống bonus bộ đã có: hành trang, rương nhà và trang bị nhân vật chính. Không tính pet đệ tử, quantity bằng 0 hoặc bất kỳ option 93 nào, kể cả còn 0 ngày.

Snapshot trực tiếp acc admin trước sửa: player ID 1367, rương nhà không có rồng nhí; túi/trang bị có 6 loại vĩnh viễn, thiếu Rồng nhí 3 sao ID 1769. Bonus bộ false nhưng danh hiệu effect 256 vẫn bật; hai bản ghi danh hiệu giống nhau cùng isUse=true.

Nguyên nhân: nhiệm vụ ME_RONG tăng theo lần nhận pet, kể cả nhận trùng; cấp danh hiệu 30 ngày rồi không đối chiếu sở hữu hiện tại. BadgesData constructor tự thêm record nhưng caller thêm lần nữa. Cache badges.idBadges không reset khi hết hạn hoặc không có record được bật. Tiến độ, quyền chọn, option chỉ số và banner dùng các điều kiện khác nhau.

Bản sửa dùng DragonPetCollection cho cả bonus và danh hiệu. Tiến độ bằng số loại vĩnh viễn hiện có / 7. Chặn cấp/chọn/banner/options nếu thiếu; dedupe record Mẹ Rồng cũ, tắt isUse và gửi banner 0 giây để bỏ hiệu ứng khi mất bộ. Reset cache trước khi chọn banner hiện hành. Gỡ lần add trùng trong cấp nhiệm vụ và mua danh hiệu; không return sớm làm chặn phần thưởng nhiệm vụ khác. Không xóa pet hoặc tự cấp pet còn thiếu; giữ record danh hiệu cũ để có thể chọn lại nếu lấy đủ bộ trong thời hạn còn lại.

Kiểm thử RED trên JAR cũ: missing, legacy, grant, lifetime, restrictions. Chạy cả năm mode cùng bộ hồi quy source/JAR trước triển khai. Kiểm tra snapshot runtime sau cập nhật nếu admin đăng nhập lại. Log và metadata nằm trong `.analysis/dragon-badge` và `SRC/jar-backup`.

## Kết quả triển khai

- Source 91/91 và packaged JAR 91/91 đạt. Năm mode DragonBadgeTest đều tái hiện lỗi trên JAR cũ và đạt trên JAR mới.
- JAR mới SHA-256 `be1056f6e334f6cda9d5fea0c556ab7ae70ddc42b8e96fe5418e52b442fd5e14`; mọi class đã đối chiếu với output build, không có test class/entry trùng.
- Backup: `SRC/jar-backup/20-before-dragon-badge-20261004-171216.jar`, SHA-256 `d2d7e5a657689c3ef1ab1f4593cdaee84a3b6b0bdc7f9df76c3ccee7ff3700b3`.
- Server được bảo trì bằng cơ chế lưu/đóng của ứng dụng, tạm giữ và khôi phục batch restart nguyên vẹn. Server mới PID 6884 nghe 14445, TCP localhost thành công.
- Snapshot sau khởi động có 0 acc online; chưa kiểm tra lại admin bằng client hoặc DB save/reload thật. Đăng nhập lại sẽ áp dụng quy tắc và làm sạch bản ghi Mẹ Rồng cũ.
- Banner client hiện có vòng đời 5 giây; khi thu hồi server gửi thời lượng 0, banner đang vẽ có thể đợi hết vòng đời tối đa 5 giây trước khi biến mất. Không sửa client.

Metadata: `.analysis/dragon-badge/deployment.json`. Logs: `.analysis/dragon-badge/red.log`, `.analysis/dragon-badge/build.log`, `.analysis/release-20261004-171007/jar-tests.log`, `.analysis/dragon-badge/deploy.log`. Snapshot trước/sau: `.analysis/timed-bag-cleanup/admin-dragon-snapshot.json`, `.analysis/dragon-badge/admin-after.json`.
