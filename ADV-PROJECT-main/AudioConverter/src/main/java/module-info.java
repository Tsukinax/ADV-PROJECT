module se233.audioconverter {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.common;
    requires com.google.gson;
    requires org.apache.commons.lang3;
    requires org.slf4j;

    // เพิ่ม requires สำหรับ ffmpeg
    requires ffmpeg;

    opens se233.audioconverter to javafx.fxml;
    opens se233.audioconverter.controller to javafx.fxml;
    opens se233.audioconverter.model to javafx.fxml;

    exports se233.audioconverter;
    exports se233.audioconverter.controller;
    exports se233.audioconverter.model;
}