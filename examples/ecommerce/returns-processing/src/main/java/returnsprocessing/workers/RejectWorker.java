package returnsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class RejectWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ret_reject";
    }

    @Override
    public TaskResult execute(Task task) {
        String reason = (String) task.getInputData().get("reason");
        System.out.println("  [reject] Return " + task.getInputData().get("returnId") + ": rejected (" + reason + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("rejected", true);
        output.put("reason", reason);
        result.setOutputData(output);
        return result;
    }
}
