package com.vod.dto;

public record VideoQuality(
    String name, 
    int width, 
    int height,
    String crf, 
    String maxrate, 
    String bufsize
) {
    public static VideoQuality of(String name, int width, int height, String crf, String maxrate, String bufsize) {
        return new VideoQuality(
            name,
            width,
            height,
            crf,
            maxrate,
            bufsize
        );
    }
}
