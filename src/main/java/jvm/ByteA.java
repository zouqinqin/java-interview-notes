package jvm;

public class ByteA {
//    public static void main(String[] args) {
//        int a =10 ;
//        int b = a ++ + ++a + a--;
//        System.out.println(a);
//        System.out.println(b);
//    }

    public static void main(String[] args) {
        boolean flag = false;
        for (int i = 0; i <= 3; i++) {
            if (i == 0) {
                System.out.println("0");
            } else if (i == 1) {
                System.out.println("1");
                continue;
            } else if (i == 2) {
                System.out.println("2");
                flag = true;
            } else if (i == 3) {
                System.out.println("3");
                break;
            } else if (i == 4) {
                System.out.println("4");
            }
            System.out.println("xixi");
        }
        // 0,xixi,1,2,xixi,3 haha
        if (flag) {
            System.out.println("haha");
            return;
        }
        System.out.println("heihei");
    }
}
