package com.daiend.muriox.organize;

import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.datascope.DataScopeGuard;
import com.daiend.muriox.datascope.UserDataScope;
import com.daiend.muriox.datascope.UserDataScopeService;
import com.daiend.muriox.post.PostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizeService {

    private final OrganizeMapper organizeMapper;
    private final PostMapper postMapper;
    private final OrganizeHierarchyChangePublisher hierarchyChangePublisher;
    private final UserDataScopeService dataScopeService;
    private final DataScopeGuard dataScopeGuard;

    public OrganizeService(
            OrganizeMapper organizeMapper,
            PostMapper postMapper,
            OrganizeHierarchyChangePublisher hierarchyChangePublisher,
            UserDataScopeService dataScopeService,
            DataScopeGuard dataScopeGuard) {
        this.organizeMapper = organizeMapper;
        this.postMapper = postMapper;
        this.hierarchyChangePublisher = hierarchyChangePublisher;
        this.dataScopeService = dataScopeService;
        this.dataScopeGuard = dataScopeGuard;
    }

    public PageResult<OrganizeTreeItem> treePage(
            long current,
            long size,
            String organizeName,
            Long userId) {
        if (current < 1) {
            throw new BusinessException("当前页必须大于等于 1");
        }

        if (size < 1 || size > 100) {
            throw new BusinessException("每页数量必须在 1 到 100 之间");
        }
        VisibleOrganizes visible = loadVisibleOrganizes(userId);
        List<OrganizeTreeItem> roots = buildTree(visible);

        if (organizeName != null
                && !organizeName.isBlank()) {
            String keyword = organizeName.trim()
                    .toLowerCase(Locale.ROOT);
            roots = roots.stream()
                    .map(root -> filterTreeItem(root, keyword))
                    .flatMap(Optional::stream)
                    .toList();
        }

        return paginateTreeItems(roots, current, size);
    }


    public List<OrganizeTreeItem> tree(Long userId) {
        return buildTree(loadVisibleOrganizes(userId));
    }

    @Transactional
    public Long addOrganize(OrganizeRequest request) {
        OrganizeValue values = toOrganizeValue(request);
        assertParentWritable(values.parentId());
        validateOrganizeForCreate(values);

        Organize organize = new Organize();
        applyOrganizeValue(organize, values);

        if (organizeMapper.insert(organize) != 1) {
            throw new BusinessException("新增组织失败");
        }
        hierarchyChangePublisher.publish();
        return organize.getId();
    }

    public OrganizeResponse getOrganize(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("组织 ID 不合法");
        }
        Organize organize = findOrganizeOrThrow(id);

        return new OrganizeResponse(
                organize.getId(),
                organize.getName(),
                organize.getParentId(),
                organize.getStatus(),
                organize.getSortOrder(),
                organize.getRemark(),
                organize.getCreatedAt(),
                organize.getUpdatedAt()
        );

    }

    @Transactional
    public Long updateOrganize(OrganizeUpdateRequest request) {
        Organize organize = findOrganizeOrThrow(request.id());
        dataScopeGuard.assertOrgAllowed(organize.getId());

        OrganizeValue organizeValue = toOrganizeValue(request);
        assertParentWritable(organizeValue.parentId());
        validateOrganizeForUpdate(organize, organizeValue);
        boolean hierarchyChanged =
                !Objects.equals(
                        organize.getParentId(),
                        organizeValue.parentId());
        applyOrganizeValue(organize, organizeValue);
        int affectedRows = organizeMapper.updateById(organize);

        if (affectedRows != 1) {
            throw new BusinessException("编辑组织失败");
        }
        if (hierarchyChanged) {
            hierarchyChangePublisher.publish();
        }

        return organize.getId();

    }

    @Transactional
    public void deleteOrganize(List<Long> ids) {
        List<Long> normalizeDeleteIds = normalizeDeleteIds(ids);
        List<Organize> organizes = organizeMapper.selectByIds(normalizeDeleteIds);
        if (organizes.size() != normalizeDeleteIds.size()) {
            throw new BusinessException("部分组织不存在");
        }
        dataScopeGuard.assertAllOrgsAllowed(
                organizes.stream()
                        .map(Organize::getId)
                        .toList());
        if (organizeMapper.hasChildrenByParentIds(normalizeDeleteIds)) {
            throw new BusinessException(
                    "待删除组织仍有子组织，请先删除子组织");
        }
        if (postMapper.existsByOrgIds(normalizeDeleteIds)) {
            throw new BusinessException(
                    "待删除组织仍有关联岗位，请先处理关联岗位");
        }
        int affectedRows = organizeMapper.deleteByIds(normalizeDeleteIds);
        if (affectedRows != normalizeDeleteIds.size()) {
            throw new BusinessException(
                    "删除组织失败");
        }
        hierarchyChangePublisher.publish();

    }

    private Optional<OrganizeTreeItem> filterTreeItem(
            OrganizeTreeItem item,
            String normalizedKeyword) {

        List<OrganizeTreeItem> matchedChildren = item.children()
                .stream()
                .map(child -> filterTreeItem(
                        child,
                        normalizedKeyword))
                .flatMap(Optional::stream)
                .toList();

        boolean currentMatched = containsIgnoreCase(
                item.name(), normalizedKeyword);

        if (!currentMatched && matchedChildren.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OrganizeTreeItem(
                item.id(),
                item.name(),
                item.parentId(),
                item.status(),
                item.sortOrder(),
                item.remark(),
                item.createdAt(),
                item.updatedAt(),
                item.dataAllowed(),
                matchedChildren));
    }

    private boolean containsIgnoreCase(
            String value,
            String normalizedKeyword) {

        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(normalizedKeyword);
    }

    private PageResult<OrganizeTreeItem> paginateTreeItems(
            List<OrganizeTreeItem> matchedRoots,
            long current,
            long size) {
        long total = matchedRoots.size();
        long pages = total == 0
                ? 0
                : (total + size - 1) / size;

        long offset = (current - 1) * size;

        if (offset >= total) {
            return new PageResult<>(
                    List.of(),
                    total,
                    current,
                    size,
                    pages);
        }
        int fromIndex = Math.toIntExact(offset);
        int toIndex = Math.toIntExact(
                Math.min(offset + size, total));
        List<OrganizeTreeItem> records =
                List.copyOf(
                        matchedRoots.subList(
                                fromIndex,
                                toIndex));


        return new PageResult<>(
                records,
                total,
                current,
                size,
                pages);
    }

    private List<OrganizeTreeItem> buildTree(
            VisibleOrganizes visible) {

        Set<Long> visibleIds = visible.organizes()
                .stream()
                .map(Organize::getId)
                .collect(Collectors.toSet());
        Map<Long, List<Organize>> childrenByParentId =
                visible.organizes().stream()
                        .filter(organize ->
                                organize.getParentId() != null)
                        .collect(Collectors.groupingBy(
                                Organize::getParentId));

        return visible.organizes().stream()
                .filter(organize ->
                        organize.getParentId() == null
                                || !visibleIds.contains(
                                organize.getParentId()))
                .map(organize -> convertToTreeItem(
                        organize,
                        childrenByParentId,
                        visible.allowedOrgIds(),
                        new HashSet<>()))
                .toList();
    }

    private OrganizeTreeItem convertToTreeItem(
            Organize organize,
            Map<Long, List<Organize>> childrenByParentId,
            Set<Long> allowedOrgIds,
            Set<Long> ancestorIds) {
        if (!ancestorIds.add(organize.getId())) {
            throw new BusinessException("组织层级存在循环");
        }
        List<OrganizeTreeItem> children = childrenByParentId
                .getOrDefault(organize.getId(), List.of())
                .stream()
                .map(child -> convertToTreeItem(
                        child,
                        childrenByParentId,
                        allowedOrgIds,
                        new HashSet<>(ancestorIds)
                ))
                .toList();
        return new OrganizeTreeItem(
                organize.getId(),
                organize.getName(),
                organize.getParentId(),
                organize.getStatus(),
                organize.getSortOrder(),
                organize.getRemark(),
                organize.getCreatedAt(),
                organize.getUpdatedAt(),
                allowedOrgIds.contains(organize.getId()),
                children
        );
    }

    private VisibleOrganizes loadVisibleOrganizes(
            Long userId) {

        UserDataScope scope =
                dataScopeService.getDataScope(userId);

        if (scope.all()) {
            List<Organize> organizes =
                    organizeMapper.findAllOrganize();
            Set<Long> allIds = organizes.stream()
                    .map(Organize::getId)
                    .collect(Collectors.toSet());
            return new VisibleOrganizes(organizes, allIds);
        }

        if (scope.isDenied()) {
            return new VisibleOrganizes(
                    List.of(), Set.of());
        }

        return new VisibleOrganizes(
                organizeMapper
                        .findAllowedAndAncestorOrganizes(
                                scope.orgIds()),
                Set.copyOf(scope.orgIds()));
    }

    private void assertParentWritable(Long parentId) {
        if (parentId == null) {
            dataScopeGuard.assertAllData();
            return;
        }

        dataScopeGuard.assertOrgAllowed(parentId);
    }

    private record VisibleOrganizes(
            List<Organize> organizes,
            Set<Long> allowedOrgIds) {
    }

    private record OrganizeValue(
            String name,
            Long parentId,
            Integer sortOrder,
            Boolean status,
            String remark) {
    }

    private OrganizeValue toOrganizeValue(OrganizeRequest request) {
        return new OrganizeValue(
                request.name().trim(),
                normalizeParentId(request.parentId()),
                request.sortOrder(),
                request.status(),
                request.remark()
        );
    }

    private OrganizeValue toOrganizeValue(OrganizeUpdateRequest request) {
        return new OrganizeValue(
                request.name().trim(),
                normalizeParentId(request.parentId()),
                request.sortOrder(),
                request.status(),
                request.remark()
        );
    }

    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return null;
        }

        return parentId;
    }

    private void validateOrganizeForCreate(OrganizeValue organizeValue) {
        validateParentOrganize(organizeValue.parentId());
        if (organizeMapper.existsByNameAndParentId(organizeValue.name(), organizeValue.parentId())) {
            throw new BusinessException("同一上级组织下名称已存在");
        }

    }

    private void validateParentOrganize(Long parentId) {
        if (parentId == null) {
            return;
        }

        Organize organize = organizeMapper.selectById(parentId);

        if (organize == null) {
            throw new BusinessException("上级组织不存在");
        }

    }

    private void applyOrganizeValue(Organize organize, OrganizeValue organizeValue) {
        organize.setName(organizeValue.name());
        organize.setParentId(organizeValue.parentId());
        organize.setSortOrder(organizeValue.sortOrder());
        organize.setRemark(organizeValue.remark());
        organize.setStatus(organizeValue.status());
    }

    private Organize findOrganizeOrThrow(Long id) {
        Organize organize = organizeMapper.selectById(id);
        if (organize == null) {
            throw new BusinessException("组织不存在");
        }

        return organize;
    }

    private void validateOrganizeForUpdate(
            Organize organize,
            OrganizeValue organizeValue) {

        validateParentForUpdate(
                organize.getId(),
                organizeValue.parentId());

        if (organizeMapper.existsByNameAndParentIdExcludingId(
                organizeValue.name(),
                organizeValue.parentId(),
                organize.getId())) {
            throw new BusinessException("同一上级组织下名称已存在");
        }

    }

    private void validateParentForUpdate(
            Long organizeId,
            Long parentId) {

        if (parentId == null) {
            return;
        }

        List<Organize> ancestors = organizeMapper
                .findAllowedAndAncestorOrganizes(
                        List.of(parentId));

        if (ancestors.stream().anyMatch(organize ->
                Objects.equals(organize.getId(), organizeId))) {
                throw new BusinessException(
                        "不能选择自身或后代作为上级组织");
        }
    }

    private List<Long> normalizeDeleteIds(
            List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(
                    "请选择需要删除的组织");
        }

        if (ids.stream().anyMatch(
                id -> id == null || id <= 0)) {

            throw new BusinessException(
                    "组织 ID 不合法");
        }

        return ids.stream()
                .distinct()
                .toList();
    }
}
