package marchingCubes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import bdv.labels.labelset.Label;
import bdv.labels.labelset.LabelMultisetType;
import bdv.labels.labelset.Multiset;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import viewer.Mesh;

/**
 * Marching cubes based on
 * https://github.com/ShibbyTheCookie/MarchingCubes/tree/master/MarchingCubesJava
 * 
 * Using generic type
 */
public class MarchingCubes_shibbyRAI
{
	/** Log */
	private float max = 0;
	private float min = Float.MAX_VALUE;
	
	private static final Logger LOGGER = Logger.getLogger( MarchingCubes_shibby.class.getName() );

	private ArrayList< float[] > vertices = new ArrayList<>();
	private ArrayList< float[] > normals = new ArrayList<>();

	private static float[] lerp( float[] vec1, float[] vec2, double alpha )
	{
		return new float[] { ( float ) ( vec1[ 0 ] + ( vec2[ 0 ] - vec1[ 0 ] ) * alpha ), ( float ) ( vec1[ 1 ] + ( vec2[ 1 ] - vec1[ 1 ] ) * alpha ), ( float ) ( vec1[ 2 ] + ( vec2[ 2 ] - vec1[ 2 ] ) * alpha ) };
	}

	// Actual position along edge weighted according to function values.
	private float vertList[][] = new float[ 12 ][ 3 ];

	public ArrayList< float[] > marchingCubes( RandomAccessibleInterval< LabelMultisetType > values, int[] volDim, float[] voxDim, int isoLevel, int offset )
	{
//		System.out.println("marching cubes");
		// Calculate maximal possible axis value (used in vertices
		// normalization)
		float maxX = voxDim[ 0 ] * ( volDim[ 0 ] - 1 );
		float maxY = voxDim[ 1 ] * ( volDim[ 1 ] - 1 );
		float maxZ = voxDim[ 2 ] * ( volDim[ 2 ] - 1 );
		float maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );

		Timestamp begin = new Timestamp( System.currentTimeMillis() );
		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( values, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { values
				.min( 0 ) - 1, values.min( 1 ) - 1, values.min( 2 ) - 1 }, new long[] { values.max(
						0 ) + 1, values.max( 1 ) + 1, values.max( 2 ) + 1 } ) )
				.localizingCursor();
		Timestamp end = new Timestamp( System.currentTimeMillis() );
		System.out.println( "time for generating cursor: " + ( end.getTime() - begin.getTime() ) );

		// Volume iteration
		while ( c.hasNext() )
		{
			c.next();

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );

			Cursor< LabelMultisetType > cu = Views.flatIterable( Views.interval( extended, new FinalInterval(
					new long[] { cursorX, cursorY, cursorZ }, new long[] { cursorX + 1,
							cursorY + 1, cursorZ + 1 } ) ) )
					.cursor();

			int i = 0;
			float[] vertex_values = new float[ 8 ];

//			begin = new Timestamp( System.currentTimeMillis() );
			while ( cu.hasNext() )
			{
				LabelMultisetType it = cu.next();
				for ( final Multiset.Entry< Label > e : it.entrySet() )
				{
					vertex_values[ i ] = e.getElement().id();
				}
				i++;
			}
//			end = new Timestamp( System.currentTimeMillis() );
//			System.out.println( "time for generating vertex values: " + ( end.getTime() - begin.getTime() ) );

			// @formatter:off
			// Indices pointing to cube vertices
			//              pyz  ___________________  pxyz
			//                  /|                 /|
			//                 / |                / |
			//                /  |               /  |
			//          pz   /___|______________/pxz|
			//              |    |              |   |
			//              |    |              |   |
			//              | py |______________|___| pxy
			//              |   /               |   /
			//              |  /                |  /
			//              | /                 | /
			//              |/__________________|/
			//             p                     px
			// @formatter:on

//			int p = x + ( volDim[ 0 ] * y ) + ( volDim[ 0 ] * volDim[ 1 ] * ( z + offset ) ),
//					px = p + 1,
//					py = p + volDim[ 0 ],
//					pxy = py + 1,
//					pz = p + volDim[ 0 ] * volDim[ 1 ],
//					pxz = px + volDim[ 0 ] * volDim[ 1 ],
//					pyz = py + volDim[ 0 ] * volDim[ 1 ],
//					pxyz = pxy + volDim[ 0 ] * volDim[ 1 ];
//
//					// X Y Z
//			float position[] = new float[] { x * voxDim[ 0 ], y * voxDim[ 1 ], ( z + offset ) * voxDim[ 2 ] };
			float position[] = new float[] { cursorX * voxDim[ 0 ], cursorY * voxDim[ 1 ], ( cursorZ + offset ) * voxDim[ 2 ] };
//
//			// Voxel intensities
//			T value0 = values[ p ],
//					value1 = values[ px ],
//					value2 = values[ py ],
//					value3 = values[ pxy ],
//					value4 = values[ pz ],
//					value5 = values[ pxz ],
//					value6 = values[ pyz ],
//					value7 = values[ pxyz ];

