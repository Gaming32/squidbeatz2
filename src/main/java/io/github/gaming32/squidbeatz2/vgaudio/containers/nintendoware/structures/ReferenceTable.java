package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ReferenceTable {
    public final int count;
    public final List<Reference> references = new ArrayList<>();

    public ReferenceTable(InputStream is, boolean bigEndian) throws IOException {
        this(is, bigEndian, 0);
    }

    public ReferenceTable(InputStream is, boolean bigEndian, int baseOffset) throws IOException {
        count = Util.readInt(is, bigEndian);

        for (int i = 0; i < count; i++) {
            references.add(new Reference(is, bigEndian, baseOffset));
        }
    }
}
