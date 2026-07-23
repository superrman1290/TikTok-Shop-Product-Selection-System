package com.tiktokinsight.admin.application;

import com.tiktokinsight.admin.domain.AdminDashboard;
import com.tiktokinsight.admin.domain.AdminUser;
import com.tiktokinsight.analysis.application.AnalysisApplicationService;
import com.tiktokinsight.analysis.domain.AnalysisJob;
import com.tiktokinsight.audit.application.AuditService;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.auth.domain.UserRole;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.api.PageResponse;
import com.tiktokinsight.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AdministrationService {
    private final NamedParameterJdbcTemplate jdbc; private final AnalysisApplicationService analysis; private final AuditService audit; private final Clock clock;
    public AdministrationService(NamedParameterJdbcTemplate jdbc, AnalysisApplicationService analysis, AuditService audit, Clock clock){this.jdbc=jdbc;this.analysis=analysis;this.audit=audit;this.clock=clock;}
    public PageResponse<AdminUser> users(int page,int size){ valid(page,size); Long total=jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM user_account",Long.class); List<AdminUser> rows=jdbc.query("SELECT id,email,username,role,status,created_at FROM user_account ORDER BY created_at DESC,id DESC LIMIT :limit OFFSET :offset",Map.of("limit",size,"offset",(page-1)*size),(rs,n)->new AdminUser(rs.getLong("id"),rs.getString("email"),rs.getString("username"),rs.getString("role"),rs.getString("status"),rs.getTimestamp("created_at").toInstant()));return new PageResponse<>(page,size,total==null?0:total,rows);}
    public AdminUser changeRole(long id,UserRole role,AccessPrincipal operator,HttpServletRequest request){ if(id==operator.userId())throw new ApiException(HttpStatus.BAD_REQUEST,ApiErrorCode.VALIDATION_FAILED); int updated=jdbc.update("UPDATE user_account SET role=:role,updated_at=UTC_TIMESTAMP(6) WHERE id=:id",Map.of("id",id,"role",role.name()));if(updated==0)throw new ApiException(HttpStatus.NOT_FOUND,ApiErrorCode.USER_NOT_FOUND);AdminUser user=user(id);audit.record(operator,"USER_ROLE_CHANGED","USER",String.valueOf(id),"SUCCESS","{\"role\":\""+role+"\"}",request);return user;}
    public AdminDashboard dashboard(){Instant since=clock.instant().minus(java.time.Duration.ofDays(7));return new AdminDashboard(count("SELECT COUNT(*) FROM user_account"),count("SELECT COUNT(*) FROM user_account WHERE status='ENABLED'"),count("SELECT COUNT(*) FROM product"),count("SELECT COUNT(*) FROM product WHERE created_at>=:since",Map.of("since",since)),count("SELECT COUNT(*) FROM import_job WHERE created_at>=:since",Map.of("since",since)),rate(since),count("SELECT COUNT(*) FROM analysis_job WHERE status IN ('PENDING','RUNNING')"),count("SELECT COUNT(*) FROM analysis_job WHERE status='FAILED'"),count("SELECT COUNT(*) FROM alert_event WHERE created_at>=:since",Map.of("since",since)));}
    public PageResponse<AnalysisJob> analysisJobs(int page,int size){valid(page,size);Long total=jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM analysis_job",Long.class);List<AnalysisJob> rows=jdbc.query("SELECT id,product_id,analysis_date,algorithm_version,status,attempt_count,next_retry_at FROM analysis_job ORDER BY created_at DESC,id DESC LIMIT :limit OFFSET :offset",Map.of("limit",size,"offset",(page-1)*size),(rs,n)->new AnalysisJob(rs.getLong("id"),rs.getLong("product_id"),rs.getObject("analysis_date",java.time.LocalDate.class),rs.getString("algorithm_version"),AnalysisJob.Status.valueOf(rs.getString("status")),rs.getInt("attempt_count"),rs.getTimestamp("next_retry_at").toInstant()));return new PageResponse<>(page,size,total==null?0:total,rows);}
    public int recalculate(AccessPrincipal operator,HttpServletRequest request){int count=analysis.scheduleAllActive();audit.record(operator,"ANALYSIS_RECALCULATE_ALL","ANALYSIS_JOB",null,"SUCCESS","{\"scheduled\":"+count+"}",request);return count;}
    private AdminUser user(long id){return jdbc.query("SELECT id,email,username,role,status,created_at FROM user_account WHERE id=:id",Map.of("id",id),(rs,n)->new AdminUser(rs.getLong("id"),rs.getString("email"),rs.getString("username"),rs.getString("role"),rs.getString("status"),rs.getTimestamp("created_at").toInstant())).stream().findFirst().orElseThrow();}
    private long count(String sql){return count(sql,Map.of());} private long count(String sql,Map<String,Object> p){Long v=jdbc.queryForObject(sql,p,Long.class);return v==null?0:v;} private double rate(Instant since){Long complete=jdbc.queryForObject("SELECT COUNT(*) FROM import_job WHERE created_at>=:since AND status IN ('SUCCESS','PARTIAL_SUCCESS','FAILED','CANCELLED')",Map.of("since",since),Long.class);if(complete==null||complete==0)return 0;Long success=jdbc.queryForObject("SELECT COUNT(*) FROM import_job WHERE created_at>=:since AND status='SUCCESS'",Map.of("since",since),Long.class);return java.math.BigDecimal.valueOf(success==null?0:success).multiply(java.math.BigDecimal.valueOf(100)).divide(java.math.BigDecimal.valueOf(complete),2,java.math.RoundingMode.HALF_UP).doubleValue();} private void valid(int page,int size){if(page<1||size<1||size>100)throw new ApiException(HttpStatus.BAD_REQUEST,ApiErrorCode.VALIDATION_FAILED);}
}
