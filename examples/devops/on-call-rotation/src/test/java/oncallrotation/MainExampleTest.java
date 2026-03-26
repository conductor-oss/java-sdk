package oncallrotation;

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
        assertEquals("oc_check_schedule", new oncallrotation.workers.CheckScheduleWorker().getTaskDefName());
        assertEquals("oc_handoff", new oncallrotation.workers.HandoffWorker().getTaskDefName());
        assertEquals("oc_update_routing", new oncallrotation.workers.UpdateRoutingWorker().getTaskDefName());
        assertEquals("oc_confirm", new oncallrotation.workers.ConfirmWorker().getTaskDefName());
    }
}
