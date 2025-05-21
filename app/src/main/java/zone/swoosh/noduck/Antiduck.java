package zone.swoosh.noduck;


import java.lang.Class;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.WeakHashMap;

import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import android.content.Context;
import android.widget.Toast;
import android.app.AndroidAppHelper;

public class Antiduck implements IXposedHookLoadPackage {

    private final WeakHashMap<AudioManager.OnAudioFocusChangeListener, Object> focusListeners = new WeakHashMap<>();
    private final HashSet<Class<?>> focusListenerClasses = new HashSet<>();

    private void hookFocusListener(AudioManager.OnAudioFocusChangeListener focusListener) throws Throwable {
        if (focusListeners.containsKey(focusListener)) {
            return;
        }
        focusListeners.put(focusListener, this);

        Class<?> focusListenerClass = focusListener.getClass();
        if (focusListenerClasses.contains(focusListenerClass)) {
            return;
        }

        Method onAudioFocusChange = focusListenerClass.getMethod("onAudioFocusChange", int.class);

        Class<?> focusListenerDeclaringClass = onAudioFocusChange.getDeclaringClass();
        if (focusListenerClasses.contains(focusListenerDeclaringClass)) {
            return;
        }

        focusListenerClasses.add(focusListenerClass);
        focusListenerClasses.add(focusListenerDeclaringClass);

        Context context = AndroidAppHelper.currentApplication();
        Toast.makeText(context, "Hooking " + focusListenerDeclaringClass.getName(), Toast.LENGTH_SHORT).show();

        XposedBridge.hookMethod(onAudioFocusChange, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (((int)param.args[0] == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) && focusListeners.containsKey((AudioManager.OnAudioFocusChangeListener)param.thisObject)) {
                        param.args[0] = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT;
                    }
                }
            }
        );
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        Method requestAudioFocus8 = XposedHelpers.findMethodExactIfExists(AudioManager.class, "requestAudioFocus", AudioManager.OnAudioFocusChangeListener.class, int.class, int.class);
        if (requestAudioFocus8 != null) {
            XposedBridge.hookMethod(requestAudioFocus8, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        AudioManager.OnAudioFocusChangeListener focusListener = (AudioManager.OnAudioFocusChangeListener)param.args[0];
                        int streamType = (int)param.args[1];
                        int durationHint = (int)param.args[2];

                        if ((streamType != AudioManager.STREAM_MUSIC) && (durationHint != AudioManager.AUDIOFOCUS_GAIN)) {
                            return;
                        }

                        hookFocusListener(focusListener);
                    }
                }
            );
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
                        AudioAttributes audioAttributes = focusRequest.getAudioAttributes();
                        int contentType = audioAttributes.getContentType();
                        int usage = audioAttributes.getUsage();
                        AudioManager.OnAudioFocusChangeListener focusListener = (AudioManager.OnAudioFocusChangeListener)getFocusListener.invoke(focusRequest);

                        if ((contentType != AudioAttributes.CONTENT_TYPE_SPEECH) && (contentType != AudioAttributes.CONTENT_TYPE_MOVIE) && (contentType != AudioAttributes.CONTENT_TYPE_MUSIC)) {
                            return;
                        }

                        if ((usage != AudioAttributes.USAGE_GAME) && (usage != AudioAttributes.USAGE_MEDIA)) {
                            return;
                        }

                        if (focusListener == null) {
                            if (contentType != AudioAttributes.CONTENT_TYPE_SPEECH) {
                                Context context = AndroidAppHelper.currentApplication();
                                Toast.makeText(context, "Hooking Content Type", Toast.LENGTH_SHORT).show();
                                audioAttributes = new AudioAttributes.Builder(audioAttributes)
                                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                        .build();
                                focusRequest = new AudioFocusRequest.Builder(focusRequest)
                                        .setAudioAttributes(audioAttributes)
                                        .build();
                            }
                        } else {
                            hookFocusListener(focusListener);
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
