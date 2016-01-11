package ru.ruranobe.wicket.webpages.admin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.extensions.markup.html.form.select.IOptionRenderer;
import org.apache.wicket.extensions.markup.html.form.select.Select;
import org.apache.wicket.extensions.markup.html.form.select.SelectOptions;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.handler.resource.ResourceRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ByteArrayResource;
import org.apache.wicket.request.resource.IResource;
import org.apache.wicket.util.string.Strings;
import org.json.JSONObject;
import ru.ruranobe.config.ApplicationContext;
import ru.ruranobe.engine.Webpage;
import ru.ruranobe.engine.image.ImageServices;
import ru.ruranobe.engine.image.RuraImage;
import ru.ruranobe.misc.RuranobeUtils;
import ru.ruranobe.mybatis.MybatisUtil;
import ru.ruranobe.mybatis.entities.tables.*;
import ru.ruranobe.mybatis.mappers.*;
import ru.ruranobe.mybatis.mappers.cacheable.CachingFacade;
import ru.ruranobe.wicket.RuraConstants;
import ru.ruranobe.wicket.components.admin.AdminAffixedListPanel;
import ru.ruranobe.wicket.components.admin.AdminInfoFormPanel;
import ru.ruranobe.wicket.webpages.base.AdminLayoutPage;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@AuthorizeInstantiation("ADMIN")
public class VolumeEdit extends AdminLayoutPage
{

    private final static IOptionRenderer<String> optionRenderer = new IOptionRenderer<String>()
    {
        @Override
        public String getDisplayValue(String object)
        {
            return RuraConstants.VOLUME_STATUS_TO_FULL_TEXT.get(object);
        }

        @Override
        public IModel<String> getModel(String value)
        {
            return Model.of(value);
        }
    };
    private final static Comparator<Chapter> chapterComparator = new Comparator<Chapter>()
    {
        @Override
        public int compare(Chapter c1, Chapter c2)
        {
            return ObjectUtils.compare(c1.getOrderNumber(), c2.getOrderNumber());
        }
    };
    private final Chapter stubСhapter;
    private final Map<String, Integer> memberNickToId = new HashMap<>();
    private Volume volume;
    private List<Project> projects;
    private List<VolumeReleaseActivity> volumeReleaseActivities;
    private List<VolumeActivity> activities;
    private List<TeamMember> teamMembers;
    private List<Chapter> chapters;
    private List<Chapter> allChapters = new ArrayList<>();
    private List<Update> updates;
    private List<ChapterImage> volumeImages;

