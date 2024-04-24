package io.github.gaming32.squidbeatz2.vgaudio.containers.nintendoware;

public class NwVersion {
    public final int major, minor, micro, revision;
    public final long packedVersion;

    public NwVersion() {
        this(0, 0, 0, 0);
    }

    public NwVersion(int major) {
        this(major, 0, 0, 0);
    }

    public NwVersion(int major, int minor) {
        this(major, minor, 0, 0);
    }

    public NwVersion(int major, int minor, int micro) {
        this(major, minor, micro, 0);
    }

    public NwVersion(int major, int minor, int micro, int revision) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        this.revision = revision;
        this.packedVersion = (long)major << 24 | (long)minor << 16 | (long)micro << 8 | revision;
    }

    public NwVersion(long packedVersion) {
        this.major = (int)((packedVersion >> 24) & 0xff);
        this.minor = (int)((packedVersion >> 16) & 0xff);
        this.micro = (int)((packedVersion >> 8) & 0xff);
        this.revision = (int)(packedVersion & 0xff);
        this.packedVersion = packedVersion;
    }
}
