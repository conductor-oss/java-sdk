package payrollworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DistributeStubsWorker implements Worker {
    @Override public String getTaskDefName() { return "prl_distribute_stubs"; }
    @Override public TaskResult execute(Task task) {
        Object cntObj = task.getInputData().get("employeeCount");
        int count = cntObj instanceof Number ? ((Number)cntObj).intValue() : 0;
        System.out.println("  [distribute] Distributing " + count + " pay stubs for batch " + task.getInputData().get("processedBatchId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("distributedCount", count); r.getOutputData().put("method", "email");
        return r;
    }
}
