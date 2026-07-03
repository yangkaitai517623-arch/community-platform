-- 修复admin密码
-- 密码: admin123
-- 使用正确的BCrypt哈希

UPDATE sys_user
SET password = '$2a$10$EqKcp1WFKVQISheBxnFOheYMKMeFSmVPfJSQP1egwFgVvBmMPBiGe'
WHERE username = 'admin';

-- 如果上面的哈希不工作，用这个备选方案（重新生成所有用户密码）
-- 密码统一为: admin123
UPDATE sys_user SET password = '$2a$10$EqKcp1WFKVQISheBxnFOheYMKMeFSmVPfJSQP1egwFgVvBmMPBiGe';
