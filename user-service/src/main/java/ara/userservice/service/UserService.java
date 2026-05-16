package ara.userservice.service;

import ara.userservice.dto.RegistrationDto;
import ara.userservice.entity.User;
import ara.userservice.exeption.UserAlreadyExistsException;
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
    private final OutboxRepository outboxRepositoryAuth;

    private final UserMapper userMapper;


    public void registrationUser(RegistrationDto registrationDto){
        if (userRepository.findByEmail(registrationDto.email()).isPresent()){
            throw new UserAlreadyExistsException("Пользователь с почтой " + registrationDto.email() + " уже существует");
        }

        User user = userMapper.toEntity(registrationDto);
        userRepository.save(user);


    }


    public String generateCode(){
       return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
