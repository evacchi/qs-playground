package org.jbpm.bpmn2.execmodel;

import static org.junit.Assert.assertNotNull;

import org.jbpm.process.core.datatype.impl.type.ObjectDataType;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.util.ExecModelBPMNProcessDumper;
import org.junit.Test;
import org.kie.api.definition.process.WorkflowProcess;

public class TransformToExecModelTest {

    @Test
    public void testSimpleTransformation() {

        RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess("org.kie.api2.MyProcessUnit");
        org.kie.api.definition.process.Process process  = factory.name("HelloWorldProcess")
                .version("1.0")
                .packageName("org.jbpm")
                .variable("person", new ObjectDataType("org.jbpm.test.Person"))
                // Nodes
                .startNode(1).name("Start").done()
                .actionNode(2).name("Action")
                .action("java", "System.out.println(\"Hello!\");").done()
                .endNode(3).name("End").done()
                // Connections
                .connection(1, 2)
                .connection(2, 3)
                .validate()
                .getProcess();
        assertNotNull(process);
        
        
        ExecModelBPMNProcessDumper dumper = ExecModelBPMNProcessDumper.INSTANCE;
        
        String content = dumper.dump((WorkflowProcess) process);
        
        System.out.println(content);
    }
    
    @Test
    public void testGenerated() {
          
        org.kie.api.definition.process.Process generated = RuleFlowProcessFactory.createProcess("org.kie.api2.MyProcessUnit")
                .name("HelloWorldProcess")
                .version("1.0")
                .packageName("org.jbpm")
                .startNode(1)
                    .name("Start").done()
                .actionNode(2)
                    .name("Action").action("java", "System.out.println(\"Hello!\");", false).done()
                .endNode(3)
                    .name("End").done()
                .connection(1, 2)
                .connection(2, 3)
                .validate().getProcess();
        
        assertNotNull(generated);
    }
}
