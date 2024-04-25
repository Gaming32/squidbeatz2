package io.github.gaming32.squidbeatz2.game.assets;

import io.github.gaming32.squidbeatz2.util.seekable.Seekable;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableChannelInputStream;
import io.github.gaming32.squidbeatz2.util.seekable.SeekableFileInputStream;
import org.apache.commons.io.function.IOFunction;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

@FunctionalInterface
public interface FileGetter<S extends InputStream & Seekable> extends IOFunction<String, S> {
    static FileGetter<?> ofDirectory(Path dir) {
        if (dir.getFileSystem() == FileSystems.getDefault()) {
            return ofDirectory(dir.toFile());
        }
        return path -> new SeekableChannelInputStream(
            Files.newByteChannel(dir.resolve(path.replace("/", dir.getFileSystem().getSeparator())))
        );
    }

    static FileGetter<?> ofDirectory(File dir) {
        return path -> new SeekableFileInputStream(new File(dir, path.replace('/', File.separatorChar)));
    }

    default FileGetter<?> orElse(FileGetter<?> other) {
        return path -> {
            try {
                return apply(path);
            } catch (NoSuchFileException | FileNotFoundException e) {
                return other.apply(path);
            }
        };
    }
}
