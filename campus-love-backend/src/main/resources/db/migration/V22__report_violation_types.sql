-- 举报支持多选违规类型，reason 改为可选
ALTER TABLE t_report ADD COLUMN violation_types VARCHAR(256) NULL COMMENT '违规类型，逗号分隔' AFTER target_id;
ALTER TABLE t_report MODIFY COLUMN reason VARCHAR(500) NULL COMMENT '举报理由（选填）';
