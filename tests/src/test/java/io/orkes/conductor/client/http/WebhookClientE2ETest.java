/*
 * Copyright 2025 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.orkes.conductor.client.http;

import com.google.common.util.concurrent.Uninterruptibles;
import com.netflix.conductor.client.http.ConductorClientResponse;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import io.orkes.conductor.client.OrkesClients;
import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.WebhookConfig;
import io.orkes.conductor.client.util.ClientTestUtil;
import io.orkes.conductor.client.util.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E tests for OrkesWebhookClient demonstrating webhook configuration management
 */
public class WebhookClientE2ETest {

    private static OrkesWebhookClient webhookClient;
    private static OrkesClients orkesClients;
    private String testWebhookId;

    @BeforeAll
    public static void setup() {
        orkesClients = ClientTestUtil.getOrkesClients();
        webhookClient = orkesClients.getWebhookClient();
    }

    @AfterEach
    public void cleanup() {
        // Clean up any created webhooks
        if (testWebhookId != null) {
            try {
                webhookClient.deleteWebhook(testWebhookId);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
            testWebhookId = null;
        }
    }

    @Test
    public void testCreateWebhook() {
        // Arrange
        String webhookName = "e2e_test_webhook_" + System.currentTimeMillis();

        // Create a simple workflow to trigger from webhook
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .workflowsToStart(Map.of(workflowName, 1))
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .secretValue("slack_secret_value")
                .build();

        // Act
        ConductorClientResponse<WebhookConfig> response = webhookClient.createWebhook(webhookConfig);

        // Assert
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getData(), "Response data should not be null");

        WebhookConfig createdWebhook = response.getData();
        assertNotNull(createdWebhook.getId(), "Webhook ID should be generated");
        assertEquals(webhookName, createdWebhook.getName(), "Webhook name should match");
        assertEquals("SLACK", createdWebhook.getSourcePlatform(), "Source platform should match");
        assertEquals(WebhookConfig.Verifier.SLACK_BASED, createdWebhook.getVerifier(), "Verifier should match");
        assertNotNull(createdWebhook.getReceiverWorkflowNamesToVersions(), "Receiver workflows should not be null");
        assertTrue(createdWebhook.getReceiverWorkflowNamesToVersions().containsKey(workflowName),
                "Should contain target workflow");

        // Store for cleanup
        testWebhookId = createdWebhook.getId();

        System.out.println("✅ Successfully created webhook with ID: " + testWebhookId);
    }

    @Test
    public void testCreateWebhookWithSignatureVerifier() {
        // Arrange
        String webhookName = "e2e_signature_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("GITHUB")
                .verifier(WebhookConfig.Verifier.SIGNATURE_BASED)
                .headerKey("X-Hub-Signature-256")
                .secretKey("webhook_secret")
                .secretValue("my_secret_value")
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        // Act
        ConductorClientResponse<WebhookConfig> response = webhookClient.createWebhook(webhookConfig);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());

        WebhookConfig createdWebhook = response.getData();
        assertNotNull(createdWebhook.getId());
        assertEquals(webhookName, createdWebhook.getName());
        assertEquals("GITHUB", createdWebhook.getSourcePlatform());
        assertEquals(WebhookConfig.Verifier.SIGNATURE_BASED, createdWebhook.getVerifier());
        assertEquals("X-Hub-Signature-256", createdWebhook.getHeaderKey());
        assertEquals("webhook_secret", createdWebhook.getSecretKey());

        // Store for cleanup
        testWebhookId = createdWebhook.getId();

