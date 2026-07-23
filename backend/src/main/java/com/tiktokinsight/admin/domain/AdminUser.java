package com.tiktokinsight.admin.domain;
import java.time.Instant;
public record AdminUser(long id,String email,String username,String role,String status,Instant createdAt) { }
