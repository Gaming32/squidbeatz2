package io.github.gaming32.squidbeatz2.util;

import com.google.common.collect.MapMaker;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public class Util {
    private static final Map<Class<?>, Constructor<?>> CONSTRUCTOR_CACHE = new MapMaker().weakKeys().makeMap();

    public static Charset consoleCharset() {
        final Console console = System.console();
        if (console != null) {
            return console.charset();
        }
        return Charset.forName(System.getProperty("stdout.encoding"));
    }

    public static int readInt(InputStream is, boolean bigEndian) throws IOException {
        final int value = is.read() | (is.read() << 8) | (is.read() << 16) | (is.read() << 24);
        return bigEndian ? Integer.reverseBytes(value) : value;
    }

    public static int readShort(InputStream is, boolean bigEndian) throws IOException {
        final int value = is.read() | (is.read() << 8);
        return bigEndian ? Short.reverseBytes((short)value) & 0xffff : value;
    }

    public static String readString(InputStream is, int length) throws IOException {
        return new String(is.readNBytes(length), StandardCharsets.ISO_8859_1);
    }

    public static long readLong(InputStream is, boolean bigEndian) throws IOException {
        return ByteBuffer.wrap(is.readNBytes(8))
            .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN)
            .getLong();
    }

    public static long[] readLongArray(InputStream is, boolean bigEndian, int length) throws IOException {
        final ByteBuffer buffer = ByteBuffer.wrap(is.readNBytes(8 * length))
            .order(bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
        final long[] result = new long[length];
        for (int i = 0; i < length; i++) {
            result[i] = buffer.getLong();
        }
        return result;
    }

    public static void writeInt(OutputStream os, int value, boolean bigEndian) throws IOException {
        if (bigEndian) {
            value = Integer.reverseBytes(value);
        }
        os.write(value);
        os.write(value >> 8);
        os.write(value >> 16);
        os.write(value >>> 24);
    }

    public static void writeShort(OutputStream os, int value, boolean bigEndian) throws IOException {
        if (bigEndian) {
            value = Short.reverseBytes((short)value) & 0xffff;
        }
        os.write(value);
        os.write(value >> 8);
    }

    public static void writeString(OutputStream os, String value) throws IOException {
        os.write(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    public static byte[] toByteArray(short[] array, boolean bigEndian) {
        final byte[] output = new byte[array.length * 2];
        if (!bigEndian) {
            for (int i = 0; i < array.length; i++) {
                output[i * 2] = (byte)array[i];
                output[i * 2 + 1] = (byte)(array[i] >>> 8);
            }
        } else {
            for (int i = 0; i < array.length; i++) {
                output[i * 2] = (byte)(array[i] >>> 8);
                output[i * 2 + 1] = (byte)array[i];
            }
        }
        return output;
    }

    public static short[] toShortArray(byte[] array, boolean bigEndian) {
        final int length = (array.length + 1) / 2;
        final short[] output = new short[length];

        if (!bigEndian) {
            for (int i = 0; i < length; i++) {
                output[i] = (short)((array[i * 2] & 0xff) << 8 | (array[i * 2 + 1] & 0xff));
            }
        } else {
            for (int i = 0; i < length; i++) {
                output[i] = (short)((array[i * 2] & 0xff) | (array[i * 2 + 1] & 0xff) << 8);
            }
        }

        return output;
    }

    public static short[] interleave(short[][] inputs, int interleaveSize) {
        return interleave(inputs, interleaveSize, -1);
    }

    public static short[] interleave(short[][] inputs, int interleaveSize, int outputSize) {
        final int inputSize = inputs[0].length;
        if (outputSize == -1) {
            outputSize = inputSize;
        }

        if (Arrays.stream(inputs).anyMatch(x -> x.length != inputSize)) {
            throw new IllegalArgumentException("Inputs must be of equal length");
        }

        final int inputCount = inputs.length;
        final int inBlockCount = (inputSize - 1 + interleaveSize) / interleaveSize;
        final int outBlockCount = (outputSize - 1 + interleaveSize) / interleaveSize;
        final int lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
        final int lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
        final int blocksToCopy = Math.min(inBlockCount, outBlockCount);

        final short[] output = new short[outputSize * inputCount];

        for (int b = 0; b < blocksToCopy; b++) {
            final int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
            final int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
            final int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

            for (int i = 0; i < inputCount; i++) {
                System.arraycopy(
                    inputs[i], interleaveSize * b,
                    output, interleaveSize * b * inputCount + currentOutputInterleaveSize * i,
                    bytesToCopy
                );
            }
        }

        return output;
    }

    public static short[][] deInterleave(short[] input, int interleaveSize, int outputCount) {
        return deInterleave(input, interleaveSize, outputCount, -1);
    }

    public static short[][] deInterleave(short[] input, int interleaveSize, int outputCount, int outputSize) {
        if (input.length % outputCount != 0) {
            throw new IllegalArgumentException("The input array length (" + input.length + ") must be divisible by the number of outputs.");
        }

        final int inputSize = input.length / outputCount;
        if (outputSize == -1) {
            outputSize = inputSize;
        }

        final int inBlockCount = (inputSize - 1 + interleaveSize) / interleaveSize;
        final int outBlockCount = (outputSize - 1 + interleaveSize) / interleaveSize;
        final int lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
        final int lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
        final int blocksToCopy = Math.min(inBlockCount, outBlockCount);

        final short[][] outputs = new short[outputCount][outputSize];

        for (int b = 0; b < blocksToCopy; b++) {
            final int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
            final int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
            final int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

            for (int o = 0; o < outputCount; o++) {
                System.arraycopy(
                    input,
                    interleaveSize * b * outputCount + currentInputInterleaveSize * o,
                    outputs[o],
                    interleaveSize * b,
                    bytesToCopy
                );
            }
        }

        return outputs;
    }

    public static byte[][] deInterleave(InputStream is, int length, int interleaveSize, int outputCount) throws IOException {
        return deInterleave(is, length, interleaveSize, outputCount, -1);
    }

    public static byte[][] deInterleave(InputStream is, int length, int interleaveSize, int outputCount, int outputSize) throws IOException {
        if (length % outputCount != 0) {
            throw new IllegalArgumentException("The input length (" + length + ") must be divisible by the number of outputs.");
        }

        final int inputSize = length / outputCount;
        if (outputSize == -1) {
            outputSize = inputSize;
        }

        final int inBlockCount = (inputSize - 1 + interleaveSize) / interleaveSize;
        final int outBlockCount = (outputSize - 1 + interleaveSize) / interleaveSize;
        final int lastInputInterleaveSize = inputSize - (inBlockCount - 1) * interleaveSize;
        final int lastOutputInterleaveSize = outputSize - (outBlockCount - 1) * interleaveSize;
        final int blocksToCopy = Math.min(inBlockCount, outBlockCount);

        final byte[][] outputs = new byte[outputCount][outputSize];

        for (int b = 0; b < blocksToCopy; b++) {
            final int currentInputInterleaveSize = b == inBlockCount - 1 ? lastInputInterleaveSize : interleaveSize;
            final int currentOutputInterleaveSize = b == outBlockCount - 1 ? lastOutputInterleaveSize : interleaveSize;
            final int bytesToCopy = Math.min(currentInputInterleaveSize, currentOutputInterleaveSize);

            for (int o = 0; o < outputCount; o++) {
                is.readNBytes(outputs[o], interleaveSize * b, bytesToCopy);
                if (bytesToCopy < currentInputInterleaveSize) {
                    is.skipNBytes(currentInputInterleaveSize - bytesToCopy);
                }
            }
        }

        return outputs;
    }

    public static byte[] shortToInterleavedByte(short[][] input) {
        final int inputCount = input.length;
        final int length = input[0].length;
        final byte[] output = new byte[inputCount * length * 2];

        for (int i = 0; i < length; i++) {
            for (int j = 0; j < inputCount; j++) {
                final int offset = (i * inputCount + j) * 2;
                output[offset] = (byte)input[j][i];
                output[offset + 1] = (byte)(input[j][i] >> 8);
            }
        }

        return output;
    }

    @SuppressWarnings("unchecked")
    public static <T> T createJaggedArray(Class<T> type, int... lengths) {
        return (T)Util.initializeJaggedArray(type.componentType(), 0, lengths);
    }

    private static Object initializeJaggedArray(Class<?> type, int index, int[] lengths) {
        final Object array = Array.newInstance(type, lengths[index]);

        final Class<?> elementType = type.componentType();
        if (elementType == null) {
            return array;
        }

        for (int i = 0; i < lengths[index]; i++) {
            Array.set(array, i, initializeJaggedArray(elementType, index + 1, lengths));
        }

        return array;
    }

    public static <T> T create(Class<T> clazz) {
        @SuppressWarnings("unchecked")
        final var constructor = (Constructor<T>)CONSTRUCTOR_CACHE.computeIfAbsent(clazz, c -> {
            try {
                return c.getConstructor();
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("No empty constructor for " + c, e);
            }
        });
        try {
            return constructor.newInstance();
        } catch (InvocationTargetException e) {
            throw rethrow(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Non-instantiable " + clazz, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T rethrow(Throwable t) throws T {
        throw (T)t;
    }
}
