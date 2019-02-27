package org.jbpm.ruleflow.core;

import org.drools.javaparser.ast.expr.Expression;
import org.drools.javaparser.ast.expr.MethodCallExpr;

/**
 * small utility to generate method chains of the form
 * foo().bar().baz()
 */
public class MethodChainBuilder {
    private Expression accumulator;

    public void append(MethodCallExpr e) {
        // the scope is the part before the dot
        // so in blah.foo() :
        //    NameExpr("blah") is the scope of MethodCallExpr("foo")
        // and in blah.foo().bar() :
        //    MethodCallExpr("foo") is the scope of MethodCallExpr("bar")
        e.setScope(accumulator);
        accumulator = e;
    }

    public void appendMethod(String name, Expression... args) {
        append(new MethodCallExpr(name, args));
    }

    @Override
    public String toString() {
        return accumulator.toString();
    }
}
