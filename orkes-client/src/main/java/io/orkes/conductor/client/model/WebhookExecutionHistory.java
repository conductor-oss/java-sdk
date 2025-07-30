package io.orkes.conductor.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Builder
@Data
@AllArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookExecutionHistory {

    private String eventId;
    private boolean matched;
    private Set<String> workflowIds;
    private String payload;
    private long timeStamp;
}