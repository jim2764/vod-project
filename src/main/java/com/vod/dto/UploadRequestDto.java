package com.vod.dto;

public record UploadRequestDto(
    String videoName,
    String mimeType,
    String extension, // 例如: .mp4
    Long size
) {}