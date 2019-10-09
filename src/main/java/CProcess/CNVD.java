package CProcess;

import GraphProcess.Graph2Json;
import GraphProcess.Util;
import com.google.common.graph.MutableNetwork;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.cdt.core.dom.ast.IASTNode;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionCallExpression;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTFunctionDefinition;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CNVD {
    public static void main(String[] args) throws IOException {
        String sourceFileDir = "../VulDeePecker-master/CWE-399/source_files/";
        String cgdFile = "cgd/VulDeeNVD399.txt";
        String saveDir = "../benchmark/nvd399-real/";
        CAPIFunctionName.setFuncTypeCWE399();
//        getNVDGraphs(sourceFileDir, cgdFile, saveDir);

        sourceFileDir = "../CodeGraph/VulDeePecker-master/CWE-119/source_files/";
        cgdFile = "../CodeGraph/cgd/VulDeeNVD119.txt";
        saveDir = "../CodeGraph/benchmark/nvd119-test/";
        CAPIFunctionName.setFuncTypeCWE119();
        getNVDGraphs(sourceFileDir, cgdFile, saveDir);
    }

    public static void getNVDGraphs(String sourceFileDir, String cgdFile, String saveDir) throws IOException {
        Util.mkdirIfNotExists(saveDir);
        Util.mkdirIfNotExists(saveDir + "trueg/");
        Util.mkdirIfNotExists(saveDir + "falseg/");
        Gson gson = new Gson();
        String json = Util.readFileToString(cgdFile);
        ArrayList<CGD> cgds = gson.fromJson(json, new TypeToken<ArrayList<CGD>>(){}.getType());
        for (CGD cgd : cgds) {
            String filePath = sourceFileDir + cgd.getFileName();
            String id = cgd.getId();

            //只是测试cgd中ID为251的这个数据。
            if (!id.equals("251")) {
                continue;
            }
            int line = 0;
            try {
                line = Integer.parseInt(cgd.getLineNumber());
            } catch (Exception e) {
                continue;
            }
            boolean label = false;
            if (cgd.getLabel().equals("1")) {
                label = true;
            }
            genSaveNVDGraph(filePath, line, label, id, saveDir);
        }
    }

    private static void genSaveNVDGraph(String filePath, int line, boolean label, String id, String saveDir) {
        BuildGraphC bc = BuildGraphC.newFromFile(filePath);
        if (bc == null) {
            System.out.println("Null: " + filePath);
            return;
        }
        CPPASTFunctionDefinition funcDefinition = bc.findFuncDefinition(line);
        if (funcDefinition == null) {
            return;
        }
        List<CPPASTFunctionCallExpression> functionCalls = bc.findAll(funcDefinition, CPPASTFunctionCallExpression.class);
        boolean notAPICall = true;
        for (CPPASTFunctionCallExpression call : functionCalls) {
            if (CAPIFunctionName.isAPIFunc(call.getFunctionNameExpression().toString())) {//获取函数的名字，判断函数名是否在我们定义的函数API中。
                notAPICall = false;
                break;
            }
        }
        if (notAPICall) {
            return;
        }
        buildGraphRelateLine(bc, funcDefinition, line);
        MutableNetwork<Object, String> network = bc.getNetwork();
        List<CPPASTFunctionDefinition> fds = new ArrayList<>();
        List<CPPASTFunctionCallExpression> fcs = new ArrayList<>();
        List<IASTNode> nodes = new ArrayList<>();
        List<IASTNode> funcNodes = bc.findAll(funcDefinition, IASTNode.class);
        for (Object node : network.nodes()) {
            if (node instanceof CPPASTFunctionDefinition) {
                fds.add((CPPASTFunctionDefinition) node);
            }
            if (node instanceof CPPASTFunctionCallExpression) {
                if (functionCalls.contains(node)) {
                    fcs.add((CPPASTFunctionCallExpression) node);
                }
            }
            if (node instanceof IASTNode) {
                if (funcNodes.contains(node)) {
                    nodes.add((IASTNode) node);
                }
            }
        }
        if (!network.edges().isEmpty()) {
            Graph2Json graph2Json = Graph2Json.newInstance(network);
            String fileName = Util.getFileNameOfFilePath(filePath);
            String saveTo;
            if (label) {
                saveTo = saveDir + "trueg/" + fileName + "_" + id + "_.txt";
            } else {
                saveTo = saveDir + "falseg/" + fileName + "_" + id + "_.txt";
            }
            graph2Json.saveToJson(saveTo);
            System.out.println(saveTo);
        }
    }

    public static void buildGraphRelateLine(BuildGraphC bc, CPPASTFunctionDefinition funcDefinition, int line) {
        bc.initNetwork();
        bc.setAll(true);
        bc.buildDFG(funcDefinition);
        Set<CPPASTName> related = bc.getSinkRelate(line);
        if (related.isEmpty()) {
            return;
        }
        bc.initNetwork();
        bc.setRelatedNames(related);
        bc.setAll(false);
        bc.buildGraph(funcDefinition);
    }
}
