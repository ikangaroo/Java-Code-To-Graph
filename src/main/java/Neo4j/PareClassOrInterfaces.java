package Neo4j;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class PareClassOrInterfaces {
    private MethodDeclaration methodDeclaration;
    @Getter
    @Setter
    private List<String> classNameList=new ArrayList<>();
    public PareClassOrInterfaces(MethodDeclaration me){//针对函数申明来得到当前函数申明中函数调用所使用的所有类
        this.methodDeclaration=me;

    }
    public List<String> findClassList(){
        String className="";
        List<MethodCallExpr> methodCallExprs=this.methodDeclaration.findAll(MethodCallExpr.class);
        //List<VariableDeclarator> variableDeclarators=methodDeclaration.findAll(VariableDeclarator.class);
        for(MethodCallExpr methodCallExpr:methodCallExprs){
            int legth=methodCallExpr.findAll(ObjectCreationExpr.class).size();
            if(legth!=0){
                //通过new来实现函数调用
                ObjectCreationExpr objectCreationExpr=methodCallExpr.findAll(ObjectCreationExpr.class).get(0);
                className=objectCreationExpr.getTypeAsString();
                classNameList.add(className);

            }
            else if(methodCallExpr.getScope().isPresent()&&methodCallExpr.getScope().get().toString().charAt(0)-0<'Z'-0){
                //通过静态类名来实现调用,并且是首字符是大写
                className=methodCallExpr.getScope().get().toString();
                classNameList.add(className);

            }
            else{
                //通过先前声明的对象来调用，小写字母，把收集当前函数声明当中的类或者接口
               //直接在最后全部加上即可
            }
        }
        methodDeclaration.findAll(ClassOrInterfaceType.class).forEach(classOrInterfaceType -> classNameList.add(classOrInterfaceType.getNameAsString()));

        //利用hashset去除重复元素
        Set hashset=new HashSet(classNameList);
        classNameList.clear();
        classNameList.addAll(hashset);
        return classNameList;
    }


}
