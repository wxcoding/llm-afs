#!/bin/bash

#=============================================
# AFS 项目健康检查脚本
#=============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置
SERVER_IP=${1:-"localhost"}
SERVER_PORT=${2:-"3000"}
API_PORT=${3:-"8080"}

check_service() {
    local name=$1
    local url=$2
    local expected_code=${3:-"200"}

    response=$(curl -s -o /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")

    if [ "$response" = "$expected_code" ]; then
        echo -e "${GREEN}[OK]${NC} $name is healthy (HTTP $response)"
        return 0
    else
        echo -e "${RED}[FAIL]${NC} $name is unhealthy (HTTP $response)"
        return 1
    fi
}

echo "========================================"
echo "         AFS 健康检查"
echo "========================================"
echo ""
echo "检查前端: http://$SERVER_IP:$SERVER_PORT"
echo "检查后端: http://$SERVER_IP:$API_PORT"
echo ""

failed=0

# 检查前端
check_service "前端 (Nginx)" "http://$SERVER_IP:$SERVER_PORT" || ((failed++))

# 检查后端 API
check_service "后端 API" "http://$SERVER_IP:$API_PORT/api/stats" || ((failed++))

# 检查数据库
echo ""
echo -e "${YELLOW}[INFO]${NC} Docker 容器状态:"
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"

echo ""
if [ $failed -eq 0 ]; then
    echo -e "${GREEN}所有检查通过！${NC}"
    exit 0
else
    echo -e "${RED}$failed 项检查失败${NC}"
    exit 1
fi
