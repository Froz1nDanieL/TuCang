-- auto-generated definition
create table picture
(
    id            bigint auto_increment comment 'id'
        primary key,
    url           varchar(512)                       not null comment '图片 url',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '简介',
    category      varchar(64)                        null comment '分类',
    tags          varchar(512)                       null comment '标签（JSON 数组）',
    picSize       bigint                             null comment '图片体积',
    picWidth      int                                null comment '图片宽度',
    picHeight     int                                null comment '图片高度',
    picScale      double                             null comment '图片宽高比例',
    picFormat     varchar(32)                        null comment '图片格式',
    userId        bigint                             not null comment '创建用户 id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    reviewStatus  int      default 0                 not null comment '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息',
    reviewerId    bigint                             null comment '审核人 ID',
    reviewTime    datetime                           null comment '审核时间',
    thumbnailUrl  varchar(512)                       null comment '缩略图 url',
    spaceId       bigint                             null comment '空间 id（为空表示公共空间）',
    likeCount     int      default 0                 null comment '点赞数',
    favoriteCount int      default 0                 null comment '收藏数'
)
    comment '图片' collate = utf8mb4_unicode_ci;

create index idx_category
    on picture (category);

create index idx_introduction
    on picture (introduction);

create index idx_name
    on picture (name);

create index idx_reviewStatus
    on picture (reviewStatus);

create index idx_spaceId
    on picture (spaceId);

create index idx_tags
    on picture (tags);

create index idx_userId
    on picture (userId);

-- auto-generated definition
create table ai_gen_history
(
    id         bigint auto_increment comment 'id'
        primary key,
    userId     bigint                             not null comment '用户ID',
    prompt     varchar(512)                       not null comment '用户提示词',
    taskId     varchar(255)                       null comment '外部任务ID',
    imageUrl   varchar(512)                       null comment '生成图片URL',
    imageSize  varchar(32)                        null comment '图片尺寸',
    status     tinyint  default 1                 null comment '状态: 1-成功, 2-失败',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间'
);

create index idx_create_time
    on ai_gen_history (createTime);

create index idx_user_id_create_time
    on ai_gen_history (userId, createTime);

-- auto-generated definition
create table picture_album
(
    id           bigint                             not null comment '收藏夹ID'
        primary key,
    name         varchar(255)                       not null comment '收藏夹名称',
    description  text                               null comment '收藏夹描述',
    userId       bigint                             not null comment '创建用户ID',
    isPublic     tinyint  default 0                 null comment '是否公开 (0-私有, 1-公开)',
    pictureCount int      default 0                 null comment '图片数量',
    viewCount    int      default 0                 null comment '浏览次数',
    createTime   datetime default CURRENT_TIMESTAMP null comment '创建时间'
);

create index idx_is_public_create_time
    on picture_album (isPublic, createTime);

create index idx_user_id
    on picture_album (userId);

-- auto-generated definition
create table space
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    spaceType  int      default 0                 not null comment '空间类型：0-私有 1-团队'
)
    comment '空间' collate = utf8mb4_unicode_ci;

create index idx_spaceLevel
    on space (spaceLevel);

create index idx_spaceName
    on space (spaceName);

create index idx_spaceType
    on space (spaceType);

create index idx_userId
    on space (userId);

-- auto-generated definition
create table space_user
(
    id         bigint auto_increment comment 'id'
        primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_spaceId_userId
        unique (spaceId, userId)
)
    comment '空间用户关联' collate = utf8mb4_unicode_ci;

create index idx_spaceId
    on space_user (spaceId);

create index idx_userId
    on space_user (userId);

-- auto-generated definition
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    constraint uk_userAccount
        unique (userAccount)
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_userName
    on user (userName);

-- auto-generated definition
create table user_picture_interaction
(
    id         bigint                             not null comment '主键ID'
        primary key,
    userId     bigint                             not null comment '用户ID',
    pictureId  bigint                             not null comment '图片ID',
    type       tinyint                            not null comment '互动类型（0-点赞 1-收藏）',
    createTime datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    albumId    bigint                             null comment '收藏夹ID',
    constraint uniq_user_picture_type_album
        unique (userId, pictureId, type, albumId)
)
    comment '用户图片互动记录表';

create index idx_picture_type
    on user_picture_interaction (pictureId, type);

create index idx_user_type
    on user_picture_interaction (userId, type);



