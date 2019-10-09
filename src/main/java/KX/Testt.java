package KX;

public class Testt {


    public static String fenciline(String s) {
        String final_s = "";

        for (char item : s.toCharArray()) {

            Boolean flag = false;
            if (item != 32 && (item < 65 || item > 90 && item < 97 || item > 122))
                flag = true;

            if (flag)
                final_s = final_s + " " + item + " ";
            else
                final_s = final_s + item;


        }
        final_s= final_s.replace("  "," ");
        System.out.println(final_s);
        return final_s;
    }
    public static void main(String[] args) {
        String a="MethodCallExpr change Destination(input.MethodCallExpr abcd>fdf||sdfs->ds*f get Column By Index(e.MethodCallExpr get Key()), e.MethodCallExpr get Value())";

//        fencimethod(a);
    }




}
