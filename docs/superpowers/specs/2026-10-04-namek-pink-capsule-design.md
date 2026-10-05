# Capsule hồng ngọc từ Tiểu đội sát thủ Namek

Trạng thái: người dùng đã chọn5 capsule chung/phân phối80%–20% và yêu cầu triển khai. Đã cập nhật source/SQL/JAR, server nạp catalog14; xem [báo cáo kiểm chứng](../../source-analysis/namek-pink-capsule.md). Chưa nghiệm thu gameplay client thực tế.

## Cơ chế

- Dùng Capsule hồng hiện có ID722, loại27. Đề xuất đổi tên hiển thị thành “Capsule hồng ngọc”, mô tả “Mở nhận ngẫu nhiên 1–100 hồng ngọc. 80% nhận 1–5 hồng ngọc.” Giữ ID/type/icon để nhiệm vụ KOL5 và client tiếp tục nhận diện đúng.
- Mỗi boss Số4/Số3/Số2/Số1/Tiểu đội trưởng Namek, ID-311/-312/-313/-314/-315, chết hợp lệ rơi đúng 5 itemMap ID722, mỗi item quantity1. Đây là tổng cộng 5 viên chung, không phải 5 viên cho mỗi người.
- Capsule public ngay khi rơi (`playerId=-1`), bất kỳ người chơi trong khu có thể nhặt, không cần đánh/kill boss. Người vào khu sau cũng có thể nhặt nếu viên còn trên đất. Không chia riêng phần cho từng người; người nhanh có thể lấy cả5. Cơ chế hết hạn drop thông thường giữ nguyên.
- Giữ cải trang theo boss: Số4→429, Số3→430, Số2→431, Số1→432, trưởng→433. Giữ cả ngọc rồng6s ID19 và7s ID20 như hiện tại. Chỉ người kết liễu được nhặt các drop này trong toàn bộ thời gian tồn tại, không tự mở quyền nhặt sau45s.
- Giữ các drop vàng ID77 và điểm sự kiện hiện có; việc bỏ/thay drop vàng chưa được yêu cầu. Không thay Tiểu đội sát thủ thường ở package `tieu_doi_sat_thu`, không thay map/HP/chu kỳ hồi sinh.

## Mở capsule và phân phối đã chốt

Nhặt capsule đưa vào hành trang, chưa cộng hồng ngọc. Dùng một viên tiêu hao1 capsule và cộng hồng ngọc vào `player.inventory.ruby`; không cộng gem, VND hoặc điểm sự kiện; không tạo một item861 nằm trong bag vì loại34 là tiền tệ khi addItemBag.

| Khoảng | Xác suất nhóm | Xác suất từng số |
|---|---:|---:|
| 1–5 | 80% | 16% |
| 6–100 | 20% | 20%/95 ≈ 0,210526% |

Roll nhóm rồi roll đều trong khoảng: `roll100 < 80 ? 1 + roll5 : 6 + roll95`, các rollN lấy số nguyên0..N-1. Mỗi lần mở roll độc lập. 6 và100 có xác suất bằng nhau; không dùng phân phối giảm dần vì người dùng chọn chia đều phần còn lại.

Trung bình13 hồng ngọc/capsule; 5 viên/boss trung bình65; cả5boss rơi25capsule, tổng trung bình325 hồng ngọc nếu tất cả được mở. Đây là kỳ vọng thống kê, không phải mức thưởng bảo đảm. 200capsule của nhiệm vụ KOL5 có kỳ vọng2600 hồng ngọc, cần40boss/8lượt đủ đội nếu một người nhặt hết, chưa tính tranh nhặt.

## Ràng buộc dữ liệu và sai sót cần tránh

- KOL5 đã đếm nhặt ID722 từ đất thành công; mở/tiêu hao không giảm tiến độ. Không đếm khi dùng, tặng, giao dịch, di chuyển rương.
- Capsule cũ ID722 sẽ cùng được mở theo cơ chế mới. Không đổi/thu hồi số dư ruby hay item861 hiện có; chưa có yêu cầu di trú tài sản.
- Không dùng chung owner của drop killer cho capsule; không đổi quyền nhặt toàn bộ itemMap trong game. `ItemMap.update` hiện mở quyền sau45s: thêm cờ riêng cho đồ killer của nhóm này và kiểm tra owner chính xác khi cờ bật.
- Một boss/lượt sống không tạo capsule lặp nếu đường reward bị gọi lại; guard riêng và reset khi respawn. Không tái sử dụng guard KOL vì hai mục đích khác nhau.
- Mở kiểm tra item còn trong bag, ID/quantity hợp lệ dưới khóa player. Cộng bằng long; nếu kết quả vượt Integer.MAX_VALUE, từ chối trước khi trừ capsule, không clamp làm mất hồng ngọc.
- Cập nhật ruby và quantity trước khi gửi packet; lỗi gửi packet không roll/cấp lại. Với stack1, hai request đồng thời chỉ một request thành công; stack2 có thể mở hai viên hợp lệ.
- RAM của capsule/ruby dùng persistence inventory hiện có; thay đổi này không tự mở rộng thành hệ thống ledger cho mọi hành động dùng vật phẩm hoặc cam kết chống crash tuyệt đối.

## Lựa chọn thiết kế

5 viên rời được chọn vì nhiều người có cơ hội tranh nhặt từng viên. Một stack5 chỉ tạo một điểm nhặt và người nhặt lấy hết; 5 viên/người làm nguồn hồng ngọc tăng theo số tài khoản đứng trong khu và khác yêu cầu đã chốt. Không thêm điều kiện đóng góp damage hoặc chống AFK trong lần này.

## Nghiệm thu

Kiểm tra 5boss ×5capsule; nonkiller nhặt ngay; hai người tranh một viên chỉ một người nhận; capsule public nhưng costume/dragonball private cả sau45s; reward gọi lặp không nhân đôi, respawn cho drop mới; mở đúng ruby/tiêu hao/biên1,5,6,100/tràn số; tiến độ KOL nhặt không đổi khi mở; hồi quy đội boss thường và drop private/public khác. Triển khai đúng quy tắc bảo trì, backup JAR/SHA-256, metadata/rollback. Phân biệt test mô phỏng với gameplay client thực tế.
