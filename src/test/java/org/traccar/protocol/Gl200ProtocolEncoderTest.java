package org.traccar.protocol;

import org.junit.jupiter.api.Test;
import org.traccar.ProtocolTest;
import org.traccar.model.Command;
import org.traccar.model.Device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

public class Gl200ProtocolEncoderTest extends ProtocolTest {

    private static final String GV350M_GTOUT_TAIL = ",,,,,,,0,0,0,0,,,0,FFFF$";

    @Test
    public void testEncodeLegacy() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DEVICE_PASSWORD, "gl200");

        assertEquals(
                "AT+GTOUT=gl200,1,,,0,0,0,0,0,0,0,,,,,,,FFFF$",
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput1On() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,1,,,0,0,0,0,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput1Off() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,0,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput2On() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(Gl200Io.ATTR_IMMOBILIZER_OUTPUT, 2));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,1,0,0,0,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput2OnWithCycleAttributes() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(
                Gl200Io.ATTR_IMMOBILIZER_OUTPUT, 2,
                Gl200Io.ATTR_OUT2_CYCLE_DURATION_MS, 20000,
                Gl200Io.ATTR_OUT2_CYCLE_COUNT, 5));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,1,200,5,0,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput2Off() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(Gl200Io.ATTR_IMMOBILIZER_OUTPUT, 2));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,0,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput3On() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(Gl200Io.ATTR_IMMOBILIZER_OUTPUT, 3));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,1,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput3OnWithPulseDurationMsAttribute() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(
                Gl200Io.ATTR_IMMOBILIZER_OUTPUT, 3,
                Gl200Io.ATTR_OUT3_PULSE_DURATION_MS, 20000));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_STOP);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,1,200,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutput3Off() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(Gl200Io.ATTR_IMMOBILIZER_OUTPUT, 3));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_ENGINE_RESUME);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,0,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutputControl3OnWithPulseDurationSecAttribute() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(Gl200Io.ATTR_OUT3_PULSE_DURATION_SEC, 20));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");
        command.set(Command.KEY_INDEX, 3);
        command.set(Command.KEY_DATA, 1);

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,1,200,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mOutputControl3OnWithCycleCountAttribute() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");
        when(device.getAttributes()).thenReturn(java.util.Map.of(
                Gl200Io.ATTR_OUT3_CYCLE_DURATION_MS, 20000,
                Gl200Io.ATTR_OUT3_CYCLE_COUNT, 5));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_OUTPUT_CONTROL);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");
        command.set(Command.KEY_INDEX, 3);
        command.set(Command.KEY_DATA, 1);

        assertEquals(
                "AT+GTOUT=gv350m,0,,,0,0,0,1,200,5" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeGv350mAllOutputsOn() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA,
                "AT+GTOUT=gv350m,1,,,1,0,0,1,0,0,,,,,,,0,0,0,0,,,0,FFFF$");

        assertEquals(
                "AT+GTOUT=gv350m,1,,,1,0,0,1,0,0" + GV350M_GTOUT_TAIL,
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeQueryIoStatus() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Device device = encoder.getCacheManager().getObject(Device.class, 1);
        when(device.getModel()).thenReturn("GV350M");

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_GET_DEVICE_STATUS);
        command.set(Command.KEY_DEVICE_PASSWORD, "gv350m");

        assertEquals("AT+GTRTO=gv350m,A,,,,,,FFFF$", encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeDefaultPassword() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_REBOOT_DEVICE);

        assertEquals("AT+GTRTO=gl200,3,,,,,,FFFF$", encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeCustom() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "AT+GTRTO=gv350m,1,,,,,,FFFF$");

        assertEquals("AT+GTRTO=gv350m,1,,,,,,FFFF$", encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeCustomAddsTerminator() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);
        command.set(Command.KEY_DATA, "AT+GTQSS=gv350m,apn,,,4,,0,192.168.1.1,5023,,,,,,,FFFF");

        assertEquals(
                "AT+GTQSS=gv350m,apn,,,4,,0,192.168.1.1,5023,,,,,,,FFFF$",
                encoder.encodeCommand(command));
    }

    @Test
    public void testEncodeCustomEmpty() throws Exception {

        var encoder = inject(new Gl200ProtocolEncoder(null));

        Command command = new Command();
        command.setDeviceId(1);
        command.setType(Command.TYPE_CUSTOM);

        assertNull(encoder.encodeCommand(command));
    }

}
