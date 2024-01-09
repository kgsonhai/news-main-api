package com.news.voicenews.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.news.voicenews.enums.CrawlerStatus;
import lombok.*;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TranslateLanguageReq {

    @NotNull
    @JsonProperty("session_id")
    private Long sessionId;

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}
