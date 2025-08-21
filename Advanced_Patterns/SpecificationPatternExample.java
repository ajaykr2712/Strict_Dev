import java.util.*;
import java.util.function.Predicate;

public class SpecificationPatternExample {
    static class Product { final String name; final String category; final double price; Product(String n, String c, double p){ name=n; category=c; price=p; } public String toString(){ return name+"("+category+", $"+price+")"; } }
    interface Spec<T> { boolean isSatisfiedBy(T t); default Spec<T> and(Spec<T> other){ return t -> this.isSatisfiedBy(t) && other.isSatisfiedBy(t); } default Spec<T> or(Spec<T> other){ return t -> this.isSatisfiedBy(t) || other.isSatisfiedBy(t); } default Spec<T> not(){ return t -> !this.isSatisfiedBy(t); } static <T> Spec<T> from(Predicate<T> p){ return p::test; } }
    public static void main(String[] args){
        List<Product> products = List.of(new Product("MacBook", "laptop", 1500), new Product("ThinkPad", "laptop", 1200), new Product("iPhone", "phone", 999));
        Spec<Product> isLaptop = Spec.from(p -> p.category.equals("laptop"));
        Spec<Product> expensive = Spec.from(p -> p.price > 1300);
        products.stream().filter(p -> isLaptop.and(expensive).isSatisfiedBy(p)).forEach(System.out::println);
    }
}
