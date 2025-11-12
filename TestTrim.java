public class TestTrim {
    public static void main(String[] args) {
        String test1 = "/test\u0001";
        String test2 = "test\u0001";
        String test3 = "\u0001test";
        
        System.out.println("Original 1: '" + test1 + "'");
        System.out.println("Trimmed 1: '" + test1.trim() + "'");
        System.out.println("Length original: " + test1.length());
        System.out.println("Length trimmed: " + test1.trim().length());
        
        System.out.println("\nOriginal 2: '" + test2 + "'");
        System.out.println("Trimmed 2: '" + test2.trim() + "'");
        System.out.println("Length original: " + test2.length());
        System.out.println("Length trimmed: " + test2.trim().length());
        
        System.out.println("\nOriginal 3: '" + test3 + "'");
        System.out.println("Trimmed 3: '" + test3.trim() + "'");
        System.out.println("Length original: " + test3.length());
        System.out.println("Length trimmed: " + test3.trim().length());
    }
}
