package org.fmi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bg.swu.nlp.tools.bglang.BgDictionary;

public class RangedPolaritySentimentStrategy implements SentimentStrategy {

    private static final double DELTA_VALUE = 0.01;

    private static final double TITLE_WEIGHT = 0.40;
    private static final double SUMMARY_WEIGHT = 0.35;
    private static final double BODY_WEIGHT = 0.25;

    private static final String WORDS_POLARITY_FILE = "words-with-polarity.txt";

    private final Map<String, Double> WORDS_POLARITY = new HashMap<>();

    private final BGLemmatizer bgLemmatizer;
    private final BgDictionary bgDictionary;

    public RangedPolaritySentimentStrategy(BGLemmatizer bgLemmatizer, BgDictionary bgDictionary)
            throws IOException {

        this.bgLemmatizer = bgLemmatizer;
        this.bgDictionary = bgDictionary;

        populateWordsWithPolarity(WORDS_POLARITY, WORDS_POLARITY_FILE);
    }

    @Override
    public int getArticlePolarity(OutputArticle outputArticle) {
        double titleSum = polaritySumTokens(outputArticle.titleTokens);
        double summarySum = polaritySumTokens(outputArticle.summaryTokens);
        double bodySum = polaritySumTokens(outputArticle.bodyTokens);

        double numerator = titleSum * TITLE_WEIGHT
                + summarySum * SUMMARY_WEIGHT
                + bodySum * BODY_WEIGHT;

        double finalScore = numerator / calcEquationDenominator(outputArticle);
        if (Math.abs(finalScore) > DELTA_VALUE) {
            if (finalScore > 0) {
                return 1;
            } else if (finalScore < 0) {
                return -1;
            }
        }

        return 0;
    }

    private int calcEquationDenominator(OutputArticle outputArticle) {
        int denominator = 0;
        if (outputArticle.titleTokens != null && outputArticle.titleTokens.size() > 0) {
            denominator++;
        }

        if (outputArticle.summaryTokens != null && outputArticle.summaryTokens.size() > 0) {
            denominator++;
        }

        if (outputArticle.bodyTokens != null && outputArticle.bodyTokens.size() > 0) {
            denominator++;
        }

        return denominator;
    }

    private double polaritySumTokens(List<String> tokens) {
        double sum = 0.0;
        int negationRun = 0;
        boolean negate = false;

        for (String token : tokens) {
            if (negate) {
                negationRun++;
            }

            if (negationRun > 2) {
                negate = false;
                negationRun = 0;
            }

            if ("не".equals(token)) {
                negate = true;
                negationRun = 0;
                continue;
            }

            if (!WORDS_POLARITY.containsKey(token)) {
                continue;
            }

            if (negate) {
                sum += -WORDS_POLARITY.get(token);
            } else {
                sum += WORDS_POLARITY.get(token);
            }
        }

        return sum;
    }

    private void populateWordsWithPolarity(Map<String, Double> holder, String file)
            throws IOException {

        ClassLoader classLoader = SentimentAnalyzer.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(file);

        Map<String, List<Double>> wordToPolarities = new HashMap<>();
        try (InputStreamReader streamReader =
                new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] wordToPolarity = line.split("\\s+");

                String word = wordToPolarity[0];
                Double polarity = Double.valueOf(wordToPolarity[1]);

                String finalWord = word;
                String wordLemma = bgLemmatizer.getLemma(bgDictionary, word, null);
                if (wordLemma != null) {
                    finalWord = wordLemma;
                }

                wordToPolarities.putIfAbsent(finalWord, new ArrayList<>());
                wordToPolarities.get(finalWord).add(polarity);
            }
        }

        for (Map.Entry<String, List<Double>> entry : wordToPolarities.entrySet()) {
            // due to lemmatization and dataset itself it is possible to have multiple polarities
            holder.put(
                    entry.getKey(),
                    entry.getValue().stream().mapToDouble(v -> v).average().getAsDouble());
        }
    }
}
