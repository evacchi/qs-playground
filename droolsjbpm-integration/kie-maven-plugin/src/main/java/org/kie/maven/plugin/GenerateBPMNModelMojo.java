package org.kie.maven.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.drools.compiler.kie.builder.impl.InternalKieModule;
import org.drools.compiler.kie.builder.impl.KieBuilderImpl;
import org.drools.core.runtime.process.InternalProcessRuntime;
import org.drools.core.util.StringUtils;
import org.drools.javaparser.ast.CompilationUnit;
import org.drools.javaparser.ast.Modifier.Keyword;
import org.drools.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.drools.javaparser.ast.body.ConstructorDeclaration;
import org.drools.javaparser.ast.body.MethodDeclaration;
import org.drools.javaparser.ast.expr.Name;
import org.drools.javaparser.ast.expr.NameExpr;
import org.drools.javaparser.ast.expr.SingleMemberAnnotationExpr;
import org.drools.javaparser.ast.expr.StringLiteralExpr;
import org.drools.javaparser.ast.stmt.BlockStmt;
import org.jbpm.assembler.BPMN2AssemblerService;
import org.jbpm.process.core.context.variable.Variable;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.core.datatype.impl.type.ObjectDataType;
import org.jbpm.ruleflow.core.RuleFlowProcessFactory;
import org.jbpm.util.ExecModelBPMNProcessDumper;
import org.kie.api.KieServices;
import org.kie.api.definition.process.Process;
import org.kie.api.definition.process.WorkflowProcess;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceConfiguration;
import org.kie.api.io.ResourceType;
import org.kie.api.io.ResourceWithConfiguration;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceWithConfigurationImpl;

@Mojo(name = "generateBPMNModel",
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresProject = true,
        defaultPhase = LifecyclePhase.COMPILE)
public class GenerateBPMNModelMojo extends AbstractKieMojo {

    @Parameter(required = true, defaultValue = "${project.build.directory}")
    private File targetDirectory;

    @Parameter(required = true, defaultValue = "${project.basedir}")
    private File projectDir;

    @Parameter
    private Map<String, String> properties;

    @Parameter(required = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(property = "generateBPMNModel", defaultValue = "no")
    private String generateBPMNModel;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (BPMNModelMode.shouldGenerateBPMNModel(generateBPMNModel)) {
            generateBPMNModel();
        }
    }

