package logisticsoptimization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class OptimizeRoutesWorker implements Worker {
    @Override public String getTaskDefName() { return "lo_optimize_routes"; }

    @Override public TaskResult execute(Task task) {
        List<Map<String, Object>> routes = List.of(
                Map.of("id", "R1", "stops", 8, "distance", "42 km", "zone", "north"),
                Map.of("id", "R2", "stops", 6, "distance", "35 km", "zone", "south"),
                Map.of("id", "R3", "stops", 10, "distance", "55 km", "zone", "east"));
        System.out.println("  [routes] Optimized " + routes.size() + " routes — saved 18% distance");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("routes", routes);
        r.getOutputData().put("routeCount", routes.size());
        return r;
    }
}
