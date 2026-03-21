package streamprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AggregateWindowsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_aggregate_windows";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> windows = (List<Map<String, Object>>) task.getInputData().get("windows");
        if (windows == null) windows = List.of();

        List<Map<String, Object>> aggregates = new ArrayList<>();
        for (Map<String, Object> w : windows) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) w.get("events");
            if (events == null) events = List.of();

            double sum = 0;
            double max = Double.NEGATIVE_INFINITY;
            for (Map<String, Object> e : events) {
                Object valObj = e.get("value");
                double val = 0;
                if (valObj instanceof Number) val = ((Number) valObj).doubleValue();
                sum += val;
                if (val > max) max = val;
            }
            int count = events.size();
            double avg = count > 0 ? sum / count : 0;

            Map<String, Object> agg = new LinkedHashMap<>();
            agg.put("windowStart", w.get("windowStart"));
            agg.put("count", count);
            agg.put("sum", Math.round(sum * 100.0) / 100.0);
            agg.put("avg", Math.round(avg * 100.0) / 100.0);
            agg.put("max", max == Double.NEGATIVE_INFINITY ? 0.0 : max);
            aggregates.add(agg);
        }

        System.out.println("  [aggregate] Computed aggregates for " + aggregates.size() + " windows");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("aggregates", aggregates);
        return result;
    }
}
