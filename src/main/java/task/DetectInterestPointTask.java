package task;

import com.sun.jna.Native;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spim.fiji.plugin.interestpointdetection.DifferenceOf;
import spim.fiji.plugin.interestpointdetection.DifferenceOfGaussian;
import spim.fiji.plugin.interestpointdetection.DifferenceOfMean;
import spim.fiji.plugin.queryXML.HeadlessParseQueryXML;
import spim.fiji.spimdata.SpimData2;
import spim.fiji.spimdata.imgloaders.AbstractImgLoader;
import spim.fiji.spimdata.interestpoints.CorrespondingInterestPoints;
import spim.fiji.spimdata.interestpoints.InterestPoint;
import spim.fiji.spimdata.interestpoints.InterestPointList;
import spim.fiji.spimdata.interestpoints.ViewInterestPointLists;
import spim.process.cuda.CUDADevice;
import spim.process.cuda.CUDASeparableConvolution;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

/**
 * Headless module for DetectinterestPointTask
 */
public class DetectInterestPointTask extends AbstractTask
{
	// Algorithms:
	//
	// DifferenceOfMean
	// DifferenceOfGaussian

	private static final Logger LOG = LoggerFactory.getLogger( DetectInterestPointTask.class );

	/**
	 * Gets task title.
	 *
	 * @return the title
	 */
	public String getTitle() { return "Detect Interest Points Task"; }

	/**
	 * The enum Method.
	 */
	public static enum Method {
		/**
		 * The DifferenceOfMean.
		 */
		DifferenceOfMean,
		/**
		 * The DifferenceOfGaussian.
		 */
		DifferenceOfGaussian };

	/**
	 * The type Parameters.
	 */
	public static class Parameters extends AbstractTask.Parameters
	{
		private Method method;
		private boolean useCluster;

		// Common Parameters
		private double imageSigmaX, imageSigmaY, imageSigmaZ;
		private double additionalSigmaX, additionalSigmaY, additionalSigmaZ;
		private double minIntensity, maxIntensity;
		// localization:
		// 0:"None",
		// 1:"3-dimensional quadratic fit",
		// 2:"Gaussian mask localization fit"
		private int localization, downsampleXY, downsampleZ;

		// Common Advanced Parameters
		private double[] threshold;
		private boolean[] findMin;
		private boolean[] findMax;

		// DifferenceOfMean
		private int[] radius1;
		private int[] radius2;

		// DifferenceOfGaussian
		private double[] sigma;

		// computeOn:
		// 0:"CPU (Java)",
		// 1:"GPU approximate (Nvidia CUDA via JNA)",
		// 2:"GPU accurate (Nvidia CUDA via JNA)"
		private int computeOn;
		private String separableConvolutionCUDALib;

		/**
		 * Gets method.
		 *
		 * @return the method
		 */
		public Method getMethod()
		{
			return method;
		}

		/**
		 * Sets method.
		 *
		 * @param method the method
		 */
		public void setMethod( Method method )
		{
			this.method = method;
		}

		/**
		 * Is use cluster.
		 *
		 * @return the boolean
		 */
		public boolean isUseCluster()
		{
			return useCluster;
		}

		/**
		 * Sets use cluster.
		 *
		 * @param useCluster the use cluster
		 */
		public void setUseCluster( boolean useCluster )
		{
			this.useCluster = useCluster;
		}

		/**
		 * Gets image sigma x.
		 *
		 * @return the image sigma x
		 */
		public double getImageSigmaX()
		{
			return imageSigmaX;
		}

		/**
		 * Sets image sigma x.
		 *
		 * @param imageSigmaX the image sigma x
		 */
		public void setImageSigmaX( double imageSigmaX )
		{
			this.imageSigmaX = imageSigmaX;
		}

		/**
		 * Gets image sigma y.
		 *
		 * @return the image sigma y
		 */
		public double getImageSigmaY()
		{
			return imageSigmaY;
		}

