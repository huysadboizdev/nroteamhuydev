# Whis theo tuần

Yêu cầu triển khai trực tiếp: sửa lỗi Whis, bảng top tuần và bonus hạng 1/2/3 là 5/4/3% HP/KI/sức đánh. Quy ước mặc định đã thông báo: tuần bắt đầu thứ Hai 00:00 Asia/Ho_Chi_Minh; chốt tuần vừa kết thúc, nhận bonus trong tuần kế tiếp. Nếu bỏ qua nhiều tuần do server tắt, chỉ tuần ngay trước tuần hiện tại có thể cấp bonus. Tuần đầu không nhập điểm lịch sử vào cuộc đua mới. Không thay đổi các sự kiện khác.

Lưu bảng điểm riêng có khóa tuần/người chơi, ngày đạt điểm và bảng người thắng để tránh PlayerDAO ghi đè khi reset. Transaction khóa singleton tuần, chốt tối đa ba người có điểm >0 theo wins DESC, last_win ASC, player_id ASC; bắt đầu điểm tuần mới bằng namespace tuần mới. SQL lưu điểm thắng và level training trong cùng transaction. Dữ liệu tuần trước giữ để đối chiếu.

Service xuất snapshot bất biến của top và bonus sau commit; tick mỗi ba giây để rollover ngay cả khi không ai thắng. Bonus cache hết hạn theo khóa tuần; không query SQL trong calPoint. Cộng vào chỉ số tổng sau tính đồ/hợp thể, clamp int, chỉ áp dụng người chơi, không nhân lặp mỗi lần calPoint.

Một player chỉ có một Whis hoạt động, admission chung cho cả hai TrainingService. Giữ token theo player.id cho tới leaveMap, từ chối người ngoài/tấn công mob, ghi nhận thắng một lần, chỉ chủ trận nhận điểm; level dùng max để không lùi. Không tự áp giới hạn một lượt/ngày chưa được yêu cầu; sửa chữ cố định gây hiểu nhầm.

Kiểm thử: ranh giới tuần/năm, hòa điểm, thắng một lần, người ngoài, trận trùng, cấp không lùi, bonus đúng/không tích lũy/không pet, restart, rollback SQL và offline nhiều tuần bằng DB test riêng. Build full source, kiểm chứng candidate trước bảo trì lưu dữ liệu, snapshot SQL, backup JAR timestamp và SHA-256, migration, thay JAR rồi xác nhận listener/runtime. Không commit toàn bộ workspace đang có thay đổi cũ.
