package ara.web_socket;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ConnectionRegistry {


    private final Map<String , UUID> connectionToUser = new ConcurrentHashMap<>();
    private final Map<UUID , String> userToConnection = new ConcurrentHashMap<>();

    public void register(String connectionId , UUID userId){
        connectionToUser.put(connectionId , userId);
        userToConnection.put(userId , connectionId);
    }

    public void unregister(String connectionId){
        UUID userId = connectionToUser.remove(connectionId);
        if (userId != null){
            userToConnection.remove(userId);
        }
    }


    public Optional<UUID> userOf(String connectionId){
        return Optional.ofNullable(connectionToUser.get(connectionId));
    }

    public Optional<String> connectionIdOf(UUID userId){
        return Optional.ofNullable(userToConnection.get(userId));
    }
}