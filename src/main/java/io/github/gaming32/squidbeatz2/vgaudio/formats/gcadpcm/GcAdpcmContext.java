package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class GcAdpcmContext {
    private final short predScale;
    private final short hist1;
    private final short hist2;

    public GcAdpcmContext(short predScale, short hist1, short hist2) {
        this.predScale = predScale;
        this.hist1 = hist1;
        this.hist2 = hist2;
    }

    public GcAdpcmContext(InputStream is, boolean bigEndian) throws IOException {
        this(
            (short)Util.readShort(is, bigEndian),
            (short)Util.readShort(is, bigEndian),
            (short)Util.readShort(is, bigEndian)
        );
    }

    public short predScale() {
        return predScale;
    }

    public short hist1() {
        return hist1;
    }

    public short hist2() {
        return hist2;
    }
}
