package ru.ruranobe.wicket.webpages;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.util.*;

import javafx.util.Pair;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.link.StatelessLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.flow.RedirectToUrlException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import ru.ruranobe.mybatis.MybatisUtil;
import ru.ruranobe.mybatis.mappers.ExternalResourcesMapper;
import ru.ruranobe.mybatis.mappers.ProjectsMapper;
import ru.ruranobe.mybatis.mappers.UpdatesMapper;
import ru.ruranobe.mybatis.mappers.VolumesMapper;
import ru.ruranobe.mybatis.mappers.cacheable.CachingFacade;
import ru.ruranobe.mybatis.tables.ExternalResource;
import ru.ruranobe.mybatis.tables.Update;
import ru.ruranobe.mybatis.tables.Volume;
import ru.ruranobe.wicket.RuraConstants;
import ru.ruranobe.wicket.components.ProjectsSidebarModule;
import ru.ruranobe.wicket.webpages.base.RuraHeaderAndFooter;

public class Project extends RuraHeaderAndFooter
{
    public Project(PageParameters parameters)
    {
        if (parameters.getNamedKeys().size() != 1)
        {
            throw REDIRECT_TO_404;
        }

        String projectUrl = parameters.getNamedKeys().iterator().next();
        SqlSessionFactory sessionFactory = MybatisUtil.getSessionFactory();
        SqlSession session = sessionFactory.openSession();

        ProjectsMapper projectsMapperCacheable = CachingFacade.getCacheableMapper(session, ProjectsMapper.class);

        final ru.ruranobe.mybatis.tables.Project mainProject = projectsMapperCacheable.getProjectByUrl(projectUrl);

        if (mainProject == null)
        {
            throw REDIRECT_TO_404;
        }
        Collection<ru.ruranobe.mybatis.tables.Project> subProjects = projectsMapperCacheable.getSubProjectsByParentProjectId(mainProject.getProjectId());
        final ArrayList<ru.ruranobe.mybatis.tables.Project> projects = new ArrayList<ru.ruranobe.mybatis.tables.Project>();
        projects.add(mainProject);
        projects.addAll(subProjects);


        VolumesMapper volumesMapperCacheable = CachingFacade.getCacheableMapper(session, VolumesMapper.class);
        //ProjectInfo projectInfo = volumesMapperCacheable.getInfoByProjectId(mainProject.getProjectId());

        Label projectTitle = new Label("projectTitle", mainProject.getTitle());
        add(projectTitle);

        final ExternalResourcesMapper externalResourcesMapperCacheable = CachingFacade.
                getCacheableMapper(session, ExternalResourcesMapper.class);
        /*ExternalResource imageResource = externalResourcesMapperCacheable.getExternalResourceById(mainProject.getImageId());
        Image projectMainImage = (imageResource == null) ? (new Image("projectMainImage", new PackageResourceReference(HomePage.class, "undefined.png")))
                                                         : (new Image("projectMainImage", imageResource.getUrl()));
        add(projectMainImage);*/

        Label projectName = new Label("projectName", mainProject.getTitle());
        add(projectName);

        Label projectAuthor = new Label("projectAuthor", mainProject.getAuthor());
        add(projectAuthor);

        Label projectIllustrator = new Label("projectIllustrator", mainProject.getIllustrator());
        add(projectIllustrator);

        Label projectFranchise = new Label("projectFranchise", mainProject.getFranchise());
        projectFranchise.setEscapeModelStrings(false);
        add(projectFranchise);

        Label projectAnnotation = new Label("projectAnnotation", mainProject.getAnnotation());
        add(projectAnnotation);


        ArrayList<Volume> mainProjectFirstCovers = new ArrayList<Volume>();
        ArrayList<Volume> mainProjectActiveCovers = new ArrayList<Volume>();
        ArrayList<Volume> firstCovers = new ArrayList<Volume>();
        ArrayList<Volume> activeCovers = new ArrayList<Volume>();
        Volume mainProjectLastCover = null;
        Volume lastCover = null;

        final Map<Pair<String, String>, Integer> shortNameLabelWidthMap = new HashMap<Pair<String, String>, Integer>();
        final Map<String, ArrayList<Pair<String, ArrayList<Volume>>>> volumeTypeToSubProjectToVolumes = new HashMap<String, ArrayList<Pair<String, ArrayList<Volume>>>>();
        for (ru.ruranobe.mybatis.tables.Project project : projects)
        {
            String subProjectTitle = project.getTitle();
            if (project == mainProject) subProjectTitle = "";
            Collection<Volume> volumes = volumesMapperCacheable.getVolumesByProjectId(project.getProjectId());
            for (Volume volume : volumes)
            {
                String type = volume.getVolumeType();
                if (!type.isEmpty())
                {
                    if (volume.getImageOne() != null)
                    {
                        if (subProjectTitle.isEmpty() && type.equals(DISPLAYABLE_NAMES.get(0)) && mainProjectFirstCovers.size() < 3)
                            mainProjectFirstCovers.add(volume);
                        else if (mainProjectFirstCovers.isEmpty() && firstCovers.size() < 3)
                            firstCovers.add(volume);

                        if (volume.getVolumeStatus().equals(RuraConstants.VOLUME_STATUS_ONGOING)
                            || volume.getVolumeStatus().equals(RuraConstants.VOLUME_STATUS_TRANSLATING)
                            || volume.getVolumeStatus().equals(RuraConstants.VOLUME_STATUS_PROOFREAD))
                        {
                            if (subProjectTitle.isEmpty() && type.equals(DISPLAYABLE_NAMES.get(0)) && mainProjectActiveCovers.size() < 3)
                                mainProjectActiveCovers.add(volume);
                            else if (mainProjectActiveCovers.isEmpty() && activeCovers.size() < 3)
                                activeCovers.add(volume);
                        }

                        if (subProjectTitle.isEmpty() && type.equals(DISPLAYABLE_NAMES.get(0))
                            && (mainProjectLastCover == null || mainProjectLastCover.getProjectId() < volume.getProjectId()))
                            mainProjectLastCover = volume;
                        if (lastCover == null || lastCover.getProjectId() < volume.getProjectId())
                            lastCover = volume;
                    }

                    if (volumeTypeToSubProjectToVolumes.get(type) == null)
                    {
                        volumeTypeToSubProjectToVolumes.put(type, new ArrayList<Pair<String, ArrayList<Volume>>>());
                    }
                    ArrayList<Pair<String, ArrayList<Volume>>> subProjectToVolumes = volumeTypeToSubProjectToVolumes.get(type);
                    if (subProjectToVolumes.isEmpty() || !subProjectToVolumes.get(subProjectToVolumes.size() - 1).getKey().equals(subProjectTitle))
                    {
                        subProjectToVolumes.add(new Pair<String, ArrayList<Volume>>(subProjectTitle, new ArrayList<Volume>()));
                    }
                    subProjectToVolumes.get(subProjectToVolumes.size() - 1).getValue().add(volume);

                    Pair<String, String> volumeTypeAndSubProject = new Pair<String, String>(type, subProjectTitle);
                    if (volume.getNameShort() != null)
                    {
                        if (shortNameLabelWidthMap.get(volumeTypeAndSubProject) == null
                            || shortNameLabelWidthMap.get(volumeTypeAndSubProject) < volume.getNameShort().length())
                        {
                            shortNameLabelWidthMap.put(volumeTypeAndSubProject, volume.getNameShort().length());
                        }
                    }
                }
            }
        }

        ListView<String> volumeTypeRepeater = new ListView<String>("volumeTypeRepeater", DISPLAYABLE_NAMES)
        {
            @Override
            protected void populateItem(final ListItem<String> listItem1)
            {
                final String displayableName = listItem1.getModelObject();
                Label volumeType = new Label("volumeType", displayableName);
                if (volumeTypeToSubProjectToVolumes.get(displayableName) == null) volumeType.setVisible(false);
                listItem1.add(volumeType);
                ListView<Pair<String, ArrayList<Volume>>> volumeSubProjectRepeater
                        = new ListView<Pair<String, ArrayList<Volume>>>("volumeSubProjectRepeater", volumeTypeToSubProjectToVolumes.get(displayableName))
                {
                    @Override
                    protected void populateItem(ListItem<Pair<String, ArrayList<Volume>>> listItem2)
                    {
                        Pair<String, ArrayList<Volume>> projectTitleAndVolumes = listItem2.getModelObject();
                        String subProjectNameString = projectTitleAndVolumes.getKey();
                        Label projectName = new Label("projectName", subProjectNameString);
                        if (subProjectNameString.isEmpty()) projectName.setVisible(false);
                        listItem2.add(projectName);
                        ArrayList<Volume> volumes = projectTitleAndVolumes.getValue();
                        Collections.sort(volumes, COMPARATOR);
                        final Integer shortNameLabelWidth = shortNameLabelWidthMap.get(new Pair<String, String>(displayableName, subProjectNameString));
                        ListView<Volume> volumeRepeater = new ListView<Volume>("volumeRepeater", projectTitleAndVolumes.getValue())
                        {
                            @Override
                            protected void populateItem(final ListItem<Volume> listItem3)
                            {
                                Volume volume = listItem3.getModelObject();
                                String nameShort = volume.getNameShort();
                                Label volumeName = new Label("volumeName", nameShort);
                                if (shortNameLabelWidth != null)
                                    volumeName.add(new AttributeModifier("style", "width:" + (shortNameLabelWidth * 7.5 + 10) + "px;"));
                                listItem3.add(volumeName);
                                String volumeStatusStr = volume.getVolumeStatus();
                                Label volumeStatus = new Label("volumeStatus", VOLUME_STATUS_LABEL_TEXT.get(volumeStatusStr));
                                volumeStatus.add(new AttributeAppender("class", " label-" + VOLUME_STATUS_LABEL_COLOR_CLASS.get(volumeStatusStr)));
                                listItem3.add(volumeStatus);
                                if (volumeStatusStr.equals(RuraConstants.VOLUME_STATUS_HIDDEN))
                                    listItem3.setVisible(false); //todo show grayed if allowed to user
                                ExternalLink volumeLink = new ExternalLink("volumeLink", "/release/" + volume.getUrl(), volume.getNameTitle());
                                listItem3.add(volumeLink);
                            }
                        };
                        listItem2.add(volumeRepeater);
                    }
                };
                listItem1.add(volumeSubProjectRepeater);
            }
        };
        add(volumeTypeRepeater);

        Set<Volume> allCoversSet = new HashSet<Volume>();
        if (mainProjectFirstCovers.isEmpty())
            allCoversSet.addAll(firstCovers);
        else allCoversSet.addAll(mainProjectFirstCovers);
        if (mainProjectActiveCovers.isEmpty())
            allCoversSet.addAll(activeCovers);
        else allCoversSet.addAll(mainProjectActiveCovers);
        if (mainProjectLastCover != null)
            allCoversSet.add(mainProjectLastCover);
        allCoversSet.add(lastCover);
        ArrayList<Volume> allCovers = new ArrayList<Volume>(allCoversSet);
        Collections.sort(allCovers, COMPARATOR);
        ArrayList<Pair<String, String>> allCoverIds = new ArrayList<Pair<String, String>>();
        for (Volume volume : allCovers)
        {
            allCoverIds.add(new Pair<String, String>(volume.getNameTitle(),
                    externalResourcesMapperCacheable.getExternalResourceById(volume.getImageOne()).getUrl()));
        }

        ListView<Pair<String, String>> projectCoverCarouselIndicators = new ListView<Pair<String, String>>("projectCoverCarouselIndicators", allCoverIds)
        {
            @Override
            protected void populateItem(ListItem<Pair<String, String>> item)
            {
                WebMarkupContainer slideIndicator = new WebMarkupContainer("slideIndicator");
                slideIndicator.add(new AttributeModifier("data-slide-to", item.getIndex()));
                slideIndicator.add(new AttributeModifier("title", item.getModelObject().getKey()));
                if (item.getIndex() == 0) slideIndicator.add(new AttributeAppender("class", " active"));
                item.add(slideIndicator);
            }
        };
        add(projectCoverCarouselIndicators);

        ListView<Pair<String, String>> projectCoverCarouselSlides = new ListView<Pair<String, String>>("projectCoverCarouselSlides", allCoverIds)
        {
            @Override
            protected void populateItem(ListItem<Pair<String, String>> item)
            {
                WebMarkupContainer slideContainer = new WebMarkupContainer("slideContainer");
                if (item.getIndex() == 0) slideContainer.add(new AttributeAppender("class", " active"));
                item.add(slideContainer);
                WebMarkupContainer slideImage = new WebMarkupContainer("slideImage");
                slideImage.add(new AttributeModifier("src", item.getModelObject().getValue()));
                slideImage.add(new AttributeModifier("alt", item.getModelObject().getKey()));
                slideContainer.add(slideImage);
            }
        };
        add(projectCoverCarouselSlides);

        Collection<ru.ruranobe.mybatis.tables.Project> allProjects = projectsMapperCacheable.getAllProjects();
        List<ProjectExtended> projectsList = new ArrayList<ProjectExtended>();

        for (ru.ruranobe.mybatis.tables.Project project : allProjects)
        {
            if (!project.isProjectHidden())
            {
                ExternalResource image = (project.getImageId() != null) ? externalResourcesMapperCacheable.getExternalResourceById(project.getImageId())
                                                                        : null;
                projectsList.add(new ProjectExtended(project, image));
            }
        }

        add(new ProjectsSidebarModule("projectsModule"));

        UpdatesMapper updatesMapperCacheable = session.getMapper(UpdatesMapper.class);
        List<Update> updates = updatesMapperCacheable.
                getLastUpdatesBy(mainProject.getProjectId(), null, null, 0, UPDATES_BY_PROJECT_ON_PAGE);

        ListView<Update> updatesView = new ListView<Update>("updatesView", updates)
        {
            @Override
            protected void populateItem(ListItem<Update> listItem)
            {
                Update update = listItem.getModelObject();
                String iconDivClassValue = UPDATE_TYPE_TO_ICON_DIV_CLASS.get(update.getUpdateType());
                WebMarkupContainer iconDivClass = new WebMarkupContainer("iconDivClass");
                iconDivClass.add(new AttributeAppender("class", Model.of(iconDivClassValue)));
                ExternalLink updateLink = new ExternalLink("updateLink", "/" + update.getChapterUrl(), update.getVolumeTitle());
                if (update.getChapterId() == null)
                {
                    updateLink = new ExternalLink("updateLink", "/" + update.getVolumeUrl(), update.getVolumeTitle());
                }
                iconDivClass.add(updateLink);
                listItem.add(iconDivClass);
            }
        };
        add(updatesView);

        final int projectId = mainProject.getProjectId();
        StatelessLink moreUpdates = new StatelessLink("moreUpdates")
        {

            @Override
            public void onClick()
            {
                PageParameters p = new PageParameters();
                p.add("project", projectId);
                setResponsePage(Updates.class, p);
            }
        };
        add(moreUpdates);

        session.close();
    }

