package ru.ruranobe.mybatis.mappers.cacheable;

import ru.ruranobe.mybatis.entities.tables.Project;
import ru.ruranobe.mybatis.mappers.ProjectsMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/* Simple inmemory cache, since the number of projects is very limited */
public class ProjectsMapperCacheable implements ProjectsMapper
{
    private static final AtomicBoolean getAllProjectsMethodCalled = new AtomicBoolean(false);
    private static final ConcurrentHashMap<Integer, Project> projectIdToProject =
            new ConcurrentHashMap<>();
    private final ProjectsMapper mapper;

    public ProjectsMapperCacheable(ProjectsMapper mapper)
    {
        this.mapper = mapper;
        if (getAllProjectsMethodCalled.compareAndSet(false, true))
        {
            Collection<Project> projects = mapper.getAllProjects();
            for (Project project : projects)
            {
                projectIdToProject.put(project.getProjectId(), project);
            }
        }
    }

    @Override
    public void insertProject(Project project)
    {
        mapper.insertProject(project);
        projectIdToProject.put(project.getProjectId(), project);
    }

    /* Uncacheable operation. For this operation DB level cache is used.
     * See ProjectsMapper cache tag */
    @Override
    public Project getProjectByUrl(String url)
    {
        return mapper.getProjectByUrl(url);
    }

    @Override
    public Project getProjectById(Integer projectId)
    {
        return mapper.getProjectById(projectId);
    }

    @Override
    public List<Project> getSubProjectsByParentProjectId(Integer parentId)
    {
        return mapper.getSubProjectsByParentProjectId(parentId);
    }

    @Override
    public Collection<Project> getAllProjects()
    {
        return new ArrayList<>(projectIdToProject.values());
    }

    @Override
    public Collection<Project> getRootProjects()
    {
        return mapper.getRootProjects();
    }

    @Override
    public Collection<Project> getAllProjectsWithCustomColumns(String columns)
    {
        return mapper.getAllProjectsWithCustomColumns(columns);
    }

    @Override
    public void updateProject(Project project)
    {
        mapper.updateProject(project);
        projectIdToProject.replace(project.getProjectId(), project);
    }

    @Override
    public void deleteProject(Integer projectId)
    {
        mapper.deleteProject(projectId);
        projectIdToProject.remove(projectId);
    }

    @Override
    public Collection<String> getAllPeople()
    {
        return mapper.getAllPeople();
    }

    @Override
    public Date getProjectUpdateDate(Integer projectId)
    {
        return mapper.getProjectUpdateDate(projectId);
    }

    @Override
    public Date getProjectEditDate(Integer projectId)
    {
        return mapper.getProjectEditDate(projectId);
    }
}

