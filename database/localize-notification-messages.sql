-- Convert existing English service notification titles and contents to Chinese.
-- Run once on an existing database if older notifications were generated before localization.

UPDATE notification
SET title = CASE title
    WHEN 'Request deleted' THEN '需求已删除'
    WHEN 'Errand request accepted' THEN '跑腿需求已接单'
    WHEN 'Errand request assigned' THEN '跑腿需求已分配'
    WHEN 'New errand task' THEN '新的跑腿任务'
    WHEN 'Errand task reassigned' THEN '跑腿任务已改派'
    WHEN 'Errand request returned to pending' THEN '跑腿需求已回到待接单'
    WHEN 'Errand request completed' THEN '跑腿需求已完成'
    WHEN 'Repair request accepted' THEN '检修需求已接单'
    WHEN 'Repair request assigned' THEN '检修需求已分配'
    WHEN 'New repair task' THEN '新的检修任务'
    WHEN 'Repair task reassigned' THEN '检修任务已改派'
    WHEN 'Repair request returned to pending' THEN '检修需求已回到待接单'
    WHEN 'Repair request completed' THEN '检修需求已确认完成'
    ELSE title
END
WHERE title IN (
    'Request deleted',
    'Errand request accepted',
    'Errand request assigned',
    'New errand task',
    'Errand task reassigned',
    'Errand request returned to pending',
    'Errand request completed',
    'Repair request accepted',
    'Repair request assigned',
    'New repair task',
    'Repair task reassigned',
    'Repair request returned to pending',
    'Repair request completed'
);

UPDATE notification
SET content = REPLACE(content, 'Your errand request has been deleted: ', '您的跑腿需求已被管理员删除：')
WHERE content LIKE 'Your errand request has been deleted:%';

UPDATE notification
SET content = REPLACE(content, 'Your repair request has been deleted: ', '您的检修需求已被管理员删除：')
WHERE content LIKE 'Your repair request has been deleted:%';

UPDATE notification
SET content = REPLACE(content, 'Your errand request has been accepted: ', '您的跑腿需求已被接单：')
WHERE content LIKE 'Your errand request has been accepted:%';

UPDATE notification
SET content = REPLACE(content, 'Your repair request has been accepted: ', '您的检修需求已被接单：')
WHERE content LIKE 'Your repair request has been accepted:%';

UPDATE notification
SET content = REPLACE(content, 'Your errand request has been assigned: ', '您的跑腿需求已由管理员分配：')
WHERE content LIKE 'Your errand request has been assigned:%';

UPDATE notification
SET content = REPLACE(content, 'Your repair request has been assigned: ', '您的检修需求已由管理员分配：')
WHERE content LIKE 'Your repair request has been assigned:%';

UPDATE notification
SET content = REPLACE(content, 'An errand request has been assigned to you: ', '管理员已为您分配跑腿任务：')
WHERE content LIKE 'An errand request has been assigned to you:%';

UPDATE notification
SET content = REPLACE(content, 'A repair request has been assigned to you: ', '管理员已为您分配检修任务：')
WHERE content LIKE 'A repair request has been assigned to you:%';

UPDATE notification
SET content = REPLACE(content, 'Your errand task has been reassigned: ', '您的跑腿任务已被管理员改派：')
WHERE content LIKE 'Your errand task has been reassigned:%';

UPDATE notification
SET content = REPLACE(content, 'Your repair task has been reassigned: ', '您的检修任务已被管理员改派：')
WHERE content LIKE 'Your repair task has been reassigned:%';

UPDATE notification
SET content = REPLACE(content, 'The accepted errand request has been cancelled: ', '接单人已取消接单，需求已回到待接单：')
WHERE content LIKE 'The accepted errand request has been cancelled:%';

UPDATE notification
SET content = REPLACE(content, 'The accepted repair request has been cancelled: ', '接单人已取消接单，需求已回到待接单：')
WHERE content LIKE 'The accepted repair request has been cancelled:%';

UPDATE notification
SET content = REPLACE(content, 'Your errand request has been completed: ', '您的跑腿需求已完成：')
WHERE content LIKE 'Your errand request has been completed:%';

UPDATE notification
SET content = REPLACE(content, 'The publisher has confirmed completion: ', '发布者已确认检修完成：')
WHERE content LIKE 'The publisher has confirmed completion:%';

UPDATE notification
SET content = REPLACE(content, '. Reason: ', '。原因：')
WHERE content LIKE '%. Reason:%';
