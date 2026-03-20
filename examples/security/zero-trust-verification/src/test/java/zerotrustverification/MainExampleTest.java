package zerotrustverification;

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
        assertEquals("zt_verify_identity", new zerotrustverification.workers.VerifyIdentityWorker().getTaskDefName());
        assertEquals("zt_assess_device", new zerotrustverification.workers.AssessDeviceWorker().getTaskDefName());
        assertEquals("zt_evaluate_context", new zerotrustverification.workers.EvaluateContextWorker().getTaskDefName());
        assertEquals("zt_enforce_policy", new zerotrustverification.workers.EnforcePolicyWorker().getTaskDefName());
    }
}
