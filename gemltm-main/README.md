# Penalty Shootout Game 🎮⚽

Game đá phạt đền (Penalty Shootout) được xây dựng bằng Java và JavaFX, hỗ trợ chơi multiplayer qua mạng.

## 📋 Yêu cầu hệ thống

### Phần mềm cần thiết:
- **Java Development Kit (JDK)**: Phiên bản 21 trở lên
- **JavaFX SDK**: Phiên bản 25 (đã có sẵn trong `lib/javafx/javafx-sdk-25`)
- **MySQL Server**: Phiên bản 8.0 trở lên
- **MySQL Connector/J**: Phiên bản 9.0.0

### Kiểm tra Java version:
```bash
java -version
```
Đảm bảo version là 21 hoặc cao hơn.

## 🔧 Cài đặt

### 1. Cài đặt MySQL
Nếu chưa có MySQL, tải và cài đặt từ: https://dev.mysql.com/downloads/mysql/

### 2. Thiết lập Database
Tạo database cho game:
```sql
CREATE DATABASE penalty_shootout;
USE penalty_shootout;

-- Tạo bảng users
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tạo bảng matches (nếu cần)
CREATE TABLE matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT,
    player2_id INT,
    winner_id INT,
    score_player1 INT,
    score_player2 INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player1_id) REFERENCES users(id),
    FOREIGN KEY (player2_id) REFERENCES users(id),
    FOREIGN KEY (winner_id) REFERENCES users(id)
);
```

**Lưu ý**: Các user mẫu đã được tạo để test:
- Username: `player1`, Password: `pass123` (100 điểm)
- Username: `player2`, Password: `pass123` (80 điểm)  
- Username: `testuser`, Password: `test` (50 điểm)

### 3. Cấu hình Database Connection
Kiểm tra và điều chỉnh thông tin kết nối database trong file:
```
src/server/DatabaseManager.java
```

Thông tin mặc định:
- **Host**: localhost
- **Port**: 3306
- **Database**: penalty_shootout
- **Username**: root
- **Password**: (empty/không có password)

### 4. Chuẩn bị MySQL Connector
MySQL Connector/J đã có sẵn trong project cũ. Script sẽ tự động tìm và sử dụng.

Nếu cần tải về:
1. Truy cập: https://dev.mysql.com/downloads/connector/j/
2. Tải phiên bản 9.0.0
3. Giải nén và copy file `mysql-connector-j-9.0.0.jar` vào thư mục `lib/`

### 5. Build Project
Nếu chưa build, sử dụng NetBeans hoặc Ant:

**Với Ant:**
```bash
ant compile
```

**Với NetBeans:**
1. Mở project trong NetBeans
2. Nhấn F11 hoặc chọn Build Project

## 🚀 Chạy Game

### Bước 1: Khởi động Server
Mở terminal và chạy:
```bash
chmod +x server.sh
./server.sh
```

Server sẽ khởi động và lắng nghe trên **port 12345**.

Bạn sẽ thấy thông báo:
```
Server đã khởi động trên cổng 12345
```

### Bước 2: Khởi động Client(s)
Mở terminal mới (hoặc nhiều terminal cho nhiều client) và chạy:
```bash
chmod +x client.sh
./client.sh
```

Client sẽ kết nối tới server tại `localhost:12345` và hiển thị giao diện đăng nhập.

### Chạy từ các máy khác nhau
Để chạy client từ máy khác kết nối tới server:
1. Trên máy server, lấy địa chỉ IP:
   ```bash
   ifconfig | grep "inet "
   # hoặc
   ipconfig getifaddr en0
   ```
2. Chỉnh sửa trong `src/client/ClientApp.java`:
   ```java
   client.startConnection("localhost", 12345);
   ```
   Thay `"localhost"` bằng địa chỉ IP của server.
3. Build lại project và chạy client.

## 📂 Cấu trúc Project

```
PenaltyShootoutClient/
├── src/
│   ├── client/          # Client-side code
│   │   ├── ClientApp.java       # Main client application
│   │   ├── Client.java          # Client network logic
│   │   └── GUI/                 # UI Controllers
│   ├── server/          # Server-side code
│   │   ├── Server.java          # Main server
│   │   ├── ClientHandler.java   # Handle client connections
│   │   ├── DatabaseManager.java # Database operations
│   │   └── GameRoom.java        # Game room logic
│   ├── common/          # Shared models
│   │   ├── Message.java
│   │   ├── User.java
│   │   ├── Match.java
│   │   └── MatchDetails.java
│   ├── resources/       # FXML files & CSS
│   └── assets/          # Images & media files
├── build/
│   └── classes/         # Compiled .class files
├── lib/
│   └── javafx/          # JavaFX SDK
├── server.sh           # Script chạy server
├── client.sh           # Script chạy client
└── README.md           # File này
```

## 🎮 Hướng dẫn chơi

1. **Đăng nhập/Đăng ký**: Sử dụng tài khoản hoặc tạo tài khoản mới
2. **Chờ đối thủ**: Server sẽ ghép cặp bạn với người chơi khác
3. **Bắt đầu game**: Chọn hướng sút/cản phá
4. **Thi đấu**: Luân phiên đá phạt đền
5. **Kết thúc**: Người có điểm cao hơn thắng

## 🐛 Xử lý lỗi thường gặp

### Lỗi: "Không thể kết nối tới server"
- Kiểm tra server đã khởi động chưa
- Kiểm tra firewall có chặn port 12345 không
- Kiểm tra địa chỉ IP nếu chạy trên nhiều máy

### Lỗi: "Không tìm thấy mysql-connector-j-9.0.0.jar"
- Tải MySQL Connector/J về và đặt trong `lib/`
- Hoặc copy từ `../PenaltyShootoutClient_OLD/`

### Lỗi: "Access denied for user"
- Kiểm tra thông tin đăng nhập MySQL trong `DatabaseManager.java`
- Đảm bảo user có quyền truy cập database

### Lỗi: "JavaFX runtime components are missing"
- Kiểm tra JavaFX SDK trong `lib/javafx/javafx-sdk-25`
- Đảm bảo script sử dụng đúng đường dẫn

### Lỗi build: "Chưa build project"
```bash
# Nếu dùng Ant
ant compile

# Nếu dùng NetBeans
# Mở project và nhấn F11
```

## 📝 Ghi chú

- Server phải được khởi động trước client
- Mỗi game cần ít nhất 2 players
- Database phải được thiết lập trước khi chạy server
- Port mặc định: 12345 (có thể thay đổi trong source code)

## 🛠️ Phát triển

### Build lại project:
```bash
ant clean compile
```

### Chạy tests (nếu có):
```bash
ant test
```

### Tạo distribution JAR:
```bash
ant jar
```

## 📞 Liên hệ & Hỗ trợ

Nếu gặp vấn đề, vui lòng:
1. Kiểm tra log output từ server và client
2. Kiểm tra MySQL logs
3. Kiểm tra Java version và JavaFX setup

---

**Chúc bạn chơi game vui vẻ! ⚽🎉**

