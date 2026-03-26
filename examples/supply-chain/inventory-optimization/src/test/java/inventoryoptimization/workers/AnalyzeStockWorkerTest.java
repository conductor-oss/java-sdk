package inventoryoptimization.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnalyzeStockWorkerTest {
    @Test void taskDefName() { assertEquals("io_analyze_stock", new AnalyzeStockWorker().getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("warehouse","WH","skuList",List.of("A"),"stockLevels",List.of(Map.of("sku","A","current",100,"dailyUsage",10)),"reorderPlan",List.of(Map.of("sku","A","currentStock",100,"reorderQty",300)),"optimizedPlan",List.of(Map.of("sku","A")))));
        TaskResult r = new AnalyzeStockWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
