package containerorchestration;

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
        assertEquals("co_build", new containerorchestration.workers.BuildWorker().getTaskDefName());
        assertEquals("co_scan", new containerorchestration.workers.ScanWorker().getTaskDefName());
        assertEquals("co_deploy", new containerorchestration.workers.DeployWorker().getTaskDefName());
        assertEquals("co_health_check", new containerorchestration.workers.HealthCheckWorker().getTaskDefName());
    }
}