		/**
		 * Sets image sigma y.
		 *
		 * @param imageSigmaY the image sigma y
		 */
		public void setImageSigmaY( double imageSigmaY )
		{
			this.imageSigmaY = imageSigmaY;
		}

		/**
		 * Gets image sigma z.
		 *
		 * @return the image sigma z
		 */
		public double getImageSigmaZ()
		{
			return imageSigmaZ;
		}

		/**
		 * Sets image sigma z.
		 *
		 * @param imageSigmaZ the image sigma z
		 */
		public void setImageSigmaZ( double imageSigmaZ )
		{
			this.imageSigmaZ = imageSigmaZ;
		}

		/**
		 * Gets additional sigma x.
		 *
		 * @return the additional sigma x
		 */
		public double getAdditionalSigmaX()
		{
			return additionalSigmaX;
		}

		/**
		 * Sets additional sigma x.
		 *
		 * @param additionalSigmaX the additional sigma x
		 */
		public void setAdditionalSigmaX( double additionalSigmaX )
		{
			this.additionalSigmaX = additionalSigmaX;
		}

		/**
		 * Gets additional sigma y.
		 *
		 * @return the additional sigma y
		 */
		public double getAdditionalSigmaY()
		{
			return additionalSigmaY;
		}

		/**
		 * Sets additional sigma y.
		 *
		 * @param additionalSigmaY the additional sigma y
		 */
		public void setAdditionalSigmaY( double additionalSigmaY )
		{
			this.additionalSigmaY = additionalSigmaY;
		}

		/**
		 * Gets additional sigma z.
		 *
		 * @return the additional sigma z
		 */
		public double getAdditionalSigmaZ()
		{
			return additionalSigmaZ;
		}

		/**
		 * Sets additional sigma z.
		 *
		 * @param additionalSigmaZ the additional sigma z
		 */
		public void setAdditionalSigmaZ( double additionalSigmaZ )
		{
			this.additionalSigmaZ = additionalSigmaZ;
		}

		/**
		 * Gets min intensity.
		 *
		 * @return the min intensity
		 */
		public double getMinIntensity()
		{
			return minIntensity;
		}

		/**
		 * Sets min intensity.
		 *
		 * @param minIntensity the min intensity
		 */
		public void setMinIntensity( double minIntensity )
		{
			this.minIntensity = minIntensity;
		}

		/**
		 * Gets max intensity.
		 *
		 * @return the max intensity
		 */
		public double getMaxIntensity()
		{
			return maxIntensity;
		}

		/**
		 * Sets max intensity.
		 *
		 * @param maxIntensity the max intensity
		 */
		public void setMaxIntensity( double maxIntensity )
		{
			this.maxIntensity = maxIntensity;
		}

		/**
		 * Gets localization.
		 *
		 * @return the localization
		 */
		public int getLocalization()
		{
			return localization;
		}

		/**
		 * Sets localization.
		 *
		 * @param localization the localization
		 */
		public void setLocalization( int localization )
		{
			this.localization = localization;
		}

		/**
		 * Gets downsample xY.
		 *
		 * @return the downsample xY
		 */
		public int getDownsampleXY()
		{
			return downsampleXY;
		}

		/**
		 * Sets downsample xY.
		 *
		 * @param downsampleXY the downsample xY
		 */
		public void setDownsampleXY( int downsampleXY )
		{
			this.downsampleXY = downsampleXY;
		}

		/**
		 * Gets downsample z.
		 *
		 * @return the downsample z
		 */
		public int getDownsampleZ()
		{
			return downsampleZ;
		}

		/**
		 * Sets downsample z.
		 *
		 * @param downsampleZ the downsample z
		 */
		public void setDownsampleZ( int downsampleZ )
		{
			this.downsampleZ = downsampleZ;
		}

