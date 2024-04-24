package io.github.gaming32.squidbeatz2.vgaudio.formats;

import com.google.common.collect.Sets;
import io.github.gaming32.squidbeatz2.util.Util;
import io.github.gaming32.squidbeatz2.vgaudio.formats.pcm16.Pcm16Format;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class AudioData {
    private final Map<Class<? extends IAudioFormat>, IAudioFormat> formats = new HashMap<>();

    public AudioData(IAudioFormat audioFormat) {
        addFormat(audioFormat);
    }

    private void addFormat(IAudioFormat format) {
        formats.put(format.getClass(), format);
    }

    public <T extends IAudioFormat> T getFormat(Class<T> clazz) {
        final T format = getAudioFormat(clazz);

        if (format != null) {
            return format;
        }

        createPcm16();
        createFormat(clazz);

        return getAudioFormat(clazz);
    }

    public Collection<IAudioFormat> getAllFormats() {
        return formats.values();
    }

    public Set<Class<? extends IAudioFormat>> listAvailableFormats() {
        return formats.keySet();
    }

    public void setLoop(boolean loop, int loopStart, int loopEnd) {
        for (final var entry : formats.entrySet()) {
            entry.setValue(entry.getValue().withLoop(loop, loopStart, loopEnd));
        }
    }

    public void setLoop(boolean loop) {
        for (final var entry : formats.entrySet()) {
            entry.setValue(entry.getValue().withLoop(loop));
        }
    }

    public static AudioData combine(AudioData... audio) {
        if (audio == null || audio.length == 0 || Arrays.stream(audio).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Audio cannot be null, empty, or have any null elements");
        }

        final var commonTypes = List.copyOf(
            Arrays.stream(audio)
                .map(AudioData::listAvailableFormats)
                .reduce(Sets::intersection)
                .orElse(Set.of())
        );

        final Class<? extends IAudioFormat> formatToUse;

        if (commonTypes.isEmpty() || (commonTypes.size() == 1 && commonTypes.contains(Pcm16Format.class))) {
            formatToUse = Pcm16Format.class;
            for (final AudioData a : audio) {
                a.createPcm16();
            }
        } else {
            formatToUse = commonTypes.stream()
                .filter(x -> x != Pcm16Format.class)
                .findFirst()
                .orElse(null);
        }

        final MutableObject<IAudioFormat> combined = new MutableObject<>(audio[0].formats.get(formatToUse));

        Arrays.stream(audio).map(x -> x.formats.get(formatToUse)).skip(1).forEach(format -> {
            if (!combined.getValue().tryAdd(format, combined)) {
                throw new IllegalArgumentException("Audio streams cannot be added together");
            }
        });

        return new AudioData(combined.getValue());
    }

    @SuppressWarnings("unchecked")
    private <T extends IAudioFormat> T getAudioFormat(Class<T> clazz) {
        final IAudioFormat format = formats.get(clazz);
        return (T)format;
    }

    private <T extends IAudioFormat> void createFormat(Class<T> clazz) {
        final Pcm16Format pcm = getAudioFormat(Pcm16Format.class);
        addFormat(Util.create(clazz).encodeFromPcm16(pcm));
    }

    private void createPcm16() {
        if (getAudioFormat(Pcm16Format.class) == null) {
            addFormat(formats.values().iterator().next().toPcm16());
        }
    }
}
