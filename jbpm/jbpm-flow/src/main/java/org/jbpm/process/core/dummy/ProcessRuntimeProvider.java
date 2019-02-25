package org.jbpm.process.core.dummy;

import java.util.HashMap;
import java.util.Map;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.core.runtime.process.InternalProcessRuntime;
import org.drools.core.runtime.process.ProcessRuntimeFactory;
import org.drools.core.runtime.process.ProcessRuntimeFactoryService;
import org.jbpm.process.instance.impl.demo.DoNothingWorkItemHandler;
import org.kie.api.definition.process.Process;

public class ProcessRuntimeProvider {

    
    private static Map<String, InternalProcessRuntime> runtimes = new HashMap<>();
    private static Map<String, KnowledgeBaseImpl> kbases = new HashMap<>();
    
    public static InternalProcessRuntime getProcessRuntime(String id, Process...processes) {
        
        if (runtimes.containsKey(id)) {
            KnowledgeBaseImpl kb = kbases.get(id);                        
            
            for (Process process : processes) {
                if (kb.getProcess(process.getId()) == null) {
                    kb.addProcess(process);
                }
            }
            
            return runtimes.get(id);
        }
        
        KnowledgeBaseImpl kb = new KnowledgeBaseImpl(id, new RuleBaseConfiguration());
        
        for (Process process : processes) {
            kb.addProcess(process);
        }
        
        ProcessRuntimeFactoryService svc = ProcessRuntimeFactory.getProcessRuntimeFactoryService();
        DummyWorkingMemory wm = new DummyWorkingMemory(kb);
        
        InternalProcessRuntime processRuntime = svc.newProcessRuntime(wm);
        wm.setProcessRuntime(processRuntime);
        
        processRuntime.getWorkItemManager().registerWorkItemHandler("Log", new DoNothingWorkItemHandler());
        
        runtimes.put(id, processRuntime);
        kbases.put(id, kb);
        
        return processRuntime;
    }
}
