package com.tiktokinsight.audit.infrastructure;

import com.tiktokinsight.audit.domain.AuditLog;
import com.tiktokinsight.audit.domain.AuditLogRepository;
import com.tiktokinsight.common.api.PageResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuditLogRepository implements AuditLogRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcAuditLogRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void append(Long userId, String email, String action, String target, String targetId, String requestId, String ip, String result, String detail, Instant at) {
        var parameters = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
                .addValue("userId", userId).addValue("email", email).addValue("action", action)
                .addValue("target", target).addValue("targetId", targetId).addValue("requestId", requestId)
                .addValue("ip", ip).addValue("result", result).addValue("detail", detail).addValue("at", Timestamp.from(at));
        jdbc.update("INSERT INTO audit_log (operator_user_id, operator_email, action_type, target_type, target_id, request_id, request_ip, result, detail, created_at) VALUES (:userId,:email,:action,:target,:targetId,:requestId,:ip,:result,CAST(:detail AS JSON),:at)", parameters);
    }
    @Override public PageResponse<AuditLog> find(String action, String target, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1=1"); Map<String,Object> p = new HashMap<>();
        if (action != null) { where.append(" AND action_type = :action"); p.put("action", action); }
        if (target != null) { where.append(" AND target_type = :target"); p.put("target", target); }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log" + where, p, Long.class);
        p.put("limit", pageSize); p.put("offset", (page - 1) * pageSize);
        List<AuditLog> rows = jdbc.query("SELECT * FROM audit_log" + where + " ORDER BY created_at DESC,id DESC LIMIT :limit OFFSET :offset", p, (rs, n) -> new AuditLog(rs.getLong("id"), rs.getObject("operator_user_id", Long.class), rs.getString("operator_email"), rs.getString("action_type"), rs.getString("target_type"), rs.getString("target_id"), rs.getString("request_id"), rs.getString("request_ip"), rs.getString("result"), rs.getString("detail"), rs.getTimestamp("created_at").toInstant()));
        return new PageResponse<>(page, pageSize, total == null ? 0 : total, rows);
    }
}
