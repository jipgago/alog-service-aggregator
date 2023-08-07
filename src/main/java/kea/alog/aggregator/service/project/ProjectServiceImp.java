package kea.alog.aggregator.service.project;

import java.util.List;
import java.util.stream.Collectors;
import kea.alog.aggregator.common.dto.PageDto;
import kea.alog.aggregator.common.dto.ResponseDto;
import kea.alog.aggregator.service.mapper.ProjectMemberMapper;
import kea.alog.aggregator.service.openfeign.ProjectFeign;
import kea.alog.aggregator.service.openfeign.UserFeign;
import kea.alog.aggregator.web.constant.ProjectSortType;
import kea.alog.aggregator.web.dto.ProjectDto.FeignProjectResponseDto;
import kea.alog.aggregator.web.dto.ProjectDto.ProjectResponseDto;
import kea.alog.aggregator.web.dto.ProjectMemberDto.ProjectMemberResponseDto;
import kea.alog.aggregator.web.dto.TeamDto.TeamResponseDto;
import kea.alog.aggregator.web.dto.UserDto.UserResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImp implements ProjectService {

    private final UserFeign userFeign;
    private final ProjectFeign projectFeign;
    private final ProjectMemberMapper projectMemberMapper;

    @Override
    public ProjectResponseDto findByPk(Long projectPk) {
        ResponseDto<FeignProjectResponseDto> response = projectFeign.findByPk(projectPk);

        return convertToProjectResponse(response.getData());
    }

    @Override
    public PageDto<ProjectResponseDto> findAll(String keyword, ProjectSortType sortType, int page,
        int size) {
        ResponseDto<PageDto<FeignProjectResponseDto>> response = projectFeign.findAll(keyword, sortType, page, size);
        List<FeignProjectResponseDto> projects = response.getData().getContent();

        return PageDto.<ProjectResponseDto>builder()
                      .content(projects.stream().map(this::convertToProjectResponse).collect(
                          Collectors.toList()))
                      .totalPages(response.getData().getTotalPages())
                      .totalElements(response.getData().getTotalElements())
                      .pageNumber(response.getData().getPageNumber())
                      .pageSize(response.getData().getPageSize())
                      .build();
    }

    @Override
    public PageDto<ProjectMemberResponseDto> findMembers(Long projectPk, String keyword, int page,
        int size) {
        ResponseDto<PageDto<Long>> response = projectFeign.findMembers(projectPk, keyword, page, size);
        List<Long> userPks = response.getData().getContent();
        List<UserResponseDto> user = userPks.stream().map(this::findUser).collect(Collectors.toList());

        return PageDto.<ProjectMemberResponseDto>builder().content(user.stream().map(projectMemberMapper::toProjectMemberResponseDto).collect(
                          Collectors.toList()))
                      .totalPages(response.getData().getTotalPages())
                      .totalElements(response.getData().getTotalElements())
                      .pageNumber(response.getData().getPageNumber())
                      .pageSize(response.getData().getPageSize())
                      .build();
    }

    private UserResponseDto findUser(Long pmPk){
        return userFeign.findUserByPk(pmPk);
    }

    private TeamResponseDto findTeam(Long projectPk, Long teamPk) {
        return userFeign.findTeamByPk(projectPk, teamPk);
    }

    private ProjectResponseDto convertToProjectResponse(FeignProjectResponseDto project) {
        Long projectPk = project.getPk();
        UserResponseDto pm = findUser(project.getPmPk());

        List<UserResponseDto> projectMembers = project.getProjectMembers().stream().map(this::findUser).collect(
            Collectors.toList());
        TeamResponseDto team = findTeam(projectPk, project.getTeamPk());

        return ProjectResponseDto.builder().pk(projectPk).name(project.getName())
                                 .description(project.getDescription()).team(team).pm(pm)
                                 .topics(project.getTopics()).projectMembers(projectMembers)
                                 .createdAt(project.getCreatedAt()).build();
    }
}
