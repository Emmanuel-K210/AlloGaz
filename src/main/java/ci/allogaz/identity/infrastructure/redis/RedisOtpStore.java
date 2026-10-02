package ci.allogaz.identity.infrastructure.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import ci.allogaz.identity.application.port.out.OtpStore;
import ci.allogaz.identity.domain.PhoneNumber;

/**
 * Clés Redis (toutes avec expiration) :
 * otp:code:{tel} (hash du code), otp:attempts:{tel}, otp:cooldown:{tel}, otp:sends:{tel}.
 */
@Component
class RedisOtpStore implements OtpStore {

    private final StringRedisTemplate redis;

    RedisOtpStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Optional<Duration> resendCooldown(PhoneNumber phone) {
        Long ttl = redis.getExpire(key("cooldown", phone));
        return ttl != null && ttl > 0 ? Optional.of(Duration.ofSeconds(ttl)) : Optional.empty();
    }

    @Override
    public long incrementSendCount(PhoneNumber phone, Duration window) {
        String key = key("sends", phone);
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, window);
        }
        return count == null ? 0 : count;
    }

    @Override
    public void storeCode(PhoneNumber phone, String codeHash, Duration ttl, Duration resendCooldown) {
        redis.opsForValue().set(key("code", phone), codeHash, ttl);
        redis.delete(key("attempts", phone));
        redis.opsForValue().set(key("cooldown", phone), "1", resendCooldown);
    }

    @Override
    public Optional<String> findCodeHash(PhoneNumber phone) {
        return Optional.ofNullable(redis.opsForValue().get(key("code", phone)));
    }

    @Override
    public long incrementAttempts(PhoneNumber phone) {
        String key = key("attempts", phone);
        Long attempts = redis.opsForValue().increment(key);
        Long codeTtl = redis.getExpire(key("code", phone));
        redis.expire(key, Duration.ofSeconds(codeTtl != null && codeTtl > 0 ? codeTtl : 60));
        return attempts == null ? 0 : attempts;
    }

    @Override
    public void deleteCode(PhoneNumber phone) {
        redis.delete(key("code", phone));
        redis.delete(key("attempts", phone));
    }

    private static String key(String kind, PhoneNumber phone) {
        return "otp:" + kind + ":" + phone.value();
    }
}
