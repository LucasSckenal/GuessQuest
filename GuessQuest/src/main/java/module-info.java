module br.edu.unijui.piu.guessquest {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires com.google.gson; 

    opens br.edu.unijui.piu.guessquest to javafx.fxml;
    exports br.edu.unijui.piu.guessquest;
}
