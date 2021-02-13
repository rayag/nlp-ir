package org.fmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import bg.swu.nlp.tools.bglang.BgDictionary;

public class SimplyCountingSentimentStrategy implements SentimentStrategy {

    private static final String POSITIVE_WORDS_FILE = "positive-words.txt";
    private static final String NEGATIVE_WORDS_FILE = "negative-words.txt";

    private final Set<String> POSITIVE_WORDS = new HashSet<>();
    private final Set<String> NEGATIVE_WORDS = new HashSet<>();

    private final BGLemmatizer bgLemmatizer;
    private final BgDictionary bgDictionary;

    public SimplyCountingSentimentStrategy(BGLemmatizer bgLemmatizer, BgDictionary bgDictionary)
            throws IOException {

        this.bgLemmatizer = bgLemmatizer;
        this.bgDictionary = bgDictionary;

        populateWords(POSITIVE_WORDS, POSITIVE_WORDS_FILE);
        populateWords(NEGATIVE_WORDS, NEGATIVE_WORDS_FILE);
    }

    @Override
    public int getArticlePolarity(OutputArticle outputArticle) {
        return Double.compare(
                polaritySumTokens(outputArticle.titleTokens)
                        + polaritySumTokens(outputArticle.summaryTokens)
                        + polaritySumTokens(outputArticle.bodyTokens),
                0);
    }

    private int polaritySumTokens(List<String> tokens) {
        if (tokens == null) {
            return 0;
        }

        return tokens.stream()
                .map(token -> {
                    if (NEGATIVE_WORDS.contains(token)) {
                        return -1;
                    } else if (POSITIVE_WORDS.contains(token)) {
                        return 1;
                    } else {
                        return 0;
                    }
                })
                .mapToInt(v -> v)
                .sum();
    }

    private void populateWords(Set<String> holder, String file) throws IOException {
        ClassLoader classLoader = SimplyCountingSentimentStrategy.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(file);

        try (InputStreamReader streamReader =
                new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String lemma = bgLemmatizer.getLemma(bgDictionary, line, null);
                holder.add(lemma == null ? line : lemma);
            }
        }
    }
}
