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

import org.drools.javaparser.ast.expr.IntegerLiteralExpr;
import org.drools.javaparser.ast.expr.LongLiteralExpr;
import org.drools.javaparser.ast.expr.MethodCallExpr;
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
    
    protected MethodChainBuilder recorded = new MethodChainBuilder();

    protected void setNodeContainer(NodeContainer nodeContainer) {
    	this.nodeContainer = nodeContainer;
    }
    
    protected NodeContainer getNodeContainer() {
    	return nodeContainer;
    }

    public StartNodeFactory startNode(long id) {
        recorded.appendMethod("startNode", new LongLiteralExpr(id));
        return new StartNodeFactory(this, nodeContainer, id, recorded);
    }

    public EndNodeFactory endNode(long id) {
        recorded.appendMethod("endNode", new LongLiteralExpr(id));
        return new EndNodeFactory(this, nodeContainer, id, recorded);
    }

    public ActionNodeFactory actionNode(long id) {
        recorded.appendMethod("actionNode", new LongLiteralExpr(id));
        return new ActionNodeFactory(this, nodeContainer, id, recorded);
    }

    public MilestoneNodeFactory milestoneNode(long id) {
        recorded.appendMethod("milestoneNode", new LongLiteralExpr(id));
        return new MilestoneNodeFactory(this, nodeContainer, id, recorded);
    }

    public TimerNodeFactory timerNode(long id) {
        recorded.appendMethod("timerNode", new LongLiteralExpr(id));
        return new TimerNodeFactory(this, nodeContainer, id, recorded);
    }

    public HumanTaskNodeFactory humanTaskNode(long id) {
        recorded.appendMethod("humanTaskNode", new LongLiteralExpr(id));
        return new HumanTaskNodeFactory(this, nodeContainer, id, recorded);
    }

    public SubProcessNodeFactory subProcessNode(long id) {
        recorded.appendMethod("subProcessNode", new LongLiteralExpr(id));
        return new SubProcessNodeFactory(this, nodeContainer, id, recorded);
    }

    public SplitFactory splitNode(long id) {
        recorded.appendMethod("splitNode", new LongLiteralExpr(id));
        return new SplitFactory(this, nodeContainer, id, recorded);
    }

    public JoinFactory joinNode(long id) {
        recorded.appendMethod("joinNode", new LongLiteralExpr(id));
        return new JoinFactory(this, nodeContainer, id, recorded);
    }

    public RuleSetNodeFactory ruleSetNode(long id) {
        recorded.appendMethod("ruleSetNode", new LongLiteralExpr(id));
        return new RuleSetNodeFactory(this, nodeContainer, id, recorded);
    }

    public FaultNodeFactory faultNode(long id) {
        recorded.appendMethod("faultNode", new LongLiteralExpr(id));
        return new FaultNodeFactory(this, nodeContainer, id, recorded);
    }

    public EventNodeFactory eventNode(long id) {
        recorded.appendMethod("eventNode", new LongLiteralExpr(id));
        return new EventNodeFactory(this, nodeContainer, id, recorded);
    }

    public BoundaryEventNodeFactory boundaryEventNode(long id) {
        recorded.appendMethod("boundaryEventNode", new LongLiteralExpr(id));
        return new BoundaryEventNodeFactory(this, nodeContainer, id, recorded);
    }

    public CompositeNodeFactory compositeNode(long id) {
        recorded.appendMethod("compositeNode", new LongLiteralExpr(id));
        return new CompositeNodeFactory(this, nodeContainer, id, recorded);
    }

    public ForEachNodeFactory forEachNode(long id) {
        recorded.appendMethod("forEachNode", new LongLiteralExpr(id));
        return new ForEachNodeFactory(this, nodeContainer, id, recorded);
    }
    
    public DynamicNodeFactory dynamicNode(long id) {
        recorded.appendMethod("dynamicNode", new LongLiteralExpr(id));
        return new DynamicNodeFactory(this, nodeContainer, id, recorded);
    }
    
    public WorkItemNodeFactory workItemNode(long id) {
        recorded.appendMethod("workItemNode", new LongLiteralExpr(id));
    	return new WorkItemNodeFactory(this, nodeContainer, id, recorded);
    }

    public RuleFlowNodeContainerFactory connection(long fromId, long toId) {
        recorded.appendMethod("connection",
                              new LongLiteralExpr(fromId),
                              new LongLiteralExpr(toId));
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

