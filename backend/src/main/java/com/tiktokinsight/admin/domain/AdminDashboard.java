package com.tiktokinsight.admin.domain;
public record AdminDashboard(long userCount,long enabledUserCount,long productCount,long newProducts7d,long importJobs7d,double importSuccessRate,long pendingAnalysisJobs,long failedAnalysisJobs,long alerts7d) { }
