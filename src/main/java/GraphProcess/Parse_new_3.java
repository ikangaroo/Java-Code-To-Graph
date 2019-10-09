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
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Parse_new_3 {
    class EmpComparator implements Comparator<Node> {
        public int compare(Node faultResult1, Node faultResult2) {
            int cr = 0;
            //先按line排升序
            int a = faultResult2.getBegin().get().line -faultResult1.getBegin().get().line;
            if (a != 0) {
                cr = (a < 0) ? 3 : -1;     // "<"升序     ">"降序
            } else {
                //再按column排升序
                a =  faultResult2.getBegin().get().column -  faultResult1.getBegin().get().column;
                if (a != 0) {
                    cr = (a < 0) ? 2 : -2; // "<"升序     ">"降序
                }
            }
            return cr;
        }
    }
    public String srcfilename;
    private CompilationUnit cu;
    private List<SimpleName> SimpleNamesOfClassOrInterfaceType;
    private ArrayList<VariableDeclarator> field_variableDeclarators = new ArrayList<VariableDeclarator>();
    public List<MethodCallExpr> methodCallExprs = new ArrayList<MethodCallExpr>();
    private List<MethodDeclaration> methodDeclarations = new ArrayList<MethodDeclaration>();
    private List<ClassExpr> classExprs = new ArrayList<>();
    public List<ConditionalExpr> conditionalExprs = new ArrayList<ConditionalExpr>();
    public Set<String> primitive= new TreeSet<>();
//    private List<>

    Parse_new_3(String srcFile) throws FileNotFoundException {
        try {
            this.srcfilename = srcFile;
            FileInputStream in = new FileInputStream(this.srcfilename);
            this.cu = JavaParser.parse(in);
            List<ClassOrInterfaceType> classOrInterfaceTypes_all = this.cu.findAll(ClassOrInterfaceType.class);
            this.SimpleNamesOfClassOrInterfaceType = classOrInterfaceTypes_all.stream().map(ClassOrInterfaceType::getName).collect(Collectors.toList());
            this.methodCallExprs = this.cu.findAll(MethodCallExpr.class);
            this.methodDeclarations = this.cu.findAll(MethodDeclaration.class);
            this.conditionalExprs=this.cu.findAll(ConditionalExpr.class);
            this.classExprs=this.cu.findAll(ClassExpr.class);
            this.cu.findAll(FieldDeclaration.class).forEach((FieldDeclaration fieldDeclaration) -> {
                this.field_variableDeclarators.addAll(new ArrayList<>(fieldDeclaration.findAll(VariableDeclarator.class)));
            });
        }catch (Exception e){
            System.out.println(e);
        }
        String[] pri={"boolean","char","byte","short","int","long","float","double","string"};//加了string
        for(String a:pri){
            this.primitive.add(a);
        }


    }

    public List<Node> getSliceOfMethod(MethodCallExpr methodCallExpr, MethodDeclaration methodDeclaration) {
        // 找到所需函数所在的方法声明
        int methodCall_line = methodCallExpr.getBegin().get().line;
            NodeList<Expression> arguments = methodCallExpr.getArguments();
            ArrayList<Parameter> parameters_of_methodDeclaration = new ArrayList<>(methodDeclaration.getParameters());
            List<IfStmt> ifStmts= methodDeclaration.findAll(IfStmt.class);
            List<TryStmt> tryStmts= methodDeclaration.findAll(TryStmt.class);
            List<MethodCallExpr> all_methodcalls = methodDeclaration.findAll(MethodCallExpr.class);
//            List<MethodCallExpr> all_methodcalls=this.methodCallExprs;
            List<VariableDeclarator> variableDeclarators_of_methodCall = methodDeclaration.findAll(VariableDeclarator.class);
            List<AssignExpr> assignExprs_of_methodCall = methodDeclaration.findAll(AssignExpr.class);
            // 将arguments中的 NameExpr 与 FieldAccessExpr 放入 _want ArrayList 中
            ArrayList<NameExpr> nameExprs_want = new ArrayList<NameExpr>();
            ArrayList<FieldAccessExpr> fieldAccessExpr_want = new ArrayList<FieldAccessExpr>();
            ArrayList<VariableDeclarator> variableDeclarator_want = new ArrayList<VariableDeclarator>();
            ArrayList<AssignExpr> assignExpr_want = new ArrayList<AssignExpr>();
            ArrayList<NameExpr> args_finalNameExprs_want = nameExprs_want;
            ArrayList<FieldAccessExpr> args_finalFieldAccessExpr_want = fieldAccessExpr_want;
            ArrayList<Expression> args_finalconditionalExprs_want = new ArrayList<>();
            List<ConditionalExpr> conditionalExprs_ = methodDeclaration.findAll(ConditionalExpr.class);
//            for(ConditionalExpr con:conditionalExprs_){
//                 System.out.println(con);
//             }
            arguments.forEach(call_args -> {
                args_finalNameExprs_want.addAll(call_args.findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
                args_finalFieldAccessExpr_want.addAll(call_args.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
            });
            for (VariableDeclarator v: variableDeclarators_of_methodCall) {
                if (v.getBegin().get().line== methodCall_line){
                    args_finalNameExprs_want.addAll(methodDeclaration.findAll(NameExpr.class).stream().filter(fi -> fi.getBegin().get().line > methodCall_line && Objects.equals(v.getNameAsString(), fi.toString())).collect(Collectors.toList()));
                }
            }
            args_finalNameExprs_want.addAll(methodDeclaration.findAll(NameExpr.class).stream().filter(fi -> fi.getBegin().get().line == methodCall_line && !this.SimpleNamesOfClassOrInterfaceType.contains(fi.getName())).collect(Collectors.toList()));
            args_finalFieldAccessExpr_want.addAll(methodDeclaration.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
            methodCallExpr.getScope().ifPresent(expression -> {
                args_finalNameExprs_want.addAll(expression.findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
                args_finalFieldAccessExpr_want.addAll(expression.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
            });

            for(IfStmt i:ifStmts){
                ArrayList<NameExpr> finalNameExprs_want = nameExprs_want;
                int size=i.getCondition().findAll(NameExpr.class).stream().filter(nameExpr -> finalNameExprs_want.contains(nameExpr)).collect(Collectors.toList()).size();
                if(size!=0){
//                    StringLiteralExpr ifstr=new StringLiteralExpr("if ("+i.getCondition()+")");
//                    args_finalconditionalExprs_want.add(ifstr);
                    args_finalconditionalExprs_want.add(i.getCondition());
//                    System.out.println("hhhhhhhhhhhhhhh");
//                    System.out.println(i.getCondition());
//                    System.out.println(i.);
                }


            }
//        for(TryStmt i:tryStmts){
//            ArrayList<NameExpr> finalNameExprs_want = nameExprs_want;
//            for (ConditionalExpr con :conditionalExprs){
//
//            }
//            int size=i.getTryBlock().findAll(NameExpr.class).stream().filter(nameExpr -> finalNameExprs_want.contains(nameExpr)).collect(Collectors.toList()).size();
//            if(size!=0){
//                args_finalconditionalExprs_want.add(i.getCondition());
////                    System.out.println(i.);
//            }
//
//
//        }

//            System.out.println(args_finalconditionalExprs_want);
//            MethodCallExpr a =new MethodCallExpr();
//            a.getScope()
            ArrayList<NameExpr> nameExprs_want_temp = new ArrayList<NameExpr>();
            ArrayList<FieldAccessExpr> fieldAccessExpr_want_temp = new ArrayList<FieldAccessExpr>();
            ArrayList<NameExpr> nameExprs_want_done = new ArrayList<NameExpr>();
            ArrayList<FieldAccessExpr> fieldAccessExpr_want_done = new ArrayList<FieldAccessExpr>();
            ArrayList<MethodCallExpr> methodCallExprs_want=new ArrayList<>();

            Boolean ifMap = Boolean.TRUE;
            // for variable is a field in fact
            List<NameExpr> nameExprs_not_decl = new ArrayList<>();
            while (ifMap) {
                for (NameExpr name : nameExprs_want) {
                    if (nameExprs_want_done.contains(name)) {
                        continue;
                    }
                    // for variable is a field in fact
                    nameExprs_not_decl.add(name);

                    for (VariableDeclarator variableDeclarator : variableDeclarators_of_methodCall) {
                        if (Objects.equals(variableDeclarator.getNameAsString(), name.toString())) {
                            variableDeclarator_want.add(variableDeclarator);
                            // for variable is a field in fact
                            nameExprs_not_decl.remove(name);

                            if (variableDeclarator.getInitializer().isPresent()) {
                                nameExprs_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
                                fieldAccessExpr_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                            }
                        }
                    }

                    for (AssignExpr assignExpr : assignExprs_of_methodCall) {
                        if (Objects.equals(assignExpr.getTarget().toString(), name.toString())) {
                            assignExpr_want.add(assignExpr);
                            nameExprs_want_temp.addAll(assignExpr.getValue().findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
                            fieldAccessExpr_want_temp.addAll(assignExpr.getValue().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                        }
                    }
                    nameExprs_want_done.add(name);

                }
                for (FieldAccessExpr fieldAccessExpr_of_want : fieldAccessExpr_want) {
                    if (fieldAccessExpr_want_done.contains(fieldAccessExpr_of_want)) {
                        continue;
                    }
                    for (VariableDeclarator variableDeclarator : this.field_variableDeclarators) {
                        if (Objects.equals(variableDeclarator.getNameAsString(), fieldAccessExpr_of_want.getNameAsString())) {
                            variableDeclarator_want.add(variableDeclarator);
                            if (variableDeclarator.getInitializer().isPresent()) {
                                nameExprs_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
                                fieldAccessExpr_want_temp.addAll(variableDeclarator.getInitializer().get().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                            }
                        }
                    }

                    for (AssignExpr assignExpr : assignExprs_of_methodCall) {
                        if (Objects.equals(assignExpr.getTarget().toString(), fieldAccessExpr_of_want.toString())) {
                            assignExpr_want.add(assignExpr);
                            nameExprs_want_temp.addAll(assignExpr.getValue().findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
                            fieldAccessExpr_want_temp.addAll(assignExpr.getValue().findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
                        }
                    }
                    fieldAccessExpr_want_done.add(fieldAccessExpr_of_want);


                }


                ArrayList<NameExpr> finalNameExprs_want1 = nameExprs_want;
                ArrayList<NameExpr> finalNameExprs_want_temp = nameExprs_want_temp;
                all_methodcalls.forEach(methodCallExpr1 -> methodCallExpr1.getScope().ifPresent(expression -> {
                    List<NameExpr> name_tmp=expression.findAll(NameExpr.class).stream().filter(nameExpr ->  finalNameExprs_want1.contains(nameExpr)).collect(Collectors.toList());


//                    System.out.println("name_sets"+name_sets);
//                    System.out.println("nameExprs_want_temp"+nameExprs_want_temp);


                    if(!methodCallExprs_want.contains(methodCallExpr1)&&name_tmp.size()!=0) {

                        methodCallExprs_want.add(methodCallExpr1);
                        List<NameExpr> name_sets=methodCallExpr1.findAll(NameExpr.class);
                        List<NameExpr> name_sets_=methodCallExpr1.findAll(NameExpr.class);
//                        System.out.println("here"+name_sets+"\n");

                        name_sets.retainAll(finalNameExprs_want_temp);
//                        System.out.println(name_sets+"namesets_\n");
                        name_sets_.removeAll(finalNameExprs_want_temp);
//                        System.out.println(name_sets_+"namesets_\n");
                        finalNameExprs_want_temp.addAll(name_sets_);
//                        System.out.println(finalNameExprs_want_temp+"finalnam\n");
                    }

                        }));

                nameExprs_want_temp=finalNameExprs_want_temp;
                nameExprs_want = nameExprs_want_temp;
                nameExprs_want_temp = new ArrayList<NameExpr>();
                fieldAccessExpr_want = fieldAccessExpr_want_temp;
                fieldAccessExpr_want_temp = new ArrayList<FieldAccessExpr>();
                if (nameExprs_want.isEmpty() && fieldAccessExpr_want.isEmpty()) {
                    ifMap = Boolean.FALSE;
                }
            }
            ArrayList<VariableDeclarator> final_variableDeclarator_want = new ArrayList<>();
            ArrayList<Node> final_variableDeclaratorParentNode_want = new ArrayList<>();
            ArrayList<AssignExpr> final_assignExpr_want = new ArrayList<>();
            ArrayList<Parameter> final_parameter_want = new ArrayList<>();
            for (Parameter parameter : parameters_of_methodDeclaration) {
                for (NameExpr nameExpr : nameExprs_want_done) {
                    if (Objects.equals(parameter.getNameAsString(), nameExpr.toString())) {
                        final_parameter_want.add(parameter);
                        // for variable is a field in fact
                        nameExprs_not_decl.remove(nameExpr);

                        break;
                    }
                }
            }
            // for variable is a field in fact
            for (NameExpr nameExpr: nameExprs_not_decl) {
//                System.out.println(nameExpr.toString());
                for (VariableDeclarator variableDeclarator : this.field_variableDeclarators) {
                    if (Objects.equals(variableDeclarator.getNameAsString(), nameExpr.getNameAsString())) {
                        if (variableDeclarator_want.contains(variableDeclarator)){
                        }else {
                            variableDeclarator_want.add(variableDeclarator);
                        }
                    }
                }
            }

            for (VariableDeclarator variableDeclarator : variableDeclarator_want) {
                if (final_variableDeclarator_want.contains(variableDeclarator)) {
                    continue;
                }
                final_variableDeclarator_want.add(variableDeclarator);
                final_variableDeclaratorParentNode_want.add(variableDeclarator.getParentNode().get().removeComment());
            }
            for (AssignExpr assignExpr : assignExpr_want) {
                if (final_assignExpr_want.contains(assignExpr)) {
                    continue;
                }
                final_assignExpr_want.add(assignExpr);
            }
            List<Node> final_nodes_want = new ArrayList<>();
//            final_nodes_want.addAll(final_variableDeclarator_want);
            final_nodes_want.addAll(final_variableDeclaratorParentNode_want);
            final_nodes_want.addAll(final_assignExpr_want);
            final_nodes_want.addAll(final_parameter_want);
            final_nodes_want.addAll(methodCallExprs_want);
            final_nodes_want.addAll(args_finalconditionalExprs_want);

            System.out.println();
//                System.out.println(final_nodes_want.size());
//            final_nodes_want = final_nodes_want.stream().filter(fi ->fi.getBegin().get().line < methodCall_line).collect(Collectors.toList());

//            final_nodes_want = final_nodes_want.stream().filter(fi -> fi.getBegin().get().line < methodCall_line).map(fi->fi.getParentNode().get()).collect(Collectors.toList());

            final_nodes_want.sort(new Comparator<Node>() {
                    @Override
                    public int compare(Node o1, Node o2) {
                        return o1.getBegin().get().line - o2.getBegin().get().line;

                    }
                });
            ;

//            final_nodes_want.add(methodCallExpr);


            return final_nodes_want;

    }

    public HashMap<String,String> renameVariableName(List<Node> nodeLists) {

        List<Node> nodeList=new ArrayList<>();
        for(Node n:nodeLists){
            nodeList.add(n);
        }
        int i = 1;
        int j = 1;
        int k=1;

        ArrayList<NameExpr> nameExprs = new ArrayList<NameExpr>();
        ArrayList<FieldAccessExpr> fieldAccessExprs = new ArrayList<FieldAccessExpr>();
        ArrayList<VariableDeclarator> variableDeclarators = new ArrayList<VariableDeclarator>();
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        ArrayList<ClassOrInterfaceType> classOrInterfaceTypes= new ArrayList<ClassOrInterfaceType>();
        nodeList.forEach(node -> {
            nameExprs.addAll(node.findAll(NameExpr.class).stream().filter(nameExpr -> !this.SimpleNamesOfClassOrInterfaceType.contains(nameExpr.getName())).collect(Collectors.toList()));
            fieldAccessExprs.addAll(node.findAll(FieldAccessExpr.class).stream().filter(fieldAccessExpr -> fieldAccessExpr.getScope().isThisExpr()).collect(Collectors.toList()));
            variableDeclarators.addAll(node.findAll(VariableDeclarator.class).stream().filter(variableDeclarator -> !this.field_variableDeclarators.contains(variableDeclarator)).collect(Collectors.toList()));
            parameters.addAll(new ArrayList<>(node.findAll(Parameter.class)));
            classOrInterfaceTypes.addAll(new ArrayList<>(node.findAll(ClassOrInterfaceType.class)));

        });

        ArrayList<VariableDeclarator> variableDeclarators_done = new ArrayList<>();



        parameters.sort(new EmpComparator());
        variableDeclarators.sort(new EmpComparator());
        this.field_variableDeclarators.sort(new EmpComparator());
        HashMap<String,String> field_done = new HashMap<String,String>();
        HashMap<String,String> variableDeclarator_done = new HashMap<String,String>();
        HashMap<String,String> class_done = new HashMap<String,String>();
        HashMap<String,String> changename=new HashMap<>();
//        System.out.println(fieldAccessExprs);
        for (VariableDeclarator variableDeclarator: this.field_variableDeclarators) {
            Boolean isGet = Boolean.FALSE;
            for (FieldAccessExpr fieldAccessExpr: fieldAccessExprs) {
                if (Objects.equals(variableDeclarator.getNameAsString(), fieldAccessExpr.getNameAsString())) {

                    changename.put( fieldAccessExpr.getNameAsString(),"var" + i);
//                    fieldAccessExpr.setName("var" + i);
                    isGet = Boolean.TRUE;
                }
            }
            if (isGet) {
                changename.put( variableDeclarator.getNameAsString(),"var" + i);
//                variableDeclarator.setName("var" + i);

                i++;
            }
        }
        for (Parameter parameter: parameters) {
            for (NameExpr nameExpr: nameExprs) {
                if (Objects.equals(parameter.getNameAsString(), nameExpr.getNameAsString())) {

                    changename.put( nameExpr.getNameAsString(),"var" + i);
//                    nameExpr.setName("var" + i);
                }
            }
            changename.put( parameter.getNameAsString(),"var" + i);
//            parameter.setName("var" + i);
            i++;
        }
        for (VariableDeclarator variableDeclarator: variableDeclarators) {
            for (NameExpr nameExpr: nameExprs) {
                if (Objects.equals(variableDeclarator.getNameAsString(), nameExpr.getNameAsString())) {

                    changename.put( nameExpr.getNameAsString(),"var" + i);
//                    nameExpr.setName("var" + i);
                }
            }
            variableDeclarator_done.put(variableDeclarator.getNameAsString(), "var" +i);
            variableDeclarators_done.add(variableDeclarator);
            changename.put( variableDeclarator.getNameAsString(),"var" + i);
//            variableDeclarator.setName("var" + i);

            i++;
        }
        for (VariableDeclarator variableDeclarator: variableDeclarators) {
            if (!variableDeclarators_done.contains(variableDeclarator)) {
                if (variableDeclarator_done.containsKey(variableDeclarator.getNameAsString())) {
//                    variableDeclarator.setName((String) variableDeclarator_done.get(variableDeclarator.getNameAsString()));
                    continue;
                }
//                variableDeclarator.setName("unused");
            }
        }



        for (ClassOrInterfaceType classOrInterfacetype:classOrInterfaceTypes) {
            if (!this.primitive.contains(classOrInterfacetype.getNameAsString().toLowerCase())) {
                if (class_done.containsKey(classOrInterfacetype.getNameAsString())) {
//                    classOrInterfacetype.setName((String) class_done.get(classOrInterfacetype.getNameAsString()));
                    continue;
                }
                for (VariableDeclarator variableDeclarator : variableDeclarators) {
                    if (Objects.equals(variableDeclarator.getNameAsString(), classOrInterfacetype.getNameAsString())) {

                        changename.put( variableDeclarator.getNameAsString(),"class" + i);
//                        variableDeclarator.setName("class" + i);
                        variableDeclarators_done.add(variableDeclarator);
                    }
                }
                changename.put( classOrInterfacetype.getNameAsString(),"class" + k);
                class_done.put(classOrInterfacetype.getNameAsString(), "class" + k);
//                classOrInterfacetype.setName("class" + k);

                k++;
            }
        }

        for (VariableDeclarator variableDeclarator: variableDeclarators) {
            if (variableDeclarators_done.contains(variableDeclarator)){}
//            variableDeclarator.setName("unused");
        }

        class_done.putAll(variableDeclarator_done);

       return changename;

    }

    public ArrayList<MethodCallExpr> getMethodCallOfline(int line){
        ArrayList<MethodCallExpr> methodCallExprsOfline = new ArrayList<MethodCallExpr>();
        for (MethodCallExpr methodCallExpr: this.methodCallExprs) {
            if (methodCallExpr.getBegin().get().line == line){methodCallExprsOfline.add(methodCallExpr);}
        }
        return methodCallExprsOfline;
    }

    public List<Node> getSliceOfMethodCallOfLine(int methodCall_line, String methodCall_name) {
//        List<MethodDeclaration> methodDeclarations_new = this.methodDeclarations;
        List<MethodDeclaration> md = new ArrayList<>();
        MethodCallExpr mc = new MethodCallExpr();
//        methodDeclarations_new.sort(new GraphProcess.NodeComparator());
//        int s = methodDeclarations_new.size();
        for (MethodDeclaration methodDeclaration :
                this.methodDeclarations) {
            List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
            Boolean isThisMethodDecl = Boolean.FALSE;
            for (MethodCallExpr methodCallExpr : methodCallExprs) {
                // 方法调用的 名字、行号
                if (Objects.equals(methodCallExpr.getNameAsString(), methodCall_name) && methodCallExpr.getBegin().get().line == methodCall_line) {
                    // 获取方法调用的参数的代码段，作为返回值
                    md.add(methodDeclaration);
                    mc = methodCallExpr;
//                    return this.getSliceOfMethod(methodCallExpr, methodDeclaration);
                }
            }
        }
        if (md.isEmpty()){
            return null;
        }
        else if (md.size() == 1){
            return this.getSliceOfMethod(mc, md.get(0));
        }else {
            md.sort(new EmpComparator());
            return this.getSliceOfMethod(mc, md.get(md.size()-1));
        }

//        for (int i = s - 1; i >=0 ; i--) {
//            List<MethodCallExpr> methodCallExprs = methodDeclarations_new.get(i).findAll(MethodCallExpr.class);
//            Boolean isThisMethodDecl = Boolean.FALSE;
//            for (MethodCallExpr methodCallExpr : methodCallExprs) {
//                // 方法调用的 名字、行号
//                if (Objects.equals(methodCallExpr.getNameAsString(), methodCall_name) && methodCallExpr.getBegin().get().line == methodCall_line) {
//                    // 获取方法调用的参数的代码段，作为返回值
//
//                    return this.getSliceOfMethod(methodCallExpr, methodDeclarations_new.get(i));
//                }
//            }
////        }
//        }

//        return null;
//        for (MethodDeclaration methodDeclaration :
//                methodDeclarations_new) {
//            List<MethodCallExpr> methodCallExprs = methodDeclaration.findAll(MethodCallExpr.class);
//            Boolean isThisMethodDecl = Boolean.FALSE;
//            for (MethodCallExpr methodCallExpr : methodCallExprs) {
//                // 方法调用的 名字、行号
//                if (Objects.equals(methodCallExpr.getNameAsString(), methodCall_name) && methodCallExpr.getBegin().get().line == methodCall_line) {
//                    // 获取方法调用的参数的代码段，作为返回值
//
//                    return this.getSliceOfMethod(methodCallExpr, methodDeclaration);
//                }
//            }
//        }

    }


    public Map<String,ArrayList> readfile(String filepath) throws IOException {
        Map<String, ArrayList> filename_deletion = new HashMap<String, ArrayList>();
        BufferedReader reader;
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(filepath)));
        String lineStr;
        while ((lineStr = reader.readLine()) != null) {
                    lineStr.split(":");
        }
        return  filename_deletion;
    }

    public static void main(String[] args) throws FileNotFoundException {
//        String srcfilename=args[0];
//        int want_line= Integer.parseInt(args[1]);
        String datapath="data/pac4j~~~pac4j/pac4j~~~pac4j_a87e81539782494f6e01d5b1b909c827e690e202_pac4j-oidc~src~main~java~org~pac4j~oidc~client~OidcClient.java.new.java";

        Parse_new_3 p = new Parse_new_3(datapath);

//        System.out.println("now is Parsing : " + p.srcfilename);

        // 示例：得到 某行 所有的方法调用，存储到节点列表methodCallExprsOfline，打印输出
//        int want_line = 111;
//        ArrayList<MethodCallExpr> methodCallExprsOfline = p.getMethodCallOfline(want_line);
////        System.out.println("\nMethodCallExpr name in " + want_line);
//        methodCallExprsOfline.forEach(methodCallExpr -> System.out.println(methodCallExpr.getName()));
//--------------------------------------
        // 示例：得到（某行 某个方法名）的，方法调用的参数的代码段，存储到节点列表final_nodes_want，//然后对结果进行变量名重命名
        // 方法调用的 名字、行号
        String methodCall_name = "redirect";
        int methodCall_line =227;
        List<Node> final_nodes_want = p.getSliceOfMethodCallOfLine(methodCall_line, methodCall_name);
        if(final_nodes_want==null){
            System.out.println("nu;ll");
        }
        if (final_nodes_want != null && final_nodes_want.size()!=0 ) {
            final_nodes_want.forEach(fi -> {
                System.out.println(String.valueOf(fi.getBegin().get().line) + " - " + String.valueOf(fi.getEnd().get().line) + ":  " + fi.toString());
            });
//        System.out.println(final_nodes_want);
        try{
                final_nodes_want.toString();
            }
            catch (Exception e){
                System.out.println(e);
            }
//             对提取到的所有代码行（节点列表final_nodes_want），进行变量名重命名
        p.renameVariableName(final_nodes_want);

//             输出重命名后的节点列表
        System.out.println(final_nodes_want);
        }
//------------------------------------------------------

        // 示例：得到 所有的方法调用，存储到节点列表methodCallExprsOfline，打印输出
//        System.out.println("\n=============================================\nMethodCallExprs name All:\n=============================================");
//        p.methodCallExprs.forEach(methodCallExpr -> System.out.println(methodCallExpr.getName()));


        // 示例：得到 所有 的，方法调用的参数的代码段，存储到节点列表final_nodes_want，//然后对结果进行变量名重命名
//        p.methodCallExprs.forEach(methodCallExpr -> {
//            String one_methodCall_name = methodCallExpr.getNameAsString();
//            int one_methodCall_line = methodCallExpr.getBegin().get().line;
//            System.out.println("\n=============================================");
//            System.out.println(one_methodCall_name + " in " + one_methodCall_line);
//            System.out.println("Slices=======================================");
//            List<Node> one_final_nodes_want = p.getSliceOfMethodCallOfLine(one_methodCall_line, one_methodCall_name);
//            if (one_final_nodes_want != null) {
//                one_final_nodes_want.forEach(fi -> {
//                    System.out.println(String.valueOf(fi.getBegin().get().line) + " - " + String.valueOf(fi.getEnd().get().line) + ":  " + fi.toString());
//                });
////            System.out.println(one_final_nodes_want);
//                // 对提取到的所有代码行（节点列表final_nodes_want），进行变量名重命名
////            p.renameVariableName(one_final_nodes_want);
//                // 输出重命名后的节点列表
////            System.out.println(one_final_nodes_want);
//            }
//        });
    }
}
