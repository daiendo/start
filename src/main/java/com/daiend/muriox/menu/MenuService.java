package com.daiend.muriox.menu;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.auth.UserAuthorityChangePublisher;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.menu.response.MenuMeta;
import com.daiend.muriox.menu.response.MenuNode;
import com.daiend.muriox.menu.response.MenuTreeItem;
import com.daiend.muriox.resource.ResourceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {
    private final MenuMapper menuMapper;
    private final UserAuthorityChangePublisher authorityChangePublisher;
    private final ResourceMapper resourceMapper;

    public MenuService(
            MenuMapper menuMapper,
            UserAuthorityChangePublisher authorityChangePublisher,
            ResourceMapper resourceMapper) {
        this.menuMapper = menuMapper;
        this.authorityChangePublisher = authorityChangePublisher;
        this.resourceMapper = resourceMapper;
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
        MenuValues values = toMenuValues(request);
        validateMenuForCreate(values);

        Menu menu = new Menu();
        applyMenuValues(menu, values);

        int affectedRows = menuMapper.insert(menu);
        if (affectedRows != 1) {
            throw new BusinessException("新增菜单失败");
        }

        return menu.getId();
    }


    public Menu getMenu(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("菜单id不合法");
        }

        return findMenuOrThrow(id);
    }

    @Transactional
    public Long updateMenu(MenuUpdateRequest request) {
        Menu menu = findMenuOrThrow(request.id());
        MenuValues values = toMenuValues(request);

        validateMenuForUpdate(menu, values);

        List<Long> affectedUserIds =
                menuMapper.findUserIdsByMenuId(menu.getId());

        applyMenuValues(menu, values);

        int affectedRows = menuMapper.updateById(menu);
        if (affectedRows != 1) {
            throw new BusinessException("编辑菜单失败");
        }

        authorityChangePublisher.publishForUsers(
                affectedUserIds);

        return menu.getId();
    }
    @Transactional
    public void deleteMenus(List<Long> ids) {
        List<Long> menuIds =
                normalizeDeleteIds(ids);

        List<Menu> menus =
                menuMapper.selectByIds(menuIds);

        if (menus.size() != menuIds.size()) {
            throw new BusinessException(
                    "部分菜单不存在");
        }

        if (menuMapper.hasChildrenByParentIds(
                menuIds)) {

            throw new BusinessException(
                    "待删除菜单仍有子菜单，请先删除子菜单");
        }

        if (resourceMapper.existsByMenuIds(
                menuIds)) {

            throw new BusinessException(
                    "待删除菜单仍有按钮权限，请先删除按钮权限");
        }

        /*
         * 必须在菜单和 sys_role_menu 关联关系删除前查询。
         */
        List<Long> affectedUserIds =
                menuMapper.findUserIdsByMenuIds(
                        menuIds);

        int affectedRows =
                menuMapper.deleteByIds(menuIds);

        if (affectedRows != menuIds.size()) {
            throw new BusinessException(
                    "删除菜单失败");
        }

        authorityChangePublisher.publishForUsers(
                affectedUserIds);
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

    private Menu findMenuOrThrow(Long menuId) {
        Menu menu = menuMapper.selectById(menuId);

        if (menu == null) {
            throw new BusinessException("菜单不存在");
        }

        return menu;
    }

    private void validateMenuForCreate(MenuValues values) {
        validateParentMenu(values.parentId());
        validatePageComponent(
                values.menuType(),
                values.component());

        if (menuMapper.existsByName(values.name())) {
            throw new BusinessException("路由名称已存在");
        }

        if (menuMapper.existsByPath(values.path())) {
            throw new BusinessException("路由地址已存在");
        }
    }

    private void validateMenuForUpdate(
            Menu menu,
            MenuValues values) {

        validateParentSelection(
                menu.getId(),
                values.parentId());

        validateMenuTypeChange(
                menu,
                values.menuType());

        validatePageComponent(
                values.menuType(),
                values.component());

        if (menuMapper.existsByNameExcludingId(
                values.name(),
                menu.getId())) {

            throw new BusinessException(
                    "路由名称已存在");
        }

        if (menuMapper.existsByPathExcludingId(
                values.path(),
                menu.getId())) {

            throw new BusinessException(
                    "路由地址已存在");
        }
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

    private void validatePageComponent(
            Integer menuType,
            String component) {

        if (menuType == 2 && component == null) {
            throw new BusinessException(
                    "页面菜单必须填写前端组件");
        }
    }

    private MenuValues toMenuValues(MenuRequest request) {
        return new MenuValues(
                normalizeParentId(request.parentId()),
                request.menuType(),
                request.name().trim(),
                request.title().trim(),
                request.path().trim(),
                normalizeComponent(
                        request.menuType(),
                        request.component()),
                normalizeOptional(request.redirect()),
                normalizeOptional(request.icon()),
                request.hidden(),
                request.status(),
                request.sortOrder(),
                normalizeOptional(request.description()),
                normalizeOptional(request.remark()));
    }

    private MenuValues toMenuValues(
            MenuUpdateRequest request) {
        return new MenuValues(
                normalizeParentId(request.parentId()),
                request.menuType(),
                request.name().trim(),
                request.title().trim(),
                request.path().trim(),
                normalizeComponent(
                        request.menuType(),
                        request.component()),
                normalizeOptional(request.redirect()),
                normalizeOptional(request.icon()),
                request.hidden(),
                request.status(),
                request.sortOrder(),
                normalizeOptional(request.description()),
                normalizeOptional(request.remark()));
    }

    private String normalizeComponent(
            Integer menuType,
            String component) {
        return menuType == 2
                ? normalizeOptional(component)
                : null;
    }

    private void applyMenuValues(
            Menu menu,
            MenuValues values) {
        menu.setParentId(values.parentId());
        menu.setMenuType(values.menuType());
        menu.setName(values.name());
        menu.setTitle(values.title());
        menu.setPath(values.path());
        menu.setComponent(values.component());
        menu.setRedirect(values.redirect());
        menu.setIcon(values.icon());
        menu.setHidden(values.hidden());
        menu.setStatus(values.status());
        menu.setSortOrder(values.sortOrder());
        menu.setDescription(values.description());
        menu.setRemark(values.remark());
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validateParentSelection(
            Long menuId,
            Long parentId) {

        if (parentId == null) {
            return;
        }

        Set<Long> visitedIds = new HashSet<>();
        Long currentId = parentId;
        boolean selectedParent = true;

        while (currentId != null) {
            if (Objects.equals(currentId, menuId)) {
                throw new BusinessException(
                        "不能选择当前菜单或其后代作为上级菜单");
            }

            if (!visitedIds.add(currentId)) {
                throw new BusinessException(
                        "菜单层级存在循环");
            }

            Menu currentMenu =
                    menuMapper.selectById(currentId);

            if (currentMenu == null) {
                throw new BusinessException(
                        "上级菜单不存在");
            }

            if (selectedParent
                    && !Integer.valueOf(1).equals(
                    currentMenu.getMenuType())) {

                throw new BusinessException(
                        "页面菜单不能作为上级菜单");
            }

            selectedParent = false;
            currentId = currentMenu.getParentId();
        }
    }

    private void validateMenuTypeChange(
            Menu menu,
            Integer targetMenuType) {

        if (Objects.equals(
                menu.getMenuType(),
                targetMenuType)) {
            return;
        }

        boolean directoryToPage =
                Integer.valueOf(1).equals(
                        menu.getMenuType())
                        && Integer.valueOf(2).equals(
                        targetMenuType);

        if (directoryToPage
                && menuMapper.hasChildren(
                menu.getId())) {

            throw new BusinessException(
                    "当前目录仍有子菜单，不能改为页面");
        }

        boolean pageToDirectory =
                Integer.valueOf(2).equals(
                        menu.getMenuType())
                        && Integer.valueOf(1).equals(
                        targetMenuType);

        if (pageToDirectory
                && resourceMapper.existsByMenuId(
                menu.getId())) {

            throw new BusinessException(
                    "当前页面仍有按钮权限，不能改为目录");
        }
    }

    private record MenuValues(
            Long parentId,
            Integer menuType,
            String name,
            String title,
            String path,
            String component,
            String redirect,
            String icon,
            Boolean hidden,
            Boolean status,
            Integer sortOrder,
            String description,
            String remark) {
    }
    private List<Long> normalizeDeleteIds(
            List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(
                    "请选择需要删除的菜单");
        }

        if (ids.stream().anyMatch(
                id -> id == null || id <= 0)) {

            throw new BusinessException(
                    "菜单 ID 不合法");
        }

        return ids.stream()
                .distinct()
                .toList();
    }
}
