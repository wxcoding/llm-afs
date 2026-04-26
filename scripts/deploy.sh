#!/bin/bash

set -e

#=============================================
# AFS 项目部署脚本
# 用于在 Linux 服务器上自动部署
#=============================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 项目目录
PROJECT_DIR="/opt/llm-afs"

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查 root 权限
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "请使用 root 权限运行此脚本"
        exit 1
    fi
}

# 检查 Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! command -v docker compose &> /dev/null; then
        log_error "Docker Compose 未安装，请先安装 Docker Compose"
        exit 1
    fi

    log_info "Docker 环境检查通过"
}

# 拉取最新代码
pull_code() {
    log_info "拉取最新代码..."

    if [ -d "$PROJECT_DIR/.git" ]; then
        cd "$PROJECT_DIR"
        git pull origin main
    else
        log_error "Git 仓库不存在，请先克隆项目"
        exit 1
    fi
}

# 构建并启动
deploy() {
    log_info "开始构建和启动容器..."

    cd "$PROJECT_DIR"

    # 拉取最新镜像（如果使用远程镜像）
    # docker compose pull

    # 构建并启动
    docker compose up -d --build

    log_info "容器启动完成"
}

# 查看状态
status() {
    log_info "容器状态："
    docker compose ps
}

# 查看日志
logs() {
    service=${1:-""}
    if [ -z "$service" ]; then
        docker compose logs -f
    else
        docker compose logs -f "$service"
    fi
}

# 停止服务
stop() {
    log_info "停止服务..."
    docker compose down
}

# 重启服务
restart() {
    log_info "重启服务..."
    docker compose restart
}

# 清理
clean() {
    log_warn "清理未使用的 Docker 资源..."
    docker image prune -f
    docker container prune -f
    docker volume prune -f
}

# 完整重新部署
redeploy() {
    log_info "开始完整重新部署..."
    stop
    pull_code
    deploy
    clean
}

# 显示帮助
show_help() {
    echo "AFS 项目部署脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  deploy    部署/更新服务"
    echo "  redeploy  完整重新部署（停止 -> 拉取 -> 构建 -> 启动）"
    echo "  start     启动服务"
    echo "  stop      停止服务"
    echo "  restart   重启服务"
    echo "  status    查看容器状态"
    echo "  logs      查看日志（可选：指定服务名）"
    echo "  clean     清理未使用的 Docker 资源"
    echo "  help      显示帮助"
    echo ""
    echo "示例:"
    echo "  $0 deploy          # 部署服务"
    echo "  $0 logs afs-server # 查看后端日志"
    echo "  $0 logs afs-web    # 查看前端日志"
}

# 主逻辑
case "${1:-deploy}" in
    deploy)
        deploy
        ;;
    redeploy)
        redeploy
        ;;
    start)
        docker compose up -d
        ;;
    stop)
        stop
        ;;
    restart)
        restart
        ;;
    status)
        status
        ;;
    logs)
        logs "$2"
        ;;
    clean)
        clean
        ;;
    help|--help|-h)
        show_help
        ;;
    *)
        log_error "未知命令: $1"
        show_help
        exit 1
        ;;
esac
