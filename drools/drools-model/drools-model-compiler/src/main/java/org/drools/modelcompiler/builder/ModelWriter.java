package org.drools.modelcompiler.builder;

import static org.drools.modelcompiler.builder.JavaParserCompiler.getPrettyPrinter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.drools.compiler.compiler.io.memory.MemoryFileSystem;
import org.drools.javaparser.ast.body.ClassOrInterfaceDeclaration;
import org.drools.javaparser.printer.PrettyPrinter;
import org.drools.modelcompiler.builder.PackageModel.RuleSourceResult;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;

public class ModelWriter {

    public Result writeModel(MemoryFileSystem srcMfs, Collection<PackageModel> packageModels) {
        List<String> sourceFiles = new ArrayList<>();
        List<String> modelFiles = new ArrayList<>();

        PrettyPrinter prettyPrinter = getPrettyPrinter();

        for (PackageModel pkgModel : packageModels) {
            String pkgName = pkgModel.getName();
            String folderName = pkgName.replace( '.', '/' );

            for (ClassOrInterfaceDeclaration generatedPojo : pkgModel.getGeneratedPOJOsSource()) {
                final String source = JavaParserCompiler.toPojoSource( pkgModel.getName(), pkgModel.getImports(), pkgModel.getStaticImports(), generatedPojo );
                pkgModel.logRule( source );
                String pojoSourceName = "src/main/java/" + folderName + "/" + generatedPojo.getName() + ".java";
                srcMfs.write( pojoSourceName, source.getBytes() );
                sourceFiles.add( pojoSourceName );
            }

            for (GeneratedClassWithPackage generatedPojo : pkgModel.getGeneratedAccumulateClasses()) {
                final String source = JavaParserCompiler.toPojoSource( pkgModel.getName(), generatedPojo.getImports(), pkgModel.getStaticImports(), generatedPojo.getGeneratedClass() );
                pkgModel.logRule( source );
                String pojoSourceName = "src/main/java/" + folderName + "/" + generatedPojo.getGeneratedClass().getName() + ".java";
                srcMfs.write( pojoSourceName, source.getBytes() );
                sourceFiles.add( pojoSourceName );
            }

            RuleSourceResult rulesSourceResult = pkgModel.getRulesSource();
            // main rules file:
            String rulesFileName = pkgModel.getRulesFileName();
            String rulesSourceName = "src/main/java/" + folderName + "/" + rulesFileName + ".java";
            String rulesSource = prettyPrinter.print( rulesSourceResult.getMainRuleClass() );
            pkgModel.logRule( rulesSource );
            byte[] rulesBytes = rulesSource.getBytes();
            srcMfs.write( rulesSourceName, rulesBytes );
            modelFiles.add( pkgName + "." + rulesFileName );
            sourceFiles.add( rulesSourceName );
            // manage additional classes, please notice to not add to modelFiles.
            for (CompilationUnit cu : rulesSourceResult.getSplitted()) {
                String addFileName = cu.findFirst( ClassOrInterfaceDeclaration.class ).get().getNameAsString();
                String addSourceName = "src/main/java/" + folderName + "/" + addFileName + ".java";
                String addSource = prettyPrinter.print( cu );
                pkgModel.logRule( addSource );
                byte[] addBytes = addSource.getBytes();
                srcMfs.write( addSourceName, addBytes );
                sourceFiles.add( addSourceName );
            }
        }

        return new Result(sourceFiles, modelFiles);
    }

    public static class Result {
        private final List<String> sourceFiles;
        private final List<String> modelFiles;

        public Result( List<String> sourceFiles, List<String> modelFiles ) {
            this.sourceFiles = sourceFiles;
            this.modelFiles = modelFiles;
        }

        public List<String> getSources() {
            return sourceFiles;
        }

        public List<String> getSourceFiles() {
            return sourceFiles;
        }

        public List<String> getModelFiles() {
            return modelFiles;
        }
    }
}
