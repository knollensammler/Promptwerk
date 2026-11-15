package de.mika.promptwerk;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.view.Surface;
import android.view.TextureView;

/** Local, silent loop. Resources are released when the page or activity is left. */
public final class FoxBackdrop extends TextureView implements TextureView.SurfaceTextureListener {
    private MediaPlayer player;
    private boolean paused=false, active=true, ready=false, disposed=false;
    public FoxBackdrop(Context context){super(context);setOpaque(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setSurfaceTextureListener(this);}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){
        if(disposed)return;
        try{
            player=new MediaPlayer();Surface surface=new Surface(texture);
            try{player.setSurface(surface);}finally{surface.release();}
            try(AssetFileDescriptor file=getResources().openRawResourceFd(R.raw.fuchs)){
                player.setDataSource(file.getFileDescriptor(),file.getStartOffset(),file.getLength());
            }
            player.setVolume(0,0);player.setLooping(true);
            player.setOnPreparedListener(p->{ready=true;fit(getWidth(),getHeight());if(active&&!paused)p.start();});
            player.setOnErrorListener((p,what,extra)->{disposePlayer();return true;});
            fit(width,height);player.prepareAsync();
        }catch(Exception unavailable){disposePlayer();}
    }
    private void fit(int width,int height){
        if(width<=0||height<=0)return;
        // Letterbox instead of cropping: the complete fox and Vereinszeichen stay visible.
        float aspect=16f/9f, viewAspect=(float)width/height;
        Matrix matrix=new Matrix();matrix.setScale(viewAspect>aspect?aspect/viewAspect:1,viewAspect<aspect?viewAspect/aspect:1,width/2f,height/2f);setTransform(matrix);
    }
    public boolean toggle(){paused=!paused;update();return paused;}
    public void suspend(){active=false;update();}
    public void resume(){active=true;update();}
    private void update(){if(player!=null&&ready){if(active&&!paused)player.start();else player.pause();}}
    private void disposePlayer(){ready=false;if(player!=null){player.release();player=null;}}
    public void release(){disposed=true;disposePlayer();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture texture){disposePlayer();return true;}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture texture,int width,int height){fit(width,height);}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture texture){}
}
