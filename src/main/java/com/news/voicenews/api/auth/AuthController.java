package com.news.voicenews.api.auth;

import com.news.voicenews.api.client.TranslateLanguageClient;
import com.news.voicenews.bloc.CrawlerBloc;
import com.news.voicenews.bloc.JwtBloc;
import com.news.voicenews.bloc.RegisterBloc;
import com.news.voicenews.dto.req.*;
import com.news.voicenews.dto.res.TokenRes;
import com.news.voicenews.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtBloc jwtBloc;
    private final AuthenticationManager authenticationManager;
    private final RegisterBloc registerBloc;

    private final CrawlerBloc crawlerBloc;

    private  final TranslateLanguageClient translateLanguageClient;

    public AuthController(final JwtBloc jwtBloc,
                          final AuthenticationManager authenticationManager,
                          final RegisterBloc registerBloc, final CrawlerBloc crawlerBloc, final TranslateLanguageClient translateLanguageClient
    ) {
        this.jwtBloc = jwtBloc;
        this.authenticationManager = authenticationManager;
        this.registerBloc = registerBloc;
        this.crawlerBloc = crawlerBloc;
        this.translateLanguageClient = translateLanguageClient;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterReq registerReq) {
        registerBloc.register(registerReq);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenRes> login(@RequestBody final LoginReq loginReq) {
        Authentication authentication =
                authenticationManager
                        .authenticate(new UsernamePasswordAuthenticationToken(loginReq.getUsername(),
                                                                              loginReq.getPassword()));
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        TokenRes tokenRes = jwtBloc.generateToken(userDetails);


        return ResponseEntity.ok(tokenRes);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRes> refreshToken(@RequestBody final RefreshTokenReq refreshTokenReq) {

        // TODO: Get refresh token from Redis to make sure that it is exists for current user
        if (!jwtBloc.isExpiredToken(refreshTokenReq)) {
            return ResponseEntity.ok(jwtBloc.generateToken(refreshTokenReq));
        }
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @PostMapping("/test/call-service-translate")
    public ResponseEntity<?> debugCrawlAudio(@RequestBody TranslateLanguageReq translateLanguageReq) {
        long sessionId = translateLanguageReq.getSessionId();
        translateLanguageClient.translateLanguage(translateLanguageReq);
        return ResponseEntity.ok(sessionId);
    }
}
