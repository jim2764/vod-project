package com.vod.dto;

public record UploadResponseDto(
    String videoId,
    String videoName,
    String videoKey,
    String uploadUrl,
    String contentType,
    String policy,
    String xAmzAlgorithm,
    String xAmzCredential,
    String xAmzDate,
    String xAmzSignature
) {
    public static UploadResponseDto of(String videoId, String videoKey, String uploadUrl, UploadRequestDto requestDto, PresignedPostFormDataDto formData) {
        return new UploadResponseDto(
            videoId,
            requestDto.videoName(),
            videoKey,
            uploadUrl, 
            requestDto.mimeType(),
            formData.policy(),
            formData.xAmzAlgorithm(),
            formData.xAmzCredential(),
            formData.xAmzDate(),
            formData.xAmzSignature()
        );
    }
}