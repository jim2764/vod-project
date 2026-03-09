package com.vod.service;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;

import com.vod.repository.VideoRepository;

public class VideoCleanupService {
    private final VideoRepository videoRepository;

    public VideoCleanupService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    // 每天凌晨 3:00 執行
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupTask() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        int deletedRows = videoRepository.deleteExpiredUploads(threshold);
        
        System.out.println("定時清理完成，共刪除 " + deletedRows + " 筆逾期影片");
    }
}
