package javafx;

import graphics.scenery.utils.SceneryPanel;
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

		SceneryPanel scPanel = new SceneryPanel( 500, 500 );
		stackPane.getChildren().addAll( scPanel );

		final javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
		primaryStage.setScene( scene );
		primaryStage.show();

		final SimpleSceneryApplication sceneryApplication = new SimpleSceneryApplication( "JavaFX - Scenery Application", 500, 500, false );
		sceneryApplication.setPanel( scPanel );
		sceneryApplication.main();
	}
}
