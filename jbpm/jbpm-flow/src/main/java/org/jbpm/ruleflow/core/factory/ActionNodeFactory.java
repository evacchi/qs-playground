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

package org.jbpm.ruleflow.core.factory;

import org.drools.javaparser.ast.body.Parameter;
import org.drools.javaparser.ast.expr.AssignExpr;
import org.drools.javaparser.ast.expr.CastExpr;
import org.drools.javaparser.ast.expr.LambdaExpr;
import org.drools.javaparser.ast.expr.MethodCallExpr;
import org.drools.javaparser.ast.expr.NameExpr;
import org.drools.javaparser.ast.expr.StringLiteralExpr;
import org.drools.javaparser.ast.expr.VariableDeclarationExpr;
import org.drools.javaparser.ast.stmt.BlockStmt;
import org.drools.javaparser.ast.stmt.ExpressionStmt;
import org.drools.javaparser.ast.stmt.Statement;
import org.drools.javaparser.ast.type.ClassOrInterfaceType;
import org.drools.javaparser.ast.type.UnknownType;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.impl.Action;
import org.jbpm.ruleflow.core.MethodChainBuilder;
import org.jbpm.ruleflow.core.RuleFlowNodeContainerFactory;
import org.jbpm.workflow.core.DroolsAction;
import org.jbpm.workflow.core.Node;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.impl.DroolsConsequenceAction;
import org.jbpm.workflow.core.node.ActionNode;

public class ActionNodeFactory extends NodeFactory {

    public ActionNodeFactory(RuleFlowNodeContainerFactory nodeContainerFactory,
                             NodeContainer nodeContainer,
                             long id,
                             MethodChainBuilder recorded
    ) {
        super(nodeContainerFactory,
              nodeContainer,
              id,
              recorded);
    }

    protected Node createNode() {
        return new ActionNode();
    }

    protected ActionNode getActionNode() {
        return (ActionNode) getNode();
    }

    public ActionNodeFactory name(String name) {
        getNode().setName(name);
        recorded.appendMethod("name", new StringLiteralExpr(name));
        return this;
    }

    public ActionNodeFactory action(String dialect,
                                    String action) {
        return action(dialect, action, false);
    }

    public ActionNodeFactory action(String dialect,
                                    String action,
                                    boolean isDroolsAction) {
        if (isDroolsAction) {
            DroolsAction droolsAction = new DroolsAction();
            droolsAction.setMetaData("Action",
                                     action);
            getActionNode().setAction(droolsAction);
        } else {
            getActionNode().setAction(new DroolsConsequenceAction(dialect,
                                                                  action));
        }
        generateAction(action);

        return this;
    }

    public ActionNodeFactory action(Action action) {
        DroolsAction droolsAction = new DroolsAction();
        droolsAction.setMetaData("Action",
                                 action);
        getActionNode().setAction(droolsAction);

        generateAction(action.toString());
        return this;
    }

    protected void generateAction(String action) {
        VariableScope vscope = (VariableScope) ((org.jbpm.process.core.Process) nodeContainer).getDefaultContext(VariableScope.VARIABLE_SCOPE);

        BlockStmt body = new BlockStmt();
        LambdaExpr lambda = new LambdaExpr(
                new Parameter(new UnknownType(), "kcontext"), // (kcontext) ->
                body
        );

        for (Variable v : vscope.getVariables()) {
            body.addStatement(makeAssignment(v));
        }

        recorded.appendMethod("action", lambda);
    }

    private Statement makeAssignment(Variable v) {
        ClassOrInterfaceType type =
                new ClassOrInterfaceType(v.getType().getStringType());
        String name = v.getName();

        // `type` `name` = (`type`) `kcontext.getVariable
        AssignExpr assignExpr = new AssignExpr(
                new VariableDeclarationExpr(type, name),
                new CastExpr(
                        type,
                        new MethodCallExpr(
                                new NameExpr("kcontext"),
                                "getVariable")
                                .addArgument(name)),
                AssignExpr.Operator.ASSIGN);

        return new ExpressionStmt(assignExpr);
    }
}