        System.out.println("✅ Successfully created signature-based webhook with ID: " + testWebhookId);
    }

    @Test
    public void testCreateWebhookWithHeaderBasedVerifier() {
        // Arrange
        String webhookName = "e2e_header_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("CUSTOM")
                .verifier(WebhookConfig.Verifier.HEADER_BASED)
                .headerKey("X-Custom-Token")
                .secretValue("custom_token_123")
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .headers(Map.of(
                        "Content-Type", "application/json",
                        "X-Custom-Header", "webhook-integration"
                ))
                .build();

        // Act
        ConductorClientResponse<WebhookConfig> response = webhookClient.createWebhook(webhookConfig);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());

        WebhookConfig createdWebhook = response.getData();
        assertNotNull(createdWebhook.getId());
        assertEquals(webhookName, createdWebhook.getName());
        assertEquals("CUSTOM", createdWebhook.getSourcePlatform());
        assertEquals(WebhookConfig.Verifier.HEADER_BASED, createdWebhook.getVerifier());
        assertEquals("X-Custom-Token", createdWebhook.getHeaderKey());
        assertEquals("custom_token_123", createdWebhook.getSecretValue());

        // Store for cleanup
        testWebhookId = createdWebhook.getId();

        System.out.println("✅ Successfully created header-based webhook with ID: " + testWebhookId);
    }

    @Test
    public void testCreateWebhookWithMultipleWorkflows() {
        // Arrange
        String webhookName = "e2e_multi_webhook_" + System.currentTimeMillis();
        String workflow1Name = "e2e_webhook_target1_" + System.currentTimeMillis();
        String workflow2Name = "e2e_webhook_target2_" + System.currentTimeMillis();

        createTestWorkflow(workflow1Name);
        createTestWorkflow(workflow2Name);

        Map<String, Integer> workflowsToVersions = new HashMap<>();
        workflowsToVersions.put(workflow1Name, 1);
        workflowsToVersions.put(workflow2Name, 1);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("STRIPE")
                .verifier(WebhookConfig.Verifier.STRIPE)
                .secretValue("whsec_test_secret")
                .receiverWorkflowNamesToVersions(workflowsToVersions)
                .build();

        // Act
        ConductorClientResponse<WebhookConfig> response = webhookClient.createWebhook(webhookConfig);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());

        WebhookConfig createdWebhook = response.getData();
        assertNotNull(createdWebhook.getId());
        assertEquals(webhookName, createdWebhook.getName());
        assertEquals("STRIPE", createdWebhook.getSourcePlatform());
        assertEquals(WebhookConfig.Verifier.STRIPE, createdWebhook.getVerifier());

        Map<String, Integer> receiverWorkflows = createdWebhook.getReceiverWorkflowNamesToVersions();
        assertNotNull(receiverWorkflows);
        assertEquals(2, receiverWorkflows.size());
        assertTrue(receiverWorkflows.containsKey(workflow1Name));
        assertTrue(receiverWorkflows.containsKey(workflow2Name));

        // Store for cleanup
        testWebhookId = createdWebhook.getId();

        System.out.println("✅ Successfully created multi-workflow webhook with ID: " + testWebhookId);
    }

    @Test
    public void testCreateWebhookValidation() {
        // Test creating webhook with missing required fields
        WebhookConfig invalidConfig = WebhookConfig.builder()
                .name("") // Empty name should fail
                .build();

        try {
            ConductorClientResponse<WebhookConfig> response = webhookClient.createWebhook(invalidConfig);
            // If this doesn't throw an exception, check the response for errors
            if (response != null && response.getData() != null) {
                fail("Expected webhook creation to fail with invalid config");
            }
        } catch (Exception e) {
            // Expected - webhook creation should fail with invalid config
            System.out.println("✅ Webhook creation properly rejected invalid config: " + e.getMessage());
        }
    }

    @Test
    public void testGetWebhook() {
        // Arrange - First create a webhook
        String webhookName = "e2e_get_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> createResponse = webhookClient.createWebhook(webhookConfig);
        assertNotNull(createResponse.getData());
        testWebhookId = createResponse.getData().getId();

        // Act
        ConductorClientResponse<WebhookConfig> response = webhookClient.getWebhook(testWebhookId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());

        WebhookConfig retrievedWebhook = response.getData();
        assertEquals(testWebhookId, retrievedWebhook.getId());
        assertEquals(webhookName, retrievedWebhook.getName());
        assertEquals("SLACK", retrievedWebhook.getSourcePlatform());
        assertEquals(WebhookConfig.Verifier.SLACK_BASED, retrievedWebhook.getVerifier());

        System.out.println("✅ Successfully retrieved webhook: " + testWebhookId);
    }

    @Test
    public void testUpdateWebhook() {
        // Arrange - First create a webhook
        String webhookName = "e2e_update_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> createResponse = webhookClient.createWebhook(webhookConfig);
        assertNotNull(createResponse.getData());
        testWebhookId = createResponse.getData().getId();

        // Act - Update the webhook
        WebhookConfig updatedConfig = WebhookConfig.builder()
                .name(webhookName + "_updated")
                .sourcePlatform("GITHUB")
                .verifier(WebhookConfig.Verifier.SIGNATURE_BASED)
                .headerKey("X-Hub-Signature-256")
                .secretKey("updated_secret")
                .secretValue("updated_secret_value")
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> updateResponse = webhookClient.updateWebhook(testWebhookId, updatedConfig);

        // Assert
        assertNotNull(updateResponse);
        assertNotNull(updateResponse.getData());

        WebhookConfig updated = updateResponse.getData();
        assertEquals(testWebhookId, updated.getId());
        assertEquals(webhookName + "_updated", updated.getName());
        assertEquals("GITHUB", updated.getSourcePlatform());
        assertEquals(WebhookConfig.Verifier.SIGNATURE_BASED, updated.getVerifier());
        assertEquals("X-Hub-Signature-256", updated.getHeaderKey());
        assertEquals("updated_secret", updated.getSecretKey());

        System.out.println("✅ Successfully updated webhook: " + testWebhookId);
    }

    @Test
    public void testGetAllWebhooks() {
        // Arrange - Create a test webhook
        String webhookName = "e2e_list_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> createResponse = webhookClient.createWebhook(webhookConfig);
        assertNotNull(createResponse.getData());
        testWebhookId = createResponse.getData().getId();

        // Act
        ConductorClientResponse<List<WebhookConfig>> response = webhookClient.getAllWebhooks();

        // Assert
        assertNotNull(response);
        assertNotNull(response.getData());

        List<WebhookConfig> webhooks = response.getData();
        assertTrue(webhooks.size() > 0, "Should have at least one webhook");

        // Verify our test webhook is in the list
        boolean found = webhooks.stream()
                .anyMatch(wh -> testWebhookId.equals(wh.getId()));
        assertTrue(found, "Should find our test webhook in the list");

        System.out.println("✅ Successfully retrieved " + webhooks.size() + " webhooks");
    }

    @Test
    public void testWebhookTagOperations() {
        // Arrange - Create a webhook
        String webhookName = "e2e_tag_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> createResponse = webhookClient.createWebhook(webhookConfig);
        assertNotNull(createResponse.getData());
        testWebhookId = createResponse.getData().getId();

        // Create tags
        Tag tag1 = new Tag();
        tag1.setKey("environment");
        tag1.setValue("testing");

        Tag tag2 = new Tag();
        tag2.setKey("team");
        tag2.setValue("e2e");

        List<Tag> tags = List.of(tag1, tag2);

        // Act - Put tags
        ConductorClientResponse<Void> putResponse = webhookClient.putTagsForWebhook(testWebhookId, tags);
        assertNotNull(putResponse);

        // Act - Get tags
        ConductorClientResponse<List<Tag>> getResponse = webhookClient.getTagsForWebhook(testWebhookId);
        assertNotNull(getResponse);
        assertNotNull(getResponse.getData());

        List<Tag> retrievedTags = getResponse.getData();
        assertEquals(2, retrievedTags.size());

        // Verify tags
        boolean hasEnvTag = retrievedTags.stream()
                .anyMatch(tag -> "environment".equals(tag.getKey()) && "testing".equals(tag.getValue()));
        boolean hasTeamTag = retrievedTags.stream()
                .anyMatch(tag -> "team".equals(tag.getKey()) && "e2e".equals(tag.getValue()));

        assertTrue(hasEnvTag, "Should have environment tag");
        assertTrue(hasTeamTag, "Should have team tag");

        System.out.println("✅ Successfully tested webhook tag operations");
    }

    @Test
    public void testDeleteWebhook() {
        // Arrange - Create a webhook
        String webhookName = "e2e_delete_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> createResponse = webhookClient.createWebhook(webhookConfig);
        assertNotNull(createResponse.getData());
        String webhookId = createResponse.getData().getId();

        // Verify webhook exists
        ConductorClientResponse<WebhookConfig> getResponse = webhookClient.getWebhook(webhookId);
        assertNotNull(getResponse.getData());

        // Act - Delete webhook
        ConductorClientResponse<Void> deleteResponse = webhookClient.deleteWebhook(webhookId);
        assertNotNull(deleteResponse);

        // Assert - Verify webhook is deleted
        try {
            ConductorClientResponse<WebhookConfig> getAfterDeleteResponse = webhookClient.getWebhook(webhookId);
            // If we get here, the webhook might still exist (depending on API behavior)
            // or the API might return an empty response
            if (getAfterDeleteResponse != null && getAfterDeleteResponse.getData() != null) {
                fail("Webhook should have been deleted");
            }
        } catch (Exception e) {
            // Expected - webhook should not be found after deletion
            System.out.println("✅ Webhook properly deleted, get request failed as expected: " + e.getMessage());
        }

        // Clear testWebhookId since it's already deleted
        testWebhookId = null;

        System.out.println("✅ Successfully deleted webhook: " + webhookId);
    }

    @Test
    public void testWebhookConvenienceMethods() {
        // Arrange - Create a webhook
        String webhookName = "e2e_convenience_webhook_" + System.currentTimeMillis();
        String workflowName = "e2e_webhook_target_" + System.currentTimeMillis();
        createTestWorkflow(workflowName);

        WebhookConfig webhookConfig = WebhookConfig.builder()
                .name(webhookName)
                .sourcePlatform("SLACK")
                .verifier(WebhookConfig.Verifier.SLACK_BASED)
                .receiverWorkflowNamesToVersions(Map.of(workflowName, 1))
                .build();

        ConductorClientResponse<WebhookConfig> createResponse = webhookClient.createWebhook(webhookConfig);
        assertNotNull(createResponse.getData());
        testWebhookId = createResponse.getData().getId();

        // Test convenience methods

        // Test getWebhookConfig
        WebhookConfig retrievedConfig = webhookClient.getWebhookConfig(testWebhookId);
        assertNotNull(retrievedConfig);
        assertEquals(testWebhookId, retrievedConfig.getId());
        assertEquals(webhookName, retrievedConfig.getName());

        // Test getWebhookConfigs
        List<WebhookConfig> allConfigs = webhookClient.getWebhookConfigs();
        assertNotNull(allConfigs);
        assertTrue(allConfigs.size() > 0);

        boolean found = allConfigs.stream()
                .anyMatch(wh -> testWebhookId.equals(wh.getId()));
        assertTrue(found, "Should find our test webhook in the list");

        // Test webhookExists
        boolean exists = webhookClient.webhookExists(testWebhookId);
        assertTrue(exists, "Webhook should exist");

        boolean nonExistentExists = webhookClient.webhookExists("non-existent-id");
        assertFalse(nonExistentExists, "Non-existent webhook should not exist");

        System.out.println("✅ Successfully tested convenience methods");
    }

    @Test
    public void testWebhookErrorHandling() {
        // Test error scenarios

        // Test getting non-existent webhook
        try {
            ConductorClientResponse<WebhookConfig> response = webhookClient.getWebhook("non-existent-webhook-id");
            if (response != null && response.getData() != null) {
                fail("Should not be able to get non-existent webhook");
            }
        } catch (Exception e) {
            System.out.println("✅ Properly handled getting non-existent webhook: " + e.getMessage());
        }

        // Test updating non-existent webhook
        try {
            WebhookConfig updateConfig = WebhookConfig.builder()
                    .name("update_test")
                    .sourcePlatform("SLACK")
                    .verifier(WebhookConfig.Verifier.SLACK_BASED)
                    .build();

            ConductorClientResponse<WebhookConfig> response = webhookClient.updateWebhook("non-existent-webhook-id", updateConfig);
            if (response != null && response.getData() != null) {
                fail("Should not be able to update non-existent webhook");
            }
        } catch (Exception e) {
            System.out.println("✅ Properly handled updating non-existent webhook: " + e.getMessage());
        }

        // Test deleting non-existent webhook
        try {
            ConductorClientResponse<Void> response = webhookClient.deleteWebhook("non-existent-webhook-id");
            // Some APIs might return success even for non-existent resources
            System.out.println("✅ Delete non-existent webhook handled gracefully");
        } catch (Exception e) {
            System.out.println("✅ Properly handled deleting non-existent webhook: " + e.getMessage());
        }
    }

    /**
     * Helper method to create a simple test workflow for webhook targets
     */
    private void createTestWorkflow(String workflowName) {
        try {
            // Create a simple task definition
            String taskName = workflowName + "_task";
            TaskDef taskDef = new TaskDef(taskName);
            taskDef.setRetryCount(1);
            taskDef.setOwnerEmail("test@orkes.io");

            TestUtil.retryMethodCall(() ->
                    orkesClients.getMetadataClient().registerTaskDefs(List.of(taskDef)));

            // Create workflow definition
            WorkflowDef workflowDef = new WorkflowDef();
            workflowDef.setName(workflowName);
            workflowDef.setVersion(1);
            workflowDef.setOwnerEmail("test@orkes.io");

            // Add a simple task to the workflow
            WorkflowTask workflowTask = new WorkflowTask();
            workflowTask.setName(taskName);
            workflowTask.setTaskReferenceName(taskName + "_ref");
            workflowTask.setType("SIMPLE");

            workflowDef.setTasks(List.of(workflowTask));

            TestUtil.retryMethodCall(() ->
                    orkesClients.getMetadataClient().registerWorkflowDef(workflowDef));

            // Wait a bit for registration to complete
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Failed to create test workflow: " + workflowName + " - " + e.getMessage());
            // Don't fail the test if workflow creation fails, as this is just setup
        }
    }
}
