/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * 
 */
package org.openimaj.audio.processor;

import org.openimaj.audio.SampleChunk;

/**
 * 	Provides an audio processor that will process sample chunks of specific
 * 	sizes when the incoming stream's sample chunk size is unknown. 
 * 	<p>
 * 	This has applications for FFT (for example) where the input sample size must be
 * 	a power of 2 and the underlying audio stream reader may be returning sample
 * 	chunks of any size. 
 * 	<p>
 * 	The only assumption made by the class about the samples is that they are
 * 	whole numbers of bytes (8, 16, 24, 32 bits etc.). This is a pretty reasonable
 * 	assumption.
 * 
 *  @author David Dupplaw <dpd@ecs.soton.ac.uk>
 *	@version $Author$, $Revision$, $Date$
 *	@created 11 Jul 2011
 */
public abstract class FixedSizeSampleAudioProcessor extends AudioProcessor
{
	/** The size of each required sample chunk */
	private int requiredSampleSetSize = 512;
	
	/** Our buffer of sample chunks */
	private SampleChunk sampleBuffer = null;
	
	/**
	 * 	Create processor that will process chunks of the given size.
	 *  @param sizeRequired The size of the chunks required (in samples)
	 */
	public FixedSizeSampleAudioProcessor( int sizeRequired )
	{
		this.requiredSampleSetSize = sizeRequired;
	}
	
	/**
	 *  @inheritDoc
	 *  @see org.openimaj.audio.processor.AudioProcessor#nextSampleChunk()
	 */
	@Override
	public SampleChunk nextSampleChunk() 
	{
		// Get the samples
		SampleChunk s = null;
		if( sampleBuffer != null && sampleBuffer.getNumberOfSamples() > requiredSampleSetSize )
		{
				s = sampleBuffer;
				sampleBuffer = null;
		}
		else	s = super.nextSampleChunk();
		
		// Catch the end of the stream
		if( s == null )
		{
			if( sampleBuffer != null )
			{
				SampleChunk t = sampleBuffer;
				sampleBuffer = null;
				return t;
			}
			else	return null;
		}
		
		// If we have something in our buffer, prepend it to the new
		// sample chunk
		if( sampleBuffer != null && sampleBuffer.getNumberOfSamples() > 0 )
		{
			s = s.prepend( sampleBuffer );
			sampleBuffer = null;
		}
		
		// Now check how many samples we have
		int nSamples = s.getNumberOfSamples();

		// If we don't have enough samples, we'll keep getting chunks until
		// we have enough.
		while( nSamples < requiredSampleSetSize )
		{
			s = s.append( super.nextSampleChunk() );
			nSamples = s.getNumberOfSamples();
		}
		
		// If we have the right number of samples, 
		// then we just return the chunk
		if( nSamples == requiredSampleSetSize )
				return s;
		
		// We must now have too many samples...
		// Store the excess in the buffer
		sampleBuffer = s.getSampleSlice( 
				requiredSampleSetSize, 
				nSamples-requiredSampleSetSize );
		
		// Return a slice of the sample chunk
		return s.getSampleSlice( 0,	requiredSampleSetSize );
	}
}