    private void generateBPMNModel() throws MojoExecutionException {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        KieServices ks = KieServices.Factory.get();

        try {
            setSystemProperties(properties);

            final KieBuilderImpl kieBuilder = (KieBuilderImpl) ks.newKieBuilder(projectDir);

            final List<String> compiledClassNames = new ArrayList<>();     

            InternalKieModule kieModule = (InternalKieModule) kieBuilder.getKieModuleIgnoringErrors();
            List<String> bpmnFiles = getBPMNFiles(kieModule);
            getLog().info("BPMN Files to process: " + bpmnFiles);

            List<Process> processes = new ArrayList<>();
            KnowledgeBuilder knowledgeBuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
            BPMN2AssemblerService assemblerService = new BPMN2AssemblerService() {

                @Override
                protected void onProcessAdded(Process process, Object kbuilder) {                   
                    super.onProcessAdded(process, kbuilder);
                    processes.add(process);
                }
            };            

            for (String bpmnFile : bpmnFiles) {
                compileBPMNFile(kieModule, assemblerService, knowledgeBuilder, bpmnFile);
            }
            
            
            final String additionalCompilerPath = "/generated-sources/bpmn/main/java";
            addNewCompileRoot(additionalCompilerPath);

            for (Process process : processes) {
                
                String classPrefix = StringUtils.capitalize(exctactProcessId(process.getId()));
                
                String sourceContent = ExecModelBPMNProcessDumper.INSTANCE.dump((WorkflowProcess) process);
                // create class with executable model for the process
                String processClazzName = classPrefix + "Process";
                final Path processFileNameRelative = transformPathToMavenPath( "org/kie/codegen/" + processClazzName + ".java");

                compiledClassNames.add(getCompiledClassName(processFileNameRelative));

                final Path processFileName = Paths.get(targetDirectory.getPath(), additionalCompilerPath, processFileNameRelative.toString());                
                createSourceFile(processFileName, generateClass(processClazzName, "return " + sourceContent));
                
                
                // create model class for all variables
                String modelClazzName = classPrefix + "Model";
                String modelDataClazz = generateModelClassForProcess(modelClazzName, (VariableScope) ((org.jbpm.process.core.Process) process).getDefaultContext(VariableScope.VARIABLE_SCOPE));
                
                final Path modelFileNameRelative = transformPathToMavenPath( "org/kie/codegen/model/" + modelClazzName + ".java");
                final Path modelFileName = Paths.get(targetDirectory.getPath(), additionalCompilerPath, modelFileNameRelative.toString());
                createSourceFile(modelFileName, modelDataClazz);
                
                
                // create REST resource class for process
                String resourceClazzName = classPrefix + "Resource";
                String resourceClazz = generateResourceClass(resourceClazzName, modelClazzName, process.getId(), compiledClassNames);
                
                final Path resourceFileNameRelative = transformPathToMavenPath( "org/kie/codegen/rest/" + resourceClazzName + ".java");
                final Path resourceFileName = Paths.get(targetDirectory.getPath(), additionalCompilerPath, resourceFileNameRelative.toString());
                createSourceFile(resourceFileName, resourceClazz);
            }
            createBPMNFile(compiledClassNames);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

        getLog().info("BPMN Model successfully generated");
    }

    private void createBPMNFile(List<String> compiledClassNames) {
        final Path dmnCompiledClassFile = Paths.get(targetDirectory.getPath(), "classes", "META-INF/kie/bpmn");

        try {
            if (!Files.exists(dmnCompiledClassFile)) {
                Files.createDirectories(dmnCompiledClassFile.getParent());
            }
            Files.write(dmnCompiledClassFile, compiledClassNames);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write file", e);
        }
    }

    private List<String> getBPMNFiles(InternalKieModule kieModule) {
        return kieModule.getFileNames()
                        .stream()
                        .filter(f -> f.endsWith("bpmn") || f.endsWith("bpmn2"))
                        .collect(Collectors.toList());
    }

    private void compileBPMNFile(InternalKieModule kieModule, BPMN2AssemblerService assemblerService, KnowledgeBuilder knowledgeBuilder, String dmnFile) throws Exception {
        Resource resource = kieModule.getResource(dmnFile);
        ResourceConfiguration resourceConfiguration = kieModule.getResourceConfiguration(dmnFile);

        ResourceWithConfiguration resourceWithConfiguration =
                new ResourceWithConfigurationImpl(resource, resourceConfiguration, a -> {
                }, b -> {
                });

        assemblerService.addResources(knowledgeBuilder, Collections.singletonList(resourceWithConfiguration), ResourceType.BPMN2);        
    }

    private void createSourceFile(Path newFile, String sourceContent) {
        try {
            Files.deleteIfExists(newFile);
            Files.createDirectories(newFile.getParent());
            Path newFilePath = Files.createFile(newFile);
            Files.write(newFilePath, sourceContent.getBytes());
            getLog().info("Generating file " + newFilePath);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write file", e);
        }
    }

    private String getCompiledClassName(Path fileNameRelative) {
        return fileNameRelative.toString()
                                .replace("/", ".")
                                .replace(".java", "");
    }

    private Path transformPathToMavenPath(String generatedFile) {
        Path fileName = Paths.get(generatedFile);
        Path originalFilePath = Paths.get("src/main/java");
        final Path fileNameRelative;
        if(fileName.startsWith(originalFilePath)) {
            fileNameRelative = originalFilePath.relativize(fileName);
        } else {
            fileNameRelative = fileName;
        }
        return fileNameRelative;
    }

    private void addNewCompileRoot(String droolsModelCompilerPath) {
        final String newCompileSourceRoot = targetDirectory.getPath() + droolsModelCompilerPath;
        project.addCompileSourceRoot(newCompileSourceRoot);
    }
    
    protected String generateClass(String clazzName, String body) {
        CompilationUnit compilationUnit = new CompilationUnit();
                
        ClassOrInterfaceDeclaration processClass = compilationUnit
                .setPackageDeclaration("org.kie.codegen")
                .addImport(Process.class)
                .addImport(RuleFlowProcessFactory.class)
                .addImport(ObjectDataType.class)
                .addClass(clazzName)
                .setPublic(true);
        MethodDeclaration processMethod = processClass.addMethod("process", Keyword.PUBLIC, Keyword.STATIC);
        processMethod.setType("Process");
        processMethod.createBody().addStatement(body);
        String code = compilationUnit.toString();
        
        return code;
    }
    
    public String generateModelClassForProcess(String clazzName, VariableScope variableScope) {
        CompilationUnit compilationUnit = new CompilationUnit();
        

        ClassOrInterfaceDeclaration modelClass = compilationUnit 
                .setPackageDeclaration("org.kie.codegen.model")
                .addImport(Map.class)
                .addImport(HashMap.class)
                .addClass(clazzName)
                .setPublic(true);
        
        modelClass.addField(Long.class, "id", Keyword.PRIVATE);
        modelClass
        .addMethod("getId", Keyword.PUBLIC)
        .setType(Long.class)
        .createBody().addStatement("return this.id;");
    
        modelClass
        .addMethod("setId", Keyword.PUBLIC)
        .addParameter(Long.class, "id")
        .createBody().addStatement("this.id = id;");
        
        List<String> toMap = new ArrayList<>();
        toMap.add("Map<String, Object> params = new HashMap<>();");
        
        List<String> fromMap = new ArrayList<>();        
        fromMap.add(clazzName + " item = new " + clazzName + "();");
        fromMap.add("item.id = id;\n");
        
        for (Variable v : variableScope.getVariables()) {            
            
            modelClass.addField(v.getType().getStringType(), v.getName(), Keyword.PRIVATE);
            
            modelClass
                .addMethod("get" + StringUtils.capitalize(v.getName()), Keyword.PUBLIC)
                .setType(v.getType().getStringType())
                .createBody().addStatement("return this." + v.getName() + ";");
            
            modelClass
                .addMethod("set" + StringUtils.capitalize(v.getName()), Keyword.PUBLIC)
                .addParameter(v.getType().getStringType(), v.getName())
                .createBody().addStatement("this." + v.getName() + " = " + v.getName() + ";");
            
            toMap.add("params.put(\"" + v.getName() + "\", this." + v.getName() + ");\n");
            
            fromMap.add("item." + v.getName() + " = (" + v.getType().getStringType()+ ") params.get(\"" + v.getName() + "\");\n");
        }
        
        toMap.add("return params;\n");
        
        
        MethodDeclaration toMapMethod = modelClass
                .addMethod("toMap", Keyword.PUBLIC)
            .setType("Map<String, Object>");
        BlockStmt body = toMapMethod.createBody();
        
        for (String s : toMap) {
            body.addStatement(s);
        }
        
        MethodDeclaration fromMapMethod = modelClass
                .addMethod("fromMap", Keyword.PUBLIC, Keyword.STATIC)
                .setType(clazzName)
            .addParameter("Long", "id")
            .addParameter("Map<String, Object>", "params");
        BlockStmt fromMapBody = fromMapMethod.createBody();
        
        for (String s : fromMap) {
            fromMapBody.addStatement(s);
        }
        fromMapBody.addStatement("return item;");
        
        return compilationUnit.toString();
    }
    
    public String generateResourceClass(String clazzName, String dataClazzName, String processId, List<String> compiledClassNames) {
        CompilationUnit compilationUnit = new CompilationUnit();
        

        ClassOrInterfaceDeclaration resourceClass = compilationUnit
                .setPackageDeclaration("org.kie.codegen.rest")
                .addImport("javax.ws.rs.*")
                .addImport("javax.ws.rs.core.*")
                .addImport("java.util.*")
                .addImport("java.util.stream.*")
                .addImport("org.kie.api.runtime.process.ProcessInstance")
                .addImport("org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl")
                .addImport("org.kie.codegen.model.*")
                .addClass(clazzName)
                .setPublic(true)
                .addAnnotation(new SingleMemberAnnotationExpr(new Name("Path"), new StringLiteralExpr("/" + exctactProcessId(processId))));
        
        resourceClass.addField(InternalProcessRuntime.class, "processRuntime", Keyword.PRIVATE);
        resourceClass.addField(String.class, "processId", Keyword.PRIVATE);
        // TODO setup method should be externalized and simply injected
        ConstructorDeclaration constructor = resourceClass.addConstructor(Keyword.PUBLIC);
        BlockStmt constructorBody = constructor.createBody();
                
        constructorBody.addStatement("this.processRuntime = org.jbpm.process.core.dummy.ProcessRuntimeProvider.getProcessRuntime(\"" + project.getArtifactId() + "\", " + compiledClassNames.stream().map(s -> s + ".process()").collect(Collectors.joining(", ")) + ");");       
        
        
        constructorBody.addStatement("this.processId = \"" + processId + "\";");
        
        
        // creates new resource
        MethodDeclaration create = resourceClass.addMethod("createResource", Keyword.PUBLIC).setType(dataClazzName)
        .addAnnotation("POST")
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Produces"), new NameExpr("MediaType.APPLICATION_JSON")))
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Consumes"), new NameExpr("MediaType.APPLICATION_JSON")));
        
        create.addAndGetParameter(dataClazzName, "resource");
        BlockStmt bodyCreate = create.createBody();
        bodyCreate.addStatement("ProcessInstance pi = this.processRuntime.startProcess(this.processId, resource.toMap());");
        bodyCreate.addStatement("return " + dataClazzName + ".fromMap(pi.getId(), ((WorkflowProcessInstanceImpl) pi).getVariables());");
        
        // get all resources
        resourceClass.addMethod("getResources", Keyword.PUBLIC).setType("List<" + dataClazzName + ">")
        .addAnnotation("GET")
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Produces"), new NameExpr("MediaType.APPLICATION_JSON")))
        .createBody().addStatement("return processRuntime.getProcessInstances().stream().filter(pi -> pi.getProcessId().equals(processId)).map(pi -> " + dataClazzName + ".fromMap(pi.getId(), ((WorkflowProcessInstanceImpl) pi).getVariables())).collect(Collectors.toList());");
        
        // get given resource
        MethodDeclaration get = resourceClass.addMethod("getResource", Keyword.PUBLIC).setType(dataClazzName)
        .addAnnotation("GET")
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Path"), new StringLiteralExpr("/{id}")))
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Produces"), new NameExpr("MediaType.APPLICATION_JSON")));
        
        get.addAndGetParameter("Long", "id").addAnnotation(new SingleMemberAnnotationExpr(new Name("PathParam"), new StringLiteralExpr("id")));
        get.createBody().addStatement("return Optional.ofNullable(this.processRuntime.getProcessInstance(id, true)).map(pi -> " + dataClazzName + ".fromMap(pi.getId(), ((WorkflowProcessInstanceImpl) pi).getVariables())).orElse(null);");
                
        // delete given resource
        MethodDeclaration delete = resourceClass.addMethod("deleteResource", Keyword.PUBLIC).setType(dataClazzName)
        .addAnnotation("DELETE")
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Path"), new StringLiteralExpr("/{id}")))
        .addAnnotation(new SingleMemberAnnotationExpr(new Name("Produces"), new NameExpr("MediaType.APPLICATION_JSON")));
        
        delete.addAndGetParameter("Long", "id").addAnnotation(new SingleMemberAnnotationExpr(new Name("PathParam"), new StringLiteralExpr("id")));
        BlockStmt deleteBody = delete.createBody();
        deleteBody.addStatement(dataClazzName + " item = Optional.ofNullable(this.processRuntime.getProcessInstance(id, true)).map(pi -> " + dataClazzName + ".fromMap(pi.getId(), ((WorkflowProcessInstanceImpl) pi).getVariables())).orElse(null);");
        deleteBody.addStatement("this.processRuntime.abortProcessInstance(id);");
        deleteBody.addStatement("return item;");
        
        return compilationUnit.toString();
    }
    
    protected String exctactProcessId(String processId) {
        if (processId.contains(".")) {
            return processId.substring(processId.lastIndexOf(".") + 1);
        }
        
        return processId;
    }
}

