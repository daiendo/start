package com.daiend.muriox.resource;

import com.daiend.muriox.auth.UserAuthorityChangePublisher;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.menu.Menu;
import com.daiend.muriox.menu.MenuMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final MenuMapper menuMapper;
    private final UserAuthorityChangePublisher authorityChangePublisher;

    public ResourceService(ResourceMapper resourceMapper, MenuMapper menuMapper, UserAuthorityChangePublisher authorityChangePublisher) {
        this.resourceMapper = resourceMapper;
        this.menuMapper = menuMapper;
        this.authorityChangePublisher = authorityChangePublisher;
    }

    @Transactional
    public Long addResource(ResourceRequest resourceRequest) {
        validateMenu(resourceRequest.menuId());

        String code = resourceRequest.code()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (resourceMapper.existsByCode(code)) {
            throw new BusinessException(
                    "权限编码已存在");
        }
        Resource resource = new Resource();
        resource.setMenuId(resourceRequest.menuId());
        resource.setName(resourceRequest.name().trim());
        resource.setCode(code);
        resource.setStatus(resourceRequest.status());
        resource.setSortOrder(resourceRequest.sortOrder());
        resource.setRemark(
                normalizeOptional(resourceRequest.remark()));
        int affectedRows =
                resourceMapper.insert(resource);

        if (affectedRows != 1) {
            throw new BusinessException(
                    "新增按钮权限失败");
        }
        return resource.getId();
    }

    public List<ResourceResponse> listByMenuId(Long menuId) {
        validateMenu(menuId);
        return resourceMapper.selectListByMenuId(menuId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ResourceResponse getResource(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(
                    "按钮权限 ID 不合法");
        }

        Resource resource =
                resourceMapper.selectById(id);

        if (resource == null) {
            throw new BusinessException(
                    "按钮权限不存在");
        }

        return toResponse(resource);
    }

    @Transactional
    public Long updateResource(
            ResourceUpdateRequest request) {

        Resource resource =
                resourceMapper.selectById(request.id());

        if (resource == null) {
            throw new BusinessException(
                    "按钮权限不存在");
        }

        validateMenu(request.menuId());

        String code = request.code()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (resourceMapper.existsByCodeExcludingId(
                code,
                request.id())) {

            throw new BusinessException(
                    "权限编码已存在");
        }

        boolean authorityChanged =
                !Objects.equals(
                        resource.getMenuId(),
                        request.menuId())
                        || !Objects.equals(
                        resource.getCode(),
                        code)
                        || !Objects.equals(
                        resource.getStatus(),
                        request.status());

        List<Long> affectedUserIds =
                authorityChanged
                        ? resourceMapper.findUserIdsByResourceId(
                        resource.getId())
                        : List.of();

        resource.setMenuId(request.menuId());
        resource.setName(request.name().trim());
        resource.setCode(code);
        resource.setStatus(request.status());
        resource.setSortOrder(request.sortOrder());
        resource.setRemark(
                normalizeOptional(request.remark()));

        int affectedRows =
                resourceMapper.updateById(resource);

        if (affectedRows != 1) {
            throw new BusinessException(
                    "编辑按钮权限失败");
        }

        if (authorityChanged) {
            authorityChangePublisher.publishForUsers(
                    affectedUserIds);
        }

        return resource.getId();
    }

    @Transactional
    public void deleteResources(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(
                    "请选择需要删除的按钮权限");
        }

        if (ids.stream().anyMatch(
                id -> id == null || id <= 0)) {

            throw new BusinessException(
                    "按钮权限 ID 不合法");
        }

        List<Long> resourceIds =
                ids.stream()
                        .distinct()
                        .toList();

        List<Resource> resources =
                resourceMapper.selectByIds(resourceIds);

        if (resources.size() != resourceIds.size()) {
            throw new BusinessException(
                    "部分按钮权限不存在");
        }

        // 必须在级联删除关联关系之前查询。
        List<Long> affectedUserIds =
                resourceMapper.findUserIdsByResourceIds(
                        resourceIds);

        // 删除资源，PostgreSQL 自动级联删除 sys_role_resource。
        int affectedRows =
                resourceMapper.deleteByIds(resourceIds);
        if (affectedRows != resourceIds.size()) {
            throw new BusinessException(
                    "删除按钮权限失败");
        }

        authorityChangePublisher.publishForUsers(
                affectedUserIds);
        ;
    }

    private void validateMenu(Long menuId) {
        Menu menu = menuMapper.selectById(menuId);

        if (menu == null) {
            throw new BusinessException(
                    "所属菜单不存在");
        }

        if (!Integer.valueOf(2).equals(
                menu.getMenuType())) {
            throw new BusinessException(
                    "按钮权限只能配置在页面菜单下");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }


    private ResourceResponse toResponse(
            Resource resource) {

        return new ResourceResponse(
                resource.getId(),
                resource.getMenuId(),
                resource.getName(),
                resource.getCode(),
                resource.getStatus(),
                resource.getSortOrder(),
                resource.getRemark(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
