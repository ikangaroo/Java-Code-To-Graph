package CProcess;
//c处理当中各种工具函数

import CCPP.metadata.TClass;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.parser.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;
import org.eclipse.cdt.internal.core.index.EmptyCIndex;
import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;


public class CParseUtil {
    public int options = 0;
    public String mSrcFilePath;
    public IASTTranslationUnit mTranslationUnit;//翻译单元
    public List<IASTFunctionDefinition> mFunctionDefinitions = new ArrayList<>();
    public List<IASTName> mVariableNames = new ArrayList<>();
    public List<String> mVariableNameStrings = new ArrayList<>();
    public List<IASTName> mTypeNames = new ArrayList<>();
    public List<String> mTypeNameStrings = new ArrayList<>();

    CParseUtil(String srcFilePath) {
        mSrcFilePath = srcFilePath;
        FileContent fileContent = FileContent.createForExternalFileLocation(srcFilePath);
        mTranslationUnit = parse(fileContent);

    }

    public void parse(File file ){
    	FileContent fileContent = FileContent.createForExternalFileLocation(file.getPath());
    	parse(fileContent);
    }

    public IASTTranslationUnit parse(FileContent fileContent ){//解析得到语法树翻译单元
        enableOption(GPPLanguage.OPTION_NO_IMAGE_LOCATIONS);
        String[] includePaths = new String[0];
        Map<String, String> definedMacros = new HashMap<>();
        IScannerInfo info = new ScannerInfo(definedMacros, includePaths);
        IParserLogService log = new DefaultLogService();
        IIndex index = EmptyCIndex.INSTANCE; // or can be null
        IncludeFileContentProvider emptyIncludes = IncludeFileContentProvider.getEmptyFilesProvider();
        try {
            return GPPLanguage.getDefault().getASTTranslationUnit(fileContent, info, emptyIncludes, index, options, log);
        } catch (Exception e) {
            return null;
        }
    }

    public <T extends IASTNode> void collectFunctionDecls(T node) {//递归收集函数声明
        for (IASTNode child : node.getChildren()) {
            if (child instanceof IASTFunctionDefinition) {
                mFunctionDefinitions.add((IASTFunctionDefinition) child);
            } else {
                collectFunctionDecls(child);//
            }
        }
    }

    public CPPASTFunctionDefinition findFuncDefinition(int line) {//根据行号查找函数申明
        for (IASTFunctionDefinition func : mFunctionDefinitions) {
            if (func.getFileLocation().getStartingLineNumber() <= line &&
                    func.getFileLocation().getEndingLineNumber() >= line) {
                return (CPPASTFunctionDefinition) func;
            }
        }
        return null;
    }

    public <T extends IASTNode> void collectVariableNames(T node) {//收集函数变量
        for (IASTNode child : node.getChildren()) {
            if (child instanceof CPPASTFunctionDeclarator) {
                CPPASTFunctionDeclarator fd = (CPPASTFunctionDeclarator) child;
                for (ICPPASTParameterDeclaration pd : fd.getParameters()) {
                    collectVariableNames(pd);
                }
                continue;
            }
            if (child instanceof CPPASTTypeId) {
                CPPASTTypeId d = (CPPASTTypeId) child;
                if (d.getAbstractDeclarator() != null
                        && d.getAbstractDeclarator().getName() != null) {
                    mTypeNames.add(d.getAbstractDeclarator().getName());
                    mTypeNameStrings.add(d.getAbstractDeclarator().getName().toString());
                }
                continue;
            }
            if (child instanceof CPPASTDeclarator) {
                CPPASTDeclarator d = (CPPASTDeclarator) child;
                if (d.getName() != null) {
                    mVariableNames.add(d.getName());
                    mVariableNameStrings.add(d.getName().toString());
                }
                continue;
            }
            {
                collectVariableNames(child);
            }
        }
    }

    public void enableOption(int option){
        options |= option;
    }

    public int getOptions() {
        return options;
    }

    public String getSrcFilePath() {
        return mSrcFilePath;
    }

    public IASTTranslationUnit getTranslationUnit() {
        return mTranslationUnit;
    }

    public List<IASTFunctionDefinition> getFunctionDefinitions() {
        return mFunctionDefinitions;
    }

    public List<IASTName> getSpecificVariableFlowsBetweenNodes(IASTNode parent, IASTName variableName, IASTNode before, IASTNode after) {
        return findAll(parent, IASTName.class)
                .stream()
                .filter(name -> name.toString().equals(variableName.toString())
                        && !name.equals(variableName)
                        && IASTNodePositionComparator.isBeforePosition(name, after)
                        && IASTNodePositionComparator.isAfterPosition(name, before))
                .sorted(new IASTNodeComparator())
                .collect(Collectors.toList());
    }

    public List<IASTName> getSpecificVariableFlowsLastNodes(IASTNode parent, IASTName variableName, IASTNode before) {
        return findAll(parent, IASTName.class)
                .stream()
                .filter(name -> name.toString().equals(variableName.toString()) &&
                        !name.equals(variableName) &&
                        IASTNodePositionComparator.isAfterPosition(name, before))
                .sorted(new IASTNodeComparator())
                .collect(Collectors.toList());
    }

