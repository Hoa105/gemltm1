# Changelog: 10 Rounds với Đổi Vai Trò

## Tổng quan
Đã sửa đổi game để 2 người chơi luân phiên vai trò sút/bắt trong 10 rounds, kiểm tra kết quả sau khi hoàn thành tất cả 10 rounds.

## Các thay đổi chính

### 1. Server - GameRoom.java

#### Thay đổi số rounds
- **MAX_ROUNDS**: 6 → 10 (tổng 10 rounds)
- **WIN_SCORE**: 3 → 10 (không còn dùng điều kiện thắng sớm)

#### Sửa logic tính điểm (QUAN TRỌNG!)
**Trước:** Chỉ cộng điểm cho shooter khi ghi bàn, goalkeeper không được điểm khi bắt bóng ❌
```java
if (goal) {
    shooterScore++;  // Chỉ có dòng này
}
```

**Sau:** Cả hai đều được điểm ✅
```java
if (goal) {
    shooterScore++;      // Shooter ghi bàn: +1 điểm
} else {
    goalkeeperScore++;   // Goalkeeper bắt được: +1 điểm
}
```

#### Sửa hiển thị kết quả (QUAN TRỌNG!)
**Trước:** Gửi cùng 1 message cho cả 2 người → Hiển thị sai ❌
```java
String kick_result = (goal ? "win" : "lose") + "-" + ...;
shooterHandler.sendMessage(new Message("kick_result", kick_result));
goalkeeperHandler.sendMessage(new Message("kick_result", kick_result));
```

**Sau:** Gửi message riêng theo góc nhìn của từng người ✅
```java
// Shooter: "win" nếu ghi bàn, "lose" nếu bị bắt
String kick_result_shooter = (goal ? "win" : "lose") + "-" + ...;
// Goalkeeper: "win" nếu bắt được, "lose" nếu thủng lưới  
String kick_result_goalkeeper = (goal ? "lose" : "win") + "-" + ...;

shooterHandler.sendMessage(new Message("kick_result", kick_result_shooter));
goalkeeperHandler.sendMessage(new Message("kick_result", kick_result_goalkeeper));
```

#### Logic đổi vai trò mới
- **Method mới `swapRoles()`**: Đổi vai trò giữa shooter và goalkeeper sau mỗi round
  - Hoán đổi shooterHandler ↔ goalkeeperHandler
  - Hoán đổi shooterScore ↔ goalkeeperScore (vì điểm gắn với handler)
  
#### Luồng game mới
1. **Round 1**: Player A = Shooter, Player B = Goalkeeper
2. **Round 2**: Player B = Shooter, Player A = Goalkeeper
3. **Round 3**: Player A = Shooter, Player B = Goalkeeper
4. ... và tiếp tục luân phiên đến round 10

#### Điều kiện kết thúc
- **Trước**: Kết thúc sớm nếu:
  - Ai đó không thể đuổi kịp điểm
  - Đạt 3 điểm trong điều kiện đặc biệt
- **Sau**: Chỉ kết thúc khi `currentRound > 10`, không kết thúc sớm

#### Thông báo vai trò mới
- Sau mỗi round, gửi message `role_change` với nội dung:
  - Người sút: "Round X: Bây giờ bạn là người sút."
  - Thủ môn: "Round X: Bây giờ bạn là người bắt."

### 2. Client - Client.java
- Thêm xử lý case `"role_change"` trong switch
- Gọi `gameRoomController.handleRoleChange()` khi nhận message

### 3. Client - GameRoomController.java
- **Method mới `handleRoleChange(String message)`**:
  - Cập nhật biến `yourRole` ("Shooter" hoặc "Goalkeeper")
  - Hiển thị Alert thông báo vai trò mới
  - Reset trạng thái nút (disable cả shootButton và goalkeeperButton)
  - Chờ message `your_turn` từ server để kích hoạt lại nút tương ứng

## Cách hoạt động chi tiết

### Ví dụ trận đấu 10 rounds

| Round | Player A      | Player B       | Ai sút trước |
|-------|--------------|----------------|--------------|
| 1     | Shooter      | Goalkeeper     | A            |
| 2     | Goalkeeper   | Shooter        | B            |
| 3     | Shooter      | Goalkeeper     | A            |
| 4     | Goalkeeper   | Shooter        | B            |
| 5     | Shooter      | Goalkeeper     | A            |
| 6     | Goalkeeper   | Shooter        | B            |
| 7     | Shooter      | Goalkeeper     | A            |
| 8     | Goalkeeper   | Shooter        | B            |
| 9     | Shooter      | Goalkeeper     | A            |
| 10    | Goalkeeper   | Shooter        | B            |

### Luồng xử lý sau mỗi round

1. **Kết thúc 1 lượt**: Server nhận đủ hướng sút + hướng bắt
2. **Tính kết quả**: Goal hoặc Save
3. **Cập nhật điểm**: Gửi `update_score` cho cả 2 client
4. **Tăng round**: `currentRound++`
5. **Kiểm tra kết thúc**:
   - Nếu `currentRound > 10`: Gọi `determineWinner()` → Kết thúc
   - Ngược lại: Tiếp tục
6. **Đổi vai trò**: Gọi `swapRoles()`
7. **Thông báo**: Gửi `role_change` cho cả 2 client
8. **Bắt đầu round mới**: Gọi `requestNextMove()`

## Điểm lưu ý khi test

### Build lại project
Đảm bảo clean và rebuild để áp dụng các thay đổi:
```bash
# Trong Eclipse
Project > Clean... > chọn project > Clean

# Hoặc xóa thủ công
rmdir /S /Q build
```

### Kiểm tra các scenario

1. **Chơi đủ 10 rounds**:
   - Mỗi người sút 5 lần, bắt 5 lần
   - Sau round 10, hiển thị kết quả cuối cùng

2. **Đổi vai trò sau mỗi round**:
   - Round lẻ (1,3,5,7,9): Player ban đầu là shooter
   - Round chẵn (2,4,6,8,10): Player ban đầu là goalkeeper

3. **Tính điểm đúng**:
   - Điểm của mỗi người được swap theo vai trò
   - Hiển thị điểm trên UI luôn đúng với vai trò hiện tại

4. **Alert đổi vai trò**:
   - Sau mỗi round, hiện thông báo vai trò mới
   - Nút shoot/goalkeeper được enable đúng theo vai trò

## Những thay đổi không làm

- **Không thay đổi** database schema (vẫn lưu match_details bình thường)
- **Không thay đổi** logic timeout
- **Không thay đổi** logic rematch
- **Không thay đổi** animation sút/bắt

## Tương thích ngược

- Các trận đấu cũ trong database vẫn xem được (có thể < 10 rounds)
- Logic rematch vẫn hoạt động bình thường
- UI/UX vẫn giữ nguyên ngoại trừ thêm alert đổi vai trò

## Troubleshooting

### Lỗi compile "cannot find symbol swapRoles()"
- Đảm bảo đã lưu file GameRoom.java
- Clean project và rebuild

### Client không nhận được role_change
- Kiểm tra Client.java đã thêm case "role_change"
- Kiểm tra GameRoomController.java đã có method handleRoleChange()

### Điểm hiển thị sai sau đổi vai trò
- Server đã swap cả shooterScore và goalkeeperScore trong swapRoles()
- Message update_score luôn gửi đúng thứ tự: [điểm của bạn, điểm đối thủ, round]

## Ngày cập nhật
2025-10-19
