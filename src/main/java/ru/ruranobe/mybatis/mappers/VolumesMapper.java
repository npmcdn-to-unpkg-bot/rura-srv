package ru.ruranobe.mybatis.mappers;

import ru.ruranobe.mybatis.entities.tables.ProjectInfo;
import ru.ruranobe.mybatis.entities.tables.Volume;

import java.util.List;

public interface VolumesMapper
{
    Volume getVolumeByUrl(String url);

    Volume getVolumeNextPrevByUrl(String url);

    ProjectInfo getInfoByProjectId(int projectId);

    List<Volume> getVolumesByProjectId(int projectId);

    void updateVolume(Volume volume);

    void updateVolumeCovers(Volume volume);

    int insertVolume(Volume volumre);

    void deleteVolume(Integer volumeId);
}
