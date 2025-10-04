package se233.audioconverter.controller;

import se233.audioconverter.Launcher;
import se233.audioconverter.exception.AudioConversionException;
import se233.audioconverter.model.AudioFile;
import se233.audioconverter.model.ConversionSettings;
import se233.audioconverter.service.FFmpegService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class MainViewController {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp3", "wav", "m4a", "flac");

    @FXML private ListView<AudioFile> fileListView;
    @FXML private ComboBox<ConversionSettings.OutputFormat> formatComboBox;
    @FXML private Slider qualitySlider;
    @FXML private Label qualityLabel;
    @FXML private ComboBox<ConversionSettings.SampleRate> sampleRateComboBox;
    @FXML private ComboBox<ConversionSettings.Channels> channelsComboBox;
    @FXML private Button convertButton;
    @FXML private Button clearButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private VBox advancedSettingsBox;
    @FXML private CheckBox showAdvancedCheckBox;

    private ObservableList<AudioFile> audioFiles;
    private ConversionSettings settings;
    private FFmpegService ffmpegService;
    private ExecutorService executorService;

    @FXML
    public void initialize() {
        audioFiles = FXCollections.observableArrayList();
        settings = new ConversionSettings();

        try {
            ffmpegService = new FFmpegService();
        } catch (IOException e) {
            showError("FFmpeg Initialization Error",
                    "Could not initialize FFmpeg. Make sure FFmpeg is installed and in your PATH.\n\n" +
                            "Error: " + e.getMessage());
            return;
        }

        executorService = Executors.newFixedThreadPool(4);

        setupUI();
        setupDragAndDrop();
    }

    private void setupUI() {
        // File list view
        fileListView.setItems(audioFiles);
        fileListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(AudioFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    // Color code by status
                    switch (item.getStatus()) {
                        case PENDING -> setStyle("-fx-text-fill: black;");
                        case PROCESSING -> setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                        case COMPLETED -> setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                        case FAILED -> setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Format ComboBox
        formatComboBox.setItems(FXCollections.observableArrayList(
                ConversionSettings.OutputFormat.values()));
        formatComboBox.setValue(ConversionSettings.OutputFormat.MP3);
        formatComboBox.setOnAction(e -> {
            settings.setOutputFormat(formatComboBox.getValue());
        });

        // Quality Slider
        qualitySlider.setMin(0);
        qualitySlider.setMax(3);
        qualitySlider.setValue(1); // Standard
        qualitySlider.setMajorTickUnit(1);
        qualitySlider.setMinorTickCount(0);
        qualitySlider.setSnapToTicks(true);
        qualitySlider.setShowTickMarks(true);
        qualitySlider.setShowTickLabels(true);

        qualitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int index = newVal.intValue();
            ConversionSettings.Quality quality = ConversionSettings.Quality.values()[index];
            settings.setQuality(quality);
            qualityLabel.setText(String.format("%s (%d kbps)",
                    quality.getLabel(), quality.getBitrate()));
        });
        qualityLabel.setText("Standard (128 kbps)");

        // Sample Rate ComboBox
        sampleRateComboBox.setItems(FXCollections.observableArrayList(
                ConversionSettings.SampleRate.values()));
        sampleRateComboBox.setValue(ConversionSettings.SampleRate.SR_44100);
        sampleRateComboBox.setOnAction(e -> {
            settings.setSampleRate(sampleRateComboBox.getValue());
        });

        // Channels ComboBox
        channelsComboBox.setItems(FXCollections.observableArrayList(
                ConversionSettings.Channels.values()));
        channelsComboBox.setValue(ConversionSettings.Channels.STEREO);
        channelsComboBox.setOnAction(e -> {
            settings.setChannels(channelsComboBox.getValue());
        });

        // Advanced settings toggle
        advancedSettingsBox.setVisible(false);
        advancedSettingsBox.setManaged(false);
        showAdvancedCheckBox.setOnAction(e -> {
            boolean show = showAdvancedCheckBox.isSelected();
            advancedSettingsBox.setVisible(show);
            advancedSettingsBox.setManaged(show);
        });

        // Buttons
        convertButton.setDisable(true);
        clearButton.setOnAction(e -> onClear());
        convertButton.setOnAction(e -> onConvert());

        // Progress
        progressBar.setProgress(0);
        statusLabel.setText("Ready");
    }

    private void setupDragAndDrop() {
        fileListView.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                boolean hasValidFile = db.getFiles().stream()
                        .anyMatch(file -> isAudioFile(file.getName()));
                if (hasValidFile) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });

        fileListView.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                List<File> validFiles = db.getFiles().stream()
                        .filter(file -> isAudioFile(file.getName()))
                        .toList();

                for (File file : validFiles) {
                    AudioFile audioFile = new AudioFile(file.getAbsolutePath());
                    // Check if already exists
                    boolean exists = audioFiles.stream()
                            .anyMatch(af -> af.getFilePath().equals(audioFile.getFilePath()));
                    if (!exists) {
                        audioFiles.add(audioFile);
                    }
                }

                success = !validFiles.isEmpty();
                convertButton.setDisable(audioFiles.isEmpty());

                if (!validFiles.isEmpty()) {
                    statusLabel.setText(String.format("Added %d file(s)", validFiles.size()));
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean isAudioFile(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }
        return SUPPORTED_FORMATS.contains(extension);
    }

    @FXML
    private void onConvert() {
        if (audioFiles.isEmpty()) {
            showError("No Files", "Please add audio files to convert.");
            return;
        }

        // Choose output directory
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Output Directory");
        File outputDir = directoryChooser.showDialog(Launcher.primaryStage);

        if (outputDir == null) {
            return; // User cancelled
        }

        // Disable UI during conversion
        setUIDisabled(true);

        // Reset all files to pending
        audioFiles.forEach(file -> file.setStatus(AudioFile.ConversionStatus.PENDING));
        fileListView.refresh();

        // Create conversion tasks
        List<AudioConversionTask> tasks = new ArrayList<>();
        for (AudioFile audioFile : audioFiles) {
            AudioConversionTask task = new AudioConversionTask(
                    audioFile, settings, outputDir.getAbsolutePath(), ffmpegService);

            // Set progress callback
            task.setProgressCallback(new AudioConversionTask.ProgressCallback() {
                @Override
                public void onProgress(double percentage, String message) {
                    // Optional: update individual file progress
                }

                @Override
                public void onStatusChange(AudioFile.ConversionStatus status) {
                    Platform.runLater(() -> fileListView.refresh());
                }
            });

            tasks.add(task);
        }

        // Execute tasks with progress tracking
        Task<Void> masterTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                CompletionService<Void> completionService =
                        new ExecutorCompletionService<>(executorService);

                int totalTasks = tasks.size();
                int completedTasks = 0;

                // Submit all tasks
                for (Callable<Void> task : tasks) {
                    completionService.submit(task);
                }

                // Wait for completion
                for (int i = 0; i < totalTasks; i++) {
                    try {
                        Future<Void> future = completionService.take();
                        future.get();
                        completedTasks++;

                        double progress = (double) completedTasks / totalTasks;
                        updateProgress(progress, 1.0);
                        updateMessage(String.format("Completed %d of %d files",
                                completedTasks, totalTasks));

                    } catch (ExecutionException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof AudioConversionException) {
                            AudioConversionException ace = (AudioConversionException) cause;
                            final String errorMsg = ace.getUserFriendlyMessage();
                            Platform.runLater(() ->
                                    showError("Conversion Error", errorMsg));
                        } else {
                            final String errorMsg = "An unexpected error occurred: " +
                                    (cause != null ? cause.getMessage() : "Unknown error");
                            Platform.runLater(() ->
                                    showError("Unexpected Error", errorMsg));
                        }
                    }
                }

                return null;
            }
        };

        // Bind progress
        progressBar.progressProperty().bind(masterTask.progressProperty());
        statusLabel.textProperty().bind(masterTask.messageProperty());

        masterTask.setOnSucceeded(e -> {
            long successful = audioFiles.stream()
                    .filter(f -> f.getStatus() == AudioFile.ConversionStatus.COMPLETED)
                    .count();
            long failed = audioFiles.stream()
                    .filter(f -> f.getStatus() == AudioFile.ConversionStatus.FAILED)
                    .count();

            statusLabel.textProperty().unbind();
            statusLabel.setText(String.format("Conversion complete: %d successful, %d failed",
                    successful, failed));

            showInfo("Conversion Complete",
                    String.format("Successfully converted %d file(s).\nFailed: %d\n\nOutput location: %s",
                            successful, failed, outputDir.getAbsolutePath()));

            setUIDisabled(false);
        });

        masterTask.setOnFailed(e -> {
            statusLabel.textProperty().unbind();
            statusLabel.setText("Conversion failed");
            showError("Error", "An error occurred during conversion.");
            setUIDisabled(false);
        });

        Thread thread = new Thread(masterTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setUIDisabled(boolean disabled) {
        convertButton.setDisable(disabled || audioFiles.isEmpty());
        clearButton.setDisable(disabled);
        formatComboBox.setDisable(disabled);
        qualitySlider.setDisable(disabled);
        sampleRateComboBox.setDisable(disabled);
        channelsComboBox.setDisable(disabled);
        showAdvancedCheckBox.setDisable(disabled);

        if (!disabled) {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        }
    }

    @FXML
    private void onClear() {
        audioFiles.clear();
        convertButton.setDisable(true);
        statusLabel.setText("Ready");
        progressBar.setProgress(0);
    }

    @FXML
    private void onClose() {
        if (executorService != null) {
            executorService.shutdown();
        }
        Platform.exit();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
