package Neo4j;

import GraphProcess.ParseUtil;
import GraphProcess.RangeNode;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.graph.*;


import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AST2Graph extends ParseUtil  {
    private List<Range> mVistedNodes=new ArrayList<>();
    private ValueGraph<Object,String >mGraph;
    private MutableNetwork<Object,String>mNetwork;
    private List<RangeNode> mDataFlowNode=new ArrayList<>();
    private CompilationUnit mcompilationUnit=null;
    private List<ClassOrInterfaceType> extendClasss=new ArrayList<>();
    private List<Node> mPreNodes=new ArrayList<>();
    private ClassOrInterfaceType mClass=null;
public AST2Graph(String srcPath) throws FileNotFoundException {
        super(srcPath);
    }

 public AST2Graph newInstance(String srcpath){
    try {
        return new AST2Graph(srcpath);
    }
    catch (FileNotFoundException e){
        System.out.println("The filePath didn't exist!");
        e.printStackTrace();
        return null;
    }
    catch (Exception e){
        e.printStackTrace();
        return null;
    }
}

public void InitNetWork(){
    RangeNode.nodeCacheClear();
    mVistedNodes.clear();
    mGraph=ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    mNetwork=NetworkBuilder.directed().allowsParallelEdges(true).build();
    mDataFlowNode.clear();


}
public void ConstructNetwork(Node node){
    this.mClass=(ClassOrInterfaceType) node.getChildNodes().stream().filter(node1 -> node instanceof ClassOrInterfaceType).collect(Collectors.toList()).get(0);//当前文件的类名，以及继承的类名
    travelStatemenNode(node);//单句函数解析
}
public void travelStatemenNode(Node node){//从一个类进入
    if(!node.getChildNodes().isEmpty()){
        for(Node subChildNode:node.getChildNodes()){
            switch (subChildNode.getClass().toString().substring("com.github.javaparser.ast.stmt".length())){
                case "TryStmt":{
                    Pre2Succes(node);
                    //ResetPreNode();

                }
                case "ContinueStmt": {}

            }


        }

    }

}
public  void Pre2Succes(Node node){
    for(Node node1:mPreNodes){
       //mNetwork.addEdge(RangeNode.newInstance(node1),RangeNode.newInstance(node),)
    }
}


}
