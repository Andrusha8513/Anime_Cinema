package ara.userservice.controller;

import ara.userservice.dto.RegistrationDto;
import ara.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RedisTemplate redisTemplate;

    @PostMapping("/registration")
    public ResponseEntity<?> registrationUser(@RequestBody RegistrationDto registrationDto){
        try {
            userService.registrationUser(registrationDto);
            return ResponseEntity.ok("Пользователь сохранён!");
        }catch (RuntimeException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
