package javafx;

import javafx.application.Application;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class JavaFXApplicationSceneryApplication extends Application
{
	public static void main( final String[] args )
	{
		launch( args );
	}

	@Override
	public void start( final Stage primaryStage ) throws Exception
	{
		primaryStage.setTitle( "JavaFX - Scenery Application" );
		final StackPane stackPane = new StackPane();

		final SimpleSceneryApplication sceneryApplication = new SimpleSceneryApplication( "JavaFX - Scenery Application", 500, 500, false );
		stackPane.getChildren().addAll( sceneryApplication.getPanel() );

		final javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
		primaryStage.setScene( scene );
		primaryStage.show();

		new Thread( () -> {
			sceneryApplication.main();
		} ).start();
	}
}
