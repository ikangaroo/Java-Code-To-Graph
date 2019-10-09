package CFG;

/*
    ******* 控制流图生成算法的设计与实现 *******
    *
    * 在分析的过程中我们需要用到语法制导翻译技术，其具体做法是：
    * 在对应的文法中嵌入相应的语义动作，其中的语义动作是相应控制流图生成算法的实现
    *
    ******* 该类的用法如下 *******
    *
    * 1、新建一个生成器实例
    * CFGGenerator cfgGenerator = new CFGGenerator();
    *
    * 2、调用生成器实例的 run() 方法，传入 AST 的编译单元 CompilationUnit，该方法会返回一个 MutableNetwork 对象，即 CFG
    * MutableNetwork<Node,String> CFG = cfgGenerator.run(compilationUnit);
    *
    * 3、调用生成器的 printCFG() 方法，可以打印控制流路径
    * cfgGenerator.printCFG();
 */

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class CFGGenerator {

    /*
        ***** 控制流图结点的数据结构 *****
        * 行号，用 tokenRange 代替，唯一标识控制流结点，由 AST.Node 提供
        * 前驱结点链表，用 network 管理
        * 后继结点链表，用 network 管理
        * mPreNodes，用来存放未确定后继结点的控制流结点
        * CFGNodeList，用 network 管理
        * mBreakNodes，存放 break 语句结点的堆栈（分析循环体和switch）
        * mContinueNodes，存放 continue 语句结点的集合堆栈（分析循环体）
        * label_stack，存放 switch 语句结点（分析 switch，case，default）
        * T_NodeList，临时储存 if 语句条件为 True 的未确定后继的结点链表，分析完 else 后，一起存放到 nosuccNodeList
        * Switch_list，用来保存 switch 各个分支的分析结果
        * link(node1, node2)，用来连接前驱和后继结点，用 network 管理
        * CFG_NODE()，创建新的控制流结点，用 network 管理
     */

    private ArrayList<Node> mPreNodes = new ArrayList<>();
    private ArrayDeque<Node> mBreakNodes = new ArrayDeque<>();
    private ArrayDeque<Node> mContinueNodes = new ArrayDeque<>();
//    private ArrayDeque<Node> label_stack = new ArrayDeque<>();
    private ArrayList<Node> mPreTempNodes = new ArrayList<>();
//    private ArrayList<Node> Switch_list = new ArrayList<>();
    public MutableNetwork<Node,String> CFG = NetworkBuilder.directed()
            .allowsParallelEdges(true).nodeOrder(ElementOrder.insertion()).allowsSelfLoops(true)
                .build();

    public MutableNetwork run(CompilationUnit compilationUnit){
        // 通过遍历 AST 构建 CFG
        travelNodeForCFG(compilationUnit);
        return CFG;
    }

    private void travelNodeForCFG(Node parentNode){
        // 只要当前传入结点的孩子结点不为空，就对每一个孩子结点进行分析
        if(!parentNode.getChildNodes().isEmpty()){
            for(Node node : parentNode.getChildNodes()){
                switch (node.getClass().toString().substring("class com.github.javaparser.ast.stmt.".length())){
                    case "TryStmt": {
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                        TryStmt tryStmt = (TryStmt) node;
                        travelNodeForCFG(tryStmt.getTryBlock());
                        mPreTempNodes.addAll(mPreNodes);
                        tryStmt.getCatchClauses().forEach(catchClause -> {
                            addNextExecEdge(tryStmt, catchClause.getParameter());
                            resetPreNodes(catchClause.getParameter());
                            travelNodeForCFG(catchClause.getBody());
                            mPreTempNodes.addAll(mPreNodes);
                            resetPreNodes(node);
                        });
                        mPreNodes.addAll(mPreTempNodes);
                        tryStmt.getFinallyBlock().ifPresent(this::travelNodeForCFG);
                    } break;
                    case "ExpressionStmt":{
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                    } break;
                    case "ReturnStmt":{
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                    } break;
                    case "IfStmt":{
                        processIfStmt(node);
                    } break;
                    case "SwitchStmt":{
                        addNextExecEdgeForAllPres(node);
                        SwitchStmt switchStmt = (SwitchStmt)node;
                        Expression selector = switchStmt.getSelector();
                        addNextExecEdge(node, selector);
                        mPreNodes.clear();
                        mBreakNodes.clear();
                        for(SwitchEntryStmt entry : switchStmt.getEntries()) {
                            mPreNodes.add(selector);
                            addNextExecEdgeForAllPres(entry);
                            entry.getLabel().ifPresent(label -> {
                                addNextExecEdge(entry, label);
                            });
                            resetPreNodes(entry);
                            travelNodeForCFG(entry);
                        }
                        mPreNodes.addAll(mBreakNodes);
                    } break;
                    case "WhileStmt":{
                        WhileStmt whileStmt = (WhileStmt)node;
                        addNextExecEdgeForAllPres(node);
                        addNextExecEdge(node, whileStmt.getCondition());
                        resetPreNodes(whileStmt.getCondition());
                        mBreakNodes.clear();
                        mContinueNodes.clear();
                        travelNodeForCFG(whileStmt.getBody());
                        mPreNodes.addAll(mContinueNodes);
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(whileStmt.getCondition());
                        mPreNodes.addAll(mBreakNodes);
                    } break;
                    case "DoStmt":{
                        DoStmt doStmt = (DoStmt)node;
                        travelNodeForCFG(doStmt.getBody());
                        addNextExecEdgeForAllPres(doStmt.getCondition());
                        resetPreNodes(doStmt.getCondition());
                        travelNodeForCFG(doStmt.getBody());
                        resetPreNodes(doStmt.getCondition());
                    } break;
                    case "ForEachStmt": {
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                        ForEachStmt foreachStmt = (ForEachStmt) node;
                        addNextExecEdgeForAllPres(foreachStmt.getIterable());
                        resetPreNodes(foreachStmt.getIterable());
                        addNextExecEdgeForAllPres(foreachStmt.getVariable());
                        resetPreNodes(foreachStmt.getVariable());
                        travelNodeForCFG(foreachStmt.getBody());
                        addNextExecEdgeForAllPres(foreachStmt.getIterable());
                        resetPreNodes(foreachStmt.getIterable());
                    } break;
                    case "ForStmt":{
                        addNextExecEdgeForAllPres(node);
                        ForStmt forStmt = (ForStmt)node;
                        resetPreNodes(forStmt);
                        forStmt.getInitialization().forEach(init -> {
                            addNextExecEdgeForAllPres(init);
                            resetPreNodes(init);
                        });
                        forStmt.getCompare().ifPresent(compare -> {
                            addNextExecEdgeForAllPres(compare);
                            resetPreNodes(compare);
                        });
                        mBreakNodes.clear();
                        mContinueNodes.clear();
                        travelNodeForCFG(forStmt.getBody());
                        mPreNodes.addAll(mContinueNodes);
                        forStmt.getUpdate().forEach(update -> {
                            addNextExecEdgeForAllPres(update);
                            resetPreNodes(update);
                        });
                        forStmt.getCompare().ifPresent(compare -> {
                            addNextExecEdgeForAllPres(compare);
                            resetPreNodes(compare);
                        });
                        mPreNodes.addAll(mBreakNodes);
                    } break;
                    case "ThrowStmt": {
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                    } break;
                    case "AssertStmt": {
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                    } break;
                    case "LabeledStmt": {
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                    } break;
                    case "SynchronizedStmt": {
                        addNextExecEdgeForAllPres(node);
                        resetPreNodes(node);
                        SynchronizedStmt synchronizedStmt = (SynchronizedStmt) node;
                        addNextExecEdgeForAllPres(synchronizedStmt.getExpression());
                        resetPreNodes(synchronizedStmt.getExpression());
                        travelNodeForCFG(synchronizedStmt.getBody());
                    } break;
                    case "BreakStmt":{
                        addNextExecEdgeForAllPres(node);
                        mPreNodes.clear();
                        mBreakNodes.push(node);
                    } break;
                    case "ContinueStmt":{
                        addNextExecEdgeForAllPres(node);
                        mPreNodes.clear();
                        mContinueNodes.push(node);
                    } break;
                    // case、default
                    default: {
                        System.out.println(node.getClass().toString());
                        travelNodeForCFG(node);
                    } break;
                }
            }
        }
    }

    private void resetPreNodes(Node newPre) {
        mPreNodes.clear();
        mPreNodes.add(newPre);
    }

    private void processIfStmt(Node childNode) {
        addNextExecEdgeForAllPres(childNode);
        IfStmt ifStmt = (IfStmt)childNode;
        addNextExecEdge(childNode, ifStmt.getCondition());
        resetPreNodes(ifStmt.getCondition());
        travelNodeForCFG(ifStmt.getThenStmt());
        // 把 true 的结果保存到 mPreTempNodes
        // mPreTempNodes = mPreNodes; 绝对不能这样写，这样写是引用赋值
        mPreTempNodes.addAll(mPreNodes);
        resetPreNodes(ifStmt.getCondition());
        ifStmt.getElseStmt().ifPresent(elseStmt -> {
            addNextExecEdgeForAllPres(elseStmt);
            if (elseStmt.isIfStmt()) {
                processIfStmt(elseStmt);
            } else {
                resetPreNodes(elseStmt);
                travelNodeForCFG(elseStmt);
            }
        });
        mPreNodes.addAll(mPreTempNodes);
        mPreTempNodes.clear();
    }

    private void addNextExecEdgeForAllPres(Node childNode) {
        if (!mPreNodes.isEmpty()) {
            for (Node node : mPreNodes) {
                addNextExecEdge(node, childNode);
            }
        }
    }

    private void addNextExecEdge(Node pre, Node succ){
        CFG.addEdge(pre, succ, "CF: " + pre.getRange().get().begin + "-->" + succ.getRange().get().begin);
    }

    public void printCFG(){
        for(String edge :CFG.edges()){
            System.out.println(edge);
        }
    }

}