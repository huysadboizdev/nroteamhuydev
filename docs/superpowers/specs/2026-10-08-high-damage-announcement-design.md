# Thông báo sát thương — thiết kế đã được giao triển khai

Người dùng đã yêu cầu triển khai kế hoạch ngày08/10/2026. Dùng chính sách mặc định đã đề xuất: mỗi người1 tin/5 giây, toàn server1 tin/giây. Hit bị giới hạn bỏ qua, không backlog.

Chỉ sát thương trực tiếp của người chơi thật lên boss/người/quái, HP thực tế bị trừ >20.000.000. Không tính overkill, miss, dame boss/bot/pet, phản dame/độc. Chụp tên người chơi và chiêu theo ID thực thi trước đòn đánh; scope theo mục tiêu và khôi phục sau nested call, chỉ ghi nhận đúng HP subtraction của hit.

Nội dung cùng một bản trên packet92 thế giới dưới tên Hệ thống và packet-25 info phía dưới, toàn server, không trừ ngọc/cooldown chat. `Người chơi abc dùng chiêu Kamejoko với mức sát thương là 25.000.000.` Tên làm sạch mã điều khiển/mã màu và rút gọn khi cần <=100 ký tự; giữ nguyên dame.

Giữ combat/death/drop/lịch sử boss không đổi, send lỗi không làm hit thất bại. Service policy/clock/sink độc lập cho test, rate admission atomic với bounded memory, không ghi SQL perhit. Không cần client/migration SQL.

Triển khai TDD feature/combat/packet tests, regression BossNotice/HighHpSkill/Giftcode, build classes/candidate; review spec và code. Dùng bảo trì có lưu, backup SQL/JAR timestamp+hash, cài khi server dừng, startup/listener verification. Chưa thử trên tài khoản người chơi thật.
