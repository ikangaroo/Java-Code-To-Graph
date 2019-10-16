package Neo4j;

import GraphProcess.AST2Graph;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FunctionClass {
    /**
     * 主要处理4种类型的函数
     * 1、构造函数
     * 2、内部类函数
     * 3、常规类中的函数
     * 4、方法重载（方法参数的长度、方法参数的类型）
     * <p>
     * 可以将这些类别分成2大类：
     * 1、内部类函数
     * 2、外部类函数
     * 不管函数的参数长度，比较的过程中我们加入参数的长度
     */

    String outclassName = "";
    private AST2Graph ast2Graph;
    //只存在一个外部类
    private ClassOrInterfaceDeclaration outclass;
    //可能存在多个内部类
    @Getter
    @Setter
    private List<ClassOrInterfaceDeclaration> innerclass = new ArrayList<>();
    //<外部类，外部类中所有的方法声明>
    @Getter
    @Setter
    private HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> outclassMethods = new HashMap<>();
    //<内部类，内部类中所有的方法声明>
    @Getter
    @Setter
    private HashMap<ClassOrInterfaceDeclaration, List<MethodDeclaration>> innerclassMethods = new HashMap<>();
    //针对文件进行处理
    public FunctionClass(File pfile) {
        this.ast2Graph = AST2Graph.newInstance(pfile.getPath());
        this.outclassName = pfile.getName().split("\\.")[0];


    }

    public void PareMethod() {

        List<MethodDeclaration> allmethodDeclaration = this.ast2Graph.getMethodDeclarations();
        List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = this.ast2Graph.getCompilationUnit().findAll(ClassOrInterfaceDeclaration.class);
        for (ClassOrInterfaceDeclaration classOrInterfaceDeclaration : classOrInterfaceDeclarations) {
            if (classOrInterfaceDeclaration.getNameAsString().equals(outclassName)) {
                //确定外部类
                this.outclass = classOrInterfaceDeclaration;
            } else {
                this.innerclass.add(classOrInterfaceDeclaration);
            }
        }
        //把所有的函数声明进行归类
        for (MethodDeclaration methodDeclaration : allmethodDeclaration) {
            if (methodDeclaration.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
                if (((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get()).getNameAsString().equals(outclassName)) {
                    //函数声明在外部类中
                    if (!outclassMethods.keySet().contains(outclass)) {
                        outclassMethods.put(outclass, new ArrayList<>());
                    }
                    outclassMethods.get(outclass).add(methodDeclaration);
                } else {
                    //函数声明在内部类中
                    if (!innerclassMethods.keySet().contains((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get())) {
                        innerclassMethods.put((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get(), new ArrayList<>());
                    }
                    innerclassMethods.get(methodDeclaration.getParentNode().get()).add(methodDeclaration);
                }
            } else {
                //直接父节点不是类的情况
                //函数声明可能是在新建接口过程中，重写了接口中的方法，这部分先不处理
            }
        }
    }
    public static String getClassOfMethod(MethodDeclaration methodDeclaration){
        String classname="";
        if (methodDeclaration.getParentNode().get() instanceof ClassOrInterfaceDeclaration){
            classname=((ClassOrInterfaceDeclaration) methodDeclaration.getParentNode().get()).getNameAsString();
            return classname;
        }
        else {
            // TODO 父节点不是ClassOrInterfaceDeclaration类型的。未处理
        }
        return classname;





    }

    public static void main(String[] args) {
        String src = "H:\\CodeGraph\\Project\\Sourcedata\\test\\GsonCompatibilityMode.java";
        File file = new File(src);
        FunctionClass functionClass = new FunctionClass(file);
        functionClass.PareMethod();
        System.out.println("done!");

    }
}
