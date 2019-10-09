package FindBugsProcess;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ParsePosition {
    public static void main(String[] args) throws FileNotFoundException {
        String path = "data/TestNodeBehavior.java";
        CompilationUnit compilationUnit= JavaParser.parse(new FileInputStream(path));
        String pathAnother = "data/TestNodeBehaviorAnother.java";
        CompilationUnit compilationUnitAnother= JavaParser.parse(new FileInputStream(pathAnother));
        System.out.println(compilationUnitAnother.toString());

    }
}
