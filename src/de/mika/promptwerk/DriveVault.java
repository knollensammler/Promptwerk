package de.mika.promptwerk;
import android.content.Context;
import android.security.keystore.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
/** Credentials are never included in library preferences or ZIP backups. */
final class DriveVault {
 static javax.crypto.SecretKey key()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(store.containsAlias("promptwerk-personal-drive"))return (javax.crypto.SecretKey)store.getKey("promptwerk-personal-drive",null);KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder("promptwerk-personal-drive",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return gen.generateKey();}
 static File file(Context c){return new File(c.getNoBackupFilesDir(),"personal-drive.enc");}
 static JSONObject read(Context c)throws Exception{File f=file(c);if(!f.isFile())return null;try(DataInputStream in=new DataInputStream(new FileInputStream(f))){int n=in.readUnsignedByte();if(n!=12)throw new IOException("Verbindungsdaten beschädigt");byte[] iv=new byte[n];in.readFully(iv);byte[] encrypted=DriveHttp.read(in,25*1024*1024);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));return new JSONObject(new String(cipher.doFinal(encrypted),StandardCharsets.UTF_8));}}
 static void write(Context c,JSONObject data)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());File target=file(c),temp=new File(target.getPath()+".tmp");try(DataOutputStream out=new DataOutputStream(new FileOutputStream(temp))){out.writeByte(cipher.getIV().length);out.write(cipher.getIV());out.write(cipher.doFinal(data.toString().getBytes(StandardCharsets.UTF_8)));}if(!temp.renameTo(target))throw new IOException("Verbindung nicht gespeichert");}
 static void clear(Context c)throws IOException{File f=file(c);if(f.exists()&&!f.delete())throw new IOException("Verbindung konnte nicht entfernt werden");}
}
