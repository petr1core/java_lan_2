module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires com.google.gson;
    requires java.desktop;

    opens com.example.demo to com.google.gson;
    exports com.example.demo;
}