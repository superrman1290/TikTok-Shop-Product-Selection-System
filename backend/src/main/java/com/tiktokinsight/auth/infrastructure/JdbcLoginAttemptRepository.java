package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.auth.domain.LoginAttemptRepository;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLoginAttemptRepository implements LoginAttemptRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcLoginAttemptRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void record(
            Long userId,
            String accountKeyHash,
            String ipAddressHash,
            boolean successful,
            Instant attemptedAt
    ) {
        jdbcTemplate.update("""
                INSERT INTO login_attempt (
                    user_id, account_key_hash, ip_address_hash, successful, attempted_at
                ) VALUES (?, ?, ?, ?, ?)
                """, userId, accountKeyHash, ipAddressHash, successful, Timestamp.from(attemptedAt));
    }
}
