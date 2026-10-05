# Dọn đồ có hạn trong hành trang — 04/10/2026

Chi Chi tại map 5 có nút **Bỏ đồ có hạn sử dụng**, đặt trước Đóng. Theo cập nhật mới của người dùng, hộp thoại chỉ hỏi **Bạn có muốn bỏ đồ có hạn sử dụng trong hành trang không?**, với hai nút **Có** / **Không**. Không hiển thị danh sách, số ô, số lượng hay mô tả dài. Chỉ xóa toàn bộ chồng đồ có option 93 trong `itemsBag`, cả 0 ngày và còn hạn. Không gỡ option để thành đồ vĩnh viễn. Nút dọn không xóa `itemsBody`, `itemsBox`, trang bị đệ tử hoặc đồ vĩnh viễn.

Preview/confirm dưới khóa player, có hạn 120 giây và dùng một lần. Từ chối danh sách thay đổi, chuyển đồ, thay quantity/options/template/createTime, logout, trade hoặc nghiệp vụ pending. Lưu toàn bộ nhân vật bằng PlayerDAO hiện có; nếu chưa lưu được thì khôi phục đồ đúng ô. Ghi audit vào `SRC/log/timed-bag-cleanup.log` khi chạy từ SRC.

Hết hạn trang bị là cơ chế riêng: dùng ngày lịch Asia/Ho_Chi_Minh UTC+7, đồ 0 ngày được giữ trong ngày hiện tại và gỡ lúc qua ngày mới. Kiểm tra trang bị sư phụ/đệ tử khi đăng nhập và mỗi giây khi online, kể cả đứng ở nhà; cập nhật chỉ số, hình dạng và companion khi gỡ. Cơ chế này không quét túi hay rương nhà; cơ chế hết hạn khi tải túi/rương trước đây vẫn giữ nguyên.

## Kiểm chứng

- 8 nhóm test mới xác nhận RED trên JAR cũ; GREEN trên source và JAR mới.
- 86/86 lượt hồi quy source và 86/86 lượt trên JAR đạt; bao gồm phạm vi, stale, guard/TTL/alias, save failure, ngày lịch, online/đệ tử, concurrent và route menu NPC.
- Không báo “đã hết hạn” khi đồ từ 1 ngày xuống 0 ngày mà vẫn được giữ.
- Toàn bộ class ứng dụng trong JAR đã đối chiếu với class biên dịch; archive không có entry trùng hoặc test class.
- Chưa thao tác bằng client game thật và chưa thử lưu/tải một nhân vật live trong DB. Save-failure tests dùng injected saver, không phải lỗi DB thật.

## Triển khai

Luồng nhận console của server cũ đã dừng từ trước (thread dump có DestroyJavaVM, không có luồng activeCommandLine), nên gửi bt qua console không kích hoạt bảo trì. Đã gọi trực tiếp Maintenance.startSeconds(5) qua Java Attach để dùng cơ chế lưu/đóng của ứng dụng. Batch restart tự chạy ngay; đã tạm giữ batch trong workspace trong lần bảo trì tiếp theo và khôi phục đúng nguyên bản trước khi khởi động lại. Không kill JVM trước khi lưu. Công cụ agent chỉ là JAR công cụ, không đóng gói vào JAR ứng dụng.

- JAR hiện tại: `SRC/20.jar`, SHA-256 `d2d7e5a657689c3ef1ab1f4593cdaee84a3b6b0bdc7f9df76c3ccee7ff3700b3`.
- Backup: `SRC/jar-backup/20-before-timed-bag-cleanup-20261004-165751.jar`, SHA-256 `4d3548998d917cd59202bc877078c0ac08694195e8c93f69cfe9295fee96b42d`.
- Metadata: `.analysis/timed-bag-cleanup/deployment.json` và `SRC/jar-backup/deployment-timed-bag-cleanup-20261004-165751.json`.
- Server mới PID 16040 nghe cổng 14445; kết nối TCP localhost thành công.
- Chỉ còn JAR ứng dụng đang dùng trong thư mục chạy, không còn staging JAR của đợt build này. Thư viện và công cụ giữ nguyên.

Logs: `.analysis/timed-bag-cleanup/red.log`, `.analysis/timed-bag-cleanup/build.log`, `.analysis/release-20261004-165630/jar-tests.log`, `.analysis/timed-bag-cleanup/deploy-final.log`, `.analysis/timed-bag-cleanup/runtime.json`.
