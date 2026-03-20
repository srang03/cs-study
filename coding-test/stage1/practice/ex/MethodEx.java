    package ct.stage1.practice.ex;

    import java.util.*;
    import java.io.*;

    public class EX05_method {
    public static void main(String[] args) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StringTokenizer st = new StringTokenizer(br.readLine());
        
        int input = Integer.parseInt(st.nextToken());
        // some(input);
        printDigits(input, false);
    }

    public static void printDigits(int input, boolean isDebug) throws IOException {
        
        int count = 0;
        for(int i = 1; i <= input; i++){
            if (some(i)) {
                if(isDebug){
                    System.out.println(i);
                }
                count++;
            }
        }
        if(isDebug){
            System.out.println("count: " + count);
        }
        System.out.println(count);
    }

    public static boolean some(int input){
        List<Integer> digits = new ArrayList<>();
        
        while(input > 0){
            digits.add(input % 10);
            input = input / 10;
        }
        if(digits.size() < 3){
            return true;
        }
        
        int key = (digits.get(digits.size()-1) - digits.get(digits.size()-2));

        for(int i = digits.size()-1; i > 0; i--){
                if((digits.get(i) - digits.get(i - 1)) == key){
                } else{
                    return false;
                }
    }
        return true;
    }
    }
