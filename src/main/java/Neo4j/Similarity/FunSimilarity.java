package Neo4j.Similarity;
import com.zhixiangli.code.similarity.CodeSimilarity;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
public class FunSimilarity {
    HashMap<File,File> old2New =new HashMap<>();
    HashMap<File,File>new2Old=new HashMap<>();

    public FunSimilarity(String cat1,String cat2){
      File[] oldFileList=new File(cat1).listFiles();
      File[] newFileList=new File(cat2).listFiles();
      if(oldFileList.length<newFileList.length){
          //新增加了文件数目
          Arrays.stream(newFileList).forEach(file -> new2Old.put(file,null));
          for(File n:newFileList){
              for (File o:oldFileList){
                  if (n.getName().equals(o.getName())){
                      new2Old.replace(n,o);
                  }
              }
          }
      }
      else {
          //删除了文件数目
          Arrays.stream(oldFileList).forEach(file -> old2New.put(file,null));
           for(File o:oldFileList){
              for (File n:newFileList){
                  if (o.getName().equals(n.getName())){
                      old2New.replace(o,n);
                  }
              }
          }
      }
    }
    public HashMap<List<File>,List<Double>> getSim(HashMap<File,File> fileHashMap, boolean flag){
        HashMap<List<File>,List<Double>> sim=new HashMap<>();
        if (flag){//新增加文件
            for(File file:fileHashMap.keySet()){
                //按行读《新，旧》json文件
                List<File> filename=new ArrayList<>();
                filename.add(file);
                filename.add(fileHashMap.get(file));
                sim.put(filename,LoadJson(file,fileHashMap.get(file)));


            }
        }
        else {//删除文件

        }
        return sim;
        }

    public List<Double> LoadJson(File newfile,File oldfile){
        List<Double> sim=new ArrayList<>();
        double msim;
        String str1,str2;
        List<String>oldJson=new ArrayList<>();
        List<String>newJson=new ArrayList<>();
        int line=0;
        if(newfile!=null&&oldfile!=null){//存在新旧文件的对应关系
            try {
                 InputStreamReader inputStreamReader1=new InputStreamReader(new FileInputStream(newfile));
                 InputStreamReader inputStreamReader2=new InputStreamReader(new FileInputStream(oldfile));
                 BufferedReader reader1=new BufferedReader(inputStreamReader1);
                 BufferedReader reader2=new BufferedReader(inputStreamReader2);
                 while ((str1=reader1.readLine())!=null){
                     if (line==0){
                         line++;
                         continue;}
                     newJson.add(str1);
                     line++;
                 }
                 inputStreamReader1.close();
                 reader1.close();
                 line=0;//行号归零
                 while ((str2=reader2.readLine())!=null){
                     if (line==0){
                         line++;
                         continue;
                     }
                     oldJson.add(str2);
                     line++;
                 }
                 inputStreamReader2.close();
                 reader2.close();
            }
            catch (IOException e){
                System.out.println("读取的文件不存在");
            }

        }
        return (FilemethodSim(newJson,oldJson));


    }
    public List<Double>FilemethodSim(List<String> newJson,List<String> oldJson){
        List<Double> sim=new ArrayList<>();
        if(newJson.size()==oldJson.size()){
             //只处理方法数目不变的情况
            for (int i = 0; i < newJson.size(); i++) {
                CodeSimilarity codeSimilarity=new CodeSimilarity();
                sim.add(codeSimilarity.get(newJson.get(i),oldJson.get(i)));
            }

        }
        else {
            //同一个文件下方法数目不一样
            System.out.println("功能未开发");
        }

        return sim;
    }

    public static void main(String[] args) {
        String cat1="H:\\CodeGraph\\Project\\Savedata\\sV1.0";
        String cat2="H:\\CodeGraph\\Project\\Savedata\\sV1.1";
        FunSimilarity funSimilarity=new FunSimilarity(cat1,cat2);
        HashMap<List<File>,List<Double>> sim=funSimilarity.getSim(funSimilarity.getOld2New(),true);
    }

    public HashMap<File, File> getNew2Old() {
        return new2Old;
    }

    public HashMap<File, File> getOld2New() {
        return old2New;
    }
}
