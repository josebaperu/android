package com.webview.youtube.service;

import android.os.Looper;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.SimpleBasePlayer;
import androidx.media3.common.util.UnstableApi;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;

/**
 * Media3 player that mirrors the WebView's playback instead of decoding audio itself.
 * Play/pause/next are forwarded to the page; {@link #setReportedPlaying(boolean)} is the
 * page reporting what the video is actually doing.
 */
@UnstableApi
final class WebViewPlayer extends SimpleBasePlayer {

    interface Callback {
        void onPlayWhenReadyChanged(boolean playWhenReady);

        void onSkipToNext();
    }

    private static final Player.Commands COMMANDS = new Player.Commands.Builder()
            .addAll(
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_METADATA)
            .build();

    private static final MediaItemData MEDIA_ITEM = new MediaItemData.Builder("youtube")
            .setMediaItem(new MediaItem.Builder()
                    .setMediaId("youtube")
                    .setMediaMetadata(new MediaMetadata.Builder()
                            .setTitle("YouTube")
                            .setArtist("playback")
                            .setAlbumTitle("YouTube")
                            .build())
                    .build())
            .setDurationUs(C.TIME_UNSET)
            .build();

    private final Callback callback;
    private boolean playWhenReady;

    WebViewPlayer(Looper looper, Callback callback) {
        super(looper);
        this.callback = callback;
    }

    /** Sync the session with what the page's video is doing, without sending a command back. */
    void setReportedPlaying(boolean playing) {
        if (playWhenReady == playing) {
            return;
        }
        playWhenReady = playing;
        invalidateState();
    }

    @Override
    protected State getState() {
        return new State.Builder()
                .setAvailableCommands(COMMANDS)
                .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
                .setPlaybackState(Player.STATE_READY)
                .setPlaylist(Collections.singletonList(MEDIA_ITEM))
                .setCurrentMediaItemIndex(0)
                .build();
    }

    @Override
    protected ListenableFuture<?> handleSetPlayWhenReady(boolean playWhenReady) {
        this.playWhenReady = playWhenReady;
        callback.onPlayWhenReadyChanged(playWhenReady);
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleSeek(int mediaItemIndex, long positionMs, int seekCommand) {
        if (seekCommand == Player.COMMAND_SEEK_TO_NEXT
                || seekCommand == Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM) {
            callback.onSkipToNext();
        }
        return Futures.immediateVoidFuture();
    }

    @Override
    protected ListenableFuture<?> handleRelease() {
        return Futures.immediateVoidFuture();
    }
}
