package tradeexecution.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExecuteWorkerTest {

    private final ExecuteWorker worker = new ExecuteWorker();

    @Test
    void taskDefName() {
        assertEquals("trd_execute", worker.getTaskDefName());
    }

    @Test
    void executesTradeWithCorrectOutput() {
        Task task = taskWith(Map.of("side", "BUY", "quantity", 100, "symbol", "AAPL", "exchange", "NYSE"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("EXEC-TRD-44201", result.getOutputData().get("executionId"));
        assertEquals(175.42, result.getOutputData().get("fillPrice"));
        assertNotNull(result.getOutputData().get("totalValue"));
        assertNotNull(result.getOutputData().get("executedAt"));
    }

    @Test
    void calculatesTotalValueCorrectly() {
        Task task = taskWith(Map.of("side", "BUY", "quantity", 200, "symbol", "AAPL", "exchange", "NASDAQ"));
        TaskResult result = worker.execute(task);

        double totalValue = ((Number) result.getOutputData().get("totalValue")).doubleValue();
        assertEquals(35084.0, totalValue, 0.01);
    }

    @Test
    void handlesStringQuantity() {
        Task task = taskWith(Map.of("side", "SELL", "quantity", "50", "symbol", "MSFT", "exchange", "NYSE"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
