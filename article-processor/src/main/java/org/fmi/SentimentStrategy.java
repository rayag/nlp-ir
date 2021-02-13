package org.fmi;

public interface SentimentStrategy {

    String TITLE_SECTION_KEY = "title";
    String SUMMARY_SECTION_KEY = "summary";
    String BODY_SECTION_KEY = "body";

    // 1 positive
    // 0 neutral
    // -1 negative
    int getArticlePolarity(OutputArticle outputArticle);
}
