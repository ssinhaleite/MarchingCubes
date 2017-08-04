package javafx;

import javafx.application.Application;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class JavaFXApplication extends Application
{
	public static void main( final String[] args )
	{
		launch( args );
	}

	@Override
	public void start( final Stage primaryStage ) throws Exception
	{
		final SimpleSceneryScene simpleSceneryScene = new SimpleSceneryScene( null );

		final StackPane stackPane = new StackPane();
		stackPane.getChildren().addAll( simpleSceneryScene.getPanel() );
		final javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
		primaryStage.setScene( scene );
		primaryStage.show();

		simpleSceneryScene.init();
	}
}
