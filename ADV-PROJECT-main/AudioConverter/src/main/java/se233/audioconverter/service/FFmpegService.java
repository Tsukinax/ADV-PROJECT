package se233.audioconverter.service;

import se233.audioconverter.exception.AudioConversionException;
import se233.audioconverter.model.AudioFile;
import se233.audioconverter.model.ConversionSettings;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegFormat;
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.ProgressListener;
import net.bramp.ffmpeg.progress.Progress;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FFmpegService {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp3", "wav", "m4a", "flac");

    private FFmpeg ffmpeg;
    private FFprobe ffprobe;
    private FFmpegExecutor executor;

    public FFmpegService() throws IOException {
        String ffmpegPath = getFfmpegPath();
        String ffprobePath = getFfprobePath();

        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.ffprobe = new FFprobe(ffprobePath);
        this.executor = new FFmpegExecutor(ffmpeg, ffprobe);
    }

    private String getFfmpegPath() {
        String os = System.getProperty("os.name").toLowerCase();

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            String ffmpegName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";

            for (String path : paths) {
                File ffmpegFile = new File(path, ffmpegName);
                if (ffmpegFile.exists() && ffmpegFile.canExecute()) {
                    return ffmpegFile.getAbsolutePath();
                }
            }
        }

        if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            String[] commonPaths = {
                    "/usr/local/bin/ffmpeg",
                    "/usr/bin/ffmpeg",
                    "/opt/homebrew/bin/ffmpeg"
            };
            for (String path : commonPaths) {
                File ffmpegFile = new File(path);
                if (ffmpegFile.exists() && ffmpegFile.canExecute()) {
                    return path;
                }
            }
        } else if (os.contains("win")) {
            String[] commonPaths = {
                    "C:\\ffmpeg\\bin\\ffmpeg.exe",
                    "C:\\Program Files\\ffmpeg\\bin\\ffmpeg.exe"
            };
            for (String path : commonPaths) {
                File ffmpegFile = new File(path);
                if (ffmpegFile.exists()) {
                    return path;
                }
            }
        }

        return os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
    }

    private String getFfprobePath() {
        String os = System.getProperty("os.name").toLowerCase();

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            String[] paths = pathEnv.split(File.pathSeparator);
            String ffprobeName = os.contains("win") ? "ffprobe.exe" : "ffprobe";

            for (String path : paths) {
                File ffprobeFile = new File(path, ffprobeName);
                if (ffprobeFile.exists() && ffprobeFile.canExecute()) {
                    return ffprobeFile.getAbsolutePath();
                }
            }
        }

        if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
            String[] commonPaths = {
                    "/usr/local/bin/ffprobe",
                    "/usr/bin/ffprobe",
                    "/opt/homebrew/bin/ffprobe"
            };
            for (String path : commonPaths) {
                File ffprobeFile = new File(path);
                if (ffprobeFile.exists() && ffprobeFile.canExecute()) {
                    return path;
                }
            }
        } else if (os.contains("win")) {
            String[] commonPaths = {
                    "C:\\ffmpeg\\bin\\ffprobe.exe",
                    "C:\\Program Files\\ffmpeg\\bin\\ffprobe.exe"
            };
            for (String path : commonPaths) {
                File ffprobeFile = new File(path);
                if (ffprobeFile.exists()) {
                    return path;
                }
            }
        }

        return os.contains("win") ? "ffprobe.exe" : "ffprobe";
    }

    public boolean isFormatSupported(String format) {
        return SUPPORTED_FORMATS.contains(format.toLowerCase());
    }

    public void validateAudioFile(AudioFile audioFile) throws AudioConversionException {
        File file = new File(audioFile.getFilePath());

        if (!file.exists()) {
            throw new AudioConversionException(
                    audioFile.getName(),
                    AudioConversionException.ErrorType.FILE_NOT_FOUND,
                    "File does not exist"
            );
        }

        if (!isFormatSupported(audioFile.getFormat())) {
            throw new AudioConversionException(
                    audioFile.getName(),
                    AudioConversionException.ErrorType.UNSUPPORTED_FORMAT,
                    "Supported formats: " + String.join(", ", SUPPORTED_FORMATS)
            );
        }
    }

    public FFmpegProbeResult probeFile(String filePath) throws AudioConversionException {
        try {
            return ffprobe.probe(filePath);
        } catch (IOException e) {
            throw new AudioConversionException(
                    new File(filePath).getName(),
                    AudioConversionException.ErrorType.FFMPEG_ERROR,
                    e
            );
        }
    }

    public void convertAudio(AudioFile audioFile, ConversionSettings settings,
                             String outputPath, ProgressListener listener)
            throws AudioConversionException {

        validateAudioFile(audioFile);

        try {
            String outputFilename = buildOutputFilename(audioFile, settings, outputPath);

            // Build FFmpeg command manually using ProcessBuilder for correct argument order
            List<String> command = new ArrayList<>();

            // FFmpeg executable path
            String ffmpegPath = getFfmpegPath();
            command.add(ffmpegPath);

            // Global options
            command.add("-y"); // Overwrite output files
            command.add("-v");
            command.add("error");

            // Input file
            command.add("-i");
            command.add(audioFile.getFilePath());

            // Output options (AFTER input file)
            command.add("-c:a");
            command.add(settings.getOutputFormat().getCodec());

            command.add("-ac");
            command.add(String.valueOf(settings.getChannels().getCount()));

            command.add("-ar");
            command.add(String.valueOf(settings.getSampleRate().getRate()));

            // Bitrate settings - check mode and format
            if (settings.getOutputFormat().supportsBitrate()) {
                if (settings.getOutputFormat() == ConversionSettings.OutputFormat.MP3 &&
                        settings.getBitrateMode() == ConversionSettings.BitrateMode.VARIABLE) {
                    // MP3 VBR mode - use -q:a (quality) instead of bitrate
                    command.add("-q:a");
                    command.add(String.valueOf(settings.getVbrQuality()));
                } else {
                    // CBR mode or other formats - use bitrate
                    int bitrate = settings.getEffectiveBitrate();
                    command.add("-b:a");
                    command.add(bitrate + "k");
                }
            }

            // Output file (MUST be last)
            command.add(outputFilename);

            // Execute command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // Read output
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println(line); // For debugging
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new AudioConversionException(
                        audioFile.getName(),
                        AudioConversionException.ErrorType.FFMPEG_ERROR,
                        "FFmpeg exit code: " + exitCode + "\n" + output.toString()
                );
            }

            // Notify listener of completion (simplified - no progress tracking)
            if (listener != null) {
                Progress progress = new Progress();
                progress.out_time_ns = 1000000000L; // Dummy value
                listener.progress(progress);
            }

        } catch (IOException | InterruptedException e) {
            throw new AudioConversionException(
                    audioFile.getName(),
                    AudioConversionException.ErrorType.FFMPEG_ERROR,
                    e
            );
        }
    }

    private String buildOutputFilename(AudioFile audioFile, ConversionSettings settings,
                                       String outputPath) {
        String baseName = audioFile.getName();
        int lastDot = baseName.lastIndexOf('.');
        if (lastDot > 0) {
            baseName = baseName.substring(0, lastDot);
        }

        String newExtension = settings.getOutputFormat().getExtension();
        Path outputDir = Paths.get(outputPath);
        return outputDir.resolve(baseName + "." + newExtension).toString();
    }

    public String getAudioInfo(String filePath) throws AudioConversionException {
        FFmpegProbeResult probeResult = probeFile(filePath);

        if (probeResult.getStreams().isEmpty()) {
            return "No audio stream found";
        }

        FFmpegStream stream = probeResult.getStreams().get(0);

        String codecName = stream.codec_name != null ? stream.codec_name : "Unknown";
        int sampleRate = stream.sample_rate;
        int channels = stream.channels;
        long bitRate = stream.bit_rate;

        return String.format(
                "Codec: %s, Sample Rate: %d Hz, Channels: %d, Bitrate: %d kbps",
                codecName,
                sampleRate,
                channels,
                bitRate / 1000
        );
    }
}