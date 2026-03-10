# 1. 使用官方 Java 21 鏡像 (已內建 Git 與 JDK 21)
FROM mcr.microsoft.com/devcontainers/java:21

# 2. 安裝 FFmpeg 與 Maven
# 使用 -y 自動確認，並在安裝後清理暫存檔以保持鏡像輕巧
# 2. 安裝工具並修正權限
RUN apt-get update && \
    apt-get install -y ffmpeg maven && \
    apt-get clean && \
    # 預先建立 Maven 資料夾，並確保 vscode 使用者擁有家目錄的所有權
    mkdir -p /home/vscode/.m2/repository && \
    chown -R vscode:vscode /home/vscode/ && \
    rm -rf /var/lib/apt/lists/*

# 3. 設定後端的工作目錄 (對應 devcontainer.json)
WORKDIR /workspaces/vod-project