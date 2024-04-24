package io.github.gaming32.squidbeatz2.byml.node;

import io.github.gaming32.squidbeatz2.byml.BymlTypes;

import java.math.BigInteger;

public final class BymlUInt64 extends BymlNumber {
    public static final BigInteger ULONG_MAX = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);

    public static final BymlUInt64 ZERO = new BymlUInt64(0L);

    private final long value;
    private BigInteger bigValue;

    private BymlUInt64(long value) {
        this.value = value;
    }

    private BymlUInt64(BigInteger value) {
        this.value = value.longValue();
        this.bigValue = value;
    }

    public static BymlUInt64 valueOf(long value) {
        return value == 0L ? ZERO : new BymlUInt64(value);
    }

    public static BymlUInt64 valueOf(BigInteger value) {
        if (value.equals(BigInteger.ZERO)) {
            return ZERO;
        }
        if (value.compareTo(BigInteger.ZERO) < 0 || value.compareTo(ULONG_MAX) > 0) {
            throw new IllegalArgumentException("BigInteger value out of range for UInt64");
        }
        return new BymlUInt64(value);
    }

    @Override
    public int getType() {
        return BymlTypes.UINT64;
    }

    @Override
    public String toString() {
        return Long.toUnsignedString(value);
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof BymlUInt64 u && u.value == value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }

    @Override
    public BigInteger toNumber() {
        return bigValue != null ? bigValue : (bigValue = toUnsignedBigInteger(value));
    }

    private static BigInteger toUnsignedBigInteger(long value) {
        if (value >= 0L) {
            return BigInteger.valueOf(value);
        }
        final int upper = (int)(value >>> 32);
        final int lower = (int)value;
        return BigInteger.valueOf(Integer.toUnsignedLong(upper)).shiftLeft(32)
            .add(BigInteger.valueOf(Integer.toUnsignedLong(lower)));
    }

    @Override
    public int intValue() {
        return (int)value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        if (value >= 0L) {
            return (float)value;
        }
        return toNumber().floatValue();
    }

    @Override
    public double doubleValue() {
        if (value >= 0L) {
            return (double)value;
        }
        return toNumber().doubleValue();
    }
}
