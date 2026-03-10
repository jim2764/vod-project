package com.vod.controller;

import com.vod.dto.ApiResponse;
import com.vod.dto.PlayResponseDto;
import com.vod.dto.ProcessRequestDto;
import com.vod.dto.UploadRequestDto;
import com.vod.dto.UploadResponseDto;
import com.vod.dto.VideoDto;
import com.vod.dto.VideoListDto;
import com.vod.dto.VideoStatusDto;
import com.vod.service.VideoService;

import java.util.List;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/batch-status")
    public ApiResponse<List<VideoStatusDto>> getBatchStatus(@RequestParam List<String> ids) {
        var videoStatuses =  videoService.getBatchStatuses(ids);

        return ApiResponse.ok(videoStatuses);
    }

    @GetMapping("/all-video")
    public ApiResponse<List<VideoListDto>> getAllVideo() {
        var videos = videoService.getAllVideo();

        return ApiResponse.ok(videos);
    }

    // Create presigned post policy
    @PostMapping("/presigned-policy")
    public ApiResponse<UploadResponseDto> generatePresignedPolicy(@RequestBody UploadRequestDto requestDto) {
        var response = videoService.createUploadPolicy(requestDto);

        return ApiResponse.ok(response);
    }

    // minio notify java
    @PostMapping("/webhook")
    public ApiResponse<Void> processVideoPipeline(@RequestBody ProcessRequestDto processRequestDto) {
        var video = VideoDto.from(processRequestDto);

        videoService.processVideo(video);

        return ApiResponse.ok();
    }

    // Get play url
    @GetMapping("/play/{videoId}")
    public ApiResponse<PlayResponseDto> generatePlayUrl(@PathVariable String videoId) {
        var response = videoService.generatePlayUrl(videoId);

        return ApiResponse.ok(response);
    }
}