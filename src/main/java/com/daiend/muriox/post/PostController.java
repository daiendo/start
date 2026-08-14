package com.daiend.muriox.post;

import com.daiend.muriox.common.ApiResponse;
import com.daiend.muriox.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/authority/post")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<PostResponse>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long orgId) {

        return ApiResponse.ok(postService.page(current, size, name, orgId));

    }

    @PostMapping
    @PreAuthorize("hasAuthority('post:add')")
    public ApiResponse<Long> save(@Valid @RequestBody PostRequest postRequest) {
        return ApiResponse.ok(postService.insert(postRequest));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPostById(@PathVariable Long id) {
        return ApiResponse.ok(postService.getPostById(id));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('post:edit')")
    public ApiResponse<Long> update(@Valid @RequestBody PostUpdateRequest postUpdateRequest) {
        return ApiResponse.ok(postService.update(postUpdateRequest));
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('post:delete')")
    public ApiResponse<Void> delete(@RequestParam List<Long> ids) {
        postService.delete(ids);
        return ApiResponse.okMessage("删除岗位成功");
    }
}
