package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.auth.domain.UserAccount;
import com.tiktokinsight.auth.domain.UserAccountRepository;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.auth.domain.UserStatus;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccountRepository implements UserAccountRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, email, username, password_hash, role, status, locked_until,
                   password_changed_at, created_at, updated_at
            FROM user_account
            """;
    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccountRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE email = ?", this::mapUser, email)
                .stream().findFirst();
    }

    @Override
    public Optional<UserAccount> findById(long userId) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE id = ?", this::mapUser, userId)
                .stream().findFirst();
    }

    @Override
    public UserAccount create(
            String email,
            String username,
            String passwordHash,
            UserRole role,
            UserStatus status,
            Instant now
    ) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO user_account (
                        email, username, password_hash, role, status, password_changed_at, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, email);
            statement.setString(2, username);
            statement.setString(3, passwordHash);
            statement.setString(4, role.name());
            statement.setString(5, status.name());
            Timestamp timestamp = Timestamp.from(now);
            statement.setTimestamp(6, timestamp);
            statement.setTimestamp(7, timestamp);
            statement.setTimestamp(8, timestamp);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("User insert did not return an id");
        }
        return findById(key.longValue()).orElseThrow();
    }

    @Override
    public void updateLockedUntil(long userId, Instant lockedUntil, Instant now) {
        jdbcTemplate.update(
                "UPDATE user_account SET locked_until = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(lockedUntil), Timestamp.from(now), userId
        );
    }

    @Override
    public void updatePassword(long userId, String passwordHash, Instant now) {
        Timestamp timestamp = Timestamp.from(now);
        jdbcTemplate.update("""
                UPDATE user_account
                SET password_hash = ?, password_changed_at = ?, locked_until = NULL, updated_at = ?
                WHERE id = ?
                """, passwordHash, timestamp, timestamp, userId);
    }

    @Override
    public void updateStatus(long userId, UserStatus status, Instant now) {
        jdbcTemplate.update(
                "UPDATE user_account SET status = ?, updated_at = ? WHERE id = ?",
                status.name(), Timestamp.from(now), userId
        );
    }

    private UserAccount mapUser(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        Timestamp lockedUntil = resultSet.getTimestamp("locked_until");
        return new UserAccount(
                resultSet.getLong("id"),
                resultSet.getString("email"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
                UserRole.valueOf(resultSet.getString("role")),
                UserStatus.valueOf(resultSet.getString("status")),
                lockedUntil == null ? null : lockedUntil.toInstant(),
                resultSet.getTimestamp("password_changed_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }
}
