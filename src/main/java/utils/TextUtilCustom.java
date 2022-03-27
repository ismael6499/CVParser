package utils;

public class TextUtilCustom {
    public static String formatToName(String str) {
        if(str == null || str.isEmpty()) {
            return str;
        }

         str = str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
        return str;
    }
}
