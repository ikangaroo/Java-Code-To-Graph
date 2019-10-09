package CProcess;

import com.github.javaparser.Position;
import com.github.javaparser.ast.Node;
import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * @ Author     ：wxkong
 */
public class IASTNodePositionComparator {
    // 两者之间，意味着，<-----a--->  <--------b--------> <-----------c--------->，这才是b在c两者之间

    public static boolean isBeforePosition(IASTNode a, IASTNode b) {
        if (isLocationNull(a) || isLocationNull(b)) {
            return false;
        }
        return a.getFileLocation().getNodeOffset() + a.getFileLocation().getNodeLength() < b.getFileLocation().getNodeOffset();
//        return compare(a, b) > 0;
    }

    public static boolean isAfterPosition(IASTNode a, IASTNode b) {
        if (isLocationNull(a) || isLocationNull(b)) {
            return false;
        }
        return b.getFileLocation().getNodeOffset() + b.getFileLocation().getNodeLength() < a.getFileLocation().getNodeOffset();
//        return compare(a, b) < 0;
    }

    public static int compare(IASTNode a, IASTNode b) {
        if (isLocationNull(a) || isLocationNull(b)) {
            return 0;
        }
        return new IASTNodeComparator().compare(a, b);
    }

    public static boolean ifSmaller(IASTNode positionSmall, IASTNode positionBig) {
        return compare(positionSmall, positionBig) >= 0;
    }

    public static boolean isLocationNull(IASTNode node) {
        return node == null || node.getFileLocation() == null;
    }
}
