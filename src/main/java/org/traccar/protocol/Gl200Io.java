/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.helper.BitUtil;
import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

import java.util.Deque;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Device attribute keys (String unless noted):
 * <ul>
 *   <li>{@value #ATTR_IMMOBILIZER_OUTPUT} (Number) – output used for engine stop/resume: 1, 2, or 3</li>
 *   <li>{@value #ATTR_OUT2_CYCLE_DURATION_MS} / {@value #ATTR_OUT2_CYCLE_DURATION_SEC} (Number) –
 *       OUT2 cycle duration for GTOUT timed mode</li>
 *   <li>{@value #ATTR_OUT2_CYCLE_COUNT} (Number) – OUT2 cycle count for GTOUT timed mode</li>
 *   <li>{@value #ATTR_OUT3_CYCLE_DURATION_MS} / {@value #ATTR_OUT3_CYCLE_DURATION_SEC} (Number) –
 *       OUT3 cycle duration for GTOUT timed mode</li>
 *   <li>{@value #ATTR_OUT3_CYCLE_COUNT} (Number) – OUT3 cycle count for GTOUT timed mode</li>
 *   <li>{@value #ATTR_IN1_NAME}, {@value #ATTR_IN2_NAME} – friendly input labels</li>
 *   <li>{@value #ATTR_OUT1_NAME}, {@value #ATTR_OUT2_NAME}, {@value #ATTR_OUT3_NAME} – friendly output labels</li>
 * </ul>
 */
public final class Gl200Io {

    public static final String ATTR_IMMOBILIZER_OUTPUT = "immobilizerOutput";
    public static final String ATTR_OUT2_CYCLE_DURATION_MS = "out2CycleDurationMs";
    public static final String ATTR_OUT2_CYCLE_DURATION_SEC = "out2CycleDurationSec";
    public static final String ATTR_OUT2_CYCLE_COUNT = "out2CycleCount";
    public static final String ATTR_OUT3_CYCLE_DURATION_MS = "out3CycleDurationMs";
    public static final String ATTR_OUT3_CYCLE_DURATION_SEC = "out3CycleDurationSec";
    public static final String ATTR_OUT3_CYCLE_COUNT = "out3CycleCount";
    // Backward-compatible aliases.
    public static final String ATTR_OUT3_PULSE_DURATION_MS = "out3PulseDurationMs";
    public static final String ATTR_OUT3_PULSE_DURATION_SEC = "out3PulseDurationSec";
    public static final String ATTR_IN1_NAME = "in1Name";
    public static final String ATTR_IN2_NAME = "in2Name";
    public static final String ATTR_OUT1_NAME = "out1Name";
    public static final String ATTR_OUT2_NAME = "out2Name";
    public static final String ATTR_OUT3_NAME = "out3Name";

    /** Latest digital output bitmask from GTIOS/GTDOS (device id → 0–7). */
    private static final ConcurrentHashMap<Long, Long> LIVE_OUTPUT_MASKS = new ConcurrentHashMap<>();

    private Gl200Io() {
    }

    public static int getImmobilizerOutput(Device device) {
        if (device == null || device.getAttributes() == null) {
            return 1;
        }
        Object value = device.getAttributes().get(ATTR_IMMOBILIZER_OUTPUT);
        int output = 1;
        if (value instanceof Number number) {
            output = number.intValue();
        } else if (value instanceof String string && !string.isEmpty()) {
            output = Integer.parseInt(string);
        } else {
            return 1;
        }
        if (output < 1 || output > 3) {
            return 1;
        }
        return output;
    }

    public static int getOutputCycleDurationTicks(Device device, int output) {
        if (device == null || device.getAttributes() == null) {
            return 0;
        }
        String durationMsKey;
        String durationSecKey;
        if (output == 2) {
            durationMsKey = ATTR_OUT2_CYCLE_DURATION_MS;
            durationSecKey = ATTR_OUT2_CYCLE_DURATION_SEC;
        } else if (output == 3) {
            durationMsKey = ATTR_OUT3_CYCLE_DURATION_MS;
            durationSecKey = ATTR_OUT3_CYCLE_DURATION_SEC;
        } else {
            return 0;
        }
        Long durationMs = readLongAttribute(device, durationMsKey);
        if (durationMs == null && output == 3) {
            durationMs = readLongAttribute(device, ATTR_OUT3_PULSE_DURATION_MS);
        }
        if (durationMs != null) {
            long ticks = durationMs / 100;
            if (ticks < 0) {
                return 0;
            }
            return (int) Math.min(ticks, Integer.MAX_VALUE);
        }
        Long durationSec = readLongAttribute(device, durationSecKey);
        if (durationSec == null && output == 3) {
            durationSec = readLongAttribute(device, ATTR_OUT3_PULSE_DURATION_SEC);
        }
        if (durationSec != null) {
            long ticks = durationSec * 10;
            if (ticks < 0) {
                return 0;
            }
            return (int) Math.min(ticks, Integer.MAX_VALUE);
        }
        return 0;
    }

    public static int getOutputCycleCount(Device device, int output) {
        if (device == null || device.getAttributes() == null) {
            return 0;
        }
        Long value = switch (output) {
            case 2 -> readLongAttribute(device, ATTR_OUT2_CYCLE_COUNT);
            case 3 -> readLongAttribute(device, ATTR_OUT3_CYCLE_COUNT);
            default -> null;
        };
        if (value == null || value < 0) {
            return 0;
        }
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    private static Long readLongAttribute(Device device, String key) {
        Object value = device.getAttributes().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isEmpty()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static boolean useLiveOutputStatus(String model) {
        if (model == null || model.isEmpty()) {
            return false;
        }
        return switch (model.toUpperCase(Locale.ROOT)) {
            case "GV350M", "GV500MAP", "GV58LAU", "GV355CEU", "GV30CEU",
                    "GV600M", "GV600MG", "GV800W", "GV600W" -> true;
            default -> false;
        };
    }

    public static void decodeOutputMask(Position position, long outputMask) {
        position.set(Position.KEY_OUTPUT, outputMask);
        position.set(Position.PREFIX_OUT + 1, BitUtil.check(outputMask, 0));
        position.set(Position.PREFIX_OUT + 2, BitUtil.check(outputMask, 1));
        position.set(Position.PREFIX_OUT + 3, BitUtil.check(outputMask, 2));
    }

    public static boolean isAuthoritativeIoReport(String reportType) {
        return "IOS".equals(reportType) || "DOS".equals(reportType);
    }

    public static void rememberLiveOutputMask(long deviceId, long outputMask) {
        if (deviceId != 0) {
            LIVE_OUTPUT_MASKS.put(deviceId, outputMask & 0x07);
        }
    }

    public static void saveLiveOutputState(CacheManager cacheManager, Position position) {
        if (!position.hasAttribute(Position.KEY_OUTPUT)) {
            return;
        }
        rememberLiveOutputMask(position.getDeviceId(), position.getLong(Position.KEY_OUTPUT));
        position.set("ioLive", true);
        applyIoNames(cacheManager, position);
    }

    /**
     * Applies the last GTIOS/GTDOS output state onto periodic GTFRI (never from GTFRI status hex).
     */
    public static void applyLiveOutputs(CacheManager cacheManager, Position position) {
        if (position.getDeviceId() == 0) {
            return;
        }
        Long mask = LIVE_OUTPUT_MASKS.get(position.getDeviceId());
        if (mask == null && cacheManager != null) {
            mask = findLiveOutputMaskInHistory(cacheManager, position.getDeviceId());
        }
        if (mask == null) {
            return;
        }
        decodeOutputMask(position, mask);
        position.set("ioLive", true);
        applyIoNames(cacheManager, position);
    }

    private static Long findLiveOutputMaskInHistory(CacheManager cacheManager, long deviceId) {
        Deque<Position> positions = cacheManager.getPositions(deviceId);
        if (positions == null || positions.isEmpty()) {
            return null;
        }
        Iterator<Position> iterator = positions.descendingIterator();
        while (iterator.hasNext()) {
            Position candidate = iterator.next();
            if (!candidate.hasAttribute(Position.KEY_OUTPUT)) {
                continue;
            }
            String type = candidate.getString(Position.KEY_TYPE);
            if (isAuthoritativeIoReport(type) || Boolean.TRUE.equals(candidate.getBoolean("ioLive"))) {
                long mask = candidate.getLong(Position.KEY_OUTPUT) & 0x07;
                rememberLiveOutputMask(deviceId, mask);
                return mask;
            }
        }
        return null;
    }

    /**
     * Merges a single output change (GTDOS) with the last known output bitmask when available.
     */
    public static void applyOutputChange(CacheManager cacheManager, Position position, int outputId, boolean enabled) {
        long mask = 0;
        if (cacheManager != null && position.getDeviceId() != 0) {
            Position last = cacheManager.getPosition(position.getDeviceId());
            if (last != null && last.hasAttribute(Position.KEY_OUTPUT)
                    && isAuthoritativeIoReport(last.getString(Position.KEY_TYPE))) {
                mask = last.getLong(Position.KEY_OUTPUT);
            }
        }
        if (outputId >= 1 && outputId <= 3) {
            int bit = outputId - 1;
            if (enabled) {
                mask |= 1L << bit;
            } else {
                mask &= ~(1L << bit);
            }
        }
        decodeOutputMask(position, mask);
        saveLiveOutputState(cacheManager, position);
    }

    public static void applyIoNames(CacheManager cacheManager, Position position) {
        if (cacheManager == null || position.getDeviceId() == 0) {
            return;
        }
        Device device = cacheManager.getObject(Device.class, position.getDeviceId());
        if (device == null) {
            return;
        }
        applyIoName(position, Position.PREFIX_IN + 1, ATTR_IN1_NAME, device);
        applyIoName(position, Position.PREFIX_IN + 2, ATTR_IN2_NAME, device);
        applyIoName(position, Position.PREFIX_OUT + 1, ATTR_OUT1_NAME, device);
        applyIoName(position, Position.PREFIX_OUT + 2, ATTR_OUT2_NAME, device);
        applyIoName(position, Position.PREFIX_OUT + 3, ATTR_OUT3_NAME, device);
    }

    private static void applyIoName(Position position, String ioKey, String nameAttribute, Device device) {
        Object raw = device.getAttributes().get(nameAttribute);
        if (raw == null) {
            return;
        }
        String name = raw.toString().trim();
        if (name.isEmpty()) {
            return;
        }
        position.set(ioKey + "Name", name);
        if (position.hasAttribute(ioKey)) {
            Object value = position.getAttributes().get(ioKey);
            if (value instanceof Boolean booleanValue) {
                position.set(name, booleanValue);
            } else if (value instanceof Byte byteValue) {
                position.set(name, byteValue);
            } else if (value instanceof Short shortValue) {
                position.set(name, shortValue);
            } else if (value instanceof Integer integerValue) {
                position.set(name, integerValue);
            } else if (value instanceof Long longValue) {
                position.set(name, longValue);
            } else if (value instanceof Float floatValue) {
                position.set(name, floatValue);
            } else if (value instanceof Double doubleValue) {
                position.set(name, doubleValue);
            } else if (value != null) {
                position.set(name, value.toString());
            }
        }
    }

}
