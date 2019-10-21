package Neo4j;
import GraphProcess.AST2Graph;
import GraphProcess.Util;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.util.*;

public class GraphParse {
    @Expose
    @SerializedName(value = "fileName")
    String fileName;
    @Expose
    @SerializedName(value = "Version")
    String version;
    @Expose
    @SerializedName(value = "callMethodName")
    List<String> callMethodName;

    public GraphParse() {
        //无参构造函数；
    }

    public static void main(String[] args) {
        System.out.println("开始解析！");
        String Version = "test";
        String SVersion = "stest";
        String SourceCat = "../CodeGraph/Project/Sourcedata/" + Version + "/";
        String SaveCat = "../CodeGraph/Project/SaveData/" + SVersion + "/";
        File dir = new File(SourceCat);
        ExtractJavafile javafile = new ExtractJavafile(SourceCat);
        javafile.getFileList(dir);
        File[] fileList = javafile.getFile();
        ProcessMultiFile(fileList, SaveCat);//处理多个文件，包含有多个类或者接

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
        HashMap<String, HashMap<MethodDeclaration, String>> CalledMethod = new HashMap<>();
        List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap = new ArrayList<>();
        //<文件，astGraph>
        Map<File, GraphProcess.AST2Graph> fileAst2GraphMap = new HashMap<>();
        Arrays.stream(fileList).forEach(file -> fileAst2GraphMap.put(file, GraphProcess.AST2Graph.newInstance(file.getPath())));
        fileMethodDeclarationMap = Utils.getfileMethodDeclarationMap(fileList);//获得所有文件的内部类函数和外部类函数

        for (File pfile : fileList) {//循环遍历文件处理
            GraphProcess.AST2Graph ast2Graph = fileAst2GraphMap.get(pfile);
            List<MethodDeclaration> methodDeclarations = new ArrayList<>();
            methodDeclarations = ast2Graph.getMethodDeclarations();

            //写入当前文件的头文件信息
            new GraphParse().FileHeader(pfile, methodDeclarations, SaveCat);
            //获得当前文件的外部类、内部类函数
            HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> outclassMethods =fileMethodDeclarationMap.get(1).get(pfile);
            HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> innerclassMethods = fileMethodDeclarationMap.get(0).get(pfile);
            for (MethodDeclaration pmethodDeclaration : methodDeclarations) {//循环遍历函数声明处理
                //  函数申明在外部类或者内部类中
                if (Utils.containMethod(pmethodDeclaration, outclassMethods, innerclassMethods)) {
                    //目前只处理外部类和内部类中的函数
                    CalledMethod = Utils.getcallMethods(pfile, pmethodDeclaration, fileMethodDeclarationMap);
                    String classNameOrMethod = FunctionParse.getClassOfMethod(pmethodDeclaration);//获得类名_函数名
                    try {

                        PreocessingMethod(ast2Graph, pmethodDeclaration, classNameOrMethod.concat("_") + pmethodDeclaration.getNameAsString(), pfile, CalledMethod, SaveCat);
                    } catch (NumberFormatException e) {
                        System.out.println(pmethodDeclaration.getNameAsString() + "\t:内外部类函数构造异常");
                        continue;
                    }

                } else {
                    //  函数申明不在内部类外部类函数中
                    // 代码中实例化的函数,比如在新建接口，需要对接口中的方法进行实现，有可能直接在new花括号中直接实现。这部分会被
                    // 这部分会被当成函数调用，注意这种格式一般是函数被重写(目前是按照这种格式来处理的)
                    if (pmethodDeclaration.getParentNode().isPresent() && pmethodDeclaration.getParentNode().get() instanceof ObjectCreationExpr) {
                        //这个是new实例化中的方法重写
                        //标记方法用新建对象的new A(){}中的A作为我们的类对象。
                        String newClassName = ((ObjectCreationExpr) pmethodDeclaration.getParentNode().get()).getTypeAsString();
                        //获得类名
                        String classNameOfMethod = PareClassOrInterfaces.concatName(newClassName);
                        //  在OuterClassMethod2Json中完成了函数名的划分
                        CalledMethod = Utils.getcallMethods(pfile, pmethodDeclaration, fileMethodDeclarationMap);//<文件名，<函数申明，类名_>>
                        try {
                            PreocessingMethod(ast2Graph, pmethodDeclaration, classNameOfMethod.concat("_") + pmethodDeclaration.getNameAsString(), pfile, CalledMethod, SaveCat);
                        } catch (NumberFormatException e) {
                            System.out.println(pmethodDeclaration.getNameAsString() + "\t:实例化函数构造异常");
                            continue;
                        }


                    } else {

                        // TODO 可能存在其他的情况，还没想到

                    }


                }

            }
        }
    }

    public static void PreocessingMethod(AST2Graph ast2Graph, MethodDeclaration pmethodDeclaration, String MethodName, File pfile, HashMap<String, HashMap<MethodDeclaration, String>> CalledMethod, String Savecat) {
        //methodCalled:<文件名，<类名_in/out_函数名>>
        String fileName = pfile.getName();
        String[] pathArray = pfile.getPath().split("\\\\");
        String version = pathArray[4];//第4个位置为我们的版本号
        MethodDeclaration2Json MethodDeclaration2Json = new MethodDeclaration2Json(pfile, version, pmethodDeclaration.getNameAsString(), CalledMethod, pmethodDeclaration);
        MethodDeclaration2Json.setMethodName(MethodName);
        MethodDeclaration2Json.newInstanceJson(ast2Graph);
        MethodDeclaration2Json.saveToJson(Savecat + fileName + ".txt");
    }

    public void FileHeader(File file, List<MethodDeclaration> methodDeclarations, String savecat) {
        // TODO json头文件需要进行重写
        String[] array = file.getPath().split("\\\\");
        List<String> methodName = new ArrayList<>();
        methodDeclarations.forEach(methodDeclaration -> methodName.add(methodDeclaration.getNameAsString()));
        this.version = array[4];//拆分的第4个位置为版本号
        this.fileName = file.getName();
        this.callMethodName = methodName;
        Util.saveToJsonFile(this, savecat + fileName + ".txt");


    }


}



