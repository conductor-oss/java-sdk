package featureenvironment;

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
        assertEquals("fe_provision", new featureenvironment.workers.ProvisionWorker().getTaskDefName());
        assertEquals("fe_deploy_branch", new featureenvironment.workers.DeployBranchWorker().getTaskDefName());
        assertEquals("fe_configure_dns", new featureenvironment.workers.ConfigureDnsWorker().getTaskDefName());
        assertEquals("fe_notify", new featureenvironment.workers.NotifyWorker().getTaskDefName());
    }
}
