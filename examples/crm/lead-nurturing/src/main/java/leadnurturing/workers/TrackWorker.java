package leadnurturing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class TrackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nur_track";
    }

    @Override
    public TaskResult execute(Task task) {
        String deliveryId = (String) task.getInputData().get("deliveryId");
        System.out.println("  [track] Engagement tracked for delivery " + deliveryId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("engagement", Map.of(
                "opened", true,
                "clicked", true,
                "timeOnPage", 45,
                "pagesViewed", 3
        ));
        return result;
    }
}
