package Neo4j;

import GraphProcess.Util;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.Type;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;

public class GraphParse {

    private String fileName;
    private String version;
    /** 函数名-类名.类名-参数类型1-参数类型2 **/
    private List<String> callMethodName = new ArrayList<>();
    private String methodName;

    public GraphParse() {
        /* 无参构造函数； */
    }

    public static void main(String[] args) {
        System.out.println("开始解析！");
        String Version = "0.9.22";
        String SourcePath = "Project\\Sourcedata\\a\\b\\" + Version + "\\";
        String SavePath = "Project\\Savadata\\" + Version + "\\";
        File dir = new File(SourcePath);
        ExtractJavaFile javaFile = new ExtractJavaFile();
        javaFile.getFileList(dir);
        File[] fileList = javaFile.getFile();
        //处理多个文件，包含有多个类或者接
        ProcessMultiFile(fileList, SavePath);
    }

    public static void ProcessMultiFile(File[] fileList, String SaveCat) {
        //写入当前文件的头文件信息
        /**
         0、methodDeclation包含当前文件中所有的函数包括如下：解决函数重名问题
         1、常规文件类中的函数
         2、内部类中的函数
         3、构造函数
         4、重载函数处理
         */
        //头文件处理
        HashMap<String, HashMap<MethodDeclaration, String>> callMethod;

        // 获得所有文件的内部类函数和外部类函数
        List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap =
                Utils.getfileMethodDeclarationMap(fileList);


        //循环遍历文件处理
        for (File file : fileList) {
            GraphProcess.AST2Graph ast2Graph = GraphProcess.AST2Graph.newInstance(file.getPath());
            // 不包含 new 类{ 函数 }的情况
            List<MethodDeclaration> methodDeclarations = ast2Graph.getmethodDeclarations();

            // 写入当前文件的头文件信息
            new GraphParse().headOfJson(file, methodDeclarations, SaveCat + Utils.getFileNameWithPath(file) + ".txt");
            //获得当前文件的外部类、内部类函数
            HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> outclassMethods =fileMethodDeclarationMap.get(1).get(file);
            HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> innerclassMethods = fileMethodDeclarationMap.get(0).get(file);

            //循环遍历函数声明处理
            for (MethodDeclaration methodDeclaration : methodDeclarations) {
                //  函数申明在外部类或者内部类中
                if (Utils.containMethod(methodDeclaration, outclassMethods, innerclassMethods)) {
                    //目前只处理外部类和内部类中的函数
                    callMethod = Utils.getcallMethods(methodDeclaration, fileMethodDeclarationMap);
                    try {
                        new GraphParse().methodOfJson(file, methodDeclaration, callMethod,SaveCat + Utils.getFileNameWithPath(file) + ".txt");
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        System.out.println(methodDeclaration.getNameAsString() + "\t:内外部类函数构造异常");
                        continue;
                    }

                } else {
                    System.out.println("函数声明的其他情况发生");

                    //  函数申明不在内部类外部类函数中
                    // 代码中实例化的函数,比如在新建接口，需要对接口中的方法进行实现，有可能直接在new花括号中直接实现。这部分会被
                    // 这部分会被当成函数调用，注意这种格式一般是函数被重写(目前是按照这种格式来处理的)
//                    if (methodDeclaration.getParentNode().isPresent() && methodDeclaration.getParentNode().get() instanceof ObjectCreationExpr) {
//                        //这个是new实例化中的方法重写
//                        //标记方法用新建对象的new A(){}中的A作为我们的类对象。
//                        String newClassName = ((ObjectCreationExpr) methodDeclaration.getParentNode().get()).getTypeAsString();
//                        //获得类名
//                        String classNameOfMethod = PareClassOrInterfaces.concatName(newClassName);
//                        //  在OuterClassMethod2Json中完成了函数名的划分
//                        callMethod = Utils.getcallMethods(pfile, methodDeclaration, fileMethodDeclarationMap);//<文件名，<函数申明，类名_>>
//                        try {
//                            new GraphParse().methodOfJson(pfile, methodDeclaration, SaveCat + pfile.getName() + ".txt");
////                            PreocessingMethod(ast2Graph, methodDeclaration, classNameOfMethod.concat("_") + methodDeclaration.getNameAsString(), pfile, callMethod, SaveCat);
//                        } catch (NumberFormatException e) {
//                            System.out.println(methodDeclaration.getNameAsString() + "\t:实例化函数构造异常");
//                            continue;
//                        }
//
//
//                    } else {
//                        // TODO 可能存在其他的情况，还没想到
//                    }


                }

            }
        }
    }

