package priorauthorization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ManualReviewWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pa_manual_review"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [manual] Auth " + task.getInputData().get("authId")
                + " sent for MANUAL REVIEW: " + task.getInputData().get("reviewNotes"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("pendingReview", true);
        output.put("assignedTo", "Medical Director");
        result.setOutputData(output);
        return result;
    }
}
