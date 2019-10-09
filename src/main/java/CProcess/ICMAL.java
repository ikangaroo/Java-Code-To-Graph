package CProcess;

import GraphProcess.Graph2Json;
import com.google.common.graph.MutableNetwork;
import org.eclipse.cdt.core.dom.ast.IASTCompoundStatement;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ICMAL {
    public static void main(String[] args) {
        getGraphs("ctest");
//        getGraphs("valid");
//        getGraphs("train");
    }

    private static void getGraphs(String cat) {
        String des = "../benchmark/";
        {
            String fileDir = des + cat + "/" + "true/";
            File dir = new File(fileDir.replace("true", "trueg"));
            if (!dir.exists()) {
                dir.mkdir();
            }
            for (File file : new File(fileDir).listFiles()) {
                String filePath = file.getPath();
                String graphPath = filePath.replace("true", "trueg");
                genSaveCGraph(filePath, graphPath);
            }
        }
        {
            String fileDir = des + cat + "/" + "false/";
            File dir = new File(fileDir.replace("false", "falseg"));
            if (!dir.exists()) {
                dir.mkdir();
            }
            File[] files = new File(fileDir).listFiles();
//            List<File> filesList = Arrays.asList(files);
//            Collections.shuffle(filesList);
//            int count = 0;
            for (File file : files) {
//            for (File file : filesList) {
//                if (count >= 10000) {
//                    break;
//                }
                String filePath = file.getPath();
                String graphPath = filePath.replace("false", "falseg");
                genSaveCGraph(filePath, graphPath);
//                count++;
            }
        }
    }

    private static void genSaveCGraph(String filePath, String graphPath) {
        BuildGraphC bc = BuildGraphC.newFromFile(filePath);
        if (bc == null) {
            System.out.println("Null: " + filePath);
            return;
        }
        for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
            if (dec.getBody() instanceof IASTCompoundStatement) {
                IASTCompoundStatement body = (IASTCompoundStatement) dec.getBody();
                if (body.getStatements().length == 0) {
                    continue;
                }
            }
            bc.buildGraph(dec);
            MutableNetwork<Object, String> network = bc.getNetwork();
            if (!network.edges().isEmpty()) {
                Graph2Json graph2Json = Graph2Json.newInstance(network);
                graph2Json.saveToJson(graphPath + ".txt");
                System.out.println(graphPath + ".txt");
            }
        }
    }
}
