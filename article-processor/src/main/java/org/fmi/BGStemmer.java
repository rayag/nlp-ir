package org.fmi;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stemming algorithm by Preslav Nakov.
 * @author Alexander Alexandrov, e-mail: sencko@mail.bg
 * @since 2003-9-30
 */
public class BGStemmer {

    public static final int STEM_BOUNDARY = 1;
    public static final Pattern VOCALS = Pattern.compile("[^аъоуеияю]*[аъоуеияю]");
    public static final Pattern P = Pattern.compile("([а-я]+)\\s==>\\s([а-я]+)\\s([0-9]+)");

    public final Map<String, String> stemmingRules = new HashMap<>();

    public void loadStemmingRules(String fileName) throws Exception {
        stemmingRules.clear();

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(fileName);

        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        String s;
        while ((s = br.readLine()) != null) {
            Matcher m = P.matcher(s);
            if (m.matches()) {
                int j = m.groupCount();
                if (j == 3) {
                    if (Integer.parseInt(m.group(3)) > STEM_BOUNDARY) {
                        stemmingRules.put(m.group(1), m.group(2));
                    }
                }
            }
        }
    }

    public String stem(String word) {
        Matcher m = VOCALS.matcher(word);
        if (!m.lookingAt()) {
            return word;
        }

        for (int i = m.end() + 1; i < word.length(); i++) {
            String suffix = word.substring(i);
            if ((suffix = stemmingRules.get(suffix)) != null) {
                return word.substring(0, i) + suffix;
            }
        }

        return word;
    }
}
