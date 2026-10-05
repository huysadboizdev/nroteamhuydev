# Crystalization Implementation Plan

**Goal:** Áp dụng đúng tỷ lệ 100%..5%, hiển thị trung thực và giữ tối đa 9 sao.

**Architecture:** Nguồn tỷ lệ chung trong CombineSystem. Hàm random riêng trong PhaLeHoaTrangBi dùng số nguyên có độ chính xác 0,001%. Phí và giao thức không thay đổi.

**Tech Stack:** Java 17, Ant/NetBeans, kiểm thử Java main độc lập.

- [x] Tạo SRC/test/nro/models/combine/CrystalizationRateTest.java, kiểm tra bảng mong muốn và biên; chạy trên source cũ để thấy tỷ lệ 1 sao sai (expected 100, actual 50).
- [x] Thay CombineSystem.getRatioPhaLeHoa bằng nội suy tuyến tính, từ chối sao ngoài 0..8.
- [x] Trong PhaLeHoaTrangBi, bỏ bảng fake, dùng tỷ lệ chung cho menu; thêm rollSuccess gọi Util.isTrue(long,long) với độ chính xác 0,001%.
- [x] Kiểm tra random bằng seed cố định đối chiếu xác suất mong muốn độc lập; kiểm tra trần 9 sao và cả API lấy tỷ lệ.
- [x] Biên dịch source và chạy test không khởi động server; ghi kết quả phân tích long/int và giới hạn client vào tài liệu.

Thực hiện trực tiếp trong workspace hiện tại; không có Git repository. Mã nguồn game chỉ sửa hai tệp combine.

Xác minh: `python .analysis/verify_crystalization.py`, COMPILE_EXIT=0, TEST_EXIT=0. JDK 21 biên dịch target Java 17. Cảnh báo unchecked/unsafe có sẵn trong source. Lượt kiểm tra được chạy ngoài sandbox hạn chế vì JDK không đọc/đóng JAR đúng trong sandbox; không chạy main server.
