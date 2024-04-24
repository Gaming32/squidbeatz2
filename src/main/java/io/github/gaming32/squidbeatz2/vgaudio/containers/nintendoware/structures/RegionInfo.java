package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures;

import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmContext;

import java.util.ArrayList;
import java.util.List;

public class RegionInfo {
    public int startSample;
    public int endSample;
    public List<GcAdpcmContext> channels = new ArrayList<>();
}
