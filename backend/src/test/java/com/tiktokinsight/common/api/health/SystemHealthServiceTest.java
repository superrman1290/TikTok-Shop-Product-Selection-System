package com.tiktokinsight.common.api.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tiktokinsight.storage.ObjectStorage;
import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class SystemHealthServiceTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final RedisConnectionFactory redisConnectionFactory = mock(RedisConnectionFactory.class);
    private final ObjectStorage objectStorage = mock(ObjectStorage.class);
    private final SystemHealthService service = new SystemHealthService(
            dataSource,
            redisConnectionFactory,
            objectStorage
    );

    @Test
    void reportsAllComponentsUp() throws Exception {
        Connection databaseConnection = mock(Connection.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(dataSource.getConnection()).thenReturn(databaseConnection);
        when(databaseConnection.isValid(2)).thenReturn(true);
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");
        when(objectStorage.isAvailable()).thenReturn(true);

        SystemHealth health = service.check();

        assertThat(health.allComponentsUp()).isTrue();
        assertThat(health.database()).isEqualTo(ComponentStatus.UP);
        assertThat(health.redis()).isEqualTo(ComponentStatus.UP);
        assertThat(health.storage()).isEqualTo(ComponentStatus.UP);
    }

    @Test
    void reportsUnavailableDependenciesWithoutThrowing() throws Exception {
        when(dataSource.getConnection()).thenThrow(new IllegalStateException("database unavailable"));
        when(redisConnectionFactory.getConnection()).thenThrow(new IllegalStateException("redis unavailable"));
        when(objectStorage.isAvailable()).thenReturn(false);

        SystemHealth health = service.check();

        assertThat(health.allComponentsUp()).isFalse();
        assertThat(health.database()).isEqualTo(ComponentStatus.DOWN);
        assertThat(health.redis()).isEqualTo(ComponentStatus.DOWN);
        assertThat(health.storage()).isEqualTo(ComponentStatus.DOWN);
    }
}
