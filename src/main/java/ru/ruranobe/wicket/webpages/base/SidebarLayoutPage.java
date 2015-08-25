package ru.ruranobe.wicket.webpages.base;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import ru.ruranobe.wicket.components.sidebar.SidebarMiniSearch;
import ru.ruranobe.wicket.components.sidebar.SidebarModuleBase;
import ru.ruranobe.wicket.webpages.Text;
import ru.ruranobe.wicket.webpages.VolumePage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Samogot on 04.05.2015.
 */
public class SidebarLayoutPage extends BaseLayoutPage
{
    protected List<SidebarModuleBase> sidebarModules = new ArrayList<SidebarModuleBase>();
    protected WebMarkupContainer textPageUtils;
    protected BookmarkablePageLink homeTextLink = null;
    protected BookmarkablePageLink prevTextLink = null;
    protected BookmarkablePageLink nextTextLink = null;

    public SidebarLayoutPage()
    {
        add(new SidebarMiniSearch("miniSearch"));
        add(new ListView<SidebarModuleBase>("sidebarModuleRepeater", sidebarModules)
        {
            @Override
            protected void populateItem(ListItem<SidebarModuleBase> item)
            {
                item.add(item.getModelObject());
            }
        });
        add(textPageUtils = new WebMarkupContainer("textPageUtils"));
        textPageUtils.setVisible(false);
    }

    @Override
    protected void onInitialize()
    {
        if (homeTextLink == null)
        {
            textPageUtils.add(homeTextLink = new BookmarkablePageLink("homeTextLink", VolumePage.class));
            homeTextLink.add(new AttributeAppender("class", " disable"));
        }
        if (prevTextLink == null)
        {
            textPageUtils.add(prevTextLink = new BookmarkablePageLink("prevTextLink", Text.class));
            prevTextLink.add(new AttributeAppender("class", " disable"));
        }
        if (nextTextLink == null)
        {
            textPageUtils.add(nextTextLink = new BookmarkablePageLink("nextTextLink", Text.class));
            nextTextLink.add(new AttributeAppender("class", " disable"));
        }
        super.onInitialize();
    }
}
