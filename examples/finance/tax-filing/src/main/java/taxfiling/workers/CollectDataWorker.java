package taxfiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "txf_collect_data"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [collect] Gathering tax data for " + task.getInputData().get("taxpayerId") + ", year " + task.getInputData().get("taxYear"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("grossIncome", 125000); r.getOutputData().put("deductions", 27500);
        r.getOutputData().put("credits", 4000); r.getOutputData().put("w2Count", 1); r.getOutputData().put("form1099Count", 3);
        return r;
    }
}
