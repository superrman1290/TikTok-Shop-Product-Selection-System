package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.auth.domain.LoginThrottle;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLoginThrottle implements LoginThrottle {

    private static final int ACCOUNT_LIMIT = 5;
    private static final int IP_LIMIT = 20;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final String ACCOUNT_PREFIX = "auth:login:account:";
    private static final String IP_PREFIX = "auth:login:ip:";
    private final StringRedisTemplate redisTemplate;

    public RedisLoginThrottle(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isIpBlocked(String ipHash) {
        String value = redisTemplate.opsForValue().get(IP_PREFIX + ipHash);
        return value != null && Long.parseLong(value) >= IP_LIMIT;
    }

    @Override
    public FailureResult recordFailure(String accountHash, String ipHash) {
        long accountCount = increment(ACCOUNT_PREFIX + accountHash);
        long ipCount = increment(IP_PREFIX + ipHash);
        return new FailureResult(accountCount >= ACCOUNT_LIMIT, ipCount >= IP_LIMIT);
    }

    @Override
    public void clearAccountFailures(String accountHash) {
        redisTemplate.delete(ACCOUNT_PREFIX + accountHash);
    }

    private long increment(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            throw new IllegalStateException("Redis did not return a login attempt count");
        }
        if (count == 1) {
            redisTemplate.expire(key, WINDOW);
        }
        return count;
    }
}
