package dev.micartera.domain.model;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class Category {
    private UUID id;
    private String name;
    private CategoryType type;

    public enum CategoryType {
        INCOME, EXPENSE
    }

    @JsonCreator
    public Category(
            @JsonProperty("id") UUID id,
            @JsonProperty("name") String name,
            @JsonProperty("type") CategoryType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getFormattedCategoryType() {
        return switch (this.type) {
            case INCOME -> "Доходы";
            case EXPENSE -> "Расходы";
        };
    }
}