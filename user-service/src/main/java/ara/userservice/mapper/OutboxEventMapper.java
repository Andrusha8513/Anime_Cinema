package ara.userservice.mapper;

import ara.userservice.entity.OutboxEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OutboxEventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", expression = "java(userId.toString())")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "AUTH_REGISTRATION")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toAuthEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", expression = "java(userId.toString())")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "EMAIL_REGISTRATION")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toEmailEvent(UUID userId, String payload);
}
