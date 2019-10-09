package SATEIV;

import GraphProcess.AST2Graph;
import GraphProcess.GenerateGraph;
import GraphProcess.Graph2Json;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.graph.MutableNetwork;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Reports {
    private String mXmlPath;
    private String mRepoPath;
    private String mGraphPath;

    public Reports(String xmlPath, String repoPath, String graphPath) {
        mXmlPath = xmlPath;
        mRepoPath = repoPath;
        mGraphPath = graphPath;
    }

    public void parseXml() throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(new File(mXmlPath));
        List<Node> paths = document.selectNodes("//@path");
        List<Node> lines = document.selectNodes("//@line");
//        List<Node> location = document.selectNodes("//location");
//        location.forEach(node -> System.out.println(node.valueOf("@path")));
//        location.forEach(node -> System.out.println(node.valueOf("@line")));
//        System.out.println(lines);
//        System.out.println(paths);
        Map<String, List<Integer>> pathLine = new HashMap<>();
        for (int i = 0; i < paths.size(); i++) {
            if (!paths.get(i).getText().endsWith(".java")) {
                continue;
            }
            if (pathLine.containsKey(paths.get(i).getText())) {
                pathLine.get(paths.get(i).getText()).add(Integer.valueOf(lines.get(i).getText()));
            } else {
                List<Integer> list = new ArrayList<>();
                list.add(Integer.valueOf(lines.get(i).getText()));
                pathLine.put(paths.get(i).getText(), list);
            }
        }
        System.out.println(pathLine.keySet().size());
        GenerateGraph generateGraph = new GenerateGraph();
        String dir = "C:/reports-Java/";
        int count = 0;
        for (String key : pathLine.keySet()) {
            String path = dir + key;
            AST2Graph ast2Graph = AST2Graph.newInstance(path);
            if (ast2Graph == null) {
                continue;
            }
            List<MethodDeclaration> methods = ast2Graph.getMethodDeclarationIn(pathLine.get(key));
            generateGraph.logTem(path);
            generateGraph.getNewSubNetworks().clear();
            generateGraph.generateSubGraphs(ast2Graph, methods, pathLine.get(key), generateGraph.getNewSubNetworks());
            int current = 1;
            String graphFile = key.replaceAll("/", "_");
            for (MutableNetwork mutableNetwork : generateGraph.getNewSubNetworks()) {
                String jsonFileName = mGraphPath + graphFile + "_" + current + ".txt";
                Graph2Json graph2Json = Graph2Json.newInstance(mutableNetwork);
                graph2Json.saveToJson(jsonFileName);
                System.out.println(jsonFileName);
                current++;
            }
            count++;
        }
        System.out.println(count);
    }

    public static void main(String[] args) throws DocumentException {
        String xmlPath = "C:/reports-Java/parasoft_jetty-fix.xml";
//        String xmlPath = "C:/reports-Java/parasoft_jetty-vln.xml";
//        String xmlPath = "C:/reports-Java/parasoft_tomcat-fix.xml";
//        String xmlPath = "C:/reports-Java/parasoft_tomcat-vln.xml";
        String repoPath = "C:/reports-Java/jetty-6.1.26";
//        String repoPath = "C:/reports-Java/jetty-6.1.16";
//        String repoPath = "C:/reports-Java/apache-tomcat-5.5.33-src";
//        String repoPath = "C:/reports-Java/apache-tomcat-5.5.13-src";
//        String graphPath = "C:\\Users\\Stark.DESKTOP-A6CMICB\\IdeaProjects\\subSATE\\subGraphsBug\\";
        String graphPath = "C:\\Users\\Stark.DESKTOP-A6CMICB\\IdeaProjects\\subSATE\\subGraphsGood\\";
        Reports reports = new Reports(xmlPath, repoPath, graphPath);
        reports.parseXml();
    }
}
