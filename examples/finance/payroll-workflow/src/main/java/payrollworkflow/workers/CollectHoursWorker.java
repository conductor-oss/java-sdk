package payrollworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class CollectHoursWorker implements Worker {
    @Override public String getTaskDefName() { return "prl_collect_hours"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [hours] Collecting hours for period " + task.getInputData().get("payPeriodId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("employeeRecords", List.of(Map.of("empId","E-001","regularHours",80,"overtimeHours",5,"rate",45.00),Map.of("empId","E-002","regularHours",80,"overtimeHours",0,"rate",55.00),Map.of("empId","E-003","regularHours",76,"overtimeHours",8,"rate",40.00)));
        r.getOutputData().put("totalEmployees", 3);
        return r;
    }
}
