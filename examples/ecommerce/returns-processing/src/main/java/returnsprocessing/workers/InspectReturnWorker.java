package returnsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class InspectReturnWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ret_inspect";
    }

    @Override
    public TaskResult execute(Task task) {
        String condition = "good";
        String decision = "refund";
        double refundAmount = 129.99;

        System.out.println("  [inspect] Return " + task.getInputData().get("returnId")
                + ": condition=" + condition + ", reason=\"" + task.getInputData().get("returnReason") + "\" -> " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("condition", condition);
        output.put("decision", decision);
        output.put("refundAmount", refundAmount);
        output.put("rejectReason", null);
        result.setOutputData(output);
        return result;
    }
}
