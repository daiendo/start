package com.daiend.muriox.post;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.daiend.muriox.common.PageResult;
import com.daiend.muriox.common.exception.BusinessException;
import com.daiend.muriox.datascope.DataScopeGuard;
import com.daiend.muriox.organize.Organize;
import com.daiend.muriox.organize.OrganizeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostMapper postMapper;
    private final OrganizeMapper organizeMapper;
    private final DataScopeGuard dataScopeGuard;

    public PostService(
            PostMapper postMapper,
            OrganizeMapper organizeMapper,
            DataScopeGuard dataScopeGuard) {
        this.postMapper = postMapper;
        this.organizeMapper = organizeMapper;
        this.dataScopeGuard = dataScopeGuard;
    }

    public PageResult<PostResponse> page(long current, long size, String name, Long orgId) {
        validatePageParams(current, size);
        Page<Post> postPage = postMapper.selectPostPage(current, size, name, orgId);
        List<Post> posts = postPage.getRecords();
        Map<Long, String> organizeNameById = loadOrganizeNameById(posts);

        List<PostResponse> records = posts.stream()
                .map(post -> toResponse(
                        post,
                        organizeNameById.get(post.getOrgId())))
                .toList();

        return new PageResult<>(
                records,
                postPage.getTotal(),
                postPage.getCurrent(),
                postPage.getSize(),
                postPage.getPages()
        );

    }

    @Transactional
    public Long insert(PostRequest request) {
        Long orgId = request.orgId();
        String name = request.name().trim();
        String code = request.code()
                .trim()
                .toLowerCase(Locale.ROOT);
        dataScopeGuard.assertOrgAllowed(orgId);

        if (organizeMapper.selectById(orgId) == null) {
            throw new BusinessException("所属组织不存在");
        }

        if (postMapper.existsByOrgIdAndCode(orgId, code)) {
            throw new BusinessException(
                    "同一组织下岗位编码已存在");
        }

        if (postMapper.existsByOrgIdAndName(orgId, name)) {
            throw new BusinessException(
                    "同一组织下岗位名称已存在");
        }

        Post post = new Post();
        post.setOrgId(orgId);
        post.setName(name);
        post.setCode(code);
        post.setStatus(request.status());
        post.setSortOrder(request.sortOrder());
        post.setRemark(request.remark());

        int affectedRows = postMapper.insert(post);
        if (affectedRows != 1) {
            throw new BusinessException("新增岗位失败");
        }

        return post.getId();
    }

    public PostResponse getPostById(Long postId) {
        if (postId == null || postId <= 0) {
            throw new BusinessException("岗位 ID 不合法");
        }

        Post post = findPostOrThrow(postId);

        Organize organize =
                organizeMapper.selectById(post.getOrgId());

        if (organize == null) {
            throw new BusinessException("岗位所属组织不存在");
        }

        return toResponse(post, organize.getName());
    }

    @Transactional
    public Long update(PostUpdateRequest request) {
        Post post = findPostOrThrow(request.id());
        dataScopeGuard.assertOrgAllowed(post.getOrgId());

        Long orgId = request.orgId();
        dataScopeGuard.assertOrgAllowed(orgId);
        String name = request.name().trim();
        String code = request.code().trim().toLowerCase(Locale.ROOT);

        if (organizeMapper.selectById(orgId) == null) {
            throw new BusinessException("所属组织不存在");
        }

        if (postMapper.existsByOrgIdAndCodeExcludingId(
                orgId, code, post.getId())) {
            throw new BusinessException("同一组织下岗位编码已存在");
        }

        if (postMapper.existsByOrgIdAndNameExcludingId(
                orgId, name, post.getId())) {
            throw new BusinessException("同一组织下岗位名称已存在");
        }

        post.setOrgId(orgId);
        post.setCode(code);
        post.setName(name);
        post.setStatus(request.status());
        post.setSortOrder(request.sortOrder());
        post.setRemark(normalizeOptional(request.remark()));

        if (postMapper.updateById(post) != 1) {
            throw new BusinessException("修改岗位失败");
        }

        return post.getId();
    }


    @Transactional
    public void delete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的岗位");
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new BusinessException("岗位ID不合法");
        }

        List<Long> postIds = ids.stream().distinct().toList();
        List<Post> posts = postMapper.selectByIds(postIds);
        if (posts.size() != postIds.size()) {
            throw new BusinessException("部分岗位不存在");
        }
        dataScopeGuard.assertAllOrgsAllowed(
                posts.stream()
                        .map(Post::getOrgId)
                        .toList());
        int affectedRows = postMapper.deleteByIds(postIds);

        if (affectedRows != postIds.size()) {
            throw new BusinessException("岗位删除失败");
        }

    }

    private Post findPostOrThrow(Long postId) {
        Post post = postMapper.selectById(postId);

        if (post == null) {
            throw new BusinessException("岗位不存在");
        }

        return post;
    }

    private void validatePageParams(
            long current,
            long size) {

        if (current < 1) {
            throw new BusinessException(
                    "当前页必须大于等于 1");
        }

        if (size < 1) {
            throw new BusinessException(
                    "每页数量必须大于等于 1");
        }

        if (size > 100) {
            throw new BusinessException(
                    "每页数量不能超过 100");
        }
    }

    private PostResponse toResponse(Post post, String orgName) {
        return new PostResponse(
                post.getId(),
                post.getOrgId(),
                orgName,
                post.getCode(),
                post.getName(),
                post.getSortOrder(),
                post.getStatus(),
                post.getRemark(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private Map<Long, String> loadOrganizeNameById(
            List<Post> posts) {

        List<Long> orgIds = posts.stream()
                .map(Post::getOrgId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (orgIds.isEmpty()) {
            return Map.of();
        }

        return organizeMapper.selectByIds(orgIds)
                .stream()
                .collect(Collectors.toMap(
                        Organize::getId,
                        Organize::getName));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
