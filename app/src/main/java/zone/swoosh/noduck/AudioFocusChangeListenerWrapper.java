package zone.swoosh.noduck;

import android.media.AudioManager;

public class AudioFocusChangeListenerWrapper implements AudioManager.OnAudioFocusChangeListener {

    private final AudioManager.OnAudioFocusChangeListener focusListener;

    public AudioFocusChangeListenerWrapper(AudioManager.OnAudioFocusChangeListener listener) {
        focusListener = listener;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            focusChange = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
        }
        focusListener.onAudioFocusChange(focusChange);
    }
}
