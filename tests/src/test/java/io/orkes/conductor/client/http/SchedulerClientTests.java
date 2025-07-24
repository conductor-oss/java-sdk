/*
 * Copyright 2022 Conductor Authors.
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

import com.netflix.conductor.common.model.BulkResponse;
import io.orkes.conductor.client.SchedulerClient;
import io.orkes.conductor.client.model.SaveScheduleRequest;
import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.WorkflowSchedule;
import io.orkes.conductor.client.util.ClientTestUtil;
import io.orkes.conductor.client.util.Commons;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchedulerClientTests {
    private static final String SCHEDULE_1 = "bulk_test_schedule_1_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SCHEDULE_2 = "bulk_test_schedule_2_" + UUID.randomUUID().toString().replace("-", "");
    private static final String SCHEDULE_3 = "bulk_test_schedule_3_" + UUID.randomUUID().toString().replace("-", "");
    private static final String NON_EXISTENT_SCHEDULE = "non_existent_schedule_" + UUID.randomUUID().toString().replace("-", "");

    private static final String CRON_EXPRESSION_1 = "0 * * * * *";
    private static final String CRON_EXPRESSION_2 = "0 0 */2 * * *"; // Every 2 hours
    private static final String CRON_EXPRESSION_3 = "0 0 0 * * *";   // Daily at midnight

    private final SchedulerClient schedulerClient = ClientTestUtil.getOrkesClients().getSchedulerClient();

    @BeforeEach
    void beforeEach() {
        schedulerClient.deleteSchedule(SCHEDULE_1);
    }

    @AfterEach
    void afterEach() {
        schedulerClient.deleteSchedule(SCHEDULE_1);
    }

    @Test
    void testMethods() {
        schedulerClient.deleteSchedule(SCHEDULE_1);
        Assertions.assertTrue(schedulerClient.getNextFewSchedules(CRON_EXPRESSION_1, 0L, 0L, 0).isEmpty());
        schedulerClient.saveSchedule(getSaveScheduleRequest());
        Assertions.assertTrue(schedulerClient.getAllSchedules(Commons.WORKFLOW_NAME).size() > 0);
        WorkflowSchedule workflowSchedule = schedulerClient.getSchedule(SCHEDULE_1);
        assertEquals(SCHEDULE_1, workflowSchedule.getName());
        assertEquals(CRON_EXPRESSION_1, workflowSchedule.getCronExpression());
        Assertions.assertFalse(schedulerClient.search(0, 10, "ASC", "*", "").getResults().isEmpty());
        schedulerClient.setSchedulerTags(getTagObject(), SCHEDULE_1);
        assertEquals(getTagObject(), schedulerClient.getSchedulerTags(SCHEDULE_1));
        schedulerClient.deleteSchedulerTags(getTagObject(), SCHEDULE_1);
        assertEquals(0, schedulerClient.getSchedulerTags(SCHEDULE_1).size());
        schedulerClient.pauseSchedule(SCHEDULE_1);
        workflowSchedule = schedulerClient.getSchedule(SCHEDULE_1);
        Assertions.assertTrue(workflowSchedule.isPaused());
        schedulerClient.resumeSchedule(SCHEDULE_1);
        workflowSchedule = schedulerClient.getSchedule(SCHEDULE_1);
        Assertions.assertFalse(workflowSchedule.isPaused());
        schedulerClient.deleteSchedule(SCHEDULE_1);
    }

    @Test
    void testDebugMethods() {
        schedulerClient.pauseAllSchedules();
        schedulerClient.resumeAllSchedules();
        schedulerClient.requeueAllExecutionRecords();
    }

    @Test
    @DisplayName("It should set the timezone to Europe/Madrid")
    void testTimeZoneId() {
        var schedule = new SaveScheduleRequest()
                .name(SCHEDULE_1)
                .cronExpression(CRON_EXPRESSION_1)
                .startWorkflowRequest(Commons.getStartWorkflowRequest())
                .zoneId("Europe/Madrid");
        schedulerClient.saveSchedule(schedule);
        var savedSchedule = schedulerClient.getSchedule(SCHEDULE_1);
        assertEquals("Europe/Madrid", savedSchedule.getZoneId());
    }

    @Test
    @DisplayName("Test bulk pause and resume functionality with valid and invalid schedules")
    void testBulkPauseResumeSchedules() {
        try {
            // Step 1: Create 3 schedules
            createTestSchedule(SCHEDULE_1, CRON_EXPRESSION_1);
            createTestSchedule(SCHEDULE_2, CRON_EXPRESSION_2);
            createTestSchedule(SCHEDULE_3, CRON_EXPRESSION_3);

            // Verify all schedules are created and active
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_1).isPaused(), "Schedule 1 should be active");
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_2).isPaused(), "Schedule 2 should be active");
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_3).isPaused(), "Schedule 3 should be active");

            // Step 2: Bulk pause all 3 existing schedules
            List<String> validSchedules = List.of(SCHEDULE_1, SCHEDULE_2, SCHEDULE_3);
            BulkResponse pauseResponse = schedulerClient.pauseSchedulers(validSchedules);

            // Verify pause response
            Assertions.assertNotNull(pauseResponse, "Pause response should not be null");
            Assertions.assertEquals(3, pauseResponse.getBulkSuccessfulResults().size(), "All 3 schedules should be paused successfully");
            Assertions.assertEquals(0, pauseResponse.getBulkErrorResults().size(), "No errors expected for valid schedules");

            // Verify all schedules are now paused
            Assertions.assertTrue(schedulerClient.getSchedule(SCHEDULE_1).isPaused(), "Schedule 1 should be paused");
            Assertions.assertTrue(schedulerClient.getSchedule(SCHEDULE_2).isPaused(), "Schedule 2 should be paused");
            Assertions.assertTrue(schedulerClient.getSchedule(SCHEDULE_3).isPaused(), "Schedule 3 should be paused");

            // Step 3: Bulk resume all 3 existing schedules
            BulkResponse resumeResponse = schedulerClient.resumeSchedulers(validSchedules);

            // Verify resume response
            Assertions.assertNotNull(resumeResponse, "Resume response should not be null");
            Assertions.assertEquals(3, resumeResponse.getBulkSuccessfulResults().size(), "All 3 schedules should be resumed successfully");
            Assertions.assertEquals(0, resumeResponse.getBulkErrorResults().size(), "No errors expected for valid schedules");

            // Verify all schedules are now active
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_1).isPaused(), "Schedule 1 should be active");
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_2).isPaused(), "Schedule 2 should be active");
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_3).isPaused(), "Schedule 3 should be active");

            // Step 4: Bulk pause with mixed valid/invalid schedules (2 valid + 1 non-existent)
            List<String> mixedSchedules = List.of(SCHEDULE_1, SCHEDULE_2, NON_EXISTENT_SCHEDULE);
            BulkResponse mixedPauseResponse = schedulerClient.pauseSchedulers(mixedSchedules);

            // Verify mixed pause response
            Assertions.assertNotNull(mixedPauseResponse, "Mixed pause response should not be null");
            Assertions.assertEquals(2, mixedPauseResponse.getBulkSuccessfulResults().size(), "2 valid schedules should be paused");
            Assertions.assertEquals(1, mixedPauseResponse.getBulkErrorResults().size(), "1 invalid schedule should cause error");

            // Verify valid schedules are paused
            Assertions.assertTrue(schedulerClient.getSchedule(SCHEDULE_1).isPaused(), "Schedule 1 should be paused");
            Assertions.assertTrue(schedulerClient.getSchedule(SCHEDULE_2).isPaused(), "Schedule 2 should be paused");

            // Step 5: Bulk resume with mixed valid/invalid schedules (2 valid + 1 non-existent)
            BulkResponse mixedResumeResponse = schedulerClient.resumeSchedulers(mixedSchedules);

            // Verify mixed resume response
            Assertions.assertNotNull(mixedResumeResponse, "Mixed resume response should not be null");
            Assertions.assertEquals(2, mixedResumeResponse.getBulkSuccessfulResults().size(), "2 valid schedules should be resumed");
            Assertions.assertEquals(1, mixedResumeResponse.getBulkErrorResults().size(), "1 invalid schedule should cause error");

            // Verify valid schedules are active
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_1).isPaused(), "Schedule 1 should be active");
            Assertions.assertFalse(schedulerClient.getSchedule(SCHEDULE_2).isPaused(), "Schedule 2 should be active");

            System.out.println("All bulk pause/resume tests completed successfully");

        } finally {
            // Cleanup - delete all test schedules
            try {
                schedulerClient.deleteSchedule(SCHEDULE_1);
            } catch (Exception ignored) {
            }
            try {
                schedulerClient.deleteSchedule(SCHEDULE_2);
            } catch (Exception ignored) {
            }
            try {
                schedulerClient.deleteSchedule(SCHEDULE_3);
            } catch (Exception ignored) {
            }
        }
    }

    // Helper method to create a test schedule
    private void createTestSchedule(String scheduleName, String cronExpression) {
        SaveScheduleRequest request = new SaveScheduleRequest()
                .name(scheduleName)
                .cronExpression(cronExpression)
                .startWorkflowRequest(Commons.getStartWorkflowRequest());
        schedulerClient.saveSchedule(request);
    }

    private SaveScheduleRequest getSaveScheduleRequest() {
        return new SaveScheduleRequest()
                .name(SCHEDULE_1)
                .cronExpression(CRON_EXPRESSION_1)
                .startWorkflowRequest(Commons.getStartWorkflowRequest());
    }

    private List<TagObject> getTagObject() {
        TagObject tagObject = new TagObject();
        tagObject.setKey("department");
        tagObject.setValue("accounts");
        return List.of(tagObject);
    }
}
