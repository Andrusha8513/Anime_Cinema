package ara.userservice.mapper;

import ara.userservice.entity.OutboxEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface OutboxEventMapper {

    @Named("uuidToString")
    static String uuidToString(UUID uuid) {
        return uuid.toString();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "AUTH_REGISTRATION")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toAuthEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "EMAIL_REGISTRATION")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toEmailEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "NEW_EMAIL")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toNewEmailEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "CONFIRMATION_CODE_SAVE")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "processed", constant = "false")
    OutboxEvent toCodeEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "code")
    @Mapping(target = "aggregateType", constant = "INTERNAL")
    @Mapping(target = "type", constant = "CONFIRM_EMAIL_REQUEST")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "processed", constant = "false")
    OutboxEvent toConfirmEmailRequestEvent(String code, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "EMAIL_CONFIRMED")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toEmailConfirmedEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "NEW_USERNAME")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toNewUsernameEvent(UUID userId, String payload);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "INTERNAL")
    @Mapping(target = "type", constant = "CONFIRM_NEW_EMAIL_REQUEST")
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "processed", constant = "false")
    OutboxEvent toConfirmNewEmailRequestEvent(UUID userId, String payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "aggregateId", source = "userId", qualifiedByName = "uuidToString")
    @Mapping(target = "aggregateType", constant = "USER")
    @Mapping(target = "type", constant = "EMAIL_CHANGED")
    @Mapping(target = "payload", source = "payload")
    OutboxEvent toEmailChangedEvent(UUID userId, String payload);
}