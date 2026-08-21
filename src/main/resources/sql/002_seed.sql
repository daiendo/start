BEGIN;

-- 系统字典
INSERT INTO sys_dict (
    code,
    name,
    value_type,
    status,
    is_system,
    remark
)
VALUES
    ('menu_node_type', '菜单节点类型', 'INTEGER', TRUE, TRUE, '系统菜单目录和页面类型'),
    ('common_status', '通用状态', 'BOOLEAN', TRUE, TRUE, '通用启用和停用状态'),
    ('user_gender', '用户性别', 'STRING', TRUE, TRUE, '用户性别选项')
ON CONFLICT DO NOTHING;

INSERT INTO sys_dict_item (
    dict_id,
    label,
    value,
    sort_order,
    status,
    is_default,
    extra
)
SELECT
    dict_info.id,
    item.label,
    item.value,
    item.sort_order,
    TRUE,
    item.is_default,
    item.extra::JSONB
FROM (
    VALUES
        ('menu_node_type', '目录', '1', 10, TRUE, '{}'),
        ('menu_node_type', '页面', '2', 20, FALSE, '{}'),
        ('common_status', '启用', 'true', 10, TRUE, '{"tagType":"success"}'),
        ('common_status', '停用', 'false', 20, FALSE, '{"tagType":"danger"}'),
        ('user_gender', '男', 'male', 10, FALSE, '{}'),
        ('user_gender', '女', 'female', 20, FALSE, '{}'),
        ('user_gender', '未知', 'unknown', 30, TRUE, '{}')
) AS item(dict_code, label, value, sort_order, is_default, extra)
JOIN sys_dict dict_info
    ON dict_info.code = item.dict_code
ON CONFLICT DO NOTHING;

-- 基础组织和岗位
INSERT INTO sys_organize (
    name,
    parent_id,
    sort_order,
    status,
    remark
)
VALUES ('Muriox', NULL, 0, TRUE, '系统根组织')
ON CONFLICT DO NOTHING;

INSERT INTO sys_organize (
    name,
    parent_id,
    sort_order,
    status,
    remark
)
SELECT
    '研发中心',
    parent_info.id,
    10,
    TRUE,
    '默认研发组织'
FROM sys_organize parent_info
WHERE parent_info.parent_id IS NULL
  AND parent_info.name = 'Muriox'
ON CONFLICT DO NOTHING;

INSERT INTO sys_post (
    org_id,
    code,
    name,
    sort_order,
    status,
    remark
)
SELECT
    organize_info.id,
    post_info.code,
    post_info.name,
    post_info.sort_order,
    TRUE,
    post_info.name
FROM (
    VALUES
        ('product', '产品经理', 10),
        ('project', '项目经理', 20)
) AS post_info(code, name, sort_order)
JOIN sys_organize organize_info
    ON organize_info.name = '研发中心'
JOIN sys_organize parent_info
    ON parent_info.id = organize_info.parent_id
   AND parent_info.name = 'Muriox'
ON CONFLICT DO NOTHING;

-- 系统菜单
INSERT INTO sys_menu (
    parent_id,
    menu_type,
    name,
    title,
    path,
    component,
    icon,
    hidden,
    status,
    sort_order,
    description,
    remark
)
VALUES (
    NULL,
    1,
    'System',
    '系统管理',
    '/system',
    'layout/index',
    'system',
    FALSE,
    TRUE,
    10,
    '系统基础配置和权限管理',
    '系统内置菜单'
)
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (
    parent_id,
    menu_type,
    name,
    title,
    path,
    component,
    hidden,
    status,
    sort_order,
    description,
    remark
)
SELECT
    parent_info.id,
    1,
    'SystemAuthority',
    '权限管理',
    '/system/authority',
    NULL,
    FALSE,
    TRUE,
    10,
    '系统权限管理',
    '系统内置菜单'
FROM sys_menu parent_info
WHERE parent_info.name = 'System'
ON CONFLICT DO NOTHING;

INSERT INTO sys_menu (
    parent_id,
    menu_type,
    name,
    title,
    path,
    component,
    icon,
    hidden,
    status,
    sort_order,
    description,
    remark
)
SELECT
    parent_info.id,
    2,
    menu_info.name,
    menu_info.title,
    menu_info.path,
    menu_info.component,
    menu_info.icon,
    FALSE,
    TRUE,
    menu_info.sort_order,
    menu_info.description,
    '系统内置菜单'
