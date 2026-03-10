package com.vod.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ProcessRequestDto(
    @JsonProperty("EventName")
    String eventName,
    @JsonProperty("Key")
    String key
) {}
