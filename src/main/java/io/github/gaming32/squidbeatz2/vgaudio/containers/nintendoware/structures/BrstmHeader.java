package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.util.Util;

import java.io.IOException;
import java.io.InputStream;

public class BrstmHeader {
    public int headBlockOffset;
    public int headBlockSize;
    public int seekBlockOffset;
    public int seekBlockSize;
    public int dataBlockOffset;
    public int dataBlockSize;

    public BrstmHeader read(InputStream is, boolean bigEndian) throws IOException {
        headBlockOffset = Util.readInt(is, bigEndian);
        headBlockSize = Util.readInt(is, bigEndian);
        seekBlockOffset = Util.readInt(is, bigEndian);
        seekBlockSize = Util.readInt(is, bigEndian);
        dataBlockOffset = Util.readInt(is, bigEndian);
        dataBlockSize = Util.readInt(is, bigEndian);
        return this;
    }

    public BrstmHeader readBrwav(InputStream is, boolean bigEndian) throws IOException {
        headBlockOffset = Util.readInt(is, bigEndian);
        headBlockSize = Util.readInt(is, bigEndian);
        dataBlockOffset = Util.readInt(is, bigEndian);
        dataBlockSize = Util.readInt(is, bigEndian);
        return this;
    }
}
