package se233.audioconverter.controller;

import se233.audioconverter.exception.AudioConversionException;
import se233.audioconverter.model.AudioFile;
import se233.audioconverter.model.ConversionSettings;
import se233.audioconverter.service.FFmpegService;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.probe.FFmpegFormat;

import java.util.concurrent.Callable;

public class AudioConversionTask implements Callable<Void> {
    private final AudioFile audioFile;
    private final ConversionSettings settings;
    private final String outputPath;
    private final FFmpegService ffmpegService;

    private ProgressCallback progressCallback;

    public interface ProgressCallback {
        void onProgress(double percentage, String message);
        void onStatusChange(AudioFile.ConversionStatus status);
    }

    public AudioConversionTask(AudioFile audioFile, ConversionSettings settings,
                               String outputPath, FFmpegService ffmpegService) {
        this.audioFile = audioFile;
        this.settings = settings;
        this.outputPath = outputPath;
        this.ffmpegService = ffmpegService;
    }

    public void setProgressCallback(ProgressCallback callback) {
        this.progressCallback = callback;
    }

    @Override
    public Void call() throws Exception {
        updateStatus(AudioFile.ConversionStatus.PROCESSING);

        try {
            FFmpegProbeResult probeResult = ffmpegService.probeFile(audioFile.getFilePath());
            FFmpegFormat format = probeResult.getFormat();


            final double duration = format.duration;

            ffmpegService.convertAudio(audioFile, settings, outputPath, new ProgressListener() {
                @Override
                public void progress(Progress progress) {
                    if (duration > 0 && progressCallback != null) {
                        // ใช้ field โดยตรง
                        double currentTime = progress.out_time_ns / 1_000_000_000.0;
                        double percentage = (currentTime / duration) * 100.0;
                        String message = String.format("Converting %s: %.1f%%",
                                audioFile.getName(), percentage);
                        progressCallback.onProgress(percentage, message);
                    }
                }
            });

            updateStatus(AudioFile.ConversionStatus.COMPLETED);

        } catch (AudioConversionException e) {
            updateStatus(AudioFile.ConversionStatus.FAILED);
            throw e;
        }

        return null;
    }

    private void updateStatus(AudioFile.ConversionStatus status) {
        audioFile.setStatus(status);
        if (progressCallback != null) {
            progressCallback.onStatusChange(status);
        }
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }
}