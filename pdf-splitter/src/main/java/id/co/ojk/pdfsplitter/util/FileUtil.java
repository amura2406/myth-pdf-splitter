package id.co.ojk.pdfsplitter.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

public class FileUtil{

    private static final int BUFFER_SIZE = 16 * 1024;
    
    public static void fastChannelCopy(final InputStream in, final OutputStream out) throws IOException{
      try{
        ReadableByteChannel src  = Channels.newChannel(in);
        WritableByteChannel dest = Channels.newChannel(out);

        final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
        while(src.read(buffer) >= 0){
          buffer.flip();
          dest.write(buffer);
          buffer.compact();
        }
        buffer.flip();
        while(buffer.hasRemaining()){
          dest.write(buffer); 
        }
      }finally{
        if(in != null) try{ in.close(); }catch(IOException ioe){}
        if(out != null) try{ out.close(); }catch(IOException ioe){}
      }
    }
    
    public static void listFilesForFolder(final File folder, List<String> output) {
    	
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
            	listFilesForFolder(fileEntry, output);
            } else {
            	output.add(fileEntry.getPath());
            }
        }
    }
}
