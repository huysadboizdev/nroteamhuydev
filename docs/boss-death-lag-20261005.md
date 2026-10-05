# Boss HP0 vẫn đứng trên bản đồ

Snapshot runtime cho thấy Tiểu đội trưởng ID -27 HP0, trạng thái ACTIVE trên map79; boss update thread vẫn chạy. Lịch sử boss đã ghi lượt chết do admin hạ. Đây là trạng thái server bị kích hoạt lại, không phải client thiếu bản vẽ.

Nguyên nhân tái hiện: SO1/SO2.doneChatE gọi parentBoss.changeStatus(ACTIVE) mà không kiểm tra HP của parent. Nếu người chơi hạ Tiểu đội trưởng trước khi các callback đó hoàn tất, chúng ghi đè trạng thái chết của boss. Các callback tương tự tồn tại ở nhóm Namek, Pic, Bojack và Xên con.

Sửa tại Boss.changeStatus: boss HP<=0 không được chuyển sang ACTIVE/AFK/CHAT_S. Kiểm tra HP và gán trạng thái trong khóa ngắn trên cùng boss với injured(), ngăn callback ghi đè trạng thái khi đòn kết liễu chạy đồng thời. Hook thông báo ở ngoài khóa mới này. Giữ nguyên pha DIE/CHAT_E/LEAVE_MAP/REST. RESPAWN vẫn được phép để luồng hồi sinh khôi phục HP như cũ.

Kiểm thử BossDeathLifecycleTest đã thất bại trên JAR cũ tại callback SO1, qua trên JAR mới với callback Số1/Số2 thường và Namek. Kiểm tra bảo toàn các pha chết/rời/nghỉ, cho phép hồi sinh và kích hoạt boss còn sống. Toàn bộ 51 nhóm server qua. Thay đổi không yêu cầu client mới.

Đã cài JAR, sao lưu phiên bản cũ và snapshot SQL sau bảo trì có lưu dữ liệu. Server PID27604 nghe port14445. Snapshot sau cập nhật không có boss HP0 còn trong pha chiến đấu trên bản đồ. Tiểu đội trưởng đã hồi sinh 50.000.000 HP, trạng thái AFK chờ đội. Chưa hạ boss trực tiếp bằng tài khoản người chơi sau cập nhật.
