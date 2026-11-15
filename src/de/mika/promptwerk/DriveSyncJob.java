package de.mika.promptwerk;
import android.app.job.*;
public final class DriveSyncJob extends JobService {
 Thread worker;
 @Override public boolean onStartJob(JobParameters p){worker=new Thread(()->{boolean retry=false;try{DriveSync.synchronize(this);}catch(Exception e){DriveSync.status(this,"Sync wartet: "+e.getMessage());retry=true;}jobFinished(p,retry);});worker.start();return true;}
 @Override public boolean onStopJob(JobParameters p){if(worker!=null)worker.interrupt();return true;}
}
