package se233.audioconverter.model;

import java.util.Arrays;
import java.util.List;

public class ConversionSettings {
    public enum OutputFormat {
        MP3("mp3", "libmp3lame", true, true),  // supports bitrate, supports VBR
        WAV("wav", "pcm_s16le", false, false),
        M4A("m4a", "aac", true, false),        // supports bitrate, no VBR
        FLAC("flac", "flac", false, false);

        private final String extension;
        private final String codec;
        private final boolean supportsBitrate;
        private final boolean supportsVBR;

        OutputFormat(String extension, String codec, boolean supportsBitrate, boolean supportsVBR) {
            this.extension = extension;
            this.codec = codec;
            this.supportsBitrate = supportsBitrate;
            this.supportsVBR = supportsVBR;
        }

        public String getExtension() {
            return extension;
        }

        public String getCodec() {
            return codec;
        }

        public boolean supportsBitrate() {
            return supportsBitrate;
        }

        public boolean supportsVBR() {
            return supportsVBR;
        }

        public List<Integer> getBitrateOptions() {
            switch (this) {
                case MP3:
                    return Arrays.asList(32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320);
                case M4A:
                    return Arrays.asList(16, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 448, 512);
                case WAV:
                case FLAC:
                default:
                    return Arrays.asList();
            }
        }

        public int getDefaultBitrate() {
            switch (this) {
                case MP3:
                    return 192;
                case M4A:
                    return 192;
                default:
                    return 0;
            }
        }

        public List<Integer> getSampleRateOptions() {
            switch (this) {
                case MP3:
                    return Arrays.asList(32000, 44100, 48000);
                case WAV:
                    return Arrays.asList(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, 64000, 88200, 96000);
                case M4A:
                case FLAC:
                    return Arrays.asList(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000);
                default:
                    return Arrays.asList(44100, 48000);
            }
        }

        public int getDefaultSampleRate() {
            return 44100;
        }

        @Override
        public String toString() {
            return extension.toUpperCase();
        }
    }

    public enum Quality {
        ECONOMY("Economy", 64),
        STANDARD("Standard", 128),
        GOOD("Good", 192),
        BEST("Best", 320);

        private final String label;
        private final int bitrate;

        Quality(String label, int bitrate) {
            this.label = label;
            this.bitrate = bitrate;
        }

        public String getLabel() {
            return label;
        }

        public int getBitrate() {
            return bitrate;
        }

        @Override
        public String toString() {
            return String.format("%s (%d kbps)", label, bitrate);
        }
    }

    public enum SampleRate {
        SR_8000("8000 Hz", 8000),
        SR_11025("11025 Hz", 11025),
        SR_12000("12000 Hz", 12000),
        SR_16000("16000 Hz", 16000),
        SR_22050("22050 Hz", 22050),
        SR_24000("24000 Hz", 24000),
        SR_32000("32000 Hz", 32000),
        SR_44100("44100 Hz", 44100),
        SR_48000("48000 Hz", 48000),
        SR_64000("64000 Hz", 64000),
        SR_88200("88200 Hz", 88200),
        SR_96000("96000 Hz", 96000);

        private final String label;
        private final int rate;

        SampleRate(String label, int rate) {
            this.label = label;
            this.rate = rate;
        }

        public String getLabel() {
            return label;
        }

        public int getRate() {
            return rate;
        }

        @Override
        public String toString() {
            return label;
        }

        public static SampleRate fromRate(int rate) {
            for (SampleRate sr : values()) {
                if (sr.rate == rate) {
                    return sr;
                }
            }
            return SR_44100; // default
        }
    }

    public enum Channels {
        MONO("Mono", 1),
        STEREO("Stereo", 2);

        private final String label;
        private final int count;

        Channels(String label, int count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    // Bitrate mode
    public enum BitrateMode {
        CONSTANT("Constant Bitrate (CBR)"),
        VARIABLE("Variable Bitrate (VBR)");

        private final String label;

        BitrateMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private OutputFormat outputFormat;
    private Quality quality;
    private Integer customBitrate; // Custom bitrate in kbps
    private SampleRate sampleRate;
    private Channels channels;
    private BitrateMode bitrateMode;
    private int vbrQuality; // VBR quality (0-5, MP3 only)

    public ConversionSettings() {
        // Default settings
        this.outputFormat = OutputFormat.MP3;
        this.quality = Quality.GOOD;
        this.customBitrate = null; // null means use quality preset
        this.sampleRate = SampleRate.SR_44100;
        this.channels = Channels.STEREO;
        this.bitrateMode = BitrateMode.CONSTANT;
        this.vbrQuality = 2; // Default VBR quality (Normal)
    }

    // Getters and Setters
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
        // Reset custom bitrate when format changes
        this.customBitrate = null;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
    }

    public Integer getCustomBitrate() {
        return customBitrate;
    }

    public void setCustomBitrate(Integer customBitrate) {
        this.customBitrate = customBitrate;
    }

    public int getEffectiveBitrate() {
        // If custom bitrate is set, use it; otherwise use quality preset
        if (customBitrate != null && customBitrate > 0) {
            return customBitrate;
        }
        return quality.getBitrate();
    }

    public SampleRate getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(SampleRate sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Channels getChannels() {
        return channels;
    }

    public void setChannels(Channels channels) {
        this.channels = channels;
    }

    public BitrateMode getBitrateMode() {
        return bitrateMode;
    }

    public void setBitrateMode(BitrateMode bitrateMode) {
        this.bitrateMode = bitrateMode;
    }

    public int getVbrQuality() {
        return vbrQuality;
    }

    public void setVbrQuality(int vbrQuality) {
        this.vbrQuality = vbrQuality;
    }
}

