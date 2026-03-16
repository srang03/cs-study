package ct.stage1.practice.ex;

import java.util.*;

public class EX05_method {
    public static void main(String[] args){

    }
    public static int sum(int a, int b, int c){
        return a + b + c;
    }

    public static double average(int a, int b, int c){
        return sum(a,b,c) / 3.0;
    }
    
    public static double max(int a, int b, int c){
        return Math.max(a,Math.max(b, c));
    }
}