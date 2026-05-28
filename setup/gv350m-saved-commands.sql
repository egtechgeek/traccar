-- Queclink GV350M saved custom commands (tc_commands) for Traccar UI.
-- Link to a device: INSERT INTO tc_device_command (deviceid, commandid) VALUES (<device_id>, <command_id>);
-- Device IMEI 862599050523883 is tc_devices.id = 2 on this server.

INSERT INTO tc_commands (description, type, textchannel, attributes) VALUES
('Configure Output 3 (pin 7)', 'custom', 0,
 '{"data":"AT+GTCFG=gv350m,,GV350M,0,0,,,003F,2,,14E3,1,1,0,300,20,,0,0,001F,0,,,,,,24,10,5,,0,0001,FFFF$"}'),
('Output 1 ON', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,1,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Output 1 OFF', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Output 2 ON', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,1,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Output 2 OFF', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Output 3 ON', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,0,0,0,1,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Output 3 OFF', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('All outputs ON', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,1,,,1,0,0,1,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('All outputs OFF', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,0,0,0,0,0,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Output 3 pulse (~20s ON/cycle)', 'custom', 0,
 '{"data":"AT+GTOUT=gv350m,0,,,0,0,0,1,200,0,,,,,,,0,0,0,0,,,0,FFFF$"}'),
('Query IO status', 'getDeviceStatus', 0, '{}')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Example: link all commands to device id 2 (adjust deviceid as needed)
-- INSERT IGNORE INTO tc_device_command (deviceid, commandid)
-- SELECT 2, id FROM tc_commands WHERE description LIKE 'Output%' OR description IN
--   ('Configure Output 3 (pin 7)', 'All outputs ON', 'All outputs OFF', 'Query IO status');
