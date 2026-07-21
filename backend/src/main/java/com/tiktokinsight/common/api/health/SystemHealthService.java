package com.tiktokinsight.common.api.health;

import com.tiktokinsight.storage.ObjectStorage;
import java.sql.Connection;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

@Service
public class SystemHealthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemHealthService.class);

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final ObjectStorage objectStorage;

    public SystemHealthService(
            DataSource dataSource,
            RedisConnectionFactory redisConnectionFactory,
            ObjectStorage objectStorage
    ) {
        this.dataSource = dataSource;
        this.redisConnectionFactory = redisConnectionFactory;
        this.objectStorage = objectStorage;
    }

    public SystemHealth check() {
        return new SystemHealth(
                ComponentStatus.UP,
                checkDatabase(),
                checkRedis(),
                objectStorage.isAvailable() ? ComponentStatus.UP : ComponentStatus.DOWN
        );
    }

    private ComponentStatus checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? ComponentStatus.UP : ComponentStatus.DOWN;
        } catch (Exception exception) {
            LOGGER.warn("event=health_check_failed component=database message={}", exception.getMessage());
            return ComponentStatus.DOWN;
        }
    }

    private ComponentStatus checkRedis() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            return "PONG".equalsIgnoreCase(connection.ping())
                    ? ComponentStatus.UP
                    : ComponentStatus.DOWN;
        } catch (Exception exception) {
            LOGGER.warn("event=health_check_failed component=redis message={}", exception.getMessage());
            return ComponentStatus.DOWN;
        }
    }
}
