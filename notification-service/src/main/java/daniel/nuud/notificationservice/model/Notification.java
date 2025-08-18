package daniel.nuud.notificationservice.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("notifications")
@Getter
@Setter
@CompoundIndex(name = "uniq_user_dedup", def = "{'userKey':1,'dedupKey':1}", unique = true)
public class Notification {
    @Id
    private String id;

    private String userKey;
    private String title;
    private String message;
    private Level level;

    private String dedupKey;
    private Instant createdAt;
    private boolean read;

    private String source;
    private String traceId;
}
