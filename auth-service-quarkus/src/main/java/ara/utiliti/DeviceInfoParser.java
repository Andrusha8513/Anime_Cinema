package ara.utiliti;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import ua_parser.Client;
import ua_parser.Parser;

@UtilityClass
@Slf4j
public class DeviceInfoParser {

    private static final Parser PARSER = new Parser();
    private static final String UNKNOWN_DEVICE = "Unknown Device";
    private static final String UNKNOWN_OS = "Unknown OS";
    private static final String UNKNOWN_BROWSER = "Unknown Browser";

    public static String parseDeviceInfo(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return UNKNOWN_DEVICE;
        }

        try {
            Client client = PARSER.parse(userAgent);

            String device = getDeviceFamily(client);
            String os = getOsInfo(client);
            String browser = getBrowserFamily(client);


            StringBuilder result = new StringBuilder();
            result.append(device);
            result.append(", ").append(os);

            if (!browser.equals(UNKNOWN_BROWSER)) {
                result.append(", ").append(browser);
            }

            return result.toString();

        } catch (Exception e) {
            log.warn("Не удалось распарсить User-Agent: {}", userAgent, e);
            return UNKNOWN_DEVICE;
        }
    }

    private static String getDeviceFamily(Client client) {
        if (client.device != null && client.device.family != null) {
            return client.device.family;
        }
        return UNKNOWN_DEVICE;
    }

    private static String getOsInfo(Client client) {
        if (client.os == null || client.os.family == null) {
            return UNKNOWN_OS;
        }

        StringBuilder os = new StringBuilder(client.os.family);
        if (client.os.major != null) {
            os.append(" ").append(client.os.major);
            if (client.os.minor != null) {
                os.append(".").append(client.os.minor);
            }
        }
        return os.toString();
    }

    private static String getBrowserFamily(Client client) {
        if (client.userAgent != null && client.userAgent.family != null) {
            return client.userAgent.family;
        }
        return UNKNOWN_BROWSER;
    }
}