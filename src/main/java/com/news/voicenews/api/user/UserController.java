package com.news.voicenews.api.user;

import com.news.voicenews.bloc.ArticleBloc;
import com.news.voicenews.bloc.UserBloc;
import com.news.voicenews.dto.req.PasswordUpdateReq;
import com.news.voicenews.dto.req.UserUpdateReq;
import com.news.voicenews.helper.SecurityHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserBloc userBloc;
    private final ArticleBloc articleBloc;

    public UserController(final UserBloc userBloc, final ArticleBloc articleBloc) {
        this.userBloc = userBloc;
        this.articleBloc = articleBloc;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUserProfile() {
        Long currentUserId = SecurityHelper.getUserId();

        return ResponseEntity.ok(userBloc.getUserProfileByUserId(currentUserId));
    }

    @GetMapping("/article")
    public ResponseEntity<?> fetchArticlesByCurrentUser() {
        return ResponseEntity.ok(articleBloc.fetchArticleByCurrentUser());
    }

    @GetMapping("/category")
    public ResponseEntity<?> fetchCategoryByCurrentUser() {
        return ResponseEntity.ok(articleBloc.fetchArticleByCurrentUser());
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateUserProfile(@RequestBody UserUpdateReq userUpdateReq) {
        userBloc.updateUserProfile(userUpdateReq);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody PasswordUpdateReq passwordUpdateReq) {
        userBloc.changePassword(passwordUpdateReq);
        return ResponseEntity.noContent().build();
    }
}
