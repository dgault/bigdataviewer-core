/*
 * #%L
 * BigDataViewer core classes with minimal dependencies
 * %%
 * Copyright (C) 2012 - 2016 Tobias Pietzsch, Stephan Saalfeld, Stephan Preibisch,
 * Jean-Yves Tinevez, HongKee Moon, Johannes Schindelin, Curtis Rueden, John Bogovic
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package net.imglib2.type.volatiles;

import net.imglib2.Volatile;
import net.imglib2.img.NativeImg;
import net.imglib2.img.NativeImgFactory;
import net.imglib2.img.basictypeaccess.IntAccess;
import net.imglib2.img.basictypeaccess.volatiles.VolatileIntAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileIntArray;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.util.Fraction;

/**
 * A {@link Volatile} variant of {@link IntType}. It uses an
 * underlying {@link IntType} that maps into a
 * {@link VolatileIntAccess}.
 *
 * @author Stephan Saalfeld
 */
public class VolatileIntType extends AbstractVolatileNativeRealType< IntType, VolatileIntType >
{
	final protected NativeImg< ?, ? extends VolatileIntAccess > img;

	private static class WrappedIntType extends IntType
	{
		public WrappedIntType( final NativeImg<?, ? extends IntAccess> img )
		{
			super( img );
		}

		public WrappedIntType( final IntAccess access )
		{
			super( access );
		}

		public void setAccess( final IntAccess access )
		{
			dataAccess = access;
		}
	}

	// this is the constructor if you want it to read from an array
	public VolatileIntType( final NativeImg< ?, ? extends VolatileIntAccess > img )
	{
		super( new WrappedIntType( img ), false );
		this.img = img;
	}

	// this is the constructor if you want to specify the dataAccess
	public VolatileIntType( final VolatileIntAccess access )
	{
		super( new WrappedIntType( access ), access.isValid() );
		this.img = null;
	}

	// this is the constructor if you want it to be a variable
	public VolatileIntType( final int value )
	{
		this( new VolatileIntArray( 1, true ) );
		set( value );
	}

	// this is the constructor if you want it to be a variable
	public VolatileIntType()
	{
		this( 0 );
	}

	public void set( final int value )
	{
		get().set( value );
	}

	@Override
	public void updateContainer( final Object c )
	{
		final VolatileIntAccess a = img.update( c );
		( (WrappedIntType) t ).setAccess( a );
		setValid( a.isValid() );
	}

	@Override
	public NativeImg< VolatileIntType, ? extends VolatileIntAccess > createSuitableNativeImg( final NativeImgFactory< VolatileIntType > storageFactory, final long[] dim )
	{
		// create the container
		@SuppressWarnings( "unchecked" )
		final NativeImg< VolatileIntType, ? extends VolatileIntAccess > container = ( NativeImg< VolatileIntType, ? extends VolatileIntAccess > ) storageFactory.createIntInstance( dim, new Fraction() );

		// create a Type that is linked to the container
		final VolatileIntType linkedType = new VolatileIntType( container );

		// pass it to the NativeContainer
		container.setLinkedType( linkedType );

		return container;
	}

	@Override
	public VolatileIntType duplicateTypeOnSameNativeImg()
	{
		return new VolatileIntType( img );
	}

	@Override
	public VolatileIntType createVariable()
	{
		return new VolatileIntType();
	}

	@Override
	public VolatileIntType copy()
	{
		final VolatileIntType v = createVariable();
		v.set( this );
		return v;
	}
}
