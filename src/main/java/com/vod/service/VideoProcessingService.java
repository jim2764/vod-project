package com.vod.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vod.dto.ProbeResultDto;
import com.vod.dto.TranscodePlanDto;
import com.vod.dto.VideoProcessingDto;
import com.vod.dto.VideoQuality;
import com.vod.exception.VideoBusinessException;
import com.vod.exception.VideoSystemException;

@Service
public class VideoProcessingService {
    private final Logger log = LoggerFactory.getLogger(VideoProcessingService.class);

    private final List<VideoQuality> videoQualities = List.of(
        VideoQuality.of("480p", 854, 480,  "28", "800k",  "1200k"),
        VideoQuality.of("720p", 1280, 720,  "24", "2000k", "3000k"),
        VideoQuality.of("1080p", 1920, 1080, "20", "4700k", "6000k")
    );

    private final ObjectMapper objectMapper;

    public VideoProcessingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Get video information
    public TranscodePlanDto probeVideo(VideoProcessingDto videoProcessingDto) {
        try {
            var cmd = List.of(
                "ffprobe", "-v", "error",
                "-show_entries", "stream=index,codec_name,codec_type,width,height:format=filename,nb_streams,format_name,probe_score,duration",
                "-of", "json",
                videoProcessingDto.url()
            );
            
            var probePb = new ProcessBuilder(cmd);
            probePb.redirectErrorStream(true); 
            Process probeProcess = probePb.start();
    
            String probeResult;
            try (InputStream inputStream = probeProcess.getInputStream()) {
                probeResult = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
    
            int exitCode = probeProcess.waitFor();
    
            System.out.println(probeResult);

            if (exitCode != 0) throw new VideoSystemException("[Probe] ffprobe error exit code: %d (Video: %s)".formatted(exitCode, videoProcessingDto.key()));
    
            var probeResultDto = objectMapper.readValue(probeResult, ProbeResultDto.class);

            // 這裏應該傳videoName進去
            return deepVideoValidation(videoProcessingDto.id(), probeResultDto);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoSystemException(e.getMessage(), e);
        } catch(IOException e) {
            throw new VideoSystemException(e.getMessage(), e);
        }
    }

    public void transcodeVideo(VideoProcessingDto videoProcessingDto, TranscodePlanDto transcodePlan, Consumer<Path> uploadAction) {
        Path tempWorkDir = null;
        try {
            tempWorkDir = Files.createTempDirectory("vod_transcode_");
            for (int i = 0; i < transcodePlan.qualityAmount(); i++) {
                String qualityName = videoQualities.get(i).name(); 
                Files.createDirectories(tempWorkDir.resolve(qualityName));
            }
            
            String absolutePath = tempWorkDir.toAbsolutePath().toString();
            String segmentPattern = absolutePath + File.separator + "%v" + File.separator + "seq_%03d.ts";
            String indexPattern = absolutePath + File.separator + "%v" + File.separator + "index.m3u8";
    
            // 組合ffmpeg指令
            var command = new ArrayList<String>(List.of(
                "ffmpeg", "-y", "-i", videoProcessingDto.url()
            ));
            List<String> mappings = new ArrayList<String>();
            List<String> encodings = new ArrayList<String>();
            List<String> streamMapGroups = new ArrayList<String>();
    
            for (int i = 0; i < transcodePlan.qualityAmount(); i++) {
                var quality = videoQualities.get(i);
    
                mappings.addAll(List.of("-map", "0:V:0"));
                if (transcodePlan.hasAudio()) mappings.addAll(List.of("-map", "0:a:0"));
    
                String scaleFilter = transcodePlan.isPortrait()
                    ? "scale=%d:-2".formatted(quality.height()) 
                    : "scale=-2:%d".formatted(quality.height());
    
                encodings.addAll(List.of(
                        "-c:v:" + i, "libx264", 
                        "-profile:v:" + i, "main", 
                        "-crf:v:" + i, quality.crf(), 
                        "-maxrate:v:" + i, quality.maxrate(), 
                        "-bufsize:v:" + i, quality.bufsize(), 
                        "-filter:v:" + i, scaleFilter, 
                        "-preset:v:" + i, "veryfast"
                    ));
    
                if (transcodePlan.hasAudio()) encodings.addAll(List.of("-b:a:" + i, "128k"));
    
                streamMapGroups.add(
                    "v:" + i + 
                    (transcodePlan.hasAudio() ? ",a:" + i : "") + 
                    ",name:" + quality.name()
                );
            }
    
            if (transcodePlan.hasAudio()) encodings.addAll(List.of(
                "-c:a", "aac", 
                "-ar", "48000"
            ));
    
            command.addAll(mappings);
            command.addAll(encodings);
            command.addAll(List.of(
                "-force_key_frames", "expr:gte(t,n_forced*2)", 
                "-sc_threshold", "0",
                "-f", "hls", 
                "-hls_time", "6", 
                "-hls_playlist_type", "vod", 
                "-hls_list_size", "0",
                "-master_pl_name", "master.m3u8",
                "-hls_segment_filename", segmentPattern,
                "-var_stream_map", String.join(" ", streamMapGroups),
                indexPattern
            ));
    
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            
            Process process = pb.start();
            int exitCode = process.waitFor();
    
            if (exitCode != 0) throw new VideoSystemException("[Transcode] ffmpeg error exit code: %d (Video: %s)".formatted(exitCode, videoProcessingDto.key()));
            
            uploadAction.accept(tempWorkDir);

        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VideoSystemException(e.getMessage(), e);
        } catch(IOException e) {
            throw new VideoSystemException(e.getMessage(), e);
        } finally {
            if (tempWorkDir != null) 
                try {
                    FileSystemUtils.deleteRecursively(tempWorkDir);
                } catch(IOException e) {
                    log.error("[Recovey] 暫存檔刪除異常 (Video: %s)".formatted(videoProcessingDto.key()));
                }
        }
    }

    // Video deep validation
    private TranscodePlanDto deepVideoValidation(String videoId , ProbeResultDto resultDto){
        if (resultDto.format() == null || resultDto.streams() == null) throw new VideoBusinessException("[Probe] 影片格式有問題 (Video: %s)".formatted(videoId));

        if (!resultDto.streams().stream().anyMatch(s -> "video".equals(s.codecType()))) throw new VideoBusinessException("[Probe] 尚未有影片 Stream (Video: %s)".formatted(videoId));

        double duration = safeStringToDouble(resultDto.format().duration());
        if (duration < 1) throw new VideoBusinessException("[Probe] 影片的 Duration 有問題 (Video:%s)".formatted(videoId));

        String formatName = resultDto.format().formatName();
        if (formatName == null || !formatName.contains("mp4")) throw new VideoBusinessException("[Probe] 只支援 .mp4 (Video: %s)".formatted(videoId));

        if (resultDto.format().probeScore() < 50) throw new VideoBusinessException("[Probe] Probe Score分數過低, 影片可能損毀 (Video: %s)".formatted(videoId));

        return TranscodePlanDto.from(resultDto);
    }

    private double safeStringToDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}