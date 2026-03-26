package maintenancewindow;

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
        assertEquals("mw_notify_start", new maintenancewindow.workers.NotifyStartWorker().getTaskDefName());
        assertEquals("mw_suppress_alerts", new maintenancewindow.workers.SuppressAlertsWorker().getTaskDefName());
        assertEquals("mw_execute_maintenance", new maintenancewindow.workers.ExecuteMaintenanceWorker().getTaskDefName());
        assertEquals("mw_restore_normal", new maintenancewindow.workers.RestoreNormalWorker().getTaskDefName());
    }
}
