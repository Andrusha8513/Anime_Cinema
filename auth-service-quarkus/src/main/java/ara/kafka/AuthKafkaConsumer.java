//package ara.kafka;
//
//
//import ara.entity.Auth;
//import ara.mapper.AuthMapper;
//import ara.repository.AuthRepository;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import io.smallrye.mutiny.Uni;
//import io.smallrye.reactive.messaging.annotations.Blocking;
//import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
//import jakarta.enterprise.context.ApplicationScoped;
//import jakarta.inject.Inject;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.eclipse.microprofile.reactive.messaging.Incoming;
//
//import java.util.Optional;
//import java.util.UUID;
//
//
//@Slf4j
//@ApplicationScoped
//@RequiredArgsConstructor
//public class AuthKafkaConsumer {
//
//    private final ObjectMapper objectMapper;
//
//   private final AuthRepository authRepository;
//    private final AuthMapper authMapper;
////    private final RegistrationTokenRepository registrationTokenRepository;
//
//
//    @Incoming("auth-service-quarkus")
//    @Blocking
//    @Transactional
//    public Uni<Void> onAuthEvent(IncomingKafkaRecord<String, String> record) {
//
//        String eventType = record.getHeaders().lastHeader("eventType") != null
//                ? new String(record.getHeaders().lastHeader("eventType").value())
//                : null;
//
//        String eventId = record.getHeaders().lastHeader("id") != null
//                ? new String((record.getHeaders()).lastHeader("id").value())
//                : null;
//
//        String payload = record.getPayload();
//
//        log.info("Получено сообщение: topic={}, partition={}, offset={}, eventId={}, eventType={}",
//                record.getTopic(), record.getPartition(), record.getOffset(), eventId, eventType);
//
//
//
//
//        try {
//            switch (eventType) {
////                case "AUTH_REGISTRATION" -> handleAuthRegistration(payload, eventId);
//                case "EMAIL_CONFIRMED" -> handleAccountStatusChange(payload, eventType, eventId);
//                default -> log.debug("Пропускаем событие с типом: {}", eventType);
//            }
//        } catch (JsonMappingException e) {
//            log.error("Критическая ошибка десериализации события {}. Не удалось прочитать JSON.", eventId, e);
//            return Uni.createFrom().failure(new RuntimeException("Ошибка обработки JSON для события " + eventId, e));
//        } catch (JsonProcessingException e) {
//            log.error("Ошибка парсинга JSON для события {}. Текст сообщения имеет некорректный формат.", eventId, e);
//            return Uni.createFrom().failure(new RuntimeException("Не удалось обработать событие " + eventId, e));
//        }
//        return Uni.createFrom().voidItem();
//    }
//
//
//
//
//
//
//
////    private void handleAuthRegistration(String payload, String eventId) throws JsonProcessingException {
////        AuthPayload authPayload = objectMapper.readValue(payload, AuthPayload.class);
////
////        if (authPayload == null || authPayload.email() == null || authPayload.email().isBlank()) {
////            throw new IllegalArgumentException("[" + eventId + "] Email обязателен в payload");
////        }
////
////
////
////        if (authRepository.findById(authPayload.userId()).isPresent()) {
////            log.info("[{}] Пользователь {} уже существует, пропускаем", eventId, authPayload.userId());
////            return;
////        }
////
////        Auth auth = authMapper.toEntity(authPayload);
////        auth.setRoles(Collections.singleton(Role.USER));
////        authRepository.persist(auth);
////
////        RegistrationToken regToken = authMapper.toRegTokenEntity(authPayload);
////        registrationTokenRepository.persist(regToken);
////
////        log.info("[{}] Пользователь {} сохранён в БД с токеном регистрации", eventId, authPayload.userId());
////    }
////
//
//
//
//
//
//    private void handleAccountStatusChange(String eventType, String payload, String eventId) throws JsonProcessingException {
//        JsonNode jsonNode = objectMapper.readTree(payload);
//        JsonNode userIdNode = jsonNode.get("userId");
//
//        if (userIdNode == null || userIdNode.asText().isBlank()) {
//            throw new IllegalArgumentException("[" + eventId + "] userId обязателен в payload для события " + eventType);
//        }
//
//        UUID userId = UUID.fromString(userIdNode.asText());
//        Optional<Auth> authOpt = authRepository.findById(userId);
//
//        if (authOpt.isEmpty()) {
//            log.error("[{}] Пользователь {} не найден для события {}", eventId, userId, eventType);
//            return;
//        }
//
//        Auth auth = authOpt.get();
//        switch (eventType) {
//            case "EMAIL_CONFIRMED" -> {
//                if (auth.isEnabled()) {
//                    log.info("[{}] Пользователь {} уже активирован, пропускаем", eventId, userId);
//                    return;
//                }
//                auth.setEnabled(true);
//                log.info("[{}] Пользователь {} активирован (email подтверждён)", eventId, userId);
//            }
//        }
//
//        authRepository.persist(auth);
//    }
//}
