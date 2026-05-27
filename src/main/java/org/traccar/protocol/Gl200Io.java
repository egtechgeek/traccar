/*
 * Copyright 2026 Anton Tananaev (anton@traccar.org)
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

import org.traccar.model.Device;
import org.traccar.model.Position;
import org.traccar.session.cache.CacheManager;

/**
 * Device attribute keys (String unless noted):
 * <ul>
 *   <li>{@value #ATTR_IMMOBILIZER_OUTPUT} (Number) – output used for engine stop/resume: 1, 2, or 3</li>
 *   <li>{@value #ATTR_IN1_NAME}, {@value #ATTR_IN2_NAME} – friendly input labels</li>
 *   <li>{@value #ATTR_OUT1_NAME}, {@value #ATTR_OUT2_NAME}, {@value #ATTR_OUT3_NAME} – friendly output labels</li>
 * </ul>
 */
public final class Gl200Io {

    public static final String ATTR_IMMOBILIZER_OUTPUT = "immobilizerOutput";
    public static final String ATTR_IN1_NAME = "in1Name";
    public static final String ATTR_IN2_NAME = "in2Name";
    public static final String ATTR_OUT1_NAME = "out1Name";
    public static final String ATTR_OUT2_NAME = "out2Name";
    public static final String ATTR_OUT3_NAME = "out3Name";

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
            position.set(name, position.getAttributes().get(ioKey));
        }
    }

}
