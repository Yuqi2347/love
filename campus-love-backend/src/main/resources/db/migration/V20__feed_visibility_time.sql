-- 动态可见时间：3=近三天，30=近一月，180=近半年，-1=展示全部（默认）
ALTER TABLE t_user ADD COLUMN feed_visibility_time INT DEFAULT -1 COMMENT '动态可见时间(天)：3=近三天，30=近一月，180=近半年，-1=全部' AFTER feed_visibility;