		/**
		 * Get threshold.
		 *
		 * @return the double [ ]
		 */
		public double[] getThreshold()
		{
			return threshold;
		}

		/**
		 * Sets threshold.
		 *
		 * @param threshold the threshold
		 */
		public void setThreshold( double[] threshold )
		{
			this.threshold = threshold;
		}

		/**
		 * Get find min.
		 *
		 * @return the boolean [ ]
		 */
		public boolean[] getFindMin()
		{
			return findMin;
		}

		/**
		 * Sets find min.
		 *
		 * @param findMin the find min
		 */
		public void setFindMin( boolean[] findMin )
		{
			this.findMin = findMin;
		}

		/**
		 * Get find max.
		 *
		 * @return the boolean [ ]
		 */
		public boolean[] getFindMax()
		{
			return findMax;
		}

		/**
		 * Sets find max.
		 *
		 * @param findMax the find max
		 */
		public void setFindMax( boolean[] findMax )
		{
			this.findMax = findMax;
		}

		/**
		 * Get radius 1.
		 *
		 * @return the int [ ]
		 */
		public int[] getRadius1()
		{
			return radius1;
		}

		/**
		 * Sets radius 1.
		 *
		 * @param radius1 the radius 1
		 */
		public void setRadius1( int[] radius1 )
		{
			this.radius1 = radius1;
		}

		/**
		 * Get radius 2.
		 *
		 * @return the int [ ]
		 */
		public int[] getRadius2()
		{
			return radius2;
		}

		/**
		 * Sets radius 2.
		 *
		 * @param radius2 the radius 2
		 */
		public void setRadius2( int[] radius2 )
		{
			this.radius2 = radius2;
		}

		/**
		 * Get sigma.
		 *
		 * @return the double [ ]
		 */
		public double[] getSigma()
		{
			return sigma;
		}

		/**
		 * Sets sigma.
		 *
		 * @param sigma the sigma
		 */
		public void setSigma( double[] sigma )
		{
			this.sigma = sigma;
		}

		/**
		 * Gets compute on.
		 *
		 * @return the compute on
		 */
		public int getComputeOn()
		{
			return computeOn;
		}

		/**
		 * Sets compute on.
		 *
		 * @param computeOn the compute on
		 */
		public void setComputeOn( int computeOn )
		{
			this.computeOn = computeOn;
		}

		/**
		 * Gets separable convolution cUDA lib.
		 *
		 * @return the separable convolution cUDA lib
		 */
		public String getSeparableConvolutionCUDALib()
		{
			return separableConvolutionCUDALib;
		}

		/**
		 * Sets separable convolution cUDA lib.
		 *
		 * @param separableConvolutionCUDALib the separable convolution cUDA lib
		 */
		public void setSeparableConvolutionCUDALib( String separableConvolutionCUDALib )
		{
			this.separableConvolutionCUDALib = separableConvolutionCUDALib;
		}
	}

	/**
	 * Task Process with the parsed params.
	 *
	 * @param params the params
	 */
	public void process( final Parameters params )
	{
		final HeadlessParseQueryXML result = new HeadlessParseQueryXML();
		result.loadXML( params.getXmlFilename(), params.isUseCluster() );

		spimData = result.getData();

		final List< ViewId > viewIdsToProcess = SpimData2.getAllViewIdsSorted( result.getData(), result.getViewSetupsToProcess(), result.getTimePointsToProcess() );

		final String clusterExtention = result.getClusterExtension();

		if( params.getMethod() != null )
		{
			switch( params.getMethod() )
			{
				case DifferenceOfGaussian:
					processGaussian( params, viewIdsToProcess, clusterExtention );
					break;
				case DifferenceOfMean:
					processMean( params, viewIdsToProcess, clusterExtention );
					break;
			}
		}
	}

