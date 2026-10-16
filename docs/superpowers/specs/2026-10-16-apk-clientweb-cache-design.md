# APK và cache ClientWeb

## Mục tiêu

Xuất một APK Android mới từ client native chính và cập nhật client game WebGL để lần mở lại tái sử dụng dữ liệu build đã cache trong trình duyệt.

## Phạm vi

- APK build từ `C:/Users/Admin/Downloads/PRJ_2Tab_550K` vào thư mục phát hành mới, không ghi đè các bản cũ. Giữ package ID, icon, landscape, IL2CPP, ARMv7 + ARM64 theo cấu hình phát hành hiện có. Project chưa cấu hình keystore riêng nên APK sẽ dùng Unity development keystore.
- WebGL build từ `ClientWeb/PRJ_2Tab_Web`. Đây là game chạy trong trình duyệt; không thay đổi website PHP `webgoc2026`.
- Giữ cache tài nguyên game trên trình duyệt bằng IndexedDB cho gói `.data`; các file build có tên phiên bản tiếp tục dùng HTTP cache bất biến. Mỗi build mới phải đổi URL/validator để trình duyệt nhận đúng dữ liệu mới.
- Không cache tài khoản, thông tin đăng nhập, phiên chơi hoặc dữ liệu gameplay của người chơi.

## Thiết kế và luồng

1. Trước build, ghi nhận trạng thái source, scene, Android package/version, gateway config và tên/hash của WebGL hiện hành.
2. Dựng APK bằng Unity `ClientDistributionBuild.Android` vào folder timestamp mới. Không cài đè artifact hiện có.
3. Kiểm chứng cache trong trình duyệt với profile sạch: lần đầu tải `.data` và lưu IndexedDB; lần mở lại cùng build xác thực ETag/Last-Modified rồi đọc dữ liệu đã cache mà không tải lại body lớn. Kiểm tra thêm khi validator thay đổi để build mới được tải.
4. Nếu kiểm chứng phát hiện lỗi, thêm regression test cho luồng tái sử dụng cache trước khi sửa. Giữ việc dọn cache/gỡ lỗi ở mức cần thiết và không đổi giao thức đăng nhập/gameplay.
5. Build WebGL vào staging riêng. Giữ lại `config.js` đang phục vụ cùng server/gateway allowlist. Sau khi build hợp lệ, backup `Builds/WebGL` hiện hành rồi cài output mới vào vị trí gateway đang phục vụ.
6. Mở browser build sau cập nhật, xác nhận game shell/load asset, ETag/cache headers và không có lỗi Console. Không tuyên bố đăng nhập/đánh quái thật nếu không có phiên server và tài khoản kiểm thử được cung cấp.

## Thành phần liên quan

- `PRJ_2Tab_550K/Assets/Editor/ClientDistributionBuild.cs`: build và kiểm tra APK phát hành.
- `ClientWeb/PRJ_2Tab_Web/Assets/WebGLTemplates/NroWeb/data-cache.js`: IndexedDB cache và validator.
- `ClientWeb/PRJ_2Tab_Web/Assets/WebGLTemplates/NroWeb/index.html`: nối cache vào Unity WebGL loader.
- `ClientWeb/PRJ_2Tab_Web/Gateway/server.cjs`: ETag, Last-Modified và Cache-Control cho build assets.
- `ClientWeb/PRJ_2Tab_Web/Gateway/server.test.cjs`: kiểm tra headers và HTTP revalidation.
- `ClientWeb/PRJ_2Tab_Web/Builds/WebGL`: build hiện được gateway phục vụ; phải backup trước khi thay.

## Tiêu chí nghiệm thu

- APK mới có package/kiến trúc/chữ ký hợp lệ, checksum và metadata build; APK cũ không bị ghi đè.
- Lần khởi động đầu tải `.data` đầy đủ và cache tồn tại trong IndexedDB.
- Lần mở lại cùng URL/build không tải lại body `.data`; nếu server không truy cập được nhưng cache đầy đủ, cache dùng được theo chính sách hiện có.
- Validator hoặc URL build thay đổi khiến client tải dữ liệu mới; dữ liệu cũ không được dùng nhầm.
- WebGL artifact mới chạy qua gateway với cấu hình kết nối cũ; bản build trước được giữ ở backup riêng.
- Website PHP, JAR server và dữ liệu tài khoản không bị thay đổi.
