package com.daiend.muriox.menu;

import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.menu.response.MenuTreeItem;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authority/menu")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/treePage")
    public ApiResponse<PageResult<MenuTreeItem>> treePage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String title
    ) {
        return ApiResponse.ok(
                menuService.treePage(current, size, title)
        );
    }

    @GetMapping("/tree")
    public ApiResponse<List<MenuTreeItem>> tree() {
        return ApiResponse.ok(
                menuService.tree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('menu:add')")
    public ApiResponse<Long> addMenu(@Valid @RequestBody MenuRequest menu) {
        return ApiResponse.ok(menuService.addMenu(menu));
    }

    @GetMapping("/{id}")
    public ApiResponse<Menu> getMenu(@PathVariable Long id) {
        return ApiResponse.ok(menuService.getMenu(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('menu:edit')")
    public ApiResponse<Long> updateMenu(@Valid @RequestBody MenuUpdateRequest menuUpdateRequest) {
        return ApiResponse.ok(menuService.updateMenu(menuUpdateRequest));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('menu:delete')")
    public ApiResponse<Void> deleteMenus(
            @RequestParam List<Long> ids) {

        menuService.deleteMenus(ids);

        return ApiResponse.okMessage(
                "删除菜单成功");
    }
}
