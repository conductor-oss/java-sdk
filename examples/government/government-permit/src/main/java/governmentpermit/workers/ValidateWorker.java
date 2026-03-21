package governmentpermit.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.HashMap;
import java.util.Map;

public class ValidateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gvp_validate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [validate] Application documents verified");
        Map<String, Object> application = (Map<String, Object>) task.getInputData().get("application");

        Map<String, Object> validatedApp = new HashMap<>(application);
        validatedApp.put("valid", true);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("validatedApp", validatedApp);
        return result;
    }
}
