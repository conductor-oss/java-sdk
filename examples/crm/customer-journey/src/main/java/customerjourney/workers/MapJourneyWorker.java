package customerjourney.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Maps customer touchpoints into journey stages.
 * Input: touchpoints
 * Output: journeyMap, stageCount
 */
public class MapJourneyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cjy_map_journey";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [map] Journey mapped across 5 stages");

        Map<String, Integer> journeyMap = Map.of(
                "awareness", 3,
                "consideration", 4,
                "decision", 2,
                "purchase", 1,
                "retention", 4
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("journeyMap", journeyMap);
        result.getOutputData().put("stageCount", 5);
        return result;
    }
}
