package org.fmi;

import static org.fmi.SentimentStrategy.BODY_SECTION_KEY;
import static org.fmi.SentimentStrategy.SUMMARY_SECTION_KEY;
import static org.fmi.SentimentStrategy.TITLE_SECTION_KEY;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class SentimentUtil {

    private SentimentUtil() {
    }

    public static OutputArticle fileToOutputArticle(File outputArticleFile, ObjectMapper mapper) {
        try {
            return mapper.readValue(outputArticleFile, OutputArticle.class);
        } catch (IOException e) {
            System.out.println(String.format("On file %s", outputArticleFile.getAbsolutePath()));
            e.printStackTrace();
            return null;
        }
    }

    public static Map<String, Map<String, Integer>> sectionToWordAndOccurrences(
            OutputArticle outputArticle) {

        return Map.of(
            TITLE_SECTION_KEY, tokenToOccurrences(outputArticle.titleTokens),
            SUMMARY_SECTION_KEY, tokenToOccurrences(outputArticle.summaryTokens),
            BODY_SECTION_KEY, tokenToOccurrences(outputArticle.bodyTokens));
    }

    private static Map<String, Integer> tokenToOccurrences(List<String> tokens) {
        Map<String, Integer> tokenToOccurrences = new HashMap<>();
        if (tokens == null) {
            return tokenToOccurrences;
        }

        for (String token : tokens) {
            tokenToOccurrences.put(token, tokenToOccurrences.getOrDefault(token, 0) + 1);
        }

        return tokenToOccurrences;
    }
}
