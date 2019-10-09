package GraphProcess;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class Parse_new {
    public static void main(String[] args) throws FileNotFoundException {
        String srcfilename = "data/2.java";
//        String srcfilename = "data/ClusterUnitDatabase.java";
        FileInputStream in = new FileInputStream(srcfilename);
        CompilationUnit cu = JavaParser.parse(in);

        List<ClassOrInterfaceType> classOrInterfaceTypes_all = cu.findAll(ClassOrInterfaceType.class);
        List<SimpleName> SimpleNamesOfClassOrInterfaceType_all = classOrInterfaceTypes_all.stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList());
        ArrayList<VariableDeclarator> field_variableDeclarators = new ArrayList<VariableDeclarator>();
        cu.findAll(FieldDeclaration.class).forEach((FieldDeclaration fieldDeclaration) -> {
            field_variableDeclarators.addAll(new ArrayList<>(fieldDeclaration.findAll(VariableDeclarator.class)));
        });
        System.out.println(field_variableDeclarators);

//        List<SynchronizedStmt> synchronizedStmt_of_methodCall1 = cu.findAll(SynchronizedStmt.class);
//        synchronizedStmt_of_methodCall1.forEach(synchronizedStmt -> System.out.println(synchronizedStmt.getExpression()));
        List<MethodDeclaration> methodDeclarations_all = cu.findAll(MethodDeclaration.class);
        for (MethodDeclaration methodDeclaration:
                methodDeclarations_all) {
            List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
            Boolean isThisMethodDecl = Boolean.FALSE;
            for (MethodCallExpr methodCallExpr: methodCallExprs) {
                // 找到所需函数所在的方法声明
                String methodCall_name = "arraycopy";
                int methodCall_line = 236;
                if (Objects.equals(methodCallExpr.getName().toString(), methodCall_name) && methodCallExpr.getBegin().get().line == methodCall_line) {
//                if (Objects.equals(methodCallExpr.getName().toString(), "loadMBB") && methodCallExpr.getBegin().get().line == 113) {
//                    System.out.println(methodDeclaration);
                    System.out.println("=============================================");
                    System.out.println(methodCallExpr);
                    System.out.println("=============================================");
                    NodeList<Expression> arguments = methodCallExpr.getArguments();
                    ArrayList<Parameter> parameters_of_methodDeclaration = new ArrayList<>(methodDeclaration.getParameters());

//                    List<NameExpr> nameExprs_of_methodCall = methodDeclaration.findAll(NameExpr.class).stream().filter(nameExpr -> ! SimpleNamesOfClassOrInterfaceType_all.contains(nameExpr.getName())).collect(Collectors.toList());
//                    List<FieldAccessExpr> fieldAccessExprs_of_methodCall = methodDeclaration.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList());
                    // 此方法声明的所有变量声明、方法调用、赋值
                    List<VariableDeclarator> variableDeclarators_of_methodCall = methodDeclaration.findAll(VariableDeclarator.class);
//                    List<MethodCallExpr> methodCallExprs_of_methodCall = methodDeclaration.findAll(MethodCallExpr.class);
                    List<AssignExpr> assignExprs_of_methodCall = methodDeclaration.findAll(AssignExpr.class);
                    // 将arguments中的 NameExpr 与 FieldAccessExpr 放入 _want ArrayList 中
                    ArrayList<NameExpr> nameExprs_want = new ArrayList<NameExpr>();
                    ArrayList<FieldAccessExpr> fieldAccessExpr_want = new ArrayList<FieldAccessExpr>();
                    ArrayList<VariableDeclarator> variableDeclarator_want = new ArrayList<VariableDeclarator>();
                    ArrayList<AssignExpr> assignExpr_want = new ArrayList<AssignExpr>();
                    ArrayList<NameExpr> args_finalNameExprs_want = nameExprs_want;
                    ArrayList<FieldAccessExpr> args_finalFieldAccessExpr_want = fieldAccessExpr_want;
                    arguments.forEach(call_args -> {
                        args_finalNameExprs_want.addAll(call_args.findAll(NameExpr.class).stream().filter(nameExpr -> !SimpleNamesOfClassOrInterfaceType_all.contains(nameExpr.getName())).collect(Collectors.toList()));
                        args_finalFieldAccessExpr_want.addAll(call_args.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                    });
//                    System.out.println("STARTTTTTTTTTTTTTTTTTTTTTTTTTTTTTWANTTTTTTTTTTTTTTT");
//                    System.out.println(nameExprs_want);
//                    System.out.println(fieldAccessExpr_want);
//                    System.out.println("ENDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDWANTTTTTTTTTTTTTTT");
//                    System.out.println("STARTTTTTTTTTTTTTTTTTTTTTTTTTTTTTWANTTTTTTTTTTTTTTT");
//                    nameExprs_want = args_finalNameExprs_want;
//                    fieldAccessExpr_want = args_finalFieldAccessExpr_want;
//                    System.out.println(nameExprs_want);
//                    System.out.println(fieldAccessExpr_want);
//                    System.out.println("ENDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDWANTTTTTTTTTTTTTTT");
//                    System.out.println(assignExprs_of_methodCall);

                    ArrayList<NameExpr> nameExprs_want_temp = new ArrayList<NameExpr>();
                    ArrayList<FieldAccessExpr> fieldAccessExpr_want_temp = new ArrayList<FieldAccessExpr>();
                    ArrayList<NameExpr> nameExprs_want_done = new ArrayList<NameExpr>();
                    ArrayList<FieldAccessExpr> fieldAccessExpr_want_done = new ArrayList<FieldAccessExpr>();

                    Boolean ifMap = Boolean.TRUE;
                    while (ifMap) {
                        System.out.println("MMMMMMMMMMMMMMMMMMMMMMMMMMAAAAAAAAAAAAAAAAAAAAAAAAPPPPPPPPPPPPPPPP");
                        for (NameExpr name : nameExprs_want) {
                            // NameExpr
//                        nameExprs_of_methodCall.stream().filter(nameExpr -> nameExpr.hashCode() == name.hashCode())
//                                .forEach(name1 -> System.out.println(name1.getBegin().get().line));
                            // VariableDeclarator variable SimpleName
//                        simpleNames.stream().filter(simpleName -> Objects.equals(simpleName.toString(), name.toString()))
//                                .forEach(simpleName -> System.out.println(simpleName.getBegin().get().line));
                            if (nameExprs_want_done.contains(name)) {
                                continue;
                            }
                            System.out.println("BEGIN==============================================NameExpr");
                            for (VariableDeclarator variableDeclarator : variableDeclarators_of_methodCall) {
                                if (Objects.equals(variableDeclarator.getName().toString(), name.toString())) {
                                    variableDeclarator_want.add(variableDeclarator);
                                    if (variableDeclarator.getInitializer().isPresent()) {
                                        System.out.println("variableDeclarator------------start");
                                        System.out.println(variableDeclarator);
                                        nameExprs_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(NameExpr.class).stream().filter(nameExpr -> !SimpleNamesOfClassOrInterfaceType_all.contains(nameExpr.getName())).collect(Collectors.toList()));
                                        fieldAccessExpr_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                                        System.out.println(nameExprs_want_temp);
                                        System.out.println(fieldAccessExpr_want_temp);
                                        System.out.println("variableDeclarator------------end");
                                    }
                                }
                            }
                            for (AssignExpr assignExpr : assignExprs_of_methodCall) {
                                if (Objects.equals(assignExpr.getTarget().toString(), name.toString())) {
                                    System.out.println("assignExpr------------start");
                                    System.out.println(assignExpr);
                                    assignExpr_want.add(assignExpr);
                                    nameExprs_want_temp.addAll(assignExpr.getValue().findAll(NameExpr.class).stream().filter(nameExpr -> !SimpleNamesOfClassOrInterfaceType_all.contains(nameExpr.getName())).collect(Collectors.toList()));
                                    fieldAccessExpr_want_temp.addAll(assignExpr.getValue().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                                    System.out.println(nameExprs_want_temp);
                                    System.out.println(fieldAccessExpr_want_temp);
                                    System.out.println("assignExpr------------end");
                                }
                            }
                            nameExprs_want_done.add(name);
                        }
                        for (FieldAccessExpr fieldAccessExpr_of_want: fieldAccessExpr_want) {
                            if (fieldAccessExpr_want_done.contains(fieldAccessExpr_of_want)) {
                                continue;
                            }
                            System.out.println("BEGIN==============================================FieldAccessExpr");
                            for (VariableDeclarator variableDeclarator : field_variableDeclarators) {
                                if (Objects.equals(variableDeclarator.getName().toString(), fieldAccessExpr_of_want.getName().toString())) {
                                    variableDeclarator_want.add(variableDeclarator);
                                    if (variableDeclarator.getInitializer().isPresent()) {
                                        System.out.println("field_variableDeclarator------------start");
                                        System.out.println(variableDeclarator);
                                        nameExprs_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(NameExpr.class).stream().filter(nameExpr -> !SimpleNamesOfClassOrInterfaceType_all.contains(nameExpr.getName())).collect(Collectors.toList()));
                                        fieldAccessExpr_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                                        System.out.println(nameExprs_want_temp);
                                        System.out.println(fieldAccessExpr_want_temp);
                                        System.out.println("variableDeclarator------------end");
                                    }
                                }
                            }

                            for (AssignExpr assignExpr : assignExprs_of_methodCall) {
                                if (Objects.equals(assignExpr.getTarget().toString(), fieldAccessExpr_of_want.toString())) {
                                    System.out.println("assignExpr------------start");
                                    System.out.println(assignExpr);
                                    assignExpr_want.add(assignExpr);
                                    nameExprs_want_temp.addAll(assignExpr.getValue().findAll(NameExpr.class).stream().filter(nameExpr -> !SimpleNamesOfClassOrInterfaceType_all.contains(nameExpr.getName())).collect(Collectors.toList()));
                                    fieldAccessExpr_want_temp.addAll(assignExpr.getValue().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                                    System.out.println(nameExprs_want_temp);
                                    System.out.println(fieldAccessExpr_want_temp);
                                    System.out.println("assignExpr------------end");
                                }
                            }


                        }
                        nameExprs_want = nameExprs_want_temp;
                        nameExprs_want_temp = new ArrayList<NameExpr>();
                        fieldAccessExpr_want = fieldAccessExpr_want_temp;
                        fieldAccessExpr_want_temp = new ArrayList<FieldAccessExpr>();
                        System.out.println(nameExprs_want);
                        System.out.println(fieldAccessExpr_want);
                        if (nameExprs_want.isEmpty() && fieldAccessExpr_want.isEmpty()) {ifMap = Boolean.FALSE;}
                    }
                    System.out.println(variableDeclarator_want);
                    System.out.println(assignExpr_want);
                    ArrayList<VariableDeclarator> final_variableDeclarator_want = new ArrayList<>();
                    ArrayList<AssignExpr> final_assignExpr_want = new ArrayList<>();
                    ArrayList<Parameter> final_parameter_want = new ArrayList<>();
                    for (Parameter parameter: parameters_of_methodDeclaration) {
                        for (NameExpr nameExpr: nameExprs_want_done) {
                            if (Objects.equals(parameter.getName().toString(), nameExpr.toString())) {
                                final_parameter_want.add(parameter);
                                break;
                            }
                        }
                    }
                    for (VariableDeclarator variableDeclarator: variableDeclarator_want) {
                        if (final_variableDeclarator_want.contains(variableDeclarator)){continue;}
                        final_variableDeclarator_want.add(variableDeclarator);
                    }
                    for (AssignExpr assignExpr: assignExpr_want) {
                        if (final_assignExpr_want.contains(assignExpr)){continue;}
                        final_assignExpr_want.add(assignExpr);
                    }
                    final_variableDeclarator_want.forEach(fi -> {
                                System.out.println(fi.getBegin().get().line);
                                System.out.println(fi);
                            });
                    final_assignExpr_want.forEach(fi -> {
                                System.out.println(fi.getBegin().get().line);
                                System.out.println(fi);
                            });
                    final_parameter_want.forEach(fi -> {
                                System.out.println(fi.getBegin().get().line);
                                System.out.println(fi);
                            });
                    List<Node> final_nodes_want = new ArrayList<>();
                    final_nodes_want.addAll(final_variableDeclarator_want);
                    final_nodes_want.addAll(final_assignExpr_want);
                    final_nodes_want.addAll(final_parameter_want);
                    final_nodes_want = final_nodes_want.stream().filter(fi -> fi.getBegin().get().line < methodCall_line).collect(Collectors.toList());
                    Collections.sort(final_nodes_want, new Comparator<Node>() {
                        @Override
                        public int compare(Node o1, Node o2) {
                            return o1.getBegin().get().line - o2.getBegin().get().line;
                        }
                    });
                    System.out.println("___________________");
                    final_nodes_want.forEach(fi -> {
                        System.out.println(fi.getBegin().get().line);
                        System.out.println(fi.toString());
                    });

                    isThisMethodDecl = Boolean.TRUE;
                }
                if (isThisMethodDecl) {break;}
            }
            if (isThisMethodDecl) {break;}
        }

    }
}
