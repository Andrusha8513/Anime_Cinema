package ara.userservice.controller;

import ara.userservice.dto.*;
import ara.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;



    @PostMapping("/registration")
    public ResponseEntity<RegistrationResponse> registrationUser(
            @RequestBody @Valid RegistrationDto registrationDto) {
        RegistrationResponse response = userService.registrationUser(registrationDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/confirm-email")
    public ResponseEntity<ConfirmationResponse> confirmEmail(
            @RequestBody @Valid ConfirmEmailRequest request) {
        ConfirmationResponse response = userService.confirmRegistration(request.code());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/resend-email")
    public ResponseEntity<String> resendEmail(@RequestBody @Valid ResendEmailRequest request) {
        userService.resendingEmail(request.userId());
        return ResponseEntity.ok("Код повторно отправлен.");
    }



    @PostMapping("/update-username")
    public ResponseEntity<String> updateUsername(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateUsernameRequest request) {
        UUID userId = userService.currentUserId(jwt);
        userService.updateUsername(userId, request.newUsername());
        return ResponseEntity.ok("Имя пользователя обновлено.");
    }

    @PostMapping("/update-email")
    public ResponseEntity<String> updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateEmailRequest request) {
        UUID userId = userService.currentUserId(jwt);
        userService.updateEmail(userId, request.newEmail());
        return ResponseEntity.ok("Запрос на смену email отправлен. Подтвердите новый адрес.");
    }

    @PostMapping("/confirm-new-email")
    public ResponseEntity<String> confirmNewEmail(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ConfirmEmailRequest request) {
        UUID userId = userService.currentUserId(jwt);
        userService.confirmNewEmail(userId, request.code());
        return ResponseEntity.ok("Email успешно изменён.");
    }
}