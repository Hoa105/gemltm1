# 🚀 Setup Nhanh - Penalty Shootout Game

## ✅ Đã hoàn thành setup!

Dự án đã được setup đầy đủ và sẵn sàng chạy:

### 📦 Đã cài đặt:
- ✓ MySQL database `penalty_shootout` đã được tạo
- ✓ Các bảng (users, matches, match_details) đã được tạo  
- ✓ User mẫu đã được thêm để test
- ✓ MySQL Connector đã được copy vào `lib/`
- ✓ Project đã được build thành công
- ✓ DatabaseManager đã được cấu hình với password đúng

### 👥 Users mẫu để test:
1. **player1** / pass123 (100 điểm)
2. **player2** / pass123 (80 điểm)
3. **testuser** / test (50 điểm)

## 🎮 Cách chạy:

### 1. Khởi động Server
```bash
cd /Users/himlam/Projects/Others/Chip/PenaltyShootoutClient
./server.sh
```

Thấy dòng này là thành công:
```
Server đã khởi động trên cổng 12345
```

### 2. Khởi động Client (terminal mới)
```bash
cd /Users/himlam/Projects/Others/Chip/PenaltyShootoutClient
./client.sh
```

Cửa sổ JavaFX sẽ mở ra với màn hình đăng nhập.

### 3. Đăng nhập & Chơi
- Dùng một trong các user mẫu ở trên để đăng nhập
- Hoặc tạo tài khoản mới (nếu có chức năng đăng ký)
- Chờ đối thủ kết nối
- Bắt đầu chơi game!

## 🔧 Nếu muốn rebuild:

```bash
cd /Users/himlam/Projects/Others/Chip/PenaltyShootoutClient
ant -Dplatforms.JDK_21_FX.home=/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home compile
```

## 🐛 Debug:

### Kiểm tra MySQL đang chạy:
```bash
mysql -uroot -e "SELECT 'OK';"
```

### Kiểm tra database:
```bash
mysql -uroot penalty_shootout -e "SHOW TABLES;"
```

### Xem users hiện có:
```bash
mysql -uroot penalty_shootout -e "SELECT * FROM users;"
```

### Kiểm tra port 12345:
```bash
lsof -i :12345
```

## 📝 Cấu hình hiện tại:

- **Java**: JDK 25 (tương thích JDK 21)
- **JavaFX**: Version 25 (tại `lib/javafx/javafx-sdk-25`)
- **MySQL**: localhost:3306
  - Database: `penalty_shootout`
  - User: `root`
  - Password: (empty)
- **Server Port**: 12345
- **MySQL Connector**: 9.0.0 (tại `lib/mysql-connector-j-9.0.0.jar`)

## ✨ Tất cả đã sẵn sàng!

Chỉ cần chạy `./server.sh` và `./client.sh` để bắt đầu chơi! 🎮⚽

