package Neo4j;

import GraphProcess.RangeNode;
import Neo4j.attribute.ParseExpression;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
    @Setter
    @Getter
    private static List<MethodCallExpr> methodCallExprsNeed = new ArrayList<>();

    public static String addAttributes(Object object) {//为CFG每个节点添加属性
        if (object instanceof RangeNode) {
            Node node = ((RangeNode) object).getmNode();
            return new ParseExpression(node).set2List();
        } else {
            System.out.println("不存在RangeNode节点");
            return null;
        }

    }

    public static Node Object2Node(Object object) {
        if (object instanceof RangeNode) {
            Node node = ((RangeNode) object).getNode();
            return node;

        } else {
            System.out.println("节点类型错误(!RangeNode)");
            return null;
        }
    }

    public static boolean containMethod(MethodDeclaration methodDeclaration, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> outclassMethods, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> innertclassMethods) {
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : outclassMethods.keySet()) {
            if (outclassMethods.get(classOrInterfaceDeclaration).contains(methodDeclaration)) {
                return true;
            }
        }
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : innertclassMethods.keySet()) {
            if (innertclassMethods.get(classOrInterfaceDeclaration).contains(methodDeclaration)) {
                return true;
            }
        }
        return false;
    }

    public static boolean compareMethod(MethodCallExpr methodCallExpr, MethodDeclaration methodDeclaration) {
        //解决函数调用和函数名相等、参数数量相等情况下，找到不止一个函数位置
        if (methodCallExpr.getNameAsString().equals(methodDeclaration.getNameAsString()) && methodCallExpr.getArguments().size() == methodDeclaration.getParameters().size()) {
            if (true) {
                return true;
                //参数类型一样
            }
        }
        return false;
    }

    public static List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> getfileMethodDeclarationMap(File[] files) {
        // 获得所有文件的内部函数声明和外部函数声明
        //<外部类，外部类中所有的方法声明>
        List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap = new ArrayList<>();
        HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>> allOutclassMethods = new HashMap<>();
        //<内部类，内部类中所有的方法声明>
        HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>> allInnerclassMethods = new HashMap<>();

        for (File file : files) {
            FunctionParse functionParse = new FunctionParse(file);
            functionParse.PareMethod();
            allOutclassMethods.put(file, functionParse.getOutclassMethods());
            allInnerclassMethods.put(file, functionParse.getInnerclassMethods());
        }
        fileMethodDeclarationMap.add(allInnerclassMethods);
        fileMethodDeclarationMap.add(allOutclassMethods);
        return fileMethodDeclarationMap;

    }

    public static ThreeTuple getcalledExprLocation(HashMap<ClassOrInterfaceDeclaration, String> candidateFile,
                                                   List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap,
                                                   MethodCallExpr methodCallExpr) {
        //返回<文件名，类名_out/in,methodDeclaration>，现在只是在内部类、外部类函数中找，(接口实现重写未处理)
        String methodCallExprName = methodCallExpr.getNameAsString();
        ThreeTuple calledExprLocation = new ThreeTuple();
        HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>> allInnerclassMethods = fileMethodDeclarationMap.get(0);
        HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>> allOutclassMethods = fileMethodDeclarationMap.get(1);
        if (candidateFile.keySet().isEmpty()){
            //候选集为空，不存在函数调用，返回空值。
            return calledExprLocation;
        }
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : candidateFile.keySet()) {
            //首先
            if (classOrInterfaceDeclaration.getNameAsString().concat(".java").equals(candidateFile.get(classOrInterfaceDeclaration))) {
                //文件名和类名一样，在外部类中调用
                //得到函数调用所在的函数名字
                String filename = getMethodExprFileName(methodCallExpr);
                for (File file : allOutclassMethods.keySet()) {
                    if (file.getName().equals(filename)) {//找到文件
                        HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> classOrInterfaceDeclarationListHashMap = allOutclassMethods.get(file);
                        ClassOrInterfaceDeclaration target_class;
                        MethodDeclaration target_method;
                        try {
                            target_class = classOrInterfaceDeclarationListHashMap.keySet().stream().filter(classOrInterfaceDeclaration1
                                    -> classOrInterfaceDeclaration1
                                    .getNameAsString().concat(".java").equals(filename))
                                    .collect(Collectors.toList()).get(0);//外部类中，类名与文件全部一样

                            target_method = classOrInterfaceDeclarationListHashMap.get(target_class).stream().filter(methodDeclaration
                                    -> methodDeclaration.getNameAsString()
                                    .equals(methodCallExprName) && methodDeclaration.getParameters().size() == methodCallExpr.getArguments().size())
                                    .collect(Collectors.toList()).get(0);//函数名一致，并且参数列表的长度一致
                        } catch (Exception e) {
                            System.out.println("MethodCallexpr：" + filename + "\n" + "classOrInterfaceDeclaration:" + classOrInterfaceDeclaration.getNameAsString());
                            continue;

                        }
                        //外部类
                        calledExprLocation.setFile(file);//File
                        calledExprLocation.setClassName(target_class.getNameAsString().concat("_outer"));//String
                        calledExprLocation.setMethodDeclaration(target_method);//MethodDeclaration
                        return calledExprLocation;//找到即返回

                    }
                }
            } else {
                //在内部类中调用
                //得到函数调用所在的文件名
                String filename = getMethodExprFileName(methodCallExpr);
                for (File file : allInnerclassMethods.keySet()) {
                    if (file.getName().equals(filename)) {//找到文件
                        ClassOrInterfaceDeclaration target_class;
                        MethodDeclaration target_method;
                        try {
                            HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> classOrInterfaceDeclarationListHashMap = allInnerclassMethods.get(file);
                            target_class = classOrInterfaceDeclarationListHashMap.keySet().stream().filter(classOrInterfaceDeclaration1
                                    -> classOrInterfaceDeclaration1
                                    .getNameAsString().equals(classOrInterfaceDeclaration.getNameAsString()))
                                    .collect(Collectors.toList()).get(0);
                            target_method = classOrInterfaceDeclarationListHashMap.get(target_class).stream().filter(methodDeclaration
                                    -> methodDeclaration.getNameAsString()
                                    .equals(methodCallExprName) && methodDeclaration.getParameters().size() == methodCallExpr.getArguments().size())
                                    .collect(Collectors.toList()).get(0);
                        } catch (Exception e) {
                            System.out.println("MethodCallexpr：" + methodCallExpr.getNameAsString() + "\n" + "classOrInterfaceDeclaration:" + classOrInterfaceDeclaration.getNameAsString());
                            continue;

                        }
                        //内部类
                        calledExprLocation.setFile(file);//File
                        calledExprLocation.setClassName(target_class.getNameAsString().concat("_iner"));//String
                        calledExprLocation.setMethodDeclaration(target_method);//MethodDeclaratinon
                        return calledExprLocation;


                    }
                }
            }
            return calledExprLocation;//这种情况没有找到，系统库函数、或者第三方的函数。

        }

        return calledExprLocation;

    }

    public static HashMap<ClassOrInterfaceDeclaration, String> getCandidateFileByClass(MethodDeclaration pmethodDeclaration,
                                                                                       List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap) {
        HashMap<ClassOrInterfaceDeclaration, String> candidateFile = new HashMap<>();
        List<File> pfile = null;
        //我们的函数调用通过类名来引用：new对象，或者直接使用静态类；
        List<String> classOrInterfaceNameList = new PareClassOrInterfaces(pmethodDeclaration).findClassList1();//获得当前函数声明中所有类名字;
        ClassOrInterfaceDeclaration classOrInterfaceDeclaration = null;
        for (String className : classOrInterfaceNameList) {
            if (className.contains(".")) {
                //处理GsonCompatibilityMode.Builder这种类型的数据，前一个是外部类，后一个是内部类，scope作为修饰符
                String fileName = className.split("\\.")[0];
                String classOrInterfaceName = className.split("\\.")[1];
                pfile = fileMethodDeclarationMap.get(0).keySet().stream().filter(file -> file.getName().equals(fileName.concat(".java"))).collect(Collectors.toList());
                if (pfile.isEmpty()) {
                    System.out.println(className + "是系统类，跳过");
                    continue;
                }
                try {
                    classOrInterfaceDeclaration = fileMethodDeclarationMap.get(0).get(pfile.get(0)).keySet().stream().filter(classOrInterfaceDeclaration1
                            -> classOrInterfaceDeclaration1.getNameAsString()
                            .equals(classOrInterfaceName)).collect(Collectors.toList()).get(0);
                } catch (Exception e) {
                    System.out.println("跳过越界");
                    continue;
                }

                //<文件名，类接口类型声明>
                candidateFile.put(classOrInterfaceDeclaration, fileName.concat(".java"));
            } else {
                // JsoniterSpi这种形式属于外部类函数
                String fileName = className;
                String classOrInterfaceName = className;
                //optional包装下，允许空值
                pfile = fileMethodDeclarationMap.get(1).keySet().stream().filter(file -> file.getName().equals(fileName.concat(".java"))).collect(Collectors.toList());
                if (pfile.isEmpty()) {
                    System.out.println(className + "是系统类，跳过");

                    continue;
                }
                try {
                    classOrInterfaceDeclaration = fileMethodDeclarationMap.get(1).get(pfile.get(0)).keySet().stream().filter(classOrInterfaceDeclaration1
                            -> classOrInterfaceDeclaration1.getNameAsString().equals(classOrInterfaceName))
                            .collect(Collectors.toList()).get(0);
                } catch (Exception e) {
                    System.out.println("跳过越界异常");
                    continue;
                }

                candidateFile.put(classOrInterfaceDeclaration, fileName.concat(".java"));
            }
        }
        return candidateFile;
    }

    public static HashMap<String, HashMap<MethodDeclaration, String>> getcallMethods(File pfile, MethodDeclaration pmethodDeclaration, List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap) {
        /**
         找到所有函数调用所在的文件名，类名、函数申明
         *
         * 通过类找到函数调用的位置，类中的函数必须通过类名或者接口来调用（提升查找效率）
         */
        //methodCallExprsNeed.clear();
        HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>> allInnerclassMethods = fileMethodDeclarationMap.get(0);
        HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>> allOutclassMethods = fileMethodDeclarationMap.get(1);
        HashMap<String, HashMap<MethodDeclaration, String>> CalledMethod = new HashMap<>();
        List<MethodCallExpr> methodCallExprList = pmethodDeclaration.findAll(MethodCallExpr.class);
        HashMap<ClassOrInterfaceDeclaration, String> candidateFile = getCandidateFileByClass(pmethodDeclaration, fileMethodDeclarationMap);//通过类名得到查找候选集<文件名，类>
        for (MethodCallExpr methodCallExpr : methodCallExprList) {
            //new GsonBuilder().setDataFormat("bdg").create(),最后一个.后面的才是我们的方法调用。
            //通过《文件名,类名》候选集来实现简化查找
            ThreeTuple calledExprLocation = getcalledExprLocation(candidateFile, fileMethodDeclarationMap, methodCallExpr);//查找到某个函数调用的位置，用三元组表示
            if (calledExprLocation.getClassName() == null || calledExprLocation.getFile() == null || calledExprLocation.getMethodDeclaration() == null) {
                System.out.println(methodCallExpr.getNameAsString() + "：是系统、库函数");
                //需要添加跳过操作continue
            } else {

                //找到函数的位置
                //methodCallExprsNeed.add(methodCallExpr);
                //[文件名，函数，类名_outer/inner],只存在一个3键对
                if (CalledMethod.keySet().contains(calledExprLocation.getFile().getName())) {
                    CalledMethod.get(calledExprLocation.getFile().getName()).put(calledExprLocation.getMethodDeclaration(), calledExprLocation.getClassName());
                } else {
                    CalledMethod.put(calledExprLocation.getFile().getName(), new HashMap<MethodDeclaration, String>() {{
                        put(calledExprLocation.getMethodDeclaration(), calledExprLocation.getClassName());
                    }});
                }
            }

        }
        return CalledMethod;

    }

    public static String getMethodExprFileName(MethodCallExpr methodCallExpr) {
        //获得函数所在的文件
        String methodExprFileName = "";
        String methodName = methodCallExpr.toString();
        String[] split = methodName.split("\\.");
        List<ObjectCreationExpr> objectCreationExpr = methodCallExpr.findAll(ObjectCreationExpr.class);
        if (!objectCreationExpr.isEmpty()) {
            //通过new创建的对象，匿名类调用
            return objectCreationExpr.get(0).getTypeAsString().split("\\.")[0].concat(".java");
        } else if (split[0].charAt(0)-0 < 'z' - 0) {
            //大写字母，静态类
            return split[0].concat(".java");
        } else {
            //TODO 通过对像来调用，提前定义好对象，然后通过对象来调用方法


        }
        return null;

    }

}
