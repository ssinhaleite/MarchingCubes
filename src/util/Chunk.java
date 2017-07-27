package util;

import bdv.labels.labelset.LabelMultisetType;
import graphics.scenery.Mesh;
import net.imglib2.RandomAccessibleInterval;

/**
 * Chunk is a part of the volume. Each chunk knows its relation with the volume,
 * its offset and keeps its mesh in different resolutions.
 * 
 * @author vleite
 *
 */
public class Chunk
{
	/**
	 * Volume of the chunk.
	 */
	private RandomAccessibleInterval< LabelMultisetType > volume;

	/**
	 * offset of the chunk, to positioning it in the world.
	 */
	private int[] offset;

	/**
	 * Scenery Mesh. Each position corresponds to one resolution. 1 - the most
	 * detailed, 8 - the least detailed
	 */
	private Mesh[] mesh;

	/**
	 * Constructor, initialize variables with dummy values.
	 */
	public Chunk()
	{
		volume = null;
		offset = null;
		mesh = new Mesh[ 8 ];
	}

	/**
	 * Return the volume of the chunk
	 * 
	 * @return RAI correspondent to the chunk
	 */
	public RandomAccessibleInterval< LabelMultisetType > getVolume()
	{
		return volume;
	}

	/**
	 * Define the volume for the chunk
	 * 
	 * @param volume
	 *            RAI
	 */
	public void setVolume( RandomAccessibleInterval< LabelMultisetType > volume )
	{
		this.volume = volume;
	}

	/**
	 * Return the offset of the chunk
	 * 
	 * @return int[] with x, y and z offset to remap the chunk in the world
	 */
	public int[] getOffset()
	{
		return offset;
	}

	/**
	 * Define the offset of the chunk
	 * 
	 * @param offset
	 *            int[] with x, y and z offset to remap the chunk in the world.
	 */
	public void setOffset( int[] offset )
	{
		this.offset = offset;
	}

	/**
	 * Define the {@link#mesh} generated for the chunk
	 * 
	 * @param mesh
	 *            list of vertices
	 * @param resolution
	 *            from 1 to 8, represents the index of the vector but also the
	 *            resolution of the mesh.
	 */
	public void setMesh( Mesh mesh, int resolution )
	{
		this.mesh[ resolution ] = mesh;
	}

	/**
	 * Return the {@link#mesh} in a specific resolution.
	 * 
	 * @param resolution
	 *            from 1 to 8, represents the index of the vector
	 * @return the {@link#mesh} at resolution position, if no mesh was found
	 *         return null.
	 */
	public Mesh getMesh( int resolution )
	{
		if ( mesh[ resolution ] != null )
			return mesh[ resolution ];

		return null;
	}

}
