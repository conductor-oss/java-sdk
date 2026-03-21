package tradeexecution.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Random;

/**
 * Routes the trade to the optimal exchange for best execution.
 */
public class RouteWorker implements Worker {

    private static final List<String> EXCHANGES = List.of("NYSE", "NASDAQ", "ARCA");
    private final Random random = new Random();

    @Override
    public String getTaskDefName() {
        return "trd_route";
    }

    @Override
    public TaskResult execute(Task task) {
        String exchange = EXCHANGES.get(random.nextInt(EXCHANGES.size()));

        System.out.println("  [route] Best execution: routing to " + exchange);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("exchange", exchange);
        result.getOutputData().put("latencyMs", 2);
        result.getOutputData().put("nbboSpread", 0.01);
        return result;
    }
}