    /**
    * @Description: 将文件的基本信息写入Json文件中（第一行）
    * @Param:
    * @return:
    * @Author: Kangaroo
    * @Date: 2019/10/22
    */
    public void headOfJson(File file, List<MethodDeclaration> methodDeclarations, String saveFilePath){
        String[] array = file.getPath().split(Matcher.quoteReplacement(File.separator));
        this.fileName = Utils.getFileNameWithPath(file);
        this.version = array[4];
        // 函数名-类名.类名-参数类型-参数类型
        // 无参函数： 函数名-类名.类名-
        methodDeclarations.forEach(methodDeclaration -> this.callMethodName.add(
                methodDeclaration.getNameAsString() + "-" +
                        getClassNameOfMethod(methodDeclaration) + "-" +
                        getMethodParameter(methodDeclaration)
        ));

        Util.saveToJsonFile(new DataToJson.Head(this.fileName, this.version, this.callMethodName), saveFilePath);
    }

    /**
     * @Description: 保存文件中函数的基本信息到Json中
     * @Param:
     * @return:
     * @Author: Kangaroo
     * @Date: 2019/10/18
     */
    public void methodOfJson(File file, MethodDeclaration methodDeclaration, HashMap<String, HashMap<MethodDeclaration, String>> CalledMethod, String savaFilePath){
        this.fileName = Utils.getFileNameWithPath(file);
        this.version = file.getParent().split(Matcher.quoteReplacement(File.separator))[4];
        this.methodName = methodDeclaration.getNameAsString() + "-" + getClassNameOfMethod(methodDeclaration) + "-" + getMethodParameter(methodDeclaration);

        DataToJson.Body body = new DataToJson.Body(file, this.fileName, this.version, this.methodName, methodDeclaration, CalledMethod);
        body.addFeatureMethodOfJson();

        Util.saveToJsonFile(body, savaFilePath);
    }

    /**
     * @Description: 返回函数的类名，多层嵌套
     * @Param:
     * @return:
     * @Author: Kangaroo
     * @Date: 2019/10/22
     */
    public String getClassNameOfMethod(Node methodDeclaration){
        List<String> allClassName = new ArrayList<>();

        while (methodDeclaration.getParentNode().isPresent() && !(methodDeclaration.getParentNode().get() instanceof CompilationUnit)){

            if (methodDeclaration.getParentNode().get() instanceof ClassOrInterfaceDeclaration){
                allClassName.add(((ClassOrInterfaceDeclaration)methodDeclaration.getParentNode().get()).getName().toString());
            }else if (methodDeclaration.getParentNode().get() instanceof ObjectCreationExpr){
                // TODO
                // 函数定义在 new 类名(){}中的情况暂不完善
                //
//                allClassName.add(((ObjectCreationExpr)methodDeclaration.getParentNode().get()).getTypeAsString());

            }else{
                // TODO
                // 第二种情况再往上遍历时，会找到其他类型的节点

//                System.out.println("此情况未考虑");
//                System.exit(0);
            }
            methodDeclaration = methodDeclaration.getParentNode().get();
        }

        Collections.reverse(allClassName);
        return StringUtils.join(allClassName.toArray(), ".");
    }

    /**
     * @Description: 获取带参数类型的函数名
     * @Param:
     * @return:  String
     * @Author: Kangaroo
     * @Date: 2019/10/22
     */
    public String getMethodParameter(MethodDeclaration methodDeclaration){
        List<String> res = new ArrayList<>();

        for (Parameter parameter: methodDeclaration.getParameters()){
            Type type = parameter.getType();
            String string = new String();

            if (type.isArrayType()){
                string = parameter.getType().asArrayType().asString();

            }else if (type.isClassOrInterfaceType()){
                string = parameter.getType().asClassOrInterfaceType().asString();

            }else if (type.isIntersectionType()){
                string = parameter.getType().asIntersectionType().asString();

            }else if (type.isPrimitiveType()){
                string = parameter.getType().asPrimitiveType().asString();

            }else if (type.isReferenceType()){
                System.out.println("ReferenceType");
                // pass

            }else if (type.isTypeParameter()){
                string = parameter.getType().asTypeParameter().asString();

            }else if (type.isUnionType()){
                string = parameter.getType().asUnionType().asString();

            }else if (type.isUnknownType()){
                string = parameter.getType().asUnknownType().asString();

            }else if (type.isVarType()){
                string = parameter.getType().asVarType().asString();

            }else if (type.isVoidType()){
                string = parameter.getType().asVoidType().asString();

            }else if (type.isWildcardType()){
                string = parameter.getType().asWildcardType().asString();

            }else {
                System.out.println("Wrong!");
                System.exit(0);
            }
            res.add(string);
        }

        return StringUtils.join(res.toArray(), "-");
    }

}



