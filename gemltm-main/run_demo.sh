#!/bin/bash

# Script demo chạy cả server và client
# Mở server trong terminal hiện tại và client trong terminal mới

GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

CURRENT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}    Penalty Shootout - Demo Mode      ${NC}"
echo -e "${GREEN}========================================${NC}\n"

echo -e "${YELLOW}📋 Sẽ khởi động:${NC}"
echo -e "   1. Server trong terminal này"
echo -e "   2. Client trong terminal mới\n"

echo -e "${BLUE}💡 Thông tin đăng nhập test:${NC}"
echo -e "   • Username: ${GREEN}player1${NC}  Password: ${GREEN}pass123${NC}"
echo -e "   • Username: ${GREEN}player2${NC}  Password: ${GREEN}pass123${NC}"
echo -e "   • Username: ${GREEN}testuser${NC} Password: ${GREEN}test${NC}\n"

# Khởi động client trong terminal mới (macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "${YELLOW}🚀 Đang mở client trong terminal mới...${NC}\n"
    osascript -e "tell application \"Terminal\" to do script \"cd '$CURRENT_DIR' && ./client.sh\""
    sleep 2
fi

# Chạy server trong terminal hiện tại
echo -e "${YELLOW}🚀 Đang khởi động server...${NC}\n"
./server.sh

