package com.tiktokinsight.alert.application;

import com.tiktokinsight.alert.domain.AlertRule;
import com.tiktokinsight.alert.domain.AlertRepository;
import com.tiktokinsight.alert.dto.AlertRuleRequest;
import com.tiktokinsight.common.api.ApiErrorCode;
import com.tiktokinsight.common.exception.ApiException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AlertRuleService {
    private final AlertRepository repository;
    private final Clock clock;
    public AlertRuleService(AlertRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    public List<AlertRule> list(long userId) { return repository.findRules(userId); }
    public AlertRule create(long userId, AlertRuleRequest input) {
        validate(input);
        if (!repository.activeProductExists(input.productId())) throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND);
        return repository.saveRule(userId, null, rule(input), clock.instant());
    }
    public AlertRule update(long userId, long ruleId, AlertRuleRequest input) {
        validate(input); repository.findRule(userId, ruleId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.ALERT_RULE_NOT_FOUND));
        if (!repository.activeProductExists(input.productId())) throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.PRODUCT_NOT_FOUND);
        return repository.saveRule(userId, ruleId, rule(input), clock.instant());
    }
    public void delete(long userId, long ruleId) {
        if (!repository.deleteRule(userId, ruleId)) throw new ApiException(HttpStatus.NOT_FOUND, ApiErrorCode.ALERT_RULE_NOT_FOUND);
    }
    private AlertRule rule(AlertRuleRequest input) { return new AlertRule(0, input.productId(), input.salesGrowth7dThreshold(), input.priceDrop7dThreshold(), input.competitionScoreIncreaseThreshold(), input.selectionScoreDropThreshold(), input.enabled(), null, null); }
    private void validate(AlertRuleRequest input) {
        if (input.salesGrowth7dThreshold() == null && input.priceDrop7dThreshold() == null && input.competitionScoreIncreaseThreshold() == null && input.selectionScoreDropThreshold() == null) throw new ApiException(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_FAILED);
    }
}
