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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

public class MainViewController {
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList("mp3", "wav", "m4a", "flac");

    // Stage 1: File Drop
    @FXML private StackPane mainStackPane;
    @FXML private VBox fileDropStage;
    @FXML private VBox dropZone;
    @FXML private VBox filePreviewBox;
    @FXML private ListView<AudioFile> filePreviewList;
    @FXML private Label fileCountLabel;
    @FXML private Button nextButton;

    // Stage 2: Configuration
    @FXML private VBox configStage;
    @FXML private Label configFileCountLabel;
    @FXML private ListView<AudioFile> fileListView;
    @FXML private ComboBox<ConversionSettings.OutputFormat> formatComboBox;
    @FXML private Label formatInfoLabel;

    // Bitrate Settings
    @FXML private VBox bitrateSettingsBox;
    @FXML private Slider qualitySlider;
    @FXML private Label qualityLabel;
    @FXML private ComboBox<Integer> bitrateComboBox;

    // Advanced Settings
    @FXML private VBox advancedSettingsBox;
    @FXML private CheckBox showAdvancedCheckBox;
    @FXML private ComboBox<ConversionSettings.SampleRate> sampleRateComboBox;
    @FXML private ComboBox<ConversionSettings.Channels> channelsComboBox;

    // Bitrate Mode (in Advanced Settings, MP3 only)
    @FXML private VBox bitrateModeBox;
    @FXML private ToggleGroup bitrateToggleGroup;
    @FXML private RadioButton constantBitrateRadio;
    @FXML private RadioButton variableBitrateRadio;
    @FXML private VBox cbrBitrateBox;  // CBR bitrate dropdown
    @FXML private VBox vbrQualityBox;  // VBR quality slider
    @FXML private Slider vbrQualitySlider;
    @FXML private Label vbrQualityLabel;


    @FXML private Button convertButton;
    @FXML private Button clearButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;

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

        setupStage1();
        setupStage2();

