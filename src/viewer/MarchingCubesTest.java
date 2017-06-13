package viewer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import bdv.bigcat.ui.ARGBConvertedLabelsSource;
import bdv.bigcat.ui.AbstractARGBConvertedLabelsSource;
import bdv.bigcat.ui.ModalGoldenAngleSaturatedARGBStream;
import bdv.img.h5.H5LabelMultisetSetupImageLoader;
import bdv.labels.labelset.LabelMultisetType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import cleargl.GLVector;
import graphics.scenery.Camera;
import graphics.scenery.DetachedHeadCamera;
import graphics.scenery.Material;
import graphics.scenery.Mesh;
import graphics.scenery.PointLight;
import graphics.scenery.SceneryDefaultApplication;
import graphics.scenery.SceneryElement;
import graphics.scenery.backends.Renderer;
import marchingCubes.MarchingCubes_shibbyRAI;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;

/**
 * Unit test for marching cubes
 * 
 * @author vleite
 */
public class MarchingCubesTest<T extends RealType<T>> {
	/** Log */
	private static final Logger LOGGER = Logger.getLogger(MarchingCubesTest.class.getName());

	private static RandomAccessibleInterval<LabelMultisetType> volumeLabels = null;

	protected static int setupId = 0;

	final static protected int[] cellDimensions = new int[] { 64, 64, 8 };

	/** color generator for composition of loaded segments and canvas */
	protected static ModalGoldenAngleSaturatedARGBStream colorStream;

	/** loaded segments */
	final protected static ArrayList<H5LabelMultisetSetupImageLoader> labels = new ArrayList<>();

	/** compositions of labels and canvas that are displayed */
	final protected static ArrayList<AbstractARGBConvertedLabelsSource> convertedLabels = new ArrayList<>();

	/**
	 * This method load the hdf file
	 */
	@BeforeClass
	public static void loadData() {

		// hdf file to use on test
		String path = "data/sample_B.augmented.0.hdf";
		String path_label = "/volumes/labels/neuron_ids";

		System.out.println("Opening labels from " + path);
		final IHDF5Reader reader = HDF5Factory.openForReading(path);
		System.out.println("reader: " + reader);

		/* labels */
		if (reader.exists(path_label)) {
			System.out.println("path exists ");
			try {
				readLabels(reader, path_label);
			} catch (IOException e) {
				System.out.println("exception!");
				e.printStackTrace();
			}
		} else {
			System.out.println("no label dataset '" + path_label + "' found");
		}

		volumeLabels = labels.get(0).getImage(0);
	}

	@Test
	public void testExample() throws Exception {
		MarchingCubeApplication viewer = new MarchingCubeApplication("Marching cube", 800, 600);
		viewer.main();
	}

	private class MarchingCubeApplication extends SceneryDefaultApplication {
		public MarchingCubeApplication(String applicationName, int windowWidth, int windowHeight) {
			super(applicationName, windowWidth, windowHeight, true);
		}

		@Override
		public void init() {

			setRenderer(Renderer.Factory.createRenderer(getHub(), getApplicationName(), getScene(), getWindowWidth(),
					getWindowHeight()));
			getHub().add(SceneryElement.RENDERER, getRenderer());

			final Material material = new Material();
			material.setAmbient(new GLVector(0.1f, 1.0f, 1.0f));
			material.setDiffuse(new GLVector(0.1f, 0.0f, 0.0f));
			material.setSpecular(new GLVector(0.1f, 0f, 0f));

			Mesh neuron = new Mesh();
			float[] verticesArray = null;
			float[] normalsArray = null;

			/** MarchingCubes - ilastik */
			
			/** MarchingCubes - shibbyRAI */
			{
				System.out.println("MarchingCubes - shibbyRAI");
				MarchingCubes_shibbyRAI<Integer> mc_shibby = new MarchingCubes_shibbyRAI<Integer>();
				int[] volDim = {2340, 1685, 153};
				float[] voxDim = {10, 10, 10};
				double isoLevel = 1771;
				int offset = 0;
				Timestamp begin = new Timestamp(System.currentTimeMillis());
				ArrayList< float[] > output = mc_shibby.marchingCubes(volumeLabels, volDim, voxDim, isoLevel, offset);
				Timestamp end = new Timestamp(System.currentTimeMillis());
				System.out.println("time for generating mesh: " + (end.getTime() - begin.getTime()));

				verticesArray = new float[ output.size() * 3 ];
				int n = 0;
				for ( float[] floatV : output )
				{
					for ( float f : floatV )
					{
						verticesArray[ n++ ] = f;
					}
				}
				normalsArray = verticesArray;
			}
			/***/


			neuron.setMaterial(material);
			neuron.setPosition(new GLVector(0.0f, 0.0f, 0.0f));
			neuron.setVertices(FloatBuffer.wrap(verticesArray));
			neuron.setNormals(FloatBuffer.wrap(normalsArray));

			getScene().addChild(neuron);

			PointLight[] lights = new PointLight[2];

			for (int i = 0; i < lights.length; i++) {
				lights[i] = new PointLight();
				lights[i].setPosition(new GLVector(2.0f * i, 2.0f * i, 2.0f * i));
				lights[i].setEmissionColor(new GLVector(1.0f, 1.0f, 1.0f));
				lights[i].setIntensity(100.2f * (i + 1));
				lights[i].setLinear(0.0f);
				lights[i].setQuadratic(0f);
				lights[i].setRadius(1000);
				getScene().addChild(lights[i]);
			}

			final Camera cam = new DetachedHeadCamera();
			cam.setPosition(new GLVector(0.0f, 0.0f, 5.0f));

			cam.perspectiveCamera(50.0f, getWindowWidth(), getWindowHeight(), 0.1f, 1000.0f);
			cam.setActive(true);
			getScene().addChild(cam);

			final Thread rotator = new Thread() {
				@Override
				public void run() {
					while (true) {
						neuron.setNeedsUpdate(true);

						float diff = cam.getPosition().minus(neuron.getPosition()).magnitude();

						if (diff < 1)
							neuron.getRotation().rotateByAngleY(0.0001f);
						else
							neuron.getRotation().rotateByAngleY(0.01f);
						try {
							Thread.sleep(20);
						} catch (InterruptedException e) {
							LOGGER.log(Level.SEVERE, "Interruption on rotator.", e);
						}
					}
				}
			};
			rotator.start();
		}
	}

	protected static void readLabels(final IHDF5Reader reader, final String labelDataset) throws IOException {
		/* labels */
		System.out.println("readLabels starts");
		final H5LabelMultisetSetupImageLoader labelLoader = new H5LabelMultisetSetupImageLoader(reader, null,
				labelDataset, setupId++, cellDimensions);
		
		System.out.println("labelLoader: " + labelLoader);

		/* converted labels */
		final ARGBConvertedLabelsSource convertedLabelsSource = new ARGBConvertedLabelsSource(setupId++, labelLoader,
				colorStream);
		
		System.out.println("convertedLabelsSource: " + convertedLabelsSource);

		labels.add(labelLoader);
		convertedLabels.add(convertedLabelsSource);
		
		System.out.println("readLabels ends");
	}

}
