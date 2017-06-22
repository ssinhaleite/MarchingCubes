package marchingCubes;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import bdv.labels.labelset.Label;
import bdv.labels.labelset.LabelMultisetType;
import bdv.labels.labelset.Multiset;
import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import viewer.Mesh;

/**
 * MarchingCubes from
 * https://github.com/funkey/sg_gui/blob/master/MarchingCubes.h
 */
public class MarchingCubesRAI
{
	/** List of Point3ds which form the isosurface. */
	private HashMap< Long, Point3dId > id2Point3dId = new HashMap< Long, Point3dId >();

	/** the mesh that represents the surface. */
	private Mesh mesh;

	/** List of Triangles which form the triangulation of the isosurface. */
	private Vector< Triangle > triangleVector = new Vector< Triangle >();

	/** No. of cells in x and y directions. */
	private long nCellsX, nCellsY;

	private int width, height, depth;

	/** The isosurface value. */
	private int isoLevel;

	private float maxAxisVal;

	/** Indicates whether a valid surface is present. */
	private boolean hasValidSurface;

	/**
	 * Indicates if the threshold will be applied for the exact value or above
	 * it
	 */
	private boolean acceptExactly = false;

	/**
	 * A point in 3D with an id
	 */
	private class Point3dId
	{
		private long id;

		private float x, y, z;

		/** constructor from RealPoint 3d - x, y, z */
		private Point3dId( RealPoint point )
		{
			id = 0;
			x = point.getFloatPosition( 0 );
			y = point.getFloatPosition( 1 );
			z = point.getFloatPosition( 2 );
		}
	}

	/**
	 * Triples of points that form a triangle.
	 */
	private class Triangle
	{
		private long[] point = new long[ 3 ];
	}

	public MarchingCubesRAI()
	{
		nCellsX = 0;
		nCellsY = 0;
		hasValidSurface = false;
		acceptExactly = false;
	}

	/**
	 * Creates the mesh given a volume
	 * 
	 * @param voxDim
	 *            dimensions x, y, z of the cube
	 * @param volDim
	 *            dimensions x, y, z of the volume
	 * @return a triangle mesh
	 */
	public Mesh generateSurface( RandomAccessibleInterval< LabelMultisetType > input, float[] voxDim, int[] volDim, boolean isExact, int level )
	{
		if ( hasValidSurface )
			deleteSurface();

		float maxX = voxDim[ 0 ] * ( volDim[ 0 ] - 1 );
		float maxY = voxDim[ 1 ] * ( volDim[ 1 ] - 1 );
		float maxZ = voxDim[ 2 ] * ( volDim[ 2 ] - 1 );
		maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );

		mesh = new Mesh();

		isoLevel = level;

		width = volDim[ 0 ];
		height = volDim[ 1 ];
		depth = volDim[ 2 ];

		nCellsX = ( long ) Math.ceil( width / voxDim[ 0 ] ) - 1;
		nCellsY = ( long ) Math.ceil( height / voxDim[ 1 ] ) - 1;
		acceptExactly = isExact;

