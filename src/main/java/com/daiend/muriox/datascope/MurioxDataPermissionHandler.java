package com.daiend.muriox.datascope;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.daiend.muriox.auth.CurrentUser;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class MurioxDataPermissionHandler
        implements MultiDataPermissionHandler {

    private static final List<String>
            TARGET_MAPPER_PREFIXES = List.of(
            "com.daiend.muriox.user.UserMapper.",
            "com.daiend.muriox.post.PostMapper.",
            "com.daiend.muriox.organize.OrganizeMapper."
    );

    /*
     * 组织树骨架查询自身已经包含安全过滤，
     * 后续 6.2 实现该 Mapper 方法。
     */
    private static final Set<String>
            IGNORED_STATEMENT_IDS = Set.of(
            "com.daiend.muriox.organize."
                    + "OrganizeMapper."
                    + "findAllowedAndAncestorOrganizes",
            "com.daiend.muriox.organize."
                    + "OrganizeMapper.hasChildrenByParentIds",
            "com.daiend.muriox.organize."
                    + "OrganizeMapper.existsByNameAndParentId",
            "com.daiend.muriox.organize."
                    + "OrganizeMapper."
                    + "existsByNameAndParentIdExcludingId",
            "com.daiend.muriox.user."
                    + "UserMapper.existsByAccount",
            "com.daiend.muriox.user."
                    + "UserMapper.existsByEmail",
            "com.daiend.muriox.user."
                    + "UserMapper.existsByMobile",
            "com.daiend.muriox.user."
                    + "UserMapper.existsByAccountExcludingId",
            "com.daiend.muriox.user."
                    + "UserMapper.existsByEmailExcludingId",
            "com.daiend.muriox.user."
                    + "UserMapper.existsByMobileExcludingId"
    );

    private static final Map<String, String>
            TABLE_SCOPE_COLUMNS = Map.of(
            "sys_user", "org_id",
            "sys_post", "org_id",
            "sys_organize", "id"
    );

    /*
     * 使用 ObjectProvider 避免：
     * interceptor -> service -> mapper ->
     * SqlSessionFactory -> interceptor 的初始化循环。
     */
    private final ObjectProvider<UserDataScopeService>
            dataScopeServiceProvider;

    public MurioxDataPermissionHandler(
            ObjectProvider<UserDataScopeService>
                    dataScopeServiceProvider) {

        this.dataScopeServiceProvider =
                dataScopeServiceProvider;
    }

    @Override
    public Expression getSqlSegment(
            Table table,
            Expression where,
            String mappedStatementId) {

        if (!supportsStatement(mappedStatementId)) {
            return null;
        }

        String tableName =
                normalizeTableName(table.getName());

        String scopeColumn =
                TABLE_SCOPE_COLUMNS.get(tableName);

        if (scopeColumn == null) {
            return null;
        }

        Long currentUserId =
                getCurrentUserId();

        /*
         * 登录阶段还没有 CurrentUser。
         * 未认证请求仍由 Spring Security 阻止，
         * 此处不能影响账号和认证权限查询。
         */
        if (currentUserId == null) {
            return null;
        }

        UserDataScope dataScope =
                dataScopeServiceProvider
                        .getObject()
                        .getDataScope(currentUserId);

        if (dataScope.all()) {
            return null;
        }

        if (dataScope.isDenied()) {
            return alwaysFalseExpression();
        }

        Column column =
                new Column(
                        resolveQualifier(table)
                                + "."
                                + scopeColumn);

        List<LongValue> values =
                dataScope.orgIds()
                        .stream()
                        .map(LongValue::new)
                        .toList();

        return new InExpression(
                column,
                new ParenthesedExpressionList<>(
                        values));
    }

    private boolean supportsStatement(
            String mappedStatementId) {

        if (mappedStatementId == null
                || IGNORED_STATEMENT_IDS.contains(
                mappedStatementId)) {

            return false;
        }

        return TARGET_MAPPER_PREFIXES
                .stream()
                .anyMatch(
                        mappedStatementId::startsWith);
    }

    private Long getCurrentUserId() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal
                instanceof CurrentUser currentUser)) {

            return null;
        }

        return currentUser.id();
    }

    private Expression alwaysFalseExpression() {
        return new EqualsTo(
                new LongValue(1),
                new LongValue(0));
    }

    private String resolveQualifier(Table table) {
        if (table.getAlias() != null
                && table.getAlias().getName() != null
                && !table.getAlias()
                .getName()
                .isBlank()) {

            return table.getAlias().getName();
        }

        return table.getName();
    }

    private String normalizeTableName(
            String tableName) {

        if (tableName == null) {
            return "";
        }

        return tableName
                .replace("\"", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
