package FindBugsProcess;

import GraphProcess.Util;

import java.util.ArrayList;

import static GraphProcess.Util.readFileToArrayList;

public class DownloadViolationFiles {
    private String mViolationListFile = "data/fixed-violation-instances.list";

    public static void main(String[] args) {
        DownloadViolationFiles download = new DownloadViolationFiles();
        ArrayList<String> violationList = readFileToArrayList(download.mViolationListFile);
        System.out.println(violationList.get(0));
        violationList.remove(0);
        String prefix = "https://raw.githubusercontent.com/";
        String prefixSave = "FindBugsJavaFile/";
        ArrayList<String> downloaded = new ArrayList<>();
        for (String info : violationList) {
            String[] unfix = info.split("=>")[0].split(":");
            String[] fix = info.split("=>")[1].split(":");
            String repo = unfix[1].replaceFirst("-", "/");
            String commitUnfix = unfix[2];
            String commitFix = fix[1];
            String pathUnfix = unfix[3];
            String pathFix = fix[2];
            String unfixStart = unfix[4];
            String unfixEnd = unfix[5];
            String urlUnfix = prefix + repo + "/" + commitUnfix + "/" + pathUnfix;
            String urlFix = prefix + repo + "/" + commitFix + "/" + pathFix;
            if (!downloaded.contains(urlUnfix)) {
                System.out.println(urlUnfix);
                Util.downloadFile(urlUnfix, prefixSave + commitUnfix + ".java");
                downloaded.add(urlUnfix);
            }
            if (!downloaded.contains(urlFix)) {
                System.out.println(urlFix);
                Util.downloadFile(urlFix, prefixSave + commitFix + ".java");
                downloaded.add(urlFix);
            }
        }
    }

}
