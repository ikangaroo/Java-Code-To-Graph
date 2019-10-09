package owaspbench;

import GraphProcess.Util;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @ Author     ：wxkong
 */
public class WebVul {
    public static String typeNeedToModify = "hash, crypto, weakrand, securecookie";
    public static void main(String[] args) throws IOException {
//        removeStmts();
//        genSaveGraph("sqli", "data/BenchmarkTest21027.java", "data/BenchmarkTest00026.java.txt");
        vulDeePeckerMethod();
    }

    public static void vulDeePeckerMethod() throws IOException {
        String csvFilePath = "data/expectedresults-1.1.csv";
        ReadCSV readCSV = new ReadCSV(csvFilePath);
        ArrayList<Map<String, ArrayList<String>>> record = readCSV.readCSV();
        Map<String, ArrayList<String>> real = record.get(0);
        Map<String, ArrayList<String>> fp = record.get(1);
        String des = "../benchmark/";

        for (String cat : real.keySet()) {
            String fileDir = des + cat + "/" + "true/";
            File dir = new File(fileDir.replace("true", "truew"));
            File dirV = new File(fileDir.replace("true", "truev"));
            if (!dir.exists()) {
                dir.mkdir();
            }
            if (!dirV.exists()) {
                dirV.mkdir();
            }

            for (String file : real.get(cat)) {
                String filePath = creatJavaFilePath(fileDir, file);
                String graphPath = filePath.replace("true", "truew");
                genSaveGraph(cat, filePath, graphPath + ".txt");
            }
        }
        for (String cat : fp.keySet()) {
            String fileDir = des + cat + "/" + "false/";
            File dir = new File(fileDir.replace("false", "falsew"));
            File dirV = new File(fileDir.replace("false", "falsev"));
            if (!dir.exists()) {
                dir.mkdir();
            }
            if (!dirV.exists()) {
                dirV.mkdir();
            }

            for (String file : fp.get(cat)) {
                String filePath = creatJavaFilePath(fileDir, file);
                String graphPath = filePath.replace("false", "falsew");
                genSaveGraph(cat, filePath, graphPath + ".txt");
            }
        }
    }

    public static void removeStmts() throws IOException {
        String csvFilePath = "data/expectedresults-1.1.csv";
        ReadCSV readCSV = new ReadCSV(csvFilePath);
        ArrayList<Map<String, ArrayList<String>>> record = readCSV.readCSV();
        Map<String, ArrayList<String>> real = record.get(0);
        Map<String, ArrayList<String>> fp = record.get(1);
        String des = "../benchmark/";

        for (String cat : real.keySet()) {
            String fileDir = des + cat + "/" + "true/";
            File dir = new File(fileDir.replace(cat, cat + "new"));
            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (String file : real.get(cat)) {
                String filePath = creatJavaFilePath(fileDir, file);
                String graphPath = filePath.replace(cat, cat + "new");
                modifyFile(cat, filePath, graphPath);
            }
        }
        for (String cat : fp.keySet()) {
            String fileDir = des + cat + "/" + "false/";
            File dir = new File(fileDir.replace(cat, cat + "new"));
            if (!dir.exists()) {
                dir.mkdirs();
            }

            for (String file : fp.get(cat)) {
                String filePath = creatJavaFilePath(fileDir, file);
                String graphPath = filePath.replace(cat, cat + "new");
                modifyFile(cat, filePath, graphPath);
            }
        }
    }

    public static void modifyFile(String cat, String filePath, String graphPath) {
        if (cat.equals("crypto")) {
            modifyCrytpoFile(filePath, graphPath);
        }
    }

    public static void modifyCrytpoFile(String filePath, String graphPath) {
        CompilationUnit cuOrigin;
        try {
            cuOrigin = JavaParser.parse(new FileInputStream(filePath));
        } catch (Exception e) {
            System.out.println(filePath + " error\n" + e);
            return;
        }
        List<MethodDeclaration> methodPosts = cuOrigin.findAll(MethodDeclaration.class).stream()
                .filter(methodDeclaration -> methodDeclaration.getNameAsString().equals("doPost"))
                .collect(Collectors.toList());
        if (!methodPosts.isEmpty()) {
            MethodDeclaration methodPost = methodPosts.get(0);
            if (methodPost.getBody().isPresent()) {
                List<Statement> bodyRemove = new ArrayList<>();
                BlockStmt body = methodPost.getBody().get();
                for (Statement st :body.getStatements()) {
                    if (!(st.isTryStmt() ||
                            ((st.toString().contains("java.security") || st.toString().contains("javax.crypto"))
                                    && !st.toString().contains("response")))) {
                        bodyRemove.add(st);
                    }
                }
                bodyRemove.forEach(body::remove);
                List<Node> parameterRemove = new ArrayList<>(methodPost.getParameters());
                parameterRemove.forEach(methodPost::remove);
                try {
                    cuOrigin = JavaParser.parse(cuOrigin.toString());
                } catch (Exception e) {
                    System.out.println(filePath + " error\n" + e);
                    return;
                }
                Util.saveToFile(cuOrigin.toString(), graphPath);
            }
        }
    }

    public static void genSaveGraph(String cat, String filePath, String graphPath) {
//        if (!cat.equals("sqli")) {
//            return;
//
//        ExtractSubGraphs.ExtractOwaspBackup(filePath, graphPath);
       // ExtractSubGraphs.getVulDeeResult(filePath, graphPath);
    }

    public static String creatJavaFilePath(String dir, String filename) {
        return dir + filename + ".java";
    }
}
