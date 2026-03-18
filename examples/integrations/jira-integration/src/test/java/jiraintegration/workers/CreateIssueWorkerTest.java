package jiraintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateIssueWorkerTest {

    @Test
    void taskDefName() {
        CreateIssueWorker worker = new CreateIssueWorker();
        assertEquals("jra_create_issue", worker.getTaskDefName());
    }

    @Test
    void throwsWithoutJiraCredentials() {
        String jiraUrl = System.getenv("JIRA_URL");
        String jiraToken = System.getenv("JIRA_API_TOKEN");
        if (jiraUrl != null && !jiraUrl.isBlank() && jiraToken != null && !jiraToken.isBlank()) {
            // Skip — real credentials are present, worker will attempt a real API call
            return;
        }

        CreateIssueWorker worker = new CreateIssueWorker();
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(Map.of(
                "project", "PROJ",
                "summary", "Test issue",
                "description", "Test description",
                "assignee", "dev@example.com")));

        assertThrows(IllegalStateException.class, () -> worker.execute(task),
                "Should throw IllegalStateException when JIRA_URL/JIRA_API_TOKEN are not set");
    }
}
