package poc.client.util;

public class Utils {

    private Utils() {
    }

    public static enum Color {
        RESET("\u001B[m"), RED("\u001B[31m"), GREEN("\u001B[32m"), YELLOW("\u001B[33m"), MAGENTA("\u001B[35m"),
        CYAN("\u001B[36m"), WHITE_INTENSE("\u001B[97m");

        private final String color;

        Color(String color) {
            this.color = color;
        }

        public String wrap(String text) {
            return color + text + Color.RESET.color;
        }

        public String highlight(String highlight, String normal) {
            return color + highlight + Color.RESET.color + " " + normal;
        }
    }
}
