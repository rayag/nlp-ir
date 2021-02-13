package org.fmi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import bg.swu.nlp.tools.bglang.BGLangTools;
import bg.swu.nlp.tools.bglang.BgDictionary;

public class Main {

    private static final Set<String> STOP_WORDS = new HashSet<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final BGLemmatizer BG_LEMMATIZER = new BGLemmatizer();

    private static final String STOP_WORDS_FILE = "stop-words.txt";
    private static final String BASE_PATH = "/Users/sgenchev/irl";

    private static final BgDictionary BG_DICTIONARY;

    static {
        BgDictionary temp = null;
        try {
            temp = BGLangTools.loadBuiltinDictionary();
        } catch (Exception e) {
            e.printStackTrace();
        }

        BG_DICTIONARY = temp;
    }

    private static final Map<String, String> MEDIA_INPUT_TO_OUTPUT_PATH = Map.of(
            BASE_PATH + "/btv/2021-1-27", BASE_PATH + "/btv-output/2021-1-27",
            BASE_PATH + "/dnevnik/2021-1-30", BASE_PATH + "/dnevnik-output/2021-1-30",
            BASE_PATH + "/nova/2021-1-30", BASE_PATH + "/nova-output/2021-1-30",
            BASE_PATH + "/vesti/2021-1-30", BASE_PATH + "/vesti-output/2021-1-30",
            BASE_PATH + "/dirbg/2021-1-31", BASE_PATH + "/dirbg-output/2021-1-31",
            BASE_PATH + "/manager/2021-1-31", BASE_PATH + "/manager-output/2021-1-31",
            BASE_PATH + "/economy/2021-1-31", BASE_PATH + "/economy-output/2021-1-31",
            BASE_PATH + "/novinibg/2021-1-30", BASE_PATH + "/novinibg-output/2021-1-30"
    );

    public static void main(String[] args) throws Exception {
        populateStopWords();

        MEDIA_INPUT_TO_OUTPUT_PATH.forEach((inputPath, outputPath) -> {
            System.out.println(String.format("In folder %s", inputPath));

            new Thread(() -> {
                Path dir = Paths.get(inputPath);
                try {
                    Files.walk(dir)
                            .map(Path::toFile)
                            .filter(File::isFile)
                            .forEach(file -> {
                                InputArticle inputArticle;
                                try {
                                    inputArticle = mapper.readValue(file, InputArticle.class);
                                } catch (IOException e) {
                                    System.out.println(String.format("On file %s", file.getAbsolutePath()));
                                    e.printStackTrace();
                                    return;
                                }

                                OutputArticle outputArticle = inputArticleToOutputArticle(inputArticle);
                                File articleFile = new File(String.format("%s/%s", outputPath, file.getName()));
                                try {
                                    mapper.writeValue(articleFile, outputArticle);
                                } catch (IOException e) {
                                    System.out.println(String.format("On file %s", file.getAbsolutePath()));
                                    e.printStackTrace();
                                }
                            });
                } catch (IOException e) {
                    System.out.println(String.format("On folder %s", inputPath));
                    e.printStackTrace();
                }
            }).start();

            System.out.println(String.format("Out of folder %s", inputPath));
        });
    }

    private static List<String> textToTokens(String text) {
        String[] splitText = text.trim().split("\\s+");
        List<String> tokens = Arrays.stream(splitText)
                .map(token -> token.replaceAll("[^-а-яА-Я]", ""))
                .map(String::trim)
                .filter(token ->
                        !token.isEmpty()
                                && !token.startsWith("-")
                                && !token.endsWith("-")
                                && !STOP_WORDS.contains(token))
                .map(token -> Objects.requireNonNullElse(
                        BG_LEMMATIZER.getLemma(BG_DICTIONARY, token, null), token))
                .collect(Collectors.toList());

        return tokens;
    }

    private static OutputArticle inputArticleToOutputArticle(InputArticle inputArticle) {
        OutputArticle.OutputArticleBuilder outputArticleBuilder = OutputArticle.builder();

        outputArticleBuilder.day(inputArticle.day);
        outputArticleBuilder.month(inputArticle.month);
        outputArticleBuilder.year(inputArticle.year);
        outputArticleBuilder.media(inputArticle.media);
        outputArticleBuilder.relativeUrl(inputArticle.relativeUrl);

        if (inputArticle.title != null) {
            outputArticleBuilder.titleTokens(textToTokens(inputArticle.title.toLowerCase()));
        }

        if (inputArticle.summary != null) {
            outputArticleBuilder.summaryTokens(textToTokens(inputArticle.summary.toLowerCase()));
        }

        if (inputArticle.body != null) {
            outputArticleBuilder.bodyTokens(textToTokens(inputArticle.body.toLowerCase()));
        }

        return outputArticleBuilder.build();
    }

    private static void populateStopWords() throws IOException {
        ClassLoader classLoader = Main.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(STOP_WORDS_FILE);

        try (InputStreamReader streamReader =
                new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(streamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                STOP_WORDS.add(line);
            }
        }
    }
}
