package com.news.voicenews.service;

import com.news.voicenews.error.exception.ObjectNotFoundException;
import com.news.voicenews.model.Score;
import com.news.voicenews.model.Session;
import com.news.voicenews.respository.ScoreRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;

    public ScoreService(final ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @Transactional(readOnly = true)
    public List<Score> fetchScoresBySessionIdCategoryWithLimit(Long sessionId, String category) {
        log.info("Fetch scores by sessionId #{}, category #{}", sessionId, category);
        log.info("Data  #{}", scoreRepository.findScoresBySessionIdAndCategoryWithLimit(sessionId, category));

        return scoreRepository.findScoresBySessionIdAndCategoryWithLimit(sessionId, category);
    }

    @Transactional(readOnly = true)
    public Score findById(Long id) {
        log.info("Find score by id #{}", id);

        return scoreRepository.findById(id)
                              .orElseThrow(() -> new ObjectNotFoundException("score"));
    }

    @Transactional(readOnly = true)
    public List<Score> findByAllArticleId(final String articlesId) {
        log.info("Find all by article id #{}", articlesId);

        return scoreRepository.findAllByArticleId(articlesId);
    }

    @Transactional(readOnly = true)
    public Score findByArticleIdAndAudioPathNotNull(final String articleId, final Long sessionId) {
        log.info("Find by article id #{} and audio not null", articleId);

        return scoreRepository.findByArticleIdAndAudioPathNotNull(sessionId,articleId);
    }

    @Transactional(readOnly = true)
    public String findByArticleIdAndAudioPathIsNull(final String articleId) {
        log.info("Find by article id #{} and audio is null", articleId);

        return scoreRepository.findByArticleIdAndAudioPathIsNull(articleId).getAudioPath();
    }

    @Transactional
    public void updateAudioPathByOldScorePath(final Score score, final String oldScoreAudioPath) {
        log.info("Update audio path by old score audio path #{}", oldScoreAudioPath);

        score.setAudioPath(oldScoreAudioPath);
        scoreRepository.save(score);
    }

    public void updateAudioPathFromAudioCrawler(final List<Score> scores) {
        log.info("Update audio path from audio crawler for scores #{}", scores);

        scoreRepository.saveAll(scores);
    }

    @Transactional(readOnly = true)
    public List<Score> findAllAudioHasNotPath(Long sessionId){
        return scoreRepository.findScoreWithPathNull(sessionId);
    }
}
