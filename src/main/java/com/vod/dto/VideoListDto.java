package com.vod.dto;

import com.vod.entity.Video;
import com.vod.enums.VideoStatus;

public record VideoListDto(
    String id,
    String videoName,
    VideoStatus status,
    String errorMsg
) {
    public static VideoListDto from(Video video){
        return new VideoListDto(
            video.getId(),
            video.getVideoName(),
            video.getstatus(),
            video.getErrorMsg()
        );
    }
}
