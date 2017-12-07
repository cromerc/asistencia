package cl.cromer.ubb.attendance;

import java.text.Normalizer;

final public class StringFixer {

    // Change accents to normal characters
    public static String normalizer(String string) {
        return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    // Fix capitalization and whitespace
    public static String fixCase(String string) {
        string = string.replaceAll("(?<=[A-Za-z0-9Á-Úá-ú])(Á)", "á");
        string = string.replaceAll("(?<=[A-Za-z0-9Á-Úá-ú])(É)", "é");
        string = string.replaceAll("(?<=[A-Za-z0-9Á-Úá-ú])(Í)", "í");
        string = string.replaceAll("(?<=[A-Za-z0-9Á-Úá-ú])(Ó)", "ó");
        string = string.replaceAll("(?<=[A-Za-z0-9Á-Úá-ú])(Ú)", "ú");
        string = string.replaceAll("(?<=[A-Za-z0-9Á-Úá-ú])(Ñ)", "ñ");
        string = string.trim();
        return string;
    }

    public static String removeInvalidFileCharacters(String string) {
        return string.replaceAll("/([|\\\\?*<>\":+\\[\\]\\/'])/", "");
    }
}
