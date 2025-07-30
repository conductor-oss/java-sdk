package io.orkes.conductor.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookConfigModel {

    private String name;

    private String id;

    private Map<String, Integer> receiverWorkflowNamesToVersions;

    private Map<String, Object> workflowsToStart;

    private boolean urlVerified;

    private String sourcePlatform;

    private Verifier verifier;

    private Map<String, String> headers;

    private String headerKey; // Required for signature_based verifier.

    private String secretKey;

    private String secretValue;

    private String createdBy;
    private List<Tag> tags;

    private List<WebhookExecutionHistory> webhookExecutionHistory;//TODO Remove this

    private String expression;
    private String evaluatorType;

    public enum Verifier {
        SLACK_BASED,
        SIGNATURE_BASED,
        HEADER_BASED,
        STRIPE,
        TWITTER,
        HMAC_BASED,
        SENDGRID
    }

    @JsonIgnore
    public List<String> getWorkflowNames() {
        return receiverWorkflowNamesToVersions == null ? List.of() : new ArrayList<>(receiverWorkflowNamesToVersions.keySet());
    }

    public void accept(WebhookConfigVisitor visitor) {
        visitor.visit(this);
    }

    public interface WebhookConfigVisitor {
        default void visit(WebhookConfig webhookConfig) {
        }
    }

}

