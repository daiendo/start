package com.daiend.muriox.organize;

import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authority/organize")
public class OrganizeController {

    private final OrganizeService organizeService;
    public OrganizeController(OrganizeService organizeService) {
        this.organizeService = organizeService;
    }

    @GetMapping("/treePage")
    public ApiResponse<PageResult<OrganizeTreeItem>> treePage(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name) {
        return ApiResponse.ok(organizeService.treePage(current, size, name));
    }

    @GetMapping("/tree")
    public ApiResponse<List<OrganizeTreeItem>> tree() {
        return ApiResponse.ok(organizeService.tree());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('organize:add')")
    public ApiResponse<Long> addOrganize(@Valid @RequestBody OrganizeRequest organizeRequest) {
        return ApiResponse.ok(organizeService.addOrganize(organizeRequest));
    }


    @GetMapping("/{id}")
    public ApiResponse<OrganizeResponse> getOrganize(
            @PathVariable Long id) {
        return ApiResponse.ok(organizeService.getOrganize(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('organize:edit')")
    public ApiResponse<Long> updateOrganize(@Valid @RequestBody OrganizeUpdateRequest request) {
        return ApiResponse.ok(organizeService.updateOrganize(request));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('organize:delete')")
    public ApiResponse<Void> deleteOrganize(@RequestParam List<Long> ids) {
        organizeService.deleteOrganize(ids);
        return ApiResponse.okMessage("删除成功");
    }
}
