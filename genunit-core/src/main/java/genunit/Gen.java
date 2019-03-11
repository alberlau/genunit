package genunit;

import static org.apache.commons.lang3.StringUtils.uncapitalize;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import fmpp.ProcessingException;
import fmpp.progresslisteners.ConsoleProgressListener;
import fmpp.setting.SettingException;
import fmpp.setting.Settings;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Gen {

    public static void gen(File basedir, String code) throws SettingException, ProcessingException, IOException, TemplateException {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
        combinedTypeSolver.add(new ReflectionTypeSolver());

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);
        CompilationUnit cu = StaticJavaParser.parse(code);
        extractPackageDeclaration(cu);
        extractClassName(cu);
        extractConstructorDeclaration(cu);
        extractFieldDeclarations(cu);
        extractFullFieldTypes(cu);
        extractMethodDeclaration(cu);
        extractServiceCalls(cu);

        invokeGenerator(basedir);
    }

    private static void extractServiceCalls(CompilationUnit cu) {
        cu.getChildNodes().stream()
            .map(node -> node.stream())
            .reduce(Stream.of(cu), Stream::concat)
            .filter(node -> node.getClass().getSimpleName().equals("MethodCallExpr"))
            .filter(node -> ((MethodCallExpr)node).getScope().isPresent())
            .forEach(node -> {
                MethodCallExpr methodCallExpr = (MethodCallExpr) node;
                SimpleName methodName = methodCallExpr.getName();
                String varRef = methodCallExpr.getScope().get().toString();
                Optional<FieldDeclarationMeta> fieldDeclaration =
                    ((List<FieldDeclarationMeta>) MetadataHolder.getMetadata()
                        .get("fieldDeclarations")).stream().filter(fieldDeclarationMeta -> fieldDeclarationMeta.getFieldName().equals(varRef)).findFirst();

                if (fieldDeclaration.isPresent()) {
                    FieldDeclarationMeta fieldDeclarationMeta = fieldDeclaration.get();
                    MethodDeclarationMeta methodDeclarationMeta = new MethodDeclarationMeta(methodName.asString());
                    NodeList<Expression> arguments = methodCallExpr.getArguments();
                    arguments.stream().forEach(expression -> {
                        String className = null;
                        String name = null;
                        if (expression instanceof NameExpr) {
                            name = expression.asNameExpr().getName().asString();
                        } else if (expression instanceof MethodCallExpr) {
                            name =  expression.asMethodCallExpr().getTokenRange().get().toString();
                        } else if (expression instanceof FieldAccessExpr) {
                            name =  expression.asFieldAccessExpr().getTokenRange().get().toString();
                        } else {
                            throw new RuntimeException("Unsupported: " + expression.getClass().getName());
                        }
                        try {
                            ResolvedType resolvedType = expression.calculateResolvedType();
                            className = ((ReferenceTypeImpl) resolvedType).toRawType().asReferenceType().getQualifiedName();
                        } catch (UnsolvedSymbolException e) {
                            className = e.getName();
                        }
                        methodDeclarationMeta.getParams().add(new FieldDeclarationMeta(className, name));
                    });
                    methodDeclarationMeta.setFieldReference(varRef);
                    fieldDeclarationMeta.getCalls().add(methodDeclarationMeta);

                }
            });

    }

    private static void invokeGenerator(File basedir) throws SettingException, ProcessingException, IOException, TemplateException {
        Settings settings = new Settings(new File("."));
        settings.addProgressListener(new ConsoleProgressListener());
        String output = Paths.get("output").toAbsolutePath().toString();
        File fOutputPath = new File(output);
        if(!fOutputPath.exists()) {
            fOutputPath.mkdirs();
        }

        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(Gen.class, "/templates");
        Template template = configuration.getTemplate("mockito.ftl");
        String subpath = "/build/generated/" + MetadataHolder.getMetadata().get("packageName").toString()
            .replaceAll("\\.", "/") + "/";

        File sub = new File(basedir, subpath);
        if (!sub.exists()) {
            sub.mkdirs();
        }
        File file = new File(sub, MetadataHolder.getMetadata().get("className") + "Test.java");
        template.process(MetadataHolder.getMetadata(), new FileWriter(file));
    }

    private static void extractFieldDeclarations(CompilationUnit cu) {
        cu.accept(new FieldDeclarationVisitor(), null);
    }

    private static void extractPackageDeclaration(CompilationUnit cu) {
        PackageDeclaration packageDeclaration = cu.findFirst(PackageDeclaration.class).get();
        MetadataHolder.getMetadata().put("packageName", packageDeclaration.getName().asString());
    }

    private static void extractConstructorDeclaration(CompilationUnit cu) {
        cu.findFirst(ConstructorDeclaration.class).ifPresent(constructorDeclaration -> {
            MetadataHolder.getMetadata().put("constructorArgs",
                constructorDeclaration.getParameters().stream().map(parameter -> {
                return uncapitalize(parameter.getType().asString());
            }).collect(Collectors.joining(", ")));
        });
    }

    private static void extractMethodDeclaration(CompilationUnit cu) {
        cu.findAll(MethodDeclaration.class).stream().forEach(methodDeclaration -> {
            boolean aPublic = methodDeclaration.getModifiers().stream()
                .anyMatch(modifier -> modifier.getKeyword().asString().equals("public"));
            List<MethodDeclarationMeta> methods = (List) MetadataHolder.getMetadata().get("methodDeclarations");
            if (methods == null) {
                methods = new ArrayList<>();
                MetadataHolder.getMetadata().put("methodDeclarations", methods);
            }
            if (aPublic) {
                MethodDeclarationMeta methodDeclarationMeta = new MethodDeclarationMeta(methodDeclaration.getNameAsString());
                List<FieldDeclarationMeta> params = methodDeclaration.getParameters().stream().map(parameter -> {
                    String type = parameter.getType().asString();
                    String varName = parameter.getName().getIdentifier();
                    FieldDeclarationMeta fieldDeclarationMeta = new FieldDeclarationMeta(type, varName);
                    extractFullFieldTypeFromImports(cu, fieldDeclarationMeta);
                    return fieldDeclarationMeta;
                }).collect(Collectors.toList());
                methodDeclarationMeta.setParams(params);
                methodDeclarationMeta.setMethodArgs(
                    params.stream().map(fieldDeclarationMeta ->
                        fieldDeclarationMeta.getFieldName()).collect(Collectors.joining(", ")));
                methods.add(methodDeclarationMeta);

            }
        });
    }

    private static void extractClassName(CompilationUnit cu) {
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = cu.findFirst(ClassOrInterfaceDeclaration.class).get();
        SimpleName className = classOrInterfaceDeclaration.getName();
        MetadataHolder.getMetadata().put("className", className.asString());
        MetadataHolder.getMetadata().put("classNameVar", uncapitalize(className.asString()));
    }

    private static void extractFullFieldTypes(CompilationUnit cu) {
        List<FieldDeclarationMeta> fieldDeclarationMetas =
            (List<FieldDeclarationMeta>) MetadataHolder.getMetadata().get("fieldDeclarations");

        extractFullFieldTypesFromImports(cu, fieldDeclarationMetas);
    }

    private static void extractFullFieldTypesFromImports(CompilationUnit cu,
        List<FieldDeclarationMeta> fieldDeclarationMetas) {
        fieldDeclarationMetas.stream().forEach(fieldDeclarationMeta -> {
            extractFullFieldTypeFromImports(cu, fieldDeclarationMeta);
        });
    }

    private static void extractFullFieldTypeFromImports(CompilationUnit cu, FieldDeclarationMeta fieldDeclarationMeta) {
        cu.getImports().stream().filter(importDeclaration ->
            importDeclaration.getNameAsString().indexOf(fieldDeclarationMeta.getFieldType()) > -1)
            .findFirst().ifPresent(importDeclaration ->
            fieldDeclarationMeta.setFullFieldType(importDeclaration.getNameAsString()));
    }
}
