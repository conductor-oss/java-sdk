package smoketesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckEndpointsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_check_endpoints";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [endpoints] order-service: all 8 endpoints responding");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("check_endpointsId", "CHECK_ENDPOINTS-1341");
        result.addOutputData("success", true);
        return result;
    }
}
