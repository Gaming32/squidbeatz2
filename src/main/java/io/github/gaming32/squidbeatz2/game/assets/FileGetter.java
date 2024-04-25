package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableChannelInputStream;
import org.apache.commons.io.function.IOFunction;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@FunctionalInterface
public interface FileGetter<S extends InputStream & Seekable> extends IOFunction<String, S> {
    static FileGetter<?> ofDirectory(Path dir) {
        return path -> new SeekableChannelInputStream(
            Files.newByteChannel(dir.resolve(path.replace("/", dir.getFileSystem().getSeparator())))
        );
    }

    default FileGetter<?> orElse(FileGetter<?> other) {
        return path -> {
            try {
                return apply(path);
            } catch (NoSuchFileException e) {
                return other.apply(path);
            }
        };
    }
}
