package com.vod.dto;

import com.vod.entity.Video;
import com.vod.enums.VideoStatus;

public record VideoStatusDto(
    String id,
    VideoStatus status,
    String errorMsg
) {
    public static VideoStatusDto from(Video video){
        return new VideoStatusDto(
            video.getId(),
            video.getstatus(),
            video.getErrorMsg()
        );
    }
}
