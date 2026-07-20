package ara.userservice.service;

import ara.userservice.component.CodeGenerator;
import ara.userservice.component.EventPayloadFactory;
import ara.userservice.component.IdGenerator;
import ara.userservice.dto.ConfirmationResponse;
import ara.userservice.dto.RedisUserDto;
import ara.userservice.dto.RegistrationDto;
import ara.userservice.dto.RegistrationResponse;
import ara.userservice.entity.User;
import ara.userservice.exeption.InvalidConfirmationCodeException;
import ara.userservice.exeption.UserAlreadyExistsException;
import ara.userservice.exeption.UserNotFoundException;
import ara.userservice.mapper.OutboxEventMapper;
import ara.userservice.mapper.UserMapper;
import ara.userservice.repository.OutboxRepository;
import ara.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final OutboxEventMapper outboxEventMapper;
    private final EventPayloadFactory eventPayloadFactory;
    private final UserMapper userMapper;
    private final CodeGenerator codeGenerator;
    private final IdGenerator idGenerator;
    private final RedisEmailService redisEmailService;
    private final RedisUser redisUser;
    private final RegistrationTokenService registrationTokenService;


    public RegistrationResponse registrationUser(RegistrationDto registrationDto) {
        rejectIfActiveUserExists(registrationDto);

        if (userRepository.findByUsername(registrationDto.username()).isPresent()) {
            throw new UserAlreadyExistsException(
                    "Пользователь с ником " + registrationDto.username() + " уже существует");
        }


        UUID userId = idGenerator.newId();
        String confirmationCode = codeGenerator.generateConfirmationCode();

        RedisUserDto redisUserDto = new RedisUserDto(registrationDto.username(), registrationDto.email());
        String userPayload = eventPayloadFactory.redisUserPayload(redisUserDto);
        String codePayload = eventPayloadFactory.codePayload(userId, confirmationCode);
        String emailPayload = eventPayloadFactory.emailRegistrationPayload(
                userId, registrationDto.email(), confirmationCode);


        outboxRepository.save(outboxEventMapper.toSaveUserToRedisEvent(userId, userPayload));
        outboxRepository.save(outboxEventMapper.toCodeEvent(userId, codePayload));
        outboxRepository.save(outboxEventMapper.toEmailEvent(userId, emailPayload));


        return new RegistrationResponse(
                "Регистрация начата. Подтвердите email кодом из письма.", userId.toString());
    }


    public ConfirmationResponse confirmRegistration(String code) {
        UUID userId = redisEmailService.getUserIdByConfirmationCode(code)
                .orElseThrow(() -> new InvalidConfirmationCodeException("Код не найден или истёк"));

        RedisUserDto pending = redisUser.getPendingUser(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Данные регистрации для " + userId + " не найдены или истекли"));


        Optional<User> existing = userRepository.findById(userId);
        if (existing.isPresent()) {
            cleanupRedis(code, userId);
            return new ConfirmationResponse(issueToken(existing.get()));
        }

        User user = new User();
        user.setId(userId);
        user.setUsername(pending.username());
        user.setEmail(pending.email());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new UserAlreadyExistsException("Пользователь с таким username или email уже существует");
        }

        String userCreatedPayload = eventPayloadFactory.userCreatedPayload(userId , user.getUsername());
        outboxRepository.save(outboxEventMapper.toUserCreatedEvent(userId , userCreatedPayload));

        cleanupRedis(code, userId);

        return new ConfirmationResponse(issueToken(user));
    }




        public void resendingEmail (UUID userId){
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));

            String confirmationCode = codeGenerator.generateConfirmationCode();

            String codePayload = eventPayloadFactory.codePayload(user.getId(), confirmationCode);
            String emailPayload = eventPayloadFactory.emailRegistrationPayload(user.getId(), user.getEmail(), confirmationCode);


            outboxRepository.save(outboxEventMapper.toCodeEvent(user.getId(), codePayload));
            outboxRepository.save(outboxEventMapper.toEmailEvent(user.getId(), emailPayload));
        }


        public void updateUsername (UUID userId, String newUsername){
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));

            user.setUsername(newUsername);
            userRepository.save(user);
            outboxRepository.save(outboxEventMapper.toNewUsernameEvent(user.getId(), newUsername));
        }

        public void updateEmail (UUID userId, String newEmail){
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

        public void confirmNewEmail (UUID userId, String code){
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("Пользователь с таким id: " + userId + " не найден"));
            if (user.getPendingEmail() == null) {
                throw new IllegalStateException("Нет ожидающей смены email");
            }

            String payload = eventPayloadFactory.confirmEmailPayload(code);
            outboxRepository.save(outboxEventMapper.toConfirmNewEmailRequestEvent(user.getId(), payload));
        }


        private String issueToken(User user){
         return   registrationTokenService.issue(user.getId(), user.getUsername(), user.getEmail());
        }

        private void cleanupRedis(String code , UUID userId){
        redisEmailService.deleteConfirmationCode(code);
        redisUser.deletePendingUser(userId);
        }

        private void rejectIfActiveUserExists(RegistrationDto dto){
        if (userRepository.findByEmail(dto.email()).isPresent()){
            throw new UserAlreadyExistsException(
                    "Пользователь с почтой " + dto.email() + " уже существует");
        }
        }




    public UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
//        private Optional<RegistrationResponse> handleExistingUser (RegistrationDto registrationDto){
//            Optional<User> existingUser = userRepository.findByEmail(registrationDto.email());
//
//            if (existingUser.isEmpty()) {
//                return Optional.empty();
//            }
//
//            User user = existingUser.get();
//
//            if (user.getStatus() == UserStatus.ACTIVE) {
//                throw new UserAlreadyExistsException(
//                        "Пользователь с почтой " + registrationDto.email() + " уже существует");
//            }
//
//            log.info("Повторная регистрация для неактивированного пользователя {}", user.getId());
//
//            String newToken = UUID.randomUUID().toString();
//            String newCode = codeGenerator.generateConfirmationCode();
//
//            outboxRepository.save(outboxEventMapper.toAuthEvent(user.getId(), eventPayloadFactory.authRegistrationPayload(user.getId(), user.getUsername(), user.getEmail(), newToken)));
//            outboxRepository.save(outboxEventMapper.toEmailEvent(user.getId(), eventPayloadFactory.emailRegistrationPayload(user.getId(), user.getEmail(), newCode)));
//            outboxRepository.save(outboxEventMapper.toCodeEvent(user.getId(), eventPayloadFactory.codePayload(user.getId(), newCode)));
//
//            user.setStatus(UserStatus.PENDING_PASSWORD);
//            userRepository.save(user);
//            return Optional.of(new RegistrationResponse(
//                    "Повторная регистрация. Установите пароль.",
//                    newToken
//            ));
//        }

    }
