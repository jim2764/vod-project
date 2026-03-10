package com.vod.dto;

import java.util.Map;

public record PresignedPostFormDataDto(
    String policy,
    String xAmzAlgorithm,
    String xAmzCredential,
    String xAmzDate,
    String xAmzSignature
) {
    public static PresignedPostFormDataDto fromMap(Map<String, String> formData) {
        return new PresignedPostFormDataDto(
            formData.get("policy"),
            formData.get("x-amz-algorithm"),
            formData.get("x-amz-credential"),
            formData.get("x-amz-date"),
            formData.get("x-amz-signature")
        );
    }
}
