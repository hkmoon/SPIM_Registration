package task;

import spim.fiji.datasetmanager.LightSheetZ1;
import spim.fiji.datasetmanager.LightSheetZ1MetaData;
import spim.fiji.datasetmanager.MicroManager;
import spim.fiji.datasetmanager.StackListImageJ;
import spim.fiji.datasetmanager.StackListLOCI;

import java.io.File;

/**
 * Define XML Task class provides headless process with full parametrized way
 */
public class DefineXmlTask extends BaseTask
{
	final String inputFile;

	public DefineXmlTask( String inputFile, String xmlFileName )
	{
		super( xmlFileName );
		this.inputFile = inputFile;
	}

	public void importLightSheetZ1( double calX, double calY, double calZ, String calUnit, int rotationAxis )
	{
		LightSheetZ1 z1 = new LightSheetZ1();
		LightSheetZ1.Parameters params = new LightSheetZ1.Parameters();

		params.setXmlFilename( xmlFileName );
		params.setFirstFile( inputFile );

		final LightSheetZ1MetaData meta = new LightSheetZ1MetaData();

		if ( !meta.loadMetaData( new File( params.getFirstFile() ) ) )
		{
			return;
		}

		meta.setCalX( calX );
		meta.setCalY( calY );
		meta.setCalZ( calZ );
		meta.setCalUnit( calUnit );

		// "X-Axis":0, "Y-Axis":1, "Z-Axis":2
		meta.setRotationAxis( rotationAxis );

		params.setMetaData( meta );

		// default process
		z1.process( params );
	}

	public void importStackListLOCI()
	{
		StackListLOCI loci = new StackListLOCI();
		StackListLOCI.Parameters params = new StackListLOCI.Parameters();

		params.setXmlFilename( xmlFileName );

		loci.process( params );
	}

	public void importStackListImageJ()
	{
		StackListImageJ ij = new StackListImageJ();
		StackListImageJ.Parameters params = new StackListImageJ.Parameters();

		params.setXmlFilename( xmlFileName );

		ij.process( params );
	}

	public void importMicroManager()
	{
		MicroManager mm = new MicroManager();
		MicroManager.Parameters params = new MicroManager.Parameters();

		params.setXmlFilename( xmlFileName );

		mm.process( params );
	}

	public static void main( String[] argv )
	{
		//		String xmlFileName = "/Users/moon/temp/moon/test.xml";
		//		File cziFile = new File("/Users/moon/temp/moon/2015-02-21_LZ1_Stock68_3.czi");

		DefineXmlTask task = new DefineXmlTask( argv[ 0 ], argv[ 1 ] );
		task.importLightSheetZ1( 0.28590, 0.28590, 1.50000, "um", 0 );
	}
}
