package identityprovisioning;

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
        assertEquals("ip_create_identity", new identityprovisioning.workers.CreateIdentityWorker().getTaskDefName());
        assertEquals("ip_assign_roles", new identityprovisioning.workers.AssignRolesWorker().getTaskDefName());
        assertEquals("ip_provision_access", new identityprovisioning.workers.ProvisionAccessWorker().getTaskDefName());
        assertEquals("ip_verify_setup", new identityprovisioning.workers.VerifySetupWorker().getTaskDefName());
    }
}
