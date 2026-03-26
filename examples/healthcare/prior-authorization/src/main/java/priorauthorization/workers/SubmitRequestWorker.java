package priorauthorization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class SubmitRequestWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pa_submit_request"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [submit] Auth request " + task.getInputData().get("authId")
                + ": " + task.getInputData().get("procedure"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("submitted", true);
        output.put("submittedAt", Instant.now().toString());
        result.setOutputData(output);
        return result;
    }
}
