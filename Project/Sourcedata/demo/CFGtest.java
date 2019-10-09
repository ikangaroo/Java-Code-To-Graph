package Neo4j;

public class CFGtest {
    private String name;
    private int age;
    private String sex;
    public CFGtest(){

    }

    public int  method1(int age){
        int i=3;
        int a;
        a=3;
        if(age<0){
            System.out.println("this is not a born baby");
            if(sex.equals("male")){
                System.out.println("this is not a male born baby");
            }
            else {
                System.out.println("this is not a female born baby");
            }
        }
        else if (age<5){
            System.out.println("this is a baby");
        }
        else {
            System.out.println("this is not a baby");
        }

        System.out.println("the IfStmt is end");

    }
}
