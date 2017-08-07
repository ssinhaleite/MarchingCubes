package javafx;

import java.util.concurrent.CountDownLatch;

import com.sun.javafx.application.PlatformImpl;

import graphics.scenery.utils.SceneryPanel;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class JavaFXSceneryApplication
{
	public static void main( final String[] args )
	{
		SceneryApplication sceneryApplication = new SceneryApplication();
		sceneryApplication.init();
	}

	private static class SceneryApplication
	{
		public void init()
		{

			CountDownLatch latch = new CountDownLatch( 1 );
			final SceneryPanel[] imagePanel = { null };

			PlatformImpl.startup( () -> {} );

			Platform.runLater( () -> {

				Stage stage = new Stage();
				stage.setTitle( "JavaFX - Scenery Application" );

				StackPane stackPane = new StackPane();

				imagePanel[ 0 ] = new SceneryPanel( 500, 500 );
				stackPane.getChildren().addAll( imagePanel[ 0 ] );

				Scene scene = new Scene( stackPane );
				stage.setScene( scene );
				stage.show();

				latch.countDown();
			} );

			try
			{
				latch.await();
			}
			catch ( InterruptedException e1 )
			{
				e1.printStackTrace();
			}

			SimpleSceneryApplication sceneryApplication = new SimpleSceneryApplication( "JavaFX - Scenery Application", 500, 500, false );
			sceneryApplication.setPanel( imagePanel[ 0 ] );

			sceneryApplication.main();
		}
	}
}
