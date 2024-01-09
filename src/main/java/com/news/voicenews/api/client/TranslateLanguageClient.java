package com.news.voicenews.api.client;

import com.news.voicenews.dto.req.NewsCrawlerReq;
import com.news.voicenews.dto.req.TranslateLanguageReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static com.news.voicenews.constant.CrawlerRanker.TRANSLATE_LANGUAGE_URL;


@Slf4j
    @Component
    public class TranslateLanguageClient {

        private final RestTemplate restTemplate;

        public TranslateLanguageClient(final RestTemplateBuilder restTemplateBuilder) {
            this.restTemplate = restTemplateBuilder.build();
        }

        public ResponseEntity<?>translateLanguage(TranslateLanguageReq translateLanguageReq) {
            return restTemplate.postForEntity(TRANSLATE_LANGUAGE_URL, translateLanguageReq, String.class);
        }
    }

