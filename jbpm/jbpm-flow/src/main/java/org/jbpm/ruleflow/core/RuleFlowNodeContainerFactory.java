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

package org.jbpm.ruleflow.core;

import org.jbpm.ruleflow.core.factory.ActionNodeFactory;
import org.jbpm.ruleflow.core.factory.BoundaryEventNodeFactory;
import org.jbpm.ruleflow.core.factory.CompositeNodeFactory;
import org.jbpm.ruleflow.core.factory.DynamicNodeFactory;
import org.jbpm.ruleflow.core.factory.EndNodeFactory;
import org.jbpm.ruleflow.core.factory.EventNodeFactory;
import org.jbpm.ruleflow.core.factory.FaultNodeFactory;
import org.jbpm.ruleflow.core.factory.ForEachNodeFactory;
import org.jbpm.ruleflow.core.factory.HumanTaskNodeFactory;
import org.jbpm.ruleflow.core.factory.JoinFactory;
import org.jbpm.ruleflow.core.factory.MilestoneNodeFactory;
import org.jbpm.ruleflow.core.factory.RuleSetNodeFactory;
import org.jbpm.ruleflow.core.factory.SplitFactory;
import org.jbpm.ruleflow.core.factory.StartNodeFactory;
import org.jbpm.ruleflow.core.factory.SubProcessNodeFactory;
import org.jbpm.ruleflow.core.factory.TimerNodeFactory;
import org.jbpm.ruleflow.core.factory.WorkItemNodeFactory;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.kie.api.definition.process.Node;

public abstract class RuleFlowNodeContainerFactory {

    private NodeContainer nodeContainer;
    
    protected StringBuilder recorded = new StringBuilder();

    protected void setNodeContainer(NodeContainer nodeContainer) {
    	this.nodeContainer = nodeContainer;
    }
    
    protected NodeContainer getNodeContainer() {
    	return nodeContainer;
    }

    public StartNodeFactory startNode(long id) {
        recorded.append(".startNode(" + id + ")\n\t");
        return new StartNodeFactory(this, nodeContainer, id, recorded);
    }

    public EndNodeFactory endNode(long id) {
        recorded.append(".endNode(" + id + ")\n\t");
        return new EndNodeFactory(this, nodeContainer, id, recorded);
    }

    public ActionNodeFactory actionNode(long id) {
        recorded.append(".actionNode(" + id + ")\n\t");
        return new ActionNodeFactory(this, nodeContainer, id, recorded);
    }

    public MilestoneNodeFactory milestoneNode(long id) {
        recorded.append(".milestoneNode(" + id + ")\n\t");
        return new MilestoneNodeFactory(this, nodeContainer, id, recorded);
    }

    public TimerNodeFactory timerNode(long id) {
        recorded.append(".timerNode(" + id + ")\n\t");
        return new TimerNodeFactory(this, nodeContainer, id, recorded);
    }

    public HumanTaskNodeFactory humanTaskNode(long id) {
        recorded.append(".humanTaskNode(" + id + ")\n\t");
        return new HumanTaskNodeFactory(this, nodeContainer, id, recorded);
    }

    public SubProcessNodeFactory subProcessNode(long id) {
        recorded.append(".subProcessNode(" + id + ")\n\t");
        return new SubProcessNodeFactory(this, nodeContainer, id, recorded);
    }

    public SplitFactory splitNode(long id) {
        recorded.append(".splitNode(" + id + ")\n\t");
        return new SplitFactory(this, nodeContainer, id, recorded);
    }

    public JoinFactory joinNode(long id) {
        recorded.append(".joinNode(" + id + ")\n\t");
        return new JoinFactory(this, nodeContainer, id, recorded);
    }

    public RuleSetNodeFactory ruleSetNode(long id) {
        recorded.append(".ruleSetNode(" + id + ")\n\t");
        return new RuleSetNodeFactory(this, nodeContainer, id, recorded);
    }

    public FaultNodeFactory faultNode(long id) {
        recorded.append(".faultNode(" + id + ")\n\t");
        return new FaultNodeFactory(this, nodeContainer, id, recorded);
    }

    public EventNodeFactory eventNode(long id) {
        recorded.append(".eventNode(" + id + ")\n\t");
        return new EventNodeFactory(this, nodeContainer, id, recorded);
    }

    public BoundaryEventNodeFactory boundaryEventNode(long id) {
        recorded.append(".boundaryEventNode(" + id + ")\n\t");
        return new BoundaryEventNodeFactory(this, nodeContainer, id, recorded);
    }

    public CompositeNodeFactory compositeNode(long id) {
        recorded.append(".compositeNode(" + id + ")\n\t");
        return new CompositeNodeFactory(this, nodeContainer, id, recorded);
    }

    public ForEachNodeFactory forEachNode(long id) {
        recorded.append(".forEachNode(" + id + ")\n\t");
        return new ForEachNodeFactory(this, nodeContainer, id, recorded);
    }
    
    public DynamicNodeFactory dynamicNode(long id) {
        recorded.append(".dynamicNode(" + id + ")\n\t");
        return new DynamicNodeFactory(this, nodeContainer, id, recorded);
    }
    
    public WorkItemNodeFactory workItemNode(long id) {
        recorded.append(".workItemNode(" + id + ")\n\t");
    	return new WorkItemNodeFactory(this, nodeContainer, id, recorded);
    }

    public RuleFlowNodeContainerFactory connection(long fromId, long toId) {
        recorded.append(".connection(" + fromId + ", " + toId + ")\n");
        Node from = nodeContainer.getNode(fromId);
        Node to = nodeContainer.getNode(toId);
        new ConnectionImpl(
            from, org.jbpm.workflow.core.Node.CONNECTION_DEFAULT_TYPE,
            to, org.jbpm.workflow.core.Node.CONNECTION_DEFAULT_TYPE);
        return this;
    }
    
    public abstract RuleFlowNodeContainerFactory done();
    
    public abstract RuleFlowNodeContainerFactory validate();
    
    public abstract RuleFlowProcess getProcess();
    
    public String getRecordedStructure() {
        return recorded.toString();
    }

}