    public VolumeEdit(final PageParameters parameters)
    {
        String projectName = parameters.get("project").toOptionalString();
        String volumeName = parameters.get("volume").toOptionalString();
        if (Strings.isEmpty(projectName) || Strings.isEmpty(volumeName))
        {
            throw RuranobeUtils.getRedirectTo404Exception(this);
        }

        String volumeUrl = projectName + "/" + volumeName;
        SqlSessionFactory sessionFactory = MybatisUtil.getSessionFactory();
        try (SqlSession session = sessionFactory.openSession())
        {
            VolumesMapper volumesMapperCacheable = CachingFacade.getCacheableMapper(session, VolumesMapper.class);
            volume = volumesMapperCacheable.getVolumeByUrl(volumeUrl);

            if (volume == null)
            {
                throw RuranobeUtils.getRedirectTo404Exception(this);
            }

            VolumeReleaseActivitiesMapper volumeReleaseActivitiesMapperCacheable = CachingFacade.getCacheableMapper(session, VolumeReleaseActivitiesMapper.class);
            volumeReleaseActivities = Lists.newArrayList(volumeReleaseActivitiesMapperCacheable.getVolumeReleaseActivitiesByVolumeId(volume.getVolumeId()));

            VolumeActivitiesMapper activitiesMapperCacheable = CachingFacade.getCacheableMapper(session, VolumeActivitiesMapper.class);
            activities = activitiesMapperCacheable.getAllVolumeActivities();

            TeamMembersMapper teamMembersMapperCacheable = CachingFacade.getCacheableMapper(session, TeamMembersMapper.class);
            teamMembers = teamMembersMapperCacheable.getAllTeamMembers();

            ChaptersMapper chaptersMapperCacheable = CachingFacade.getCacheableMapper(session, ChaptersMapper.class);
            chapters = chaptersMapperCacheable.getChaptersByVolumeId(volume.getVolumeId());

            UpdatesMapper updatesMapperCacheable = CachingFacade.getCacheableMapper(session, UpdatesMapper.class);
            updates = updatesMapperCacheable.getUpdatesByVolumeId(volume.getVolumeId());

            ProjectsMapper projectsMapperCacheable = CachingFacade.getCacheableMapper(session, ProjectsMapper.class);
            projects = Lists.newArrayList(projectsMapperCacheable.getAllProjects());

            ChapterImagesMapper chapterImagesMapperCacheable = CachingFacade.getCacheableMapper(session, ChapterImagesMapper.class);
            volumeImages = chapterImagesMapperCacheable.getChapterImagesByVolumeId(volume.getVolumeId());

            ExternalResourcesMapper externalResourcesMapperCacheable = CachingFacade.getCacheableMapper(session, ExternalResourcesMapper.class);
            if (volume.getImageOne() != null)
            {
                ExternalResource resource = externalResourcesMapperCacheable.getExternalResourceById(volume.getImageOne());
                volumeImages.add(0, new ChapterImage(null, -1, volume.getVolumeId(), null, resource, 1));
            }
            if (volume.getImageTwo() != null)
            {
                ExternalResource resource = externalResourcesMapperCacheable.getExternalResourceById(volume.getImageTwo());
                volumeImages.add(1, new ChapterImage(null, -1, volume.getVolumeId(), null, resource, 2));
            }
            if (volume.getImageThree() != null)
            {
                ExternalResource resource = externalResourcesMapperCacheable.getExternalResourceById(volume.getImageThree());
                volumeImages.add(2, new ChapterImage(null, -1, volume.getVolumeId(), null, resource, 3));
            }
            if (volume.getImageFour() != null)
            {
                ExternalResource resource = externalResourcesMapperCacheable.getExternalResourceById(volume.getImageFour());
                volumeImages.add(3, new ChapterImage(null, -1, volume.getVolumeId(), null, resource, 4));
            }

        }

        final Map<Integer, Chapter> chaptertIdToChapter = new HashMap<>();
        for (Chapter chapter : chapters)
        {
            chaptertIdToChapter.put(chapter.getChapterId(), chapter);
        }
        for (Update update : updates)
        {
            update.setChapter(chaptertIdToChapter.get(update.getChapterId()));
        }


        final Map<Integer, Project> projectIdToProject = new HashMap<>();
        for (Project project : projects)
        {
            projectIdToProject.put(project.getProjectId(), project);
        }
        volume.setProject(projectIdToProject.get(volume.getProjectId()));
        Collections.sort(projects, new Comparator<Project>()
        {
            @Override
            public int compare(Project p1, Project p2)
            {
                if (p1.getParentId() == null && p2.getParentId() == null)
                {
                    return ObjectUtils.compare(p1.getOrderNumber(), p2.getOrderNumber(), true);
                }
                else if (p1.getParentId() == null)
                {
                    int parentComp = ObjectUtils.compare(p1.getOrderNumber(), projectIdToProject.get(p2.getParentId()).getOrderNumber(), true);
                    return parentComp == 0 ? -1 : parentComp;
                }
                else if (p2.getParentId() == null)
                {
                    int parentComp = ObjectUtils.compare(projectIdToProject.get(p1.getParentId()).getOrderNumber(), p2.getOrderNumber(), true);
                    return parentComp == 0 ? 1 : parentComp;
                }
                else
                {
                    int parentComp = ObjectUtils.compare(projectIdToProject.get(p1.getParentId()).getOrderNumber(),
                            projectIdToProject.get(p2.getParentId()).getOrderNumber(), true);
                    return parentComp == 0 ? ObjectUtils.compare(p1.getOrderNumber(), p2.getOrderNumber(), true) : parentComp;
                }
            }
        });
        Map<Integer, VolumeActivity> activityIdToActivity = new HashMap<>();
        for (VolumeActivity activity : activities)
        {
            activityIdToActivity.put(activity.getActivityId(), activity);
        }
        for (VolumeReleaseActivity volumeReleaseActivity : volumeReleaseActivities)
        {
            volumeReleaseActivity.setActivity(activityIdToActivity.get(volumeReleaseActivity.getActivityId()));
        }
        for (TeamMember member : teamMembers)
        {
            memberNickToId.put(member.getNickname(), member.getMemberId());
        }

        stubСhapter = new Chapter();
        stubСhapter.setOrderNumber(-1);
        stubСhapter.setTitle("Весь том");
        Collections.sort(chapters, chapterComparator);
        reinitAllChapters();
//        final Dataset teamMembersDataset = new Dataset("teamMembers").withLocal(teamMembers).withValueKey("nickname");

        add(new BookmarkablePageLink("breadcrumbProject", ProjectEdit.class, volume.getProject().getUrlParameters())
                .setBody(Model.of(volume.getProject().getTitle())));
        add(new Label("breadcrumbActive", volume.getNameTitle()));

        add(new AdminInfoFormPanel<Volume>("info", "Информация", new CompoundPropertyModel<>(volume))
        {
            @Override
            public void onSubmit()
            {
                try (SqlSession session = MybatisUtil.getSessionFactory().openSession())
                {
                    VolumesMapper mapper = CachingFacade.getCacheableMapper(session, VolumesMapper.class);
                    mapper.updateVolume(volume);
                    session.commit();
                }
            }

            @Override
            protected Component getContentItemLabelComponent(String id, IModel<Volume> model)
            {
                return new Fragment(id, "volumeInfoFragment", VolumeEdit.this, model)
                {
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();

                        add(new TextField<String>("urlPart").setRequired(true).setLabel(Model.of("Ссылка")));
                        add(new TextField<String>("nameFile").setRequired(true).setLabel(Model.of("Имя для файлов")));
                        add(new TextField<String>("nameTitle").setRequired(true).setLabel(Model.of("Заголовок")));
                        add(new TextField<String>("nameJp"));
                        add(new TextField<String>("nameEn"));
                        add(new TextField<String>("nameRu"));
                        add(new TextField<String>("nameRomaji"));
                        add(new TextField<String>("nameShort"));
                        add(new DropDownChoice<>("project", projects).setChoiceRenderer(new ChoiceRenderer<Project>("title", "projectId"))
                                                                     .setOutputMarkupId(true));
                        add(new TextField<Float>("sequenceNumber"));
                        add(new TextField<String>("author"));
                        add(new TextField<String>("illustrator"));
                        add(new TextField<String>("originalDesign"));
                        add(new TextField<String>("originalStory"));
                        add(new DateTextField("releaseDate", "dd.MM.yyyy"));
                        add(new TextField<String>("isbn"));
                        add(new DropDownChoice<>("volumeType", RuraConstants.VOLUME_TYPE_LIST));
                        add(new Select<String>("volumeStatus")
                                .add(new SelectOptions<>("basic", RuraConstants.VOLUME_STATUS_BASIC_LIST, optionRenderer))
                                .add(new SelectOptions<>("external", RuraConstants.VOLUME_STATUS_EXTERNAL_LIST, optionRenderer))
                                .add(new SelectOptions<>("not_in_work", RuraConstants.VOLUME_STATUS_IN_WORK_LIST, optionRenderer))
                                .add(new SelectOptions<>("in_work", RuraConstants.VOLUME_STATUS_NOT_IN_WORK_LIST, optionRenderer))
                                .add(new SelectOptions<>("published", RuraConstants.VOLUME_STATUS_PUBLISHED_LIST, optionRenderer)));
                        add(new TextField<String>("externalUrl"));
                        add(new TextArea<String>("annotation"));
                        add(new CheckBox("adult"));
                        add(new NumberTextField<Integer>("topicId").setMinimum(1).add(new AttributeModifier(
                                "data-forum-id", volume.getProject().getForumId())));
                    }
                };
            }
        });

