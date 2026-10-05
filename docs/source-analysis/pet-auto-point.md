# Đệ tử tự cộng chỉ số theo lô

Đã triển khai ngày 02/10/2026 cho các loại đệ tử dùng lớp Pet: thường, Mabư, Uub, Kid Beer, Jiren. NewPet trang trí không dùng luồng này.

Pet.increasePoint phân bổ đều số điểm HP/KI/sức đánh khi đủ tiềm năng, mỗi vòng tối đa 1.000 điểm/loại, tối đa 20 vòng/cập nhật. Một điểm HP/KI tăng 20, một điểm sức đánh tăng 1. Khi thiếu tiềm năng, phần dư luân phiên giữa ba chỉ số; khi chạm trần, bỏ qua chỉ số đó. Không đổi tốc độ vòng cập nhật server.

NPoint.tryIncreasePetPoint kiểm tra đệ tử còn thuộc sư phụ, đủ tiềm năng và còn chỗ dưới trần trước khi trừ phí. NPoint.increasePoint chặn nâng giáp/chí mạng đối với đệ tử; chỉ số đã có và tác động trang bị vẫn giữ. NPoint.tiemNangUp dùng cùng khóa sư phụ để tránh mất cập nhật khi cộng tiềm năng đồng thời.

Chi phí bằng tổng nâng từng điểm, tính bằng long: HP/KI n*(base+1000)+10*n*(n-1), sức đánh 100*n*base+50*n*(n-1). Tối đa 1.000 điểm mỗi lần gọi. Khi có thay đổi, Service.point cập nhật chỉ số đệ tử trước rồi sư phụ, Service.showInfoPet gửi thông tin một lần mỗi lượt.

## Kiểm chứng

Bảy chế độ mới đã thất bại trên JAR cũ trước sửa và đạt sau sửa. Toàn bộ 56/56 lượt kiểm thử source và 56/56 lượt trên JAR đã đạt. Gồm phân bổ đều, lô lớn, phí sát biên, trần, chỉ số bị chặn, đường nâng của nhân vật chính và cạnh tranh đồng thời. Đã rà soát độc lập, đối chiếu class sản xuất trong JAR và kiểm tra không đóng gói test/cache hoặc entry trùng.

Nhật ký: `.analysis/pet-point-red/red.log`, `.analysis/pet-point-build.log`, `.analysis/gold-economy-check/test.log`, `.analysis/release-20261002-161954/jar-tests.log`. Compiler có cảnh báo AccessDeniedException lúc đóng thư viện ZIP trong sandbox, exit 0; class đã được kiểm chứng qua đóng gói và chạy thử. Chưa kiểm tra bằng tài khoản/client game thật hoặc lưu/tải lại database; luồng làm mới hợp thể được rà soát qua source.

## Bản đang chạy

- JAR: `SRC/20.jar`.
- SHA-256 mới: `9ba1ce642c691b9f140e7d22711f3f29f212ef1ec6319f7aefc8ce20690e9454`.
- Backup: `SRC/jar-backup/20-before-pet-auto-point-20261002-162112.jar`.
- SHA-256 backup: `f9782f004d1d42aeb13569e0458753d7b113a7b3c70d26f51a032de0de7d701e`.
- Server Java PID 4068 nghe cổng 14445 sau bảo trì lưu dữ liệu và khởi động lại.
- Metadata: `.analysis/latest-deployment.json`; `SRC/jar-backup/deployment-20261002-162112.json`.

JAR cũ đã được chuyển vào backup trước khi cài; không còn JAR ứng dụng dư trong thư mục chạy hoặc thư mục release.
