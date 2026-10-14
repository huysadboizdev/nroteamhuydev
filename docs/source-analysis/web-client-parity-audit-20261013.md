# Kiểm tra game trình duyệt local — 13/10/2026

## Phạm vi

Người dùng xác nhận bản chơi trực tiếp trong trình duyệt, chạy local. Project đúng là `ClientWeb/PRJ_2Tab_Web`, phục vụ tại `http://localhost:8086`. Website PHP `webgoc2026` chỉ chứa gói tải, không phải bản chơi này. Không build/phát hành game hoặc thay JAR/APK/WebGL trong lượt kiểm tra.

## Lỗi kết nối đã tái hiện

- HTTP localhost:8086 trả 200, nên gateway và trang web đang chạy.
- `Builds/WebGL/config.js` chọn `160.191.245.239:14445`.
- `Gateway/config.json` có target sai `160.191.245.23912:21:14445`.
- Thử WebSocket trực tiếp vào gateway đang chạy: target `127.0.0.1:14445` và `160.191.245.239:14445` đều đóng với code 1008, lý do `Target not allowed`, không nhận byte TCP nào. Target sai đóng code 1011 `TCP unavailable`.
- Template config cho lần build tiếp theo lại chọn `127.0.0.1:14445`. Có ba cấu hình lệch nhau. File config không được gateway tự đọc lại sau khởi động.

## Chênh lệch phiên bản

Bản đang được index.html tham chiếu là `web-icon-black-aura-20261011-final`, build ngày 11/10. So sánh source native và web thấy 47 file C# chung có khác biệt; nhiều khác biệt là phần port trình duyệt cần giữ, không được chép đè toàn project.

Cả Game1 và Game2 trên web chưa có:

- `PickMobController.CanAutoAct`, xử lý focus hợp lệ và gửi tàn sát một lần.
- `Service.TryCharMove/TrySelectSkill/TrySendPlayerAttack` cùng kiểm tra kết nối.
- `GameScr.TrySelectSkillForAutoCombat`.
- Chặn vòng AutoAttack chạy cạnh tranh với tàn sát.
- Đọc đuôi hào quang đồ mới trong gói appearance -90.
- Setter `Char.idEff_Set_Item` xóa cache hiệu ứng khi đổi cấp.

HP/KI/sức đánh hợp thể, bonus đủ năm món, Killis và pet được server tính; không nhân đôi các bonus trong client web. Client cần đọc đúng packet và hiển thị đúng hiệu ứng.

Không tìm thấy IPA/Xcode build trong cây Builds native; chưa thể kết luận đã đối chiếu với bản iOS phát hành. Những bản APK/PC cũ cũng chưa chứa sửa tàn sát vừa sửa source ngày 13/10.

## Kiểm tra

- Node gateway/platform/viewport/branding: 17 tests, 17 pass.
- ResourceDownloadChecks, VirtualControlsChecks, TabSwitchChecks: pass cả hai tài khoản.
- EffectDataChecks: pass 150 hiệu ứng server cho mỗi tài khoản; có warning CS0675, không có failure. Đây là chương trình kiểm thử riêng, không phải bản build game.
- Browser network fixture: WebGL khởi tạo được, nhưng thao tác chọn server tự động không tạo kết nối TCP trước timeout 30 giây. Thử lại bằng click cũng timeout. Không tính đây là bằng chứng đăng nhập/chiến đấu thành công; cần kiểm tra tiếp điều khiển/chọn server khi sửa.
- Bằng chứng browser: `ClientWeb/PRJ_2Tab_Web/Tests/evidence/network-console.log`, `network-failure.png`.

## Phương án sửa đề xuất, chưa thực hiện

1. Cho local dùng `127.0.0.1:14445` nhất quán ở gateway, config đang phục vụ và template; kiểm tra server TCP thực sự chạy rồi khởi động lại riêng gateway. Sao lưu cấu hình trước khi thay.
2. Ghép riêng các thay đổi gameplay/protocol từ client chính vào cả hai tài khoản WebGL. Giữ WebSocket, lưu dữ liệu trình duyệt, coroutine, nhập liệu, responsive, kiểm tra số lượng resource unsigned và các sửa riêng của web.
3. Kiểm thử cấu hình thực, packet, tàn sát, hào quang và hai tài khoản; xử lý kiểm thử browser chọn server đang timeout trước khi tuyên bố hoàn tất.
4. Khi người dùng yêu cầu build, build các nền tảng từ cùng revision gameplay, lưu hash/version manifest và dùng tên asset mới để tránh cache cũ. Chưa build hoặc phát hành lúc này.

Chỉ sửa cấu hình giải quyết kết nối nhưng không bổ sung gameplay mới vào wasm đã build. Chép đè source native vào web làm mất phần port. Phương án khuyến nghị là đồng bộ có chọn lọc, rồi build WebGL khi được yêu cầu.
