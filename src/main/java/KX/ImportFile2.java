package KX;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ImportFile2 {

    static CompilationUnit AST_new;

    public static  List<Statement>  findrelateline_var(String var_needfind,MethodDeclaration methodDeclaration){
        //find   Byte data;
//        System.out.println(methodDeclaration.getNameAsString());
        List<SimpleName> simpleNames=new ArrayList<>();
        List<Statement> Stmts=new ArrayList<>();
        methodDeclaration.findAll(SimpleName.class).stream().forEach(c -> {
                if (c.toString().equals(var_needfind)) {
                    if(c.findParent(Statement.class).isPresent()) {
                        // if (!Stmts.contains(c.findParent(Statement.class).get()))
                        if(Stmts.stream().filter(m -> m.getRange().equals(c.findParent(Statement.class).get().getRange())).toArray().length==0){
                            Stmts.add(c.findParent(Statement.class).get());
                        }


                    }
                }

            });
            return Stmts;
    }
    public static List<Statement> findrelateline( List<Statement> statements_org,List<String> var_known,MethodDeclaration methodDeclaration){

        List<Statement> statements_org_=new ArrayList<>();
        statements_org.forEach(c->statements_org_.add(c));
        List<String> var_unknown=new ArrayList<>();

        for(Statement statement:statements_org) {
            if (statement.getClass().equals(IfStmt.class)) {
                List<String> finalVar_known1 = var_known;
                statement.asIfStmt().getCondition().findAll(NameExpr.class).forEach(c -> {
                    if (!c.getNameAsString().equals("IO")&&!finalVar_known1.contains(c.getNameAsString()) && !var_unknown.contains(c.getNameAsString())) {
                        var_unknown.add(c.getNameAsString());

                    }

                });
            } else {
                List<String> finalVar_known = var_known;
                statement.findAll(NameExpr.class).forEach(c -> {
                    if (!c.getNameAsString().equals("IO")&&!finalVar_known.contains(c.getNameAsString()) && !var_unknown.contains(c.getNameAsString())) {
                        var_unknown.add(c.getNameAsString());

                    }

                });

                statement.findAll(SimpleName.class).forEach(c -> {
                    if (c.getParentNode().get().getClass().equals(VariableDeclarator.class)  && !finalVar_known.contains(c.toString()) && !var_unknown.contains(c.toString())) {

                        var_unknown.add(c.toString());

                    }
                });
            }

        }

        for (String var:var_unknown ) {

            List<Statement> finalStatements_org = statements_org;
            findrelateline_var(var,methodDeclaration).forEach(c->{
               if (finalStatements_org.stream().filter(m -> m.getRange().equals(c.getRange())).toArray().length==0) {
                   finalStatements_org.add(c);
               }
            });

//            System.out.println(statements_org.size());
        }
//        statements_org.forEach(c-> System.out.println(c.getRange().get().begin.line+" "+c.toString()));

        Collections.sort(statements_org, new Comparator<Statement>() {

            @Override
            public int compare(Statement o1, Statement o2) {
                return o1.getRange().get().begin.line-o2.getRange().get().begin.line;
            }

        });
        var_unknown.forEach(c->
        {
            if (!var_known.contains(c))
                var_known.add(c);
        });

        if(var_unknown.size()!=0)
//        if(!statements_org.equals(statements_org_))
        { statements_org=findrelateline(statements_org,var_known,methodDeclaration);}






        return statements_org;

    }

    public static String fencimethod(String s){
        String final_s="";
        if(s.toUpperCase().equals(s)){
            return s;
        }
        for(char item:s.toCharArray() ){

            if(item>64 && item<91){
                final_s=final_s+" ";

            }
            final_s=final_s+item;
        }
//            final_s=final_s.trim();

        return final_s;
    }
    public static List<String> changename(List<Statement> statements){

        List<String>slices=new ArrayList<>();
        for(Statement c:statements) {

            String c_string = c.toString();
            HashMap<String,String> var2classvar=new HashMap<>();
            c.stream().forEach(m ->
                    {

                        if (m.getChildNodes().isEmpty() && !(m instanceof NameExpr)) {

                            String mclass = "";
                            String mstring = "";
//                            if (m.getParentNode().isPresent() && !(m.getParentNode().get() instanceof NameExpr)) {
                            if (m.getParentNode().isPresent()) {
                                String[] tmp = m.getParentNode().get().getClass().getName().split("\\.");
                                mclass = tmp[tmp.length - 1];
                                if(!(m instanceof SimpleName)){
                                    String[] tmp2=m.getClass().getName().split("\\.");
                                    mclass = tmp2[tmp2.length - 1];
                                }

                                if (m.getParentNode().get() instanceof MethodCallExpr || m instanceof SimpleName || m instanceof NameExpr)
                                    mstring = fencimethod(m.toString());

                                else
                                    mstring = m.toString();

                                var2classvar.put(m.toString(), mclass + " " + mstring);

                            }

                        }

                    }
            );

            if (c.getClass().equals(IfStmt.class)){

                c_string=c.asIfStmt().getCondition().toString();

            }
            for (String var: var2classvar.keySet() ) {
                        c_string=c_string.replace(var,var2classvar.get(var));

                    }
            slices.add(c_string.trim());
        }

        return  slices;

    }
    public static String fenciline(String s) {
        String final_s = "";

        for (char item : s.toCharArray()) {

            Boolean flag = false;
            if (item != 32 && (item < 65 || item > 90 && item < 97 || item > 122))
                flag = true;
            if((item>=48&&item<=57))
                flag=false;
            if (flag)
                final_s = final_s + " " + item + " ";
            else
                final_s = final_s + item;


        }
        final_s= final_s.replace("  "," ");
//            System.out.println(final_s);
        return final_s;
    }
    public static boolean comparestring(String A, String B){

        String a="";
        String b="";
        if(A.length()>B.length()){
            String tmp=B;
            B=A;
            A=tmp;
        }
        int i=0;
        for(i=0;i<A.length();i++){
            if(A.charAt(i)==B.charAt(i)){
                continue;
            }else {
                a=a+A.charAt(i);
                b=b+B.charAt(i);

            }
        }

        if(i<B.length()){
            b=b+B.substring(i,B.length());
        }
        List<String> set= new ArrayList<>(Arrays.asList("!","=","true","false","true()","false()"));

        if(set.contains(a.toLowerCase())&&set.contains(b.toLowerCase())){
            return true;

        }
        else {
            return false;
        }
    }

    public static String classgoodorbad(){

        List<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations=AST_new.findAll(ClassOrInterfaceDeclaration.class);
        for(ClassOrInterfaceDeclaration item:classOrInterfaceDeclarations){
//            System.out.println(item.getNameAsString().contains("goodG2B"));
            if(item.getNameAsString().contains("bad")){
                return "bad";
            }
            else if(item.getNameAsString().contains("goodG2B")){
                return  "bad";
            }
            else if(item.getNameAsString().contains("goodB2G")){
                return  "good";
            }
            else if(item.getNameAsString().contains("base")){
                return  "base";
            }
            else
                return "none";
        }
        return null;
    }

    public static void writeToFileView(String storepath,List<String> slices,String newOrold,String filename,String Methodname) throws IOException {

        if(slices!=null&&slices.size()>0) {
            String path = storepath;
            File file = new File(path);
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if (!file.exists())
                file.createNewFile();
            FileWriter fw = new FileWriter(file, true);
            fw.write("------------------" + filename + " " + newOrold + " " + Methodname + "\n");
            for (String line : slices) {

                line = fenciline(line);
                fw.write(line);
                fw.write("\n");
            }

            fw.close();
        }
    }
    public static  void writeToFile(String storepath,List<String> slices) throws IOException {

        if(slices!=null&&slices.size()>0) {
            String path = storepath;
            File file = new File(path);
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            if (!file.exists())
                file.createNewFile();
            FileWriter fw = new FileWriter(file, true);
//            fw.write("------------------"+hashjava+" "+newOrold+"\n");

            for (String line : slices) {


                if (line != null && line.trim() != null && line.trim() != " ") {
                    line = fenciline(line);
                    line = line.trim();
                    line = line.replace("\r", "");
                    line = line.replace("\n", "");
                    fw.write(line);
                    fw.write(" NEXTLINE ");
                }

            }
            fw.write("\n");


            fw.close();

        }
    }

    public static List<Statement> add(List<Statement> part,List<Statement> part_tmp){
        if(part_tmp!=null&&part_tmp.size()>0){

            part_tmp.forEach(c->
            {
                if(!part.contains(c)){
                    part.add(c);
                }

            });

        }
        return part;
    }
    public static void parse(File file,String file_relativepath ) throws IOException {
        AST_new = JavaParser.parse(file);
        String file_new = "slices_sard\\" + file_relativepath + "_new";
        String file_old = "slices_sard\\" + file_relativepath + "_old";
        String storepath;
        String samplelabel;
        for (MethodDeclaration methodDeclaration : AST_new.findAll(MethodDeclaration.class)) {
            try {
                methodDeclaration.getBody().get().getChildNodes().stream().filter(c -> c.getClass().equals(IfStmt.class));
                int num = (int) methodDeclaration.getBody().get().getChildNodes().stream()
                        .filter(c -> c.getClass().equals(IfStmt.class)).count();
                if (num > 2) {
                    System.out.println(file_relativepath);
                    System.out.println(methodDeclaration);
                    System.out.println("同一个方法体的if个数" + num);
                }
                if (num == 2) {
                    List<Node> ifstms = methodDeclaration.getBody().get().getChildNodes().stream()
                            .filter(c -> c.getClass().equals(IfStmt.class)).collect(Collectors.toList());
                    IfStmt if1 = (IfStmt) ifstms.get(0);
                    IfStmt if2 = (IfStmt) ifstms.get(1);
                    if (!if1.getCondition().equals(if2.getCondition())) {
                        if(comparestring(if1.getCondition().toString(), if2.getCondition().toString())){
                            changeifelse(if1.getElseStmt().get(),if2.getThenStmt());
                            if(if2.getElseStmt().isPresent()&&if1.getElseStmt().isPresent()){
                                changeifelse(if1.getThenStmt(),if2.getElseStmt().get());
                          }
                        }
                    }else {
                       if(if2.getElseStmt().isPresent()&&!if1.getElseStmt().isPresent()){
                           System.out.println(file_relativepath+" if2.getElseStmt().isPresent()&&!if1.getElseStmt().isPresent()");
                       }
                       changeifelse(if1.getThenStmt(),if2.getThenStmt());
                       if(if2.getElseStmt().isPresent()&&if1.getElseStmt().isPresent()){
                           changeifelse(if1.getElseStmt().get(),if2.getElseStmt().get());
                     }
                    }
                    methodDeclaration.getBody().get().remove(if2);
                }
            } catch (NoSuchElementException exception) {
            }

            List<Statement> statements= findrelateline_var("data",methodDeclaration);
            List<String>var_known=new ArrayList<>();
            var_known.add("data");
            List<Statement>statementList=findrelateline(statements,var_known,methodDeclaration);

            Object[] ifStmts_new= methodDeclaration.findAll(IfStmt.class).stream().filter(c-> c.getElseStmt().isPresent()).toArray();
            List<List<Statement>> seperatelist=new ArrayList<>();
            if(ifStmts_new.length==0){
                seperatelist.add(statementList);
            }

            if(ifStmts_new.length>0) {
                List<List<Statement>> seperatelist_tmp=new ArrayList<>();
                IfStmt ifStmt_tmp= (IfStmt) ifStmts_new[0];
                List<Integer> ifelseline = returnifelseline((IfStmt) ifStmts_new[0]);
//                seperatelist = cutstatementlist(statementList, ifelseline.get(0), ifelseline.get(1), ifelseline.get(2), ifelseline.get(3));
                seperatelist = seperIfandElse(statementList,ifStmt_tmp);

            }

            if(ifStmts_new.length>1){
                for(int i=1;i<ifStmts_new.length;i++){
                    List<List<Statement>> seperatelist_tmp=new ArrayList<>();
                    IfStmt ifStmt_tmp= (IfStmt) ifStmts_new[i];
                    List<Integer> ifelseline = returnifelseline((ifStmt_tmp));
                    List<Statement> iforelse=new ArrayList<>();
                    for (List<Statement> statements_tmp:seperatelist
                         ) {
                        if(statements_tmp.contains(ifStmt_tmp)){
                            iforelse=statements_tmp;
                        }
                    }
                     seperatelist_tmp= seperIfandElse(iforelse,ifStmt_tmp);
                     seperatelist.remove(iforelse);
                     seperatelist.addAll(seperatelist_tmp);
                }
            }

            if(seperatelist.size()>0) {
                for (List<Statement> statementList1 : seperatelist) {
                    List<String>slices=new ArrayList<>();
                    samplelabel="new";
                    storepath=file_new;
                    if(methodDeclaration.getName().toString().contains("bad")){
                        for(Statement statement:statementList1){
                            if(statement.getComment().isPresent()&&statement.getComment().toString().contains("POTENTIAL FLAW")){
                                samplelabel="old";
                                storepath=file_old;
                                break;
                            }
                        }
                    }
                    String classgoogdorbad=classgoodorbad();
                    if(classgoogdorbad!=null&&(classgoogdorbad.equals("base"))){
                        for(Statement statement:statementList1){
                            if(statement.getComment().isPresent()&&statement.getComment().toString().contains("POTENTIAL FLAW")){
                                samplelabel="old";
                                storepath=file_old;
                                break;
                            }
                        }
                    }

                    if(classgoogdorbad!=null&&(classgoogdorbad.equals("bad"))){
                       samplelabel="old";
                       storepath=file_old;
                    }

                    if(statementList1.size()>0)
                        statementList1.forEach(c->c.removeComment());
                    for (Statement statement : statementList1) {
                        if (statement.getClass().equals(IfStmt.class)) {
                            slices.add(statement.asIfStmt().getCondition().toString());
                        } else {
                            slices.add(statement.toString());
                        }
                    }

                    if(slices!=null&&slices.size()>0) {
                        writeToFileView(storepath, slices, samplelabel, file_relativepath, methodDeclaration.getNameAsString());
                        List<String> slices_end = changename(statementList1);
//                    String storepath1 = storepath + "_test";
//                    writeToFileView(storepath1, slices_end, samplelabel, file_relativepath,methodDeclaration.getNameAsString());
                        String storepath2 = storepath.replace("sard", "sard_train");
//                        writeToFile(storepath2, slices_end);
                        writeToFile(storepath2, slices);
                    }
                    }
                }
        }
    }

    public static List<Integer> returnifelseline(IfStmt ifStmt){
        List<Integer> ifelseline=new ArrayList<>();
        int ifstart,ifend,elsestart,elseend;
        if(ifStmt.getElseStmt().isPresent()){
            elsestart=ifStmt.getElseStmt().get().getRange().get().begin.line;
            elseend=ifStmt.getElseStmt().get().getRange().get().end.line;

        }else {
            elsestart=elseend=0;
        }
        ifstart=ifStmt.getCondition().getRange().get().begin.line;
        ifend=ifStmt.getThenStmt().getRange().get().end.line;
        ifelseline.add(ifstart);
        ifelseline.add(ifend);
        ifelseline.add(elsestart);
        ifelseline.add(elseend);

        return ifelseline;


    }
    private static List< List<Statement>>  seperIfandElse(List<Statement> statementList,IfStmt ifStmt_tmp) {
        List<Statement> ifpart=new ArrayList<>();
        List<Statement> elsepart=new ArrayList<>();
        Statement ifif=ifStmt_tmp.getThenStmt();
        Statement elseelse=ifStmt_tmp.getElseStmt().get();


        for (Statement statement:statementList
                ) {
            if (ifStmt_tmp.equals(statement)) {

                ifpart.add(statement);
            }
            else {
            if (ifif.stream().filter(c -> c.getRange().equals(statement.getRange())).toArray().length>0){
                ifpart.add(statement);
            } else if (elseelse.stream().filter(c -> c.getRange().equals(statement.getRange())).toArray().length>0) {
                elsepart.add(statement);
            } else {
                ifpart.add(statement);
                elsepart.add(statement);
            }
        }


        }
        List< List<Statement>> ifandelse=new ArrayList<>();
        ifandelse.add(ifpart);
        ifandelse.add(elsepart);
        return ifandelse;

    }
    public static  void changeifelse(Statement if1_ifelse,Statement if2_ifelse){
        List<Statement> newone=new ArrayList<>();
        if2_ifelse.getChildNodes().forEach(c-> {
            if(c instanceof Statement) {
                newone.add((Statement) c);
            }
        }
       );
        BlockStmt blockStmt= (BlockStmt) if1_ifelse;
        newone.forEach(c-> blockStmt.getStatements().add(c));

    }



    public static void main(String[] args) throws IOException {
        String[] slicestorepath={"slices_sard","slices_sard_train"};
        for (String s:slicestorepath) {
            DeleteFolder deleteFolder=new DeleteFolder();
            deleteFolder.delFolder(s);
        }

        File differdiroot= new File("C:\\Users\\Stark.DESKTOP-A6CMICB\\Desktop\\CWEfiles\\");
        File[] filelists= differdiroot.listFiles();
        System.out.println(filelists.length);
        for(int i=0;i<filelists.length;i++) {
            if (!filelists[i].isFile()) {
                if (i % 100== 0) {
                    System.out.println(i / 100 + 1);
                }
                File[] filelist = filelists[i].listFiles();
                for (int j = 0; j < filelist.length; j++) {
                    String relativepath = filelist[j].getName().replace(".java", " ").trim();
                    parse(filelist[j], relativepath);
                }
            }
        }
    }
}
