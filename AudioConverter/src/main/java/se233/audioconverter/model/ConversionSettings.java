package se233.audioconverter.model;

public class ConversionSettings {
    public enum OutputFormat {
        MP3("mp3", "libmp3lame"),
        WAV("wav", "pcm_s16le"),
        M4A("m4a", "aac"),
        FLAC("flac", "flac");

        private final String extension;
        private final String codec;

        OutputFormat(String extension, String codec) {
            this.extension = extension;
            this.codec = codec;
        }

        public String getExtension() {
            return extension;
        }

        public String getCodec() {
            return codec;
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

    private OutputFormat outputFormat;
    private Quality quality;
    private SampleRate sampleRate;
    private Channels channels;

    public ConversionSettings() {
        // Default settings
        this.outputFormat = OutputFormat.MP3;
        this.quality = Quality.STANDARD;
        this.sampleRate = SampleRate.SR_44100;
        this.channels = Channels.STEREO;
    }

    // Getters and Setters
    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(OutputFormat outputFormat) {
        this.outputFormat = outputFormat;
    }

    public Quality getQuality() {
        return quality;
    }

    public void setQuality(Quality quality) {
        this.quality = quality;
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
}