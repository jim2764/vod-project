package com.vod.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.vod.dto.PresignedPostFormDataDto;
import com.vod.dto.VideoDto;
import com.vod.exception.MinioOperationException;
import com.vod.exception.VideoSystemException;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.RemoveObjectArgs;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;

@Service
public class MinioService {
    private final String rawBucket;
    private final String hlsBucket;
    private final Integer uploadTimeLimit;
    private final Integer downloadTimeLimit;
    private final Long maxVideoSize;
    private final Long minVideoSize;

    private final MinioClient minioClient;

    public MinioService(
        @Value("${minio.buckets.raw}") String rawBucket,
        @Value("${minio.buckets.hls}") String hlsBucket,
        @Value("${minio.upload-time-limit}") Integer uploadTimeLimit,
        @Value("${minio.download-time-limit}") Integer downloadTimeLimit,
        @Value("${video.max-video-size}") Long maxVideoSize,
        @Value("${video.min-video-size}") Long minVideoSize,
        MinioClient minioClient
    ) {
        this.rawBucket = rawBucket;
        this.hlsBucket = hlsBucket;
        this.uploadTimeLimit = uploadTimeLimit;
        this.downloadTimeLimit = downloadTimeLimit;
        this.maxVideoSize = maxVideoSize;
        this.minVideoSize = minVideoSize;
        this.minioClient = minioClient;
    }

    // Generate upload presigned post form data
    public PresignedPostFormDataDto generatePresignedPostFormData(String videoKey, String mimeType) {
        try {
            PostPolicy policy = new PostPolicy(rawBucket, ZonedDateTime.now().plusMinutes(uploadTimeLimit));
            policy.addEqualsCondition("key", videoKey);
            policy.addContentLengthRangeCondition(minVideoSize, maxVideoSize);
            policy.addEqualsCondition("Content-Type", mimeType);
    
            var formData = minioClient.getPresignedPostFormData(policy);
    
            return PresignedPostFormDataDto.fromMap(formData);
        } catch(Exception e) {
            throw new MinioOperationException("[Minio] 建立 Presigned post formData 錯誤 (Video: %s)".formatted(videoKey), e);
        }
    }

    // Generate download presigned URL
    public String generatePresignedDownloadUrl(String videoKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(rawBucket)
                    .object(videoKey)
                    .expiry(downloadTimeLimit, TimeUnit.HOURS)
                    .build()
            );
        } catch(Exception e) {
            throw new MinioOperationException("[Minio] 建立 Presigned download url 錯誤 (Video: %s)".formatted(videoKey), e);
        }
    }

    // Upload Segments to Minio
    public void uploadHlsSegments(Path tempWorkDir, VideoDto videoDto) {

        try (Stream<Path> walk = Files.walk(tempWorkDir)) {
            walk.filter(Files::isRegularFile).forEach(filePath -> {
                try {
                    String fileName = filePath.getFileName().toString();
                    String contentType = getmineContentType(fileName);
                    Path relativePath = tempWorkDir.relativize(filePath);
                    String videoKey = String.join("/", videoDto.id(), relativePath.toString().replace(File.separator, "/"));
    
                    minioClient.uploadObject(
                        UploadObjectArgs.builder()
                            .bucket(hlsBucket)
                            .object(videoKey)
                            .filename(filePath.toAbsolutePath().toString())
                            .contentType(contentType)
                            .build()
                    );
                } catch(Exception e) {
                    throw new MinioOperationException("[Minio] 上傳 Segments 錯誤 (Video: %s)".formatted(videoDto.key()), e);
                }
            });
        } catch(IOException e) {
            throw new VideoSystemException(e.getMessage(), e);
        }
    }

    public void removeVideo(String videoKey) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(rawBucket)
                    .object(videoKey)  
                    .build()
            );
        } catch(Exception e) {
            throw new MinioOperationException("[Minio] 刪除影片錯誤 (Video: %s)".formatted(videoKey), e);
        }
    }

    private String getmineContentType(String fileName) {
        if (fileName.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (fileName.endsWith(".ts")) return "video/MP2T";
        return "application/octet-stream";
    }
}