	private void processMean( final Parameters params, final List< ViewId > viewIdsToProcess, final String clusterExtention )
	{
		final DifferenceOfMean differenceOfMean = new DifferenceOfMean( spimData, viewIdsToProcess);

		final ArrayList< Channel > channels = SpimData2.getAllChannelsSorted( spimData, viewIdsToProcess );

		differenceOfMean.init( channels.size() );

		if ( DifferenceOfMean.defaultBrightness == null || DifferenceOfMean.defaultBrightness.length != channels.size() )
		{
			DifferenceOfMean.defaultBrightness = new int[ channels.size() ];
			for ( int i = 0; i < channels.size(); ++i )
				DifferenceOfMean.defaultBrightness[ i ] = 1;
		}

		differenceOfMean.setLocalization( params.getLocalization() );

		final ArrayList< Channel > channelsToProcess = differenceOfMean.getChannelsToProcess();
		final int[] brightness = new int[ channelsToProcess.size() ];

		for ( int c = 0; c < channelsToProcess.size(); ++c )
		{
			final Channel channel = channelsToProcess.get( c );
			brightness[ c ] = DifferenceOfMean.defaultBrightness[ channel.getId() ];
		}

		differenceOfMean.setDownsampleXY( params.getDownsampleXY() );
		differenceOfMean.setDownsampleZ( params.getDownsampleZ() );
		differenceOfMean.setAdditionalSigmaX( params.getAdditionalSigmaX() );
		differenceOfMean.setAdditionalSigmaY( params.getAdditionalSigmaY() );
		differenceOfMean.setAdditionalSigmaZ( params.getAdditionalSigmaZ() );
		differenceOfMean.setMinIntensity( params.getMinIntensity() );
		differenceOfMean.setMaxIntensity( params.getMaxIntensity() );

		for ( int c = 0; c < channelsToProcess.size(); ++c )
		{
			final Channel channel = channelsToProcess.get( c );

			if ( brightness[ c ] <= 3 )
			{
				if ( !differenceOfMean.setDefaultValues( channel, brightness[ c ] ) )
					return;
			}
			// TODO: setAdvanceValues and setInteractiveValues are commented out for now
			//			else if ( brightness[ c ] == 4 )
			//			{
			//				if ( !setAdvancedValues( channel ) )
			//					return;
			//			}
			//			else
			//			{
			//				if ( !setInteractiveValues( channel ) )
			//					return;
			//			}
		}

		differenceOfMean.setImageSigmaX( params.getImageSigmaX() );
		differenceOfMean.setImageSigmaY( params.getImageSigmaY() );
		differenceOfMean.setImageSigmaZ( params.getImageSigmaZ() );

		findInterestPoints( differenceOfMean, params, spimData, viewIdsToProcess, clusterExtention );
	}

