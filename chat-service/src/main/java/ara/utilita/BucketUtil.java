package ara.utilita;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public final class BucketUtil {
    private BucketUtil(){}

    public static  int bucketFor(Instant instant){
        long days = ChronoUnit.DAYS.between(Instant.EPOCH , instant);
        return (int) (days / 7);
    }

    public static int currentBucket(){
        return bucketFor(Instant.now());
    }
}
