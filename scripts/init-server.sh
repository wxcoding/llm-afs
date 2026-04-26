#!/bin/bash

set -e

#=============================================
# AFS 项目初始化部署脚本
# 用于在空白 Linux 服务器上首次部署
#=============================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

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

log_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

# 检查 root 权限
check_root() {
    if [[ $EUID -ne 0 ]]; then
        log_error "请使用 root 权限运行此脚本"
        exit 1
    fi
}

# 安装 Docker
install_docker() {
    log_step "安装 Docker..."

    # 安装依赖
    yum install -y yum-utils

    # 添加 Docker 仓库
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo

    # 安装 Docker
    yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

    # 启动 Docker
    systemctl start docker
    systemctl enable docker

    log_info "Docker 安装完成"
}

# 安装 Git
install_git() {
    log_step "安装 Git..."
    yum install -y git
    log_info "Git 安装完成"
}

# 配置防火墙
config_firewall() {
    log_step "配置防火墙..."

    # 检查 firewalld 是否存在
    if systemctl is-active --quiet firewalld; then
        firewall-cmd --permanent --add-port=3000/tcp
        firewall-cmd --permanent --add-port=8080/tcp
        firewall-cmd --permanent --add-port=5432/tcp
        firewall-cmd --reload
        log_info "防火墙端口开放完成"
    else
        log_warn "firewalld 未运行，跳过防火墙配置"
    fi
}

# 创建项目目录
create_directory() {
    log_step "创建项目目录..."
    mkdir -p /opt/llm-afs
    log_info "项目目录创建完成: /opt/llm-afs"
}

# 克隆项目
clone_project() {
    log_step "克隆项目..."

    read -p "请输入 Git 仓库 URL（直接回车跳过）: " repo_url

    if [ -z "$repo_url" ]; then
        log_warn "跳过 Git 克隆，请手动上传项目到 /opt/llm-afs"
    else
        cd /opt
        git clone "$repo_url" llm-afs
        log_info "项目克隆完成"
    fi
}

# 配置环境变量
setup_env() {
    log_step "配置环境变量..."

    cat > /opt/llm-afs/.env << 'EOF'
# MySQL/PostgreSQL 配置
MYSQL_ROOT_PASSWORD=ReplaceWithStrongPassword

# 大模型配置
AI_DASHSCOPE_API_KEY=ReplaceWithYourDashScopeKey
AI_DASHSCOPE_MODEL=qwen-turbo

# Java 环境
JAVA_OPTS=-Xmx512m -Xms256m
EOF

    log_info "环境变量配置文件已创建: /opt/llm-afs/.env"
    log_warn "请编辑 .env 文件，设置正确的密码和 API Key"
}

# 启动服务
start_services() {
    log_step "启动 Docker 服务..."

    systemctl start docker
    systemctl enable docker

    cd /opt/llm-afs

    log_info "构建并启动容器（首次构建需要较长时间）..."
    docker compose up -d --build

    log_info "容器启动完成"
}

# 验证部署
verify_deployment() {
    log_step "验证部署..."

    sleep 5

    echo ""
    echo "========================================"
    echo "           容器状态"
    echo "========================================"
    docker compose ps

    echo ""
    echo "========================================"
    echo "           端口检查"
    echo "========================================"
    ss -tlnp | grep -E '3000|8080|5432'

    echo ""
    echo "========================================"
    echo "           部署完成"
    echo "========================================"
    echo "前端地址: http://<服务器IP>:3000"
    echo "后端地址: http://<服务器IP>:8080"
    echo ""
    echo "查看日志: docker compose logs -f"
    echo "停止服务: docker compose down"
}

# 主流程
main() {
    echo "========================================"
    echo "   AFS 项目 - 服务器初始化部署脚本"
    echo "========================================"
    echo ""

    check_root

    log_info "开始初始化部署..."
    echo ""

    # 检查 Docker
    if ! command -v docker &> /dev/null; then
        install_docker
    else
        log_info "Docker 已安装"
    fi

    # 检查 Git
    if ! command -v git &> /dev/null; then
        install_git
    else
        log_info "Git 已安装"
    fi

    config_firewall
    create_directory

    # 询问是否克隆
    read -p "是否需要克隆 Git 仓库？(y/n): " clone_repo
    if [[ "$clone_repo" =~ ^[Yy]$ ]]; then
        clone_project
    fi

    # 询问是否配置环境变量
    read -p "是否创建 .env 配置文件？(y/n): " create_env
    if [[ "$create_env" =~ ^[Yy]$ ]]; then
        setup_env
    fi

    # 询问是否启动服务
    read -p "是否立即启动服务？(y/n): " start_now
    if [[ "$start_now" =~ ^[Yy]$ ]]; then
        start_services
        verify_deployment
    else
        log_info "部署准备完成"
        log_info "请手动执行以下命令启动服务:"
        echo "  cd /opt/llm-afs"
        echo "  docker compose up -d --build"
    fi
}

main "$@"
