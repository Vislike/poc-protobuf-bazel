package poc.client;

public class Terminal {

    private Terminal() {
    }

    public static void systemMessage(String text) {
        System.out.println(Utils.Color.YELLOW.wrap(text));
    }
}
