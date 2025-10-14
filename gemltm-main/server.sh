#!/bin/bash

# Script chạy Server cho PenaltyShootout Game
# Tác giả: Auto-generated
# Mô tả: Khởi động server game trên port 12345

# Thiết lập màu sắc cho output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   Penalty Shootout - Server Startup   ${NC}"
echo -e "${GREEN}========================================${NC}\n"

# Lấy thư mục hiện tại
CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Thiết lập đường dẫn JavaFX
JAVAFX_HOME="$CURRENT_DIR/lib/javafx/javafx-sdk-25"
JAVAFX_LIB="$JAVAFX_HOME/lib"

# Thiết lập đường dẫn MySQL Connector
MYSQL_CONNECTOR="$CURRENT_DIR/lib/mysql-connector-j-9.0.0.jar"
if [ ! -f "$MYSQL_CONNECTOR" ]; then
    echo -e "${RED}❌ Lỗi: Không tìm thấy mysql-connector-j-9.0.0.jar tại $MYSQL_CONNECTOR${NC}"
    echo -e "${YELLOW}Vui lòng tải về từ: https://dev.mysql.com/downloads/connector/j/${NC}"
    echo -e "${YELLOW}Và đặt vào thư mục lib/${NC}"
    exit 1
fi

# Kiểm tra JavaFX
if [ ! -d "$JAVAFX_LIB" ]; then
    echo -e "${RED}❌ Lỗi: Không tìm thấy JavaFX SDK tại $JAVAFX_LIB${NC}"
    exit 1
fi

# Kiểm tra build classes
if [ ! -d "$CURRENT_DIR/build/classes" ]; then
    echo -e "${RED}❌ Lỗi: Chưa build project. Vui lòng build bằng NetBeans hoặc Ant${NC}"
    echo -e "${YELLOW}Chạy lệnh: ant compile${NC}"
    exit 1
fi

# Thiết lập classpath
CLASSPATH="$CURRENT_DIR/build/classes"
CLASSPATH="$CLASSPATH:$MYSQL_CONNECTOR"
CLASSPATH="$CLASSPATH:$JAVAFX_LIB/javafx.base.jar"
CLASSPATH="$CLASSPATH:$JAVAFX_LIB/javafx.controls.jar"
CLASSPATH="$CLASSPATH:$JAVAFX_LIB/javafx.fxml.jar"
CLASSPATH="$CLASSPATH:$JAVAFX_LIB/javafx.graphics.jar"

echo -e "${GREEN}✓${NC} JavaFX SDK: $JAVAFX_HOME"
echo -e "${GREEN}✓${NC} MySQL Connector: $MYSQL_CONNECTOR"
echo -e "${GREEN}✓${NC} Build Classes: $CURRENT_DIR/build/classes\n"

echo -e "${YELLOW}🚀 Đang khởi động Server...${NC}"
echo -e "${YELLOW}📡 Server sẽ lắng nghe trên port: 12345${NC}\n"

# Chạy server
java -cp "$CLASSPATH" \
     --module-path "$JAVAFX_LIB" \
     --add-modules javafx.controls,javafx.fxml \
     server.Server

# Kiểm tra exit code
if [ $? -ne 0 ]; then
    echo -e "\n${RED}❌ Server đã dừng với lỗi${NC}"
    exit 1
else
    echo -e "\n${GREEN}✓ Server đã dừng bình thường${NC}"
fi

