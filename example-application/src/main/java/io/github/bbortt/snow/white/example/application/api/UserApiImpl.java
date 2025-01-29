package io.github.bbortt.snow.white.example.application.api;

import io.github.bbortt.snow.white.example.application.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserApiImpl implements UserApi {

  public ResponseEntity<User> getUserByName(String username) {
    return ResponseEntity.ok(new User());
  }
}
