package io.virtualapp.delegate;

public class StringUtils {
    /**
     * 转化为合格的json
     *
     * @param baseString
     * @return
     */
    public static String replaceChar2JsonString(String baseString) {
        return baseString.replace("\\\\\\\"", "\"")
                .replace("\\\\\\\"", "\"")
                .replace("\\\"", "\"")
                .replace("\"{", "{")
                .replace("\"[", "[")
                .replace("\\\\n", "\n")
                .replace("\\n", "")
                .replace("\\\\t", "")
                .replace("]\"", "]")
                .replace("}\"", "}")
                .replace("\\\\t", "")
                .replace("\\t", "")
                .replace("&amp;", "&");
    }
}
