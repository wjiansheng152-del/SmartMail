-- 清空所有租户/账户及相关业务数据（慎用：不可恢复）
-- 执行顺序：先清 tenant_default 各业务表，再清 platform 的 sys_user 与 tenant_metadata
-- 无外键，按表逐条 DELETE；清空后 IAM 重启会通过 DataInitializer 重新创建 default 租户与 admin 用户

USE tenant_default;

-- 业务表清空顺序：关联表/子表先删，主表后删
DELETE FROM audit_log;
DELETE FROM tracking_event;
DELETE FROM delivery_task;
DELETE FROM campaign_ab_assignment;
DELETE FROM campaign_batch;
DELETE FROM campaign;
DELETE FROM schedule_job;
DELETE FROM contact_group_member;
DELETE FROM contact_tag;
DELETE FROM contact_group;
DELETE FROM contact;
DELETE FROM tag;
DELETE FROM email_template;
DELETE FROM smtp_config;
DELETE FROM unsubscribe_list;
DELETE FROM blacklist;

USE platform;

-- 当前部署下 platform 仅有 tenant_metadata（sys_user 由 IAM 默认 H2 管理，未落 MySQL）
-- 若日后 IAM 改为连接 MySQL platform 库并存在 sys_user 表，可在此处增加：DELETE FROM sys_user;
DELETE FROM tenant_metadata;
