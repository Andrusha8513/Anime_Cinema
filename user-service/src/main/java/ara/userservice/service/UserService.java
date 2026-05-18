package ara.userservice.service;

import ara.userservice.component.CodeGenerator;
import ara.userservice.component.EventPayloadFactory;
import ara.userservice.dto.RegistrationDto;
import ara.userservice.entity.OutboxEvent;
import ara.userservice.entity.User;
import ara.userservice.exeption.UserAlreadyExistsException;
import ara.userservice.mapper.OutboxEventMapper;
import ara.userservice.mapper.UserMapper;
import ara.userservice.repository.OutboxRepository;
import ara.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
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

        String authPayload = eventPayloadFactory.authRegistrationPayload(user.getId(), user.getUsername(), user.getEmail(), token);
        String emailPayload = eventPayloadFactory.emailRegistrationPayload(user.getId(), user.getEmail(), confirmationCode);


        outboxRepository.save(outboxEventMapper.toAuthEvent(user.getId(), authPayload));
        outboxRepository.save(outboxEventMapper.toEmailEvent(user.getId(), emailPayload));
        outboxRepository.save(outboxEventMapper.toCodeEvent(user.getId(), confirmationCode));
    }


    public void confirmEmail(String code) {
        UUID userId = redisEmailService.getUserIdByConfirmationCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Код не найден или истёк"));

        redisEmailService.deleteConfirmationCode(code);
        redisEmailService.markEmailConfirmed(userId);

        String activationPayload = eventPayloadFactory.emailConfirmedPayload(userId);
        OutboxEvent event = outboxEventMapper.toEmailConfirmedEvent(userId, activationPayload);
        outboxRepository.save(event);
    }

}
