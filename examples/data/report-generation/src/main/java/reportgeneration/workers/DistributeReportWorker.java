package reportgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Distributes the generated report to recipients via email or Slack.
 * Input: report, recipients (list of strings)
 * Output: recipientCount, status, deliveries
 */
public class DistributeReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rg_distribute_report";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> recipients =
                (List<String>) task.getInputData().getOrDefault("recipients", List.of());

        List<Map<String, String>> deliveries = new ArrayList<>();
        Set<String> channels = new LinkedHashSet<>();
        for (String r : recipients) {
            String channel = r.contains("@") ? "email" : "slack";
            channels.add(channel);
            deliveries.add(Map.of("recipient", r, "channel", channel, "status", "delivered"));
        }

        System.out.println("  [distribute] Distributed report to " + deliveries.size()
                + " recipients via " + String.join(", ", channels));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recipientCount", deliveries.size());
        result.getOutputData().put("status", "all_delivered");
        result.getOutputData().put("deliveries", deliveries);
        return result;
    }
}
