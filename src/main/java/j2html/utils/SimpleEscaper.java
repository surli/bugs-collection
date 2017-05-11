package j2html.utils;

public class SimpleEscaper {

    public static String escape(String string) {
        StringBuilder escapedText = new StringBuilder();
        char currentChar;
        for (int i = 0; i < string.length(); i++) {
            currentChar = string.charAt(i);
            switch (currentChar) {
                case '<':
                    escapedText.append("&lt;");
                    break;
                case '>':
                    escapedText.append("&gt;");
                    break;
                case '&':
                    escapedText.append("&amp;");
                    break;
                case '"':
                    escapedText.append("&quot;");
                    break;
                case '\'':
                    escapedText.append("&#x27;");
                    break;
                case '/':
                    escapedText.append("&#x2F;");
                    break;
                default:
                    escapedText.append(currentChar);
            }
        }
        return escapedText.toString();
    }

}
