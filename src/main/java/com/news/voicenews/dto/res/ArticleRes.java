package com.news.voicenews.dto.res;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArticleRes {

    private Long id;

    @JsonProperty("article_id")
    private String articleId;

    private String url;

    private String domain;

    private String title;

    @JsonProperty("title_english")
    private String titleEnglish;

    private String content;

    @JsonProperty("content_english")
    private String contentEnglish;

    @JsonProperty("audio_path")
    private String audioPath;

    @JsonProperty("audio_path_en")
    private String audioPathEn;

    private String category;

    private Instant time;

    @JsonProperty("img_urls")
    private String imgUrls;


}
