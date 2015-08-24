package ru.ruranobe.engine.wiki.parser;

import com.google.common.collect.ImmutableMap;
import static ru.ruranobe.engine.wiki.parser.WikiTagType.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Replacement
{

    public Replacement(WikiTag tag)
    {
        this.startPosition = tag.getStartPosition();
        this.endPosition = tag.getStartPosition()+tag.getWikiTagLength();
        this.mainTag = tag;
        String replacementText = TAG_TO_REPLACEMENT_TEXT.get(tag.getWikiTagType());
        if (tag.getWikiTagType() == FOOTNOTE)
        {
            Map<String, String> attributeNameToValue = tag.getAttributeNameToValue();
            String hrefTag = "href=\"" + attributeNameToValue.get("href") + "\"";
            this.replacementText = String.format(replacementText, tag.getUniqueId(), hrefTag, tag.getListOrderNumber());
        }
        else if (tag.getWikiTagType() == IMAGE)
        {
            this.replacementText = String.format(replacementText, tag.getImageUrl());
        }
        else if (tag.getWikiTagType() == NEW_LINE)
        {
            String additionalTags = "";
            Map<String, String> attributeNameToValue = tag.getAttributeNameToValue();
            if (attributeNameToValue != null && !attributeNameToValue.isEmpty())
            {
                StringBuilder additionalTagsBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry: attributeNameToValue.entrySet())
                {
                    additionalTagsBuilder.append(entry.getKey())
                            .append("=\"")
                            .append(entry.getValue())
                            .append("\" ");
                    additionalTags = additionalTagsBuilder.toString();
                }
            }
            this.replacementText = String.format(replacementText, tag.getUniqueId(), additionalTags);
        }
        else
        {
            this.replacementText = replacementText;
        }
    }

    public static List<Replacement> getReplacementsForPair(WikiTag startTag, WikiTag endTag)
    {
        List<Replacement> replacements = new ArrayList<Replacement>();
        if (startTag.getWikiTagType() == FOOTNOTE)
        {
            String replacementText = TAG_TO_REPLACEMENT_TEXT.get(FOOTNOTE);
            replacementText = String.format(replacementText, startTag.getUniqueId(), startTag.getUniqueId(), startTag.getListOrderNumber());
            replacements.add(new Replacement(startTag.getStartPosition(), endTag.getStartPosition() + endTag.getWikiTagLength(), replacementText, startTag));
            return replacements;
        }
        WikiTagPair tagPair = new WikiTagPair(startTag.getWikiTagType(), endTag.getWikiTagType());
        String replacementText = PAIR_TO_START_REPLACEMENT_TEXT.get(tagPair);
        if (startTag.getWikiTagType() == TWO_EQUAL ||
            startTag.getWikiTagType() == THREE_EQUAL ||
            startTag.getWikiTagType() == FOUR_EQUAL)
        {
            replacementText = String.format(replacementText, startTag.getUniqueId());
        }
        replacements.add(new Replacement(startTag.getStartPosition(), startTag.getStartPosition()+startTag.getWikiTagLength(), replacementText, startTag));
        tagPair = new WikiTagPair(startTag.getWikiTagType(), endTag.getWikiTagType());
        replacements.add(new Replacement(endTag.getStartPosition(), endTag.getStartPosition()+endTag.getWikiTagLength(), PAIR_TO_END_REPLACEMENT_TEXT.get(tagPair), startTag));
        return replacements;
    }

    public int getStartPosition()
    {
        return startPosition;
    }

    public int getEndPosition()
    {
        return endPosition;
    }

    public String getReplacementText()
    {
        return replacementText;
    }

    public WikiTag getMainTag()
    {
        return mainTag;
    }

    private Replacement(int startPosition, int endPosition, String replacementText, WikiTag mainTag)
    {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.replacementText = replacementText;
        this.mainTag = mainTag;
    }

    private static class WikiTagPair implements Map.Entry<WikiTagType, WikiTagType>
    {
        private final WikiTagType key;
        private WikiTagType value;

        public WikiTagPair(WikiTagType key, WikiTagType value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public WikiTagType getKey()
        {
            return key;
        }

        @Override
        public WikiTagType getValue()
        {
            return value;
        }

        @Override
        public WikiTagType setValue(WikiTagType value)
        {
            this.value = value;
            return this.value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            WikiTagPair that = (WikiTagPair) o;

            if (key != that.key) return false;
            return value == that.value;

        }

        @Override
        public int hashCode() {
            int result = key.getWikiTagCharSequence().hashCode();
            result = 31 * result + value.getWikiTagCharSequence().hashCode();
            return result;
        }
    }

    private static final Map<WikiTagType, String> TAG_TO_REPLACEMENT_TEXT =
            new EnumMap<WikiTagType, String>(WikiTagType.class)
    {
        {
            put(NEW_LINE, "</p><p id=\"%s\" %s>");
            put(FOOTNOTE, "<sup id=\"cite_ref-%s\" class=\"reference\"><a href=\"%s\">[%d]</a></sup>");
            put(IMAGE, "<img src=\'%s\'/>");
        }
    };

    private static final Map<WikiTagPair, String> PAIR_TO_START_REPLACEMENT_TEXT = new ImmutableMap.Builder<WikiTagPair, String>()
            .put(new WikiTagPair(SUBTITLE, DOUBLE_END_BRACKET), "<div class=\"subtitle\">")
            .put(new WikiTagPair(TWO_EQUAL, TWO_EQUAL), "<h2 id=\"h_id-%s\">")
            .put(new WikiTagPair(THREE_EQUAL, THREE_EQUAL), "<h3 id=\"h_id-%s\">")
            .put(new WikiTagPair(FOUR_EQUAL, FOUR_EQUAL), "<h4 id=\"h_id-%s\">")
            .put(new WikiTagPair(TWO_QUOTES, TWO_QUOTES), "<i>")
            .put(new WikiTagPair(THREE_QUOTES, THREE_QUOTES), "<b>")
            .build();

    private static final Map<WikiTagPair, String> PAIR_TO_END_REPLACEMENT_TEXT = new ImmutableMap.Builder<WikiTagPair, String>()
            .put(new WikiTagPair(SUBTITLE, DOUBLE_END_BRACKET), "</div>")
            .put(new WikiTagPair(TWO_EQUAL, TWO_EQUAL), "</h2>")
            .put(new WikiTagPair(THREE_EQUAL, THREE_EQUAL), "</h3>")
            .put(new WikiTagPair(FOUR_EQUAL, FOUR_EQUAL), "</h4>")
            .put(new WikiTagPair(TWO_QUOTES, TWO_QUOTES), "</i>")
            .put(new WikiTagPair(THREE_QUOTES, THREE_QUOTES), "</b>")
            .build();

    private final int startPosition;
    private final int endPosition;
    private final String replacementText;
    private final WikiTag mainTag;
}
