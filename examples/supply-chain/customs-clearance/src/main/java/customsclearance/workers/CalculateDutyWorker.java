package customsclearance.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;
public class CalculateDutyWorker implements Worker {
    @Override public String getTaskDefName() { return "cst_calculate_duty"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> goods = (List<Map<String, Object>>) task.getInputData().get("goods");
        if (goods == null) goods = List.of();
        double totalValue = goods.stream().mapToDouble(g -> g.get("value") instanceof Number ? ((Number)g.get("value")).doubleValue() : 0).sum();
        double dutyRate = 0.05;
        double dutyAmount = Math.round(totalValue * dutyRate * 100.0) / 100.0;
        System.out.println("  [duty] Goods value: $" + totalValue + ", duty (" + (dutyRate*100) + "%): $" + dutyAmount);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("dutyAmount", dutyAmount); r.getOutputData().put("dutyRate", dutyRate); return r;
    }
}
