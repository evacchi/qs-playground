package org.drools.modelcompiler.builder.generator;

import static org.drools.javaparser.JavaParser.parseType;
import static org.drools.javaparser.ast.NodeList.nodeList;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.drools.compiler.lang.descr.FunctionDescr;
import org.drools.javaparser.ast.stmt.BlockStmt;
import org.drools.javaparser.ast.stmt.TryStmt;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;

public class FunctionGenerator {

    public static MethodDeclaration toFunction(FunctionDescr desc) {

        List<Parameter> parameters = new ArrayList<>();

        List<String> parameterTypes = desc.getParameterTypes();
        for (int i = 0; i < parameterTypes.size(); i++) {
            String type = parameterTypes.get(i);
            String name = desc.getParameterNames().get(i);
            parameters.add(new Parameter(parseType(type), name));
        }

        EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC, Modifier.STATIC);
        MethodDeclaration methodDeclaration = new MethodDeclaration(modifiers, desc.getName(), parseType(desc.getReturnType()), nodeList(parameters));

        BlockStmt block = DrlxParseUtil.parseBlock("try {} catch (Exception e) { throw new RuntimeException(e); }");
        TryStmt tryStmt = (TryStmt) block.getStatement( 0 );
        tryStmt.setTryBlock( DrlxParseUtil.parseBlock(desc.getBody() ) );

        methodDeclaration.setBody( block );

        return methodDeclaration;
    }
}
