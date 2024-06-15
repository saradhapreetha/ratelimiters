import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class TokenBucketRateLimiter {

    private Map<String , TokenBucket > bucketMap = new ConcurrentHashMap<String, TokenBucket>();


    private final long refillInterval;
    private final int tokenLimit;

    public TokenBucketRateLimiter(long refillInterval, int tokenLimit, TimeUnit unit)
    {
        this.refillInterval = unit.toMillis(refillInterval);
        this.tokenLimit = tokenLimit;
    }

    public boolean allowRequest(String clientId){
            TokenBucket clientBucket = bucketMap.computeIfAbsent(clientId, id -> new TokenBucket(refillInterval,tokenLimit));
            return clientBucket.allowRequest();
    }

    public static class TokenBucket{

         private final long refillInterval;
         private long lastRefillTimestamp;
         private final int tokenLimit;
         private int tokensRemaining;

         public TokenBucket(long refillInterval,int tokenLimit){
             this.refillInterval = refillInterval;
             this.lastRefillTimestamp = System.currentTimeMillis();
             this.tokensRemaining = tokenLimit;
             this.tokenLimit = tokenLimit;
         }

         synchronized boolean allowRequest(){
             long now = System.currentTimeMillis();
             refillTokens(now);
             if(tokensRemaining > 0)
             {
                 tokensRemaining--;
                 return true;
             }
             else{
                 return false;
             }
         }

         private void refillTokens(long currentRequestTime)
         {
             if(currentRequestTime > (lastRefillTimestamp + refillInterval)){
                 tokensRemaining = tokenLimit;
                 lastRefillTimestamp = currentRequestTime;
             }

         }
    }

    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(2, 2, TimeUnit.SECONDS);
        Random random = new Random();
        String[] choices = {"Rock", "Paper", "Scissors"};

        for (int i = 0; i < 10; i++) {
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            if (rateLimiter.allowRequest(ipAddress)) {
                String choice = choices[random.nextInt(choices.length)];
                System.out.println("Request allowed: " + choice);
            } else {
                System.out.println("Too many requests - try again later.");
            }
            // Sleep for a short time to simulate time between requests
            Thread.sleep(400);  // Adjust the sleep time as needed
        }
    }

}
