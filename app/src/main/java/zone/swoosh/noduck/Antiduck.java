package zone.swoosh.noduck;

import java.lang.reflect.Method;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Antiduck implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        Method setOnAudioFocusChangeListener1 = XposedHelpers.findMethodExactIfExists(AudioFocusRequest.Builder.class, "setOnAudioFocusChangeListener", AudioManager.OnAudioFocusChangeListener.class);
        Method setOnAudioFocusChangeListener2 = XposedHelpers.findMethodExactIfExists(AudioFocusRequest.Builder.class, "setOnAudioFocusChangeListener", AudioManager.OnAudioFocusChangeListener.class, android.os.Handler.class);
        XC_MethodHook focusListenerHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                AudioManager.OnAudioFocusChangeListener focusListener = (AudioManager.OnAudioFocusChangeListener)param.args[0];
                if (!(focusListener instanceof AudioFocusChangeListenerWrapper)) {
                    param.args[0] = new AudioFocusChangeListenerWrapper(focusListener);
                }
            }
        };
        if (setOnAudioFocusChangeListener1 != null) {
            XposedBridge.hookMethod(setOnAudioFocusChangeListener1, focusListenerHook);
        }
        if (setOnAudioFocusChangeListener2 != null) {
            XposedBridge.hookMethod(setOnAudioFocusChangeListener2, focusListenerHook);
        }

        Method getFocusListener = XposedHelpers.findMethodExactIfExists(AudioFocusRequest.class, "getOnAudioFocusChangeListener");
        Method focusRequestBuild = XposedHelpers.findMethodExactIfExists(AudioFocusRequest.Builder.class, "build");
        if ((focusRequestBuild != null) && (getFocusListener != null)) {
            XposedBridge.hookMethod(focusRequestBuild, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.hasThrowable()) {
                                return;
                            }

                            AudioFocusRequest focusRequest = (AudioFocusRequest)param.getResult();
                            Object focusListener = getFocusListener.invoke(focusRequest);
                            if (focusListener == null) {
                                AudioAttributes audioAttributes = focusRequest.getAudioAttributes();
                                audioAttributes = new AudioAttributes.Builder(audioAttributes)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build();
                                focusRequest = new AudioFocusRequest.Builder(focusRequest)
                                        .setAudioAttributes(audioAttributes)
                                        .build();
                            } else {
                                focusRequest = new AudioFocusRequest.Builder(focusRequest)
                                        .setWillPauseWhenDucked(true)
                                        .build();
                            }

                            param.setResult(focusRequest);
                        }
                    }
            );
        }

    }

}
