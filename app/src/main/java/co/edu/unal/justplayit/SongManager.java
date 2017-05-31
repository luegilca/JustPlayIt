package co.edu.unal.justplayit;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;
import java.io.FileFilter;

/**
 * Created by Luis Ernesto Gil on 30/05/2017.
 */

public class SongManager
{
    private final String MEDIA_PATH = "/";
    private ArrayList<HashMap<String, String>> songList;

    public SongManager( )
    {
        this.songList = new ArrayList<HashMap<String, String>>();
    }
    public ArrayList<HashMap<String, String>> getSongList( )
    {
        FileExtensionFilter fex = new FileExtensionFilter();
        File home =  new File( MEDIA_PATH );
        if( home.listFiles( fex ).length > 0 )
            for( File f : home.listFiles( fex ) )
            {
                HashMap<String, String> song = new HashMap<String, String>();
                song.put( "songTitle", f.getName( ).substring(0, ( f.getName( ).length() - 4 ) ) );
                song.put( "songPath", f.getPath( ) );
                songList.add( song );
            }
        return songList;
    }
}
