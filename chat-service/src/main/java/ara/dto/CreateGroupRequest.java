package ara.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateGroupRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank String type,
        List<String> members
) {}

