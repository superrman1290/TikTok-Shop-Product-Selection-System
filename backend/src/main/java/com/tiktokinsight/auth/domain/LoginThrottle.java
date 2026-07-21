package com.tiktokinsight.auth.domain;

public interface LoginThrottle {
    boolean isIpBlocked(String ipHash);

    FailureResult recordFailure(String accountHash, String ipHash);

    void clearAccountFailures(String accountHash);

    record FailureResult(boolean accountThresholdReached, boolean ipThresholdReached) {
    }
}
