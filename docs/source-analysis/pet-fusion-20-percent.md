# Hợp thể ba đệ đặc biệt tăng 20%

Đã triển khai ngày 02/10/2026. Uub (type2), Kid Beer/Beerus (type3), Kid Jiren (type4) tăng đúng 20% HP/KI/sức đánh của phần đệ khi sư phụ hợp thể, rồi phần đệ được cộng vào sư phụ. Áp dụng hợp thể thường, Porata 1/2/3 và mode Gogeta. Giữ các tác động trang bị/buff khác theo thứ tự tính hiện tại; không tăng giáp/chí mạng và không thay công thức đệ thường/Mabư.

NPoint.hasSpecialPetFusionBonus kiểm tra loại đệ, sư phụ còn giữ đúng đối tượng đệ và đang hợp thể. HP/KI/dame dùng biến local, cộng một lần; đã bỏ hai nhánh sức đánh Beerus trùng nhau. Bonus World Cup 10% KI cũng dùng local, khắc phục tăng dồn khi mặc hai món tương ứng. NPoint.calPoint vẫn tính đệ trước sư phụ.

Ví dụ không đồ/buff: đệ HP/KI 400.000, dame 20.000 thành 480.000/480.000/24.000 khi hợp thể, giống nhau cho ba loại. Sư phụ HP/KI 100.000, dame1.000 thành 580.000/580.000/25.000. Tính lại 30 lần, huỷ hợp thể và hợp thể lại không tăng dồn.

Trần sức mạnh vẫn MAX_LIMIT=9, POWER_LIMITS[9]=90_010_000_000 (90 tỷ 10 triệu). Kiểm thử sư phụ và đệ tăng sát trần bằng Long.MAX_VALUE rồi thêm1 vẫn dừng đúng trần. Không sửa bảng giới hạn hoặc bậc khởi tạo đệ trong lần thay đổi này; kế hoạch đệ 40 tỷ bậc4 lưu riêng.

## Kiểm chứng

PetFusionBonusTest gồm5mode: bonus, stable, legacy, equipment, max. Đã bắt RED trên JAR cũ cho Beerus28.800 thay24.000, KI496.000 thay480.000, KI trang bị sai. Rà soát bắt thêm WorldCup cachedKI; test RED trên bản compile trung gian cho543.360 thay528.000 ở lần tính thứ2, sau đó sửa. Các fixture gear kiểm tra đồ đệ, optionPorata2(item921)/3(item1819), bóng966(type11) cùng cải trang883(type5) kích hoạtWorldCup. Giữ bonusWorldCup10%HP/KI/dame, không bị tính dồn.

Toàn bộ source và JAR cuối đạt66/66 lượt; các class sản xuất được đối chiếu, không có test/cache/entrytrùng. Review độc lập chấp thuận sau sửa WorldCup. Log: .analysis/pet-fusion-red/red.log, .analysis/pet-fusion-worldcup-red/red.log, .analysis/pet-fusion-build-final.log, .analysis/gold-economy-check/test.log, .analysis/release-20261002-171025/jar-tests.log. Chưa kiểm tra bằng client game thật hoặc tài khoản/liveDB.

## Triển khai

- Đã gửi bảo trì lưu dữ liệu, chờ server dừng, chuyển JAR cũ trước khi cài mới.
- JAR hiện tại SRC/20.jar, SHA-256 aad0030e7594e9b0ad5098d90aa11f47372a8eb302d2b256f574adab7fd66b33.
- Backup SRC/jar-backup/20-before-pet-fusion-bonus-20261002-171219.jar, SHA-256 c19de0aecc144b01c15cfdf2638049ce524bba78ddbd68771043b26e5fa3359f.
- Bản build trung gian không dùng cũng đã vào jar-backup:20-unused-fusion-before-worldcup-20261002-170720.jar.
- JavaPID19388 nghe cổng14445 sau khởi động lại.
- Metadata .analysis/latest-deployment.json và SRC/jar-backup/deployment-20261002-171219.json.
- Không còn JAR ứng dụng dư trong SRC hoặc thư mục release.

Báo cáo lỗi trước sửa: [pet-fusion-audit.md](pet-fusion-audit.md).
