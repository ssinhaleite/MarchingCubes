package marchingCubes;

//import java.sql.Timestamp;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
//
//import bdv.labels.labelset.Label;
//import bdv.labels.labelset.LabelMultisetType;
//import bdv.labels.labelset.Multiset;
//import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
//import net.imagej.ops.geom.geom3d.mesh.Facet;
//import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
//import net.imagej.ops.geom.geom3d.mesh.Vertex;
//import net.imglib2.Cursor;
//import net.imglib2.FinalInterval;
//import net.imglib2.RandomAccessibleInterval;
//import net.imglib2.view.ExtendedRandomAccessibleInterval;
//import net.imglib2.view.Views;
//import viewer.Mesh;

/**
 * This is a marching cubes implementation. It is inspired by Paul Bourke's
 * (http://paulbourke.net/geometry/polygonise/) implementation. Especially the
 * lookup tables are from his implementation.
 * 
 * @author Tim-Oliver Buchholz (University of Konstanz)
 * @param <T>
 *            BooleanType
 */
public class MarchingCubes_ThreeDViewer
{
//	private double isolevel;
//	
//	private ArrayList< double[] > vertices = new ArrayList<>();
//	private ArrayList< double[] > normals = new ArrayList<>();
//
//	public DefaultMesh calculate( final RandomAccessibleInterval< LabelMultisetType > input, int isoLevel )
//	{
//		isolevel = isoLevel;
//
//		DefaultMesh output = new DefaultMesh();
//		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
//				Views.extendValue( input, new LabelMultisetType() );
//		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { input
//				.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
//						0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) )
//				.localizingCursor();
//
//		while ( c.hasNext() )
//		{
//			c.next();
//
//			int cursorX = c.getIntPosition( 0 );
//			int cursorY = c.getIntPosition( 1 );
//			int cursorZ = c.getIntPosition( 2 );
//			Cursor< LabelMultisetType > cu = getCube( extended, cursorX, cursorY, cursorZ );
//			int i = 0;
//			double[] vertex_values = new double[ 8 ];
//			while ( cu.hasNext() )
//			{
//				LabelMultisetType it = cu.next();
//				
//				for ( final Multiset.Entry< Label > e : it.entrySet() )
//				{
//					vertex_values[ i ] = e.getElement().id();
//				}
//				i++;
//			}
//
//			// @formatter:off
//			//  6------7
//			// /|     /|
//			// 2-----3 |
//			// |4----|-5
//			// |/    |/
//			// 0-----1
//			// @formatter:on
//			vertex_values = mapFlatIterableToLookUpCube( vertex_values );
//
//			// @formatter:off
//			//  4------5
//			// /|     /|
//			// 7-----6 |
//			// |0----|-1
//			// |/    |/
//			// 3-----2
//			// @formatter:on
//			int cubeindex = getCubeIndex( vertex_values );
//
//			if ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] != 0 )
//			{
//				int[] p0 = new int[] { 0 + cursorX, 0 + cursorY, 1 + cursorZ };
//				int[] p1 = new int[] { 1 + cursorX, 0 + cursorY, 1 + cursorZ };
//				int[] p2 = new int[] { 1 + cursorX, 0 + cursorY, 0 + cursorZ };
//				int[] p3 = new int[] { 0 + cursorX, 0 + cursorY, 0 + cursorZ };
//				int[] p4 = new int[] { 0 + cursorX, 1 + cursorY, 1 + cursorZ };
//				int[] p5 = new int[] { 1 + cursorX, 1 + cursorY, 1 + cursorZ };
//				int[] p6 = new int[] { 1 + cursorX, 1 + cursorY, 0 + cursorZ };
//				int[] p7 = new int[] { 0 + cursorX, 1 + cursorY, 0 + cursorZ };
//
//				double[][] vertlist = new double[ 12 ][];
//
//				/* Find the vertices where the surface intersects the cube */
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 1 ) )
//				{
//					vertlist[ 0 ] = interpolatePoint( p0, p1, vertex_values[ 0 ],
//							vertex_values[ 1 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 2 ) )
//				{
//					vertlist[ 1 ] = interpolatePoint( p1, p2, vertex_values[ 1 ],
//							vertex_values[ 2 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 4 ) )
//				{
//					vertlist[ 2 ] = interpolatePoint( p2, p3, vertex_values[ 2 ],
//							vertex_values[ 3 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 8 ) )
//				{
//					vertlist[ 3 ] = interpolatePoint( p3, p0, vertex_values[ 3 ],
//							vertex_values[ 0 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 16 ) )
//				{
//					vertlist[ 4 ] = interpolatePoint( p4, p5, vertex_values[ 4 ],
//							vertex_values[ 5 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 32 ) )
//				{
//					vertlist[ 5 ] = interpolatePoint( p5, p6, vertex_values[ 5 ],
//							vertex_values[ 6 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 64 ) )
//				{
//					vertlist[ 6 ] = interpolatePoint( p6, p7, vertex_values[ 6 ],
//							vertex_values[ 7 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 128 ) )
//				{
//					vertlist[ 7 ] = interpolatePoint( p7, p4, vertex_values[ 7 ],
//							vertex_values[ 4 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 256 ) )
//				{
//					vertlist[ 8 ] = interpolatePoint( p0, p4, vertex_values[ 0 ],
//							vertex_values[ 4 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 512 ) )
//				{
//					vertlist[ 9 ] = interpolatePoint( p1, p5, vertex_values[ 1 ],
//							vertex_values[ 5 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 1024 ) )
//				{
//					vertlist[ 10 ] = interpolatePoint( p2, p6, vertex_values[ 2 ],
//							vertex_values[ 6 ] );
//				}
//				if ( 0 != ( MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ] & 2048 ) )
//				{
//					vertlist[ 11 ] = interpolatePoint( p3, p7, vertex_values[ 3 ],
//							vertex_values[ 7 ] );
//				}
//
//				/* Create the triangle */
//				for ( i = 0; MarchingCubesTables.triTable[ cubeindex ][ i ] != -1; i += 3 )
//				{
//
////					vertices.add( new double[] {vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 0 ],
////							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 1 ],
////							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 2 ]} );
////					vertices.add( new double[] {vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 0 ],
////									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 1 ],
////									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 2 ]} );
////					vertices.add( new double[] {vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 0 ],
////									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 1 ],
////									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 2 ]} );
//					TriangularFacet face = new TriangularFacet( new Vertex(
//							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 0 ],
//							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 1 ],
//							vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 2 ] ][ 2 ] ), new Vertex(
//									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 0 ],
//									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 1 ],
//									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i + 1 ] ][ 2 ] ),
//							new Vertex(
//									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 0 ],
//									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 1 ],
//									vertlist[ MarchingCubesTables.triTable[ cubeindex ][ i ] ][ 2 ] ) );
//					face.getArea();
//					output.addFace( face );
//				}
//			}
//		}
//		
////		computeNormals();
////		
////		float[][] verticesArray = new float[ vertices.size()][3];
////		float[][] normalsArray = new float[ vertices.size()][3];
////		int i = 0;
////		for ( double[] doubleV : vertices )
////		{
////			verticesArray[i][0] = ( float ) doubleV[0];
////			verticesArray[i][1] = ( float ) doubleV[1];
////			verticesArray[i][2] = ( float ) doubleV[2];
////			i++;
////		}
////
////		i = 0;
////		for ( double[] doubleV : normals )
////		{
////			normalsArray[i][0] = ( float ) doubleV[0];
////			normalsArray[i][1] = ( float ) doubleV[1];
////			normalsArray[i][2] = ( float ) doubleV[2];
////			i++;
////		}
////
////		output.setVertices( verticesArray );
////		output.setNormals( normalsArray );
////		output.setNumberOfVertices( vertices.size() );
////		
////		System.out.println("The End");
//		return output;
//	}
//
//	private double[] interpolatePoint( int[] p0, int[] p1, double v0, double v1 )
//	{
//		
//		double[] output = new double[3];
//
//		if (Math.abs(isolevel - v0) < 0.00001) {
//			for (int i = 0; i < 3; i++) {
//				output[i] = p0[i];
//			}
//		} else if (Math.abs(isolevel - v1) < 0.00001) {
//			for (int i = 0; i < 3; i++) {
//				output[i] = p1[i];
//			}
//		} else if (Math.abs(v0 - v1) < 0.00001) {
//			for (int i = 0; i < 3; i++) {
//				output[i] = p0[i];
//			}
//		} else {
//			double mu = (isolevel - v0) / (v1 - v0);
//
//			output[0] = p0[0] + mu * (p1[0] - p0[0]);
//			output[1] = p0[1] + mu * (p1[1] - p0[1]);
//			output[2] = p0[2] + mu * (p1[2] - p0[2]);
//		}
//
//		return output;
//	}
//
//	private int getCubeIndex( final double[] vertex_values )
//	{
//		int cubeindex = 0;
//		for ( int i = 0; i < 8; i++ )
//		{
//			if ( vertex_values[ i ] < isolevel )
//			{
//				cubeindex |= ( int ) Math.pow( 2, i );
//			}
//		}
//		return cubeindex;
//	}
//
//	private double[] mapFlatIterableToLookUpCube( final double[] vertex_values )
//	{
//		double[] vv = new double[ 8 ];
//		vv[ 0 ] = vertex_values[ 4 ];
//		vv[ 1 ] = vertex_values[ 5 ];
//		vv[ 2 ] = vertex_values[ 1 ];
//		vv[ 3 ] = vertex_values[ 0 ];
//		vv[ 4 ] = vertex_values[ 6 ];
//		vv[ 5 ] = vertex_values[ 7 ];
//		vv[ 6 ] = vertex_values[ 3 ];
//		vv[ 7 ] = vertex_values[ 2 ];
//
//		return vv;
//	}
//
//	private Cursor< LabelMultisetType > getCube(
//			final ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended,
//			final int cursorX, final int cursorY, final int cursorZ )
//	{
//		return Views.flatIterable( Views.interval( extended, new FinalInterval(
//				new long[] { cursorX, cursorY, cursorZ }, new long[] { cursorX + 1,
//						cursorY + 1, cursorZ + 1 } ) ) )
//				.cursor();
//	}
//	
//	private void computeNormals() {
//		for ( int i = 0; i < vertices.size()/3; i++ )
//		{
//			double[] v0 = vertices.get( i * 3);
//			double[] v1 = vertices.get( i * 3 + 1 );
//			double[] v2 = vertices.get( i * 3 + 2 );
//
//			double[] v1MinusV0 = { v1[ 0 ] - v0[ 0 ], v1[ 1 ] - v0[ 1 ], v1[ 2 ] - v0[ 2 ] };
//			double[] v2MinusV0 = { v2[ 0 ] - v0[ 0 ], v2[ 1 ] - v0[ 1 ], v2[ 2 ] - v0[ 2 ] };
//
//			normals.add( new double[] { ( v1MinusV0[ 1 ] * v2MinusV0[ 2 ] ) - ( v2MinusV0[ 1 ] * v1MinusV0[ 2 ] ),
//					( v1MinusV0[ 2 ] * v2MinusV0[ 0 ] ) - ( v2MinusV0[ 2 ] * v1MinusV0[ 0 ] ),
//					( v1MinusV0[ 0 ] * v2MinusV0[ 1 ] ) - ( v2MinusV0[ 0 ] * v1MinusV0[ 1 ] ) } );
//			
//			normals.add( new double[] { ( v1MinusV0[ 1 ] * v2MinusV0[ 2 ] ) - ( v2MinusV0[ 1 ] * v1MinusV0[ 2 ] ),
//					( v1MinusV0[ 2 ] * v2MinusV0[ 0 ] ) - ( v2MinusV0[ 2 ] * v1MinusV0[ 0 ] ),
//					( v1MinusV0[ 0 ] * v2MinusV0[ 1 ] ) - ( v2MinusV0[ 0 ] * v1MinusV0[ 1 ] ) } );
//
//			normals.add( new double[] { ( v1MinusV0[ 1 ] * v2MinusV0[ 2 ] ) - ( v2MinusV0[ 1 ] * v1MinusV0[ 2 ] ),
//					( v1MinusV0[ 2 ] * v2MinusV0[ 0 ] ) - ( v2MinusV0[ 2 ] * v1MinusV0[ 0 ] ),
//					( v1MinusV0[ 0 ] * v2MinusV0[ 1 ] ) - ( v2MinusV0[ 0 ] * v1MinusV0[ 1 ] ) } );
//
//		}
//	}
}

