-- 仅更新最初导入且尚未被用户改名的 6 条传感器名称。
-- 在 WaveHubDB 中执行；不修改用户新增数据或用户已更改的名称。
BEGIN;
UPDATE sensor SET name = '台站 A 传感器 1', updated_at = CURRENT_TIMESTAMP
WHERE id = 1101 AND station_id = 1001 AND code = 'A01' AND name = '台站 A 阵元 1';
UPDATE sensor SET name = '台站 A 传感器 2', updated_at = CURRENT_TIMESTAMP
WHERE id = 1102 AND station_id = 1001 AND code = 'A02' AND name = '台站 A 阵元 2';
UPDATE sensor SET name = '台站 A 传感器 3', updated_at = CURRENT_TIMESTAMP
WHERE id = 1103 AND station_id = 1001 AND code = 'A03' AND name = '台站 A 阵元 3';
UPDATE sensor SET name = '台站 B 传感器 1', updated_at = CURRENT_TIMESTAMP
WHERE id = 1201 AND station_id = 1002 AND code = 'B01' AND name = '台站 B 阵元 1';
UPDATE sensor SET name = '台站 B 传感器 2', updated_at = CURRENT_TIMESTAMP
WHERE id = 1202 AND station_id = 1002 AND code = 'B02' AND name = '台站 B 阵元 2';
UPDATE sensor SET name = '台站 B 传感器 3', updated_at = CURRENT_TIMESTAMP
WHERE id = 1203 AND station_id = 1002 AND code = 'B03' AND name = '台站 B 阵元 3';
COMMIT;
