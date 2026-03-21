# VOD-Stream：高效影片處理與串流平台 (Core Service)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green.svg)](https://spring.io/projects/spring-boot)
[![Vue 3](https://img.shields.io/badge/Vue-3.x-brightgreen.svg)](https://vuejs.org/)
[![Nginx](https://img.shields.io/badge/Nginx-Secure%20Link-blue.svg)](https://nginx.org/)
[![MinIO](https://img.shields.io/badge/MinIO-Object%20Storage-red.svg)](https://min.io/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Container-blue.svg)](https://www.docker.com/)
[![FFmpeg](https://img.shields.io/badge/FFmpeg-Multimedia-orange.svg)](https://ffmpeg.org/)

<img width="1090" height="497" alt="Image" src="https://github.com/user-attachments/assets/a48bf59e-7303-4742-8ba1-c1cd4d372e98" />
本專案是一個影片管理平台，實現了從影片非同步上傳、處理狀態追蹤到 HLS 自適應串流觀看的完整流程。







## 🚀 核心亮點

### 安全直傳 (Presigned Post Policy)
前端直接上傳影片至 MinIO 繞過後端伺服器，大幅減輕 Spring Boot 負擔。

### 自動化影片處理 Pipeline
整合 **FFprobe 驗證**與 **FFmpeg 背景轉碼**。流程全自動化執行且不阻塞系統，使用者可透過前端即時追蹤處理進度。

### HLS 自適應串流 (ABR)
支援畫質自動切換（1080p/720p/480p），針對不同網路環境自動優化，確保播放過程不卡頓。

### Nginx 高效分發與安全驗證
利用 **Nginx Secure Link** 模組進行 URL 權限驗證，並結合其優異的靜態資源處理能力，實現龐大影片的高速分發與保護。

### MinIO 物件儲存
採用 **Hash 索引** 實現秒級資源存取，專為海量影音設計的高擴展性儲存架構，確保數據儲存的效能與穩定。







## 🏗️ 系統架構

### 影片上傳與非同步處理
<img width="635" height="642" alt="Image" src="https://github.com/user-attachments/assets/cd24480f-46eb-46bd-b638-2f1620a67e36" />

* **安全授權**：Vue 向 Spring Boot 申請 Pre-signed Policy，確保上傳行為合法。
* **效能直傳**：Vue 跳過後端直接上傳影片至 MinIO，節省Spring Boot伺服器頻寬。
* **事件觸發**：MinIO 通知後端啟動 FFprobe 驗證與 FFmpeg 轉碼任務。
* **狀態追蹤**：Vue 透過 Polling 即時獲取影片狀態。

### 安全串流播放
<img width="681" height="615" alt="Image" src="https://github.com/user-attachments/assets/5f527c7b-365c-4b56-90c2-d50245103fd6" />

* **網址簽名**：Spring Boot 產生Secure Link。
* **安全校驗**：Nginx Secure Link 即時比對簽章，防止非法盜連。
* **串流播放**：驗證通過後由 Nginx 分發 HLS 檔案，支援自適應畫質播放。







## 🛠️ 技術棧

### 後端
  * **Spring Boot 3**
  * **MySQL**
  * **MinIO (Object Storage)**
  * **FFmpeg / FFprobe**
  * **Presigned Post Policy (安全直傳上傳)**
  * **Secure Link (安全觀看)**
### 前端
  * **Vue 3**
  * **HLS.js**
### 基礎設施
  * **Nginx (Secure Link 驗證 & 高效分發)**
  * **Docker**







## ⚡ 執行步驟

### 1. Clone Repositories
```bash
git clone https://github.com/jim2764/vod-front.git
git clone https://github.com/jim2764/vod-project.git
```
### 2. 啟動docker container
```bash
docker compose -f vod-project/.devcontainer/docker-compose.yml up -d
docker compose -f vod-front/.devcontainer/docker-compose.yml up -d
```
### 3. 執行程式
#### 執行Spring Boot
```bash
docker exec -t vod-app /bin/sh -c "mvn spring-boot:run"
```
#### 再開啟另一個終端機, 執行Vue
```bash
docker exec -t front-app /bin/sh -c "npm run dev --host 0.0.0.0"
```
### 4. 打開瀏覽器
```bash
http://localhost:5173/
```






## 🔗 相關專案連結

本系統採前後端分離架構

* **[後端(目前)](https://github.com/jim2764/vod-project)**
* **[前端(UI)](https://github.com/jim2764/vod-front)**






## 📬 聯絡資訊
* **姓名**：Jim Lin
* **Email**：jim2345678@gmail.com
* **GitHub**: [jim2764](https://github.com/jim2764)
