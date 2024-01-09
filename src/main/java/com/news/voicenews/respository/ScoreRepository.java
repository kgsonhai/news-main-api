package com.news.voicenews.respository;

import com.news.voicenews.model.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScoreRepository
        extends JpaRepository<Score, Long> {

    @Query(value = "SELECT * FROM scores"
            + " WHERE session_id = :sessionId"
            + " AND category = :category"
            + " AND score > 0"
            + " ORDER BY score DESC"
            ,nativeQuery = true)
    List<Score> findScoresBySessionIdAndCategoryWithLimit(@Param("sessionId") Long sessionId,
                                                          @Param("category") String category);

    Optional<Score> findById(Long id);

    List<Score> findAllByArticleId(String articleId);

    @Query(value = "SELECT * FROM scores"
            + " WHERE session_id = :sessionId"
            + " AND article_id = :articleId"
            + " AND audio_path IS NOT NULL"
            + " LIMIT 1", nativeQuery = true)
    Score findByArticleIdAndAudioPathNotNull(@Param("sessionId") Long sessionId, @Param("articleId") String articleId);

    @Query(value = "SELECT * FROM scores"
            + " WHERE article_id = :articleId"
            + " AND audio_path IS NULL"
            , nativeQuery = true)
    Score findByArticleIdAndAudioPathIsNull(@Param("articleId") String articleId);

    @Query(value = "SELECT * FROM scores"
            + " WHERE session_id = :sessionId"
            + " AND audio_path IS NULL"
            + " AND score > 0", nativeQuery = true)
    List<Score> findScoreWithPathNull(@Param("sessionId") Long sessionId);
}
