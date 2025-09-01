package daniel.nuud.notificationservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Table("notifications")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Notification {
    @Id
    private Long id;

    @Column("user_key")
    private String userKey;

    private String title;
    private String message;

    @Builder.Default
    private Level level = Level.INFO;

    @Column("read_flag")
    private boolean readFlag;

    @Column("created_at")
    private Instant createdAt;

    @Column("dedupe_key")
    private String dedupeKey;
}
