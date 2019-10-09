package CFG;

import GraphProcess.AST2Graph;
import com.github.javaparser.ast.Node;
import com.google.common.graph.MutableNetwork;

import java.io.FileNotFoundException;

public class Operator {

    public static void main(String[] args) throws FileNotFoundException {


        String filePath = "data/tsExpStmt.java";
        ParserUtil parserUtil = new ParserUtil(filePath);
        AST2Graph ast2Graph = AST2Graph.newInstance(filePath);


//        生成 CFG 部分
        CFGGenerator cfgGenerator = new CFGGenerator();
        cfgGenerator.run(ast2Graph.getCompilationUnit());
//        cfgGenerator.run(parserUtil.construct());
        //通过AST构建图
        //cfgGenerator.printCFG();


        MutableNetwork<Node, String> network = cfgGenerator.CFG;
//        ast2Graph.travelNodeForCFG(ast2Graph.getCompilationUnit());
//        MutableNetwork<Node, String> network = ast2Graph.CFG;
        int i = 1;
        for (String edge : network.edges()) {
            System.out.println(i++);
            for (Node pair : network.incidentNodes(edge)) {
                System.out.println(pair.getRange().get().begin);
            }
        }



//        生成图部分
//        AST2Graph ast2Graph = new AST2Graph(parserUtil.construct());
//        ast2Graph.construct();

//        ast2Graph.printGraph();
//        System.out.println(""+ ast2Graph.format(ast2Graph.construct().nodes()) );
//        System.out.println(ast2Graph.getNetworkFeature(ast2Graph.construct().nodes()));
    }
}