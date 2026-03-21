package certificatemanagement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    @Test
    void workflowJsonIsLoadable() {
        var is = getClass().getClassLoader().getResourceAsStream("workflow.json");
        assertNotNull(is, "workflow.json should be loadable from resources");
    }

    @Test
    void workerInstantiation() {
        assertEquals("cm_inventory", new certificatemanagement.workers.InventoryWorker().getTaskDefName());
        assertEquals("cm_assess_expiry", new certificatemanagement.workers.AssessExpiryWorker().getTaskDefName());
        assertEquals("cm_renew", new certificatemanagement.workers.RenewWorker().getTaskDefName());
        assertEquals("cm_distribute", new certificatemanagement.workers.DistributeWorker().getTaskDefName());
    }
}
