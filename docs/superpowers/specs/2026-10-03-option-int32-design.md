# Option 32-bit server/client

Theo yêu cầu sửa đồng bộ source client tại PRJ_2Tab_550K và server, giữ nguyên option param int trong dữ liệu. Client Game1/Game2 quảng bá capability IOP32 trong chuỗi platform; server xác nhận bằng packet -29/sub100 trước gửi item. Client chỉ đọc int khi đã nhận xác nhận, mặc định đọc unsigned short và reset khi ngắt kết nối. Server chỉ gửi int cho phiên đã quảng bá capability; phiên cũ giữ short, giới hạn giá trị hiển thị unsigned16 để không wrap.

Một codec chung phía server áp dụng cho mọi option item/radar: túi, body, box, login, pet, shop, giao dịch, ký gửi và radar. Giao dịch phải chọn codec theo người nhận. Hai namespace client dùng codec tương ứng cho đúng những lần đọc param option, giữ ID/count/quantity/các trường khác như cũ.

Kiểm tra Java và C# phải đọc đúng 32767,32768,65535,65536,100000,1000000,INT_MAX và không lệch trường tiếp theo. Kiểm tra legacy và reset khi reconnect; build source Java và C# trước cài server. Backup client và JAR; chưa có build client mới thì bản client cũ vẫn dùng protocol legacy. Không chuyển count mảnh sang quantity hoặc sửa DB trong thay đổi này.
