package KX;

import GraphProcess.GenerateGraph;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;

import java.io.*;
import java.util.ArrayList;

public class ImportFile {

    public GitFile paseAst(GitFile file) throws FileNotFoundException {
        try {
            FileInputStream new_ = new FileInputStream(file.path_newfile);
            CompilationUnit AST_new = JavaParser.parse(new_);

            ArrayList<Integer> addlist_=file.addlist;

            AST_new.stream().forEach(node->{
                if(node.getClass().equals(FieldDeclaration.class)||node.getClass().equals(VariableDeclarationExpr.class))
                    if (!file.new_declarition.contains(node))
                        file.new_declarition.add(node);
            });

            for(MethodDeclaration methodDeclaration:AST_new.findAll(MethodDeclaration.class)){
                int line_s=methodDeclaration.getBegin().get().line;
                int line_e=methodDeclaration.getEnd().get().line;
                for(int line:addlist_){
                    if (line>=line_s&&line<=line_e){
                        if (!file.new_funcs.contains(methodDeclaration))
                               file.new_funcs.add(methodDeclaration);
                    }
                }
            }

            FileInputStream old_= new FileInputStream(file.path_oldfile);
            CompilationUnit AST_old = JavaParser.parse(old_);
            AST_old.stream().forEach(c->{
                    if(c.getClass().equals(FieldDeclaration.class)||c.getClass().equals(VariableDeclarationExpr.class))
                        if (!file.old_declarition.contains(c))
                            file.old_declarition.add(c);
                });

            for(MethodDeclaration methodDeclaration:AST_old.findAll(MethodDeclaration.class)){
                for(MethodDeclaration newMethodDecl:file.new_funcs) {
                    if(newMethodDecl.getNameAsString().equals(methodDeclaration.getNameAsString())
                            &&newMethodDecl.getEnd().get().line>=methodDeclaration.getEnd().get().line)
                        if(!file.old_funcs.contains(methodDeclaration))
                               file.old_funcs.add(methodDeclaration);
                }
            }
    //        if (file.old_funcs.size()==0||file.new_funcs.size()==0)
    //            System.out.println(file.hashjava);
    //        if(file.old_funcs.size()!=file.new_funcs.size()){
    //                System.out.println(file.hashjava+"快看一下，nodes_old 和nodes_new 长度不一样");
    //        }
            }catch (ParseProblemException e){
                throw  e;
            }
        return file;
    }

    public GitFile readFile(String username ,String differline) throws IOException {
        String hashjava=differline.split(":")[0].trim();
        String line=differline.split(":")[1].trim();
        String addlists=line.split("ADD")[1].trim();
        String dellists=line.split("ADD")[0].trim();
        ArrayList<Integer> addlist=new ArrayList<Integer>();
        ArrayList<Integer> dellist=new ArrayList<Integer>();
        ArrayList<Node> AST_trim=new ArrayList<>();
        if(!dellists.isEmpty()) {
            for (String col : dellists.split(" ")) {
                dellist.add(Integer.parseInt(col));
            }
        }
        for (String col : addlists.split(" ")) {
            addlist.add(Integer.parseInt(col));
        }
        GitFile file=new GitFile(username,hashjava,addlist,dellist);
        try {
            file = paseAst(file);
        }catch (ParseProblemException e){
            System.out.println("parse error "+file.hashjava);
        }
        return file;
    }

    public GitFile readFileNPE(String username ,String differline) throws IOException {
        String hashjava=differline.split("\\$")[0].trim();
        String dellists=differline.split("\\$")[1].replace("Del", "").trim();
        String addlists=differline.split("\\$")[2].replace("ADD", "").trim();
        ArrayList<Integer> addlist=new ArrayList<Integer>();
        ArrayList<Integer> dellist=new ArrayList<Integer>();
        if(!dellists.isEmpty()) {
            for (String col : dellists.split(" ")) {
                dellist.add(Integer.parseInt(col));
            }
        }
        for (String col : addlists.split(" ")) {
            addlist.add(Integer.parseInt(col));
        }
        GitFile file=new GitFile(username,hashjava,addlist,dellist);
        try {
            file = paseAst(file);
        }catch (ParseProblemException e){
            System.out.println("parse error "+file.hashjava);
        }
        return file;
    }

    public void readDifferLine(String differdir) throws IOException {
        File differdiroot = new File(differdir);
        File[] filelist= differdiroot.listFiles();
        int notFound = 0;
        for (File file:filelist) {
            String username=file.getName().split("_")[1];
//            String username=file.getName().split("_")[2];
/*
            // add to print Aaron1011
            boolean end = false;
            if (username.equals("Aaron1011")) {
                end = true;
            } else {
                continue;
            }
            // add to print Aaron1011
*/
            if (!username.equals("2016")) {
                continue;
            }
            BufferedReader br=new BufferedReader(new FileReader(file));
            String line;
            while((line=br.readLine())!=null) {
//                if (!line.contains("0a46a268aa67c4baa096e4c87ce41c1a53232590")) {
//                if (!line.contains("0bf662c5b9497215f543fc24b176220538fd4470")) {
                if (!line.contains("00d2cf552f15f63febaa680abf4094cce9c38be2")) {
                    continue;
                }
                try {
                    GitFile gitFile = readFileNPE(username, line);
                    GenerateGraph generateGraph = new GenerateGraph();
                    try {
                        generateGraph.generateGraphs(gitFile);
                        return;
                    } catch (Exception e) {
                        System.out.println(username + " " + line + "\n" + e);
                    }
                }catch (FileNotFoundException e){
                    notFound++;
//                    System.out.println(e + ": File Not Found");
                }
            }
            br.close();
/*
            // add to print Aaron1011
            if (end) {
                break;
            }
            // add to print Aaron1011
*/
        }
        System.out.println(notFound + " not found.");
    }

    public void readDifferLine_test(String username,String line) throws IOException {
        GitFile gitFile=readFile(username,line);
        gitFile.findIfandAssign();
    }

    public static void main(String[] args) throws IOException {

        String slicestorepath="slices";
        DeleteFolder deleteFolder=new DeleteFolder();
        deleteFolder.delFolder(slicestorepath);
        ImportFile importFile=new ImportFile();
        String differdir="../npe_diff";
//        String differdir="../javabyuser_part12";
        importFile.readDifferLine(differdir);
    }
}