        add(new AdminAffixedListPanel<VolumeReleaseActivity>("staff", "Этапы работы", new ListModel<>(volumeReleaseActivities))
        {
            @Override
            public void onSubmit()
            {
                try (SqlSession session = MybatisUtil.getSessionFactory().openSession())
                {
                    VolumeReleaseActivitiesMapper mapper = CachingFacade.getCacheableMapper(session, VolumeReleaseActivitiesMapper.class);
                    for (VolumeReleaseActivity item : model.getObject())
                    {
                        if (!removed.contains(item))
                        {
                            if (item.getActivityId() != null)
                            {
                                mapper.updateVolumeReleaseActivity(item);
                            }
                            else
                            {
                                mapper.insertVolumeReleaseActivity(item);
                            }
                        }
                    }
                    for (VolumeReleaseActivity removedItem : removed)
                    {
                        if (removedItem.getActivityId() != null)
                        {
                            mapper.deleteVolumeReleaseActivity(removedItem.getActivityId());
                        }
                    }
                    session.commit();
                }
            }

            @Override
            protected VolumeReleaseActivity makeItem()
            {
                return new VolumeReleaseActivity();
            }

            @Override
            protected Component getSelectorItemLabelComponent(String id, IModel<VolumeReleaseActivity> model)
            {
                return new Label(id, new PropertyModel<VolumeReleaseActivity>(model, "title"));
            }

            @Override
            protected Component getFormItemLabelComponent(String id, IModel<VolumeReleaseActivity> model)
            {
                return new Fragment(id, "staffFormItemFragment", VolumeEdit.this, model)
                {
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();
                        add(new TextField<String>("memberName").setRequired(true).setLabel(Model.of("Участник")).setOutputMarkupId(true));
                        add(new DropDownChoice<>("activity", activities)
                                .setChoiceRenderer(new ChoiceRenderer<VolumeActivity>("activityName", "activityId")));
                        add(new CheckBox("teamHidden"));
                    }
                };
            }
        }.setSortable(true));

        add(new AdminAffixedListPanel<Chapter>("chapters", "Главы", new ListModel<>(chapters))
        {
            @Override
            public void onSubmit()
            {
                try (SqlSession session = MybatisUtil.getSessionFactory().openSession())
                {
                    ChaptersMapper mapper = CachingFacade.getCacheableMapper(session, ChaptersMapper.class);
                    for (Chapter item : model.getObject())
                    {
                        if (!removed.contains(item))
                        {
                            if (item.getChapterId() != null)
                            {
                                mapper.updateChapter(item);
                            }
                            else
                            {
                                mapper.insertChapter(item);
                            }
                        }
                    }
                    for (Chapter removedItem : removed)
                    {
                        if (removedItem.getChapterId() != null)
                        {
                            mapper.deleteChapter(removedItem.getChapterId());
                        }
                    }
                    session.commit();
                }
            }

            @Override
            protected Chapter makeItem()
            {
                Chapter new_chapter = new Chapter();
                new_chapter.setVolumeId(volume.getVolumeId());
                new_chapter.setUrl(volume.getUrl() + "/");
                return new_chapter;
            }

            @Override
            protected Component getSelectorItemLabelComponent(String id, IModel<Chapter> model)
            {
                return new Label(id, new PropertyModel<VolumeReleaseActivity>(model, "title"))
                        .add(new AttributeAppender("class", model.getObject().isNested() ? " sub-chapter" : ""));
            }

            @Override
            protected Component getFormItemLabelComponent(String id, final IModel<Chapter> model)
            {
                return new Fragment(id, "chapterFormItemFragment", VolumeEdit.this, model)
                {
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();
                        add(new HiddenField<Integer>("chapterId"));
                        add(new CheckBox("nested"));
                        add(new TextField<String>("urlPart").setRequired(true).setLabel(Model.of("Ссылка")));
                        add(new TextField<String>("title").setRequired(true).setLabel(Model.of("Заголовок")));
                        add(new BookmarkablePageLink("link", Editor.class, model.getObject().getUrlParameters()));
                    }
                };
            }
        }.setSortable(true));

        add(new AdminAffixedListPanel<Update>("updates", "Обновления", new ListModel<>(updates))
        {
            @Override
            public void onSubmit()
            {
                try (SqlSession session = MybatisUtil.getSessionFactory().openSession())
                {
                    UpdatesMapper mapper = CachingFacade.getCacheableMapper(session, UpdatesMapper.class);
                    for (Update item : model.getObject())
                    {
                        if (!removed.contains(item))
                        {
                            if (item.getUpdateId() != null)
                            {
                                mapper.updateUpdate(item);
                            }
                            else
                            {
                                mapper.insertUpdate(item);
                            }
                        }
                    }
                    for (Update removedItem : removed)
                    {
                        if (removedItem.getUpdateId() != null)
                        {
                            mapper.deleteUpdate(removedItem.getUpdateId());
                        }
                    }
                    session.commit();
                }
            }

            @Override
            protected Update makeItem()
            {
                Update new_update = new Update();
                new_update.setShowTime(new Date());
                new_update.setUpdateType(RuraConstants.UPDATE_TYPE_PUBLISH);
                return new_update;
            }

            @Override
            protected Component getSelectorItemLabelComponent(String id, IModel<Update> model)
            {
                return new Label(id, new PropertyModel<VolumeReleaseActivity>(model, "title"));
            }

            @Override
            protected Component getFormItemLabelComponent(String id, IModel<Update> model)
            {
                return new Fragment(id, "updateFormItemFragment", VolumeEdit.this, model)
                {
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();
                        add(new DropDownChoice<>("updateType", RuraConstants.UPDATE_TYPE_LIST));
                        add(new DropDownChoice<>("chapter", allChapters)
                                .setChoiceRenderer(new ChoiceRenderer<Chapter>("leveledTitle", "chapterId")));
                        add(new DateTextField("showTime", "dd.MM.yyyy HH:mm"));
                        add(new TextField<String>("description"));
                    }
                };
            }
        });

        add(new AdminAffixedListPanel<ChapterImage>("images", "Изображения", new ListModel<>(volumeImages))
        {
            @Override
            public void onSubmit()
            {
                try (SqlSession session = MybatisUtil.getSessionFactory().openSession())
                {
                    ChapterImagesMapper mapper = CachingFacade.getCacheableMapper(session, ChapterImagesMapper.class);
                    boolean coversEdit = false;
                    for (ChapterImage item : model.getObject())
                    {
                        if (!removed.contains(item))
                        {
                            if (item.getChapterId() == -1)
                            {
                                switch (item.getOrderNumber())
                                {
                                    case 1:
                                        coversEdit = true;
                                        volume.setImageOne(item.getNonColoredImage().getResourceId());
                                        break;
                                    case 2:
                                        coversEdit = true;
                                        volume.setImageTwo(item.getNonColoredImage().getResourceId());
                                        break;
                                    case 3:
                                        coversEdit = true;
                                        volume.setImageThree(item.getNonColoredImage().getResourceId());
                                        break;
                                    case 4:
                                        coversEdit = true;
                                        volume.setImageFour(item.getNonColoredImage().getResourceId());
                                        break;
                                    default:
                                        //todo invalid
                                }
                            }
                            else
                            {
                                if (item.getChapterImageId() != null)
                                {
                                    mapper.updateChapterImage(item);
                                }
                                else
                                {
                                    mapper.insertChapterImage(item);
                                }
                            }
                        }
                    }
                    if (coversEdit)
                    {
                        VolumesMapper volumesMapper = CachingFacade.getCacheableMapper(session, VolumesMapper.class);
                        volumesMapper.updateVolumeCovers(volume);
                    }
                    for (ChapterImage removedItem : removed)
                    {
                        if (removedItem.getChapterImageId() != null)
                        {
                            mapper.deleteChapterImage(removedItem.getChapterImageId());
                        }
                    }
                    session.commit();
                }
            }

            @Override
            protected ChapterImage makeItem()
            {
                ChapterImage chapterImage = new ChapterImage();
                chapterImage.setVolumeId(volume.getVolumeId());
                return chapterImage;
            }

            @Override
            protected void initializeSelectorBlockListItem(ListItem<ChapterImage> item)
            {
                super.initializeSelectorBlockListItem(item);
                ChapterImage model = item.getModelObject();
                item.add(new AttributeModifier("data-chapter-id", model.getChapterId()));
                item.add(new AttributeModifier("data-order-number", model.getOrderNumber()));
                item.add(new AttributeModifier("data-internal-index", item.getIndex()));
            }

            @Override
            protected Component getSelectorItemLabelComponent(String id, final IModel<ChapterImage> model)
            {
                return new Fragment(id, "imageSelectorItemFragment", VolumeEdit.this, model)
                {
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();
                        add(new WebMarkupContainer("nonColoredImage.url")
                        {
                            @Override
                            protected void onComponentTag(ComponentTag tag)
                            {
                                tag.getAttributes().put("src", getDefaultModelObjectAsString());
                            }
                        });
                        add(new Label("nonColoredImage.title"));
                    }
                };
            }


            @Override
            protected Component getFormItemLabelComponent(String id, final IModel<ChapterImage> model)
            {
                return new Fragment(id, "imageFormItemFragment", VolumeEdit.this, model)
                {
                    @Override
                    protected void onInitialize()
                    {
                        super.onInitialize();
                        WebMarkupContainer coloredImageTrigger, coloredImageAddButton;
                        add(new HiddenField<Integer>("chapterId"));
                        add(new WebMarkupContainer("nonColoredImage.url")
                        {
                            @Override
                            protected void onComponentTag(ComponentTag tag)
                            {
                                tag.getAttributes().put("src", getDefaultModelObjectAsString());
                            }
                        });
                        add(new TextField<String>("nonColoredImage.title"));
                        add(new DateTextField("nonColoredImage.uploadedWhen"));
                        add(coloredImageTrigger = new WebMarkupContainer("coloredImageTrigger")
                        {
                            @Override
                            protected void onComponentTag(ComponentTag tag)
                            {
                                if (model.getObject().getColoredImage() == null)
                                {
                                    tag.getAttributes().put("style", "display:none");
                                }
                            }
                        });
                        add(coloredImageAddButton = new WebMarkupContainer("coloredImageAddButton")
                        {
                            @Override
                            protected void onComponentTag(ComponentTag tag)
                            {
                                if (model.getObject().getColoredImage() == null)
                                {
                                    tag.getAttributes().put("title", "Добавить изображение");
                                }
                            }
                        });
                        coloredImageAddButton.add(new WebMarkupContainer("coloredImageAddButtonIcon")
                        {
                            @Override
                            protected void onComponentTag(ComponentTag tag)
                            {
                                if (model.getObject().getColoredImage() == null)
                                {
                                    tag.getAttributes().put("class", "fa fa-plus");
                                }
                            }
                        });
                        coloredImageTrigger.add(new WebMarkupContainer("coloredImage.url")
                        {
                            @Override
                            protected void onComponentTag(ComponentTag tag)
                            {
                                tag.getAttributes().put("src", getDefaultModelObjectAsString());
                            }
                        });
                        coloredImageTrigger.add(new TextField<String>("coloredImage.title"));
                        coloredImageTrigger.add(new DateTextField("coloredImage.uploadedWhen"));
                    }
                };
            }

            @Override
            protected void onInitialize()
            {
                super.onInitialize();
                toolbarButtons.add(0, new WebMarkupContainer("button")
                {
                    @Override
                    public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag)
                    {
                        replaceComponentTagBody(markupStream, openTag, "<i class=\"fa fa-plus\"></i><input type=\"file\" class=\"fileupload\" multiple=\"\">");
                    }
                }.add(new AttributeAppender("class", Model.of("btn-success"), " "), new AttributeModifier("title", "Загрузить"))
                 .setMarkupId("btn-image-add"));

                toolbarButtons.get(1).add(new AttributeModifier("style", "display:none"));

                add(new AbstractAjaxBehavior()
                {
                    @Override
                    public void onRequest()
                    {
                        HttpServletRequest request = (HttpServletRequest) getRequest().getContainerRequest();
                        processUpload(request);
                    }


                    @Override
                    protected void onComponentTag(ComponentTag tag)
                    {
                        tag.getAttributes().put("data-add-url", toolbarButtons.get(1).getBehaviors(AbstractAjaxBehavior.class).get(0).getCallbackUrl());
                        tag.getAttributes().put("data-upload-url", getCallbackUrl());
                    }
                });
                add(new AbstractDefaultAjaxBehavior()
                {
                    @Override
                    protected void respond(AjaxRequestTarget target)
                    {
                        String index = getRequest().getPostParameters().getParameterValue("index").toString();
                        if (!Strings.isEmpty(index))
                        {
                            Component formBlockItem = formBlockItemRepeater.get(index);
                            Component selectorBlockItem = selectorBlockItemRepeater.get(index);
                            target.add(formBlockItem);
                            target.add(selectorBlockItem);
                            target.appendJavaScript(String.format(
                                    "$('#%s .list-group.select.sortable').trigger('sortupdate');" +
                                    "$('#%s').click();" +
                                    "initFormItemFileUpload('#%s .image-data-main');" +
                                    "initFormItemFileUpload('#%s .image-data-color');",
                                    form.getMarkupId(), selectorBlockItem.getMarkupId(),
                                    formBlockItem.getMarkupId(), formBlockItem.getMarkupId()));
                        }
                    }

                    @Override
                    protected void onComponentTag(ComponentTag tag)
                    {
                        tag.getAttributes().put("data-reload-url", getCallbackUrl());
                    }
                });

            }
        }.setSortable(true));
    }

    private void processUpload(HttpServletRequest request)
    {
        if (ServletFileUpload.isMultipartContent(request))
        {
            JSONObject responseString = new JSONObject();
            try
            {
                File imageTempFile;
                try
                {
                    imageTempFile = File.createTempFile("ruranobe-image_temp", ".tmp");
                }
                catch (IOException ex)
                {
                    throw new RuntimeException("Unable to create temp file during image upload", ex);
                }

                FileItemFactory factory = new DiskFileItemFactory();
                ServletFileUpload upload = new ServletFileUpload(factory);
                String uploadingFileExtension = null;
                String filename = null;
                HashMap<String, String> multipartParameters = new HashMap<>();
                try
                {
                    List<FileItem> items = upload.parseRequest(request);
                    for (FileItem item : items)
                    {
                        if (item.getName() == null)
                        {
                            multipartParameters.put(item.getFieldName(), item.getString());
                        }
                        else
                        {
                            filename = item.getName();
                            uploadingFileExtension = FilenameUtils.getExtension(filename);
                            item.write(imageTempFile);
                        }
                    }
                }
                catch (Exception ex)
                {
                    throw new RuntimeException("Unable to write uploading image to temp file", ex);
                }
                ChapterImage chapterImage = volumeImages.get(Integer.parseInt(multipartParameters.get("index")));
                if (chapterImage == null)
                {
                    throw new IllegalArgumentException("Bad index " + multipartParameters.get("index"));
                }

                ExternalResourceHistory externalResourceHistory = new ExternalResourceHistory();
                externalResourceHistory.setChapterImage(chapterImage);
                externalResourceHistory.setVolumeId(volume.getVolumeId());
                externalResourceHistory.setProjectId(volume.getProjectId());
                externalResourceHistory.setColoredType(multipartParameters.get("ctype"));

                ApplicationContext context = RuranobeUtils.getApplicationContext();
                Webpage webpage = context.getWebpageByPageClass(this.getPage().getClass().getName());
                RuraImage image = new RuraImage(imageTempFile, uploadingFileExtension, filename);
                List<ExternalResource> externalResources = ImageServices.uploadImage(image, webpage.getImageStorages(),
                        externalResourceHistory, new ImmutableMap.Builder<String, String>()
                                .put("project", volume.getUrl().split("/", -1)[0])
                                .put("volume", volume.getUrl().split("/", -1)[1])
                                .build());
                ExternalResource externalResource = externalResources.iterator().next();
                if (Strings.isEqual(multipartParameters.get("ctype"), "main"))
                {
                    chapterImage.setNonColoredImage(externalResource);
                }
                else if (Strings.isEqual(multipartParameters.get("ctype"), "color"))
                {
                    chapterImage.setColoredImage(externalResource);
                }
                else
                {
                    throw new IllegalArgumentException(
                            String.format("Unknown value %s of ctype parameter", multipartParameters.get("ctype")));
                }
                imageTempFile.delete();

                responseString.put("index", multipartParameters.get("index"));
            }
            catch (Exception ex)
            {
                responseString.put("error", ex.toString());
            }

            IResource jsonResource = new ByteArrayResource("text/plain", responseString.toString().getBytes());
            IRequestHandler requestHandler = new ResourceRequestHandler(jsonResource, null);
            requestHandler.respond(getRequestCycle());
        }
    }

    private void reinitAllChapters()
    {
        allChapters.clear();
        allChapters.addAll(chapters);
        allChapters.add(stubСhapter);
        Collections.sort(allChapters, chapterComparator);
    }
}