    public List<IASTName> getVariableNames(IASTNode parentNode) {
        return findAll(parentNode, IASTName.class)
                .stream()
                .filter(name -> mVariableNameStrings.contains(name.toString()))
                .collect(Collectors.toList());
    }

    public List<IASTName> getSpecificVariableFlows(IASTNode parentNode, IASTName variableName) {
        return findAll(parentNode, IASTName.class)
                .stream()
                .filter(name -> name.toString().equals(variableName.toString()) && !name.equals(variableName))
                .sorted(new IASTNodeComparator())
                .collect(Collectors.toList());
    }

    public List<IASTName> getSpecificVariableFlowsUntilFirstWrite(IASTNode parentNode, IASTName variableName) {
        // TODO: improve
        List<CPPASTBinaryExpression> assignExprs = getSpecificAssignExpr(parentNode, variableName);
        if (assignExprs.isEmpty()) {
            return getSpecificVariableFlows(parentNode, variableName);
        } else {
            CPPASTBinaryExpression firstAssign = assignExprs.get(0);
            List<IASTName> result = getSpecificVariableFlowsStartNodes(parentNode, variableName, firstAssign);
            result.addAll(getSpecificVariableFlows(firstAssign.getOperand2(), variableName));
            result.addAll(getSpecificVariableFlows(firstAssign.getOperand1(), variableName));
            return result;
        }
    }

    public List<IASTName> getSpecificVariableFlowsStartNodes(IASTNode parent, IASTName variableName, IASTNode after) {
        return findAll(parent, IASTName.class)
                .stream()
                .filter(name -> name.toString().equals(variableName.toString()) && !name.equals(variableName)
                        && IASTNodePositionComparator.isBeforePosition(name, after))
                .sorted(new IASTNodeComparator())
                .collect(Collectors.toList());
    }

    public List<IASTNode> getAssignsOrStmtsContains(IASTNode parent, IASTName variableName) {
        List<IASTNode> containNodes = new ArrayList<>();
        List<Class> nodeTypes = Arrays.asList(CPPASTIfStatement.class, CPPASTWhileStatement.class, CPPASTDoStatement.class,
                CPPASTForStatement.class, CPPASTSwitchStatement.class, CPPASTTryBlockStatement.class, CPPASTLambdaExpression.class);
        for (Class type : nodeTypes) {
            containNodes.addAll(getNodesContains(parent, variableName, type));
        }
        List<IASTNode> toRemove = new ArrayList<>();
        for (IASTNode node : containNodes) {
            for (IASTNode other : containNodes) {
                if (node.equals(other)) {
                    continue;
                }
                if (isParentContains(node, other, IASTNode.class)) {
                    toRemove.add(other);
                } else if (isParentContains(other, node, IASTNode.class)) {
                    toRemove.add(node);
                }
            }
        }
        containNodes.removeAll(toRemove);

        containNodes.addAll(getSpecificAssignExpr(parent, variableName).stream()
                .filter(assignExpr -> !isParentsContains(containNodes, assignExpr, CPPASTBinaryExpression.class))
                .collect(Collectors.toList()));
        containNodes.sort(new IASTNodeComparator());
        return containNodes;
    }

    public <T extends IASTNode> boolean isParentsContains(List<IASTNode> parents, T child, Class<T> tClass) {
        for (IASTNode parent : parents) {
            if (isParentContains(parent, child, tClass)) {
                return true;
            }
        }
        return false;
    }

    public <T extends IASTNode> boolean isParentContains(IASTNode parent, T child, Class<T> tClass) {
        return new ArrayList<>(findAll(parent, tClass)).contains(child);
    }

    public List<String> getIASTNameStringOfNode(IASTNode parent) {
        // 每个IASTName可能toString()相同，但不是同一个对象，天然的不同的IASTNode
        return findAll(parent, IASTName.class).stream()
                .map(IASTName::toString)
                .collect(Collectors.toList());
    }

    public List<CPPASTBinaryExpression> getSpecificAssignExpr(IASTNode parentNode, IASTName variableName) {
        return findAll(parentNode, CPPASTBinaryExpression.class)
                .stream()
                .filter(assignExpr -> assignExpr.getOperator() == 17 &&
                        getIASTNameStringOfNode(assignExpr.getOperand1()).contains(variableName.toString()))
                .sorted(new IASTNodeComparator()).collect(Collectors.toList());
    }

    public <T extends IASTNode> List<T> getNodesContains(IASTNode parent, IASTName simpleName, Class<T> tClass) {
        return findAll(parent, tClass)
                .stream()
                .filter(node -> getIASTNameStringOfNode(node).contains(simpleName.toString()))
                .collect(Collectors.toList());
    }

    public <T extends IASTNode> List<T> findAll(IASTNode node, Class<T> tClass) {
        List<T> nodes = new ArrayList<>();
        if (node == null) {
            return nodes;
        }
        for (IASTNode child : node.getChildren()) {
            if (tClass.isInstance(child)) {
                nodes.add((T) child);
            }
            nodes.addAll(findAll(child, tClass));
        }
        return nodes;
    }

    public boolean isAssignExpr(IASTExpression expression) {
        return  (expression instanceof CPPASTBinaryExpression && ((CPPASTBinaryExpression) expression).getOperator() == 17);
    }
}
