# Reset đệ tử khi đổi và kiểm tra đệ đặc biệt

> Dùng subagent-driven-development cho kiểm thử và kiểm tra tài nguyên độc lập. Người dùng đã yêu cầu thực hiện thay đổi; tiếp tục trong phạm vi đã được giao.

**Goal:** Đệ tử mới không kế thừa giới hạn đã mở của đệ cũ, và đối chiếu Uub/Kid Beer/Kid Jiren trong source, SQL và tài nguyên.

**Architecture:** Giữ mặc định khởi tạo riêng từng loại, đặt limitPower=0 và tiemNang=0. Tạo đệ đồng bộ dưới khóa sư phụ, chuẩn bị đầy đủ trước khi thay đối tượng; xử lý hợp thể/thoát map/xoá đệ cũ. Chỉ lời chào trễ còn chạy nền và phải kiểm tra đúng đối tượng mới. Không thay database của các đệ hiện có khi chưa đổi.

**Tech Stack:** Java 17, SQL dump, Python build/deploy tooling, Windows.

## Hành vi

Đệ thường: sức mạnh 2.000; Mabư: 1.500.000; Uub/Kid Beer/Kid Jiren: 40 tỷ, HP/KI gốc 400.000, sức đánh gốc 20.000 theo mặc định source. Tất cả đệ mới có giới hạn bậc 0 (17.999.999.999); đệ đặc biệt phải mở lại các bậc tại Quốc Vương mới tăng tiếp. Không kế thừa tiềm năng, chỉ số đã luyện, kỹ năng, đồ đang mặc hoặc tên cũ; đây là hành vi thay đệ hiện có, không bổ sung cơ chế hoàn đồ.

Giữ chữ ký API create* có byte... limitPower cho tương thích nhưng bỏ truyền/áp dụng bậc cũ. Sửa changeUub/changeKidBeer đọc đệ sau khi null và changeJiren gọi nhầm Beer. Vật phẩm 1795 dùng luồng thay đệ đúng, xác nhận và tiêu hao trong khóa sư phụ để không đổi/tiêu hao hai lần cùng yêu cầu.

## Các bước

- [x] Viết kiểm thử PetReplacementTest, đăng ký vào hai script; xác nhận RED trên JAR hiện tại.
- [x] Sao lưu source và script trước sửa.
- [x] Sửa PetService tạo/thay đệ nguyên tử, bậc 0, chat bắt đúng đối tượng.
- [x] Sửa UseItem.changePetRamdom dùng đúng change* và khóa xuyên suốt xác nhận/tiêu hao.
- [x] Kiểm tra SQL part và item 1795, ID hiển thị Pet, icon x1–x4, đường nạp DataGame và bảng part đóng gói.
- [x] Kiểm thử toàn bộ source và JAR, rà soát yêu cầu/chất lượng.
- [x] Xác nhận server đã dừng trước khi thay, chuyển JAR cũ vào jar-backup, cài bản mới, đối chiếu hash và cổng 14445. Server đã dừng sẵn nên không gửi lệnh bảo trì lần này.
- [x] Ghi báo cáo với phân biệt SQL dump, DB đang chạy và client thực tế.

## Kiểm thử

Bao phủ đổi thường/Mabư/ba loại đặc biệt từ đệ bậc cao; đúng loại/gender và bậc 0; chỉ số/mặc định mới; đệ cũ đã cleanup; không nhận legacy override. Luồng vật phẩm đủ 3000 Kilis/Mabư 40 tỷ, mất bình một lần, sai điều kiện giữ nguyên. Chạy lại kiểm thử mở giới hạn và auto cộng theo lô để xác nhận tương thích.

Đã đạt 61/61 lượt source và JAR. Kiểm thử bổ sung bảo vệ chỉ số Super Broly, chat trễ đúng đệ mới, điều kiện sai/replay/hai yêu cầu đổi đồng thời. Rà soát yêu cầu và chất lượng độc lập đã chấp thuận.
