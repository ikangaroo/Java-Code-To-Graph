package Neo4j;

import GraphProcess.RangeNode;
import com.github.javaparser.ast.Node;

public class Utils {
    public Utils(){
        //无参构造函数
    }
    public static String addAttributes(Object object){//为CFG每个节点添加属性
        if (object instanceof RangeNode){
            Node node=((RangeNode) object).getmNode();
            return new ParseExpression(node).set2List();
        }
        else {
            System.out.println("不存在RangeNode节点");
            return null;
        }

    }
    public static Node Object2Node(Object object){
        if (object instanceof RangeNode){
            Node node=((RangeNode) object).getNode();
            return node;

        }
        else {
            System.out.println("节点类型错误(!RangeNode)");
            return null;
        }
    }


}
