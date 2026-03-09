package com.vod.dto;

public record VideoProcessingDto(
    String id,
    String key,
    String url
) {
    public static VideoProcessingDto of(String id, String key, String url) {
        return new VideoProcessingDto(id, key, url);
    }
}
