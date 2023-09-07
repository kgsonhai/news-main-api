package com.news.voicenews.api.admin;

import com.news.voicenews.bloc.CrawlerBloc;
import com.news.voicenews.dto.req.RefreshTokenReq;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/admin")
public class CrawlerController {

    private final CrawlerBloc crawlerBloc;

    public CrawlerController(final CrawlerBloc crawlerBloc) {
        this.crawlerBloc = crawlerBloc;
    }

    @PostMapping("/crawler")
    public ResponseEntity<?> startCrawler() {
        return new ResponseEntity<>(crawlerBloc.startCrawler(), CREATED);
    }

    @PostMapping("/test/audio/empty")
    public ResponseEntity<?> testAudio() {
        crawlerBloc.updateScoreHasPointButAudioNull(47L);
        return ResponseEntity.ok(400);
    }

}
