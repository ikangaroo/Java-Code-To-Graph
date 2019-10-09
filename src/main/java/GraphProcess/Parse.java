package GraphProcess;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Parse {
    public static void main(String[] args) throws FileNotFoundException {
        // creates an input stream for the file to be parsed
        String srcfilename = "data/ClusterUnitDatabase.java";
//        String srcfilename = "data/pac4j~~~pac4j/pac4j~~~pac4j_a87e81539782494f6e01d5b1b909c827e690e202_pac4j-oidc~src~main~java~org~pac4j~oidc~client~OidcClient.java.new.java";
//        String srcfilename = "data/2.java";
        FileInputStream in = new FileInputStream(srcfilename);

        // parse the file
        CompilationUnit cu = JavaParser.parse(in);
        // Now comes the inspection code:
//        JsonPrinter jsonPrinter = new JsonPrinter(true);
//        System.out.println(jsonPrinter.output(cu));
// Now comes the inspection code:
//        DotPrinter printer = new DotPrinter(true);
//        try (FileWriter fileWriter = new FileWriter("ast.dot");
//             PrintWriter printWriter = new PrintWriter(fileWriter)) {
//            printWriter.print(printer.output(cu));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        List<ClassOrInterfaceType> classOrInterfaceTypes = cu.findAll(ClassOrInterfaceType.class);
        List<SimpleName> SimpleNamesOfClassOrInterfaceType = classOrInterfaceTypes.stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList());
        List<NameExpr> nameExprs = cu.findAll(NameExpr.class).stream().filter(nameExpr -> ! SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList());
        List<FieldAccessExpr> fieldAccessExprs = cu.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList());
        List<VariableDeclarator> variableDeclarators = cu.findAll(VariableDeclarator.class);

