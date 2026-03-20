package devsecopspipeline;

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
        assertEquals("dso_sast_scan", new devsecopspipeline.workers.SastScanWorker().getTaskDefName());
        assertEquals("dso_sca_scan", new devsecopspipeline.workers.ScaScanWorker().getTaskDefName());
        assertEquals("dso_container_scan", new devsecopspipeline.workers.ContainerScanWorker().getTaskDefName());
        assertEquals("dso_security_gate", new devsecopspipeline.workers.SecurityGateWorker().getTaskDefName());
    }
}
