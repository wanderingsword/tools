package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        //测试
//        sample(primaryStage);
        Parent root = FXMLLoader.load((getClass().getResource("aab/aab.fxml")));
        primaryStage.setScene(new Scene(root, 500, 400));
        primaryStage.show();
    }

    private void sample(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("test/sample.fxml"));
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }


}
