package com.duan.musicoco.aidl;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.duan.musicoco.service.PlayController;

import java.util.List;

import static com.duan.musicoco.service.PlayController.ERROR_INVALID;

/**
 * Created by DuanJiaNing on 2017/5/23.<br><br>
 * 仅有从 {@link com.duan.musicoco.aidl.IPlayControl}.aidl 继承的方法在跨进程调用时有效<br>
 * 1. 该类中的方法运行在服务端 Binder 线程池中，所有需要处理线程同步<br>
 * 2. 这些方法被客户端调用时客户端线程会被挂起，如果客户端的线程为 UI 线程，注意处理耗时操作以避免出现的 ANR<br>
 */

public class PlayControlImpl extends com.duan.musicoco.aidl.IPlayControl.Stub {

    protected RemoteCallbackList<IOnSongChangedListener> mSongChangeListeners;

    protected RemoteCallbackList<IOnPlayStatusChangedListener> mStatusChangeListeners;

    private PlayController manager;

    public PlayControlImpl(List<Song> songs) {
        this.mSongChangeListeners = new RemoteCallbackList<>();
        this.mStatusChangeListeners = new RemoteCallbackList<>();
        this.manager = PlayController.getMediaController(songs, new NotifyStatusChange());
    }

    /**
     * 播放相同列表中的指定曲目
     *
     * @param which 曲目
     * @return 播放是否成功
     */
    @Override
    public synchronized int play(Song which) {
        int re = PlayController.ERROR_UNKNOWN;
        if (manager.getCurrentSong() != which) {
            if ((re = manager.play(which)) > 0) {
                notifySongChange(which, manager.getSongsList().indexOf(which));
            }
        }
        return re;
    }

    @Override
    public int playByIndex(int index) {
        int re = PlayController.ERROR_UNKNOWN;
        if (manager.getCurrentSongIndex() != index) {
            if ((re = manager.play(index)) > 0) {
                notifySongChange(manager.getCurrentSong(), index);
            }
        }
        return re;
    }

    /**
     * 该方法并没有在 aidl 文件中声明，客户端不应调用该方法
     *
     * @param index 播放列表对应下标
     */
    public int play(int index) {
        return manager.play(index);
    }

    @Override
    public synchronized Song pre() {
        Song pre = manager.getCurrentSong();
        Song s = manager.preSong();
        if (s != pre)
            notifySongChange(s, manager.getSongsList().indexOf(s));
        return s;
    }

    @Override
    public synchronized Song next() {
        Song pre = manager.getCurrentSong();
        //随机播放时可能播放同一首
        Song next = manager.nextSong();
        if (next != pre)
            notifySongChange(next, manager.getSongsList().indexOf(next));
        return next;
    }

    @Override
    public synchronized int pause() {
        return manager.pause();
    }

    @Override
    public synchronized int resume() {
        return manager.resume();
    }

    @Override
    public Song currentSong() {
        return manager.getCurrentSong();
    }

    @Override
    public int status() {
        return manager.getPlayState();
    }

    @Override
    public synchronized Song setPlayList(List<Song> songs) {
        Song n = manager.setPlayList(songs);
        notifySongChange(n, 0);
        return n;
    }

    @Override
    public List<Song> getPlayList() {
        return manager.getSongsList();
    }

    @Override
    public synchronized void setPlayMode(int mode) {
        if (mode >= PlayController.MODE_DEFAULT && mode <= PlayController.MODE_RANDOM)
            manager.setPlayMode(mode);
    }

    @Override
    public int getProgress() {
        return manager.getProgress();
    }

    @Override
    public synchronized int seekTo(int pos) {
        return manager.seekTo(pos);
    }

    @Override
    public void registerOnSongChangedListener(IOnSongChangedListener li) {
        mSongChangeListeners.register(li);
    }

    @Override
    public void registerOnPlayStatusChangedListener(IOnPlayStatusChangedListener li) {
        mStatusChangeListeners.register(li);
    }

    @Override
    public void unregisterOnSongChangedListener(IOnSongChangedListener li) {
        mSongChangeListeners.unregister(li);
    }

    @Override
    public void unregisterOnPlayStatusChangedListener(IOnPlayStatusChangedListener li) {
        mStatusChangeListeners.unregister(li);
    }

    private void notifySongChange(Song song, int index) {
        final int N = mSongChangeListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            IOnSongChangedListener listener = mSongChangeListeners.getBroadcastItem(i);
            if (listener != null) {
                try {
                    listener.onSongChange(song, index);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mSongChangeListeners.finishBroadcast();
    }


    public void releaseMediaPlayer() {
        manager.releaseMediaPlayer();
    }

    private class NotifyStatusChange implements PlayController.NotifyStatusChanged {

        @Override
        public void notify(Song song, int index, int status) {
            final int N = mStatusChangeListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                IOnPlayStatusChangedListener listener = mStatusChangeListeners.getBroadcastItem(i);
                if (listener != null) {
                    try {
                        switch (status) {
                            case PlayController.STATUS_START:
                                listener.playStart(song, index);
                                break;
                            case PlayController.STATUS_COMPLETE:
                                listener.playStop(song, index);
                                break;
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            mStatusChangeListeners.finishBroadcast();
        }
    }
}