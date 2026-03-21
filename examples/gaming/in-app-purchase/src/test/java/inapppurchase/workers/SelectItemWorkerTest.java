package inapppurchase.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class SelectItemWorkerTest {
    @Test void testExecute() {
        SelectItemWorker w = new SelectItemWorker();
        assertEquals("iap_select_item", w.getTaskDefName());
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS); t.setInputData(Map.of("playerId", "P-042", "itemId", "ITEM-DragonArmor"));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
