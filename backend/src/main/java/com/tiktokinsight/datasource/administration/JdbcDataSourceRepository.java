package com.tiktokinsight.datasource.administration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDataSourceRepository implements DataSourceRepository {
    private final NamedParameterJdbcTemplate jdbc; private final ObjectMapper json;
    public JdbcDataSourceRepository(NamedParameterJdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }
    @Override public List<DataSourceRecord> findAll() { return jdbc.query("SELECT * FROM data_source ORDER BY created_at DESC,id DESC", Map.of(), (rs,n) -> map(rs)); }
    @Override public Optional<DataSourceRecord> find(long id) { return jdbc.query("SELECT * FROM data_source WHERE id=:id", Map.of("id",id), (rs,n) -> map(rs)).stream().findFirst(); }
    @Override public DataSourceRecord create(DataSourceRecord r, String secret, long userId, Instant now) { var keys = new GeneratedKeyHolder(); jdbc.update("INSERT INTO data_source (name,source_type,markets_json,endpoint_url,encrypted_secret,secret_hint,enabled,created_by,updated_by,created_at,updated_at) VALUES (:name,:type,CAST(:markets AS JSON),:url,:secret,:hint,:enabled,:userId,:userId,:now,:now)", params(r,secret,userId,now), keys, new String[]{"id"}); return find(keys.getKey().longValue()).orElseThrow(); }
    @Override public DataSourceRecord update(long id, DataSourceRecord r, String secret, long userId, Instant now) { var p=params(r,secret,userId,now).addValue("id",id); String secretSql=secret == null ? "encrypted_secret" : ":secret"; jdbc.update("UPDATE data_source SET name=:name,source_type=:type,markets_json=CAST(:markets AS JSON),endpoint_url=:url,encrypted_secret="+secretSql+",secret_hint=CASE WHEN :secret IS NULL THEN secret_hint ELSE :hint END,enabled=:enabled,updated_by=:userId,updated_at=:now WHERE id=:id",p); return find(id).orElseThrow(); }
    private MapSqlParameterSource params(DataSourceRecord r,String secret,long userId,Instant now) { try { return new MapSqlParameterSource().addValue("name",r.name()).addValue("type",r.sourceType()).addValue("markets",json.writeValueAsString(r.markets())).addValue("url",r.endpointUrl()).addValue("secret",secret).addValue("hint",r.secretHint()).addValue("enabled",r.enabled()).addValue("userId",userId).addValue("now",Timestamp.from(now)); } catch(Exception e){ throw new IllegalStateException(e); } }
    private DataSourceRecord map(java.sql.ResultSet rs) throws java.sql.SQLException { try { return new DataSourceRecord(rs.getLong("id"),rs.getString("name"),rs.getString("source_type"),json.readValue(rs.getString("markets_json"),json.getTypeFactory().constructCollectionType(List.class,String.class)),rs.getString("endpoint_url"),rs.getString("secret_hint"),rs.getBoolean("enabled"),rs.getTimestamp("created_at").toInstant(),rs.getTimestamp("updated_at").toInstant()); } catch(Exception e){ throw new java.sql.SQLException(e); } }
}
