/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpm.process.core.ContextContainer;
import org.jbpm.process.core.ParameterDefinition;
import org.jbpm.process.core.Work;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.ruleflow.core.factory.WorkItemNodeFactory;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.jbpm.workflow.core.node.ActionNode;
import org.jbpm.workflow.core.node.EndNode;
import org.jbpm.workflow.core.node.StartNode;
import org.jbpm.workflow.core.node.WorkItemNode;
import org.kie.api.definition.process.Connection;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.NodeContainer;
import org.kie.api.definition.process.WorkflowProcess;

public class ExecModelBPMNProcessDumper {

	public static final ExecModelBPMNProcessDumper INSTANCE = new ExecModelBPMNProcessDumper();
    private ExecModelBPMNProcessDumper() {
   
    }

    public String dump(WorkflowProcess process) {
        RuleFlowProcessFactory factory = RuleFlowProcessFactory.createProcess(process.getId());
        
        visitProcess(process, factory);
        
        return factory.getRecordedStructure();
    }


	private Set<String> visitedVariables;

	protected void visitProcess(WorkflowProcess process, RuleFlowProcessFactory factory) {
        
    	// item definitions
    	this.visitedVariables = new HashSet<String>();
    	VariableScope variableScope = (VariableScope)
    		((org.jbpm.process.core.Process) process).getDefaultContext(VariableScope.VARIABLE_SCOPE);

    	visitVariableScope(variableScope, factory);
    	visitSubVariableScopes(process.getNodes(), factory);

	    visitInterfaces(process.getNodes(), factory);

	    // the process itself
		factory
		    .name(process.getName())
		    .packageName(process.getPackageName())
		    .dynamic(((org.jbpm.workflow.core.WorkflowProcess) process).isDynamic())
		    .version(process.getVersion());

        visitHeader(process, factory);
        
        List<org.jbpm.workflow.core.Node> processNodes = new ArrayList<org.jbpm.workflow.core.Node>();
        for( Node procNode : process.getNodes()) { 
            processNodes.add((org.jbpm.workflow.core.Node) procNode);
        }
        visitNodes(processNodes, factory);
        visitConnections(process.getNodes(), factory);
        
        factory.validate().getProcess();
   
    }

    private void visitVariableScope(VariableScope variableScope, RuleFlowProcessFactory factory) {
        if (variableScope != null && !variableScope.getVariables().isEmpty()) {
            for (Variable variable: variableScope.getVariables()) {
                
                if( !visitedVariables.add(variable.getName()) ) { 
                    continue;
                }
                factory.variable(variable.getName(), variable.getType());                
            }
            
        }
    }

    private void visitSubVariableScopes(Node[] nodes, RuleFlowProcessFactory factory) {
        for (Node node: nodes) {
            if (node instanceof ContextContainer) {
                VariableScope variableScope = (VariableScope) 
                    ((ContextContainer) node).getDefaultContext(VariableScope.VARIABLE_SCOPE);
                if (variableScope != null) {
                    visitVariableScope(variableScope, factory);
                }
            }
            if (node instanceof NodeContainer) {
                visitSubVariableScopes(((NodeContainer) node).getNodes(), factory);
            }
        }
    }

    protected void visitHeader(WorkflowProcess process, RuleFlowProcessFactory factory) {
        Map<String, Object> metaData = getMetaData(process.getMetaData());
    	Set<String> imports = ((org.jbpm.process.core.Process) process).getImports();
    	Map<String, String> globals = ((org.jbpm.process.core.Process) process).getGlobals();
    	if ((imports != null && !imports.isEmpty()) || (globals != null && globals.size() > 0) || !metaData.isEmpty()) {    		
    		if (imports != null) {
	    		for (String s: imports) {
	    			factory.imports(s);
	    		}
    		}
    		if (globals != null) {
	    		for (Map.Entry<String, String> global: globals.entrySet()) {
	    			factory.global(global.getKey(), global.getValue());
	    		}
    		}
    	}        
    }

    public static Map<String, Object> getMetaData(Map<String, Object> input) {
    	Map<String, Object> metaData = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry: input.entrySet()) {
        	String name = entry.getKey();
        	if (entry.getKey().startsWith("custom") 
        			&& entry.getValue() instanceof String) {
        		metaData.put(name, entry.getValue());
        	}
        }
        return metaData;
    }

    protected void visitInterfaces(Node[] nodes, RuleFlowProcessFactory factory) {
        for (Node node: nodes) {
            if (node instanceof WorkItemNode) {
                Work work = ((WorkItemNode) node).getWork();
                if (work != null) {
                }
            }
        }
    }

    public void visitNodes(List<org.jbpm.workflow.core.Node> nodes, RuleFlowProcessFactory factory) {
    	
        for (Node node: nodes) {
            visitNode(node, factory);
        }
        
    }

    private void visitNode(Node node, RuleFlowProcessFactory factory) {
        if (node instanceof StartNode) {
            StartNode startNode = (StartNode) node;
            
            factory.startNode(startNode.getId())
            .name(startNode.getName())            
            .done();
            
        } else if (node instanceof ActionNode) {
     	    ActionNode actionNode = (ActionNode) node;
     	    
     	    factory.actionNode(actionNode.getId())
     	    .name(actionNode.getName())
     	    .action("java", actionNode.getAction().toString())
     	    .done();     	    
     	} else if (node instanceof EndNode) {
     	   EndNode endNode = (EndNode) node;
            
            factory.endNode(endNode.getId())
            .name(endNode.getName())
            .terminate(endNode.isTerminate())
            .done();
            
        } else if (node instanceof WorkItemNode) {
            WorkItemNode workItemNode = (WorkItemNode) node;
            Work work = workItemNode.getWork();
            WorkItemNodeFactory nodefactory = factory.workItemNode(workItemNode.getId())
            .name(workItemNode.getName())
            .workName(work.getName());
            
            for (String parameter : work.getParameterNames()) {
                nodefactory.workParameter(parameter, work.getParameter(parameter));
            }
            
            for (ParameterDefinition parameter : work.getParameterDefinitions()) {
                nodefactory.workParameterDefinition(parameter.getName(), parameter.getType());
            }
            
            nodefactory.done();
            
        }
    }

    

    private void visitConnections(Node[] nodes, RuleFlowProcessFactory factory) {
    	
        List<Connection> connections = new ArrayList<Connection>();
        for (Node node: nodes) {
            for (List<Connection> connectionList: node.getIncomingConnections().values()) {
                connections.addAll(connectionList);
            }
        }
        for (Connection connection: connections) {
            visitConnection(connection, factory);
        }
        
    }

    private boolean isConnectionRepresentingLinkEvent(Connection connection) {
        boolean bValue = connection.getMetaData().get("linkNodeHidden") != null;
        return bValue;
    }

    public void visitConnection(Connection connection, RuleFlowProcessFactory factory) {
    	// if the connection was generated by a link event, don't dump.
        if (isConnectionRepresentingLinkEvent(connection)) {
        	return;
        }
        // if the connection is a hidden one (compensations), don't dump
        Object hidden = ((ConnectionImpl) connection).getMetaData("hidden");
        if( hidden != null && ((Boolean) hidden) ) { 
           return; 
        }
        factory.connection(connection.getFrom().getId(), connection.getTo().getId());        
    }

}
