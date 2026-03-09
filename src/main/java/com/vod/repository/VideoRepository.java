package com.vod.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vod.entity.Video;
import com.vod.enums.VideoStatus;

import jakarta.transaction.Transactional;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {

    @Transactional
    @Query(value = """
        (
          SELECT * FROM videos 
          WHERE id In (:ids) 
            AND status IN (1, 2, 3)
        )
        UNION ALL
        (
          SELECT * FROM videos 
          WHERE id In (:ids) 
            AND status = 0
            AND created_at >= :thresholdTime
        )
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<Video> findAllVideosByIds(
        @Param("ids") List<String> ids,
        @Param("thresholdTime") LocalDateTime thresholdTime
    );

    // 0 UPLOADING
    // 1 PROCESSING
    // 2 READY
    // 3 FAILED
    @Transactional
    @Query(value = """
        (
          SELECT * FROM videos 
          WHERE status IN (1, 2, 3)
        )
        UNION ALL
        (
          SELECT * FROM videos 
          WHERE status = 0
            AND created_at >= :thresholdTime
        )
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<Video> findAllVideos(@Param("thresholdTime") LocalDateTime thresholdTime);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Video v WHERE v.status = com.vod.enums.VideoStatus.UPLOADING AND v.createdAt < :threshold")
    int deleteExpiredUploads(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.status = :newStatus WHERE v.id = :id AND v.status = :oldStatus")
    int updateStatus(
        @Param("id") String id, 
        @Param("oldStatus") VideoStatus oldStatus, 
        @Param("newStatus") VideoStatus newStatus
    );

    @Modifying
    @Transactional
    @Query("UPDATE Video v SET v.status = com.vod.enums.VideoStatus.FAILED, v.errorMsg = :errorMsg WHERE v.id = :id AND v.status = com.vod.enums.VideoStatus.PROCESSING")
    void markAsFailed(
        @Param("id") String id, 
        @Param("errorMsg") String errorMsg
    );
    
}
