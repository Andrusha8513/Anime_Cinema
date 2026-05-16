package ara.userservice.mapper;

import ara.userservice.dto.RegistrationDto;
import ara.userservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegistrationDto registrationDto);
}
