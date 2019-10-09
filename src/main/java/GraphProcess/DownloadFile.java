package GraphProcess;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;

public class DownloadFile{
//    https://codeload.github.com/bugs-dot-jar/accumulo/zip/bugs-dot-jar_ACCUMULO-1044_9396979b
//    https://codeload.github.com/bugs-dot-jar/accumulo/zip/bugs-dot-jar_ACCUMULO-412_be2fdba7
    private static final String ACCUMULO =
            "https://codeload.github.com/bugs-dot-jar/accumulo/zip/";
    private static final String CAMEL =
            "https://codeload.github.com/bugs-dot-jar/camel/zip/";
    private static final String MATH =
            "https://codeload.github.com/bugs-dot-jar/commons-math/zip/";
    private static final String FLINK =
            "https://codeload.github.com/bugs-dot-jar/flink/zip/";
    private static final String OAK =
            "https://codeload.github.com/bugs-dot-jar/jackrabbit-oak/zip/";
    private static final String LOG4J2 =
            "https://codeload.github.com/bugs-dot-jar/logging-log4j2/zip/";
    private static final String MAVEN =
            "https://codeload.github.com/bugs-dot-jar/maven/zip/";
    private static final String WICKET =
            "https://codeload.github.com/bugs-dot-jar/wicket/zip/";


    public static void main(String[] args) throws IOException {

        File archive = new File("archive/");
        File[] archives = archive.listFiles();

        ArrayList<String> archiveList = new ArrayList<>();
        if (archives != null) {
            for (File arch : archives) {
                if (arch.isFile()) {
                    archiveList.add(arch.getPath());
                }
            }
        }


        String urlPrefix = "";
        String saveDir = "";
        for (String arch : archiveList) {
            if (arch.contains("accumulo")) {
                urlPrefix = ACCUMULO;
                saveDir = "bug/accumulo/";
            } else if (arch.contains("camel")) {
                urlPrefix = CAMEL;
                saveDir = "bug/camel/";
            } else if (arch.contains("math")) {
                urlPrefix = MATH;
                saveDir = "bug/math/";
            } else if (arch.contains("flink")) {
                urlPrefix = FLINK;
                saveDir = "bug/flink/";
            } else if (arch.contains("oak")) {
                urlPrefix = OAK;
                saveDir = "bug/oak/";
            } else if (arch.contains("log4j2")) {
                urlPrefix = LOG4J2;
                saveDir = "bug/log4j2/";
            } else if (arch.contains("maven")) {
                urlPrefix = MAVEN;
                saveDir = "bug/maven/";
            } else if (arch.contains("wicket")) {
                urlPrefix = WICKET;
                saveDir = "bug/wicket/";
            }
            String versions = Util.readFileToString(arch).trim();
            String[] lines = versions.split("\n");
            for (String line : lines) {
                line = line.trim().split("/")[2];
                String url = urlPrefix + line;
                String path = saveDir + line + ".zip";
                System.out.println(line);
                downloadFile(url, path);
            }
        }
    }


    //链接url下载文件
    private static void downloadFile(String urlString, String savePath) {
        try {
            URL url = new URL(urlString);
            InputStream inputStream = url.openStream();
            DataInputStream dataInputStream = new DataInputStream(inputStream);

            FileOutputStream fileOutputStream = new FileOutputStream(new File(savePath));
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = dataInputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            fileOutputStream.write(output.toByteArray());
            inputStream.close();
            dataInputStream.close();
            fileOutputStream.close();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