		Timestamp begin = new Timestamp( System.currentTimeMillis() );

		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( input, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.interval( extended, new FinalInterval( new long[] { input
				.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
						0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) )
				.localizingCursor();

		Timestamp end = new Timestamp( System.currentTimeMillis() );
//		System.out.println( "preparing RAI: " + ( end.getTime() - begin.getTime() ) );

		while ( c.hasNext() )
		{
//			begin = new Timestamp( System.currentTimeMillis() );
			c.next();

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );

			if ( cursorX == -1 || cursorY == -1 || cursorZ == -1 )
				continue;
			
			if ( cursorX == volDim[0] || cursorY == volDim[1] || cursorZ == volDim[2] )
				continue;

			Cursor< LabelMultisetType > cu = getCube( extended, cursorX, cursorY, cursorZ );

			int i = 0;
			double[] vertex_values = new double[ 8 ];
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
//			System.out.println( "getting cube information: " + ( end.getTime() - begin.getTime() ) );

			// @formatter:off
			// the values from the cube are given first in z, then y, then x
			// this way, the vertex_values (from getCube) are positioned in this way:
			//
			//  1------3
			// /|     /|
			// 0-----2 |
			// |5----|-7
			// |/    |/
			// 4-----6
			//
			// this algorithm (based on http://paulbourke.net/geometry/polygonise/)
			// considers the vertices of the cube in this order:
			//
			//  4------5
			// /|     /|
			// 7-----6 |
			// |0----|-1
			// |/    |/
			// 3-----2
			//
			// This way, we need to remap the cube vertices:
			// @formatter:on

//			begin = new Timestamp( System.currentTimeMillis() );
			vertex_values = remapCube( vertex_values );

			// Calculate table lookup index from those vertices which are
			// below the isolevel.
			
			System.out.println("cube: " + vertex_values[0] + " " +
					  + vertex_values[1] + " " +
					  + vertex_values[2] + " " +
					  + vertex_values[3] + " " +
					  + vertex_values[4] + " " +
					  + vertex_values[5] + " " +
					  + vertex_values[6] + " " +
					  + vertex_values[7] );


			int tableIndex = 0;
			for ( i = 0; i < 8; i++ )
			{
				if ( !interiorTest( vertex_values[ i ] ) )
				{
					tableIndex |= ( int ) Math.pow( 2, i );
				}
			}
			
			System.out.println("tableIndex: " + tableIndex);

			// edge indexes:
			// @formatter:off
			//          4-----*4*----5
			//         /|           /|
			//        /*8*         / |
			//      *7* |        *5* |
			//      /   |        /  *9*
			//     7-----*6*----6    |
			//     |    0----*0*-----1
			//   *11*  /       *10* /
			//     |  /         | *1*
			//     |*3*         | /
			//     |/           |/
			//     3-----*2*----2
			// @formatter: on

			// Now create a triangulation of the isosurface in this cell.
			if ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] != 0 )
			{
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 8 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 3, vertex_values[ 3 ], vertex_values[ 0 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 3 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 0, vertex_values[ 0 ], vertex_values[ 1 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 0 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 256 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 8, vertex_values[ 0 ], vertex_values[ 4 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 8 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 4 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 2, vertex_values[ 2 ], vertex_values[ 3 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 2 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2048 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 11, vertex_values[ 3 ], vertex_values[ 7 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 11 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 1, vertex_values[ 1 ], vertex_values[ 2 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 1 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 512 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 9, vertex_values[ 1 ], vertex_values[ 5 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 9 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 16 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 4, vertex_values[ 4 ], vertex_values[ 5 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 4 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 128 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 7, vertex_values[ 7 ], vertex_values[ 4 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 7 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1024 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 10, vertex_values[ 2 ], vertex_values[ 6 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 10 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 64 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 6, vertex_values[ 6 ], vertex_values[ 7 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 6 );
					id2Point3dId.put( id, pt );
				}
				if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 32 ) != 0 )
				{
					Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 5, vertex_values[ 5 ], vertex_values[ 6 ] );
					long id = getEdgeId( cursorX, cursorY, cursorZ, 5 );
					id2Point3dId.put( id, pt );
				}

				for ( i = 0; MarchingCubesTables.triTable[ tableIndex ][ i ] != MarchingCubesTables.Invalid; i += 3 )
				{
					Triangle triangle = new Triangle();
					long pointId0, pointId1, pointId2;
					pointId0 = getEdgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i ] );
					pointId1 = getEdgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i + 1 ] );
					pointId2 = getEdgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i + 2 ] );
					triangle.point[ 0 ] = pointId0;
					triangle.point[ 1 ] = pointId1;
					triangle.point[ 2 ] = pointId2;
					triangleVector.add( triangle );
				}
			}
			
//			end = new Timestamp( System.currentTimeMillis() );
//			System.out.println( "creating triangles: " + ( end.getTime() - begin.getTime() ) );
		}

//		begin = new Timestamp( System.currentTimeMillis() );

		renameVerticesAndTriangles();
		calculateNormals();
		normalizeVerticesAndNormals();
		hasValidSurface = true;
		
