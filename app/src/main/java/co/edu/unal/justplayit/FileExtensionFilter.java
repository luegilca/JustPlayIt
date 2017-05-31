package co.edu.unal.justplayit;

import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by Luis Ernesto Gil on 30/05/2017.
 */

public class FileExtensionFilter implements FilenameFilter
{
    @Override
    public boolean accept(File dir, String name)
    {
        return( name.endsWith(".mp3") || name.endsWith(".MP3") );
    }
}
