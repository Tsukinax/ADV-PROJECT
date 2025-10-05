package se233.audioconverter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Launcher extends Application {
    public static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        FXMLLoader fxmlLoader = new FXMLLoader(
                Launcher.class.getResource("main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        primaryStage.setTitle("Audio Converter");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(820);
        primaryStage.setMinHeight(620);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}