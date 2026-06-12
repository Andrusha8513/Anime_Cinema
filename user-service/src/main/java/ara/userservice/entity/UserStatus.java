package ara.userservice.entity;

public enum UserStatus {
    PENDING_PASSWORD,              // Ввёл email, ждёт установки пароля
    PENDING_EMAIL_CONFIRMATION,    // Установил пароль, ждёт подтверждения email
    ACTIVE,                        // Полностью зарегистрирован
    BLOCKED                        // Заблокирован администратором
}
