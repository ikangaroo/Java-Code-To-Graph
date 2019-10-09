package owaspbench;

import com.csvreader.CsvReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @ Author     ：wxkong
 */
public class ReadCSV {
    String mCsvFilePath;
    CsvReader mCsvReader;

    public ReadCSV(String csvFilePath) throws FileNotFoundException {
        mCsvFilePath = csvFilePath;
        mCsvReader = new CsvReader(mCsvFilePath);
    }

    public ArrayList<Map<String, ArrayList<String>>> readCSV() throws IOException {
        mCsvReader.readHeaders();
        Map<String, ArrayList<String>> real = new HashMap<>();
        Map<String, ArrayList<String>> fp = new HashMap<>();
        for (String s : mCsvReader.getHeaders()) {
            System.out.println(s);
        }
        while (mCsvReader.readRecord()){
            String category = mCsvReader.get("category");
            String filename = mCsvReader.get("# test name");
            if (mCsvReader.get("real vulnerability").equalsIgnoreCase("true")) {
                addMapValue(real, category, filename);
            } else {
                addMapValue(fp, category, filename);
            }
        }
        ArrayList<Map<String, ArrayList<String>>> record = new ArrayList<>();
        record.add(real);
        record.add(fp);
        return record;
    }
    public static void main(String[] args) throws IOException {
        String csvFilePath = "data/expectedresults-1.1.csv";
        ReadCSV readCSV = new ReadCSV(csvFilePath);
        ArrayList<Map<String, ArrayList<String>>> record = readCSV.readCSV();
        Map<String, ArrayList<String>> real = record.get(0);
        Map<String, ArrayList<String>> fp = record.get(1);

        String srcDir = "../Benchmark-1.1final/src/main/java/org/owasp/benchmark/testcode/";
        String des = "../benchmark/";
//        String des = "../benchmark-1.1/";
        for (String cat : real.keySet()) {
            String desDir = des + cat + "/" + "true/";
            copyFiles(real, srcDir, cat, desDir);
//            System.out.println(cat + ": " + real.get(cat).size());
        }
        for (String cat : fp.keySet()) {
            String desDir = des  + cat + "/"+ "false/";
            copyFiles(fp, srcDir, cat, desDir);
//            System.out.println(cat + ": " + fp.get(cat).size());
        }
    }

    private static void copyFiles(Map<String, ArrayList<String>> fp, String srcDir, String cat, String desDir) throws IOException {
        File dir = new File(desDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        for (String file : fp.get(cat)) {
            Files.copy(new File(srcDir + file + ".java").toPath(),
                    new File(desDir + file + ".java").toPath());
        }
    }

    private static <T, U> void addMapValue(Map<T, ArrayList<U>> map, T key, U valueOfList) {
        if (map.containsKey(key)) {
            map.get(key).add(valueOfList);
        } else {
            ArrayList<U> list = new ArrayList<>();
            list.add(valueOfList);
            map.put(key, list);
        }
    }

    public String getCsvFilePath() {
        return mCsvFilePath;
    }

    public CsvReader getCsvReader() {
        return mCsvReader;
    }
}
