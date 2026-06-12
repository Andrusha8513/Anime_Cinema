package ara.mapper;



import ara.dto.AuthPayload;
import ara.entity.Auth;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;


@Mapper(componentModel = "jakarta",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuthMapper {

    Auth toEntity(AuthPayload authPayload);


}