FROM (
    VALUES
        ('SystemOrganize', '组织管理', '/system/authority/organize', 'system/authority/organize/index', 'organize', 10, '维护系统组织'),
        ('SystemPost', '岗位管理', '/system/authority/post', 'system/authority/post/index', 'post', 20, '维护组织直属岗位'),
        ('SystemUser', '用户管理', '/system/authority/user', 'system/authority/user/index', 'user', 30, '维护系统用户'),
        ('SystemMenu', '菜单管理', '/system/authority/menu', 'system/authority/menu/index', 'menu', 40, '维护菜单和按钮权限资源'),
        ('SystemRole', '角色管理', '/system/authority/role', 'system/authority/role/index', 'role', 50, '维护角色及授权关系')
) AS menu_info(name, title, path, component, icon, sort_order, description)
JOIN sys_menu parent_info
    ON parent_info.name = 'SystemAuthority'
ON CONFLICT DO NOTHING;

-- 当前阶段按钮权限资源
INSERT INTO sys_resource (
    menu_id,
    name,
    code,
    status,
    sort_order,
    remark
)
SELECT
    menu_info.id,
    resource_info.name,
    resource_info.code,
    TRUE,
    resource_info.sort_order,
    resource_info.name
FROM (
    VALUES
        ('SystemMenu', '新增按钮权限', 'resource:add', 10),
        ('SystemMenu', '编辑按钮权限', 'resource:edit', 20),
        ('SystemMenu', '删除按钮权限', 'resource:delete', 30),
        ('SystemMenu', '新增菜单', 'menu:add', 40),
        ('SystemMenu', '编辑菜单', 'menu:edit', 50),
        ('SystemMenu', '删除菜单', 'menu:delete', 60),
        ('SystemOrganize', '新增组织', 'organize:add', 10),
        ('SystemOrganize', '编辑组织', 'organize:edit', 20),
        ('SystemOrganize', '删除组织', 'organize:delete', 30),
        ('SystemPost', '新增岗位', 'post:add', 10),
        ('SystemPost', '编辑岗位', 'post:edit', 20),
        ('SystemPost', '删除岗位', 'post:delete', 30),
        ('SystemRole', '新增角色', 'role:add', 10),
        ('SystemRole', '编辑角色', 'role:edit', 20),
        ('SystemRole', '删除角色', 'role:delete', 30),
        ('SystemRole', '配置角色权限', 'role:config:authority', 40),
        ('SystemRole', '角色用户授权', 'role:user:authorization', 50),
        ('SystemUser', '新增用户', 'user:add', 10),
        ('SystemUser', '编辑用户', 'user:edit', 20),
        ('SystemUser', '删除用户', 'user:delete', 30),
        ('SystemUser', '配置用户角色', 'user:role:authorization', 40),
        ('SystemUser', '上传用户头像', 'user:upload:avatar', 50),
        ('SystemUser', '重置用户密码', 'user:reset:password', 60),
        ('SystemUser', '修改用户状态', 'user:change:status', 70)
) AS resource_info(menu_name, name, code, sort_order)
JOIN sys_menu menu_info
    ON menu_info.name = resource_info.menu_name
ON CONFLICT (code) DO NOTHING;

-- 内置超级管理员角色
INSERT INTO sys_role (
    code,
    name,
    status,
    sort_order,
    remark,
    built_in
)
VALUES (
    'super_admin',
    '超级管理员',
    TRUE,
    0,
    '系统内置超级管理员角色',
    TRUE
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO sys_role_menu (
    role_id,
    menu_id
)
SELECT
    role_info.id,
    menu_info.id
FROM sys_role role_info
CROSS JOIN sys_menu menu_info
WHERE role_info.code = 'super_admin'
ON CONFLICT DO NOTHING;

INSERT INTO sys_role_resource (
    role_id,
    resource_id
)
SELECT
    role_info.id,
    resource_info.id
FROM sys_role role_info
CROSS JOIN sys_resource resource_info
WHERE role_info.code = 'super_admin'
ON CONFLICT DO NOTHING;

-- 初始账号：muriox / Muriox@123456
-- 首次登录后必须立即修改密码。重复执行不会重置现有账号密码。
INSERT INTO sys_user (
    account,
    username,
    password_hash,
    enabled,
    email,
    remark,
    sex,
    org_id,
    must_change_password,
    password_changed_at,
    built_in
)
VALUES (
    'muriox',
    'Muriox 管理员',
    '$2a$10$RI89lwKaK63iSn6GnIflguwgbC7QEZ.TojHTEzCoRfRgX0vgzPIVS',
    TRUE,
    NULL,
    '系统内置管理员',
    'unknown',
    NULL,
    TRUE,
    CURRENT_TIMESTAMP,
    TRUE
)
ON CONFLICT (account) DO NOTHING;

INSERT INTO sys_user_role (
    user_id,
    role_id
)
SELECT
    user_info.id,
    role_info.id
FROM sys_user user_info
JOIN sys_role role_info
    ON role_info.code = 'super_admin'
WHERE user_info.account = 'muriox'
ON CONFLICT DO NOTHING;

COMMIT;
