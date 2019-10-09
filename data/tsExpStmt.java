package org.javaparser.samples;

import java.util.ArrayList;

public class tsExpStmt {
    public void testCall() {
        int m;
        int n;
        test(m, n);
    }

    public void test(int m, int n){
        int x=0;
        ArrayList<Integer> array = new ArrayList<>();
        array.add(1);
        array.add(1);
        for (Integer i :
                array) {
            System.out.println("i = " + i);
        }
        switch (x){ // switch ——> 条件 ——> 各case ——> case内容，有无default无法处理
            case 1:
                x = 1;
                x = 52;
            case 2:
                x = 2;
                x = 56;
                break;
            case 4:
                x = 4;
                break;
        }
        if(true){ // if ——> 条件 ——> 块
            x++;
        } else if (false) { // if ——> else ——> 块
            x = 100;
            x--;
        } else if (true) {
            x++;
        } else {
            x--;
        }
        while(x<10){ // While 循环条件判断
            if(x==0){
                x = 4;
                continue;
            }
            x++;
        }
        x--;

        for(int i=0; // for 初始化，条件判断等顺序
            i<5;
            i++){
            x++;
        }

        x=1;

        for(int i=0,j=1;i+j<5;i++,j++){
            x++;
            if (true) {
                continue;
            } else if (false) {
                continue;
            }
            if (true) {
                break;
            } else if (false) {
                break;
            }
            x--;
        }

        do{  // do 开头
            x++;
            x--;
        } while (x==0); // while 开头

        while(x==0){ // while 开头——> 条件
            x++;
        }
        int a = 1;
        int b = 2;
        int c = 3;
        try {
            x = 4;
            x++;
        } catch (IllegalArgumentException e) {
            System.out.println("e = " + e);
            x--;
        } catch (Exception e) {
            System.out.println("e = " + e);
            x++;
        } finally {
            x = 0;
        }
    }

}