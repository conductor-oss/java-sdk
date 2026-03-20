package slackapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
class PostSlackWorkerTest {

    @Test
    void taskDefName() {
        PostSlackWorker worker = new PostSlackWorker();
        assertEquals("sa_post_slack", worker.getTaskDefName());
    }

    @Test
    void returnsPostedTrue() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "channel", "#approvals",
                "requestor", "alice@example.com",
                "reason", "Need access"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("posted"));
    }

    @Test
    void generatesSlackPayloadWithChannel() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "channel", "#ops-approvals",
                "requestor", "bob@example.com",
                "reason", "Deploy to prod"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        assertNotNull(payload);
        assertEquals("#ops-approvals", payload.get("channel"));
    }

    @Test
    void slackPayloadContainsBlocks() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "channel", "#approvals",
                "requestor", "alice@example.com",
                "reason", "Need access"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");
        assertNotNull(blocks);
        assertEquals(3, blocks.size());
    }

    @Test
    void blocksContainHeaderSectionAndActions() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "channel", "#approvals",
                "requestor", "alice@example.com",
                "reason", "Need access"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");

        assertEquals("header", blocks.get(0).get("type"));
        assertEquals("section", blocks.get(1).get("type"));
        assertEquals("actions", blocks.get(2).get("type"));
    }

    @Test
    void actionsBlockContainsApproveAndRejectButtons() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "channel", "#approvals",
                "requestor", "alice@example.com",
                "reason", "Need access"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");
        Map<String, Object> actionsBlock = blocks.get(2);
        List<Map<String, Object>> elements = (List<Map<String, Object>>) actionsBlock.get("elements");

        assertEquals(2, elements.size());
        assertEquals("approve_action", elements.get(0).get("action_id"));
        assertEquals("approved", elements.get(0).get("value"));
        assertEquals("reject_action", elements.get(1).get("action_id"));
        assertEquals("rejected", elements.get(1).get("value"));
    }

    @Test
    void sectionContainsRequestorAndReason() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "channel", "#approvals",
                "requestor", "carol@example.com",
                "reason", "Database migration"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");
        Map<String, Object> section = blocks.get(1);
        Map<String, Object> text = (Map<String, Object>) section.get("text");
        String textContent = (String) text.get("text");

        assertTrue(textContent.contains("carol@example.com"));
        assertTrue(textContent.contains("Database migration"));
    }

    @Test
    void usesDefaultChannelWhenNotProvided() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>(Map.of(
                "requestor", "alice@example.com",
                "reason", "Need access"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        assertEquals("#approvals", payload.get("channel"));
    }

    @Test
    void usesDefaultRequestorWhenNotProvided() {
        PostSlackWorker worker = new PostSlackWorker();
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        Map<String, Object> payload = (Map<String, Object>) result.getOutputData().get("slackPayload");
        List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");
        Map<String, Object> section = blocks.get(1);
        Map<String, Object> text = (Map<String, Object>) section.get("text");
        String textContent = (String) text.get("text");

        assertTrue(textContent.contains("unknown"));
        assertTrue(textContent.contains("No reason provided"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
