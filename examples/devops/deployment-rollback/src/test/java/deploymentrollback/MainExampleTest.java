package deploymentrollback;

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
        assertEquals("rb_detect_failure", new deploymentrollback.workers.DetectFailureWorker().getTaskDefName());
        assertEquals("rb_identify_version", new deploymentrollback.workers.IdentifyVersionWorker().getTaskDefName());
        assertEquals("rb_rollback_deploy", new deploymentrollback.workers.RollbackDeployWorker().getTaskDefName());
        assertEquals("rb_verify_rollback", new deploymentrollback.workers.VerifyRollbackWorker().getTaskDefName());
    }
}
