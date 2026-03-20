package npsscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CollectResponsesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nps_collect_responses";
    }

    @Override
    public TaskResult execute(Task task) {
        String campaignId = (String) task.getInputData().get("campaignId");
        String period = (String) task.getInputData().get("period");
        System.out.println("  [collect_responses] Collecting NPS responses for campaign "
                + campaignId + " period " + period);

        // Perform collected responses (scores 0-10)
        List<Map<String, Object>> responses = IntStream.range(0, 1520)
                .mapToObj(i -> Map.<String, Object>of(
                        "userId", "USR-" + (i + 1),
                        "score", i % 11
                ))
                .toList();

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("responses", responses);
        result.getOutputData().put("totalResponses", 1520);
        return result;
    }
}
