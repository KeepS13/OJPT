INSERT INTO `submission` (
    `id`, `user_id`, `problem_id`, `language`, `source_code`, `status`,
    `time_ms`, `memory_kb`, `compile_message`, `judge_message`, `created_at`
) VALUES
    (
        2300000000000000001,
        1998338632572506117,
        2100000000000000001,
        'Java',
        'import java.util.*;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner scanner = new Scanner(System.in);\n        int n = scanner.nextInt();\n        int[] nums = new int[n];\n        for (int i = 0; i < n; i++) {\n            nums[i] = scanner.nextInt();\n        }\n        int target = scanner.nextInt();\n        Map<Integer, Integer> seen = new HashMap<>();\n        for (int i = 0; i < n; i++) {\n            int need = target - nums[i];\n            if (seen.containsKey(need)) {\n                System.out.println(seen.get(need) + \" \" + i);\n                return;\n            }\n            seen.put(nums[i], i);\n        }\n    }\n}',
        'AC',
        12,
        256,
        NULL,
        '示例通过记录',
        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 2 DAY)
    ),
    (
        2300000000000000002,
        1998338632572506117,
        2100000000000000002,
        'Python3',
        'def main():\n    print("not implemented")\n\nif __name__ == "__main__":\n    main()',
        'WA',
        8,
        192,
        NULL,
        '示例错误答案记录',
        DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 DAY)
    )
ON DUPLICATE KEY UPDATE
    `status` = VALUES(`status`),
    `source_code` = VALUES(`source_code`),
    `time_ms` = VALUES(`time_ms`),
    `memory_kb` = VALUES(`memory_kb`),
    `judge_message` = VALUES(`judge_message`);
