package CProcess;

import GraphProcess.Graph2Json;
import GraphProcess.Util;
import com.google.common.graph.MutableNetwork;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTStatement;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSARD {
    public static void main(String[] args) {
        getSARDGraphs("cf", "cfg");
        //getSARDGraphs("CWE-119", "CWE-119G");
    }

    private static void getSARDGraphs(String sourceDir, String graphDir) {
        String source = "../CodeGraph/" + sourceDir + "/";
        String des = "../CodeGraph/" + graphDir + "/";
        //String source = "../benchmark/" + sourceDir + "/";
        //String des = "../benchmark/" + graphDir + "/";
        String desT = des + "trueg/";
        String desF = des + "falseg/";
        Util.mkdirIfNotExists(des);
        Util.mkdirIfNotExists(desT);
        Util.mkdirIfNotExists(desF);
        for (File subDir : new File(source).listFiles()) {
            File[] cweFiles = subDir.listFiles();
            if (cweFiles.length == 1) {//单个文件的操作
//                continue;
                File cwe = cweFiles[0];

                if (cwe.getName().startsWith("CWE")) {
                    genSaveSardSingelGraph(cwe.getPath(), cwe.getName(), desT, desF);
                }
            }
            if (cweFiles.length > 1) {//多个文件的操作
                if (cweFiles[0].getName().startsWith("CWE")) {
                    genSaveSardMultiGraph(cweFiles, desT, desF);
                }
            }
        }
    }

    public static void genSaveSardMultiGraph(File[] files, String desDirT, String desDirF) {//跨越多文件数据集的操作。
        //构建哈希图存储构图：以数据文件名为键，初始化的图作为值，只是存储下，后面进行具体的图构造
        Map<File, BuildGraphC> fileGraphMap = new HashMap<>();
        for (File file : files) {
            if (file.getName().endsWith(".h")) {
                continue;
            }
            BuildGraphC bc = BuildGraphC.newFromFile(file.getPath());
            if (bc == null) {
                System.out.println("Null: " + file.getPath());
            } else {
                fileGraphMap.put(file, bc);
            }
        }
        File aFile = null;
        for (File file : fileGraphMap.keySet()) {
            if (file.getName().endsWith("a.cpp") || file.getName().endsWith("a.c")) {
                aFile = file;
                break;
            }
        }
        if (aFile == null) {
            return;
        }
        BuildGraphC aBC = fileGraphMap.get(aFile);
        int falseG = 0;
        int trueG = 0;
        for (IASTFunctionDefinition dec : aBC.getFunctionDefinitions()) {//已经将我们源码文件的所有函数声明已经加入到代码图当中，循环取函数声明即可
            CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) dec.getDeclarator();//取函数声明
            String funcName = fdTor.getName().toString();
            if (funcName.endsWith("bad")) {
                for (File file : fileGraphMap.keySet()) {//这段代码？
                    fileGraphMap.put(file, BuildGraphC.newFromFile(file.getPath()));//
                }
                aBC = fileGraphMap.get(aFile);
                trueG = getGraphCrossMultiFile(aBC, dec, fileGraphMap, desDirT, aFile, trueG);//保存到trueG文件下
//                System.out.println(trueG);
            }
            if (funcName.endsWith("good")) {//找到函数声明以good结尾的函数
                for (CPPASTFunctionCallExpression fc : aBC.findAll(dec, CPPASTFunctionCallExpression.class)) {//找到good函数种所有放入方法调用
                    String functionCallName = aBC.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();//找到方法调用的函数名字
                    for (IASTFunctionDefinition calledFuncDef : aBC.getFunctionDefinitions()) {//获取所有的函数定义
                        CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
                        String calledName = calledFdTor.getName().toString();//获取方法调用的名字
                        if (functionCallName.equals(calledName)) {
                            for (File file : fileGraphMap.keySet()) {
                                fileGraphMap.put(file, BuildGraphC.newFromFile(file.getPath()));
                            }
                            aBC = fileGraphMap.get(aFile);
                            falseG = getGraphCrossMultiFile(aBC, calledFuncDef, fileGraphMap, desDirF, aFile, falseG);//保存到falseG文件下
//                            System.out.println(falseG);
                        }
                    }
                }
            }
        }
    }

    public static int getGraphCrossMultiFile(BuildGraphC aBC, IASTFunctionDefinition funcDef,
                                             Map<File, BuildGraphC> fileGraphMap, String desDir, File aFile, int graphNum) {
        aBC.initNetwork();
        Map<File, List<IASTFunctionDefinition>> visitedFileFunc = new HashMap<>();//每个文件的所有函数声明集合
        getFuncDefGraphInMultiFiles(aBC, funcDef, fileGraphMap, aFile, visitedFileFunc);//收集所有函数的方法声明
        MutableNetwork<Object, String> network = aBC.getNetwork();
        if (!network.edges().isEmpty()) {
            graphNum++;
            Graph2Json graph2Json = Graph2Json.newInstance(network);
            graph2Json.saveToJson(desDir + aFile.getName() + "_" + graphNum + "_.txt");
            System.out.println(desDir + aFile.getName() + "_" + graphNum + "_.txt");
        }
        return graphNum;
    }

    private static void getFuncDefGraphInMultiFiles(BuildGraphC bc, IASTFunctionDefinition funcDefinition,
                                                   Map<File, BuildGraphC> fileGraphMap, File file,
                                                   Map<File, List<IASTFunctionDefinition>> visitedFileFunc) {//跨文件找到多个函数声明
        bc.buildGraphWithoutInit(funcDefinition);//原有的基础上进行构图

        if (!visitedFileFunc.containsKey(file)) {
            visitedFileFunc.put(file, new ArrayList<>());
        }
        for (Object node : bc.getNetwork().nodes()) {
            if (node instanceof IASTFunctionDefinition) {
                visitedFileFunc.get(file).add((IASTFunctionDefinition) node);
            }
        }
        List<CPPASTFunctionCallExpression> visitedFuncCall = new ArrayList<>();
        // C++中类的名字与其他文件的名字是否一样，若一样则检查方法调用与FieldReference是否在那个文件中声明
        //CPPASTName temp=bc.findAll(funcDefinition.getBody(),CPPASTName.class).get(0);
        List<CPPASTName> cppastNameList=bc.findAll(funcDefinition.getBody(),CPPASTName.class);
       IASTStatement iastStatement= funcDefinition.getBody();
        for (CPPASTName className : bc.findAll(funcDefinition.getBody(), CPPASTName.class)) {//获得funcDefinition的所有变量名和类型，过滤掉以base为后缀的函数调用
            if (className.toString().endsWith("base")) {
                continue;
            }
            for (File otherFile : fileGraphMap.keySet()) {
                if (!otherFile.getName().equals(file.getName()) && otherFile.getName().contains(className.toString())) {
                    if (!fileGraphMap.containsKey(otherFile)) {
                        System.out.println("Not contain the file: " + otherFile.getName());
                        continue;
                    }
                    BuildGraphC otherBC = fileGraphMap.get(otherFile);
                    //当前函数声明中所有被调用的函数，然后判断被调函数的类型，文件之间的调用还是库函数的引用。
                    List <CPPASTFunctionCallExpression> cppastFunctionCallExpressions=bc.findAll(funcDefinition, CPPASTFunctionCallExpression.class);
                    for (CPPASTFunctionCallExpression fc : bc.findAll(funcDefinition, CPPASTFunctionCallExpression.class)) {//获取当前方法声明中的所有方法调用。
                        if (visitedFuncCall.contains(fc)) {
                            continue;
                        }
                        String functionCallName;
                        //测试点1
                        IASTExpression testPoint1=fc.getFunctionNameExpression();
                        //fc 外部调用函数


                        //只处理一些库函数（strlen、malloc、）和一些别的文件中的函数。注意类对象并没有引用。
                        if (fc.getFunctionNameExpression() instanceof CPPASTFieldReference) {//文件之间引用函数
                            functionCallName = ((CPPASTFieldReference) fc.getFunctionNameExpression()).getFieldName().toString();
                        } else {//系统库函数
                            functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
                        }
                        connectFuncCallToOtherFile(bc, fileGraphMap, visitedFileFunc, visitedFuncCall, fc, functionCallName, otherFile, otherBC);
                    }
                }
            }
        }
        // C中方法的名字与其他文件的方法名字是否一样
        for (CPPASTFunctionCallExpression fc : bc.findAll(funcDefinition, CPPASTFunctionCallExpression.class)) {
            if (visitedFuncCall.contains(fc)) {
                continue;
            }
            String functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
            for (File otherFile : fileGraphMap.keySet()) {
                if (!fileGraphMap.containsKey(otherFile)) {
                    System.out.println("Not contain the file: " + otherFile.getName());
                    continue;
                }
                BuildGraphC otherBC = fileGraphMap.get(otherFile);
                connectFuncCallToOtherFile(bc, fileGraphMap, visitedFileFunc, visitedFuncCall, fc, functionCallName, otherFile, otherBC);
            }
        }
    }

    private static void connectFuncCallToOtherFile(BuildGraphC bc, Map<File, BuildGraphC> fileGraphMap,
                                                   Map<File, List<IASTFunctionDefinition>> visitedFileFunc,
                                                   List<CPPASTFunctionCallExpression> visitedFuncCall,
                                                   CPPASTFunctionCallExpression fc, String functionCallName,
                                                   File otherFile, BuildGraphC otherBC) {
        for (IASTFunctionDefinition calledFuncDef : otherBC.getFunctionDefinitions()) {//获取其他文件的函数声明
            CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
            if ((functionCallName.equals(calledFdTor.getName().toString()) || calledFdTor.getName().toString().contains(functionCallName))//可能不完全相等，存在继承，异常操作
                    && fc.getArguments().length == calledFdTor.getParameters().length) {
                visitedFuncCall.add(fc);//在其他文件的函数声明中找到我们传过来的函数。
                // 添加方法调用边
                bc.addFormalArgs(fc, calledFdTor);
                bc.addMethodCall(fc, calledFuncDef);
                // 在已经构造的图的基础上，对被调方法的声明构图
                if (visitedFileFunc.containsKey(otherFile) && visitedFileFunc.get(otherFile).contains(calledFuncDef)) {
                    continue;
                }
                //参数交接，完成递归过程。
                otherBC.initNetwork();
                otherBC.setNetwork(bc.getNetwork());
                otherBC.setEdgeNumber(bc.getEdgeNumber());
                //递归调用，与上面的函数进行相互调用，迭代完成。
                getFuncDefGraphInMultiFiles(otherBC, calledFuncDef, fileGraphMap, otherFile, visitedFileFunc);
                bc.setNetwork(otherBC.getNetwork());
                bc.setEdgeNumber(otherBC.getEdgeNumber());
            }
        }
    }

    public static void genSaveSardSingelGraph(String filePath, String fileName, String desDirT, String desDirF) {//不跨越多文件的操作。
        BuildGraphC bc = BuildGraphC.newFromFile(filePath);
        if (bc == null) {
            System.out.println("Null: " + filePath);
            return;
        }
        int falseG = 0;
        int trueG = 0;
        for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
            CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) dec.getDeclarator();
            String funcName = fdTor.getName().toString();
            if (funcName.endsWith("bad")) { // bad()方法只有一个
                trueG = getFuncDefGraph(bc, dec, fileName, desDirT, trueG);
            }
            bc.initNetwork();
            if (funcName.endsWith("good")) { // good()方法里有多个goodXXX()方法调用，每一个方法调用都是一个独立的图
                for (CPPASTFunctionCallExpression fc : bc.findAll(dec, CPPASTFunctionCallExpression.class)) {//找到当前函数声明中的所有函数调用
                    String functionCallName = bc.findAll(fc.getFunctionNameExpression(), IASTName.class).get(0).toString();
                    for (IASTFunctionDefinition calledFuncDef : bc.getFunctionDefinitions()) {//再次遍历整个文件的函数声明，同一文件中查找
                        CPPASTFunctionDeclarator calledFdTor = (CPPASTFunctionDeclarator) calledFuncDef.getDeclarator();
                        String calledName = calledFdTor.getName().toString();
                        if (functionCallName.equals(calledName)) {
                            falseG = getFuncDefGraph(bc, calledFuncDef, fileName, desDirF, falseG);
                        }
                    }
                }
            }
        }
    }

    public static int getFuncDefGraph(BuildGraphC bc, IASTFunctionDefinition funcDefinition, String fileName, String desDirName, int graphNum) {
        bc.buildGraph(funcDefinition);
        MutableNetwork<Object, String> network = bc.getNetwork();
        if (!network.edges().isEmpty()) {
            graphNum++;
            Graph2Json graph2Json = Graph2Json.newInstance(network);
            graph2Json.saveToJson(desDirName + fileName + "_" + graphNum + "_.txt");
            System.out.println(desDirName + fileName + "_" + graphNum + "_.txt");
        }
        return graphNum;
    }

}
