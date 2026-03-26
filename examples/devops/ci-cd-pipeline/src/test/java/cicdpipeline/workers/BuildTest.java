package cicdpipeline.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BuildTest {

    private final Build worker = new Build();

    @Test
    void taskDefName() {
        assertEquals("cicd_build", worker.getTaskDefName());
    }

    @Test
    void failsOnMissingRepoUrl() {
        Task task = taskWith(null, "main", "abc1234");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("repoUrl"));
    }

    @Test
    void failsOnBlankRepoUrl() {
        Task task = taskWith("  ", "main", "abc1234");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
    }

    @Test
    void failsOnMissingBranch() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", null, "abc1234");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("branch"));
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", "main", "abc1234def5678");
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void outputContainsBuildId() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", "main", "abc1234def5678");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("buildId"));
        assertTrue(result.getOutputData().get("buildId").toString().startsWith("BLD-"));
    }

    @Test
    void outputContainsImageTag() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", "main", "abc1234def5678");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("imageTag"));
    }

    @Test
    void buildIdIsDeterministic() {
        Task t1 = taskWith("https://github.com/octocat/Hello-World.git", "main", "abc1234");
        Task t2 = taskWith("https://github.com/octocat/Hello-World.git", "main", "abc1234");
        TaskResult r1 = worker.execute(t1);
        TaskResult r2 = worker.execute(t2);
        assertEquals(r1.getOutputData().get("buildId"), r2.getOutputData().get("buildId"));
    }

    @Test
    void handlesNullCommitSha() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", "main", null);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void extractRepoNameHandlesGitSuffix() {
        assertEquals("Hello-World", Build.extractRepoName("https://github.com/octocat/Hello-World.git"));
    }

    @Test
    void extractRepoNameHandlesNoSuffix() {
        assertEquals("my-repo", Build.extractRepoName("https://github.com/org/my-repo"));
    }

    @Test
    void extractRepoNameHandlesNull() {
        assertEquals("app", Build.extractRepoName(null));
    }

    @Test
    void outputContainsBranch() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", "develop", "abc1234");
        TaskResult result = worker.execute(task);
        assertEquals("develop", result.getOutputData().get("branch"));
    }

    @Test
    void outputContainsDurationMs() {
        Task task = taskWith("https://github.com/octocat/Hello-World.git", "main", "abc1234");
        TaskResult result = worker.execute(task);
        assertNotNull(result.getOutputData().get("durationMs"));
        assertTrue(((Number) result.getOutputData().get("durationMs")).longValue() >= 0);
    }

    private Task taskWith(String repoUrl, String branch, String commitSha) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        Map<String, Object> input = new HashMap<>();
        input.put("repoUrl", repoUrl);
        input.put("branch", branch);
        input.put("commitSha", commitSha);
        task.setInputData(input);
        return task;
    }
}
