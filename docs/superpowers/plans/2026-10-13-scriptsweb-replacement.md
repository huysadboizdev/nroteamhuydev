# Scriptsweb Replacement Implementation Plan

**Goal:** Project client web dùng trọn source Scriptsweb, scene/global type đúng, gateway local và mobile phù hợp.

**Architecture:** Import207source files theo cây gốc, bỏ AssemblyInfo thuộc metadata decompile. Dùng tài nguyên hiện có và overlay resource tham chiếu. Web integration riêng hỗ trợ browser; giữ UI/gameplay gốc.

**Tech Stack:** Unity2022.3 C#, JSlib, Node gateway, C# compiler validation và fixtures.

- [x] Ghi manifest source, tạo stagingAssets bằng Scriptsweb; kiểm tra import đủ và lớp Main chỉ một.
- [x] Tích hợp WebSocket/gateway cùng-origin và kiểm thử transport bằng fixture, giữ networking của Scriptsweb khi hợp lệ.
- [x] Tích hợp Main thiết bị/viewport, keyboard/storage; chuyển tác vụ thread của gameplay sang coroutine riêng cho web, giữ UI.
- [x] Validate toàn bộ C# staging bằng Unity assemblies và compiler riêng; sửa lỗi tích hợp, không build release.
- [x] Sao lưu Assets/settings/editor/tests cũ; cài source mới, scene trỏ GUIDMain củaScriptsweb; thay builder phù hợp một scene nhưng không chạy.
- [x] Review, kiểm thử lại import/platform/transport và ghi báo cáo rõ source/runtime chưa build.
