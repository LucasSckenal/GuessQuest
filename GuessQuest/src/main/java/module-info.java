module br.edu.unijui.piu.guessquest {
    requires javafx.controls;
    requires javafx.fxml;

    opens br.edu.unijui.piu.guessquest to javafx.fxml;
    exports br.edu.unijui.piu.guessquest;
}
