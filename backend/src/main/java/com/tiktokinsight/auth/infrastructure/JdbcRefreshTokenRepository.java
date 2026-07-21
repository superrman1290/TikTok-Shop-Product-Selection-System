package com.tiktokinsight.auth.infrastructure;

import com.tiktokinsight.auth.domain.RefreshToken;
import com.tiktokinsight.auth.domain.RefreshTokenRepository;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcRefreshTokenRepository implements RefreshTokenRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcRefreshTokenRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RefreshToken create(long userId, String tokenHash, Instant expiresAt, Instant now, String ipHash) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO user_refresh_token (
                        user_id, token_hash, expires_at, created_at, created_by_ip_hash
                    ) VALUES (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, Timestamp.from(expiresAt));
            statement.setTimestamp(4, Timestamp.from(now));
            statement.setString(5, ipHash);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Refresh token insert did not return an id");
        }
        return new RefreshToken(key.longValue(), userId, tokenHash, expiresAt, null, null, now, ipHash);
    }

    @Override
    public Optional<RefreshToken> findByHashForUpdate(String tokenHash) {
        return jdbcTemplate.query("""
                SELECT id, user_id, token_hash, expires_at, revoked_at,
                       replaced_by_token_id, created_at, created_by_ip_hash
                FROM user_refresh_token
                WHERE token_hash = ?
                FOR UPDATE
                """, this::mapToken, tokenHash).stream().findFirst();
    }

    @Override
    public void revoke(long tokenId, Instant revokedAt, Long replacementTokenId) {
        jdbcTemplate.update("""
                UPDATE user_refresh_token
                SET revoked_at = ?, replaced_by_token_id = ?
                WHERE id = ? AND revoked_at IS NULL
                """, Timestamp.from(revokedAt), replacementTokenId, tokenId);
    }

    @Override
    public void revokeAllForUser(long userId, Instant revokedAt) {
        jdbcTemplate.update("""
                UPDATE user_refresh_token
                SET revoked_at = ?
                WHERE user_id = ? AND revoked_at IS NULL
                """, Timestamp.from(revokedAt), userId);
    }

    private RefreshToken mapToken(java.sql.ResultSet resultSet, int rowNumber) throws java.sql.SQLException {
        Timestamp revokedAt = resultSet.getTimestamp("revoked_at");
        long replacementId = resultSet.getLong("replaced_by_token_id");
        return new RefreshToken(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getString("token_hash"),
                resultSet.getTimestamp("expires_at").toInstant(),
                revokedAt == null ? null : revokedAt.toInstant(),
                resultSet.wasNull() ? null : replacementId,
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getString("created_by_ip_hash")
        );
    }
}
