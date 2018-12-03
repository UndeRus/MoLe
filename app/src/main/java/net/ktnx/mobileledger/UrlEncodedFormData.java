package net.ktnx.mobileledger;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

class UrlEncodedFormData {
    private List<AbstractMap.SimpleEntry<String,String>> pairs;

    UrlEncodedFormData() {
        pairs = new ArrayList<AbstractMap.SimpleEntry<String, String>>();
    }

    void add_pair(String name, String value) {
        pairs.add(new AbstractMap.SimpleEntry<String,String>(name, value));
    }

    @NonNull
    public String toString() {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry<String,String> pair : pairs) {
            if (first) {
                first = false;
            }
            else {
                result.append('&');
            }

            try {
                result.append(URLEncoder.encode(pair.getKey(), "UTF-8"))
                      .append('=')
                      .append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return result.toString();
    }
}
