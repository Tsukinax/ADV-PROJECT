package se233.audioconverter.model;

import java.util.Arrays;
import java.util.List;

public class ConversionSettings {
    public enum OutputFormat {
        MP3("mp3", "libmp3lame", true),
        WAV("wav", "pcm_s16le", false),
        M4A("m4a", "aac", true),
        FLAC("flac", "flac", false);

        private final String extension;
        private final String codec;
        private final boolean supportsBitrate;

        OutputFormat(String extension, String codec, boolean supportsBitrate) {
            this.extension = extension;
            this.codec = codec;
            this.supportsBitrate = supportsBitrate;
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

        public List<Integer> getBitrateOptions() {
            switch (this) {
                case MP3:
                    return Arrays.asList(64, 96, 128, 160, 192, 224, 256, 320);
                case M4A:
                    return Arrays.asList(64, 96, 128, 160, 192, 256, 320);
                case WAV:
                case FLAC:
                default:
                    return Arrays.asList(); // Empty for lossless formats
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
        SR_44100("44.1 kHz", 44100),
        SR_48000("48 kHz", 48000);

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

