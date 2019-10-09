package GraphProcess;

import Neo4j.ExtractJavafile;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.graph.MutableNetwork;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.github.javaparser.ast.stmt.*;
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
        String Version="0.9.22";
        String SVersion="s0.9.22";
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
         new Graph2Neo4j().FileHeader(pfile,methodDeclarations,SaveCat);

        for(MethodDeclaration pmethodDeclaration:methodDeclarations){
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
                //methodCalled存储的是本函数中调用的函数名字
            }
            PreocessingCallmethod(ast2Graph,pmethodDeclaration,pfile,methodCalled,SaveCat);
        }

    }

 public static void ProcessMultiFile(File[] fileList,String SaveCat) {
      Map<String,List<String>> fileMethodDeclarationMap=new HashMap<>();
      Map<File,AST2Graph> fileAst2GraphMap=new HashMap<>();
     Arrays.stream(fileList).forEach(file -> fileAst2GraphMap.put(file,AST2Graph.newInstance(file.getPath())));
     for(File file:fileList){
         List<String> methodName=new ArrayList<>();
         fileAst2GraphMap.get(file).getMethodDeclarations().stream().forEach(m->methodName.add(m.getNameAsString()));
         fileMethodDeclarationMap.put(file.getName(),methodName);
     }

     for (File pfile : fileList) {//确定是.java文件
         AST2Graph ast2Graph = AST2Graph.newInstance(pfile.getPath());
         List<MethodDeclaration> methodDeclarations = new ArrayList<>();
         methodDeclarations = ast2Graph.getMethodDeclarations();

         //写入当前文件的头文件信息
         new Graph2Neo4j().FileHeader(pfile,methodDeclarations,SaveCat);

         for (MethodDeclaration pmethodDeclaration : methodDeclarations) {
               Map<String,List<String>> CalledMethod=new HashMap<>();
               List<ClassOrInterfaceType> classOrInterfaceTypeList=pmethodDeclaration.findAll(ClassOrInterfaceType.class);
               List<MethodCallExpr>methodCallExprList=pmethodDeclaration.findAll(MethodCallExpr.class);
             //先得到本函数中所有的方法调用，然后得到所有的类或者接口类型，然后过滤到接口
             // 然后在当前目录下遍历找是不是有接口的名字，或者类的名字，如果存在，则在本类中找具体的方法
              classOrInterfaceTypeList=classOrInterfaceTypeList.stream().filter(classOrInterfaceType ->fileMethodDeclarationMap.keySet().contains(classOrInterfaceType.getNameAsString()+".java")).collect(Collectors.toList());
              //依次遍历方法调用（调用的顺序依次为，本文件下，类接口文件、（如果类接口文件为空）全部文件）
             List<String>InfileCalledMethod=new ArrayList<>();
             Map<String,List<String>>OtherfileCalledMethod=new HashMap<>();
             List<String>AllfileCalledMethod=new ArrayList<>();
             for (MethodCallExpr methodCallExpr:methodCallExprList){
                 if(fileMethodDeclarationMap.get(pfile.getName()).contains(methodCallExpr.getNameAsString())){
                     //本文件查找
                     InfileCalledMethod.add(methodCallExpr.getNameAsString());
                 }
                 else if(classOrInterfaceTypeList.size()!=0){
                     //存在接口或者类文件列表
                     for(ClassOrInterfaceType classOrInterfaceType:classOrInterfaceTypeList){
                         if(fileMethodDeclarationMap.get(classOrInterfaceType.getNameAsString()+".java").contains(methodCallExpr.getNameAsString())){
                             if(OtherfileCalledMethod.containsKey(classOrInterfaceType.getNameAsString()+".java")){
                                 OtherfileCalledMethod.get(classOrInterfaceType.getNameAsString()+".java").add(methodCallExpr.getNameAsString());
                             }
                             else {
                                 OtherfileCalledMethod.put(classOrInterfaceType.getNameAsString()+".java",new ArrayList<String>());
                                 OtherfileCalledMethod.get(classOrInterfaceType.getNameAsString()+".java").add(methodCallExpr.getNameAsString());
                             }
                         }
                         else {
                             System.out.println(pmethodDeclaration.getNameAsString()+"函数中找不到方法调用");
                         }
                     }
                 }
                 else {
                     System.out.println(methodCallExpr.getNameAsString()+"：这是个系统库函数，不需要进行构建");
                 }
             }
             if (InfileCalledMethod.size()!=0){
                 CalledMethod.put(pfile.getName(),InfileCalledMethod);
             }
             else if(OtherfileCalledMethod.size()!=0){
                 OtherfileCalledMethod.keySet().forEach(file->CalledMethod.put(file,OtherfileCalledMethod.get(file)));

             }
             PreocessingCallmethod(ast2Graph,pmethodDeclaration,pfile,CalledMethod,SaveCat);



         }
     }
 }

   public static void PreocessingCallmethod(AST2Graph ast2Graph,MethodDeclaration pmethodDeclaration,File pfile,Map<String,List<String>>methodCalled,String Savecat){

        String fileName=pfile.getName();
        String[] pathArray=pfile.getPath().split("\\\\");
        String version=pathArray[4];//第4个位置为我们的版本号
        ast2Graph.initNetwork();
        ast2Graph.constructNetwork(pmethodDeclaration);
        MutableNetwork<Object,String> mutableNetwork=ast2Graph.getNetwork();
        Graph2Json graph2Json=Graph2Json.newInstanceForNeo4j(mutableNetwork,pfile,version,pmethodDeclaration.getNameAsString(),methodCalled,pmethodDeclaration);
        graph2Json.saveToJson(Savecat+fileName+".txt");
   }
   public  void FileHeader(File file,List<MethodDeclaration> methodDeclarations,String savecat){
        String[] array=file.getPath().split("\\\\");
        List<String> methodName=new ArrayList<>();
        methodDeclarations.forEach(methodDeclaration -> methodName.add(methodDeclaration.getNameAsString()));
        this.version=array[4];//拆分的第4个位置为版本号
        this.fileName=file.getName();
        this.callMethodName=methodName;
        Util.saveToJsonFile(this,savecat+fileName+".txt");


        }




}


