package dev.micartera.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Data
public class User {
    private UUID id;
    private String login;
    private String passwordHash;
    private LocalDateTime created;

    @JsonCreator
    public User(@JsonProperty("id") UUID id,
                @JsonProperty("login") String login,
                @JsonProperty("passwordHash") String passwordHash,
                @JsonProperty("created") LocalDateTime created) {
        this.id = id;
        this.login = login;
        this.passwordHash = passwordHash;
        this.created = created;
    }
}
