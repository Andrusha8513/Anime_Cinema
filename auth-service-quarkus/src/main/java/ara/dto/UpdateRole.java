package ara.dto;

import ara.jwt.Role;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UpdateRole(
        @NotEmpty(message = "Набор ролей не может быть пустым")
        Set<Role> roles) {

}
