package ru.ruranobe.wicket.components.admin;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.list.PropertyListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * Created by samogot on 27.08.15.
 */
public abstract class AdminTableListPanel<T> extends AdminListPanel<T>
{

    protected abstract Component getRowComponent(String id, IModel<T> model);

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        form.add(rowRepeater = new PropertyListView<T>("rowRepeater", model)
        {
            @Override
            protected void populateItem(ListItem<T> item)
            {
                initializeRowListItem(item);
            }
        });

        form.add(new ListView<String>("headerRepeater", headersList)
        {
            @Override
            protected void populateItem(ListItem<String> item)
            {
                item.add(new AttributeModifier("class", "column-number-" + item.getIndex()));
                item.add(new Label("label", item.getModelObject()));
            }
        });
    }

    @Override
    protected void onAddItem(T newItem, AjaxRequestTarget target, Form form)
    {
        ListItem<T> rowListItem = new ListItem<T>(rowRepeater.size(), new CompoundPropertyModel<T>(newItem));
        initializeRowListItem(rowListItem);
        rowRepeater.add(rowListItem);
        target.prependJavaScript(String.format(";addAdminTableRowStub('%s', '%s');", rowListItem.get("item").getMarkupId(), form.getMarkupId()));
        target.add(rowListItem.get("item"));
        target.appendJavaScript(String.format(";$('#%s').click();", rowListItem.get("item").getMarkupId()));
    }

    @Override
    protected void onRemoveItem(T removedItem, AjaxRequestTarget target, Form form)
    {
        target.appendJavaScript(String.format(";removeAdminTableRow('%s');", form.getMarkupId()));
        rowRepeater.get(model.getObject().indexOf(removedItem)).setVisible(false);
    }

    private void initializeRowListItem(final ListItem<T> item)
    {
        Component rowComponent = getRowComponent("item", item.getModel());
        rowComponent.setOutputMarkupId(true);
        rowComponent.add(new AjaxEventBehavior("click")
        {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
            {
                super.updateAjaxAttributes(attributes);
                attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
            }

            @Override
            protected void onEvent(AjaxRequestTarget target)
            {
                selectedItem = item.getModelObject();
            }
        });
        item.add(rowComponent);
    }

    public AdminTableListPanel(String id, String title, IModel<? extends List<T>> model, List<String> headersList)
    {
        super(id, title, model);
        this.headersList = headersList;
        toolbarButtons.add(toolbarButtons.size() - 1, new AdminToolboxColumnsFilterButton("button", Model.ofList(headersList)));
    }

    private List<String> headersList;
    private PropertyListView<T> rowRepeater;
}