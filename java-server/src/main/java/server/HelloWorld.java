package server;

import java.util.List;

public class HelloWorld {

    public static void main(String[] args) throws InterruptedException {
        List<String> words = List.of("Hello", "World");
        System.out.println(words);
        Thread thread = Thread.ofVirtual().start(() -> {
            test(25);
            test("25");
        });
        thread.join();
    }

    private static void test(Object obj) {
        System.out.println(switch (obj) {
            case Integer i -> i;
            default -> "Not a number";
        });
    }
}
