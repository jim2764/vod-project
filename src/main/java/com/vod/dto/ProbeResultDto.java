package com.vod.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProbeResultDto(
    List<Stream> streams,
    Format format
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Stream(
        int index,
        @JsonProperty("codec_type") String codecType,
        @JsonProperty("codec_name") String codecName,
        Integer width,
        Integer height
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Format(
        @JsonProperty("format_name") String formatName,
        String duration,
        @JsonProperty("probe_score") int probeScore,
        @JsonProperty("nb_streams") int nbStreams
    ) {}
}