package ara.utiliti;

import io.vertx.core.http.HttpServerRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ClientInfoExtractor {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
            "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED"
    };

    public static String extractClientIp(HttpServerRequest request) {
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.remoteAddress() != null ? request.remoteAddress().host() : "unknown";
    }

    public static String extractUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown";
        }
        return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
    }
}