//			begin = new Timestamp( System.currentTimeMillis() );
			int p = 4,
					px = 5,
					py = 1,
					pxy = 0,
					pz = 6,
					pxz = 7,
					pyz = 3,
					pxyz = 2;

			// Voxel intensities
			float value0 = vertex_values[ p ],
					value1 = vertex_values[ px ],
					value2 = vertex_values[ py ],
					value3 = vertex_values[ pxy ],
					value4 = vertex_values[ pz ],
					value5 = vertex_values[ pxz ],
					value6 = vertex_values[ pyz ],
					value7 = vertex_values[ pxyz ];
			
			vertex_values = remapCube( vertex_values );

			// Voxel is active if its value matches the isolevel
			int cubeindex = 0;
			for ( i = 0; i < 8; i++ )
			{
				if ( vertex_values[ i ] < isoLevel )
				{
					cubeindex |= ( int ) Math.pow( 2, i );
				}
			}
//			if ( value0 < isoLevel )
//				cubeindex |= 1;
//			if ( value1 < isoLevel )
//				cubeindex |= 2;
//			if ( value2 < isoLevel )
//				cubeindex |= 8;
//			if ( value3 < isoLevel )
//				cubeindex |= 4;
//			if ( value4 < isoLevel )
//				cubeindex |= 16;
//			if ( value5 < isoLevel )
//				cubeindex |= 32;
//			if ( value6 < isoLevel )
//				cubeindex |= 128;
//			if ( value7 < isoLevel )
//				cubeindex |= 64;

			// Fetch the triggered edges
			int bits = MarchingCubesTables.MC_EDGE_TABLE[ cubeindex ];

