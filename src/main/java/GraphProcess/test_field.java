package GraphProcess;

import com.github.javaparser.ast.Node;

import java.io.FileNotFoundException;
import java.util.List;

public class test_field {
    int a = 3;
    int b = 4;
    private test_field(){
        a = 5;
        System.out.println(a);
    }
    private void change_a(){
        a = 6;
        System.out.println(a);
    }

    public static void main(String[] args) throws FileNotFoundException {
        String datapath="data/pac4j";
        Parse_new_3 p = new Parse_new_3(datapath);
        String methodCall_name = "getEnclosingElement";
        int methodCall_line =46;
        List<Node> final_nodes_want = p.getSliceOfMethodCallOfLine(methodCall_line, methodCall_name);
        if(final_nodes_want==null){
            System.out.println("nu;ll");
        }
        if (final_nodes_want != null && final_nodes_want.size()!=0 ) {
            final_nodes_want.forEach(fi -> {
                System.out.println(String.valueOf(fi.getBegin().get().line) + " - " + String.valueOf(fi.getEnd().get().line) + ":  " + fi.toString());
            });
        }
    }
}