    private static class VolumesComparator implements Comparator<Volume>
    {

        @Override
        public int compare(Volume volume1, Volume volume2)
        {
            int projectComparison = volume1.getProjectId().compareTo(volume2.getProjectId());
            if (projectComparison != 0)
            {
                return projectComparison;
            }

            return ((volume1.getSequenceNumber() == null) ? 0 : volume1.getSequenceNumber().compareTo(volume2.getSequenceNumber()));
        }

    }

    private static final RedirectToUrlException REDIRECT_TO_404 = new RedirectToUrlException("http://404");
    private static final ArrayList<String> DISPLAYABLE_NAMES = Lists.newArrayList("Ранобэ", "Побочные истории", "Авторские додзинси", "Другое");
    private static final Map<String, String> VOLUME_STATUS_LABEL_TEXT =
            new ImmutableMap.Builder<String, String>()
                    .put(RuraConstants.VOLUME_STATUS_EXTERNAL_DROPPED, "сторонний перевод")
                    .put(RuraConstants.VOLUME_STATUS_EXTERNAL_ACTIVE, "сторонний перевод")
                    .put(RuraConstants.VOLUME_STATUS_EXTERNAL_DONE, "сторонний перевод")
                    .put(RuraConstants.VOLUME_STATUS_NO_ENG, "нет анлейта")
                    .put(RuraConstants.VOLUME_STATUS_FREEZE, "заморожен")
                    .put(RuraConstants.VOLUME_STATUS_ON_HOLD, "приостановлен")
                    .put(RuraConstants.VOLUME_STATUS_QUEUE, "очередь")
                    .put(RuraConstants.VOLUME_STATUS_ONGOING, "онгоинг")
                    .put(RuraConstants.VOLUME_STATUS_TRANSLATING, "перевод")
                    .put(RuraConstants.VOLUME_STATUS_PROOFREAD, "редакт")
                    .put(RuraConstants.VOLUME_STATUS_DECOR, "не оформлен")
                    .put(RuraConstants.VOLUME_STATUS_DONE, "завершен")
                    .build();
    private static final Map<String, String> VOLUME_STATUS_LABEL_COLOR_CLASS =
            new ImmutableMap.Builder<String, String>()
                    .put(RuraConstants.VOLUME_STATUS_EXTERNAL_DROPPED, "danger")
                    .put(RuraConstants.VOLUME_STATUS_EXTERNAL_ACTIVE, "warning")
                    .put(RuraConstants.VOLUME_STATUS_EXTERNAL_DONE, "success")
                    .put(RuraConstants.VOLUME_STATUS_NO_ENG, "danger")
                    .put(RuraConstants.VOLUME_STATUS_FREEZE, "danger")
                    .put(RuraConstants.VOLUME_STATUS_ON_HOLD, "info")
                    .put(RuraConstants.VOLUME_STATUS_QUEUE, "info")
                    .put(RuraConstants.VOLUME_STATUS_ONGOING, "warning")
                    .put(RuraConstants.VOLUME_STATUS_TRANSLATING, "warning")
                    .put(RuraConstants.VOLUME_STATUS_PROOFREAD, "warning")
                    .put(RuraConstants.VOLUME_STATUS_DECOR, "success")
                    .put(RuraConstants.VOLUME_STATUS_DONE, "success")
                    .build();

    private static final VolumesComparator COMPARATOR = new VolumesComparator();
    private static final int UPDATES_BY_PROJECT_ON_PAGE = 5;
    private static final Map<String, String> UPDATE_TYPE_TO_ICON_DIV_CLASS =
            new ImmutableMap.Builder<String, String>()
                    .put(RuraConstants.UPDATE_TYPE_TRANSLATE, "background-image:url(img/updIcons/icon4.png)")
                    .put(RuraConstants.UPDATE_TYPE_IMAGES, "background-image:url(img/updIcons/icon5.png)")
                    .put(RuraConstants.UPDATE_TYPE_PROOFREAD, "background-image:url(img/updIcons/icon3.png)")
                    .put(RuraConstants.UPDATE_TYPE_OTHER, "background-image:url(img/updIcons/icon1.png)")
                    .build();
}