//			end = new Timestamp( System.currentTimeMillis() );
//			System.out.println( "time for calculate bits: " + ( end.getTime() - begin.getTime() ) );

			// If no edge is triggered... skip
			if ( bits == 0 )
				continue;

			// Interpolate the positions based od voxel intensities
			double mu = 0.5f;

			// bottom of the cube
			if ( ( bits & 1 ) != 0 )
			{
				mu = ( isoLevel - value0 ) / ( value1 - value0 );
				vertList[ 0 ] = lerp( position, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] }, mu );
			}
			if ( ( bits & 2 ) != 0 )
			{
				mu = ( isoLevel - value1 ) / ( value3 - value1 );
				vertList[ 1 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, mu );
			}
			if ( ( bits & 4 ) != 0 )
			{
				mu = ( isoLevel - value2 ) / ( value3 - value2 );
				vertList[ 2 ] = lerp( new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, mu );
			}
			if ( ( bits & 8 ) != 0 )
			{
				mu = ( isoLevel - value0 ) / ( value2 - value0 );
				vertList[ 3 ] = lerp( position, new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, mu );
			}
			// top of the cube
			if ( ( bits & 16 ) != 0 )
			{
				mu = ( isoLevel - value4 ) / ( value5 - value4 );
				vertList[ 4 ] = lerp( new float[] { position[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			if ( ( bits & 32 ) != 0 )
			{
				mu = ( isoLevel - value5 ) / ( value7 - value5 );
				vertList[ 5 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			if ( ( bits & 64 ) != 0 )
			{
				mu = ( isoLevel - value6 ) / ( value7 - value6 );
				vertList[ 6 ] = lerp( new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			if ( ( bits & 128 ) != 0 )
			{
				mu = ( isoLevel - value4 ) / ( value6 - value4 );
				vertList[ 7 ] = lerp( new float[] { position[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			// vertical lines of the cube
			if ( ( bits & 256 ) != 0 )
			{
				mu = ( isoLevel - value0 ) / ( value4 - value0 );
				vertList[ 8 ] = lerp( position, new float[] { position[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			if ( ( bits & 512 ) != 0 )
			{
				mu = ( isoLevel - value1 ) / ( value5 - value1 );
				vertList[ 9 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			if ( ( bits & 1024 ) != 0 )
			{
				mu = ( isoLevel - value3 ) / ( value7 - value3 );
				vertList[ 10 ] = lerp( new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, new float[] { position[ 0 ] + voxDim[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}
			if ( ( bits & 2048 ) != 0 )
			{
				mu = ( isoLevel - value2 ) / ( value6 - value2 );
				vertList[ 11 ] = lerp( new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] }, new float[] { position[ 0 ], position[ 1 ] + voxDim[ 1 ], position[ 2 ] + voxDim[ 2 ] }, mu );
			}

			// construct triangles -- get correct vertices from
			// triTable.
			i = 0;
			// "Re-purpose cubeindex into an offset into triTable."
			cubeindex <<= 4;

			while ( MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i ] != -1 )
			{
				int index1 = MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i ];
				int index2 = MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i + 1 ];
				int index3 = MarchingCubesTables.MC_TRI_TABLE[ cubeindex + i + 2 ];

				// Add triangles vertices normalized with the maximal
				// possible value
				vertices.add( new float[] { vertList[ index3 ][ 0 ] / maxAxisVal - 0.5f, vertList[ index3 ][ 1 ] / maxAxisVal - 0.5f, vertList[ index3 ][ 2 ] / maxAxisVal - 0.5f } );
				vertices.add( new float[] { vertList[ index2 ][ 0 ] / maxAxisVal - 0.5f, vertList[ index2 ][ 1 ] / maxAxisVal - 0.5f, vertList[ index2 ][ 2 ] / maxAxisVal - 0.5f } );
				vertices.add( new float[] { vertList[ index1 ][ 0 ] / maxAxisVal - 0.5f, vertList[ index1 ][ 1 ] / maxAxisVal - 0.5f, vertList[ index1 ][ 2 ] / maxAxisVal - 0.5f } );
				vertices.add( new float[] { vertList[ index3 ][ 0 ], vertList[ index3 ][ 1 ], vertList[ index3 ][ 2 ] } );
				vertices.add( new float[] { vertList[ index2 ][ 0 ], vertList[ index2 ][ 1 ], vertList[ index2 ][ 2 ] } );
				vertices.add( new float[] { vertList[ index1 ][ 0 ], vertList[ index1 ][ 1 ], vertList[ index1 ][ 2 ] } );

				i += 3;
			}

			LOGGER.log( Level.FINEST, "Number of vertices: " + vertices.size() );

		} // end while
		LOGGER.log( Level.FINEST, "total number of vertices: " + vertices.size() );
		
		calculateNormals();
		return vertices;
	}
	
	private float[] remapCube( final float[] vertex_values )
	{
		float[] vv = new float[ 8 ];
		vv[ 0 ] = vertex_values[ 4 ];
		vv[ 1 ] = vertex_values[ 5 ];
		vv[ 2 ] = vertex_values[ 1 ];
		vv[ 3 ] = vertex_values[ 0 ];
		vv[ 4 ] = vertex_values[ 6 ];
		vv[ 5 ] = vertex_values[ 7 ];
		vv[ 6 ] = vertex_values[ 3 ];
		vv[ 7 ] = vertex_values[ 2 ];
		
//		vv[ 0 ] = vertex_values[ 0 ];
//		vv[ 1 ] = vertex_values[ 1 ];
//		vv[ 2 ] = vertex_values[ 4 ];
//		vv[ 3 ] = vertex_values[ 5 ];
//		vv[ 4 ] = vertex_values[ 2 ];
//		vv[ 5 ] = vertex_values[ 3 ];
//		vv[ 6 ] = vertex_values[ 6 ];
//		vv[ 7 ] = vertex_values[ 7 ];

		return vv;
	}
	
	private void calculateNormals()
	{
		int vertexCount = vertices.size();

		// omp parallel for
		for ( int i = 0; i < vertexCount/3; ++i )
		{
			float[] vec1 = new float[ 3 ], 
					vec2 = new float[ 3 ],
					normal = new float[ 3 ];
			
			vec1[ 0 ] = vertices.get( i * 3 + 1 )[ 0 ] - vertices.get( i * 3 )[ 0 ];
			vec1[ 1 ] = vertices.get( i * 3 + 1 )[ 1 ] - vertices.get( i * 3 )[ 1 ];
			vec1[ 2 ] = vertices.get( i * 3 + 1 )[ 2 ] - vertices.get( i * 3 )[ 2 ];
			vec2[ 0 ] = vertices.get( i * 3 + 2 )[ 0 ] - vertices.get( i * 3 )[ 0 ];
			vec2[ 1 ] = vertices.get( i * 3 + 2 )[ 1 ] - vertices.get( i * 3 )[ 1 ];
			vec2[ 2 ] = vertices.get( i * 3 + 2 )[ 2 ] - vertices.get( i * 3 )[ 2 ];
			normal[ 0 ] = vec1[ 2 ] * vec2[ 1 ] - vec1[ 1 ] * vec2[ 2 ];
			normal[ 1 ] = vec1[ 0 ] * vec2[ 2 ] - vec1[ 2 ] * vec2[ 0 ];
			normal[ 2 ] = vec1[ 1 ] * vec2[ 0 ] - vec1[ 0 ] * vec2[ 1 ];
			
			normals.add( new float[] {normal[0], normal[1], normal[2] } );
			normals.add( new float[] {normal[0], normal[1], normal[2] } );
			normals.add( new float[] {normal[0], normal[1], normal[2] } );
		}

		// omp parallel for
		for ( int i = 0; i < normals.size(); ++i )
		{
			float length = ( float ) Math.sqrt( normals.get( i )[ 0 ] * normals.get( i )[ 0 ] + normals.get( i )[ 1 ] * normals.get( i )[ 1 ] + normals.get( i )[ 2 ] * normals.get( i )[ 2 ] );
			normals.set( i, new float[] {normals.get( i )[0] / length, normals.get( i )[1] / length, normals.get( i )[2] / length} );
		}
	}
	
	public ArrayList< float[] > getNormals()
	{
		return normals;
	}
}
