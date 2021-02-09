package org.fmi;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutputArticle {

    public int day;
    public int month;
    public int year;

    public List<String> titleTokens;
    public List<String> summaryTokens;
    public List<String> bodyTokens;

    public String media;

    public String relativeUrl;
}
