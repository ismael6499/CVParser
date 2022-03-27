package utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailFinder {

    public static String find(String emailStr) {
        Matcher matcher = Pattern.compile("[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+", Pattern.CASE_INSENSITIVE).matcher(emailStr);
        if( matcher.find()){
            return matcher.group(0);
        }
        return "";
    }
}
