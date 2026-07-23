package com.tiktokinsight.datasource.administration;

import com.tiktokinsight.audit.application.AuditService;
import com.tiktokinsight.auth.application.AccessPrincipal;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DataSourceAdministrationService {
    private final DataSourceRepository repository; private final DataSourceCrypto crypto; private final AuditService audit; private final Clock clock;
    public DataSourceAdministrationService(DataSourceRepository repository, DataSourceCrypto crypto, AuditService audit, Clock clock) { this.repository=repository;this.crypto=crypto;this.audit=audit;this.clock=clock; }
    public List<DataSourceRecord> list() { return repository.findAll(); }
    public DataSourceRecord create(DataSourceRequest request, AccessPrincipal operator, HttpServletRequest servlet) { DataSourceRecord result=repository.create(record(request),crypto.encrypt(request.secret()),operator.userId(),clock.instant()); audit.record(operator,"DATA_SOURCE_CREATED","DATA_SOURCE",String.valueOf(result.id()),"SUCCESS","{\"name\":\""+result.name()+"\"}",servlet); return result; }
    public DataSourceRecord update(long id, DataSourceRequest request, AccessPrincipal operator, HttpServletRequest servlet) { if(repository.find(id).isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND,ApiErrorCode.DATA_SOURCE_NOT_FOUND); DataSourceRecord result=repository.update(id,record(request),crypto.encrypt(request.secret()),operator.userId(),clock.instant()); audit.record(operator,"DATA_SOURCE_UPDATED","DATA_SOURCE",String.valueOf(id),"SUCCESS","{\"name\":\""+result.name()+"\"}",servlet); return result; }
    public DataSourceRecord updateStatus(long id, boolean enabled, AccessPrincipal operator, HttpServletRequest servlet) { DataSourceRecord current=repository.find(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,ApiErrorCode.DATA_SOURCE_NOT_FOUND)); DataSourceRequest request=new DataSourceRequest(current.name(),current.sourceType(),current.markets(),current.endpointUrl(),null,enabled); DataSourceRecord result=repository.update(id,record(request),null,operator.userId(),clock.instant()); audit.record(operator,enabled?"DATA_SOURCE_ENABLED":"DATA_SOURCE_DISABLED","DATA_SOURCE",String.valueOf(id),"SUCCESS","{}",servlet); return result; }
    private DataSourceRecord record(DataSourceRequest r) { String hint=r.secret()==null||r.secret().isBlank()?null:"****"+r.secret().substring(Math.max(0,r.secret().length()-4)); return new DataSourceRecord(0,r.name().trim(),r.sourceType().trim().toUpperCase(),r.markets(),r.endpointUrl()==null?null:r.endpointUrl().trim(),hint,r.enabled(),null,null); }
}
