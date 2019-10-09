package CFG;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;

//JavaParser扩展工具包
public class ParserUtil {

    //两个属性：文件路径和编译单元（AST的根）
    private String filePath;
    private CompilationUnit compilationUnit;

    //构造方法：传入文件路径构造AST
    ParserUtil(String filePath){

        this.filePath = filePath;

    }

    public CompilationUnit construct() throws FileNotFoundException {

        this.compilationUnit = JavaParser.parse(new File(filePath));
        return compilationUnit;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public CompilationUnit getCompilationUnit() {
        return compilationUnit;
    }

    public void setCompilationUnit(CompilationUnit compilationUnit) {
        this.compilationUnit = compilationUnit;
    }
}