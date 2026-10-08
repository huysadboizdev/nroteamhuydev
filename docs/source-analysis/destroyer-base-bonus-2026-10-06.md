# Khôi phục bonus nền Hủy Diệt — 06/10/2026

Đồ mới đổi tại Beerus có hai bonus: tăng chỉ số gốc ngẫu nhiên0–15% theo phân phối cũ và một dòng HP/KI/sức đánh độc lập1–5%. Một mức tăng nền áp dụng cho các option0/22/23/14/27/28/47 đang có trên món, làm tròn xuống như code cũ. Yêu cầu sức mạnh21 và khóa giao dịch30 giữ nguyên. Không có optionấn. Không random lại đồ đã sở hữu hoặc thay đổi Thiên Sứ.

Phân phối tăng nền: 1% đạt15%;14% chọn11–14%;20% chọn7–10%;25% chọn4–6%;40% chọn0–3%. Ví dụ găng gốc8800 đạt15% thành10120, sau đó vẫn nhận một dòng HP/KI/SD1–5%.

Giá100ruby,99thứcăn,mónThầnđangmặc,đủbộ5món,mua lần tiếp thiếu1món và rollback giữ nguyên. Không thaySQL/client. JAR chỉ thay DestroyerBonusRoll.class, các class khác giữ nguyênbyte.

63 lượt kiểm thử PASS trên classes, candidate và JAR đã cài, gồm mức0–15%, cácoptionnền, bonus riêng, chỉ số khi mặc, giao dịch và hồi quy. Runtimecache15/15 và hash class khớp.

Server PID2508, cổng14445. Backup `SRC/jar-backup/20-before-destroyer-base-20261006-093442.jar`. JAR SHA-256 `98c22322b850b8d8e09c5793b4938b40aab5fddb55c002f7e2e0219ed32287cd`.

Bảo trì có lưu dữ liệu, snapshotSQL sau khi serverdừng, backup JARtimestamp/hash. Metadata `.analysis/destroyer-base-deployment-latest.json`. Rollback bằng bảo trì lưu dữ liệu và JARbackup; không restoreplayersnapshot.
