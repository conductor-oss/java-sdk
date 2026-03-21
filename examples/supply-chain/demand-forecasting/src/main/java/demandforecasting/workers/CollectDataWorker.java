package demandforecasting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class CollectDataWorker implements Worker {
    @Override public String getTaskDefName() { return "df_collect_data"; }
    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> data = List.of(
            Map.of("month","Jan","units",1200), Map.of("month","Feb","units",1500),
            Map.of("month","Mar","units",1500), Map.of("month","Apr","units",1420),
            Map.of("month","May","units",1680), Map.of("month","Jun","units",1800));
        System.out.println("  [collect] " + data.size() + " months of data for " + task.getInputData().get("productCategory") + " in " + task.getInputData().get("region"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("historicalData", data); r.getOutputData().put("dataPointCount", data.size());
        return r;
    }
}
