package com.vod.entity;

import com.vod.enums.VideoStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    private String id;

    @Column(name = "video_name", nullable = false)
    private String videoName;

    @Enumerated(EnumType.ORDINAL)
    @Column(nullable = false)
    private VideoStatus status = VideoStatus.UPLOADING;

    @Column(name = "error_msg")
    private String errorMsg;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Video create(String id, String videoName) {
        Video video = new Video();
        video.setId(id);
        video.setVideoName(videoName);
        return video;
    }

    // --- Getter & Setter ---
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVideoName() {
        return this.videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public VideoStatus getstatus() {
        return this.status;
    }

    public String getErrorMsg() {
        return this.errorMsg;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }
}
