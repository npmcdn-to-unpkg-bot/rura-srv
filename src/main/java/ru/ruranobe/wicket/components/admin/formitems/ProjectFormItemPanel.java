package ru.ruranobe.wicket.components.admin.formitems;

import com.google.common.collect.ImmutableMap;
import com.rometools.utils.Strings;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import ru.ruranobe.mybatis.entities.tables.Project;
import ru.ruranobe.wicket.components.admin.BannerUploadComponent;
import ru.ruranobe.wicket.webpages.ProjectEdit;


/**
 * Created by samogot on 27.08.15.
 */
public class ProjectFormItemPanel extends Panel
{
    public ProjectFormItemPanel(String id, IModel<Project> model)
    {
        super(id, model);
        add(new TextField<String>("url").setRequired(true).setLabel(Model.of("Ссылка")));
        add(new TextField<String>("title").setRequired(true).setLabel(Model.of("Заголовок")));
        add(new CheckBox("projectHidden"));
        add(new CheckBox("bannerHidden"));
        ImmutableMap<String, String> contextVariables = null;
        if (!Strings.isEmpty(model.getObject().getUrl()))
        {
            contextVariables = new ImmutableMap.Builder<String, String>()
                    .put("project", model.getObject().getUrl())
                    .build();
        }
        add(new BannerUploadComponent("image").setContextVariables(contextVariables).setVisible(contextVariables != null));
        add(new BookmarkablePageLink("link", ProjectEdit.class, model.getObject().getUrlParameters()));
    }
}
