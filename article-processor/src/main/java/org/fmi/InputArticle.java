package org.fmi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InputArticle {

    public int day;
    public int month;
    public int year;

    public String title;
    public String summary;
    public String body;

    public String media;

    public String relativeUrl;
}
