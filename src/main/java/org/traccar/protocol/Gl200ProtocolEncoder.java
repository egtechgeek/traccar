/*
 * Copyright 2016 - 2026 Anton Tananaev (anton@traccar.org)
 * Contributed by EGTechGEEK
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import org.traccar.StringProtocolEncoder;
import org.traccar.model.Command;
import org.traccar.model.Device;
import org.traccar.Protocol;

import java.util.Locale;

public class Gl200ProtocolEncoder extends StringProtocolEncoder {

    public Gl200ProtocolEncoder(Protocol protocol) {
        super(protocol);
    }

    private static boolean useExtendedGtout(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        return switch (model.toUpperCase(Locale.ROOT)) {
            case "GV350M", "GV500MAP", "GV58LAU", "GV355CEU", "GV30CEU",
                    "GV600M", "GV600MG", "GV800W", "GV600W" -> true;
            default -> false;
        };
    }

    private String getDefaultPassword(long deviceId) {
        String model = getDeviceModel(deviceId);
        if (model != null) {
            return switch (model.toUpperCase(Locale.ROOT)) {
                case "GV350M", "GV355CEU", "GV30CEU", "GV58LAU", "GV600M", "GV600MG", "GV800W", "GV600W" -> "gv350m";
                case "GV500MAP" -> "gv500map";
                default -> "gl200";
            };
        }
        return "gl200";
    }

    private String formatGtout(Command command, boolean active) {
        Device device = getCacheManager().getObject(Device.class, command.getDeviceId());
        int immobilizer = Gl200Io.getImmobilizerOutput(device);
        int out1 = immobilizer == 1 && active ? 1 : 0;
        int out2 = immobilizer == 2 && active ? 1 : 0;
        int out3 = immobilizer == 3 && active ? 1 : 0;
        String model = getDeviceModel(command.getDeviceId());
        if (useExtendedGtout(model)) {
            return formatCommand(command, String.format(
                    "AT+GTOUT=%%s,%d,,,%d,0,0,%d,0,0,,,,,,,0,2,0,0,,,FFFF$",
                    out1, out2, out3),
                    Command.KEY_DEVICE_PASSWORD);
        }
        int legacyStatus = immobilizer == 1 && active ? 1 : 0;
        return formatCommand(command, String.format(
                "AT+GTOUT=%%s,%d,,,0,0,0,0,0,0,0,,,,,,,FFFF$", legacyStatus),
                Command.KEY_DEVICE_PASSWORD);
    }

    @Override
    protected Object encodeCommand(Command command) {

        initDevicePassword(command, getDefaultPassword(command.getDeviceId()));

        return switch (command.getType()) {
            case Command.TYPE_CUSTOM -> {
                String data = command.getString(Command.KEY_DATA);
                if (data == null || data.isEmpty()) {
                    yield null;
                }
                yield data.endsWith("$") ? data : data + "$";
            }
            case Command.TYPE_POSITION_SINGLE -> formatCommand(
                    command, "AT+GTRTO=%s,1,,,,,,FFFF$", Command.KEY_DEVICE_PASSWORD);
            case Command.TYPE_ENGINE_STOP -> formatGtout(command, true);
            case Command.TYPE_ENGINE_RESUME -> formatGtout(command, false);
            case Command.TYPE_IDENTIFICATION -> formatCommand(
                    command, "AT+GTRTO=%s,8,,,,,,FFFF$", Command.KEY_DEVICE_PASSWORD);
            case Command.TYPE_REBOOT_DEVICE -> formatCommand(
                    command, "AT+GTRTO=%s,3,,,,,,FFFF$", Command.KEY_DEVICE_PASSWORD);
            default -> null;
        };
    }

}
