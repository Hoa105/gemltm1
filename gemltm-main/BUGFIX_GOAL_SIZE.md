# Fix: Điều chỉnh kích thước khung thành khớp với 6 ô chọn

## 🎯 Vấn đề
- Ảnh khung thành quá nhỏ (60% width, max 260px)
- 6 ô chọn không khớp với ảnh khung thành
- Vị trí các ô cố định không theo ảnh thực tế

## ✅ Giải pháp

### 1. Tăng kích thước ảnh khung thành

**File:** `src/client/GUI/GameRoomController.java`

**Trước:**
```java
double desiredGoalWidth = Math.min(paneWidth * 0.6, 260);  // 60%, max 260px
goalView.setLayoutY(-45);  // Vị trí cao quá
```

**Sau:**
```java
double desiredGoalWidth = Math.min(paneWidth * 0.75, 400);  // 75%, max 400px ✅
goalView.setLayoutY(20);   // Hạ xuống để thấy rõ hơn ✅
```

### 2. Ô chọn tự động khớp với ảnh khung thành

**Trước:** Dùng giá trị cố định
```java
double goalWidth = paneW * 0.6;   // Không khớp với ảnh
double goalHeight = 200;          // Cố định
double startX = (paneW - goalWidth) / 2.0;
double startY = 100;              // Không theo ảnh
```

**Sau:** Dùng kích thước và vị trí thực tế của ảnh
```java
double goalWidth = currentGoalWidth;              // Theo ảnh ✅
double goalHeight = goalView.getBoundsInParent().getHeight();  // Theo ảnh ✅
double startX = currentGoalLeftX;                 // Theo ảnh ✅
double startY = goalView.getLayoutY();            // Theo ảnh ✅
```

## 📊 Kết quả

### Trước
```
Ảnh khung thành: 60% width (nhỏ)
6 ô chọn: Vị trí cố định
→ Không khớp nhau
```

### Sau
```
Ảnh khung thành: 75% width (to hơn, max 400px)
6 ô chọn: Tự động căn theo ảnh
→ Khớp hoàn toàn ✅
```

## 🎨 Chi tiết layout

```
┌─────────────────────────────────────┐
│         Game Pane (600x400)         │
│                                     │
│   ┌──────────────────────────┐     │
│   │   Ảnh khung thành        │     │ Y=20
│   │   (75% width, ~450px)    │     │
│   │ ┌───┬───┬───┐            │     │
│   │ │ 1 │ 2 │ 3 │ Row 0      │     │
│   │ ├───┼───┼───┤            │     │
│   │ │ 4 │ 5 │ 6 │ Row 1      │     │
│   │ └───┴───┴───┘            │     │
│   └──────────────────────────┘     │
│                                     │
│            🧍 Thủ môn               │
│                                     │
│              ⚽ Bóng                │
│                                     │
│            🏃 Cầu thủ               │
└─────────────────────────────────────┘
```

## 🔧 Thay đổi chi tiết

| Tham số | Trước | Sau |
|---------|-------|-----|
| Khung thành width | 60% (max 260px) | 75% (max 400px) |
| Khung thành Y | -45 | 20 |
| Ô chọn width | Cố định 60% | Theo `currentGoalWidth` |
| Ô chọn height | Cố định 200px | Theo ảnh thực tế |
| Ô chọn startX | Tính toán riêng | Theo `currentGoalLeftX` |
| Ô chọn startY | Cố định 100 | Theo `goalView.getLayoutY()` |

## ✅ Build & Test

1. **Clean project** trong Eclipse:
   ```
   Project > Clean...
   ```

2. **Chạy client** và kiểm tra:
   - [ ] Ảnh khung thành to hơn, rõ hơn
   - [ ] 6 ô chọn khớp chính xác với khung thành
   - [ ] Click vào ô nào thì chọn đúng vị trí đó
   - [ ] Hover vào ô thì highlight (màu xanh)

## 🎮 Tương tác người dùng

### Click vào ô
```
Row 0, Col 0 (Top-Left)    → "Left"
Row 0, Col 1 (Top-Middle)  → "Middle"
Row 0, Col 2 (Top-Right)   → "Right"
Row 1, Col 0 (Bot-Left)    → "Left"
Row 1, Col 1 (Bot-Middle)  → "Middle"
Row 1, Col 2 (Bot-Right)   → "Right"
```

### Visual feedback
- **Hover**: Ô chuyển sang màu xanh nhạt (0.3 alpha)
- **Normal**: Ô trong suốt (0.1 alpha)
- **Border**: Viền trắng

## 📝 Ghi chú

- Kích thước ảnh khung thành giờ responsive: 75% width nhưng không vượt quá 400px
- Nếu pane nhỏ (mobile/tablet), khung thành sẽ tự scale xuống
- Các ô chọn luôn khớp 100% với ảnh, dù resize window

## 🔄 Nếu muốn điều chỉnh thêm

### Tăng/giảm kích thước khung thành
```java
// Dòng 166: Thay 0.75 thành giá trị khác (0.6-0.9)
double desiredGoalWidth = Math.min(paneWidth * 0.8, 450);  // 80%, max 450px
```

### Thay đổi vị trí Y
```java
// Dòng 169: Thay 20 thành giá trị khác
goalView.setLayoutY(10);   // Cao hơn
goalView.setLayoutY(50);   // Thấp hơn
```

---
**Ngày cập nhật:** 2025-10-19  
**Mục đích:** Cải thiện UX và khớp layout