//        for (NameExpr nameExpr:
//                nameExprs) {
//            System.out.println(nameExpr);
//            System.out.println(nameExpr.getName().hashCode());
//            System.out.println(nameExpr.getBegin().get().line);
//            System.out.println("NameExpr========================================");
//        }
        List<VariableDeclarator> variableDeclarators1 = cu.findAll(VariableDeclarator.class);
        for (VariableDeclarator variableDeclarator:
                variableDeclarators1) {
            System.out.println(variableDeclarator);
//            System.out.println(variableDeclarator.getParentNode());
            variableDeclarator.findAll(Node.class).stream().forEach(node -> System.out.println(node));
            System.out.println(variableDeclarator.getBegin().get().line);
            System.out.println("variableDeclarator========================================");
        }

        List<FieldAccessExpr> fieldAccessExprs1 = cu.findAll(FieldAccessExpr.class);
        for (FieldAccessExpr fieldAccessExpr:
                fieldAccessExprs1) {
            System.out.println(fieldAccessExpr);
            System.out.println(fieldAccessExpr.getName().hashCode());
            System.out.println(fieldAccessExpr.getName());
            System.out.println(fieldAccessExpr.getBegin().get().line);
            System.out.println("FieldAccessExpr========================================");
        }
        List<MethodDeclaration> methodDeclarations = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration:
             methodDeclarations) {
            List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
            Boolean isThisMethodDecl = Boolean.FALSE;
            for (MethodCallExpr methodCallExpr:
                 methodCallExprs) {
                if (Objects.equals(methodCallExpr.getName().toString(), "loadMBB") && methodCallExpr.getBegin().get().line == 105) {
                    System.out.println(methodDeclaration);
                    System.out.println("=============================================");
                    System.out.println(methodCallExpr);
                    NodeList<Expression> arguments = methodCallExpr.getArguments();

                    List<NameExpr> nameExprs1 = methodDeclaration.findAll(NameExpr.class);
                    ArrayList<SimpleName> simpleNames = new ArrayList<SimpleName>();
                    methodDeclaration.findAll(VariableDeclarator.class).stream().forEach(variableDeclarator -> simpleNames.add(variableDeclarator.getName()));

                    ArrayList<NameExpr> nameExprs2 = new ArrayList<NameExpr>();
                    simpleNames.stream().forEach(simpleName -> System.out.println(simpleName.getBegin().get().line));
                    System.out.println(nameExprs2);
                    for (Node node:
                         arguments) {
                        System.out.println("arg--------------------------------------");
                        System.out.println(node);
                        System.out.println("children nodes-----------");
                        node.findAll(NameExpr.class).stream().forEach(nameExprConsumer -> {
                            System.out.println(nameExprConsumer);
//                            if (nameExprConsumer.getClass() == NameExpr.class){
                            System.out.println(nameExprConsumer.hashCode());
                            nameExprs2.add(nameExprConsumer);
                            System.out.println(nameExprs2);
                            for (NameExpr name:
                                    nameExprs2) {
                                // NameExpr
                                nameExprs1.stream().filter(nameExpr -> nameExpr.hashCode() == name.hashCode())
                                .forEach(name1 -> System.out.println(name1.getBegin().get().line));
                                // VariableDeclarator variable SimpleName
                                simpleNames.stream().filter(simpleName -> Objects.equals(simpleName.toString(), name.toString()))
                                .forEach(simpleName -> System.out.println(simpleName.getBegin().get().line));
                                }
//                            };
                        });
                    }

                    isThisMethodDecl = Boolean.TRUE;
                }
                if (isThisMethodDecl) {break;}
            }
            if (isThisMethodDecl) {break;}
        }
        /*
        List<Expression> expressions = cu.findAll(Expression.class);
        List<ObjectCreationExpr> objectCreationExprs = cu.findAll(ObjectCreationExpr.class);
        objectCreationExprs.stream()
                .forEach(f -> {
                    System.out.println("\nobjectCreationExprsSSSSSSSSSSSSSSSSSSSS");
                    System.out.println(f.toString() + "\n" + f.getRange().map(s -> s.begin.line).orElse(-1) + f.getClass() + f.hashCode());

                    f.getArguments().stream().filter(a -> a.isNameExpr() || a.isArrayAccessExpr() || a.isArrayCreationExpr()||a.isArrayInitializerExpr()||a.isAssignExpr()||a.isBinaryExpr()||a.isCastExpr()||a.isClassExpr()||a.isConditionalExpr()||a.isEnclosedExpr()||a.isFieldAccessExpr()||a.isLambdaExpr()||a.isMethodCallExpr()||a.isMethodReferenceExpr()||a.isObjectCreationExpr()||a.isUnaryExpr()||a.isVariableDeclarationExpr()||a.isInstanceOfExpr()||a.isSuperExpr()||a.isTypeExpr()).forEach(arg -> {
                        System.out.println("\nargargargargargargargargargargargarg");
                        System.out.println(arg.toString() + "\n" + arg.getRange().map(as -> as.begin.line).orElse(-1) + arg.getClass() + arg.hashCode());
                        System.out.println(":::::::::::::::::::::::::::::::");
//                        System.out.println("-------------------------------");
                        arg.findAll(Node.class).stream().forEach(x -> System.out.println(x.toString() + "\n" + x.getRange().map(xs -> xs.begin.line).orElse(-1) + x.getClass() + x.hashCode()));
//                        System.out.println("\nargargargargargargargargargargargarg");
                    });

                    System.out.println();
                });

        expressions.stream().forEach(f -> {
            System.out.println("\n++++++++++++++++++++++++++++++++++++++++Statementssssssssssssssssss");
            System.out.println(f.toString() + "\n" + f.getRange().map(s -> s.begin.line).orElse(-1) + f.getClass() + f.hashCode());
            System.out.println("++++++++++++++++++++++++++++++++++++++++");
            List<Node> childNodes = f.getChildNodes();
            System.out.println("++++++++++++++++++++++++++++++++++++++++Chlideeeeeeeeeeeeeeeeeeeee");
            childNodes.stream().forEach(fc -> {
                System.out.println("++++++++++++++++++++++++++++++++++++++++Chlideeeeeeeeeeeeeeeeeeeee");
                System.out.println(fc.toString() + "\n" + fc.getRange().map(sc -> sc.begin.line).orElse(-1) + fc.getClass() + fc.hashCode());
                List<Node> cchildNodes = fc.getChildNodes();
                System.out.println("++++++++++++++++++++++++++++++++++++++++ChlidChlidChlidChlidChlidChlid");
                cchildNodes.stream().forEach(fcc -> {
                    System.out.println(fcc.toString() + "\n" + fcc.getRange().map(scc -> scc.begin.line).orElse(-1) + fcc.getClass() + fcc.hashCode());
                });
            });

            });
        */
    }
}
