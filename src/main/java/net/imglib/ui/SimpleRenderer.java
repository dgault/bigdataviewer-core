package net.imglib.ui;

import java.awt.image.BufferedImage;

import net.imglib.ui.component.InteractiveDisplayCanvas;
import net.imglib.ui.util.GuiUtil;
import net.imglib2.RandomAccessible;
import net.imglib2.RealRandomAccessible;
import net.imglib2.concatenate.Concatenable;
import net.imglib2.display.ARGBScreenImage;
import net.imglib2.realtransform.AffineGet;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.ARGBType;

public class SimpleRenderer< A extends AffineGet & Concatenable< AffineGet > >
{
	final protected InteractiveDisplayCanvas< ? > display;

	/**
	 * Thread that triggers repainting of the display.
	 * Requests for repainting are send there.
	 */
	final protected PainterThread painterThread;

	/**
	 * Currently active projector, used to re-paint the display. It maps the
	 * {@link #source} data to {@link #screenImage}.
	 */
	protected SimpleInterruptibleRenderer< ?, ARGBType > projector;

	/**
	 * Whether double buffering is used.
	 */
	final protected boolean doubleBuffered;

	/**
	 * Used to render the image for display. Two images per screen resolution if
	 * double buffering is enabled. (Index is double-buffer.)
	 */
	protected ARGBScreenImage[] screenImages;

	/**
	 * {@link BufferedImage}s wrapping the data in the {@link #screenImages}.
	 * (Index is double-buffer.)
	 */
	protected BufferedImage[] bufferedImages;

	/**
	 * How many threads to use for rendering.
	 */
	final protected int numRenderingThreads;

	/**
	 * @param display
	 *            The canvas that will display the images we render.
	 * @param painterThread
	 *            Thread that triggers repainting of the display. Requests for
	 *            repainting are send there.
	 * @param doubleBuffered
	 *            Whether to use double buffered rendering.
	 * @param numRenderingThreads
	 *            How many threads to use for rendering.
	 */
	public SimpleRenderer( final InteractiveDisplayCanvas< ? > display, final PainterThread painterThread, final boolean doubleBuffered, final int numRenderingThreads )
	{
		this.display = display;
		this.painterThread = painterThread;
		this.doubleBuffered = doubleBuffered;
		this.numRenderingThreads = numRenderingThreads;
		screenImages = new ARGBScreenImage[ 2 ];
		bufferedImages = new BufferedImage[ 2 ];
		projector = null;
	}

	/**
	 * Request a repaint of the display from the painter thread. The painter
	 * thread will trigger a {@link #paint()} as soon as possible (that is,
	 * immediately or after the currently running {@link #paint()} has
	 * completed).
	 */
	public synchronized void requestRepaint()
	{
//		if ( renderingMayBeCancelled && projector != null )
//			projector.cancel();
		painterThread.requestRepaint();
	}

	/**
	 * Check whether the size of the display component was changed and
	 * recreate {@link #screenImages} and {@link #screenScaleTransforms} accordingly.
	 */
	protected synchronized void checkResize()
	{
		final int componentW = display.getWidth();
		final int componentH = display.getHeight();
		if ( screenImages[ 0 ] == null || screenImages[ 0 ].dimension( 0 ) != componentW || screenImages[ 0 ].dimension( 1 ) != componentH )
		{
			for ( int b = 0; b < ( doubleBuffered ? 2 : 1 ); ++b )
			{
				screenImages[ b ] = new ARGBScreenImage( componentW, componentH );
				bufferedImages[ b ] = GuiUtil.getBufferedImage( screenImages[ b ] );
			}
		}
	}

	public boolean paint( final SimpleSource< ?, A > source, final A viewerTransform )
	{
		checkResize();

		// the corresponding ARGBScreenImage (to render to)
		final ARGBScreenImage screenImage;

		// the corresponding BufferedImage (to paint to the canvas)
		final BufferedImage bufferedImage;

		// the projector that paints to the screenImage.
		final SimpleInterruptibleRenderer< ?, ARGBType > p;

		synchronized( this )
		{
			p = createProjector( source, viewerTransform );
			screenImage = screenImages[ 0 ];
			bufferedImage = bufferedImages[ 0 ];
			projector = p;
		}

		// try rendering
		final boolean success = p.map( screenImage, numRenderingThreads );
		final long rendertime = p.getLastFrameRenderNanoTime();

		synchronized ( this )
		{
			// if rendering was not cancelled...
			if ( success )
			{
				display.setBufferedImage( bufferedImage );

				if ( doubleBuffered )
				{
					screenImages[ 0 ] = screenImages[ 1 ];
					screenImages[ 1 ] = screenImage;
					bufferedImages[ 0 ] = bufferedImages[ 1 ];
					bufferedImages[ 1 ] = bufferedImage;
				}
			}
		}

		return success;
	}

	protected < T, A extends AffineGet & Concatenable< AffineGet > > SimpleInterruptibleRenderer< T, ARGBType > createProjector( final SimpleSource< T, A > source, final A viewerTransform )
	{
		return new SimpleInterruptibleRenderer< T, ARGBType >( getTransformedSource( source, viewerTransform ), source.getConverter() );
	}

	protected static < T, A extends AffineGet & Concatenable< AffineGet > > RandomAccessible< T > getTransformedSource( final SimpleSource< T, A > source, final A viewerTransform )
	{
		final RealRandomAccessible< T > img = source.getInterpolatedSource();

		@SuppressWarnings( "unchecked" )
		final A sourceToScreen = ( A ) ( viewerTransform.copy() );
//		viewerState.getViewerTransform( sourceToScreen );
//		sourceToScreen.set( viewerTransform );
		sourceToScreen.concatenate( source.getSourceTransform() );

		return RealViews.constantAffine( img, sourceToScreen );
	}
}