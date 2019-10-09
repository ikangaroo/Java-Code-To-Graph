import java.util.ArrayList;
import java.util.List;

public class TestNodeBehavior {
    public static void main(String[] args) {
        int i = 1;
        System.out.println(i);
        int z = i + 1;
        i = i - 1;
        z = z + i;
        System.out.println(z);
        boolean boo = false;
        if (boo = true) {
            System.out.println("boo");
        }
        if (i > 0) {
            i = i + 2;
        } else {
            i = i - 2;
        }

        int z2 = i + 3;
        i = i - 3;

        for (int i2 = i - 5, iAddition = i + 8; i2 < i; i2++) {
            i = i + i2;
        }

        int z3 = i + 3;
        i = i - 3;

        while (i > 0) {
            System.out.println(i);
            i = i + 4;
        }

        int z4 = i + 4;

        List<Integer> list = new ArrayList<>();
        list.add(i);
        for (Integer integer : list) {
            i = integer;
            integer = i;
            System.out.println(integer);
        }
    }
}
