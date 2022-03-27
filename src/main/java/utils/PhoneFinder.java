package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneFinder {
//    private static String patterns
//            = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$"
//            + "|^(\\+\\d{1,3}( )?)?(\\d{3}[ ]?){2}\\d{3}$"
//            + "|^(\\+\\d{1,3}( )?)?(\\d{3}[ ]?)(\\d{2}[ ]?){2}\\d{2}$";

    public static String find(String palabra) {
        try {
            String numero = palabra.replaceAll("[ ()-]", "");
//            String patterns = "^(?:(?:00)?549?)?0?(?:11|[2368]\\d)(?:(?=\\d{0,2}15)\\d{2})??\\d{8}$";
//            String patterns = "(((\\+?34([ \\t|\\-])?)?[9|6|7]((\\d{1}([ \\t|\\-])?[0-9]{3})|(\\d{2}([ \\t|\\-])?[0-9]{2}))([ \\t|\\-])?[0-9]{2}([ \\t|\\-])?[0-9]{2}))";
            String patterns = "(?:(?:00)?549?)?0?(?:11|[2368]\\d)?(?:(?=\\d{0,2}15)\\d{2})??\\d{8}";
            Pattern pattern = Pattern.compile(patterns);
            Matcher matcher = pattern.matcher(numero);
            if (matcher.find()) {
                return matcher.group(0);
            }
        } catch (Exception ignored) {

        }
        return "";
    }


}
