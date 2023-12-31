package com.news.voicenews.bloc;

import com.news.voicenews.constant.NumberOfArticles;
import com.news.voicenews.dto.res.ArticleRes;
import com.news.voicenews.helper.SecurityHelper;
import com.news.voicenews.model.Article;
import com.news.voicenews.model.Category;
import com.news.voicenews.model.Score;
import com.news.voicenews.service.ArticleService;
import com.news.voicenews.service.CategoryService;
import com.news.voicenews.service.ScoreService;
import com.news.voicenews.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
public class ArticleBloc {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final SessionService sessionService;
    private final ScoreService scoreService;

    public ArticleBloc(final ArticleService articleService,
                       final CategoryService categoryService,
                       final SessionService sessionService,
                       final ScoreService scoreService) {
        this.articleService = articleService;
        this.categoryService = categoryService;
        this.sessionService = sessionService;
        this.scoreService = scoreService;
    }

    @Transactional(readOnly = true)
    public ArticleRes findArticleById(Long id) {
        log.info("Find article by id #{}", id);
        Score score = scoreService.findById(id);
        Article articleMongo = articleService.fetchArticleById(score.getArticleId());

        return ArticleRes.builder()
                         .id(score.getId())
                         .url(score.getUrl())
                         .domain(score.getDomain())
                         .title(articleMongo.getTitle())
                         .titleEnglish(articleMongo.getTitle_english())
                         .category(score.getCategory())
                         .time(articleMongo.getTime())
                         .content(articleMongo.getContent())
                         .contentEnglish(articleMongo.getContent_english())
                         .audioPath(score.getAudioPath())
                         .audioPathEn(score.getAudioPathEn())
                         .imgUrls(articleMongo.getImg_urls())
                         .build();
    }

    @Transactional(readOnly = true)
    public List<ArticleRes> fetchArticlesNoLogin() {
        log.info("Fetch articles no login");

        Long sessionId = sessionService.findValidSessionId();
        List<Category> categories = categoryService.fetchAllCategories();

        log.info("Get all article", getArticleRes(categories, sessionId));

        return getArticleRes(categories, sessionId);
    }

    @Transactional(readOnly = true)
    public List<ArticleRes> fetchArticleByCurrentUser() {
        log.info("Fetch articles by current user");

        Long currentUserId = SecurityHelper.getUserId();
        Long sessionId = sessionService.findValidSessionId();

        List<Category> categories = categoryService.findAllByUserId(currentUserId);
        return getArticleRes(categories, sessionId);
    }

    @Transactional(readOnly = true)
    public List<ArticleRes> fetchArticlesByCategoryName(final String categoryName) {
        log.info("Fetch articles by category name #{}", categoryName);

        Long sessionId = sessionService.findValidSessionId();

        List<Category> categories = Collections.singletonList(categoryService.findByName(categoryName));
        return getArticleRes(categories, sessionId);
    }

    public List<ArticleRes> getArticleRes(final List<Category> categories, Long sessionId) {
        List<String> categoryNames = categories.stream()
                                               .map(Category::getName)
                                               .collect(Collectors.toList());

        List<Integer> numberOfArticlesPerCategory = getNumberOfArticlesPerCategory(NumberOfArticles.NUMBER_OF_ARTICLES,
                                                                                   categories.size());

        List<Score> scores = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            List<Score> sc = scoreService.fetchScoresBySessionIdCategoryWithLimit(sessionId,
                                                                                  categoryNames.get(i));
            scores.addAll(sc);
        }

        log.info("scores", scores);

        return scores.stream()
                     .map(score -> {
                         Article articleMongo = articleService
                                 .fetchArticleById(score.getArticleId());
                         return ArticleRes.builder()
                                          .id(score.getId())
                                          .articleId(articleMongo.getUuid())
                                          .url(score.getUrl())
                                          .domain(score.getDomain())
                                          .title(articleMongo.getTitle())
                                          .category(score.getCategory())
                                          .time(articleMongo.getTime())
                                          .content(articleMongo.getContent())
                                          .audioPath(score.getAudioPath())
                                          .imgUrls(articleMongo.getImg_urls())
                                          .titleEnglish(articleMongo.getTitle_english())
                                          .contentEnglish(articleMongo.getContent_english())
                                          .audioPathEn(score.getAudioPathEn())
                                          .build();
                     })
                     .collect(Collectors.toList());
    }

    public List<Integer> getNumberOfArticlesPerCategory(int numberOfArticles, int numberOfCategories) {
        List<Integer> numberOfArticlesPerCategory;

        numberOfArticlesPerCategory = IntStream.range(0, numberOfCategories)
                                               .mapToObj(i -> numberOfArticles / numberOfCategories)
                                               .collect(Collectors.toList());

        int r = numberOfArticles - numberOfCategories * (numberOfArticles / numberOfCategories);

        List<Integer> finalNumberOfArticlesPerCategory = numberOfArticlesPerCategory;
        IntStream.range(0, r)
                 .forEach(i -> finalNumberOfArticlesPerCategory.set(i, finalNumberOfArticlesPerCategory.get(i) + 1));

        return finalNumberOfArticlesPerCategory;
    }


}
