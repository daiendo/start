package com.daiend.muriox.resource;

import com.daiend.muriox.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authority/resource")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('resource:add')")
    public ApiResponse<Long> addResource(@Valid @RequestBody ResourceRequest resourceRequest) {
        return ApiResponse.ok(resourceService.addResource(resourceRequest));
    }

    @GetMapping("/list")
    public ApiResponse<List<ResourceResponse>> listResourcesByMenuId(@RequestParam Long menuId
    ) {
        return ApiResponse.ok(resourceService.listByMenuId(menuId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ResourceResponse> getResource(
            @PathVariable Long id) {

        return ApiResponse.ok(
                resourceService.getResource(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('resource:edit')")
    public ApiResponse<Long> updateResource(
            @Valid @RequestBody
            ResourceUpdateRequest request) {

        return ApiResponse.ok(
                resourceService.updateResource(request));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('resource:delete')")
    public ApiResponse<Void> deleteResources(
            @RequestParam List<Long> ids) {

        resourceService.deleteResources(ids);

        return ApiResponse.okMessage(
                "删除按钮权限成功");
    }


}