        // Start with Stage 1
        showStage1();
    }

    private void setupStage1() {
        filePreviewList.setItems(audioFiles);
        filePreviewList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(AudioFile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (" + item.getFormat().toUpperCase() + ")");
                }
            }
        });

        setupDragAndDrop(dropZone);
    }

    private void setupStage2() {
        // File list view with status
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
            updateFormatUI();
        });

        // Quality Slider (always visible - for quality presets)
        qualitySlider.setMin(0);
        qualitySlider.setMax(3);
        qualitySlider.setValue(2); // Good
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

            // Update bitrate combobox to match
            if (settings.getOutputFormat().supportsBitrate()) {
                bitrateComboBox.setValue(quality.getBitrate());
            }
        });
        qualityLabel.setText("Good (192 kbps)");

        // Bitrate ComboBox
        bitrateComboBox.setOnAction(e -> {
            Integer selectedBitrate = bitrateComboBox.getValue();
            if (selectedBitrate != null) {
                settings.setCustomBitrate(selectedBitrate);
            }
        });

        // Bitrate Mode Radio Buttons (in Advanced Settings)
        constantBitrateRadio.setOnAction(e -> {
            settings.setBitrateMode(ConversionSettings.BitrateMode.CONSTANT);
            updateBitrateModeUI();
        });

        variableBitrateRadio.setOnAction(e -> {
            settings.setBitrateMode(ConversionSettings.BitrateMode.VARIABLE);
            updateBitrateModeUI();
        });

        // VBR Quality Slider (0-5, MP3 only)
        if (vbrQualitySlider != null) {
            vbrQualitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int vbrQuality = newVal.intValue();
                settings.setVbrQuality(vbrQuality);

                String[] vbrLabels = {"Best", "High", "Normal", "Medium", "Low", "Smallest"};
                if (vbrQualityLabel != null) {
                    vbrQualityLabel.setText(vbrQuality + " (" + vbrLabels[vbrQuality] + ")");
                }
            });
            if (vbrQualityLabel != null) {
                vbrQualityLabel.setText("2 (Normal)");
            }
        }

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
        convertButton.setOnAction(e -> onConvert());
        clearButton.setOnAction(e -> onClear());

        // Progress
        progressBar.setProgress(0);
        statusLabel.setText("Ready");

        // Initial UI update
        updateFormatUI();
    }

    private void setupDragAndDrop(VBox targetZone) {
        targetZone.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                boolean hasValidFile = db.getFiles().stream()
                        .anyMatch(file -> isAudioFile(file.getName()));
                if (hasValidFile) {
                    event.acceptTransferModes(TransferMode.COPY);
                    targetZone.setStyle("-fx-border-color: #2196F3; -fx-border-width: 3; -fx-border-style: dashed; -fx-border-radius: 10; -fx-background-color: #E3F2FD; -fx-background-radius: 10; -fx-padding: 60;");
                }
            }
            event.consume();
        });

        targetZone.setOnDragExited(event -> {
            targetZone.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 3; -fx-border-style: dashed; -fx-border-radius: 10; -fx-background-color: #f9f9f9; -fx-background-radius: 10; -fx-padding: 60;");
            event.consume();
        });

        targetZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                List<File> validFiles = db.getFiles().stream()
                        .filter(file -> isAudioFile(file.getName()))
                        .toList();

                for (File file : validFiles) {
                    AudioFile audioFile = new AudioFile(file.getAbsolutePath());
                    boolean exists = audioFiles.stream()
                            .anyMatch(af -> af.getFilePath().equals(audioFile.getFilePath()));
                    if (!exists) {
                        audioFiles.add(audioFile);
                    }
                }

                success = !validFiles.isEmpty();
                updateFilePreview();
            }

            targetZone.setStyle("-fx-border-color: #4CAF50; -fx-border-width: 3; -fx-border-style: dashed; -fx-border-radius: 10; -fx-background-color: #f9f9f9; -fx-background-radius: 10; -fx-padding: 60;");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void updateFormatUI() {
        ConversionSettings.OutputFormat format = settings.getOutputFormat();

        // Update format info label
        if (format.supportsBitrate()) {
            formatInfoLabel.setText("Lossy compression format");
            bitrateSettingsBox.setVisible(true);
            bitrateSettingsBox.setManaged(true);
            cbrBitrateBox.setVisible(true);
            cbrBitrateBox.setManaged(true);

            // Update bitrate options
            List<Integer> bitrateOptions = format.getBitrateOptions();
            bitrateComboBox.setItems(FXCollections.observableArrayList(bitrateOptions));
            bitrateComboBox.setValue(format.getDefaultBitrate());
            settings.setCustomBitrate(format.getDefaultBitrate());

        } else {
            formatInfoLabel.setText("Lossless format (no bitrate selection)");
            bitrateSettingsBox.setVisible(true);
            bitrateSettingsBox.setManaged(true);
            cbrBitrateBox.setVisible(false);
            cbrBitrateBox.setManaged(false);
        }

        // Show/hide Bitrate Mode section (MP3 only)
        boolean isMp3 = format == ConversionSettings.OutputFormat.MP3;
        bitrateModeBox.setVisible(isMp3);
        bitrateModeBox.setManaged(isMp3);
    }

    private void updateBitrateModeUI() {
        boolean isVBR = variableBitrateRadio.isSelected();
        boolean isMp3 = settings.getOutputFormat() == ConversionSettings.OutputFormat.MP3;

        // Show/hide based on mode selection (only when format is MP3)
        if (isMp3) {
            if (isVBR) {
                // VBR mode - show VBR quality slider, hide CBR bitrate dropdown
                if (cbrBitrateBox != null) {
                    cbrBitrateBox.setVisible(false);
                    cbrBitrateBox.setManaged(false);
                }
                if (vbrQualityBox != null) {
                    vbrQualityBox.setVisible(true);
                    vbrQualityBox.setManaged(true);
                }
            } else {
                // CBR mode - show CBR bitrate dropdown, hide VBR quality slider
                if (cbrBitrateBox != null) {
                    cbrBitrateBox.setVisible(true);
                    cbrBitrateBox.setManaged(true);
                }
                if (vbrQualityBox != null) {
                    vbrQualityBox.setVisible(false);
                    vbrQualityBox.setManaged(false);
                }
            }
        }
    }

    private void updateFilePreview() {
        if (audioFiles.isEmpty()) {
            filePreviewBox.setVisible(false);
            filePreviewBox.setManaged(false);
            nextButton.setDisable(true);
            fileCountLabel.setText("0 files");
        } else {
            filePreviewBox.setVisible(true);
            filePreviewBox.setManaged(true);
            nextButton.setDisable(false);
            fileCountLabel.setText(audioFiles.size() + " file(s)");
        }
        filePreviewList.refresh();
    }

    private boolean isAudioFile(String filename) {
        String extension = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filename.substring(lastDot + 1).toLowerCase();
        }
        return SUPPORTED_FORMATS.contains(extension);
    }

    // Stage Navigation
    private void showStage1() {
        fileDropStage.setVisible(true);
        fileDropStage.setManaged(true);
        configStage.setVisible(false);
        configStage.setManaged(false);
    }

    private void showStage2() {
        fileDropStage.setVisible(false);
        fileDropStage.setManaged(false);
        configStage.setVisible(true);
        configStage.setManaged(true);

        // Update file count in config stage
        configFileCountLabel.setText(audioFiles.size() + " file(s) selected");
    }

    @FXML
    private void onBrowseFiles() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio Files");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3", "*.wav", "*.m4a", "*.flac"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(Launcher.primaryStage);

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            for (File file : selectedFiles) {
                if (isAudioFile(file.getName())) {
                    AudioFile audioFile = new AudioFile(file.getAbsolutePath());
                    boolean exists = audioFiles.stream()
                            .anyMatch(af -> af.getFilePath().equals(audioFile.getFilePath()));
                    if (!exists) {
                        audioFiles.add(audioFile);
                    }
                }
            }
            updateFilePreview();
        }
    }

    @FXML
    private void onNextToConfig() {
        if (audioFiles.isEmpty()) {
            showError("No Files", "Please add audio files first.");
            return;
        }
        showStage2();
    }

    @FXML
    private void onBackToFiles() {
        showStage1();
    }

    @FXML
    private void onClearFromStage1() {
        audioFiles.clear();
        updateFilePreview();
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
            return;
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

        // Execute tasks
        Task<Void> masterTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                CompletionService<Void> completionService =
                        new ExecutorCompletionService<>(executorService);

                int totalTasks = tasks.size();
                int completedTasks = 0;

                for (Callable<Void> task : tasks) {
                    completionService.submit(task);
                }

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
        convertButton.setDisable(disabled);
        clearButton.setDisable(disabled);
        formatComboBox.setDisable(disabled);
        qualitySlider.setDisable(disabled);
        bitrateComboBox.setDisable(disabled);
        constantBitrateRadio.setDisable(disabled);
        variableBitrateRadio.setDisable(disabled);
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
        updateFilePreview();
        fileListView.refresh();
        statusLabel.setText("Ready");
        progressBar.setProgress(0);

        // Go back to stage 1
        showStage1();
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
