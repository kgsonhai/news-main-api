package com.news.voicenews.api.common;

import com.news.voicenews.bloc.ArticleBloc;
import com.news.voicenews.dto.req.LoginReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/common")
public class ArticleController {

    private final ArticleBloc articleBloc;

    public ArticleController(final ArticleBloc articleBloc) {
        this.articleBloc = articleBloc;
    }

    @GetMapping("/article/{id}")
    public ResponseEntity<?> fetchArticleDetail(@PathVariable Long id) {
        return ResponseEntity.ok(articleBloc.findArticleById(id));
    }

    @GetMapping("/article")
    public ResponseEntity<?> fetchArticlesRanked() {
        return ResponseEntity.ok(articleBloc.fetchArticlesNoLogin());
    }

    @GetMapping("article/name/{categoryName}")
    public ResponseEntity<?> fetchArticlesByCategoryName(@PathVariable final String categoryName) {
        if(categoryName.equals("all")){
            return ResponseEntity.ok(articleBloc.fetchArticlesNoLogin());
        }
        return ResponseEntity.ok(articleBloc.fetchArticlesByCategoryName(categoryName));
    }
}
