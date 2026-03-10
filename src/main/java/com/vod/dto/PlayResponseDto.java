package com.vod.dto;

public record PlayResponseDto(String playUrl) {
    public static PlayResponseDto of(String playUrl) {
        return new PlayResponseDto(playUrl);
    }
}
