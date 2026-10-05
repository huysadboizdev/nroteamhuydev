# Nhiệm vụ KOL tại Quy Lão Kame — kiểm tra ngày 04/10/2026

Đối chiếu định nghĩa 14 nhiệm vụ trực tiếp trong `SRC/20.jar`, mã nguồn `SRC/src/nro/models/npc_list/QuyLaoKame.java`, và tên vật phẩm trong database hiện tại bằng SELECT chỉ đọc. Kết quả thô: `.analysis/vip-reward-check/kol-catalog.json`. Không thay đổi mã nguồn ứng dụng, JAR hoặc database.

## Tên nhiệm vụ và phần quà

Hai chuỗi KOL thường và KOL VIP có cùng 7 điều kiện. Phải nhận thưởng lần lượt từ nhiệm vụ 1 đến 7; tiến độ chuỗi thường và VIP được lưu riêng.

| STT | Nhiệm vụ theo menu | Điều kiện mã nguồn kiểm tra | Quà KOL thường | Quà KOL VIP |
| --- | --- | --- | --- | --- |
| 1 | Thu thập cuốn chả giò (quái doanh trại) | 100 Cuốn chả giò, ID 1778, trong túi | 5 Trứng vàng rồng nhí (1821) | 10 Trứng vàng rồng nhí (1821) |
| 2 | Thu thập chai cuke 2 lít (boss doanh trại) | 10 vật phẩm ID 1824 trong túi; database hiện đặt tên **Cậu Vàng**, lệch mô tả menu | 5 Hộp quà Goku Day VIP (1592) + 5 Hộp quà Cađíc VIP (1757) | 10 Hộp quà Goku Day VIP (1592) + 10 Hộp quà Cađíc VIP (1757) |
| 3 | Hoàn thành phó bản Destron Gas cấp 70 trên 20 lần | Bộ đếm hoàn thành đạt **20 lần**, phó bản cấp **70 trở lên** | 1 Cờ Ma đèn nhảy múa (1360), hạn 90 ngày | 1 Cờ Ma đèn nhảy múa (1360), hạn 90 ngày |
| 4 | Đánh bại 10 người trong đại hội võ thuật | Bộ đếm thắng trận đại hội võ thuật đạt 10 | 1 Pet Cerberus (1654), hạn 30 ngày | 1 Pet Cerberus (1654), hạn 30 ngày |
| 5 | Hoàn thành 30 nhiệm vụ siêu khó hàng ngày | Bộ đếm `dailySuperHardQuestCompletionCount` đạt 30 | 10 Rada ngọc rồng (1822) | 20 Rada ngọc rồng (1822) |
| 6 | Tham gia hạ gục boss Baby 5 lần | Bộ đếm `bossBabyDefeatParticipationCount` đạt 5 | 1 Cải trang Picolo (1797), hạn 90 ngày + 5 Hộp quà Goku Day VIP (1592) + 5 Hộp quà Cađíc VIP (1757) | 1 Cải trang Picolo (1797), hạn 90 ngày + 10 Hộp quà Goku Day VIP (1592) + 10 Hộp quà Cađíc VIP (1757) |
| 7 | Hạ 100.000 quái (dùng tự động luyện tập) | Bộ đếm `monsterKillCountAutoTrain` đạt 100.000 | 10 Hộp quà Goku Day VIP (1592) + **9.999 Xúc xích (664)** + 5 Hộp quà Cađíc VIP (1757) | 20 Hộp quà Goku Day VIP (1592) + 10 Hộp quà Cađíc VIP (1757); **không có Xúc xích** |

## Chỉ số các phần quà có hạn sử dụng

Áp dụng giống nhau cho KOL thường và VIP:

- Cờ Ma đèn nhảy múa (1360): sức đánh +13%, HP +13%, KI +13%, tiềm năng/sức mạnh +20%; không thể giao dịch; hạn 90 ngày.
- Pet Cerberus (1654): sức đánh +16%, HP +15%, KI +15%, không ảnh hưởng bởi cái lạnh; không thể giao dịch; hạn 30 ngày.
- Cải trang Picolo (1797): sức đánh +25%, KI +30%; không thể giao dịch; hạn 90 ngày. Luồng này không thêm option tăng HP.
- Những quà còn lại không được thêm option/hạn sử dụng tại luồng thưởng KOL. Riêng Rada ngọc rồng (1822) được cấp trực tiếp, không thêm chỉ số tại đây; không suy ra là bản Rada VIP có chỉ số từ tên vật phẩm.

## Quy tắc nhận thưởng và điểm lệch cần lưu ý

1. KOL VIP yêu cầu có ít nhất 1 **Vé Nhệm Vụ KOL Vip** (ID 1825, tên đúng trong database) trong túi khi nhận thưởng. Luồng này chỉ kiểm tra vé, **không trừ vé**. Không kiểm tra cấp VIP mùa của người chơi.
2. Nhiệm vụ thu thập 1 và 2 trừ đủ lượng vật phẩm yêu cầu khi nhận thưởng. Những bộ đếm nhiệm vụ 3–7 không bị reset ở luồng nhận thưởng và được cả hai chuỗi sử dụng.
3. Nhiệm vụ 2 lệch tên rõ ràng: mô tả “chai cuke 2 lít” nhưng kiểm tra ID 1824 = Cậu Vàng.
4. Nhiệm vụ 3 ghi “trên 20 lần” nhưng code cho nhận ngay khi đạt 20; hoàn thành phó bản cấp từ 70 trở lên được tính. Điểm cộng ở `DestronGas.sendThanhTichKhiGas`.
5. Nhiệm vụ 5: qua tìm kiếm toàn bộ source, chưa thấy luồng tăng `dailySuperHardQuestCompletionCount`. `TaskService.paySideTask` trả thưởng nhiệm vụ hàng ngày nhưng không tăng bộ đếm này. Có dấu hiệu nhiệm vụ bị kẹt nếu bộ đếm ban đầu chưa đủ 30; chưa kiểm thử thực tế trên nhân vật.
6. Nhiệm vụ 6 ghi “tham gia” nhưng source cộng bộ đếm cho `plKill` tại `Baby.reward`, không cộng cho toàn bộ người tham gia.
7. Nhiệm vụ 7 ghi “dùng tự động luyện tập” nhưng luồng `Mob` không kiểm tra bật tự động luyện tập khi tăng bộ đếm; điều kiện vùng là `MapService.AllMap(mapid)`, tức ID map từ 0 đến 163.

## Nguồn đối chiếu

- `QuyLaoKame.java`: định nghĩa KOL thường ở dòng 65–71, KOL VIP ở dòng 77–83; nhận thưởng ở `handleKOLQuestRewardConfirm`; option quà ở `getPetBackpackOptions`.
- `DestronGas.java`: `sendThanhTichKhiGas`, cộng số lần hoàn thành cấp từ 70.
- `WorldMartialArtsTournament.java`: cộng `martialArtsTournamentWins` khi thắng trận.
- `Baby.java`: `reward`, cộng bộ đếm cho người nhận thưởng hạ boss.
- `Mob.java`: tăng số quái; `MapService.java`: phạm vi `AllMap`.
- `PlayerDAO.java` / `MrBlue.java`: lưu và tải tiến độ từ cột `player.nhiem_vu_kol`.
