package dev.micartera.domain.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
public class Budget {
    private UUID categoryId;
    private BigDecimal limit;
    private BigDecimal spent;
    private boolean enabled;

    @JsonCreator
    public Budget(
            @JsonProperty("categoryId") UUID categoryId,
            @JsonProperty("limit") BigDecimal limit,
            @JsonProperty("spent") BigDecimal spent,
            @JsonProperty("enabled") boolean enabled) {
        this.categoryId = categoryId;
        this.limit = limit;
        this.spent = spent;
        this.enabled = enabled;
    }
}