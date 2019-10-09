package CProcess;

import GraphProcess.Graph;
import GraphProcess.Util;
import com.google.common.graph.MutableNetwork;
import com.google.common.graph.NetworkBuilder;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.util.*;

import static GraphProcess.Util.isContain;

//继承CParseUtil，
public class BuildGraphC extends CParseUtil implements Graph {
    public List<IASTNode> mVisitedNodes = new ArrayList<>();
    public List<IASTNode> mVisitedDFGNodes = new ArrayList<>();
    public List<IASTNode> mVisitedCFGNodes = new ArrayList<>();
    // For CFG
    public ArrayList<IASTNode> mPreNodes = new ArrayList<>();
    public ArrayDeque<IASTNode> mBreakNodes = new ArrayDeque<>();
    public ArrayDeque<IASTNode> mContinueNodes = new ArrayDeque<>();
    public ArrayList<IASTNode> mPreTempNodes = new ArrayList<>();
    public int mEdgeNumber;
    public MutableNetwork<Object, String> mNetwork;
//    public MutableNetwork<Object, String> mCFGNetwork;
    public boolean mAll = false;
//    public boolean mAll = true;
    public IASTFunctionDefinition mFunctionDefinition;
    public IASTName mLastUse;
    public IASTName mLastWrite;
    public List<IASTFunctionDefinition> mCalledMethodDecls = new ArrayList<>();
    public Set<CPPASTName> mRelatedNames = new HashSet<>();
    //数据流边的类型
    public String[] mDFGEdgeTypes = new String[]{EDGE_LAST_USE, EDGE_LAST_WRITE, EDGE_COMPUTED_FROM,
            EDGE_LAST_LEXICAL_USE, EDGE_GUARDED_BY, EDGE_GUARDED_BY_NEGATION};


    private BuildGraphC(String srcFilePath) {//父类的构造方法
        super(srcFilePath);
    }

    public static BuildGraphC newFromFile(String srcFilePath) {
        BuildGraphC bc = new BuildGraphC(srcFilePath);//构图
        if (bc.mTranslationUnit == null) {
            return null;
        } else {
            bc.collectFunctionDecls(bc.mTranslationUnit);//构图的过程中使用了CParseUtil解析函数，
            bc.collectVariableNames(bc.mTranslationUnit);
            return bc;
        }
    }

    public void initNetwork() {//初始化网络
        mVisitedNodes.clear();
        mVisitedDFGNodes.clear();
        mNetwork = NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
//        mCFGNetwork = NetworkBuilder.directed().allowsParallelEdges(true).allowsSelfLoops(true).build();
        mEdgeNumber = 0;
        initCFG();
    }

    public void initCFG() {//初始化控制流图
        //初始化控制流图
        mVisitedCFGNodes.clear();
        mLastUse = null;
        mLastWrite = null;
        mPreNodes.clear();
        mBreakNodes.clear();
        mContinueNodes.clear();
        mPreTempNodes.clear();
    }

    public void travelCalled() {//？
        if (mCalledMethodDecls.isEmpty()) {
            return;
        }
        IASTFunctionDefinition called = mCalledMethodDecls.remove(0);
        if (!mVisitedNodes.contains(called)) {
            boolean beforeAll = mAll;
            mAll = true;
            initCFG();
            visitNode(called);
            buildCFG(called);
            buildDFG(called);
            mAll = beforeAll;
        }
        travelCalled();
    }

    public <T extends IASTNode> void buildGraph(T node) {
        initNetwork();
        buildGraphWithoutInit(node);
    }

    public <T extends IASTNode> void buildGraphWithoutInit(T node) {
        visitNode(node);
        buildDFG(node);
        buildCFG(node);
        travelCalled();
    }

