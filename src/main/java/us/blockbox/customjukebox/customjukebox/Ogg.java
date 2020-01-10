package us.blockbox.customjukebox.customjukebox;

import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.audio.ogg.util.OggInfoReader;

import java.io.File;
import java.io.RandomAccessFile;

class Ogg
{
    int ogg_length;

    public Ogg(final File file) throws Exception {
        this(file.getPath());
    }
    
    public Ogg(final String file) throws Exception {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file,"rwd");
        OggInfoReader oggInfoReader = new OggInfoReader();
        GenericAudioHeader read = oggInfoReader.read(randomAccessFile);
        this.ogg_length = read.getTrackLength();

    }
    
    long getSeconds() {
       return ogg_length;
    }
    
    public static void main(final String[] args) throws Exception {

    }

}
