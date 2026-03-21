package reportgeneration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DistributeReportWorkerTest {

    private final DistributeReportWorker worker = new DistributeReportWorker();

    @Test
    void taskDefName() {
        assertEquals("rg_distribute_report", worker.getTaskDefName());
    }

    @Test
    void distributesToRecipients() {
        Task task = taskWith(Map.of("recipients", List.of("user@test.com", "#channel")));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("recipientCount"));
    }

    @Test
    void returnsAllDeliveredStatus() {
        Task task = taskWith(Map.of("recipients", List.of("user@test.com")));
        TaskResult result = worker.execute(task);
        assertEquals("all_delivered", result.getOutputData().get("status"));
    }

    @Test
    void handlesEmptyRecipients() {
        Task task = taskWith(Map.of("recipients", List.of()));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("recipientCount"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsDeliveryDetails() {
        Task task = taskWith(Map.of("recipients", List.of("user@test.com")));
        TaskResult result = worker.execute(task);
        List<Map<String, String>> deliveries = (List<Map<String, String>>) result.getOutputData().get("deliveries");
        assertEquals(1, deliveries.size());
        assertEquals("email", deliveries.get(0).get("channel"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void slackChannelUsesSlack() {
        Task task = taskWith(Map.of("recipients", List.of("#sales-channel")));
        TaskResult result = worker.execute(task);
        List<Map<String, String>> deliveries = (List<Map<String, String>>) result.getOutputData().get("deliveries");
        assertEquals("slack", deliveries.get(0).get("channel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
