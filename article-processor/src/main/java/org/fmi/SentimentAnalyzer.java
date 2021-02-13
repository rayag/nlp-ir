package org.fmi;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import bg.swu.nlp.tools.bglang.BGLangTools;
import bg.swu.nlp.tools.bglang.BgDictionary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import org.fmi.SentimentAnalyzer.QuarterMediaData.StrategyResult;

public class SentimentAnalyzer {

    private static final String BASE_PATH = "/Users/sgenchev/irl";
    private static final Map<String, String> MEDIA_DIRECTORIES = new HashMap<>();
    static {
        MEDIA_DIRECTORIES.put("btv", BASE_PATH + "/btv-output/2021-1-27");
        MEDIA_DIRECTORIES.put("dnevnik", BASE_PATH + "/dnevnik-output/2021-1-30");
        MEDIA_DIRECTORIES.put("nova", BASE_PATH + "/nova-output/2021-1-30");
        MEDIA_DIRECTORIES.put("vesti", BASE_PATH + "/vesti-output/2021-1-30");
        MEDIA_DIRECTORIES.put("dirbg", BASE_PATH + "/dirbg-output/2021-1-31");
        MEDIA_DIRECTORIES.put("manager", BASE_PATH + "/manager-output/2021-1-31");
        MEDIA_DIRECTORIES.put("economy", BASE_PATH + "/economy-output/2021-1-31");
        MEDIA_DIRECTORIES.put("novinibg", BASE_PATH + "/novinibg-output/2021-1-30");
    }

    private static final String ARTICLE_DATE_FORMAT = "%s-%s-%s";

    private static final DateTimeFormatter FILE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("d-M-yyyy");

    public static void main(String[] args) throws Exception {
        BGLemmatizer bgLemmatizer = new BGLemmatizer();
        BgDictionary bgDictionary = BGLangTools.loadBuiltinDictionary();
        ObjectMapper mapper = new ObjectMapper();

        SentimentStrategy rangedPolarityStrategy =
                new RangedPolaritySentimentStrategy(bgLemmatizer, bgDictionary);
        SentimentStrategy countingStrategy = new SimplyCountingSentimentStrategy(bgLemmatizer,
                bgDictionary);

        Map<String, MediaQuarters> mediaToQuarters = new HashMap<>();
        for (Map.Entry<String, String> entry : MEDIA_DIRECTORIES.entrySet()) {
            String directoryPath = entry.getValue();
            System.out.println(directoryPath);

            File directory = new File(directoryPath);
            File[] files = directory.listFiles();

            // listFiles doesn't guarantee ordering
            Comparator<File> fileComparator = (file1, file2) -> {
                String file1StringDate = file1.getName()
                        .substring(0, file1.getName().lastIndexOf("-"));
                String file2StringDate = file2.getName()
                        .substring(0, file2.getName().lastIndexOf("-"));

                return LocalDate.parse(file1StringDate, FILE_TIME_FORMATTER)
                        .compareTo(LocalDate.parse(file2StringDate, FILE_TIME_FORMATTER));
            };
            Arrays.sort(files, fileComparator.reversed());

            int positiveCountForCurrentQuarterRanged = 0;
            int negativeCountForCurrentQuarterRanged = 0;
            int neutralCountForCurrentQuarterRanged = 0;

            int positiveCountForCurrentQuarterCounting = 0;
            int negativeCountForCurrentQuarterCounting = 0;
            int neutralCountForCurrentQuarterCounting = 0;

            String currentDate = files[0].getName().substring(0, files[0].getName().lastIndexOf("-"));
            LocalDate lastPeriodEndDate = LocalDate.parse(currentDate, FILE_TIME_FORMATTER);

            mediaToQuarters.put(entry.getKey(), MediaQuarters.builder()
                    .media(entry.getKey())
                    .quarters(new ArrayList<>())
                    .build());
            for (File file : files) {
                OutputArticle outputArticle = SentimentUtil.fileToOutputArticle(file, mapper);
                if (outputArticle == null) {
                    continue;
                }

                currentDate = String.format(ARTICLE_DATE_FORMAT,
                        outputArticle.day, outputArticle.month, outputArticle.year);
                LocalDate currentDateLocalDate = LocalDate.parse(currentDate, FILE_TIME_FORMATTER);

                long between = ChronoUnit.MONTHS.between(currentDateLocalDate, lastPeriodEndDate);
                if (Math.abs(between) > 2) {
                    mediaToQuarters.get(entry.getKey()).quarters.add(
                            buildQuarterMediaData(
                                    currentDateLocalDate.toString(),
                                    lastPeriodEndDate.toString(),
                                    positiveCountForCurrentQuarterCounting,
                                    negativeCountForCurrentQuarterCounting,
                                    neutralCountForCurrentQuarterCounting,
                                    positiveCountForCurrentQuarterRanged,
                                    negativeCountForCurrentQuarterRanged,
                                    neutralCountForCurrentQuarterRanged));

                    lastPeriodEndDate = currentDateLocalDate;

                    positiveCountForCurrentQuarterRanged = 0;
                    negativeCountForCurrentQuarterRanged = 0;
                    neutralCountForCurrentQuarterRanged = 0;

                    positiveCountForCurrentQuarterCounting = 0;
                    negativeCountForCurrentQuarterCounting = 0;
                    neutralCountForCurrentQuarterCounting = 0;
                }

                int articlePolarityRangedStrategy =
                        rangedPolarityStrategy.getArticlePolarity(outputArticle);
                if (articlePolarityRangedStrategy < 0) {
                    negativeCountForCurrentQuarterRanged++;
                } else if (articlePolarityRangedStrategy == 0) {
                    neutralCountForCurrentQuarterRanged++;
                } else {
                    positiveCountForCurrentQuarterRanged++;
                }

                int articlePolarityCountingStrategy =
                        countingStrategy.getArticlePolarity(outputArticle);
                if (articlePolarityCountingStrategy < 0) {
                    negativeCountForCurrentQuarterCounting++;
                } else if (articlePolarityRangedStrategy == 0) {
                    neutralCountForCurrentQuarterCounting++;
                } else {
                    positiveCountForCurrentQuarterCounting++;
                }
            }

            // won't really be a quarter but still
            mediaToQuarters.get(entry.getKey()).quarters.add(buildQuarterMediaData(
                    LocalDate.parse(currentDate, FILE_TIME_FORMATTER).toString(),
                    lastPeriodEndDate.toString(),
                    positiveCountForCurrentQuarterCounting,
                    negativeCountForCurrentQuarterCounting,
                    neutralCountForCurrentQuarterCounting,
                    positiveCountForCurrentQuarterRanged,
                    negativeCountForCurrentQuarterRanged,
                    neutralCountForCurrentQuarterRanged));

            File file = new File(String.format("%s/%s/%s", BASE_PATH, "nlp-ir", entry.getKey()));
            try {
                mapper.writeValue(file, mediaToQuarters.get(entry.getKey()));
                System.out.println("Successful " + entry.getKey());
            } catch (IOException e) {
                System.out.println(String.format("On file %s", file.getAbsolutePath()));
                e.printStackTrace();
            }
        }
    }

