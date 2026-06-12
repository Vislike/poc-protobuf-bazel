package poc.client.util;

import poc.client.util.Utils.Color;

public class Terminal {

    private Terminal() {
    }

    public static void systemMessage(String text) {
        System.out.println(Color.YELLOW.wrap(text));
    }

    public static void redMessage(String red, String text) {
        System.out.println(Color.RED.highlight(red, text));
    }

    public static void userConnected(String userName) {
        System.out.println(Color.GREEN.wrap(userName) + Color.CYAN.wrap(" connected"));
    }

    public static void userDisconnected(String userName) {
        System.out.println(Color.GREEN.wrap(userName) + Color.MAGENTA.wrap(" disconnected"));
    }

    public static void chatSelf(String userName, String text) {
        System.out.println(Color.WHITE_INTENSE.highlight(userName, text));
    }

    public static void chatOther(String userName, String text) {
        System.out.println(Color.GREEN.highlight(userName, text));
    }
}
