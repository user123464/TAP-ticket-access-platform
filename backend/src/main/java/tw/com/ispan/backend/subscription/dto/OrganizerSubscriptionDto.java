package tw.com.ispan.backend.subscription.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerSubscriptionDto {
    private String subscriptionId;
    private String organizerId;
    private String planId;
    private String planName;
    private String statusCode;
    private String upgradeType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
