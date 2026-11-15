package de.mika.promptwerk;
import java.io.*;
public final class AttachmentPolicy {
 public static final long FILE_LIMIT=25L*1024*1024,TOTAL_LIMIT=100L*1024*1024;
 public static boolean validKey(String s){return s!=null&&s.matches("[a-f0-9]{64}");}
 public static boolean allowed(String mime){return mime!=null&&(mime.matches("image/(jpeg|png|webp|gif|heic|heif|bmp|avif)")||mime.equals("application/pdf")||mime.equals("text/plain"));}
 public static void check(int images,int docs,long bytes)throws IOException{if(images>10||docs>10||bytes>TOTAL_LIMIT)throw new IOException("Maximal 10 Bilder, 10 PDF/TXT-Dateien und zusammen 100 MB pro Prompt.");}
 public static long copy(InputStream in,OutputStream out,long limit)throws IOException{byte[] b=new byte[16384];long total=0;int n;while((n=in.read(b))!=-1){total+=n;if(total>limit)throw new IOException("Datei oder Sicherung überschreitet die erlaubte Größe.");out.write(b,0,n);}return total;}
}
