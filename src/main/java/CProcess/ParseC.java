package CProcess;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.internal.core.dom.parser.cpp.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @ Author     ：wxkong
 */
public class ParseC {
    public static void main(String[] args) {
        String fcsarda = "cf/112708/CWE761_Free_Pointer_Not_at_Start_of_Buffer__char_file_82a.cpp";
        String fcsard = "cf/117923/CWE789_Uncontrolled_Mem_Alloc__new_wchar_t_rand_45.cpp";
        String fcpp = "cFile/CWE401.cpp";
        String fc = "cFile/CWE761.c";
        String fc1 = "cFile/10-476_test.c";
        String fc2 = "cFile/63-119-120_test.c";
        String fc3 = "cFile/0_test.c";
        List<String> files = new ArrayList<>();
        files.add(fcsarda);
        files.add(fcsard);
        files.add(fc3);
        files.add(fcpp);
        files.add(fc);
        files.add(fc2);
        files.add(fc1);

        for (String file : files) {
            BuildGraphC bc = BuildGraphC.newFromFile(file);
            assert bc != null;
            bc.initNetwork();
            System.out.println(file);
            IASTTranslationUnit tu = bc.getTranslationUnit();
            for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
                if (dec.getBody() instanceof IASTCompoundStatement) {
                    IASTCompoundStatement body = (IASTCompoundStatement) dec.getBody();
                    if (body.getStatements().length == 0) {
                        System.out.println("O body");
                        continue;
                    }
                }
                bc.buildGraph(dec);
//                bc.visitNode(dec);
//                bc.buildDFG(dec);
//                bc.buildCFG(dec);
                System.out.println("END=============");
            }
        }
    }

    public void testRelatedNode() {
        String fcsarda = "cf/112708/CWE761_Free_Pointer_Not_at_Start_of_Buffer__char_file_82a.cpp";
        String fcsard = "cf/117923/CWE789_Uncontrolled_Mem_Alloc__new_wchar_t_rand_45.cpp";
        String fcpp = "cFile/CWE401.cpp";
        String fc = "cFile/CWE761.c";
        String fc1 = "cFile/10-476_test.c";
        String fc2 = "cFile/63-119-120_test.c";
        String fc3 = "cFile/0_test.c";
        String fc4 = "cFile/firefox.cpp";
        List<String> files = new ArrayList<>();
        files.add(fc4);
        files.add(fcsarda);
        files.add(fcsard);
        files.add(fc3);
        files.add(fcpp);
        files.add(fc);
        files.add(fc2);
        files.add(fc1);

        for (String file : files) {
            if (!file.equals(fc4)) {
                continue;
            }
            BuildGraphC bc = BuildGraphC.newFromFile(file);
            assert bc != null;
            bc.initNetwork();

            System.out.println(file);
            IASTTranslationUnit tu = bc.getTranslationUnit();
            for (IASTFunctionDefinition dec : bc.getFunctionDefinitions()) {
                if (dec.getBody() instanceof IASTCompoundStatement) {
                    IASTCompoundStatement body = (IASTCompoundStatement) dec.getBody();
                    if (body.getStatements().length == 0 ||
                            !dec.getDeclarator().getName().toString().equals("EditorPrefsChangedCallback")) {
                        System.out.println("O body or not required method");
                        continue;
                    }
                }
                bc.initNetwork();
                bc.setAll(true);
                bc.buildDFG(dec);
                Set<CPPASTName> related = bc.getSinkRelate(155);
                related.forEach(name ->
                        System.out.println(name.getFileLocation().getStartingLineNumber() + ": " + name.toString()));
                bc.initNetwork();
                bc.setRelatedNames(related);
                bc.setAll(false);
                bc.buildGraph(dec);
                System.out.println("END=============");
            }
        }

    }

}
