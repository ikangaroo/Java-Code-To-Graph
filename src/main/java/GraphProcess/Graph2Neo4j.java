package GraphProcess;

import Neo4j.ExtractJavafile;
import Neo4j.FunctionClass;
import Neo4j.Utils;
import Neo4j.classMethod2Json.outerClassMethod2Json;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.google.common.graph.MutableNetwork;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Graph2Neo4j {
    @Expose
    @SerializedName(value = "fileName")
    String fileName;
    @Expose
    @SerializedName(value = "Version")
    String version;
    @Expose
    @SerializedName(value = "callMethodName")
    List<String> callMethodName;
    public Graph2Neo4j(){
        //无参构造函数；
    }
    public static void main(String[] args) {
        System.out.println("开始解析！");
        String Version="test";
        String SVersion="stest";
        String SourceCat="../CodeGraph/Project/Sourcedata/"+Version+"/";
        String SaveCat="../CodeGraph/Project/SaveData/"+SVersion+"/";
        File dir=new File(SourceCat);
        ExtractJavafile javafile= new ExtractJavafile(SourceCat);
        javafile.getFileList(dir);
        File []fileList=javafile.getFile();
        if(fileList.length==1){
            ProcessSingleFile(fileList[0],SaveCat);//处理单个文件。这个不会存在多个类或者接口，因为一个类对应同一个文件
        }
        else {
            ProcessMultiFile(fileList,SaveCat);//处理多个文件，包含有多个类或者接口
        }

    }
    public static void ProcessSingleFile(File pfile,String SaveCat){
        AST2Graph ast2Graph= AST2Graph.newInstance(pfile.getPath());
        List<MethodDeclaration> methodDeclarations=new ArrayList<>();
        methodDeclarations=ast2Graph.getMethodDeclarations();
        //写入当前文件的头文件信息
        /**
         0、methodDeclation包含当前文件中所有的函数包括如下：解决函数重名问题
         1、常规文件类中的函数
         2、内部类中的函数
         3、构造函数
         4、重载函数处理
         */
        //头文件处理
         new Graph2Neo4j().FileHeader(pfile,methodDeclarations,SaveCat);

        for(MethodDeclaration pmethodDeclaration:methodDeclarations){
            //判断函数申明是在内部类中，还是外部类当中，针对不同的类型，进行处理
            Map<String,List<String>> methodCalled=new HashMap<>();//存储文件-》对应的调用的函数
            methodCalled.put(pfile.getName(),new ArrayList<>());//本文件，只有一个文件
            String pMethodName=pmethodDeclaration.getNameAsString();
            BlockStmt body=pmethodDeclaration.findAll(BlockStmt.class).get(0);
            NodeList<Statement> statements=body.getStatements();
            for(Statement statement:statements){
                if(statement.findAll((MethodCallExpr.class)).size()==0){continue;}
                List<MethodCallExpr> methodCallExpr=statement.findAll(MethodCallExpr.class);
                HashMap<MethodCallExpr,String> methodCallExprNames=new HashMap<>();
                methodCallExpr.forEach(m ->methodCallExprNames.put(m,m.getNameAsString()));
                if(ast2Graph.getCompilationUnit().findAll(MethodDeclaration.class).stream().filter(m->methodCallExprNames.containsValue(m.getNameAsString())).collect(Collectors.toList()).size()==0){continue;}
                List<MethodDeclaration> method=ast2Graph.getCompilationUnit().findAll(MethodDeclaration.class)
                        .stream().filter(m->methodCallExprNames.containsValue(m.getNameAsString())).collect(Collectors.toList());
                method.forEach(methodDeclaration -> methodCalled.get(pfile.getName()).add(methodDeclaration.getNameAsString()));
                //methodCalled存储的是《文件名，文件名中包含的函数名》，方便确定查找结点间的调用关系。
            }
            //PreocessingMethod(ast2Graph,pmethodDeclaration,pfile,methodCalled,SaveCat);
        }

    }

 public static void ProcessMultiFile(File[] fileList,String SaveCat) {
        HashMap<String, HashMap<MethodDeclaration,String>> CalledMethod = new HashMap<>();
       List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap=new ArrayList<>();
      Map<File,AST2Graph> fileAst2GraphMap=new HashMap<>();
      Arrays.stream(fileList).forEach(file -> fileAst2GraphMap.put(file,AST2Graph.newInstance(file.getPath())));
      fileMethodDeclarationMap=Utils.getfileMethodDeclarationMap(fileList);
     for (File pfile : fileList) {//循环遍历文件处理
         AST2Graph ast2Graph = AST2Graph.newInstance(pfile.getPath());
         List<MethodDeclaration> methodDeclarations = new ArrayList<>();
         methodDeclarations = ast2Graph.getMethodDeclarations();

         //写入当前文件的头文件信息
         new Graph2Neo4j().FileHeader(pfile,methodDeclarations,SaveCat);
         //外部类、内部类函数判断
         FunctionClass functionClass=new FunctionClass(pfile);
         functionClass.PareMethod();
         HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>> outclassMethods=functionClass.getOutclassMethods();
         HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>innerclassMethods=functionClass.getInnerclassMethods();
          List<ClassOrInterfaceDeclaration> innerclass=functionClass.getInnerclass();
         for (MethodDeclaration pmethodDeclaration : methodDeclarations) {//循环遍历函数声明处理
            if(Utils.containMethod(pmethodDeclaration,outclassMethods,innerclassMethods)){
                //目前只处理外部类和内部类中的函数
                CalledMethod=Utils.getcallMethods(pfile,pmethodDeclaration,fileMethodDeclarationMap);
                PreocessingMethod(ast2Graph,pmethodDeclaration,pfile,CalledMethod,SaveCat);

            }
            else {
                //TODO 代码中实例化的函数,比如在新建接口，需要对接口中的方法进行实现，有可能直接在new花括号中直接实现。这部分会被
                // 这部分会被当成函数调用


            }

         }
     }
 }

   public static void PreocessingMethod(AST2Graph ast2Graph,MethodDeclaration pmethodDeclaration,File pfile, HashMap<String, HashMap<MethodDeclaration,String>> CalledMethod,String Savecat){
        //methodCalled:<文件名，<类名_in/out_函数名>>
        String fileName=pfile.getName();
        String[] pathArray=pfile.getPath().split("\\\\");
        String version=pathArray[4];//第4个位置为我们的版本号
       outerClassMethod2Json outerClassMethod2Json=new outerClassMethod2Json(pfile,version,pmethodDeclaration.getNameAsString(),CalledMethod,pmethodDeclaration);
       outerClassMethod2Json.newInstanceJson(ast2Graph);
       outerClassMethod2Json.saveToJson(Savecat+fileName+".txt");
   }
   public  void FileHeader(File file,List<MethodDeclaration> methodDeclarations,String savecat){
        // TODO json头文件需要进行重写
        String[] array=file.getPath().split("\\\\");
        List<String> methodName=new ArrayList<>();
        methodDeclarations.forEach(methodDeclaration -> methodName.add(methodDeclaration.getNameAsString()));
        this.version=array[4];//拆分的第4个位置为版本号
        this.fileName=file.getName();
        this.callMethodName=methodName;
        Util.saveToJsonFile(this,savecat+fileName+".txt");


        }




}


