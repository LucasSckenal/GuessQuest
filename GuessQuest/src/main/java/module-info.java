module br.edu.unijui.piu.guessquest {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.net.http;
    requires java.base;
    requires com.google.gson;

    opens br.edu.unijui.piu.guessquest to javafx.fxml;
    exports br.edu.unijui.piu.guessquest;
}
