package ru.ruranobe.mybatis.entities.tables;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.Strings;
import ru.ruranobe.wicket.webpages.common.TextPage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Chapter extends PageRepresentable implements Serializable
{

    private static final long serialVersionUID = 1L;
    private Integer chapterId;
    private Integer volumeId;
    private Integer textId;
    private String url;
    private String title;
    private Integer orderNumber;
    private Date publishDate;
    private boolean nested;
    /* Optional */
    private Chapter prevChapter;
    private Chapter nextChapter;
    private Chapter parentChapter;
    private List<Chapter> childChapters;
    private boolean visibleOnPage = false;
    private Text text;

    public Chapter()
    {
    }

    public Chapter(Integer volumeId, Integer textId, String url, String title, Integer orderNumber, boolean nested)
    {
        this.volumeId = volumeId;
        this.textId = textId;
        this.url = url;
        this.title = title;
        this.orderNumber = orderNumber;
        this.nested = nested;
    }

    public static PageParameters makeUrlParameters(String[] urlParts)
    {
        return new PageParameters().set("project", urlParts[0]).set("volume", urlParts[1]).set("chapter", urlParts[2]);
    }

    public Date getPublishDate()
    {
        return publishDate;
    }

    public Class getLinkClass()
    {
        return TextPage.class;
    }

    public PageParameters getUrlParameters()
    {
        if (url == null)
        {
            return null;
        }
        return makeUrlParameters(url.split("/", -1));
    }

    public Integer getChapterId()
    {
        return chapterId;
    }

    public void setChapterId(Integer chapterId)
    {
        this.chapterId = chapterId;
    }

    public boolean isNested()
    {
        return nested;
    }

    public void setNested(boolean nested)
    {
        this.nested = nested;
    }

    public Integer getOrderNumber()
    {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber)
    {
        this.orderNumber = orderNumber;
    }

    public boolean isPublished()
    {
        return publishDate != null && new Date().after(publishDate);
    }

    public void setPublished(boolean published)
    {
        this.publishDate = published ? new Date() : null;
    }

    public Integer getTextId()
    {
        return textId;
    }

    public void setTextId(Integer textId)
    {
        this.textId = textId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getLeveledTitle()
    {
        return (nested ? "　" : "") + title;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrlPart()
    {
        return url.split("/", -1)[2];
    }

    public void setUrlPart(String urlPart)
    {
        String[] parts = this.url.split("/", -1);
        parts[2] = urlPart;
        this.url = Strings.join("/", parts);
    }

    public Integer getVolumeId()
    {
        return volumeId;
    }

    public void setVolumeId(Integer volumeId)
    {
        this.volumeId = volumeId;
    }

    public Chapter getPrevChapter()
    {
        return prevChapter;
    }

    public void setPrevChapter(Chapter prevChapter)
    {
        this.prevChapter = prevChapter;
    }

    public Chapter getNextChapter()
    {
        return nextChapter;
    }

    public void setNextChapter(Chapter nextChapter)
    {
        this.nextChapter = nextChapter;
    }

    public Chapter getParentChapter()
    {
        return parentChapter;
    }

    public void setParentChapter(Chapter parentChapter)
    {
        this.parentChapter = parentChapter;
    }

    public List<Chapter> getChildChapters()
    {
        return childChapters;
    }

    public void setChildChapters(List<Chapter> childChapters)
    {
        this.childChapters = childChapters;
    }

    public void addChildChapter(Chapter chapter)
    {
        if (this.childChapters == null)
        {
            this.childChapters = new ArrayList<>();
        }
        this.childChapters.add(chapter);
    }

    public boolean hasChildChapters()
    {
        return this.childChapters != null && !this.childChapters.isEmpty();
    }

    public boolean isVisibleOnPage()
    {
        return visibleOnPage;
    }

    public void setVisibleOnPage(boolean visibleOnPage)
    {
        this.visibleOnPage = visibleOnPage;
    }

    public Text getText()
    {
        return text;
    }

    public void setText(Text text)
    {
        this.text = text;
        this.textId = text == null ? null : text.getTextId();
    }

}
