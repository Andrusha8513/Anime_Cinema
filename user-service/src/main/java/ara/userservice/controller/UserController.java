package ara.userservice.controller;

import ara.userservice.dto.RegistrationDto;
import ara.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/registration")
    public ResponseEntity<String> registrationUser(@RequestBody @Valid RegistrationDto registrationDto) {
        userService.registrationUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Пользователь сохранён! Проверьте почту.");
    }

    @PostMapping("/confirm-email")
    public ResponseEntity<String> confirmEmail(@RequestParam String code) {
        userService.confirmEmail(code);
        return ResponseEntity.ok("Запрос на подтверждение почты принят.");
    }

    @PostMapping("/{userId}/resend-email")
    public ResponseEntity<String> resendEmail(@PathVariable UUID userId) {
        userService.resendingEmail(userId);
        return ResponseEntity.ok("Код повторно отправлен.");
    }

    @PostMapping("/{userId}/update-username")
    public ResponseEntity<String> updateUsername(@PathVariable UUID userId,
                                                 @RequestParam String newName) {
        userService.updateUsername(userId, newName);
        return ResponseEntity.ok("Имя пользователя обновлено.");
    }

    @PostMapping("/{userId}/update-email")
    public ResponseEntity<String> updateEmail(@PathVariable UUID userId,
                                              @RequestParam String newEmail) {
        userService.updateEmail(userId, newEmail);
        return ResponseEntity.ok("Запрос на смену email отправлен. Подтвердите новый адрес.");
    }

    @PostMapping("/{userId}/confirm-new-email")
    public ResponseEntity<String> confirmNewEmail(@PathVariable UUID userId,
                                                  @RequestParam String code) {
        userService.confirmNewEmail(userId, code);
        return ResponseEntity.ok("Email успешно изменён.");
    }
}