package de.mika.promptwerk;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsAnimation;
import android.view.WindowManager;
import java.util.List;

/** Keep the weighted editor between its header and actions inside the usable window. */
final class KeyboardLayout {
    static void install(Window window, View body) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (Build.VERSION.SDK_INT < 30) return; // Legacy windows resize above the IME.
        final int left = body.getPaddingLeft(), top = body.getPaddingTop();
        final int right = body.getPaddingRight(), bottom = body.getPaddingBottom();
        window.setDecorFitsSystemWindows(false);
        body.setOnApplyWindowInsetsListener((v, insets) -> {
            apply(v, insets, left, top, right, bottom);
            return insets;
        });
        body.setWindowInsetsAnimationCallback(new WindowInsetsAnimation.Callback(
                WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE) {
            @Override public WindowInsets onProgress(WindowInsets insets,
                    List<WindowInsetsAnimation> animations) {
                apply(body, insets, left, top, right, bottom);
                return insets;
            }
        });
        body.requestApplyInsets();
    }

    private static void apply(View body, WindowInsets insets, int l, int t, int r, int b) {
        if (Build.VERSION.SDK_INT < 30) return;
        android.graphics.Insets safe = insets.getInsets(
            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
        // Both offsets start at the window edge: take their maximum, never sum them.
        body.setPadding(l + Math.max(safe.left, ime.left), t + safe.top,
            r + Math.max(safe.right, ime.right), b + Math.max(safe.bottom, ime.bottom));
    }
}
