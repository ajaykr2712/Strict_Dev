public class InputValidationPatternExample {
    static class Validator {
        static boolean isSafeUsername(String s){
            if (s == null) return false;
            if (s.length() < 3 || s.length() > 20) return false;
            return s.matches("[A-Za-z0-9_]+");
        }
        static boolean isSafeEmail(String s){
            if (s == null) return false;
            return s.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
        }
    }
    public static void main(String[] args){
        System.out.println("username ok? " + Validator.isSafeUsername("john_doe"));
        System.out.println("username ok? " + Validator.isSafeUsername("../hack"));
        System.out.println("email ok? " + Validator.isSafeEmail("user@example.com"));
        System.out.println("email ok? " + Validator.isSafeEmail("bad@com"));
    }
}
