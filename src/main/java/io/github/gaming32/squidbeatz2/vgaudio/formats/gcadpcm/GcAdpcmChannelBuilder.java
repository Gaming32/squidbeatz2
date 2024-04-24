package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmDecoder;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmParameters;

public class GcAdpcmChannelBuilder {
    public final byte[] adpcm;
    public final short[] coefs;
    public short[] pcm;
    public final int sampleCount;

    public GcAdpcmContext startContext;
    public short gain;
    public int loopAlignmentMultiple;

    public boolean ensureSeekTableIsSelfCalculated;
    public boolean ensureLoopContextIsSelfCalculated;

    public short[] seekTable;
    public int samplesPerSeekTableEntry;
    public boolean seekTableIsSelfCalculated;

    public GcAdpcmContext loopContext;
    public int loopContextStart;
    public boolean loopContextIsSelfCalculated;

    public boolean looping;
    public int loopStart;
    public int loopEnd;

    public GcAdpcmSeekTable previousSeekTable;
    public GcAdpcmLoopContext previousLoopContext;
    public GcAdpcmAlignment previousAlignment;

    public short[] alignedPcm;
    public byte[] alignedAdpcm;
    public int alignedLoopStart;
    public int alignedSampleCount;

    public GcAdpcmChannel build() {
        prepareForBuild();
        return new GcAdpcmChannel(this);
    }

    public GcAdpcmChannelBuilder prepareForBuild() {
        alignedPcm = pcm;
        alignedAdpcm = adpcm;
        alignedLoopStart = loopStart;
        alignedSampleCount = sampleCount;
        startContext = startContext != null ? startContext : new GcAdpcmContext((short)0, (short)0, (short)0);
        loopContext = loopContext != null ? loopContext : new GcAdpcmContext((short)0, (short)0, (short)0);
        return this;
    }

    public GcAdpcmChannelBuilder(byte[] adpcm, short[] coefs, int sampleCount) {
        this.adpcm = adpcm;
        this.coefs = coefs;
        this.sampleCount = sampleCount;
    }

    public GcAdpcmChannelBuilder withLoopAlignment(int loopAlignmentMultiple) {
        this.loopAlignmentMultiple = loopAlignmentMultiple;
        return this;
    }

    public GcAdpcmChannelBuilder withSeekTable(short[] seekTable, int samplesPerEntry) {
        return withSeekTable(seekTable, samplesPerEntry, false);
    }

    public GcAdpcmChannelBuilder withSeekTable(short[] seekTable, int samplesPerEntry, boolean isSelfCalculated) {
        this.seekTable = seekTable;
        samplesPerSeekTableEntry = samplesPerEntry;
        seekTableIsSelfCalculated = isSelfCalculated;
        return this;
    }

    public GcAdpcmChannelBuilder withSamplesPerSeekTableEntry(int samplesPerEntry) {
        if (samplesPerEntry != samplesPerSeekTableEntry) {
            seekTable = null;
            seekTableIsSelfCalculated = false;
        }
        samplesPerSeekTableEntry = samplesPerEntry;
        return this;
    }

    public GcAdpcmChannelBuilder withLoopContext(int loopStart, short predScale, short loopHist1, short loopHist2) {
        return withLoopContext(loopStart, predScale, loopHist1, loopHist2, false);
    }

    public GcAdpcmChannelBuilder withLoopContext(int loopStart, short predScale, short loopHist1, short loopHist2, boolean isSelfCalculated) {
        loopContextStart = loopStart;
        loopContext = new GcAdpcmContext(predScale, loopHist1, loopHist2);
        loopContextIsSelfCalculated = isSelfCalculated;
        return this;
    }

    public GcAdpcmChannelBuilder withLoop(boolean loop, int loopStart, int loopEnd) {
        if (!loop) {
            return withLoop(false);
        }

        looping = true;
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
        return this;
    }

    public GcAdpcmChannelBuilder withLoop(boolean loop) {
        looping = loop;
        loopStart = 0;
        loopEnd = loop ? sampleCount : 0;
        return this;
    }

    public GcAdpcmChannelBuilder withPrevious(GcAdpcmSeekTable seekTable, GcAdpcmLoopContext loopContext, GcAdpcmAlignment alignment) {
        previousSeekTable = seekTable;
        previousLoopContext = loopContext;
        previousAlignment = alignment;
        return this;
    }

    public boolean previousAlignmentIsValid() {
        return looping &&
               previousAlignment != null && previousAlignment.loopStartAligned == loopStart &&
               previousAlignment.loopEnd == loopEnd &&
               previousAlignment.alignmentMultiple == loopAlignmentMultiple;
    }

    public boolean previousLoopContextIsValid(int loopStart) {
        return previousLoopContext != null && previousLoopContext.loopStart == loopStart &&
               (!ensureLoopContextIsSelfCalculated || previousLoopContext.isSelfCalculated);
    }

    public boolean currentLoopContextIsValid(int loopStart) {
        return loopContextStart == loopStart &&
               (!ensureLoopContextIsSelfCalculated || loopContextIsSelfCalculated);
    }

    public boolean previousSeekTableIsValid() {
        return seekTable == null &&
               previousSeekTable != null && previousSeekTable.samplesPerEntry() == samplesPerSeekTableEntry &&
               (!ensureSeekTableIsSelfCalculated || previousSeekTable.isSelfCalculated());
    }

    public boolean currentSeekTableIsValid() {
        return seekTable != null &&
               (!ensureSeekTableIsSelfCalculated || seekTableIsSelfCalculated);
    }

    public GcAdpcmAlignment getAlignment() {
        if (previousAlignmentIsValid()) {
            return previousAlignment;
        }
        final GcAdpcmAlignment alignment = new GcAdpcmAlignment(loopAlignmentMultiple, loopStart, loopEnd, adpcm, coefs);

        if (alignment.alignmentNeeded) {
            alignedAdpcm = alignment.adpcmAligned;
            alignedPcm = alignment.pcmAligned;
            alignedLoopStart = alignment.loopStart;
        }

        return alignment;
    }

    public GcAdpcmLoopContext getLoopContext() {
        if (previousLoopContextIsValid(alignedLoopStart)) {
            return previousLoopContext;
        }

        if (currentLoopContextIsValid(alignedLoopStart)) {
            return new GcAdpcmLoopContext(loopContext.predScale(), loopContext.hist1(), loopContext.hist2(), loopContextStart, loopContextIsSelfCalculated);
        }

        ensurePcmDecoded();
        return new GcAdpcmLoopContext(adpcm, alignedPcm, alignedLoopStart);
    }

    public GcAdpcmSeekTable getSeekTable() {
        if (samplesPerSeekTableEntry == 0) {
            return null;
        }

        if (previousSeekTableIsValid()) {
            return previousSeekTable;
        }

        if (currentSeekTableIsValid()) {
            return new GcAdpcmSeekTable(seekTable, samplesPerSeekTableEntry, seekTableIsSelfCalculated);
        }
        ensurePcmDecoded();
        return new GcAdpcmSeekTable(alignedPcm, samplesPerSeekTableEntry);
    }

    private void ensurePcmDecoded() {
        if (alignedPcm == null) {
            final GcAdpcmParameters params = new GcAdpcmParameters();
            params.sampleCount = alignedSampleCount;
            alignedPcm = GcAdpcmDecoder.decode(alignedAdpcm, coefs, params);
        }
    }
}
