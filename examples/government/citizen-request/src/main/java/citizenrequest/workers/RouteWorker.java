package citizenrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RouteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ctz_route";
    }

    @Override
    public TaskResult execute(Task task) {
        String category = (String) task.getInputData().get("category");
        System.out.printf("  [route] Routed to Public Works (%s)%n", category);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("department", "Public Works");
        result.getOutputData().put("assignee", "PW-Agent-3");
        return result;
    }
}
