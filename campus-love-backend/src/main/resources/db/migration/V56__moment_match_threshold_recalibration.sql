UPDATE t_moment_match_config
SET
    base_threshold = 75,
    prioritize_offset = 10,
    priority_offset = 5,
    priority_max_stack = 2
WHERE id = 1;
