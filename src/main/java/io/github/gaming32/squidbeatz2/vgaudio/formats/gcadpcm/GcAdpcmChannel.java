package io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm;

import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmDecoder;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmMath;
import io.github.gaming32.squidbeatz2.vgaudio.codecs.gcadpcm.GcAdpcmParameters;

public class GcAdpcmChannel {
    public final byte[] adpcm;
    private final short[] pcm;
    public final int unalignedSampleCount;

    public final short gain;
    public final short[] coefs;
    public final GcAdpcmContext startContext;

    private final GcAdpcmSeekTable seekTable;
    private final GcAdpcmLoopContext loopContextEx;
    private final GcAdpcmAlignment alignment;

    public GcAdpcmChannel(byte[] adpcm, short[] coefs, int sampleCount) {
        this.adpcm = adpcm;
        this.coefs = coefs;
        unalignedSampleCount = sampleCount;

        pcm = null;
        gain = 0;
        startContext = null;
        seekTable = null;
        loopContextEx = null;
        alignment = null;
    }

    public GcAdpcmChannel(GcAdpcmChannelBuilder b) {
        if (b.alignedAdpcm.length < GcAdpcmMath.sampleCountToByteCount(b.sampleCount)) {
            throw new IllegalArgumentException("Audio array length is too short for the specified number of samples.");
        }

        unalignedSampleCount = b.sampleCount;
        adpcm = b.adpcm;

        coefs = b.coefs;
        gain = b.gain;
        startContext = new GcAdpcmContext(adpcm[0], b.startContext.hist1(), b.startContext.hist2());

        alignment = b.getAlignment();
        loopContextEx = b.getLoopContext();
        seekTable = b.getSeekTable();

        if (!isAlignmentNeeded()) {
            pcm = b.alignedPcm;
        } else {
            pcm = b.pcm;
        }
    }

    public int getSampleCount() {
        return isAlignmentNeeded() ? alignment.sampleCountAligned : unalignedSampleCount;
    }

    public GcAdpcmContext getLoopContext() {
        return loopContextEx;
    }

    private int getAlignmentMultiple() {
        return alignment != null ? alignment.alignmentMultiple : 0;
    }

    private boolean isAlignmentNeeded() {
        return alignment != null && alignment.alignmentNeeded;
    }

    public short[] getPcmAudio() {
        if (isAlignmentNeeded()) {
            return alignment.pcmAligned;
        }
        if (pcm != null) {
            return pcm;
        }
        final GcAdpcmParameters params = new GcAdpcmParameters();
        params.sampleCount = getSampleCount();
        params.history1 = startContext.hist1();
        params.history2 = startContext.hist2();
        return GcAdpcmDecoder.decode(getAdpcmAudio(), coefs, params);
    }

    public short[] getSeekTable() {
        if (seekTable != null && seekTable.seekTable() != null) {
            return seekTable.seekTable();
        }
        return new short[0];
    }

    public byte[] getAdpcmAudio() {
        return isAlignmentNeeded() ? alignment.adpcmAligned : adpcm;
    }

    public byte getPredScale(int sampleNum) {
        return GcAdpcmLoopContext.getPredScale(getAdpcmAudio(), sampleNum);
    }

    public short getHist1(int sampleNum) {
        return GcAdpcmLoopContext.getHist1(pcm, sampleNum);
    }

    public short getHist2(int sampleNum) {
        return GcAdpcmLoopContext.getHist2(pcm, sampleNum);
    }

    public GcAdpcmChannelBuilder getCloneBuilder() {
        final GcAdpcmChannelBuilder builder = new GcAdpcmChannelBuilder(adpcm, coefs, unalignedSampleCount);
        builder.pcm = pcm;
        builder.gain = gain;
        builder.startContext = startContext;
        builder.loopAlignmentMultiple = getAlignmentMultiple();
        builder.withPrevious(seekTable, loopContextEx, alignment);

        if (seekTable != null) {
            builder.withSeekTable(seekTable.seekTable(), seekTable.samplesPerEntry(), seekTable.isSelfCalculated());
        }

        if (loopContextEx != null) {
            builder.withLoopContext(loopContextEx.loopStart, startContext.predScale(), startContext.hist1(), startContext.hist2());
        }

        if (alignment != null) {
            builder.withLoop(true, alignment.loopStart, alignment.loopEnd);
        }

        return builder;
    }
}
