package poc.client;

public class Terminal {

    private Terminal() {
    }

    public static void systemMessage(String text) {
        System.out.println(Utils.Color.YELLOW.wrap(text));
    }

    public static void unknownMessage(String description, String msg) {
        System.out.println(Utils.Color.RED.highlight(description, msg));
    }

    public static void userEvent(String userName, String description) {
        System.out.println(Utils.Color.GREEN.wrap(userName) + " " + Utils.Color.CYAN.wrap(description));
    }

    public static void chatSelf(String userName, String text) {
        System.out.println(Utils.Color.WHITE_INTENSE.highlight(userName, text));
    }

    public static void chatOther(String userName, String text) {
        System.out.println(Utils.Color.GREEN.highlight(userName, text));
    }
}
