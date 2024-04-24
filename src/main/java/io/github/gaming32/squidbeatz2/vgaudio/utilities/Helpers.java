package io.github.gaming32.squidbeatz2.vgaudio.utilities;

public class Helpers {
    private static final byte[] SIGNED_NIBBLES = {0, 1, 2, 3, 4, 5, 6, 7, -8, -7, -6, -5, -4, -3, -2, -1};

    public static int getNextMultiple(int value, int multiple) {
        if (multiple <= 0) {
            return value;
        }

        if (value % multiple == 0) {
            return value;
        }

        return value + multiple - value % multiple;
    }

    public static boolean loopPointsAreAligned(int loopStart, int alignmentMultiple) {
        return !(alignmentMultiple != 0 && loopStart % alignmentMultiple != 0);
    }

    public static byte getHighNibble(byte value) {
        return (byte)((value >> 4) & 0xF);
    }

    public static byte getLowNibble(byte value) {
        return (byte)(value & 0xF);
    }

    public static byte getHighNibbleSigned(byte value) {
        return SIGNED_NIBBLES[(value >> 4) & 0xF];
    }

    public static byte getLowNibbleSigned(byte value) {
        return SIGNED_NIBBLES[value & 0xF];
    }

    public static byte combineNibbles(int high, int low) {
        return (byte)((high << 4) | (low & 0xF));
    }

    public static short clamp16(int value) {
        if (value > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (value < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short)value;
    }

    public static byte clamp4(int value) {
        if (value > 7) {
            return 7;
        }
        if (value < -8) {
            return -8;
        }
        return (byte)value;
    }
}
