package KX;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TEST {
    public static void main(String[] args) throws FileNotFoundException {
        String a="a",b="b",c="c";
        FileInputStream new_ = new FileInputStream("C:\\generate_slice_byuser_artifactid\\src\\main\\java\\TEST.java");
        CompilationUnit AST_new = JavaParser.parse(new_);
        for(IfStmt ifStmt:AST_new.findAll(IfStmt.class))
        {
                List<NameExpr> bugvar = new ArrayList<>();
                if(ifStmt.getCondition().findAll(BinaryExpr.class).size()>0) {


                    for (BinaryExpr i : ifStmt.getCondition().findAll(BinaryExpr.class)
                            ) {

                        if (ifStmt.getCondition().findAll(BinaryExpr.class).size() > 0) {
                            for (BinaryExpr a_ : ifStmt.getCondition().findAll(BinaryExpr.class)
                                    ) {
//                            BinaryExpr.Operator operator=a.getOperator();
                                for (NullLiteralExpr null_ : a_.findAll(NullLiteralExpr.class)) {
                                    null_.getParentNode().get().findAll(NameExpr.class).stream().forEach(c_ -> {
                                        if (!bugvar.contains(c_))
                                            bugvar.add(c_);
                                    });

                                }
                            }


                        }




                    }
//                    System.out.println(ifStmt.toString());
                    if(bugvar.size()>0) {
                        bugvar.forEach(op -> System.out.println(op.getNameAsString()));
                       ifStmt.findParent(MethodDeclaration.class).get().accept(new MethodChangerVisitor(bugvar), null);
                    }
                }



        }
        if((a=b.replace("c","a"))!=null&&c==b||a.equals(b)||a.equals(b)){
            System.out.println();

        }
    }

    private static class MethodChangerVisitor extends VoidVisitorAdapter<Void> {
        List<NameExpr> bugvar=new ArrayList<>();
        List<Node> nodeList=new ArrayList<>();
        Node node_;
        public MethodChangerVisitor(List<NameExpr> bugvar){
            this.bugvar=bugvar;
        }

        public void getnode(Node node){
            if (node instanceof Expression||node instanceof VariableDeclarator){

                node_=node;
//                System.out.println(node.toString()+" "+node.getBegin().get().line);
                getnode(node.getParentNode().get());



            }



        }




        public void visit(MethodDeclaration n,Void arg) {

            n.stream().forEach(c->{
                for(NameExpr name:bugvar) {
                    if ((c instanceof NameExpr || c instanceof SimpleName)&& c.toString().equals(name.toString())) {

                        getnode(c.getParentNode().get());

                        if(!nodeList.contains(node_))
                             this.nodeList.add(node_);

                 }
                }
            });
            nodeList.forEach(c-> System.out.println(c.toString()+" "+c.getBegin().get().line));

        }
    }

}