//else if ( implementationType.equals( "threeDViewer" ) )
//{
//	System.out.println( "MarchingCubes - ThreeDViewer" );
//	MarchingCubes_ThreeDViewer mc_threeDViewer = new MarchingCubes_ThreeDViewer();
//
//	begin = new Timestamp( System.currentTimeMillis() );
//	DefaultMesh m = mc_threeDViewer.calculate( volumeLabels, isoLevel );
//	end = new Timestamp( System.currentTimeMillis() );
//	System.out.println( "time for generating mesh: " + ( end.getTime() - begin.getTime() ) );
//
//	begin = new Timestamp( System.currentTimeMillis() );
//	System.out.println( "Converting mesh to scenery mesh..." );
//
//	List< Facet > facets = m.getFacets();
//	verticesArray = new float[ facets.size() * 3 * 3 ];
//	normalsArray = new float[ facets.size() * 3 * 3 ];
//
//	int count = 0;
//	List< Vertex > vertices;
//	for ( Facet facet : facets )
//	{
//		TriangularFacet tri = ( TriangularFacet ) facet;
//		vertices = tri.getVertices();
//		Vector3D normal = tri.getNormal();
//		for ( Vertex v : vertices )
//		{
//			for ( int d = 0; d < 3; d++ )
//			{
//				verticesArray[ count ] = ( float ) v.getDoublePosition( d );
//				if ( d == 0 )
//					normalsArray[ count ] = ( float ) normal.getX();
//				else if ( d == 1 )
//					normalsArray[ count ] = ( float ) normal.getY();
//				else if ( d == 2 )
//					normalsArray[ count ] = ( float ) normal.getZ();
//				count++;
//			}
//		}
//	}
//	end = new Timestamp( System.currentTimeMillis() );
//	System.out.println( "time for generating arrays: " + ( end.getTime() - begin.getTime() ) );
//}