	private void processGaussian( final Parameters params, final List< ViewId > viewIdsToProcess, final String clusterExtention )
	{
		final DifferenceOfGaussian differenceOfGaussian = new DifferenceOfGaussian( spimData, viewIdsToProcess);

		final ArrayList< Channel > channels = SpimData2.getAllChannelsSorted( spimData, viewIdsToProcess );

		differenceOfGaussian.init( channels.size() );

		if ( DifferenceOfGaussian.defaultBrightness == null || DifferenceOfGaussian.defaultBrightness.length != channels.size() )
		{
			DifferenceOfGaussian.defaultBrightness = new int[ channels.size() ];
			for ( int i = 0; i < channels.size(); ++i )
				DifferenceOfGaussian.defaultBrightness[ i ] = 1;
		}

		differenceOfGaussian.setLocalization( params.getLocalization() );

		final ArrayList< Channel > channelsToProcess = differenceOfGaussian.getChannelsToProcess();
		final int[] brightness = new int[ channelsToProcess.size() ];

		for ( int c = 0; c < channelsToProcess.size(); ++c )
		{
			final Channel channel = channelsToProcess.get( c );
			brightness[ c ] = DifferenceOfGaussian.defaultBrightness[ channel.getId() ];
		}

		differenceOfGaussian.setDownsampleXY( params.getDownsampleXY() );
		differenceOfGaussian.setDownsampleZ( params.getDownsampleZ() );
		differenceOfGaussian.setAdditionalSigmaX( params.getAdditionalSigmaX() );
		differenceOfGaussian.setAdditionalSigmaY( params.getAdditionalSigmaY() );
		differenceOfGaussian.setAdditionalSigmaZ( params.getAdditionalSigmaZ() );
		differenceOfGaussian.setMinIntensity( params.getMinIntensity() );
		differenceOfGaussian.setMaxIntensity( params.getMaxIntensity() );

		for ( int c = 0; c < channelsToProcess.size(); ++c )
		{
			final Channel channel = channelsToProcess.get( c );

			if ( brightness[ c ] <= 3 )
			{
				if ( !differenceOfGaussian.setDefaultValues( channel, brightness[ c ] ) )
					return;
			}
			// TODO: setAdvanceValues and setInteractiveValues are commented out for now
			//			else if ( brightness[ c ] == 4 )
			//			{
			//				if ( !setAdvancedValues( channel ) )
			//					return;
			//			}
			//			else
			//			{
			//				if ( !setInteractiveValues( channel ) )
			//					return;
			//			}
		}

		differenceOfGaussian.setImageSigmaX( params.getImageSigmaX() );
		differenceOfGaussian.setImageSigmaY( params.getImageSigmaY() );
		differenceOfGaussian.setImageSigmaZ( params.getImageSigmaZ() );

		if(params.getComputeOn() > 0)
		{
			CUDASeparableConvolution cuda = (CUDASeparableConvolution) Native.loadLibrary( params.getSeparableConvolutionCUDALib(), CUDASeparableConvolution.class );

			if ( cuda == null )
			{
				LOG.info( "Cannot load CUDA JNA library." );
				return;
			}
			else
			{
				ArrayList< CUDADevice > deviceList = new ArrayList< CUDADevice >();
				final int numDevices = cuda.getNumDevicesCUDA();
				if ( numDevices == -1 )
				{
					LOG.info( "Querying CUDA devices crashed, no devices available." );
					return;
				}
				else if ( numDevices == 0 )
				{
					LOG.info( "No CUDA devices detected." );
					return;
				}
				else
				{
					// TODO: Support multiple GPUs
					// Use the first GPU
					final byte[] name = new byte[ 256 ];
					cuda.getNameDeviceCUDA( 0, name );
					final String deviceName = new String( name );

					final long mem = cuda.getMemDeviceCUDA( 0 );
					long freeMem;

					try
					{
						freeMem = cuda.getFreeMemDeviceCUDA( 0 );
					}
					catch (UnsatisfiedLinkError e )
					{
						LOG.info( "Using an outdated version of the CUDA libs, cannot query free memory. Assuming total memory." );
						freeMem = mem;
					}

					final int majorVersion = cuda.getCUDAcomputeCapabilityMajorVersion( 0 );
					final int minorVersion = cuda.getCUDAcomputeCapabilityMinorVersion( 0 );

					if(isDebug)
					{
						LOG.info("GPU :" + deviceName);
						LOG.info("Memory :" + freeMem);
					}

					deviceList.add( new CUDADevice( 0, deviceName, mem, freeMem, majorVersion, minorVersion ) );

					differenceOfGaussian.setCuda( cuda );
					differenceOfGaussian.setDeviceList( deviceList );
				}
			}
		}

		DifferenceOfGaussian.defaultComputationChoiceIndex = params.getComputeOn();

		findInterestPoints( differenceOfGaussian, params, spimData, viewIdsToProcess, clusterExtention );
	}

