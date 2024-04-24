package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class SizedReference extends Reference {
    public final int size;

    public SizedReference(InputStream is, boolean bigEndian) throws IOException {
        this(is, bigEndian, 0);
    }

    public SizedReference(InputStream is, boolean bigEndian, int baseOffset) throws IOException {
        super(is, bigEndian, baseOffset);
        size = Util.readInt(is, bigEndian);
    }
}
