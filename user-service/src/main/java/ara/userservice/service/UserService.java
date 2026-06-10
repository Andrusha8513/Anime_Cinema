package ara.userservice.service;

import ara.userservice.component.CodeGenerator;
import ara.userservice.component.EventPayloadFactory;
import ara.userservice.dto.RegistrationDto;
import ara.userservice.entity.User;
import ara.userservice.exeption.UserAlreadyExistsException;
import ara.userservice.exeption.UserNotFoundException;
import ara.userservice.mapper.OutboxEventMapper;
import ara.userservice.mapper.UserMapper;
import ara.userservice.repository.OutboxRepository;
import ara.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final OutboxEventMapper outboxEventMapper;
    private final EventPayloadFactory eventPayloadFactory;
    private final UserMapper userMapper;
    private final RedisEmailService redisEmailService;
    private final CodeGenerator codeGenerator;


    public void registrationUser(RegistrationDto registrationDto) {
        if (userRepository.findByEmail(registrationDto.email()).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с почтой " + registrationDto.email() + " уже существует");
        }

        User user = userMapper.toEntity(registrationDto);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        String confirmationCode = codeGenerator.generateConfirmationCode();


        String codePayload = eventPayloadFactory.codePayload(user.getId(), confirmationCode);
        String authPayload = eventPayloadFactory.authRegistrationPayload(user.getId(), user.getUsername(), user.getEmail(), token);
        String emailPayload = eventPayloadFactory.emailRegistrationPayload(user.getId(), user.getEmail(), confirmationCode);


        outboxRepository.save(outboxEventMapper.toAuthEvent(user.getId(), authPayload));
        outboxRepository.save(outboxEventMapper.toEmailEvent(user.getId(), emailPayload));
        outboxRepository.save(outboxEventMapper.toCodeEvent(user.getId(), codePayload));
    }


    public void confirmEmail(String code) {
        String confirmPayload = eventPayloadFactory.confirmEmailPayload(code);
        outboxRepository.save(outboxEventMapper.toConfirmEmailRequestEvent(code, confirmPayload));
    }

    public void resendingEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));

        String confirmationCode = codeGenerator.generateConfirmationCode();

        String codePayload = eventPayloadFactory.codePayload(user.getId() , confirmationCode);
        String emailPayload = eventPayloadFactory.emailRegistrationPayload(user.getId(), user.getEmail(), confirmationCode);


        outboxRepository.save(outboxEventMapper.toCodeEvent(user.getId(), codePayload));
        outboxRepository.save(outboxEventMapper.toEmailEvent(user.getId(), emailPayload));
    }


    public void updateUsername(UUID userId, String newUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));

        user.setUsername(newUsername);
        userRepository.save(user);
        outboxRepository.save(outboxEventMapper.toNewUsernameEvent(user.getId(), newUsername));
    }

    public void updateEmail(UUID userId, String newEmail) {
        if (userRepository.findByEmail(newEmail).isPresent()) {
            throw new UserAlreadyExistsException("Пользователь с почтой " + newEmail + " уже существует");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));

        user.setPendingEmail(newEmail);
        String confirmationCode = codeGenerator.generateConfirmationCode();

        String codePayload = eventPayloadFactory.codePayload(user.getId(), confirmationCode);
        String emailPayload = eventPayloadFactory.emailRegistrationPayload(user.getId(), newEmail, confirmationCode);

        userRepository.save(user);
        outboxRepository.save(outboxEventMapper.toNewEmailEvent(user.getId(), emailPayload));
        outboxRepository.save(outboxEventMapper.toCodeEvent(user.getId(), codePayload));
    }

    public void confirmNewEmail(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));
        if (user.getPendingEmail() == null) {
            throw new IllegalStateException("Нет ожидающей смены email");
        }

        String payload = eventPayloadFactory.confirmEmailPayload(code);
        outboxRepository.save(outboxEventMapper.toConfirmNewEmailRequestEvent(user.getId(), payload));
    }
}
