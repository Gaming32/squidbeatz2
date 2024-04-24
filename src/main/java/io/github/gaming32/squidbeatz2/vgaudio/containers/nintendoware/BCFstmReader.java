package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware;

import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.vgaudio.containers.AudioReader;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.ChannelInfo;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.PrefetchData;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.Reference;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.ReferenceType;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.RegionInfo;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.SizedReference;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.StreamInfo;
import io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware.structures.TrackInfo;
import io.github.gaming32.squidbeatz2.vgaudio.formats.IAudioFormat;
import io.github.gaming32.squidbeatz2.vgaudio.formats.gcadpcm.GcAdpcmContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BCFstmReader extends AudioReader<BxstmStructure> {
    private static final Set<String> VALID_MAGIC = Set.of("CSTM", "FSTM", "CWAV", "FWAV", "CSTP", "FSTP");

    @Override
    protected <S extends InputStream & Seekable> BxstmStructure readFile(S is) throws IOException {
        final String magic = Util.readString(is, 4);
        if (!VALID_MAGIC.contains(magic)) {
            throw new IOException("File has no CSTM or FSTM header");
        }

        final int bom = Util.readShort(is, false);
        final boolean bigEndian = switch (bom) {
            case 0xFEFF -> false;
            case 0xFFFE -> true;
            default -> throw new IOException("File has no byte order mark");
        };

        final BxstmStructure structure = new BxstmStructure();
        structure.bigEndian = bigEndian;

        readHeader(is, bigEndian, structure);
        readInfoBlock(is, bigEndian, structure);
        readSeekBlock(is, bigEndian, structure);
        readRegionBlock(is, bigEndian, structure);
        readDataBlock(is, bigEndian, structure);

        return structure;
    }

    @Override
    protected IAudioFormat toAudioStream(BxstmStructure structure) {
        return Common.toAudioStream(structure);
    }

    private static void readHeader(InputStream is, boolean bigEndian, BxstmStructure structure) throws IOException {
        structure.headerSize = Util.readShort(is, bigEndian);
        structure.version = new NwVersion(Util.readInt(is, bigEndian) & 0xFFFFFFFFL);
        structure.fileSize = Util.readInt(is, bigEndian);

        structure.blockCount = Util.readShort(is, bigEndian);
        is.skipNBytes(2);

        for (int i = 0; i < structure.blockCount; i++) {
            structure.blocks.add(new SizedReference(is, bigEndian));
        }
    }

    private static <S extends InputStream & Seekable> void readInfoBlock(S is, boolean bigEndian, BxstmStructure structure) throws IOException {
        final SizedReference reference = structure.blocks.stream()
            .filter(x -> x.type() == ReferenceType.STREAM_INFO_BLOCK || x.type() == ReferenceType.WAVE_INFO_BLOCK)
            .findFirst()
            .orElse(null);
        if (reference == null) {
            throw new IOException("File has no INFO block");
        }

        is.seek(reference.getAbsoluteOffset());
        if (!Util.readString(is, 4).equals("INFO")) {
            throw new IOException("Unknown or invalid INFO block");
        }

        if (Util.readInt(is, bigEndian) != reference.size) {
            throw new IOException("INFO block size in main header doesn't match size in INFO header");
        }

        switch (reference.type()) {
            case ReferenceType.STREAM_INFO_BLOCK -> {
                final int baseOffset = (int)is.tell();
                final Reference streamInfo = new Reference(is, bigEndian, baseOffset);
                final Reference trackInfo = new Reference(is, bigEndian, baseOffset);
                final Reference channelInfo = new Reference(is, bigEndian, baseOffset);

                readStreamInfo(is, bigEndian, structure, streamInfo);
                readTrackInfo(is, bigEndian, structure, trackInfo);
                readChannelInfo(is, bigEndian, structure, channelInfo);
            }
            case ReferenceType.WAVE_INFO_BLOCK -> {
                structure.streamInfo = StreamInfo.readBfwav(is, bigEndian, structure.version);
                structure.channelInfo = new ChannelInfo().readBfstm(is, bigEndian);
            }
        }
    }

    private static <S extends InputStream & Seekable> void readStreamInfo(S is, boolean bigEndian, BxstmStructure structure, Reference reference) throws IOException {
        if (!reference.isType(ReferenceType.STREAM_INFO)) {
            throw new IOException("Could not read stream info.");
        }

        is.seek(reference.getAbsoluteOffset());
        structure.streamInfo = new StreamInfo().readBfstm(is, bigEndian, structure.version);
    }

    private static <S extends InputStream & Seekable> void readTrackInfo(S is, boolean bigEndian, BxstmStructure structure, Reference reference) throws IOException {
        if (!reference.isType(ReferenceType.REFERENCE_TABLE)) return;

        is.seek(reference.getAbsoluteOffset());
        structure.trackInfo = new TrackInfo().readBfstm(is, bigEndian);
    }

    private static <S extends InputStream & Seekable> void readChannelInfo(S is, boolean bigEndian, BxstmStructure structure, Reference reference) throws IOException {
        if (!reference.isType(ReferenceType.REFERENCE_TABLE)) return;

        is.seek(reference.getAbsoluteOffset());
        structure.channelInfo = new ChannelInfo().readBfstm(is, bigEndian);
    }

    private static <S extends InputStream & Seekable> void readSeekBlock(S is, boolean bigEndian, BxstmStructure structure) throws IOException {
        final SizedReference reference = structure.blocks.stream()
            .filter(x -> x.type() == ReferenceType.STREAM_SEEK_BLOCK)
            .findFirst()
            .orElse(null);
        if (reference == null) return;

        is.seek(reference.getAbsoluteOffset());
        final StreamInfo info = structure.streamInfo;

        if (!Util.readString(is, 4).equals("SEEK")) {
            throw new IOException("Unknown or invalid SEEK block");
        }

        if (Util.readInt(is, bigEndian) != reference.size) {
            throw new IOException("SEEK block size in main header doesn't match size in SEEK header");
        }

        final int bytesPerEntry = 4 * info.channelCount;
        final int numSeekTableEntries = (info.sampleCount - 1 + info.samplesPerSeekTableEntry) / info.samplesPerSeekTableEntry;

        final int seekTableSize = bytesPerEntry * numSeekTableEntries;

        final byte[] tableBytes = is.readNBytes(seekTableSize);

        structure.seekTable = Util.deInterleave(Util.toShortArray(tableBytes, false), 2, info.channelCount);
    }

    private static <S extends InputStream & Seekable> void readRegionBlock(S is, boolean bigEndian, BxstmStructure structure) throws IOException {
        final SizedReference reference = structure.blocks.stream()
            .filter(x -> x.type() == ReferenceType.STREAM_REGION_BLOCK)
            .findFirst()
            .orElse(null);
        if (reference == null) return;

        is.seek(reference.getAbsoluteOffset());

        if (!Util.readString(is, 4).equals("REGN")) {
            throw new IOException("Unknown or invalid REGN block");
        }

        if (Util.readInt(is, bigEndian) != reference.size) {
            throw new IOException("REGN block size in main header doesn't match size in REGN header");
        }

        final StreamInfo info = structure.streamInfo;
        final int startAddress = reference.getAbsoluteOffset() + 8 + info.regionReference.offset();
        final List<RegionInfo> regions = new ArrayList<>(info.regionCount);

        for (int i = 0; i < info.regionCount; i++) {
            is.seek(startAddress + (long)info.regionInfoSize * i);

            final RegionInfo entry = new RegionInfo();
            entry.startSample = Util.readInt(is, bigEndian);
            entry.endSample = Util.readInt(is, bigEndian);

            for (int c = 0; c < info.channelCount; c++) {
                entry.channels.add(new GcAdpcmContext(is, bigEndian));
            }
            regions.add(entry);
        }

        structure.regions = regions;
    }

    private static <S extends InputStream & Seekable> void readDataBlock(S is, boolean bigEndian, BxstmStructure structure) throws IOException {
        final SizedReference reference = structure.blocks.stream()
            .filter(x ->
                x.type() == ReferenceType.STREAM_DATA_BLOCK ||
                x.type() == ReferenceType.STREAM_PREFETCH_DATA_BLOCK ||
                x.type() == ReferenceType.WAVE_DATA_BLOCK
            )
            .findFirst()
            .orElse(null);
        if (reference == null) {
            throw new IOException("File has no DATA block");
        }

        final StreamInfo info = structure.streamInfo;

        is.seek(reference.getAbsoluteOffset());

        final String blockId = Util.readString(is, 4);
        if (!blockId.equals("DATA") && !blockId.equals("PDAT")) {
            throw new IOException("Unknown or invalid DATA block");
        }

        if (Util.readInt(is, bigEndian) != reference.size) {
            throw new IOException("DATA block size in main header doesn't match size in DATA header");
        }

        if (reference.isType(ReferenceType.WAVE_DATA_BLOCK)) {
            final int audioDataLength = Common.samplesToBytes(info.sampleCount, info.codec);
            structure.audioData = Util.createJaggedArray(byte[][].class, info.channelCount, audioDataLength);
            final int baseOffset = (int)is.tell();

            for (int i = 0; i < info.channelCount; i++) {
                is.seek(baseOffset + structure.channelInfo.waveAudioOffsets.get(i));
                structure.audioData[i] = is.readNBytes(audioDataLength);
            }
        } else if (reference.isType(ReferenceType.STREAM_DATA_BLOCK)) {
            final int audioOffset = reference.getAbsoluteOffset() + info.audioReference.offset() + 8;
            is.seek(audioOffset);
            final int audioDataLength = reference.size - (audioOffset - reference.getAbsoluteOffset());
            final int outputSize = Common.samplesToBytes(info.sampleCount, info.codec);

            structure.audioData = Util.deInterleave(is, audioDataLength, info.interleaveSize, info.channelCount, outputSize);
        } else if (reference.isType(ReferenceType.STREAM_PREFETCH_DATA_BLOCK)) {
            final int count = Util.readInt(is, bigEndian);
            structure.prefetchData = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                structure.prefetchData.add(PrefetchData.readPrefetchData(is, bigEndian, info));
            }

            is.seek(structure.prefetchData.getFirst().audioData.getAbsoluteOffset());
            structure.audioData = Util.deInterleave(
                is,
                structure.prefetchData.getFirst().size,
                info.interleaveSize,
                info.channelCount,
                structure.prefetchData.getFirst().size
            );
        }
    }
}
