package bdv.img.imaris;

import java.io.File;
import java.util.List;

import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import mpicbg.spim.data.sequence.TimePoint;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.basictypeaccess.volatiles.VolatileAccess;
import net.imglib2.img.cell.CellImg;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Fraction;
import bdv.AbstractViewerSetupImgLoader;
import bdv.ViewerImgLoader;
import bdv.img.cache.Cache;
import bdv.img.cache.CacheArrayLoader;
import bdv.img.cache.CacheHints;
import bdv.img.cache.CachedCellImg;
import bdv.img.cache.LoadingStrategy;
import bdv.img.cache.VolatileGlobalCellCache;
import bdv.img.cache.VolatileImgCells;
import bdv.img.cache.VolatileImgCells.CellCache;
import bdv.img.hdf5.MipmapInfo;
import bdv.img.hdf5.ViewLevelId;
import bdv.img.imaris.DataTypes.DataType;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;

public class ImarisImageLoader< T extends NativeType< T >, V extends Volatile< T > & NativeType< V > , A extends VolatileAccess > implements ViewerImgLoader
{
	private final DataType< T, V, A > dataType;

	private IHDF5Access hdf5Access;

	private final MipmapInfo mipmapInfo;

	private final long[][] mipmapDimensions;

	private final File hdf5File;

	private final AbstractSequenceDescription< ?, ?, ? > sequenceDescription;

	private VolatileGlobalCellCache cache;

	private CacheArrayLoader< A > loader;

	public ImarisImageLoader(
			final DataType< T, V, A > dataType,
			final File hdf5File,
			final MipmapInfo mipmapInfo,
			final long[][] mipmapDimensions,
			final AbstractSequenceDescription< ?, ?, ? > sequenceDescription )
	{
		this.dataType = dataType;
		this.hdf5File = hdf5File;
		this.mipmapInfo = mipmapInfo;
		this.mipmapDimensions = mipmapDimensions;
		this.sequenceDescription = sequenceDescription;
	}

	private boolean isOpen = false;

	private void open()
	{
		if ( !isOpen )
		{
			synchronized ( this )
			{
				if ( isOpen )
					return;
				isOpen = true;

				final IHDF5Reader hdf5Reader = HDF5Factory.openForReading( hdf5File );

				final List< TimePoint > timepoints = sequenceDescription.getTimePoints().getTimePointsOrdered();
				final List< ? extends BasicViewSetup > setups = sequenceDescription.getViewSetupsOrdered();

				final int maxNumTimepoints = timepoints.get( timepoints.size() - 1 ).getId() + 1;
				final int maxNumSetups = setups.get( setups.size() - 1 ).getId() + 1;
				final int maxNumLevels = mipmapInfo.getNumLevels();

				try
				{
					hdf5Access = new HDF5AccessHack( hdf5Reader );
				}
				catch ( final Exception e )
				{
					throw new RuntimeException( e );
				}
				loader = dataType.createArrayLoader( hdf5Access );
				cache = new VolatileGlobalCellCache( maxNumTimepoints, maxNumSetups, maxNumLevels, 1 );
			}
		}
	}

	/**
	 * (Almost) create a {@link CellImg} backed by the cache. The created image
	 * needs a {@link NativeImg#setLinkedType(net.imglib2.type.Type) linked
	 * type} before it can be used. The type should be either
	 * {@link UnsignedShortType} and {@link VolatileUnsignedShortType}.
	 */
	protected < T extends NativeType< T > > CachedCellImg< T, A > prepareCachedImage( final ViewLevelId id, final LoadingStrategy loadingStrategy )
	{
		open();
		final int timepointId = id.getTimePointId();
		final int setupId = id.getViewSetupId();
		final int level = id.getLevel();
		final long[] dimensions = mipmapDimensions[ level ];
		final int[] cellDimensions = mipmapInfo.getSubdivisions()[ level ];

		final int priority = mipmapInfo.getMaxLevel() - level;
		final CacheHints cacheHints = new CacheHints( loadingStrategy, priority, false );
		final CellCache< A > c = cache.new VolatileCellCache< A >( timepointId, setupId, level, cacheHints, loader );
		final VolatileImgCells< A > cells = new VolatileImgCells< A >( c, new Fraction(), dimensions, cellDimensions );
		final CachedCellImg< T, A > img = new CachedCellImg< T, A >( cells );
		return img;
	}

	@Override
	public Cache getCache()
	{
		open();
		return cache;
	}

	@Override
	public SetupImgLoader getSetupImgLoader( final int setupId )
	{
		return null;
	}

	public class SetupImgLoader extends AbstractViewerSetupImgLoader< T, V >
	{
		private final int setupId;

		protected SetupImgLoader( final int setupId )
		{
			super( dataType.getType(), dataType.getVolatileType() );
			this.setupId = setupId;
		}

		@Override
		public RandomAccessibleInterval< T > getImage( final int timepointId, final int level )
		{
			final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
			final CachedCellImg< T, A > img = prepareCachedImage( id, LoadingStrategy.BLOCKING );
			final T linkedType = dataType.createLinkedType( img );
			img.setLinkedType( linkedType );
			return img;
		}

		@Override
		public RandomAccessibleInterval< V > getVolatileImage( final int timepointId, final int level )
		{
			final ViewLevelId id = new ViewLevelId( timepointId, setupId, level );
			final CachedCellImg< V, A > img = prepareCachedImage( id, LoadingStrategy.BUDGETED );
			final V linkedType = dataType.createLinkedVolatileType( img );
			img.setLinkedType( linkedType );
			return img;
		}

		@Override
		public double[][] getMipmapResolutions()
		{
			return mipmapInfo.getResolutions();
		}

		@Override
		public AffineTransform3D[] getMipmapTransforms()
		{
			return mipmapInfo.getTransforms();
		}

		@Override
		public int numMipmapLevels()
		{
			return mipmapInfo.getNumLevels();
		}
	}
}