    public <T extends IASTNode> boolean visitNode(T node) {
        // 先visit，再 || mAll
        if (node == null || node instanceof IASTComment) {
            return false;
        }
//        System.out.println(node.toString());
        boolean add = mAll;
        if (mVisitedNodes.contains(node)) {
            return add;
        }
        mVisitedNodes.add(node);
        String nodeClassPackage = node.getClass().toString();
        String nodeClass = Util.getClassLastName(nodeClassPackage);
        {
            if (node instanceof IASTTranslationUnit) {
                IASTTranslationUnit t = (IASTTranslationUnit) node;
                return isAddChildAndNext(add, t) || mAll;
            }
            if (node instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition fd = (CPPASTFunctionDefinition) node;
                mFunctionDefinition = fd;
                boolean declarator = visitNode(fd.getDeclarator());
                boolean body = visitNode(fd.getBody());
                mFunctionDefinition = null;
                addChildNode(fd, fd.getDeclSpecifier());
                addChildNode(fd, fd.getDeclarator());
                addChildNode(fd, fd.getBody());
                addNextNode(fd.getDeclSpecifier(), fd.getDeclarator());
                addNextNode(fd.getDeclarator(), fd.getBody());
                add = declarator || body;
                return add || mAll;
            }
            if (node instanceof CPPASTSimpleDeclSpecifier) {
                CPPASTSimpleDeclSpecifier ds = (CPPASTSimpleDeclSpecifier) node;
                boolean decl = visitNode(ds.getDeclTypeExpression());
                if (decl) {
                    addChildNode(ds, ds.getDeclTypeExpression());
                }
                add = add || mAll;
                if (add) {
                    addChildToken(ds, "Type " + ds.getType());
                }
                return add;
            }
            if (node instanceof CPPASTTypeId) {
                CPPASTTypeId t = (CPPASTTypeId) node;
                boolean s = visitNode(t.getDeclSpecifier());
                boolean d = visitNode(t.getAbstractDeclarator());
                if (s) {
                    addChildNode(t, t.getDeclSpecifier());
                }
                if (d) {
                    addChildNode(t, t.getAbstractDeclarator());
                }
                return s || d || mAll;
            }
            if (node instanceof CPPASTNamedTypeSpecifier) {
                CPPASTNamedTypeSpecifier t = (CPPASTNamedTypeSpecifier) node;
                if (visitNode(t.getName())) {
                    addChildNode(t, t.getName());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTAliasDeclaration) {
                CPPASTAliasDeclaration a = (CPPASTAliasDeclaration) node;
                if (mAll) {
                    addChildNode(a, a.getMappingTypeId());
                    addChildNode(a, a.getAlias());
                    addNextNode(a.getMappingTypeId(), a.getAlias());
                }
                return mAll;
            }
            if (node instanceof CPPASTFunctionDeclarator) {
                CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) node;
                fdTor.getExceptionSpecification();
                boolean name = visitNode(fdTor.getName());
                addChildNode(fdTor, fdTor.getName());
                IASTNode before = fdTor.getName();
                for (IASTNode child : fdTor.getParameters()) {
                    if (visitNode(child)) {
                        addChildNode(fdTor, child);
                        if (before != null) {
                            addNextNode(before, child);
                        }
                        before = child;
                    }
                }
                add = name;
                return add || mAll;
            }
            if (node instanceof CPPASTParameterDeclaration) {
                CPPASTParameterDeclaration pd = (CPPASTParameterDeclaration) node;
                boolean declarator = visitNode(pd.getDeclarator());
                if (declarator) {
                    addChildNode(pd, pd.getDeclarator());
                    addChildNode(pd, pd.getDeclSpecifier());
//                    visitNode(pd.getDeclSpecifier());
                    addNextNode(pd.getDeclSpecifier(), pd.getDeclarator());
                }
                add = declarator;
                return add || mAll;
            }
            if (node instanceof CPPASTCompoundStatement) {
                CPPASTCompoundStatement c = (CPPASTCompoundStatement) node;
                add = mAll;
                for (IASTStatement s : c.getStatements()) {
                    boolean child = visitNode(s);
                    if (child) {
                        addChildNode(c, s);
                        add = true;
                    }
                }
                return add;
            }
            if (node instanceof IASTBreakStatement) {
                return mAll;
            }
            if (node instanceof IASTContinueStatement) {
                return mAll;
            }
            if (node instanceof CPPASTDeclarationStatement) {
                CPPASTDeclarationStatement ds = (CPPASTDeclarationStatement) node;
                boolean dec = visitNode(ds.getDeclaration());
                if (dec) {
                    addChildNode(ds, ds.getDeclaration());
                }
                return dec || mAll;
            }
            if (node instanceof CPPASTSimpleDeclaration) {
                CPPASTSimpleDeclaration dc = (CPPASTSimpleDeclaration) node;
                for (IASTDeclarator dor : dc.getDeclarators()) {
                    boolean should = visitNode(dor);
                    add = add || should;
                    if (should) {
                        addChildNode(dc, dor);
                        addNextNode(dc.getDeclSpecifier(), dor);
                    }
                }
                if (add) {
                    addChildNode(dc, dc.getDeclSpecifier());
                }
                return add || mAll;
            }
            if (node instanceof CPPASTArrayDeclarator) {
                CPPASTArrayDeclarator ar = (CPPASTArrayDeclarator) node;
                ar.getArrayModifiers();
                add = processASTDeclarator(ar, add);
                if (add) {
                    IASTNode before = null;
                    for (IASTArrayModifier m : ar.getArrayModifiers()) {
                        addChildNode(ar, m);
                        if (before != null) {
                            addNextNode(before, m);
                        }
                        before = m;
                    }
                    if (before != null) {
                        addNextNode(before, ar.getName());
                    }
                }
                return add;
            }
            if (node instanceof CPPASTDeclarator) {
                return processASTDeclarator((CPPASTDeclarator) node, add);
            }
            if (node instanceof CPPASTConstructorInitializer) {
                CPPASTConstructorInitializer c = (CPPASTConstructorInitializer) node;
                return isAddChildAndNext(add, c) || mAll;
            }
            if (node instanceof IASTEqualsInitializer) {
                IASTEqualsInitializer e = (IASTEqualsInitializer) node;
                IASTNode before = null;
                add = visitNode(e.getInitializerClause());
                if (add) {
                    addChildNode(e, e.getInitializerClause());
                    before = e.getInitializerClause();
                }
                for (IASTNode child : e.getChildren()) {
                    if (!child.equals(e.getInitializerClause()) && visitNode(child)) {
                        add = true;
                        addChildNode(e, child);
                        if (before != null) {
                            addNextNode(before, child);
                        }
                        before = child;
                    }
                }
                return add || mAll;
            }
            if (node instanceof IASTInitializer) {
                IASTInitializer init = (IASTInitializer) node;
                return isAddChildAndNext(add, init) || mAll;
            }
            if (node instanceof CPPASTExpressionStatement) {
                CPPASTExpressionStatement e = (CPPASTExpressionStatement) node;
                boolean expr = visitNode(e.getExpression());
                if (expr) {
                    addChildNode(e, e.getExpression());
                }
                return expr || mAll;
            }
            if (node instanceof CPPASTBinaryExpression) {
                CPPASTBinaryExpression b = (CPPASTBinaryExpression) node;
                StringCapsule operator = StringCapsule.newInstance(getOperatorString(b.getOperator()));
                if (b.getOperator() == 17) { // Operator 17: =
                    boolean assigned = visitNode(b.getOperand1());
                    if (assigned) {
                        addChildNode(b, b.getOperand1());
                        visitNode(b.getOperand2());
                        addChildNode(b, b.getOperand2());
                        addChildToken(b, operator);
                        addNextNode(b.getOperand1(), operator);
                        addNextNode(operator, b.getOperand2());
                        add = true;
                    } else {
                        boolean equals = visitNode(b.getOperand2());
                        if (equals) {
                            addChildNode(b, b.getOperand2());
                        }
                        add = equals;
                    }
                } else {
                    boolean o1 = visitNode(b.getOperand1());
                    boolean o2 = visitNode(b.getOperand2());
                    if (o1) {
                        addChildNode(b, b.getOperand1());
                    }
                    if (o2) {
                        addChildNode(b, b.getOperand2());
                    }
                    if (o1 && o2) {
                        addChildToken(b, operator);
                        addNextNode(b.getOperand1(), operator);
                        addNextNode(operator, b.getOperand2());
                    }
                    add = o1 || o2;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTUnaryExpression) {
                CPPASTUnaryExpression u = (CPPASTUnaryExpression) node;
                if (visitNode(u.getOperand())) {
                    addChildNode(u, u.getOperand());
                    StringCapsule operator = StringCapsule.newInstance(getOperatorString(u.getOperator()));
                    addChildToken(u, operator);
                    addNextNode(operator, u.getOperand());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTArraySubscriptExpression) {
                CPPASTArraySubscriptExpression as = (CPPASTArraySubscriptExpression) node;
                boolean arrayExpr = visitNode(as.getArrayExpression());
                boolean tmpAll = mAll;
                if (arrayExpr) {
                    addChildNode(as, as.getArrayExpression());
                    addNextNode(as.getArrayExpression(), as.getArgument());
                    mAll = true;
                }
                boolean argument = visitNode(as.getArgument());
                if (argument) {
                    addChildNode(as, as.getArgument());
                }
                mAll = tmpAll;
                return arrayExpr || argument || mAll;
            }
            if (node instanceof CPPASTIdExpression) {
                CPPASTIdExpression id = (CPPASTIdExpression) node;
                boolean name = visitNode(id.getName());
                if (name) {
                    addChildNode(id, id.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTWhileStatement) {
                CPPASTWhileStatement w = (CPPASTWhileStatement) node;
                boolean body = visitNode(w.getBody());
                if (body) {
                    addChildNode(w, w.getBody());
                    visitNode(w.getCondition());
                    addChildNode(w, w.getCondition());
                    visitNode(w.getConditionDeclaration());
                    addChildNode(w, w.getConditionDeclaration());
                    add = true;
                } else {
                    boolean cond = visitNode(w.getCondition());
                    boolean conD = visitNode(w.getConditionDeclaration());
                    if (cond) {
                        addChildNode(w, w.getConditionDeclaration());
                    }
                    if (conD) {
                        addChildNode(w, w.getConditionDeclaration());
                    }
                    add = cond || conD;
                }
                return add || mAll;
            }
            if (node instanceof CPPASTNewExpression) {
                CPPASTNewExpression n = (CPPASTNewExpression) node;
                if (visitNode(n.getInitializer())) {
                    addChildNode(n, n.getInitializer());
                    addNextNode(n.getTypeId(), n.getInitializer());
                    add = true;
                }
                add = add || mAll;
                if (add) {
                    addChildNode(n, n.getTypeId());
                }
                return add;
            }
            if (node instanceof CPPASTFunctionCallExpression) {
                CPPASTFunctionCallExpression fc = (CPPASTFunctionCallExpression) node;
                boolean name = visitNode(fc.getFunctionNameExpression());
                IASTNode before = null;
                if (name) {
                    addChildNode(fc, fc.getFunctionNameExpression());
                    add = true;
                    before = fc.getFunctionNameExpression();
                }
                boolean tmpAll = mAll;
                if (add) {
                    mAll = true;
                }
                for (IASTInitializerClause init : fc.getArguments()) {
                    if (visitNode(init)) {
                        addChildNode(fc, init);
                        if (before != null) {
                            addNextNode(before, init);
                        }
                        before = init;
                        add = true;
                    }
                }
                mAll = tmpAll;
                if (add || mAll) {
                    addMethodCallFor(fc);
                }
                return add || mAll;
            }
            if (node instanceof CPPASTLambdaExpression) {
                CPPASTLambdaExpression l = (CPPASTLambdaExpression) node;
                boolean f = visitNode(l.getBody());
                boolean d = visitNode(l.getDeclarator());
                add = f || d || mAll;
                if (add) {
                    addChildNode(l, l.getDeclarator());
                    addChildNode(l, l.getBody());
                    addNextNode(l.getDeclarator(), l.getBody());
                }
                for (IASTNode child : l.getChildren()) {
                    if (!child.equals(l.getDeclarator()) && !child.equals(l.getBody())
                            && visitNode(child)) {
                        add = true;
                        addChildNode(l, child);
                    }
                }
                return add;
            }
            if (node instanceof CPPASTPointerToMember) {
                CPPASTPointerToMember p = (CPPASTPointerToMember) node;
                boolean name = visitNode(p.getName());
                if (name) {
                    addChildNode(p, p.getName());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof ICPPASTStaticAssertDeclaration) {
                ICPPASTStaticAssertDeclaration a = (ICPPASTStaticAssertDeclaration) node;
                boolean d = visitNode(a.getCondition());
                if (d) {
                    addChildNode(a, a.getCondition());
                    addChildNode(a, a.getMessage());
                    addNextNode(a.getCondition(), a.getMessage());
                }
                return d || mAll;
            }
            if (node instanceof CPPASTFieldReference) {
                CPPASTFieldReference fr = (CPPASTFieldReference) node;
                boolean owner = visitNode(fr.getFieldOwner());
                boolean name = visitNode(fr.getFieldName());
                add = owner || name || mAll;
                if (add) {
                    addChildNode(fr, fr.getFieldOwner());
                    addChildNode(fr, fr.getFieldName());
                    addNextNode(fr.getFieldOwner(), fr.getFieldName());
                }
                return add;
            }
            if (node instanceof CPPASTIfStatement) {
                CPPASTIfStatement ifs = (CPPASTIfStatement) node;
                boolean c = visitNode(ifs.getConditionExpression());
                if (c) {
                    addChildNode(ifs, ifs.getConditionExpression());
                }
                boolean cd = visitNode(ifs.getConditionDeclaration());
                if (cd) {
                    addChildNode(ifs, ifs.getConditionDeclaration());
                }
                boolean then = visitNode(ifs.getThenClause());
                if (then) {
                    addChildNode(ifs, ifs.getThenClause());
                }
                boolean el = visitNode(ifs.getElseClause());
                if (el) {
                    addChildNode(ifs, ifs.getElseClause());
                }
                boolean init = visitNode(ifs.getInitializerStatement());
                if (init) {
                    addChildNode(ifs, ifs.getInitializerStatement());
                }
                return  c || cd || then || el || init || mAll;
            }
            if (node instanceof CPPASTCastExpression) {
                CPPASTCastExpression ca = (CPPASTCastExpression) node;
                StringCapsule operator = StringCapsule.newInstance(getOperatorString(ca.getOperator()));
                if (visitNode(ca.getOperand())) {
                    addChildNode(ca, ca.getOperand());
                    addChildToken(ca, operator);
                    addChildNode(ca, ca.getTypeId());
                    addNextNode(ca.getTypeId(), operator);
                    addNextNode(operator, ca.getOperand());
                    add = true;
                }
                return add || mAll;
            }
            if (node instanceof IASTConditionalExpression) {
                IASTConditionalExpression c = (IASTConditionalExpression) node;
                boolean logical = visitNode(c.getLogicalConditionExpression());
                boolean positive = visitNode(c.getPositiveResultExpression());
                boolean negative = visitNode(c.getNegativeResultExpression());
                if (logical) {
                    addChildNode(c, c.getLogicalConditionExpression());
                }
                if (positive) {
                    addChildNode(c, c.getPositiveResultExpression());
                }
                if (negative) {
                    addChildNode(c, c.getNegativeResultExpression());
                }
                return logical || positive || negative || mAll;
            }
            if (node instanceof CPPASTCapture) {
                CPPASTCapture c = (CPPASTCapture) node;
                boolean id = visitNode(c.getIdentifier());
                if (id) {
                    addChildNode(c, c.getIdentifier());
                }
                add = id;
                for (IASTNode child : c.getChildren()) {
                    if (!child.equals(c.getIdentifier()) && visitNode(child)) {
                        add = true;
                        addChildNode(c, child);
                    }
                }
                return add || mAll;
            }
            if (node instanceof CPPASTDeleteExpression) {
                CPPASTDeleteExpression d = (CPPASTDeleteExpression) node;
                boolean expr = visitNode(d.getOperand());
                if (expr) {
                    addChildNode(d, d.getOperand());
                }
                return expr || mAll;
            }
            if (node instanceof ICPPASTForStatement) {
                CPPASTForStatement f = (CPPASTForStatement) node;
                boolean init = visitNode(f.getInitializerStatement());
                if (init) {
                    addChildNode(f, f.getInitializerStatement());
                }
                boolean iter = visitNode(f.getIterationExpression());
                if (iter) {
                    addChildNode(f, f.getIterationExpression());
                }
                boolean cond = visitNode(f.getConditionExpression());
                if (cond) {
                    addChildNode(f, f.getConditionExpression());
                }
                boolean conD = visitNode(f.getConditionDeclaration());
                if (conD) {
                    addChildNode(f, f.getConditionDeclaration());
                }
                boolean body = visitNode(f.getBody());
                if (body) {
                    addChildNode(f, f.getBody());
                }
                return init || iter || cond || conD || body || mAll;
            }
            if (node instanceof CPPASTTryBlockStatement) {
                CPPASTTryBlockStatement t = (CPPASTTryBlockStatement) node;
                boolean tb = visitNode(t.getTryBody());
                if (tb) {
                    addChildNode(t, t.getTryBody());
                    add = true;
                }
                for (ICPPASTCatchHandler ch : t.getCatchHandlers()) {
                    if (visitNode(ch)) {
                        addChildNode(t, ch);
                        add = true;
                    }
                }
                return add || mAll;
            }
            if (node instanceof CPPASTSwitchStatement) {
                CPPASTSwitchStatement s = (CPPASTSwitchStatement) node;
                boolean cond = visitNode(s.getControllerDeclaration());
                boolean cone = visitNode(s.getControllerExpression());
                boolean init = visitNode(s.getInitializerStatement());
                boolean body = visitNode(s.getBody());
                if (cond) {
                    addChildNode(s, s.getControllerDeclaration());
                }
                if (cone) {
                    addChildNode(s, s.getControllerExpression());
                }
                if (init) {
                    addChildNode(s, s.getInitializerStatement());
                }
                if (body) {
                    addChildNode(s, s.getBody());
                }
                return cond || cone || init || body;
            }
            if (node instanceof IASTCaseStatement) {
                IASTCaseStatement c = (IASTCaseStatement) node;
                boolean expr = visitNode(c.getExpression());
                if (expr) {
                    addChildNode(c, c.getExpression());
                    add = true;
                }
                for (IASTNode child : c.getChildren()) {
                    if (!child.equals(c.getExpression()) && visitNode(child)) {
                        addChildNode(c, child);
                        add = true;
                    }
                }
                return add || mAll;
            }
            if (node instanceof ICPPASTFunctionWithTryBlock) {
                ICPPASTFunctionWithTryBlock ftb = (ICPPASTFunctionWithTryBlock) node;
                return isAddChild(add, ftb) || mAll;
            }
            if (node instanceof CPPASTCatchHandler) {
                CPPASTCatchHandler c = (CPPASTCatchHandler) node;
                boolean body = visitNode(c.getCatchBody());
                if (body) {
                    add = true;
                    addChildNode(c, c.getCatchBody());
                    visitNode(c.getDeclaration());
                    addChildNode(c, c.getDeclaration());
                    addNextNode(c.getDeclaration(), c.getCatchBody());
                }
                return add || mAll;
            }
            if (node instanceof CPPASTUsingDeclaration) {
                CPPASTUsingDeclaration u = (CPPASTUsingDeclaration) node;
                boolean name = visitNode(u.getName());
                if (name) {
                    addChildNode(u, u.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTDoStatement) {
                CPPASTDoStatement d = (CPPASTDoStatement) node;
                boolean cond = visitNode(d.getCondition());
                boolean body = visitNode(d.getBody());
                if (body) {
                    addChildNode(d, d.getBody());
                    addChildNode(d, d.getCondition());
                    addNextNode(d.getCondition(), d.getBody());
                } else if (cond){
                    addChildNode(d, d.getCondition());
                }
                return body || cond || mAll;
            }
            if (node instanceof CPPASTGotoStatement) {
                CPPASTGotoStatement g = (CPPASTGotoStatement) node;
                boolean name = visitNode(g.getName());
                if (name) {
                    addChildNode(g, g.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTLabelStatement) {
                CPPASTLabelStatement label = (CPPASTLabelStatement) node;
                boolean name = visitNode(label.getName());
                if (name) {
                    addChildNode(label, label.getName());
                }
                return name || mAll;
            }
            if (node instanceof CPPASTNullStatement) {
                CPPASTNullStatement n = (CPPASTNullStatement) node;
                return isAddChild(add, n) || mAll;
            }
            if (node instanceof CPPASTReturnStatement) {
                CPPASTReturnStatement r = (CPPASTReturnStatement) node;
                boolean value = visitNode(r.getReturnValue());
                boolean arg = visitNode(r.getReturnArgument());
                if (value) {
                    addChildNode(r, r.getReturnValue());
                }
                if (arg) {
                    addChildNode(r, r.getReturnArgument());
                }
                addReturnTo(r, mFunctionDefinition);
                return value || arg || mAll;
            }
            if (node instanceof CPPASTLiteralExpression) {
                CPPASTLiteralExpression literal = (CPPASTLiteralExpression) node;
                literal.getKind();
                literal.getOperatorName();
                literal.getValue();
                literal.getValueCategory();
            }
            if (node instanceof CPPASTName) {
                CPPASTName n = (CPPASTName) node;
                if (mRelatedNames.contains(n)) {
                    return true;
                }
                return mAll;
            }
            {
                return isAddChildAndNext(add, node) || mAll;
            }
        }
    }

    public void addMethodCallFor(CPPASTFunctionCallExpression functionCaller) {
        if (findAll(functionCaller.getFunctionNameExpression(), IASTName.class).size() == 1) {
            String functionCallName = findAll(functionCaller.getFunctionNameExpression(), IASTName.class).get(0).toString();
            for (IASTFunctionDefinition called : mFunctionDefinitions) {
                CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) called.getDeclarator();
                if (functionCallName.equals(fdTor.getName().toString())
                        && functionCaller.getArguments().length == fdTor.getParameters().length) {
                    addFormalArgs(functionCaller, fdTor);
                    addMethodCall(functionCaller, called);
                    break;
                }
            }
        }
    }

    public void addMethodCall(CPPASTFunctionCallExpression caller, IASTFunctionDefinition called) {
        putEdge(caller, called, EDGE_METHOD_CALL);
        mCalledMethodDecls.add(called);
    }

    public void addFormalArgs(CPPASTFunctionCallExpression caller, CPPASTFunctionDeclarator called) {
        ICPPASTParameterDeclaration[] parameters = called.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            putEdge(caller.getArguments()[i], parameters[i].getDeclarator().getName(), EDGE_FORMAL_ARG_NAME);
        }
    }

    public <T extends IASTNode> boolean buildCFG(T node) {
        // 先visit，再 || mAll
        if (node == null || node instanceof IASTComment) {
            return false;
        }
        boolean add = mAll;
        if (mVisitedCFGNodes.contains(node)) {
            return add;
        }
        mVisitedCFGNodes.add(node);
        {
            if (!mNetwork.nodes().contains(node)) {
                return mAll;
            } else {
                add = true;
            }
            if (node instanceof CPPASTTryBlockStatement) {
                CPPASTTryBlockStatement t = (CPPASTTryBlockStatement) node;
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                buildCFG(t.getTryBody());
                mPreTempNodes.addAll(mPreNodes);
                for (ICPPASTCatchHandler catchClause : t.getCatchHandlers()) {
                    if (buildCFG(catchClause.getCatchBody())) {
                        addNextExecEdge(t, catchClause.getDeclaration());
                        resetPreNodes(catchClause.getDeclaration());
                        mPreTempNodes.addAll(mPreNodes);
                        resetPreNodes(node);
                    }
                }
                mPreNodes.addAll(mPreTempNodes);
                return true;
            }
            if (node instanceof CPPASTReturnStatement) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                return true;
            }
            if (node instanceof CPPASTIfStatement) {
                processIfStmt((CPPASTIfStatement) node);
                return true;
            }
            if (node instanceof CPPASTSwitchStatement) {
                CPPASTSwitchStatement switchStmt = (CPPASTSwitchStatement) node;
                addNextExecEdgeForAllPres(node);
                if (buildCFG(switchStmt.getControllerExpression())) {
                    addNextExecEdge(node, switchStmt.getControllerExpression());
                    mPreNodes.clear();
                    mBreakNodes.clear();
                }
                if (buildCFG(switchStmt.getBody())) {
                    for (IASTNode child : switchStmt.getBody().getChildren()) {
                        if (buildCFG(child)) {
                            mPreNodes.add(switchStmt.getControllerExpression());
                            addNextExecEdgeForAllPres(child);
                            resetPreNodes(child);
                        }
                    }
                    mPreNodes.addAll(mBreakNodes);
                }
                return true;
            }
            if (node instanceof CPPASTWhileStatement) {
                CPPASTWhileStatement whileStmt = (CPPASTWhileStatement) node;
                addNextExecEdgeForAllPres(node);
                addNextExecEdge(node, whileStmt.getCondition());
                resetPreNodes(whileStmt.getCondition());
                mBreakNodes.clear();
                mContinueNodes.clear();
                buildCFG(whileStmt.getBody());
                mPreNodes.addAll(mContinueNodes);
                addNextExecEdgeForAllPres(node);
                resetPreNodes(whileStmt.getCondition());
                mPreNodes.addAll(mBreakNodes);
                return true;
            }
            if (node instanceof CPPASTDoStatement) {
                CPPASTDoStatement doStmt = (CPPASTDoStatement) node;
                buildCFG(doStmt.getBody());
                addNextExecEdgeForAllPres(doStmt.getCondition());
                resetPreNodes(doStmt.getCondition());
                buildCFG(doStmt.getBody());
                resetPreNodes(doStmt.getCondition());
                return true;
            }
            if (node instanceof CPPASTForStatement) {
                CPPASTForStatement forStmt = (CPPASTForStatement) node;
                addNextExecEdgeForAllPres(node);
                resetPreNodes(forStmt);
                if (buildCFG(forStmt.getInitializerStatement())) {
                    addNextExecEdgeForAllPres(forStmt.getInitializerStatement());
                    resetPreNodes(forStmt.getInitializerStatement());
                }
                if (buildCFG(forStmt.getConditionExpression())) {
                    addNextExecEdgeForAllPres(forStmt.getConditionExpression());
                    resetPreNodes(forStmt.getConditionExpression());
                }
                mBreakNodes.clear();
                mContinueNodes.clear();
                if (buildCFG(forStmt.getBody())) {
                    mPreNodes.addAll(mContinueNodes);
                }
                if (buildCFG(forStmt.getIterationExpression())) {
                    addNextExecEdgeForAllPres(forStmt.getIterationExpression());
                    resetPreNodes(forStmt.getIterationExpression());
                }
                if (buildCFG(forStmt.getConditionExpression())) {
                    addNextExecEdgeForAllPres(forStmt.getConditionExpression());
                    resetPreNodes(forStmt.getConditionExpression());
                }
                mPreNodes.addAll(mBreakNodes);
                return true;
            }
            if (node instanceof ICPPASTStaticAssertDeclaration) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                return true;
            }
            if (node instanceof CPPASTLabelStatement) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                return true;
            }
            if (node instanceof CPPASTBreakStatement) {
                addNextExecEdgeForAllPres(node);
                mPreNodes.clear();
                mBreakNodes.push(node);
                return true;
            }
            if (node instanceof CPPASTContinueStatement) {
                addNextExecEdgeForAllPres(node);
                mPreNodes.clear();
                mContinueNodes.push(node);
                return true;
            }
            if (node instanceof CPPASTExpressionStatement) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                return true;
            }
            if (node instanceof CPPASTDeclarationStatement) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                return true;
            }
            if (node instanceof CPPASTCompoundStatement) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                for (IASTNode child : node.getChildren()) {
                    buildCFG(child);
                }
                return true;
            }
            if (node instanceof IASTStatement) {
                addNextExecEdgeForAllPres(node);
                resetPreNodes(node);
                return true;
            }
            {
                for (IASTNode child : node.getChildren()) {
                    buildCFG(child);
                }
                return true;
            }
        }
    }

    public <T extends IASTNode> boolean buildDFG(T node) {
        // 先visit，再 || mAll
        if (node == null || node instanceof IASTComment) {
            return false;
        }
        boolean add = mAll;
        if (mVisitedDFGNodes.contains(node)) {
            return add;
        }
        mVisitedDFGNodes.add(node);
        {
            if (node instanceof CPPASTFunctionDefinition) {
                CPPASTFunctionDefinition fd = (CPPASTFunctionDefinition) node;
                boolean param = false;
                IASTFunctionDeclarator dec = fd.getDeclarator();
                if (dec instanceof CPPASTFunctionDeclarator) {
                    CPPASTFunctionDeclarator fdTor = (CPPASTFunctionDeclarator) dec;
                    for (ICPPASTParameterDeclaration parameter : fdTor.getParameters()) {
                        if (buildDFG(parameter.getDeclarator().getName())) {
                            addDataFlowVarLinksForASTName(fd, parameter.getDeclarator().getName(), parameter);
                            mVisitedDFGNodes.add(parameter.getDeclarator());
                            param = true;
                        }
                    }
                }
                boolean body = buildDFG(fd.getBody());
                add = param || body;
                return add || mAll;
            }
            if (node instanceof CPPASTDeclarationStatement){
                CPPASTDeclarationStatement d = (CPPASTDeclarationStatement) node;
                if (d.getParent() != null && d.getDeclaration() instanceof CPPASTSimpleDeclaration) {
                    CPPASTSimpleDeclaration declaration = ((CPPASTSimpleDeclaration) d.getDeclaration());
                    for (IASTDeclarator dTor : declaration.getDeclarators()) {
                        if (buildDFG(dTor.getName()) && dTor instanceof CPPASTDeclarator) {
                            addDataFlowVarLinksForVarDector(d.getParent(), (CPPASTDeclarator) dTor);
                            add = true;
                        }
                    }
                }
                return add || mAll;
            }
//            if (node instanceof IASTName) {
            if (node instanceof CPPASTName) {
                CPPASTName n = (CPPASTName) node;
                if (mRelatedNames.contains(n)) {
                    return true;
                }
                return add || mAll;
            }
            {
                for (IASTNode child : node.getChildren()) {
                    if (buildDFG(child)) {
                        add = true;
                    }
                }
                return add || mAll;
            }
        }
    }

    public void addDataFlowVarLinksForVarDector(IASTNode parent, CPPASTDeclarator variableDeclarator) {
        IASTName specificName = variableDeclarator.getName();
        if (variableDeclarator.getInitializer() != null) {
            addComputedFromList(specificName, getVariableNames(variableDeclarator.getInitializer()));
        }
        addDataFlowVarLinksForASTName(parent, specificName, variableDeclarator);
    }

    private void addDataFlowVarLinksForASTName(IASTNode parent, IASTName specificName, IASTNode dataFlowBeforeNode) {
        mLastUse = specificName;
        mLastWrite = specificName;
        // Process the data flow between neighbor nodes of variableDeclarator and nodes in assign or Stmts.
        updateBlockDataFlow(parent, specificName, dataFlowBeforeNode);
    }

    public void updateBlockDataFlow(IASTNode parent, IASTName specificName, IASTNode dataFlowBeforeNode) {
        for (IASTNode exprOrStmt : getAssignsOrStmtsContains(parent, specificName)) {
            updateLastUseWriteOfVariables(
                    getSpecificVariableFlowsBetweenNodes(parent, specificName, dataFlowBeforeNode, exprOrStmt));
            addDataFlowEdge(exprOrStmt, specificName);
            dataFlowBeforeNode = exprOrStmt;
        }
        updateLastUseWriteOfVariables(
                getSpecificVariableFlowsLastNodes(parent, specificName, dataFlowBeforeNode));
    }

    public void addDataFlowEdge(IASTNode node, IASTName variableName) {
        // TODO: Process every type node, such as If/For/While, add data flow edge in it.
        if (node instanceof CPPASTBinaryExpression) {
            CPPASTBinaryExpression b = (CPPASTBinaryExpression) node;
            if (isAssignExpr(b)) {
                addDataFlowEdgeAssignExpr(b, variableName);
            }
            return;
        }
        if (node instanceof CPPASTIfStatement) {
            addDataFlowEdgeIfStmt((CPPASTIfStatement) node, variableName);
            return;
        }
        if (node instanceof CPPASTWhileStatement) {
            addDataFlowEdgeWhileStmt((CPPASTWhileStatement) node, variableName);
            return;
        }
        if (node instanceof CPPASTDoStatement) {
            addDataFlowEdgeDoStmt((CPPASTDoStatement) node, variableName);
            return;
        }
        if (node instanceof CPPASTForStatement) {
            addDataFlowEdgeForStmt((CPPASTForStatement) node, variableName);
            return;
        }
        if (node instanceof CPPASTSwitchStatement) {
            addDataFlowEdgeSwitchStmt((CPPASTSwitchStatement) node, variableName);
            return;
        }
        if (node instanceof CPPASTTryBlockStatement) {
            addDataFlowEdgeTryStmt((CPPASTTryBlockStatement) node, variableName);
            return;
        }
        {// TODO: improve
//            updateBlockDataFlow(node, variableName);
        }
    }

    public void addDataFlowEdgeTryStmt(CPPASTTryBlockStatement tryStmt, IASTName variableName) {
        // TODO: improve
        updateBlockDataFlow(tryStmt.getTryBody(), variableName);
        for (ICPPASTCatchHandler catchHandler : tryStmt.getCatchHandlers()) {
            updateBlockDataFlow(catchHandler, variableName);
        }
    }

    public void addDataFlowEdgeSwitchStmt(CPPASTSwitchStatement switchStmt, IASTName variableName) {
        // TODO: improve
        updateBlockDataFlow(switchStmt.getControllerExpression(), variableName);
        updateBlockDataFlow(switchStmt.getBody(), variableName);
    }

    public void addDataFlowEdgeForStmt(CPPASTForStatement forStmt, IASTName variableName) {
        IASTName lastWrite = mLastWrite;
        updateBlockDataFlow(forStmt.getInitializerStatement(), variableName);
        updateBlockDataFlow(forStmt.getConditionExpression(), variableName);
        updateBlockDataFlow(forStmt.getIterationExpression(), variableName);
        updateBlockDataFlow(forStmt.getBody(), variableName);
        updateLastAgainWhenLoop(forStmt, variableName, lastWrite);
    }

    public void addDataFlowEdgeDoStmt(CPPASTDoStatement doStmt, IASTName variableName) {
        IASTName lastWrite = mLastWrite;
        updateBlockDataFlow(doStmt.getBody(), variableName);
        updateBlockDataFlow(doStmt.getCondition(), variableName);
        updateLastAgainWhenLoop(doStmt, variableName, lastWrite);
    }

    public void updateBlockDataFlow(IASTNode parent, IASTName specificName) {
        List<IASTNode> exprOrStmts = getAssignsOrStmtsContains(parent, specificName);
        if (exprOrStmts.isEmpty()) {
            updateLastUseWriteOfVariables(getSpecificVariableFlows(parent, specificName));
        } else {
            IASTNode dataFlowBeforeNode = exprOrStmts.get(0);
            updateLastUseWriteOfVariables(getSpecificVariableFlowsStartNodes(parent, specificName, dataFlowBeforeNode));
            addDataFlowEdge(dataFlowBeforeNode, specificName);
            exprOrStmts.remove(dataFlowBeforeNode);
            for (IASTNode exprOrStmt : exprOrStmts) {
                updateLastUseWriteOfVariables(
                        getSpecificVariableFlowsBetweenNodes(parent, specificName, dataFlowBeforeNode, exprOrStmt));
                addDataFlowEdge(exprOrStmt, specificName);
                dataFlowBeforeNode = exprOrStmt;
            }
            updateLastUseWriteOfVariables(
                    getSpecificVariableFlowsLastNodes(parent, specificName, dataFlowBeforeNode));
        }
    }

    public void addDataFlowEdgeWhileStmt(CPPASTWhileStatement whileStmt, IASTName variableName) {
        IASTName lastWrite = mLastWrite;
        updateBlockDataFlow(whileStmt.getCondition(), variableName);
        updateBlockDataFlow(whileStmt.getBody(), variableName);
        // getSpecificVariableFlowsUntilFirstWrite: 没有什么是加一个中间层解决不了的。
        updateLastAgainWhenLoop(whileStmt, variableName, lastWrite);
    }

    public void updateLastAgainWhenLoop(IASTNode loopNode, IASTName variableName, IASTName lastWrite) {
        List<IASTName> variableFlows = getSpecificVariableFlowsUntilFirstWrite(loopNode, variableName);
        if (!variableFlows.isEmpty()) {
            if (!lastWrite.equals(mLastWrite)) {
                addLastWriteList(variableFlows, mLastWrite);
            }
            addLastUse(variableFlows.get(0), mLastUse);
        }
    }

    public void addDataFlowEdgeAssignExpr(CPPASTBinaryExpression assignExpr, IASTName variableName) {
        for (IASTName variableNameExpr : getSpecificVariableFlows(assignExpr.getOperand1(), variableName)) {
            addComputedFromList(variableNameExpr, getVariableNames(assignExpr.getOperand2()));
            updateLastUseWriteOfVariables(getSpecificVariableFlows(assignExpr.getOperand2(), variableName));
            updateLastUseWrite(variableNameExpr);
            mLastWrite = variableNameExpr;
        }
    }

    public void addDataFlowEdgeIfStmt(CPPASTIfStatement ifStmt, IASTName variableName) {
        updateBlockDataFlow(ifStmt.getConditionExpression(), variableName);
        IASTName lastUse = mLastUse;
        IASTName lastWrite = mLastWrite;
        updateBlockDataFlow(ifStmt.getThenClause(), variableName);
        if (ifStmt.getElseClause() != null) {
            IASTName lastLexicalUse = mLastUse;
            mLastUse = lastUse;
            mLastWrite = lastWrite;
            updateBlockDataFlow(ifStmt.getElseClause(), variableName);
            addLastLexicalUse(mLastUse, lastLexicalUse);
        }
    }

    public void updateLastUseWriteOfVariables(List<IASTName> variableFlows) {
        for (IASTName variableNameExpr : variableFlows) {
            updateLastUseWrite(variableNameExpr);
        }
    }

    public void updateLastUseWrite(IASTName variableName) {
        addLastUse(variableName, mLastUse);
        addLastWrite(variableName, mLastWrite);
        mLastUse = variableName;
    }

    public void processIfStmt(CPPASTIfStatement ifStmt) {
        addNextExecEdgeForAllPres(ifStmt);
        buildCFG(ifStmt.getConditionExpression());
        addNextExecEdge(ifStmt, ifStmt.getConditionExpression());
        resetPreNodes(ifStmt.getConditionExpression());
        buildCFG(ifStmt.getThenClause());
        // 把 true 的结果保存到 mPreTempNodes
        // mPreTempNodes = mPreNodes; 绝对不能这样写，这样写是引用赋值
        mPreTempNodes.addAll(mPreNodes);
        resetPreNodes(ifStmt.getConditionExpression());
        IASTStatement elseStmt = ifStmt.getElseClause();
        if (mNetwork.nodes().contains(elseStmt)){
            addNextExecEdgeForAllPres(elseStmt);
            if (elseStmt instanceof CPPASTIfStatement) {
                processIfStmt((CPPASTIfStatement) elseStmt);
            } else {
                resetPreNodes(elseStmt);
                buildCFG(elseStmt);
            }
        }
        mPreNodes.addAll(mPreTempNodes);
        mPreTempNodes.clear();
    }

    public void resetPreNodes(IASTNode newPre) {
        mPreNodes.clear();
        mPreNodes.add(newPre);
    }

    public void addNextExecEdgeForAllPres(IASTNode succNode) {
        for (IASTNode preNode : mPreNodes) {
            addNextExecEdge(preNode, succNode);
        }
    }

    public void addNextExecEdge(IASTNode pre, IASTNode succ) {
        if (mNetwork.nodes().contains(pre) && mNetwork.nodes().contains(succ)) {
            putEdge(pre, succ, EDGE_NEXT_EXEC);
        }
//        putEdge(pre, succ, EDGE_NEXT_EXEC);
    }

    public void addLastWriteList(List<IASTName> nowNodes, IASTName lastWrite) {
        nowNodes.forEach(now -> addLastWrite(now, lastWrite));
    }

    public void addComputedFromList(IASTName now, List<IASTName> computedFroms) {
        computedFroms.forEach(computedFrom -> addComputedFrom(now, computedFrom));
    }

    public void addComputedFrom(IASTName now, IASTName computedFrom) {
        putEdge(now, computedFrom, EDGE_COMPUTED_FROM);
    }

    public void addLastLexicalUse(IASTName now, IASTName lastUse) {
        putEdge(now, lastUse, EDGE_LAST_LEXICAL_USE);
        putEdge(now, lastUse, EDGE_COMPUTED_FROM);
    }

    public void addLastUse(IASTName now, IASTName lastUse) {
    putEdge(now, lastUse, EDGE_LAST_USE);
}

    public void addLastWrite(IASTName now, IASTName lastWrite) {
        putEdge(now, lastWrite, EDGE_LAST_WRITE);
    }

    public void addReturnTo(CPPASTReturnStatement r, IASTFunctionDefinition functionDefinition) {
        putEdge(r, functionDefinition, EDGE_RETURNS_TO);
    }

    public boolean processASTDeclarator(CPPASTDeclarator node, boolean add) {
        boolean name = visitNode(node.getName());
        if (name) {
            addChildNode(node, node.getName());
            IASTNode before = null;
            for (IASTPointerOperator po : node.getPointerOperators()) {
                addChildNode(node, po);
                if (before != null) {
                    addNextNode(before, po);
                }
                before = po;
            }
            if (before != null) {
                addNextNode(before, node.getName());
            }
        }
        add = name || mAll;
        if (node.getInitializer() != null) {
            if (name) {
                boolean tmpAll = mAll;
                mAll = true;
                visitNode(node.getInitializer());
                addChildNode(node, node.getInitializer());
                StringCapsule initialize = StringCapsule.newInstance("initialize");
                addChildToken(node, initialize);
                addNextNode(node.getName(), initialize);
                addNextNode(initialize, node.getInitializer());
                mAll = tmpAll;
            } else {
                boolean init = visitNode(node.getInitializer());
                if (init) {
                    addChildNode(node, node.getInitializer());
                    add = true;
                }
            }
        }
        return add;
    }

    public boolean isAddChildAndNext(boolean add, IASTNode node) {
        IASTNode before = null;
        for (IASTNode child : node.getChildren()) {
            if (visitNode(child)) {
                add = true;
                addChildNode(node, child);
                if (before != null) {
                    addNextNode(before, child);
                }
                before = child;
            }
        }
        return add;
    }

    public boolean isAddChild(boolean add, IASTNode node) {
        for (IASTNode child : node.getChildren()) {
            if (visitNode(child)) {
                add = true;
                addChildNode(node, child);
            }
        }
        return add;
    }

    public String getOperatorString(int operator) {
        return "Operator " + operator;
    }

    public void addNextNode(Object before, Object after) {
        putEdge(before, after, EDGE_NEXT_TOKEN);
    }

    public void addChildToken(IASTNode parent, Object child) {
        putEdge(parent, child, EDGE_CHILD_TOKEN);
    }

    public void addChildNode(IASTNode parent, IASTNode child) {
        putEdge(parent, child, EDGE_CHILD_NODE);
    }

    public void putEdge(Object nodeU, Object nodeV, String edgeType) {
        if (nodeU == null || nodeV == null) {
            return;
        }
        if (!mAll && isContain(edgeType, mDFGEdgeTypes) &&
                !(mNetwork.nodes().contains(nodeU) && mNetwork.nodes().contains(nodeV))) {
            return;
        }
        mNetwork.addEdge(nodeU, nodeV, edgeType + "_" + mEdgeNumber);
//        if (edgeType.contains(EDGE_NEXT_EXEC)) {
//            mCFGNetwork.addEdge(nodeU, nodeV, edgeType + "_" + mEdgeNumber);
//        }
        mEdgeNumber = mEdgeNumber + 1;
    }

    public Set<CPPASTName> getDFGRelateNodes(CPPASTName name, Set<CPPASTName> relatedNodes) {
        relatedNodes.add(name);
        for (Object object : mNetwork.nodes()) {
            if (object instanceof CPPASTName && !relatedNodes.contains(object) &&
                    isDFGEdgeConnect(name, (CPPASTName) object)) {
                relatedNodes.addAll(getDFGRelateNodes((CPPASTName) object, relatedNodes));
            }
        }
        return relatedNodes;
    }

    public Set<CPPASTName> getSinkRelate(int line) {
        Set<CPPASTName> relatedNodes = new HashSet<>();
        for (Object object : mNetwork.nodes()) {
            if (object instanceof CPPASTName) {
                CPPASTName name = (CPPASTName) object;
                if (name.getFileLocation().getStartingLineNumber() == line) {
                    relatedNodes.addAll(getDFGRelateNodes(name, relatedNodes));
                }
            }
        }
        return relatedNodes;
    }

    public boolean isDFGEdgeConnect(CPPASTName pre, CPPASTName succ) {
        return (mNetwork.hasEdgeConnecting(pre, succ) &&
                hasConnectingBelongs(pre, succ, new String[]{EDGE_COMPUTED_FROM, EDGE_LAST_USE,
                        EDGE_LAST_WRITE, EDGE_LAST_LEXICAL_USE, EDGE_FORMAL_ARG_NAME})) ||
                (mNetwork.hasEdgeConnecting(pre, succ)
                        && hasConnectingBelongs(succ, pre, new String[]{EDGE_LAST_USE, //  TODO: EDGE_FORMAL_ARG_NAME 可以看到var所在的方法体被谁调用
                        EDGE_LAST_WRITE, EDGE_LAST_LEXICAL_USE}));
    }

    public boolean hasConnectingBelongs(Object nodeU, Object nodeV, String[] edgeTypes) {
        for (String edgeType : mNetwork.edgesConnecting(nodeU, nodeV)) {
            if (isContain(edgeType, edgeTypes)) {
                return true;
            }
        }
        return false;
    }

    public MutableNetwork<Object, String> getNetwork() {
        return mNetwork;
    }

    public void setNetwork(MutableNetwork<Object, String> network) {
        mNetwork = network;
    }

    public int getEdgeNumber() {
        return mEdgeNumber;
    }

    public void setEdgeNumber(int edgeNumber) {
        mEdgeNumber = edgeNumber;
    }

    public boolean isAll() {
        return mAll;
    }

    public void setAll(boolean all) {
        mAll = all;
    }

    public Set<CPPASTName> getRelatedNames() {
        return mRelatedNames;
    }

    public void setRelatedNames(Set<CPPASTName> relatedNames) {
        mRelatedNames = relatedNames;
    }
}