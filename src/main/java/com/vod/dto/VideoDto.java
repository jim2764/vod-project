package com.vod.dto;

import java.nio.file.Path;

public record VideoDto(
    String id,
    String key
) {
    public static VideoDto from(ProcessRequestDto processRequestDto) {

        String fullKey = processRequestDto.key();

        String videoKey = Path.of(fullKey).getFileName().toString();

        int dotIndex = videoKey.lastIndexOf('.');
        String videoId = (dotIndex == -1) ? videoKey : videoKey.substring(0, dotIndex);

        return new VideoDto(videoId, videoKey);
    }
}
