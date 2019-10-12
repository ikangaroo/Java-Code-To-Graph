package Neo4j;

import GraphProcess.RangeNode;
import com.github.javaparser.HasParentNode;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.jaxen.expr.Step;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Utils {
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
    public static boolean containMethod(MethodDeclaration methodDeclaration, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>> outclassMethods, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>> innertclassMethods){
        for(ClassOrInterfaceDeclaration classOrInterfaceDeclaration:outclassMethods.keySet()){
            if (outclassMethods.get(classOrInterfaceDeclaration).contains(methodDeclaration)){return true;}
        }
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration:innertclassMethods.keySet()){
            if (innertclassMethods.get(classOrInterfaceDeclaration).contains(methodDeclaration)){return true;}
        }
        return false;
    }
public static List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> getfileMethodDeclarationMap(File[] files){
       // 获得所有文件的内部函数声明和外部函数声明
       //<外部类，外部类中所有的方法声明>
    List<HashMap<File, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>>> fileMethodDeclarationMap=new ArrayList<>();
    HashMap<File, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>> allOutclassMethods=new HashMap<>();
       //<内部类，内部类中所有的方法声明>
    HashMap<File,HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>>allInnerclassMethods=new HashMap<>();

    for (File file:files){
        FunctionClass functionClass=new FunctionClass(file);
        functionClass.PareMethod();
        allOutclassMethods.put(file,functionClass.getOutclassMethods());
        allInnerclassMethods.put(file,functionClass.getInnerclassMethods());
    }
    fileMethodDeclarationMap.add(allInnerclassMethods);
    fileMethodDeclarationMap.add(allOutclassMethods);
    return fileMethodDeclarationMap;

}
    public static HashMap<File,HashMap<String,MethodDeclaration>> getcalledExprLocation( HashMap<String,ClassOrInterfaceType> candidateFile,
                                                                                         List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap,
                                                                                         MethodCallExpr methodCallExpr){
        //返回<文件名，类名_out/in,methodDeclaration>，现在只是在内部类、外部类函数中找，(接口实现重写未处理)
        String methodCallExprName=methodCallExpr.getNameAsString();
         HashMap<File,HashMap<String ,MethodDeclaration>> calledExprLocation=new HashMap<>();
         HashMap<File, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>> allInnerclassMethods=fileMethodDeclarationMap.get(0);
         HashMap<File, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>> allOutclassMethods=fileMethodDeclarationMap.get(1);
        for (String filename:candidateFile.keySet()){
            if(filename==candidateFile.get(filename).getNameAsString()){
                //文件名和类名一样，在外部类中调用
                for(File file:allOutclassMethods.keySet()){
                    if(file.getName().equals(filename)){//找到文件
                        HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>> classOrInterfaceDeclarationListHashMap=allOutclassMethods.get(file);
                        ClassOrInterfaceDeclaration target_class=classOrInterfaceDeclarationListHashMap.keySet().stream().filter(classOrInterfaceDeclaration
                                -> classOrInterfaceDeclaration
                                .getNameAsString().concat(".java").equals(filename))
                                .collect(Collectors.toList()).get(0);//外部类中，类名与文件全部一样

                        MethodDeclaration target_method=classOrInterfaceDeclarationListHashMap.get(target_class).stream().filter(methodDeclaration
                                -> methodDeclaration.getNameAsString()
                                .equals(methodCallExprName)&&methodDeclaration.getParameters().size()==methodCallExpr.getArguments().size())
                                .collect(Collectors.toList()).get(0);//函数名一致，并且参数列表的长度一致
                        calledExprLocation.put(file,new HashMap<String, MethodDeclaration>(){{put(target_class.getNameAsString()+"_outer",target_method);}});//外部类
                        return calledExprLocation;//找到即返回

                    }
                }
            }
            else {
                //在内部类中调用
                for(File file:allInnerclassMethods.keySet()){
                    if(filename==file.getName()){//找到文件
                        HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>> classOrInterfaceDeclarationListHashMap=allInnerclassMethods.get(file);
                        //
                        ClassOrInterfaceDeclaration target_class=classOrInterfaceDeclarationListHashMap.keySet().stream().filter(classOrInterfaceDeclaration
                                ->classOrInterfaceDeclaration
                                .getNameAsString().equals(candidateFile.get(filename)))
                                .collect(Collectors.toList()).get(0);
                        MethodDeclaration target_method=classOrInterfaceDeclarationListHashMap.get(target_class).stream().filter(methodDeclaration
                                ->methodDeclaration.getNameAsString()
                                .equals(methodCallExprName)&&methodDeclaration.getParameters().size()==methodCallExpr.getArguments().size() )
                                .collect(Collectors.toList()).get(0);
                        calledExprLocation.put(file,new HashMap<String, MethodDeclaration>(){{put(target_class.getNameAsString()+"_inner",target_method);}});//内部类
                        return calledExprLocation;


                    }
                }
            }
            return calledExprLocation;//这种情况没有找到，系统库函数、或者第三方的函数。

        }

        return calledExprLocation;

    }
    public static HashMap<String,ClassOrInterfaceType> getCandidateFileByClass(MethodDeclaration pmethodDeclaration ,
                                                                               List<HashMap<File, HashMap<ClassOrInterfaceDeclaration,
                                                                                       List<MethodDeclaration>>>> fileMethodDeclarationMap){
        HashMap<String ,ClassOrInterfaceType> candidateFile=new HashMap<>();
        String file=null;
        List<ClassOrInterfaceType> classOrInterfaceTypeList=pmethodDeclaration.findAll(ClassOrInterfaceType.class);//接口查找存在bug
        //处理GsonCompatibilityMode.Builder这种类型的数据，前一个是外部类，后一个是内部类，scope作为修饰符
        for(ClassOrInterfaceType classOrInterfaceType:classOrInterfaceTypeList){
            if(!classOrInterfaceType.getScope().isPresent()){//调用外部类的函数
                file=classOrInterfaceType.getNameAsString()+".java";
                candidateFile.put(file,classOrInterfaceType);
            }
            else {
                file=classOrInterfaceType.getScope().get().getNameAsString()+".java";
                candidateFile.put(file,classOrInterfaceType);
            }

        }

        return candidateFile;

    }
    public static Map<String,List<String>> getcallMethods(File pfile, MethodDeclaration pmethodDeclaration, List<HashMap<File, HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>>>> fileMethodDeclarationMap) {
        /**
         *
         * 通过类找到函数调用的位置，类中的函数必须通过类名或者接口来调用（提升查找效率）
         */
        HashMap<File, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>> allInnerclassMethods=fileMethodDeclarationMap.get(0);
        HashMap<File, HashMap<ClassOrInterfaceDeclaration,List<MethodDeclaration>>> allOutclassMethods=fileMethodDeclarationMap.get(1);
        Map<String, List<String>> CalledMethod = new HashMap<>();
        List<MethodCallExpr> methodCallExprList = pmethodDeclaration.findAll(MethodCallExpr.class);
        HashMap<String,ClassOrInterfaceType> candidateFile=getCandidateFileByClass(pmethodDeclaration,fileMethodDeclarationMap);//候选查找文件集<文件名，类>
        for (MethodCallExpr methodCallExpr : methodCallExprList) {
              //new GsonBuilder().setDataFormat("bdg").create(),最后一个.后面的才是我们的方法调用。
              //通过《文件名,类名》候选集来实现简化查找
              HashMap<File,HashMap<String ,MethodDeclaration>> calledExprLocation=getcalledExprLocation(candidateFile,fileMethodDeclarationMap,methodCallExpr);//查找到函数调用的位置
              if (calledExprLocation.isEmpty()){
                  System.out.println(methodCallExpr.getNameAsString()+"：是系统、库函数");
              }
              else {
                   //只在0号位置存储一个值，下面两个数组
              File[] file=new File[1];//fileName[0]
              String[] className=new String[1];//className[0]
              MethodDeclaration me;
              calledExprLocation.keySet().forEach(file1 -> file[0]=file1);
              calledExprLocation.get(file).keySet().forEach(s -> className[0]=s);
              me=calledExprLocation.get(file[0]).get(className[0]);
              String classend=className[0].split("_")[1];//标识是内部类还是外部类_outer/_inner,后面是否会用到
              String Name=className[0]+"_"+me.getNameAsString();
              if(CalledMethod.keySet().contains(file[0].getName())){
                  CalledMethod.get(file[0]).add(Name);
              }
              else {
                  CalledMethod.put(file[0].getName(),new ArrayList<String>(){{add(Name);}});
              }
              }

        }
    return CalledMethod;
}

}
