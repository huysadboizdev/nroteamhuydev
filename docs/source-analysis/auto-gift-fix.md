# Auto mở quà và capsule

Lỗi được tái hiện: capsule hồng ngọc 722 thiếu trong danh sách auto; capsule mở cải trang 962/963 chưa được phân loại là item nhận thưởng trong bước dừng khi túi đầy. Nút auto trước đây đứng sau các nút sử dụng cho đệ tử và bỏ ra/bán, dễ bị khuất trên màn hình nhỏ.

Bản sửa ở cả Game1 và Game2 bổ sung 722, phân loại 962/963 và đặt Auto sử dụng / Dừng auto ngay sau Sử dụng. Nhập khoảng cách giây giữ nguyên, 0 chạy nhanh nhất với một yêu cầu chờ xác nhận mỗi lần. Auto dừng khi túi không đủ ô hoặc server báo đạt giới hạn hồng ngọc/giao dịch chưa kết thúc.

Đã đối chiếu danh mục thực tế và luồng mở của server: capsule kì bí 380, hồng ngọc 722, cap thời trang 5/7 ngày 962/963, hộp giáng sinh 648, hộp 5 sao 736, các hộp 1170/1171, rương 1560/1569, hộp Goku Day 1591/1592/1594/1840, quà thiếu nhi 1608, quà Cađíc 1757, hộp thần linh 1775, quà sự kiện 1776/1777 và trứng vàng rồng nhí 1821. Trang bị/cải trang để mặc vẫn bị loại khỏi auto. Các template hộp chưa có handler mở ở server không được bật auto chỉ dựa vào tên hoặc type 27; capsule dịch chuyển 193/194 cũng không phải capsule mở quà.

Server 962/963 nhận thưởng thành công trước khi trừ capsule, giữ khóa player xuyên suốt kiểm tra và nhận thưởng. Capsule 722 dùng luồng nguyên tử có sẵn, bảo vệ túi đầy và tràn số dư hồng ngọc. Giới hạn buff 500 phút giữ nguyên.

Kiểm tra gồm 50 ca auto client (bao gồm 20 loại quà và loại trừ trang bị), 4 ca chat thực PC/mobile, biên dịch toàn bộ Unity; 137 ca nguồn server và 137 ca JAR đóng gói. Metadata/hash/kiểm tra JVM: `.analysis/auto-gift/deployment.json`, `.analysis/auto-gift/verification.json`. Không tiêu hao item của tài khoản online để chạy kiểm thử.
