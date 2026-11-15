package de.mika.promptwerk;

import android.content.Context;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.KeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/** Restore all input properties changed by TextView's selectable/read-only mode. */
final class EditorMode {
    static void apply(EditText field, boolean read, KeyListener keys, int inputType) {
        int start = Math.max(0, field.getSelectionStart());
        int end = Math.max(0, field.getSelectionEnd());
        if (read) {
            field.setKeyListener(null);
            field.setTextIsSelectable(true);
        } else {
            field.setTextIsSelectable(false);
            field.setKeyListener(keys);
            // setKeyListener alone can drop multiline and capitalization flags.
            field.setRawInputType(inputType);
            field.setMovementMethod(ArrowKeyMovementMethod.getInstance());
        }
        field.setEnabled(true);
        field.setFocusable(true);
        field.setFocusableInTouchMode(true);
        field.setClickable(true);
        field.setLongClickable(true);
        field.setCursorVisible(!read);
        field.setShowSoftInputOnFocus(!read);
        field.setSelection(Math.min(start, field.length()), Math.min(end, field.length()));
    }

    static void showKeyboard(EditText field, Window window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        field.requestFocus();
        field.post(() -> {
            if (!field.isAttachedToWindow() || field.getKeyListener() == null ||
                !field.getShowSoftInputOnFocus()) return;
            field.requestFocus();
            ((InputMethodManager)field.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                .showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    static void hideKeyboard(EditText field, Window window) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE |
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        ((InputMethodManager)field.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(field.getWindowToken(), 0);
    }
}
