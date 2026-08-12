package com.daiend.muriox.menu;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.menu.response.MenuMeta;
import com.daiend.muriox.menu.response.MenuNode;
import com.daiend.muriox.menu.response.MenuTreeItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private final MenuMapper menuMapper;

    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    public List<MenuNode> findEnabledMenuTreeByUserId(Long userId) {
        List<Menu> menus = menuMapper.findEnabledMenusByUserId(userId);

        Map<Long, List<Menu>> childrenByParentId = menus.stream().filter(menu -> menu.getParentId() != null).collect(Collectors.groupingBy(Menu::getParentId));

        return menus.stream().filter(menu -> menu.getParentId() == null)
                .map(menu -> convertToNode(menu, childrenByParentId, new HashSet<>()))
                .toList();
    }

    public PageResult<MenuTreeItem> treePage(
            long current,
            long size,
            String title
    ) {
        if (current < 1) {
            throw new BusinessException("当前页必须大于等于 1");
        }

        if (size < 1) {
            throw new BusinessException("每页数量必须大于等于 1");
        }
        if (title != null && !title.isBlank()) {
            return searchTreePage(
                    current,
                    size,
                    title.trim());
        }
        Page<Menu> rootPage =
                menuMapper.selectRootPage(current, size);

        if (rootPage.getRecords().isEmpty()) {
            return new PageResult<>(
                    List.of(),
                    rootPage.getTotal(),
                    rootPage.getCurrent(),
                    rootPage.getSize(),
                    rootPage.getPages()
            );
        }

        List<Menu> allMenus = menuMapper.findAllMenus();

        Map<Long, List<Menu>> childrenByParentId =
                allMenus.stream()
                        .filter(menu -> menu.getParentId() != null)
                        .collect(
                                Collectors.groupingBy(
                                        Menu::getParentId
                                )
                        );

        List<MenuTreeItem> records =
                rootPage.getRecords()
                        .stream()
                        .map(menu -> convertToTreeItem(
                                menu,
                                childrenByParentId,
                                new HashSet<>()
                        ))
                        .toList();

        return new PageResult<>(
                records,
                rootPage.getTotal(),
                rootPage.getCurrent(),
                rootPage.getSize(),
                rootPage.getPages()
        );
    }


    public List<MenuTreeItem> tree() {
        List<Menu> allMenus = menuMapper.findAllMenus();

        Map<Long, List<Menu>> childrenByParentId =
                allMenus.stream()
                        .filter(menu -> menu.getParentId() != null)
                        .collect(Collectors.groupingBy(
                                Menu::getParentId));

        return allMenus.stream()
                .filter(menu -> menu.getParentId() == null)
                .map(menu -> convertToTreeItem(
                        menu,
                        childrenByParentId,
                        new HashSet<>()))
                .toList();
    }

    @Transactional
    public Long addMenu(MenuRequest request) {
        Long parentId = normalizeParentId(request.parentId());
        validateParentMenu(parentId);
        validateMenuRequest(request);
        String name = request.name().trim();
        String path = request.path().trim();
        if (menuMapper.existsByName(name)) {
            throw new BusinessException("路由名称已存在");
        }
        if (menuMapper.existsByPath(path)) {
            throw new BusinessException("路由地址已存在");
        }


        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setMenuType(request.menuType());
        menu.setName(name);
        menu.setTitle(request.title().trim());
        menu.setPath(path);
        menu.setComponent(
                request.menuType() == 2
                        ? normalizeOptional(request.component())
                        : null);
        menu.setRedirect(
                normalizeOptional(request.redirect()));
        menu.setIcon(
                normalizeOptional(request.icon()));
        menu.setHidden(request.hidden());
        menu.setStatus(request.status());
        menu.setSortOrder(request.sortOrder());
        menu.setDescription(
                normalizeOptional(request.description()));
        menu.setRemark(
                normalizeOptional(request.remark()));
        int affectedRows = menuMapper.insert(menu);
        if (affectedRows != 1) {
            throw new BusinessException("新增菜单失败");
        }

        return menu.getId();
    }

    private MenuNode convertToNode(Menu menu, Map<Long, List<Menu>> childrenByParentId, Set<Long> ancestorIds) {
        if (!ancestorIds.add(menu.getId())) {
            throw new BusinessException("菜单层级存在循环");
        }
        List<MenuNode> children = childrenByParentId.getOrDefault(menu.getId(), List.of())
                .stream()
                .map(child -> convertToNode(child, childrenByParentId, new HashSet<>(ancestorIds)))
                .toList();
        MenuMeta meta = new MenuMeta(menu.getTitle(), menu.getIcon(), Boolean.TRUE.equals(menu.getHidden()), menu.getDescription());

        return new MenuNode(menu.getId(), menu.getName(), menu.getPath(), menu.getComponent(), menu.getRedirect(), meta, children);
    }

    private MenuTreeItem convertToTreeItem(
            Menu menu,
            Map<Long, List<Menu>> childrenByParentId,
            Set<Long> ancestorIds
    ) {
        if (!ancestorIds.add(menu.getId())) {
            throw new BusinessException("菜单层级存在循环");
        }

        List<MenuTreeItem> children =
                childrenByParentId
                        .getOrDefault(menu.getId(), List.of())
                        .stream()
                        .map(child -> convertToTreeItem(
                                child,
                                childrenByParentId,
                                new HashSet<>(ancestorIds)
                        ))
                        .toList();

        return new MenuTreeItem(
                menu.getId(),
                menu.getParentId(),
                menu.getMenuType(),
                menu.getName(),
                menu.getTitle(),
                menu.getPath(),
                menu.getComponent(),
                menu.getRedirect(),
                menu.getIcon(),
                menu.getHidden(),
                menu.getStatus(),
                menu.getSortOrder(),
                menu.getDescription(),
                menu.getRemark(),
                menu.getCreatedAt(),
                menu.getUpdatedAt(),
                children
        );
    }

    private PageResult<MenuTreeItem> searchTreePage(
            long current,
            long size,
            String keyword) {

        List<Menu> allMenus = menuMapper.findAllMenus();

        Map<Long, List<Menu>> childrenByParentId =
                allMenus.stream()
                        .filter(menu -> menu.getParentId() != null)
                        .collect(Collectors.groupingBy(
                                Menu::getParentId));

        String normalizedKeyword =
                keyword.toLowerCase(Locale.ROOT);

        List<MenuTreeItem> matchedRoots =
                allMenus.stream()
                        .filter(menu -> menu.getParentId() == null)
                        .map(menu -> filterTreeItem(
                                menu,
                                childrenByParentId,
                                normalizedKeyword,
                                new HashSet<>()))
                        .flatMap(Optional::stream)
                        .toList();

        return paginateTreeItems(
                matchedRoots,
                current,
                size);
    }

    private Optional<MenuTreeItem> filterTreeItem(
            Menu menu,
            Map<Long, List<Menu>> childrenByParentId,
            String keyword,
            Set<Long> ancestorIds) {

        if (!ancestorIds.add(menu.getId())) {
            throw new BusinessException("菜单层级存在循环");
        }

        List<MenuTreeItem> matchedChildren =
                childrenByParentId
                        .getOrDefault(menu.getId(), List.of())
                        .stream()
                        .map(child -> filterTreeItem(
                                child,
                                childrenByParentId,
                                keyword,
                                new HashSet<>(ancestorIds)))
                        .flatMap(Optional::stream)
                        .toList();

        boolean currentMatched =
                containsIgnoreCase(menu.getTitle(), keyword);

        if (!currentMatched && matchedChildren.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                createTreeItem(menu, matchedChildren));
    }

    private boolean containsIgnoreCase(
            String value,
            String normalizedKeyword) {

        return value != null
                && value.toLowerCase(Locale.ROOT)
                .contains(normalizedKeyword);
    }

    private MenuTreeItem createTreeItem(
            Menu menu,
            List<MenuTreeItem> children) {

        return new MenuTreeItem(
                menu.getId(),
                menu.getParentId(),
                menu.getMenuType(),
                menu.getName(),
                menu.getTitle(),
                menu.getPath(),
                menu.getComponent(),
                menu.getRedirect(),
                menu.getIcon(),
                menu.getHidden(),
                menu.getStatus(),
                menu.getSortOrder(),
                menu.getDescription(),
                menu.getRemark(),
                menu.getCreatedAt(),
                menu.getUpdatedAt(),
                children);
    }

    private PageResult<MenuTreeItem> paginateTreeItems(
            List<MenuTreeItem> matchedRoots,
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

        List<MenuTreeItem> records =
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


    private Long normalizeParentId(Long parentId) {
        if (parentId == null || parentId == 0L) {
            return null;
        }

        return parentId;
    }

    private void validateParentMenu(Long parentId) {
        if (parentId == null) {
            return;
        }

        Menu parentMenu = menuMapper.selectById(parentId);

        if (parentMenu == null) {
            throw new BusinessException("上级菜单不存在");
        }

        if (!Integer.valueOf(1).equals(
                parentMenu.getMenuType())) {
            throw new BusinessException(
                    "页面菜单不能作为上级菜单");
        }
    }

    private void validateMenuRequest(
            MenuRequest request) {

        if (request.menuType() == 2
                && normalizeOptional(
                request.component()) == null) {
            throw new BusinessException(
                    "页面菜单必须填写前端组件");
        }
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
