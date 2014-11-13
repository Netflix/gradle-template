package io.reactivex.lab.gateway.clients;

import java.util.List;

public class UrlGenerator {

    public static String generate(String paramName, List<?> list) {
        StringBuffer url = new StringBuffer();
        for (Object o : list) {
            if (url.length() > 0) {
                url.append("&");
            }
            url.append(paramName).append("=");
            if (o instanceof ID) {
                url.append(((ID) o).getId());
            } else {
                url.append(o);
            }

        }
        return url.toString();
    }
}