    private static QuarterMediaData buildQuarterMediaData(
            String startDate,
            String endDate,
            int positiveCountForCurrentQuarterCounting,
            int negativeCountForCurrentQuarterCounting,
            int neutralCountForCurrentQuarterCounting,
            int positiveCountForCurrentQuarterRanged,
            int negativeCountForCurrentQuarterRanged,
            int neutralCountForCurrentQuarterRanged) {

        List<StrategyResult> strategyResults = new ArrayList<>();

        StrategyResult countingStrategyResult = StrategyResult.builder()
                .strategy("counting")
                .positiveCount(positiveCountForCurrentQuarterCounting)
                .negativeCount(negativeCountForCurrentQuarterCounting)
                .neutralCount(neutralCountForCurrentQuarterCounting)
                .build();
        strategyResults.add(countingStrategyResult);

        StrategyResult rangedPolarityStrategyResult = StrategyResult.builder()
                .strategy("ranged")
                .positiveCount(positiveCountForCurrentQuarterRanged)
                .negativeCount(negativeCountForCurrentQuarterRanged)
                .neutralCount(neutralCountForCurrentQuarterRanged)
                .build();
        strategyResults.add(rangedPolarityStrategyResult);

        return QuarterMediaData.builder()
                .periodStart(startDate)
                .periodEnd(endDate)
                .strategyResults(strategyResults)
                .build();
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class MediaQuarters {

        public String media;
        public List<QuarterMediaData> quarters = new ArrayList<>();
    }

    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class QuarterMediaData {


        public List<StrategyResult> strategyResults;

        public String periodStart;
        public String periodEnd;

        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static final class StrategyResult {

            public String strategy;

            public int positiveCount;
            public int negativeCount;
            public int neutralCount;
        }
    }
}