	private void findInterestPoints( final DifferenceOf ipd, final Parameters params, final SpimData2 data, final List< ViewId > viewIds, final String clusterExtention )
	{
		final String label = "beads";

		// now extract all the detections
		for ( final TimePoint tp : SpimData2.getAllTimePointsSorted( data, viewIds ) )
		{
			final HashMap< ViewId, List< InterestPoint > > points = ipd.findInterestPoints( tp );
			if ( ipd instanceof DifferenceOf )
			{
				LOG.info( "Opening of files took: " + ipd.getBenchmark().openFiles/1000 + " sec." );
				LOG.info( "Detecting interest points took: " + ipd.getBenchmark().computation / 1000 + " sec." );
			}
			// save the file and the path in the XML
			final SequenceDescription seqDesc = data.getSequenceDescription();
			for ( final ViewId viewId : points.keySet() )
			{
				final ViewDescription viewDesc = seqDesc.getViewDescription( viewId.getTimePointId(), viewId.getViewSetupId() );
				final int channelId = viewDesc.getViewSetup().getChannel().getId();
				final InterestPointList list = new InterestPointList(
						data.getBasePath(),
						new File( "interestpoints", "tpId_" + viewId.getTimePointId() + "_viewSetupId_" + viewId.getViewSetupId() + "." + label ) );
				list.setParameters( ipd.getParameters( channelId ) );
				list.setInterestPoints( points.get( viewId ) );

				if ( !list.saveInterestPoints() )
				{
					LOG.info( "Error saving interest point list: " + new File( list.getBaseDir(), list.getFile().toString() + list.getInterestPointsExt() ) );
					return;
				}
				list.setCorrespondingInterestPoints( new ArrayList< CorrespondingInterestPoints >() );
				if ( !list.saveCorrespondingInterestPoints() )
					LOG.info( "Failed to clear corresponding interest point list: " + new File( list.getBaseDir(), list.getFile().toString() + list.getCorrespondencesExt() ) );

				final ViewInterestPointLists vipl = data.getViewInterestPoints().getViewInterestPointLists( viewId );
				vipl.addInterestPointList( label, list );
			}
			// update metadata if necessary
			if ( data.getSequenceDescription().getImgLoader() instanceof AbstractImgLoader )
			{
				LOG.info( "(" + new Date( System.currentTimeMillis() ) + "): Updating metadata ... " );
				try
				{
					( (AbstractImgLoader)data.getSequenceDescription().getImgLoader() ).updateXMLMetaData( data, false );
				}
				catch( Exception e )
				{
					LOG.info( "Failed to update metadata, this should not happen: " + e );
				}
			}

			if( params.isUseCluster() )
				SpimData2.saveXML( data, params.getXmlFilename(), clusterExtention );
			else
				SpimData2.saveXML( data, params.getXmlFilename(), "" );
		}
	}

