package com.vod.dto;

public record TranscodePlanDto(
    Boolean hasAudio,
    Boolean isPortrait,
    Integer qualityAmount
) {
    public static TranscodePlanDto from(ProbeResultDto resultDto) {
        // 是否有聲音
        boolean hasAudio = resultDto.streams().stream()
            .anyMatch(s -> "audio".equals(s.codecType()));

        // 是否是直式
        var videoStream = resultDto.streams().stream()
            .filter(s -> "video".equals(s.codecType()))
            .findFirst().get();
        int width = videoStream.width();
        int height = videoStream.height();
        boolean isPortrait = height > width;

        // 切割的畫質數量
        int shortEdge = isPortrait? width: height;
        int qualityAmount = (shortEdge >= 1080) ? 3 : (shortEdge >= 720) ? 2 : 1;

        return new TranscodePlanDto(
            hasAudio,
            isPortrait,
            qualityAmount
        );
    }
}
