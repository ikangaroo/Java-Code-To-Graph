package GraphProcess;
import com.github.javaparser.Range;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.*;
import com.google.common.graph.*;

import javax.swing.text.html.Option;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class JulitJava{
     public static void main(String[] args) {
         String CAT="XSS";
        getJulietGraph("GNN-Data/"+CAT+"/sourceData","GNN-Data/"+CAT+"/saveData");

    }

    public static void getJulietGraph(String dataUrl,String saveUrl){
         String source="../CodeGraph/"+dataUrl+"/";
         String des="../CodeGraph/"+saveUrl+"/";
         String desT=des+"trueg/";
         String desF=des+"falseg/";
         Util.mkdirIfNotExists(des);
         Util.mkdirIfNotExists(desT);
         Util.mkdirIfNotExists(desF);
         for(File fileDir:new File(source).listFiles()){
             File[] subFile=fileDir.listFiles();
             if (subFile.length==1){
                  File dataProcess=subFile[0];
                  getSaveSingelGraph(dataProcess.getPath(),dataProcess.getName(),desT,desF);
             }
             else {
                getSaveMultiGraph(subFile,desT,desF);
             }
         }
    }
    public static void getSaveSingelGraph(String filePath,String fileName,String desT,String desF){
         int trueG=0;
         int falseG=0;
         AST2Graph ast2Graph=AST2Graph.newInstance(filePath);//解析java代码成AST语法树。
         if(ast2Graph==null){return;}
         List <MethodDeclaration> methodDeclarations=ast2Graph.getMethodDeclarations();

         if(methodDeclarations.size()<=0){
             System.out.println("there is no methodDeclarations!");
             System.exit(0);
         }
         for(MethodDeclaration me:methodDeclarations){
             Optional<BlockStmt> blockStmt=me.getBody();
             //获取所有
             List<SimpleName> simpleNames=blockStmt.get().findAll(SimpleName.class);

             String funcName=me.getName().toString();
             if(funcName.endsWith("bad")){
                 trueG=getFuncDefGraph(ast2Graph,me,fileName,desT,trueG);//为啥是trueG
             }
             if (funcName.endsWith("good")){
                 BlockStmt body =me.findAll(BlockStmt.class).get(0);
                 NodeList<Statement> statements=body.getStatements();
                 for(Statement st:statements){
                     MethodCallExpr expression=st.findAll(MethodCallExpr.class).get(0);
                     String calledName=expression.getName().toString();
                     MethodDeclaration method=ast2Graph.getCompilationUnit().findAll(MethodDeclaration.class)
                             .stream().filter(m->m.getNameAsString()
                                     .equals(calledName)).collect(Collectors.toList()).get(0);

                     falseG=getFuncDefGraph(ast2Graph,method,fileName,desF,falseG);


                 }

                }
             }



         }
    public static void getSaveMultiGraph(File[] fileList,String desT,String desF){
         HashMap<File,AST2Graph> ast2GraphHashMap=new HashMap<>();
         for(File processFile:fileList){
             if(processFile.getName().endsWith(".base")){continue;}//过滤到.base文件
             AST2Graph ast2Graph=AST2Graph.newInstance(processFile.getPath());
             if (ast2Graph==null){
                 System.out.println(processFile.getPath()+"is not exit\n");
             }
             else {
                 ast2GraphHashMap.put(processFile,ast2Graph);
             }
         }
         File Pfile=null;
         for(File file:ast2GraphHashMap.keySet()){
             if(file.getName().endsWith("a.java")){//找到以a.java结尾的文件
                Pfile=file;
                break;
             }
         }
         if (Pfile==null){return;}
         else {
             int flaseG=0;
             int trueG=0;

             AST2Graph PAstGraph=ast2GraphHashMap.get(Pfile);
            List<MethodDeclaration> methodDeclaration=PAstGraph.getMethodDeclarations();
            for(MethodDeclaration pm:methodDeclaration){
                if(pm.getName().toString().endsWith("bad")){
                    //bad函数的操作
                    trueG=getGraphCrossMultiFile(PAstGraph,pm,ast2GraphHashMap,Pfile,desT,trueG);


                }
                if(pm.getName().toString().endsWith("good")){
                    //good函数操作
                    BlockStmt body=pm.findAll(BlockStmt.class).get(0);
                    NodeList<Statement> statements=body.getStatements();
                    for(Statement st:statements){
                        MethodCallExpr methodCallExpr=st.findAll(MethodCallExpr.class).get(0);
                        String funCalledName=methodCallExpr.getName().toString();//被调用函数的名字
                        for(MethodDeclaration methodDeclaration1:PAstGraph.getMethodDeclarations()){
                            String methodDeclaration1Name=methodDeclaration1.getName().toString();
                            if(funCalledName.equals(methodDeclaration1Name)){

                                for(File file:ast2GraphHashMap.keySet()){
                                    ast2GraphHashMap.put(file,AST2Graph.newInstance(file.getPath()));
                                }
                                PAstGraph=ast2GraphHashMap.get(Pfile);
                                flaseG=getGraphCrossMultiFile(PAstGraph,methodDeclaration1,ast2GraphHashMap,Pfile,desF,flaseG);

                            }
                        }
                    }

                }
            }



         }


    }
    public static int getGraphCrossMultiFile(AST2Graph ast2Graph,MethodDeclaration me,HashMap<File,AST2Graph> ast2GraphHashMap,File file,String Dest,int trueG){
         ast2Graph.initNetwork();
         HashMap<File,List<MethodDeclaration>> fileListHashMap=new HashMap<>();
         getMethodDeclCrossFile(ast2Graph,me,ast2GraphHashMap,file,fileListHashMap);
         MutableNetwork<Object,String> network=ast2Graph.getNetwork();
         if(!network.edges().isEmpty()){
             trueG++;
             Graph2Json graph2Json=Graph2Json.newInstance(network);
             graph2Json.saveToJson(Dest+file.getName().toString()+"_"+trueG+"_.txt");
             System.out.println(Dest+file.getName().toString()+"_"+trueG+"_.txt");
         }

         return trueG;
    }
    public static void getMethodDeclCrossFile(AST2Graph ast2Graph,MethodDeclaration me,HashMap<File,AST2Graph> ast2GraphHashMap,File file,HashMap<File,List<MethodDeclaration>> fileMethordListHashMap){

         /*
         1、通过传进来的某个方法声明，生成该方法声明中的所有函数调用，包括系统调用函数和一些自己已经写好的在别的文件中的一些函数。
         2、遍历所有的函数调用。
         3、遍历所有的文件，找到函数调用与文件名字相似的那个文件（Java或者C++的性质）。
         4、遍历该文件的所有方法声明，如果已经访问，则过滤掉。
                  */

         ast2Graph.constructNetwork(me);
         if(!fileMethordListHashMap.containsKey(file)){
             fileMethordListHashMap.put(file,new ArrayList<>());
         }
         for(Object node:ast2Graph.getNetwork().nodes()){
             if(node instanceof RangeNode&&((RangeNode) node).getNode().getClass()==MethodDeclaration.class){
                 fileMethordListHashMap.get(file).add((MethodDeclaration)((RangeNode) node).getNode());

             }
         }
         List<MethodCallExpr>methodCallExprs=new ArrayList<>();
         Optional<BlockStmt>blockStmt=me.getBody();
         List<SimpleName> simpleNames=blockStmt.get().findAll(SimpleName.class);
         for(SimpleName s:simpleNames ){
             if(s.getIdentifier().endsWith("base")){continue;}
             for(File otherFile:ast2GraphHashMap.keySet()){
                 if(!otherFile.getName().equals(file.getName())&&otherFile.getName().contains(s.getIdentifier())){
                     AST2Graph otherFileAstGraph=ast2GraphHashMap.get(otherFile);//取得外部被调用函数ASTGraph
                     if(!ast2GraphHashMap.containsKey(otherFile)){//有些文件被过滤掉
                         System.out.println("the file is not existen"+otherFile.getName());
                         continue;
                     }



                     /*
                     1、我们已经可能存在函数调用所在的外部文件，以及对应文件名的AST2Graph
                    2、遍历我们当前函数声明中的所有函数，然后在otherASTGraph中进行查找构建*/
                     Optional<BlockStmt> calledfunList=me.getBody();
                     List<SimpleName> vistedCalledfunNames=calledfunList.get().findAll(SimpleName.class);
                     for(SimpleName CFN:vistedCalledfunNames){
                         if(methodCallExprs.contains(CFN)){continue;}
                         List<MethodCallExpr> methodCallExprsList=CFN.findAll(MethodCallExpr.class);
                         if(methodCallExprsList.size()==0){continue;}//没有找到
                         else {//当前的CFN为我们找的。
                             MethodCallExpr MCE=methodCallExprsList.get(0);
                            String fName =CFN.getIdentifier();
                            connectFuncCallToOtherFile(ast2Graph,ast2GraphHashMap,fileMethordListHashMap,MCE,fName,methodCallExprsList,otherFile,otherFileAstGraph);
                         }
                     }

                     }
                 }
             }
         }


         public static void connectFuncCallToOtherFile(AST2Graph ast2Graph,HashMap<File,AST2Graph> ast2GraphHashMap
                                                      ,HashMap<File,List<MethodDeclaration>> fileMethordListHashMap
                                                      ,MethodCallExpr MCE,String fName,List<MethodCallExpr> methodCallExprsList
                                                      ,File otherFile,AST2Graph otherFileAstGraph){
         for(MethodDeclaration otherFileMethodDecl:otherFileAstGraph.getMethodDeclarations()){
             String otherFileMethodDeclName=otherFileMethodDecl.getName().toString();
             if(fName.equals(otherFileMethodDeclName)||otherFileMethodDeclName.contains(fName)){//少一个参数一样的判断
                 methodCallExprsList.add(MCE);//传过来的函数调用已经被访问过，这个vistedCalledSimpleNames在父函数中被声明
                 ast2Graph.addFormalArgs(MCE,otherFileMethodDecl);//传过来是MethodCallExpr格式
                 ast2Graph.addMethodCall(MCE,otherFileMethodDecl);
                 if(fileMethordListHashMap.containsKey(otherFile)&&fileMethordListHashMap.get(otherFile).contains(otherFileMethodDecl)){continue;}
             }
             otherFileAstGraph.initNetwork();
             otherFileAstGraph.setNetwork(otherFileAstGraph.getNetwork());
             otherFileAstGraph.setEdgeNumber(otherFileAstGraph.getEdgeNumber());
             getMethodDeclCrossFile(otherFileAstGraph,otherFileMethodDecl,ast2GraphHashMap,otherFile,fileMethordListHashMap);
             ast2Graph.setNetwork(otherFileAstGraph.getNetwork());
             ast2Graph.setEdgeNumber(otherFileAstGraph.getEdgeNumber());
         }


         }







    public static int getFuncDefGraph(AST2Graph ast2Graph,MethodDeclaration me,String fileName,String des,int graphNumm){
         ast2Graph.initNetwork();//初始化图网络，将网络添加到ASTGraph中，进行初始化。
         ast2Graph.constructNetwork(me);//根据函数声明，将网络图的信息进行完善
         MutableNetwork <Object,String> network=ast2Graph.getNetwork();

         if(!network.edges().isEmpty()){//网络中边的数目不为空
             graphNumm++;
             Graph2Json graph2Json=Graph2Json.newInstance(network);//特征json格式的构造和初始化
             graph2Json.saveToJson(des+fileName+"_"+graphNumm+"_.txt");//保存文件
             System.out.println(des+fileName+"_"+graphNumm+"_.txt");
         }


         return graphNumm;
    }


}

