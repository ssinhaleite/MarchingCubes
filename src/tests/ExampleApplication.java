package tests;

import javafx.application.Application;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import viewer.Viewer3D;

public class ExampleApplication extends Application
{
	public static void main( final String[] args )
	{
		launch( args );
	}

	@Override
	public void start( final Stage primaryStage ) throws Exception
	{
		System.out.println( "creating viewer... " );
		final Viewer3D viewer3D = new Viewer3D( null );
		System.out.println( "... " );
		viewer3D.init();

		final StackPane stackPane = new StackPane();
		stackPane.getChildren().add( viewer3D.getPanel() );
		final javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
		primaryStage.setScene( scene );
		primaryStage.show();
	}
}
