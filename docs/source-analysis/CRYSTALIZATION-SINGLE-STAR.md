# Kiểm tra mỗi lượt pha lê hóa chỉ tăng một sao

Luồng cũ đã dừng sau lần thành công đầu tiên của nút 10/100 lần. Không tái hiện được lỗi tăng nhiều sao trong một lượt xử lý tuần tự. Tuy nhiên, kiểm thử tái hiện được lỗi lấy phí đã lưu từ menu: khi phí bằng 0, lượt đầu lên sao nhưng không trừ đủ vàng/ngọc. Kiểm tra và cập nhật cooldown cũng chưa được khóa đồng thời.

Đã sửa `PhaLeHoaTrangBi.phaLeHoa`:

- Khóa xử lý trên từng người chơi, gồm cooldown, trừ tài nguyên và cập nhật sao.
- Chỉ nhận một trang bị còn trong hành trang; từ chối sao âm, đủ 9 sao và option 107 trùng.
- Lấy phí trực tiếp từ số sao hiện tại, kiểm tra tiền trước mỗi lần thử.
- Nút 10/100 chỉ thử lại khi thất bại; dừng ngay ở thành công đầu tiên, tăng đúng một sao.
- Chỉ nhận số lần 1, 10 hoặc 100; không vượt 9 sao.

`CrystalizationSingleStarTest` chạy logic thật với đầu ra mạng thay thế: các nút 1/10/100, cooldown, thất bại rồi thành công ở sao 9, hết vàng giữa batch, dữ liệu không hợp lệ và hai yêu cầu đồng thời. Cùng bộ kiểm thử tỷ lệ và ép sao trực tiếp, chạy bằng `python .analysis/verify_crystalization.py`. Không chạy server hoặc kết nối database. Bản JAR chạy server chưa được thay thế.
