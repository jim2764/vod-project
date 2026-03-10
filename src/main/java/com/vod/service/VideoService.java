package com.vod.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import com.vod.dto.PlayResponseDto;
import com.vod.dto.TranscodePlanDto;
import com.vod.dto.UploadRequestDto;
import com.vod.dto.UploadResponseDto;
import com.vod.dto.VideoDto;
import com.vod.dto.VideoListDto;
import com.vod.dto.VideoProcessingDto;
import com.vod.dto.VideoStatusDto;
import com.vod.entity.Video;
import com.vod.enums.VideoStatus;
import com.vod.exception.VideoBusinessException;
import com.vod.exception.VideoSystemException;
import com.vod.repository.VideoRepository;

@Service
public class VideoService {
    private final Logger log = LoggerFactory.getLogger(VideoService.class);

    private final String endpoint; 
    private final String rawBucket;
    private final Long uploadTimeLimit;
    private final String allowedExtension;
    private final String allowedMimeType;
    private final Long maxVideoSize;
    private final String secureLinkSecret;

    private final MinioService minioService;
    private final VideoProcessingService videoProcessingService;
    private final VideoRepository videoRepository;

    public VideoService(
        @Value("${minio.endpoint}") String endpoint,
        @Value("${minio.buckets.raw}") String rawBucket,
        @Value("${minio.upload-time-limit}") Long uploadTimeLimit,
        @Value("${video.allowed-extension}") String allowedExtension,
        @Value("${video.allowed-mime-type}") String allowedMimeType,
        @Value("${video.max-video-size}") Long maxVideoSize,
        @Value("${nginx.secure-link-secret}") String secureLinkSecret,
        MinioService minioService, 
        VideoProcessingService videoProcessingService, 
        VideoRepository videoRepository
    ) {
        this.endpoint = endpoint;
        this.rawBucket = rawBucket;
        this.uploadTimeLimit = uploadTimeLimit;
        this.allowedExtension = allowedExtension;
        this.allowedMimeType = allowedMimeType;
        this.maxVideoSize = maxVideoSize;
        this.secureLinkSecret = secureLinkSecret;
        this.minioService = minioService;
        this.videoProcessingService = videoProcessingService;
        this.videoRepository = videoRepository;
    }

    public List<VideoStatusDto> getBatchStatuses(List<String> ids) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(uploadTimeLimit);

        return videoRepository.findAllVideosByIds(ids, threshold)
                .stream()
                .map(video -> VideoStatusDto.from(video))
                .toList();
    }

    public List<VideoListDto> getAllVideo() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(uploadTimeLimit);

        return videoRepository.findAllVideos(threshold)
                .stream()
                .map(video -> VideoListDto.from(video))
                .toList();
                
    }

    public UploadResponseDto createUploadPolicy(UploadRequestDto requestDto) {
        // video validation
        videoPreValidation(requestDto);

        // generate presigned policy
        String videoId = UUID.randomUUID().toString();
        String videoKey = videoId + requestDto.extension();
        var formData = minioService.generatePresignedPostFormData(videoKey, requestDto.mimeType());
        
        // create video data to DB
        Video video = Video.create(videoId, requestDto.videoName());
        videoRepository.save(video);
        
        String uploadUrl = "%s/upload/%s".formatted(endpoint, rawBucket);
        return UploadResponseDto.of(videoId, videoKey, uploadUrl, requestDto, formData);
    }

    public void processVideo(VideoDto videoDto) {
        // Update video status from UPLOADING to PROCESSING
        videoRepository.updateStatus(videoDto.id(), VideoStatus.UPLOADING, VideoStatus.PROCESSING);
    }

    @Async("videoExecutor")
    private void processVideoPipeline(VideoDto videoDto) {
        try {
            // Get Video Download Url
            String downloadUrl = minioService.generatePresignedDownloadUrl(videoDto.key());
    
            // Probe video
            var videoProcessingDto = VideoProcessingDto.of(videoDto.id(), videoDto.key(), downloadUrl);
            TranscodePlanDto transcodePlanDto = videoProcessingService.probeVideo(videoProcessingDto);
    
            // Transcode and upload video
            videoProcessingService.transcodeVideo(videoProcessingDto, transcodePlanDto, (tempWorkDir) -> {
                minioService.uploadHlsSegments(tempWorkDir, videoDto);
            });
    
            // Update video status from PROCESSING to REDY
            videoRepository.updateStatus(videoDto.id(), VideoStatus.PROCESSING, VideoStatus.READY);

        } catch(VideoBusinessException e) {
            log.warn(e.getMessage());
            recoverVideo(videoDto, e.getMessage());
        } catch(VideoSystemException e) {
            log.error(e.getMessage(), e);
            recoverVideo(videoDto, "系統發生錯誤, 請稍後再試");
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            recoverVideo(videoDto, "系統發生錯誤, 請稍後再試");
        }
    }

    public PlayResponseDto generatePlayUrl(String videoId) {
        long expires = Instant.now().plus(Duration.ofHours(3)).getEpochSecond();
        String realPathDir = "/hls-video/%s/".formatted(videoId);
        String stringToSign = "%d!%s!%s".formatted(expires, realPathDir, secureLinkSecret);

        byte[] md5Bytes = DigestUtils.md5Digest(stringToSign.getBytes(StandardCharsets.UTF_8));
        String md5 = Base64.getUrlEncoder().withoutPadding().encodeToString(md5Bytes);

        // Front get: {host}/hls/{md5}/{expires}/hls-video/rabbit1080/index.m3u8
        String playUrl = "%s/hls/%s/%d%smaster.m3u8".formatted(
            endpoint,
            md5,
            expires,
            realPathDir
        );

        return PlayResponseDto.of(playUrl);
    }

    private void videoPreValidation(UploadRequestDto requestDto) {
        if (!allowedExtension.equalsIgnoreCase(requestDto.extension())) throw new VideoBusinessException("[Pre-Validation] 不支援的格式: %s (Video: %s)".formatted(requestDto.extension(), requestDto.videoName()));
        if (!allowedMimeType.equalsIgnoreCase(requestDto.mimeType())) throw new VideoBusinessException("[Pre-Validation] 不支援的 MIME 類型: %s (Video: %s)".formatted(requestDto.mimeType(), requestDto.videoName()));
        if (requestDto.size() > maxVideoSize) throw new VideoBusinessException("[Pre-Validation] 影片檔案過大，不得超過 2GB (Video: %s)".formatted(requestDto.videoName()));
    }

    private void recoverVideo(VideoDto videoDto, String errorMessage) {
        try {
            videoRepository.markAsFailed(videoDto.id(), errorMessage);
        } catch(Exception e) {
            log.error("[Recover] DB recover異常 (Video: {})", videoDto.key(), e);
        }

        try {
            minioService.removeVideo(videoDto.key());
        } catch(Exception e) {
            log.error("[Recover] Minio recover異常 (Video: {})", videoDto.key(), e);
        }
    }
}