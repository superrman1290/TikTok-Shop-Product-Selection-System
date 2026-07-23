package com.tiktokinsight.datasource.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DataSourceRequest(@NotBlank @Size(max = 120) String name, @NotBlank String sourceType,
                                @NotEmpty List<@Pattern(regexp = "US|GB|TH|VN|PH|MY|SG|ID") String> markets,
                                @Size(max = 500) String endpointUrl, @Size(max = 4096) String secret,
                                boolean enabled) { }
