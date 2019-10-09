package KX;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CountLineWord {
    public static double Variance(ArrayList<Integer> x) {
        int m=x.size();
        double sum=0;
        for(int i=0;i<m;i++){//求和
            sum+=x.get(i);
        }
        double dAve=sum/m;//求平均值
        System.out.println("平均值"+dAve);
        double dVar=0;
        for(int i=0;i<m;i++){//求方差
            dVar+=(x.get(i)-dAve)*(x.get(i)-dAve);
        }
        return dVar/m;
    }

    //标准差σ=sqrt(s^2)
    public static double StandardDiviation(ArrayList<Integer> x) {
        int m = x.size();
        double sum = 0;
        for (int i = 0; i < m; i++) {//求和
            sum += x.get(i);
        }
        double dAve = sum / m;//求平均值
        System.out.println("平均值"+dAve);
        double dVar = 0;
        for (int i = 0; i < m; i++) {//求方差
            dVar += (x.get(i) - dAve) * (x.get(i) - dAve);
        }
        return Math.sqrt(dVar / m);
    }



    public static void main(String[] args) throws IOException {
        String path="C:\\generate_slice_byuser_artifactid\\slices\\mature";
        File file=new File(path);
        File[] files=file.listFiles();

        ArrayList<Integer> nums=new ArrayList<>();
        for(File file1:files){
            if(file1.getName().contains("new")){
                FileReader fileReader=new FileReader(file1);
                BufferedReader bufferedReader=new BufferedReader(fileReader);
                String s="";
                int num=0;
                while ((s=bufferedReader.readLine())!=null){
                    num=num+1;
                }

                File file2=new File(file1.getAbsolutePath().replace("new","old"));
                FileReader fileReader2=new FileReader(file2);
                BufferedReader bufferedReader2=new BufferedReader(fileReader2);
                int num2=0;
                while ((s=bufferedReader2.readLine())!=null){
                    num2=num2+1;
                }

                if(num!=num2) {
                    System.out.println(file1.getAbsolutePath());
                    System.out.println("num " + num);
                    System.out.println("num2 " + num2);
                }


            }
        }

//
//        double dV=Variance(nums);
//        System.out.println("方差="+dV);
//        //计算标准差
//        double dS=StandardDiviation(nums);
//        System.out.println("标准差="+dS);




    }



}
