package com.daiend.muriox.organize;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.post.PostMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrganizeService {

    private final OrganizeMapper organizeMapper;
    private final PostMapper postMapper;

    public OrganizeService(OrganizeMapper organizeMapper, PostMapper postMapper) {
        this.organizeMapper = organizeMapper;
        this.postMapper = postMapper;
    }

    public PageResult<OrganizeTreeItem> treePage(long current, long size, String organizeName) {
        if (current < 1) {
            throw new BusinessException("当前页必须大于等于 1");
        }

        if (size < 1 || size > 100) {
            throw new BusinessException("每页数量必须在 1 到 100 之间");
        }
        if (organizeName != null && !organizeName.isBlank()) {
            return searchTreePage(
                    current,
                    size,
                    organizeName.trim());
        }

        Page<Organize> rootPage = organizeMapper.selectRootOrganizePage(current, size);
        if (rootPage.getRecords().isEmpty()) {
            return new PageResult<>(
                    List.of(),
                    rootPage.getTotal(),
                    rootPage.getCurrent(),
                    rootPage.getSize(),
                    rootPage.getPages()
            );
        }
        List<Organize> allOrganizes = organizeMapper.findAllOrganize();

        Map<Long, List<Organize>> childrenByParentId = allOrganizes
                .stream()
                .filter(organize -> organize.getParentId() != null)
                .collect(Collectors.groupingBy(Organize::getParentId));

        List<OrganizeTreeItem> records = rootPage.getRecords()
                .stream()
                .map(organize -> convertToTreeItem(
                        organize,
                        childrenByParentId,
                        new HashSet<>()
                )).toList();

        return new PageResult<>(
                records,
                rootPage.getTotal(),
                rootPage.getCurrent(),
                rootPage.getSize(),
                rootPage.getPages()
        );
    }


    public List<OrganizeTreeItem> tree() {

        List<Organize> organizes = organizeMapper.findAllOrganize();
        Map<Long, List<Organize>> childrenByParentId = organizes
                .stream()
                .filter(organize -> organize.getParentId() != null)
                .collect(Collectors.groupingBy(Organize::getParentId));
        return organizes
                .stream()
                .filter(organize -> organize.getParentId() == null)
                .map(organize -> convertToTreeItem(
                        organize,
                        childrenByParentId,
                        new HashSet<>()
                ))
                .toList();

    }

    @Transactional
    public Long addOrganize(OrganizeRequest request) {
        OrganizeValue values = toOrganizeValue(request);
        validateOrganizeForCreate(values);

        Organize organize = new Organize();
        applyOrganizeValue(organize, values);

        if (organizeMapper.insert(organize) != 1) {
            throw new BusinessException("新增组织失败");
        }
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

        OrganizeValue organizeValue = toOrganizeValue(request);
        validateOrganizeForUpdate(organize, organizeValue);
        applyOrganizeValue(organize, organizeValue);
        int affectedRows = organizeMapper.updateById(organize);

        if (affectedRows != 1) {
            throw new BusinessException("编辑组织失败");
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


    }

    private PageResult<OrganizeTreeItem> searchTreePage(long current,
                                                        long size,
                                                        String keyword) {
        List<Organize> allOrganizes = organizeMapper.findAllOrganize();
        Map<Long, List<Organize>> childrenByParentId = allOrganizes.stream()
                .filter(organize -> organize.getParentId() != null).collect(Collectors.groupingBy(Organize::getParentId));

        String normalizedKeyword =
                keyword.toLowerCase(Locale.ROOT);
        List<OrganizeTreeItem> matchedRoots = allOrganizes.stream()
                .filter(organize -> organize.getParentId() == null)
                .map(organize -> filterTreeItem(organize, childrenByParentId, normalizedKeyword, new HashSet<>()))
                .flatMap(Optional::stream)
                .toList();

        return paginateTreeItems(matchedRoots, current, size);
    }

    private Optional<OrganizeTreeItem> filterTreeItem(Organize organize, Map<Long, List<Organize>> childrenByParentId, String normalizedKeyword, Set<Long> ancestorIds) {
        if (!ancestorIds.add(organize.getId())) {
            throw new BusinessException("组织层级存在循环");
        }
        List<OrganizeTreeItem> matchedChildren = childrenByParentId.getOrDefault(organize.getId(), List.of())
                .stream()
                .map(child -> filterTreeItem(
                        child,
                        childrenByParentId,
                        normalizedKeyword,
                        new HashSet<>(ancestorIds)
                ))
                .flatMap(Optional::stream)
                .toList();

        boolean currentMatched = containsIgnoreCase(organize.getName(), normalizedKeyword);

        if (!currentMatched && matchedChildren.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(createTreeItem(organize, matchedChildren));
    }

    private boolean containsIgnoreCase(
            String value,
            String normalizedKeyword) {

        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(normalizedKeyword);
    }

    private OrganizeTreeItem createTreeItem(Organize organize, List<OrganizeTreeItem> children) {
        return new OrganizeTreeItem(
                organize.getId(),
                organize.getName(),
                organize.getParentId(),
                organize.getStatus(),
                organize.getSortOrder(),
                organize.getRemark(),
                organize.getCreatedAt(),
                organize.getUpdatedAt(),
                children
        );
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

    private OrganizeTreeItem convertToTreeItem(Organize organize, Map<Long, List<Organize>> childrenByParentId, Set<Long> ancestorIds) {
        if (!ancestorIds.add(organize.getId())) {
            throw new BusinessException("组织层级存在循环");
        }
        List<OrganizeTreeItem> children = childrenByParentId
                .getOrDefault(organize.getId(), List.of())
                .stream()
                .map(child -> convertToTreeItem(
                        child,
                        childrenByParentId,
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
                children
        );
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

        Set<Long> visitedIds = new HashSet<>();
        Long currentId = parentId;

        while (currentId != null) {
            if (!visitedIds.add(currentId)) {
                throw new BusinessException("组织层级存在循环");
            }

            if (Objects.equals(currentId, organizeId)) {
                throw new BusinessException(
                        "不能选择自身或后代作为上级组织");
            }

            Organize parent = organizeMapper.selectById(currentId);
            if (parent == null) {
                throw new BusinessException("上级组织不存在");
            }

            currentId = parent.getParentId();
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
