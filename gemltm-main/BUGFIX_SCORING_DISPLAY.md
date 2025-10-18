# Fix: Hiển thị và Tính Điểm Sai

## 🐛 Vấn đề phát hiện

Bạn phát hiện 2 lỗi nghiêm trọng trong logic game:

### Lỗi 1: Tính điểm không công bằng
- ❌ **Trước**: Chỉ shooter được điểm khi ghi bàn
- ❌ Goalkeeper bắt được bóng nhưng không được điểm
- ✅ **Sau**: Cả hai đều được điểm mỗi lượt

### Lỗi 2: Hiển thị kết quả sai
- ❌ **Trước**: Server gửi cùng 1 message cho cả 2
  - Shooter nhận "win" khi ghi bàn → Đúng ✓
  - Goalkeeper cũng nhận "win" khi shooter ghi bàn → SAI ✗
- ✅ **Sau**: Gửi message riêng theo góc nhìn

## 🔧 Chi tiết sửa đổi

### File: `src/server/GameRoom.java`

#### Sửa 1: Thêm logic cộng điểm cho goalkeeper (dòng 147-153)

**Code cũ:**
```java
boolean goal = !shooterDirection.equalsIgnoreCase(goalkeeperDirection);
if (goal) {
    shooterScore++;  // Chỉ có dòng này
}
```

**Code mới:**
```java
boolean goal = !shooterDirection.equalsIgnoreCase(goalkeeperDirection);
if (goal) {
    shooterScore++;      // Shooter ghi bàn: +1
} else {
    goalkeeperScore++;   // Goalkeeper bắt được: +1
}
```

#### Sửa 2: Gửi kết quả riêng cho từng người (dòng 156-162)

**Code cũ:**
```java
String kick_result = (goal ? "win" : "lose") + "-" + shooterDirection + "-" + goalkeeperDirection;
shooterHandler.sendMessage(new Message("kick_result", kick_result));
goalkeeperHandler.sendMessage(new Message("kick_result", kick_result));
```

**Code mới:**
```java
// Shooter: "win" nếu ghi bàn, "lose" nếu bị bắt
String kick_result_shooter = (goal ? "win" : "lose") + "-" + shooterDirection + "-" + goalkeeperDirection;
// Goalkeeper: "win" nếu bắt được, "lose" nếu thủng lưới
String kick_result_goalkeeper = (goal ? "lose" : "win") + "-" + shooterDirection + "-" + goalkeeperDirection;

shooterHandler.sendMessage(new Message("kick_result", kick_result_shooter));
goalkeeperHandler.sendMessage(new Message("kick_result", kick_result_goalkeeper));
```

## 📊 Kết quả sau sửa

### Tính điểm
Mỗi lượt chơi, **1 trong 2 người được điểm**:

| Kết quả | Shooter | Goalkeeper |
|---------|---------|------------|
| Bóng vào lưới | +1 điểm ⚽ | +0 điểm |
| Bắt được bóng | +0 điểm | +1 điểm 🧤 |

→ Sau 10 rounds: **Tổng điểm luôn = 10**

### Hiển thị UI

#### Khi shooter ghi bàn:
- **Shooter thấy**: Animation "win" + "Bạn: X+1"
- **Goalkeeper thấy**: Animation "lose" + "Đối thủ: X+1"

#### Khi goalkeeper bắt được:
- **Shooter thấy**: Animation "lose" + "Đối thủ: X+1"
- **Goalkeeper thấy**: Animation "win" + "Bạn: X+1"

## ✅ Test case

### Test 1: Shooter ghi bàn
```
Server logic:
- shooterDirection = "Left"
- goalkeeperDirection = "Middle"
- goal = true (khác hướng)
- shooterScore++ → A: 1

Message gửi:
- Shooter A nhận: "win-Left-Middle" → Hiện "YOU WIN!"
- Goalkeeper B nhận: "lose-Left-Middle" → Hiện "YOU LOSE!"
```

### Test 2: Goalkeeper bắt được
```
Server logic:
- shooterDirection = "Right"
- goalkeeperDirection = "Right"
- goal = false (cùng hướng)
- goalkeeperScore++ → B: 1

Message gửi:
- Shooter A nhận: "lose-Right-Right" → Hiện "YOU LOSE!"
- Goalkeeper B nhận: "win-Right-Right" → Hiện "YOU WIN!"
```

## 🎯 Ví dụ trận đấu hoàn chỉnh

```
Round 1: A sút Left, B bắt Middle
  → GOAL → A+1 → Score: A:1, B:0
  → A thấy "win", B thấy "lose" ✓

Round 2: (Đổi vai trò) B sút Right, A bắt Right
  → SAVE → A+1 → Score: A:2, B:0
  → B thấy "lose", A thấy "win" ✓

Round 3: (Đổi vai trò) A sút Middle, B bắt Left
  → GOAL → A+1 → Score: A:3, B:0
  → A thấy "win", B thấy "lose" ✓

Round 4: (Đổi vai trò) B sút Left, A bắt Left
  → SAVE → A+1 → Score: A:4, B:0
  → B thấy "lose", A thấy "win" ✓

... tiếp tục đến round 10
```

## 🔄 Build lại

**Quan trọng:** Phải clean build để áp dụng thay đổi!

```bash
# Trong Eclipse
Project > Clean... > chọn project > Clean

# Hoặc command line
cd C:\Users\Lenovo\git\repository\gemltm-main\gemltm-main
rmdir /S /Q build
```

## 📝 Checklist kiểm tra

- [ ] Shooter ghi bàn → Shooter +1 điểm, hiện "win"
- [ ] Shooter ghi bàn → Goalkeeper +0 điểm, hiện "lose"
- [ ] Goalkeeper bắt được → Goalkeeper +1 điểm, hiện "win"
- [ ] Goalkeeper bắt được → Shooter +0 điểm, hiện "lose"
- [ ] Tổng điểm sau 10 rounds = 10
- [ ] UI animation phù hợp với kết quả của từng người

## 🙏 Cảm ơn

Cảm ơn bạn đã phát hiện bug này! Logic tính điểm và hiển thị giờ đã hoàn toàn chính xác.

---
**Ngày sửa:** 2025-10-19  
**Severity:** Critical (ảnh hưởng trực tiếp gameplay)