	private Parameters getParams( final String[] args )
	{
		final Properties props = parseArgument( "DetectInterestPoint", getTitle(), args );

		final Parameters params = new Parameters();
		params.setXmlFilename( props.getProperty( "xml_filename" ) );
		params.setUseCluster( Boolean.parseBoolean( props.getProperty( "use_cluster", "false" ) ) );

		// downsample { 1, 2, 4, 8 }
		params.setDownsampleXY( Integer.parseInt( props.getProperty( "downsample_xy", "1" ) ) );
		params.setDownsampleZ( Integer.parseInt( props.getProperty( "downsample_z", "1" ) ) );

		// additional Smoothing
		params.setAdditionalSigmaX( Double.parseDouble( props.getProperty( "presmooth_sigma_x", "0.0" ) ) );
		params.setAdditionalSigmaY( Double.parseDouble( props.getProperty( "presmooth_sigma_y", "0.0" ) ) );
		params.setAdditionalSigmaZ( Double.parseDouble( props.getProperty( "presmooth_sigma_z", "0.0" ) ) );

		// set min max
		params.setMinIntensity( Double.parseDouble( props.getProperty( "minimal_intensity", "NaN" ) ) );
		params.setMinIntensity( Double.parseDouble( props.getProperty( "maximal_intensity", "NaN" ) ) );

		// define anisotropy
		params.setImageSigmaX( Double.parseDouble( props.getProperty( "image_sigma_x", "0.5" ) ) );
		params.setImageSigmaY( Double.parseDouble( props.getProperty( "image_sigma_y", "0.5" ) ) );
		params.setImageSigmaZ( Double.parseDouble( props.getProperty( "image_sigma_z", "0.5" ) ) );

		// sub-pixel localization
		// 0: None
		// 1: 3-dimensional quadratic fit (all detections) (default)
		// 2: Gauss fit (true correspondences)
		// 3: Gauss fit (all detections)
		params.setLocalization( Integer.parseInt( props.getProperty( "subpixel_localization", "1" ) ) );

		params.setMethod( Method.valueOf( props.getProperty( "method" ) ) );

		switch( params.getMethod() )
		{
			case DifferenceOfMean:
				params.setMethod( Method.DifferenceOfMean );

				// The below is for advanced parameters

				//			// -Dradius_1={2, 2, 2}
				//			params.setRadius1( PluginHelper.parseArrayIntegerString( props.getProperty( "radius_1" ) ) );
				//			// -Dradius_2={3, 3, 3}
				//			params.setRadius2( PluginHelper.parseArrayIntegerString( props.getProperty( "radius_2" ) ) );
				//			// -Dthreshold={0.02, 0.02, 0.02}
				//			params.setThreshold( PluginHelper.parseArrayDoubleString( props.getProperty( "threshold" ) ) );
				//			// -Dfind_minima={false, false, false}
				//			params.setFindMin( PluginHelper.parseArrayBooleanString( props.getProperty( "find_minima" ) ) );
				//			// -Dfind_maxima={true, true, true}
				//			params.setFindMax( PluginHelper.parseArrayBooleanString( props.getProperty( "find_maxima" ) ) );
				break;
			case DifferenceOfGaussian:
				params.setMethod( Method.DifferenceOfGaussian );

				// The below is for advanced parameters

				params.setComputeOn( Integer.parseInt( props.getProperty( "compute_on", "0" ) ) );
				params.setSeparableConvolutionCUDALib( props.getProperty( "separable_convolution_cuda_lib" ) );

				//			// -Dsigma={1.8, 1.8, 1.8}
				//			params.setSigma( PluginHelper.parseArrayDoubleString( props.getProperty( "sigma" ) ) );
				//			// -Dthreshold={0.02, 0.02, 0.02}
				//			params.setThreshold( PluginHelper.parseArrayDoubleString( props.getProperty( "threshold" ) ) );
				//			// -Dfind_minima={false, false, false}
				//			params.setFindMin( PluginHelper.parseArrayBooleanString( props.getProperty( "find_minima" ) ) );
				//			// -Dfind_maxima={true, true, true}
				//			params.setFindMax( PluginHelper.parseArrayBooleanString( props.getProperty( "find_maxima" ) ) );
				break;
		}

		return params;
	}

	@Override public void process( final String[] args )
	{
		process( getParams( args ) );
	}

	/**
	 * The entry point of application.
	 *
	 * @param argv the input arguments
	 */
	public static void main( final String[] argv )
	{
		// Test mvn commamnd
		//
		// module load cuda/6.5.14
		// export MAVEN_OPTS="-Xms4g -Xmx16g -Djava.awt.headless=true"
		// mvn exec:java -Dexec.mainClass="task.DetectInterestPointTask" -Dexec.args="-Dxml_filename=/projects/pilot_spim/moon/test.xml -Dmethod=DifferenceOfGaussian -Dcompute_on=1 -Dseparable_convolution_cuda_lib=lib/libSeparableConvolutionCUDALib.so"
		DetectInterestPointTask task = new DetectInterestPointTask();
		task.process( argv );
		System.exit( 0 );
	}
}
