package supplierevaluation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "spe_collect_data"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> suppliers = List.of(
            Map.of("name","AlphaParts","deliveryOnTime",0.95,"qualityRate",0.98,"priceIndex",1.0),
            Map.of("name","BetaSupply","deliveryOnTime",0.88,"qualityRate",0.96,"priceIndex",0.9),
            Map.of("name","GammaMfg","deliveryOnTime",0.92,"qualityRate",0.99,"priceIndex",1.1));
        System.out.println("  [collect] Collected data on " + suppliers.size() + " suppliers for " + task.getInputData().get("category"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("suppliers", suppliers); r.getOutputData().put("supplierCount", suppliers.size());
        return r;
    }
}