//		end = new Timestamp( System.currentTimeMillis() );
//		System.out.println( "finalizing mesh: " + ( end.getTime() - begin.getTime() ) );

		return mesh;
	}

	private void deleteSurface()
	{
		nCellsX = 0;
		nCellsY = 0;
		hasValidSurface = false;
	}

	private void renameVerticesAndTriangles()
	{
		long nextId = 0;
		Iterator< Entry< Long, Point3dId > > mapIterator = id2Point3dId.entrySet().iterator();
		Iterator< Triangle > vecIterator = triangleVector.iterator();
		while ( mapIterator.hasNext() )
		{
			HashMap.Entry< Long, Point3dId > entry = mapIterator.next();
			entry.getValue().id = nextId;
			nextId++;
		}

		// Now rename triangles.
		while ( vecIterator.hasNext() )
		{
			Triangle next = vecIterator.next();
			for ( int i = 0; i < 3; i++ )
			{
				long newId = id2Point3dId.get( next.point[ i ] ).id;
				next.point[ i ] = newId;
			}
		}

		// Copy all the vertices and triangles into two arrays so that they
		// can be efficiently accessed.
		// Copy vertices.
		int numberOfVertices = id2Point3dId.size();
		mesh.setNumberOfVertices( numberOfVertices );

		mapIterator = id2Point3dId.entrySet().iterator();
		float[][] vertices = new float[ numberOfVertices ][ 3 ];

		for ( int i = 0; i < numberOfVertices; i++ )
		{
			HashMap.Entry< Long, Point3dId > entry = mapIterator.next();
			vertices[ i ][ 0 ] = entry.getValue().x;
			vertices[ i ][ 1 ] = entry.getValue().y;
			vertices[ i ][ 2 ] = entry.getValue().z;
		}

		mesh.setVertices( vertices );
		// Copy vertex indices which make triangles.
		vecIterator = triangleVector.iterator();
		int numberOfTriangles = triangleVector.size();
		mesh.numberOfTriangles = numberOfTriangles;
		int[] faces = new int[ numberOfTriangles * 3 ];
		for ( int i = 0; i < numberOfTriangles; i++ )
		{
			Triangle next = vecIterator.next();
			faces[ i * 3 ] = ( int ) next.point[ 0 ];
			faces[ i * 3 + 1 ] = ( int ) next.point[ 1 ];
			faces[ i * 3 + 2 ] = ( int ) next.point[ 2 ];
		}

		mesh.setTriangles( faces );
		id2Point3dId.clear();
		triangleVector.clear();
	}

	private void calculateNormals()
	{
		int vertexCount = mesh.getNumberOfVertices();
		float[][] vertices = mesh.getVertices();
		int triangleCount = mesh.getNumberOfTriangles();
		int[] triangles = mesh.getTriangles();

		float[][] normals = new float[ vertexCount ][ 3 ];

		// Set all normals to 0.
		// omp parallel for
		for ( int i = 0; i < vertexCount; ++i )
		{
			normals[ i ][ 0 ] = 0;
			normals[ i ][ 1 ] = 0;
			normals[ i ][ 2 ] = 1;
		}

		// Calculate normals.
		// omp parallel for
		for ( int i = 0; i < triangleCount; ++i )
		{
			float[] vec1 = new float[ 3 ],
					vec2 = new float[ 3 ],
					normal = new float[ 3 ];

			int id0, id1, id2;

			id0 = triangles[ i * 3 ];
			id1 = triangles[ i * 3 + 1 ];
			id2 = triangles[ i * 3 + 2 ];
			vec1[ 0 ] = vertices[ id1 ][ 0 ] - vertices[ id0 ][ 0 ];
			vec1[ 1 ] = vertices[ id1 ][ 1 ] - vertices[ id0 ][ 1 ];
			vec1[ 2 ] = vertices[ id1 ][ 2 ] - vertices[ id0 ][ 2 ];
			vec2[ 0 ] = vertices[ id2 ][ 0 ] - vertices[ id0 ][ 0 ];
			vec2[ 1 ] = vertices[ id2 ][ 1 ] - vertices[ id0 ][ 1 ];
			vec2[ 2 ] = vertices[ id2 ][ 2 ] - vertices[ id0 ][ 2 ];
			normal[ 0 ] = vec1[ 2 ] * vec2[ 1 ] - vec1[ 1 ] * vec2[ 2 ];
			normal[ 1 ] = vec1[ 0 ] * vec2[ 2 ] - vec1[ 2 ] * vec2[ 0 ];
			normal[ 2 ] = vec1[ 1 ] * vec2[ 0 ] - vec1[ 0 ] * vec2[ 1 ];
			normals[ id0 ][ 0 ] += normal[ 0 ];
			normals[ id0 ][ 1 ] += normal[ 1 ];
			normals[ id0 ][ 2 ] += normal[ 2 ];
			normals[ id1 ][ 0 ] += normal[ 0 ];
			normals[ id1 ][ 1 ] += normal[ 1 ];
			normals[ id1 ][ 2 ] += normal[ 2 ];
			normals[ id2 ][ 0 ] += normal[ 0 ];
			normals[ id2 ][ 1 ] += normal[ 1 ];
			normals[ id2 ][ 2 ] += normal[ 2 ];
		}

		// Normalize normals.
		// omp parallel for
		for ( int i = 0; i < vertexCount; ++i )
		{
			float length = ( float ) Math.sqrt(
					normals[ i ][ 0 ] * normals[ i ][ 0 ] +
							normals[ i ][ 1 ] * normals[ i ][ 1 ] +
							normals[ i ][ 2 ] * normals[ i ][ 2 ] );

			normals[ i ][ 0 ] /= length;
			normals[ i ][ 1 ] /= length;
			normals[ i ][ 2 ] /= length;
		}
		mesh.setNormals( normals );
	}

	private Point3dId calculateIntersection( int nX, int nY, int nZ, int nEdgeNo, double vertex_values, double vertex_values2 )
	{
		RealPoint p1 = new RealPoint( 3 ),
				p2 = new RealPoint( 3 );
		int v1x = nX, v1y = nY, v1z = nZ;
		int v2x = nX, v2y = nY, v2z = nZ;

		switch ( nEdgeNo )
		{
		case 0:
			v2y += 1;
			break;
		case 1:
			v1y += 1;
			v2x += 1;
			v2y += 1;
			break;
		case 2:
			v1x += 1;
			v1y += 1;
			v2x += 1;
			break;
		case 3:
			v1x += 1;
			break;
		case 4:
			v1z += 1;
			v2y += 1;
			v2z += 1;
			break;
		case 5:
			v1y += 1;
			v1z += 1;
			v2x += 1;
			v2y += 1;
			v2z += 1;
			break;
		case 6:
			v1x += 1;
			v1y += 1;
			v1z += 1;
			v2x += 1;
			v2z += 1;
			break;
		case 7:
			v1x += 1;
			v1z += 1;
			v2z += 1;
			break;
		case 8:
			v2z += 1;
			break;
		case 9:
			v1y += 1;
			v2y += 1;
			v2z += 1;
			break;
		case 10:
			v1x += 1;
			v1y += 1;
			v2x += 1;
			v2y += 1;
			v2z += 1;
			break;
		case 11:
			v1x += 1;
			v2x += 1;
			v2z += 1;
			break;
		}

		p1.setPosition( ( v1x - 1) /** cellSizeX*/, 0 );
		p1.setPosition( ( v1y -1  ) /** cellSizeY*/, 1 );
		p1.setPosition( ( v1z - 1) /** cellSizeZ*/, 2 );
		p2.setPosition( ( v2x -1 ) /** cellSizeX*/, 0 );
		p2.setPosition( ( v2y - 1) /** cellSizeY*/, 1 );
		p2.setPosition( ( v2z - 1) /** cellSizeZ*/, 2 );

		if ( interiorTest( vertex_values ) && !interiorTest( vertex_values2 ) )
		{
			return findSurfaceIntersection( p2, p1, vertex_values2, vertex_values );
		}
		else
		{
			return findSurfaceIntersection( p1, p2, vertex_values, vertex_values2 );
		}
	}

	private long getEdgeId( long nX, long nY, long nZ, int nEdgeNo )
	{
		switch ( nEdgeNo )
		{
		case 0:
			return getVertexId( nX, nY, nZ ) + 1;
		case 1:
			return getVertexId( nX, nY + 1, nZ );
		case 2:
			return getVertexId( nX + 1, nY, nZ ) + 1;
		case 3:
			return getVertexId( nX, nY, nZ );
		case 4:
			return getVertexId( nX, nY, nZ + 1 ) + 1;
		case 5:
			return getVertexId( nX, nY + 1, nZ + 1 );
		case 6:
			return getVertexId( nX + 1, nY, nZ + 1 ) + 1;
		case 7:
			return getVertexId( nX, nY, nZ + 1 );
		case 8:
			return getVertexId( nX, nY, nZ ) + 2;
		case 9:
			return getVertexId( nX, nY + 1, nZ ) + 2;
		case 10:
			return getVertexId( nX + 1, nY + 1, nZ ) + 2;
		case 11:
			return getVertexId( nX + 1, nY, nZ ) + 2;
		default:
			// Invalid edge no.
			return MarchingCubesTables.Invalid;
		}
	}

	private long getVertexId( long nX, long nY, long nZ )
	{
		return 3 * ( nZ * ( nCellsY + 1 ) * ( nCellsX + 1 ) + nY * ( nCellsX + 1 ) + nX );
	}

	private Point3dId findSurfaceIntersection( RealPoint p1, RealPoint p2, double val1, double val2 )
	{

		Point3dId interpolation = null;

		// binary search for intersection
		float mu = ( float ) 0.5;
		float delta = ( float ) 0.25;

		// assume that p1 is outside, p2 is inside
		//
		// mu == 0 -> p1, mu == 1 -> p2
		//
		// incrase mu -> go to inside
		// decrease mu -> go to outside

		for ( long i = 0; i < 10; i++, delta /= 2.0 )
		{

			float diffX = p2.getFloatPosition( 0 ) - p1.getFloatPosition( 0 );
			float diffY = p2.getFloatPosition( 1 ) - p1.getFloatPosition( 1 );
			float diffZ = p2.getFloatPosition( 2 ) - p1.getFloatPosition( 2 );

			diffX = diffX * mu;
			diffY = diffY * mu;
			diffZ = diffZ * mu;

			diffX = diffX + p1.getFloatPosition( 0 );
			diffY = diffY + p1.getFloatPosition( 1 );
			diffZ = diffZ + p1.getFloatPosition( 2 );

			RealPoint diff = new RealPoint( diffX, diffY, diffZ );
			interpolation = new Point3dId( diff );// p1 + mu*(p2-p1);

			if ( interiorTest( val1 + mu * ( val2 - val1 ) ) )
				mu -= delta; // go to outside
			else
				mu += delta; // go to inside
		}

		return interpolation;
	}

	private boolean interiorTest( double vertex_values )
	{

		if ( acceptExactly )
		{
			if ( ( int ) vertex_values == isoLevel )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		else
		{
			if ( ( int ) vertex_values < isoLevel )
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}

	private double[] remapCube( final double[] vertex_values )
	{
		double[] vv = new double[ 8 ];
		vv[ 0 ] = vertex_values[ 5 ];
		vv[ 1 ] = vertex_values[ 7 ];
		vv[ 2 ] = vertex_values[ 6 ];
		vv[ 3 ] = vertex_values[ 4 ];
		vv[ 4 ] = vertex_values[ 1 ];
		vv[ 5 ] = vertex_values[ 3 ];
		vv[ 6 ] = vertex_values[ 2 ];
		vv[ 7 ] = vertex_values[ 0 ];

		return vv;
	}

	private Cursor< LabelMultisetType > getCube(
			final ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended,
			final int cursorX, final int cursorY, final int cursorZ )
	{
		return Views.flatIterable( Views.interval( extended, new FinalInterval(
				new long[] { cursorX, cursorY, cursorZ }, new long[] { cursorX + 1,
						cursorY + 1, cursorZ + 1 } ) ) )
				.cursor();
	}

	private void normalizeVerticesAndNormals()
	{
		int vertexCount = mesh.getNumberOfVertices();
		float[][] vertices = mesh.getVertices();
		float[][] normals = mesh.getNormals();
		
		// omp parallel for
		for ( int i = 0; i < vertexCount; ++i )
		{
			vertices[ i ][ 0 ] /= maxAxisVal;
			vertices[ i ][ 1 ] /= maxAxisVal;
			vertices[ i ][ 2 ] /= maxAxisVal;

			normals[ i ][ 0 ] /= maxAxisVal;
			normals[ i ][ 1 ] /= maxAxisVal;
			normals[ i ][ 2 ] /= maxAxisVal;
		}

		mesh.setVertices( vertices );
		mesh.setNormals( normals );
	}
	
	public Mesh generateSurface2( RandomAccessibleInterval< LabelMultisetType > input, float[] voxDim, int[] volDim, boolean isExact, int level )
	{
		if ( hasValidSurface )
			deleteSurface();

		float maxX = voxDim[ 0 ] * ( volDim[ 0 ] - 1 );
		float maxY = voxDim[ 1 ] * ( volDim[ 1 ] - 1 );
		float maxZ = voxDim[ 2 ] * ( volDim[ 2 ] - 1 );
		maxAxisVal = Math.max( maxX, Math.max( maxY, maxZ ) );

		mesh = new Mesh();

		isoLevel = level;

		width = volDim[ 0 ];
		height = volDim[ 1 ];
		depth = volDim[ 2 ];

		nCellsX = ( long ) Math.ceil( width / voxDim[ 0 ] ) - 1;
		nCellsY = ( long ) Math.ceil( height / voxDim[ 1 ] ) - 1;
		acceptExactly = isExact;

		ExtendedRandomAccessibleInterval< LabelMultisetType, RandomAccessibleInterval< LabelMultisetType > > extended =
				Views.extendValue( input, new LabelMultisetType() );
		Cursor< LabelMultisetType > c = Views.flatIterable( Views.interval( extended, new FinalInterval(
				new long[] { input
						.min( 0 ) - 1, input.min( 1 ) - 1, input.min( 2 ) - 1 }, new long[] { input.max(
								0 ) + 1, input.max( 1 ) + 1, input.max( 2 ) + 1 } ) ) )
				.localizingCursor();

		List<Long> volume = new ArrayList<Long>();

		while ( c.hasNext() )
		{
			LabelMultisetType it = c.next();

			int cursorX = c.getIntPosition( 0 );
			int cursorY = c.getIntPosition( 1 );
			int cursorZ = c.getIntPosition( 2 );
			
			if ( cursorX == -1 || cursorY == -1 || cursorZ == -1 )
				continue;

			for ( final Multiset.Entry< Label > e : it.entrySet() )
			{
				volume.add( e.getElement().id());
			}
		}

		int xWidth = ( volDim[0] + 1 );
		int xyWidth = xWidth * ( volDim[1] + 1 );

		double[] vertex_values = new double[ 8 ];

		for( int cursorZ = 0; cursorZ < volDim[2] ; cursorZ++)
		{
			for( int cursorY = 0; cursorY < volDim[1] ; cursorY++)
			{
				for( int cursorX = 0; cursorX < volDim[0] ; cursorX++)
				{
					vertex_values[7] = volume.get( cursorZ * xyWidth + cursorY * xWidth + cursorX );
					vertex_values[6] = volume.get( cursorZ * xyWidth + ( cursorY + 1 ) * xWidth + cursorX );
					vertex_values[5] = volume.get( cursorZ * xyWidth + ( cursorY + 1 ) * xWidth + ( cursorX + 1 ) );
					vertex_values[4] = volume.get( cursorZ * xyWidth + cursorY * xWidth + ( cursorX + 1 ) );
					vertex_values[3] = volume.get( ( cursorZ + 1 ) * xyWidth + cursorY * xWidth + cursorX );
					vertex_values[2] = volume.get( ( cursorZ + 1 ) * xyWidth + ( cursorY + 1 ) * xWidth + cursorX );
					vertex_values[1] = volume.get( ( cursorZ + 1 ) * xyWidth + ( cursorY + 1 ) * xWidth + ( cursorX + 1 ) );
					vertex_values[0] = volume.get( ( cursorZ + 1 ) * xyWidth + cursorY * xWidth + ( cursorX + 1 ) );

					// @formatter:off
					// this algorithm (based on http://paulbourke.net/geometry/polygonise/)
					// considers the vertices of the cube in this order:
					//
					//  4------5
					// /|     /|
					// 7-----6 |
					// |0----|-1
					// |/    |/
					// 3-----2
					//
					// @formatter:on

					// Calculate table lookup index from those vertices which
					// are below the isolevel.
					int tableIndex = 0;
					for ( int i = 0; i < 8; i++ )
					{
						if ( !interiorTest( vertex_values[ i ] ) )
						{
							tableIndex |= ( int ) Math.pow( 2, i );
						}
					}

					// edge indexes:
					// @formatter:off
					//          4-----*4*----5
					//         /|           /|
					//        /*8*         / |
					//      *7* |        *5* |
					//      /   |        /  *9*
					//     7-----*6*----6    |
					//     |    0----*0*-----1
					//   *11*  /       *10* /
					//     |  /         | *1*
					//     |*3*         | /
					//     |/           |/
					//     3-----*2*----2
					// @formatter: on

					// Now create a triangulation of the isosurface in this cell.
					if ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] != 0 )
					{
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 8 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 3, vertex_values[ 3 ], vertex_values[ 0 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 3 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 0, vertex_values[ 0 ], vertex_values[ 1 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 0 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 256 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 8, vertex_values[ 0 ], vertex_values[ 4 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 8 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 4 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 2, vertex_values[ 2 ], vertex_values[ 3 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 2 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2048 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 11, vertex_values[ 3 ], vertex_values[ 7 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 11 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 2 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 1, vertex_values[ 1 ], vertex_values[ 2 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 1 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 512 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 9, vertex_values[ 1 ], vertex_values[ 5 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 9 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 16 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 4, vertex_values[ 4 ], vertex_values[ 5 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 4 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 128 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 7, vertex_values[ 7 ], vertex_values[ 4 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 7 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 1024 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 10, vertex_values[ 2 ], vertex_values[ 6 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 10 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 64 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 6, vertex_values[ 6 ], vertex_values[ 7 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 6 );
							id2Point3dId.put( id, pt );
						}
						if ( ( MarchingCubesTables.MC_EDGE_TABLE[ tableIndex ] & 32 ) != 0 )
						{
							Point3dId pt = calculateIntersection( cursorX, cursorY, cursorZ, 5, vertex_values[ 5 ], vertex_values[ 6 ] );
							long id = getEdgeId( cursorX, cursorY, cursorZ, 5 );
							id2Point3dId.put( id, pt );
						}
		
						for ( int i = 0; MarchingCubesTables.triTable[ tableIndex ][ i ] != MarchingCubesTables.Invalid; i += 3 )
						{
							Triangle triangle = new Triangle();
							long pointId0, pointId1, pointId2;
							pointId0 = getEdgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i ] );
							pointId1 = getEdgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i + 1 ] );
							pointId2 = getEdgeId( cursorX, cursorY, cursorZ, MarchingCubesTables.triTable[ tableIndex ][ i + 2 ] );
							triangle.point[ 0 ] = pointId0;
							triangle.point[ 1 ] = pointId1;
							triangle.point[ 2 ] = pointId2;
							triangleVector.add( triangle );
						}
					}
				}
			}
		}

		renameVerticesAndTriangles();
		calculateNormals();
		normalizeVerticesAndNormals();
		hasValidSurface = true;

		return mesh;
	}
}
