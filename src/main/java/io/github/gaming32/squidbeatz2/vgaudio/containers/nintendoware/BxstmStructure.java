package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware;

import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.BrstmHeader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.ChannelInfo;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.PrefetchData;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.RegionInfo;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.SizedReference;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.StreamInfo;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.TrackInfo;

import java.util.ArrayList;
import java.util.List;

public class BxstmStructure {
    public int fileSize;
    public boolean bigEndian;

    public int headerSize;
    public NwVersion version;
    public int blockCount;

    public BrstmHeader brstmHeader;
    public List<SizedReference> blocks = new ArrayList<>();

    public StreamInfo streamInfo;
    public TrackInfo trackInfo;
    public ChannelInfo channelInfo;

    public List<RegionInfo> regions;
    public List<PrefetchData> prefetchData;

    public int brstmSeekTableType;

    public short[][] seekTable;
    public byte[][] audioData;
}
