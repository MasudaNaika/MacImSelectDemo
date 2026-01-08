package org.masudanaika.macimselect;

import com.sun.jna.Callback;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * MacImSelect.
 *
 * @author masuda, Masudana Ika
 */
public class MacImSelectJNA {

    private String romanId = "com.apple.keylayout.ABC";
    private String kanjiId = "com.apple.inputmethod.Kotoeri.RomajiTyping.Japanese";

    public void setRomanId(String romanId) {
        this.romanId = romanId;
    }

    public void setKanjiId(String kanjiId) {
        this.kanjiId = kanjiId;
    }

    public void toRomanMode() {
        selectInputSource(romanId);
    }

    public void toKanjiMode() {
        selectInputSource(kanjiId);
    }

    public void selectInputSource(String sourceId) {

        Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
            DispatchTask task = ctx -> {
                try {
                    NSTextInputContext context = NSTextInputContext.getCurrentInputContext();
                    if (context != null) {
                        context.selectInputSource(sourceId);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            };
            Carbon.dispatch_sync(task);
        });
    }

    public String getSelectedInputSourceId() {

        final String[] result = {""};

        try {
            Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                DispatchTask task = ctx -> {
                    try {
                        NSTextInputContext context = NSTextInputContext.getCurrentInputContext();
                        if (context != null) {
                            result[0] = context.getSelectedInputSourceId();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                    }
                };
                Carbon.dispatch_sync(task);
            }).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace(System.err);
        }

        return result[0];
    }

    public List<String> getInputSourceList() {

        final List<String> list = new ArrayList<>();

        try {
            Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                DispatchTask task = ctx -> {
                    try {
                        NSTextInputContext context = NSTextInputContext.getCurrentInputContext();
                        if (context != null) {
                            list.addAll(context.getInputSourceList());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                    }
                };
                Carbon.dispatch_sync(task);
            }).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace(System.err);
        }

        return list;
    }

    private interface DispatchTask extends Callback {

        public void invoke(Pointer context);
    }

    private static class Carbon {

        private static final Pointer _dispatch_main_q;

        static {
            NativeLibrary lib = NativeLibrary.getInstance("Carbon");
            Native.register(lib);
            _dispatch_main_q = lib.getGlobalVariableAddress("_dispatch_main_q");
        }

        private static void dispatch_sync(Callback task) {
            dispatch_sync_f(dispatch_get_main_queue(), null, task);
        }

        private static Pointer dispatch_get_main_queue() {
            return _dispatch_main_q;
        }

        private static native void dispatch_sync_f(Pointer queue, Pointer context, Callback task);

        private static native Pointer objc_msgSend(NSTextInputContext receiver, Selector selector);

        private static native NativeLong objc_msgSend(NSTextInputContext receiver, Selector selector, Pointer arg1, Pointer arg2);

        private static native NativeLong objc_msgSend(NSArray receiver, Selector selector);

        private static native Pointer objc_msgSend(NSArray receiver, Selector selector, NativeLong arg1);

        private static native Pointer objc_msgSend(NSString receiver, Selector selector);

        private static native Pointer objc_msgSend(NSString receiver, Selector selector, String str);

        private static native Pointer objc_lookUpClass(String name);

        private static native Pointer sel_getUid(String name);

    }

    public static class NSTextInputContext extends Pointer {

        private static final NSTextInputContext CLASS_PTR
                = new NSTextInputContext(Carbon.objc_lookUpClass("NSTextInputContext"));

        private static final Selector sel_currentInputContext = new Selector("currentInputContext");
        private static final Selector sel_keyboardInputSources = new Selector("keyboardInputSources");
        private static final Selector sel_selectedKeyboardInputSource = new Selector("selectedKeyboardInputSource");
        private static final Selector sel_setValueForKey = new Selector("setValue:forKey:");

        private static final Pointer SELECTED_KEYBOARD_INPUT_SOURCE
                = new NSString("selectedKeyboardInputSource");

        public NSTextInputContext(Pointer ptr) {
            super(Pointer.nativeValue(ptr));
        }

        private static NSTextInputContext getCurrentInputContext() {
            Pointer ptr = Carbon.objc_msgSend(CLASS_PTR, sel_currentInputContext);
            return ptr != Pointer.NULL ? new NSTextInputContext(ptr) : null;
        }

        private int selectInputSource(String sourceId) {
            Pointer arrayPtr = Carbon.objc_msgSend(this, sel_keyboardInputSources);
            if (arrayPtr != Pointer.NULL) {
                NSArray array = new NSArray(arrayPtr);
                for (int i = 0, len = array.getLength(); i < len; ++i) {
                    Pointer ptr = array.getElementPtr(i);
                    if (ptr != Pointer.NULL) {
                        String is = new NSString(ptr).utf8String();
                        if (is != null && is.equals(sourceId)) {
                            return Carbon.objc_msgSend(this, sel_setValueForKey, ptr,
                                    SELECTED_KEYBOARD_INPUT_SOURCE).intValue();
                        }
                    }
                }
            }
            return 0;
        }

        private String getSelectedInputSourceId() {
            Pointer ptr = Carbon.objc_msgSend(this, sel_selectedKeyboardInputSource);
            return ptr != Pointer.NULL ? new NSString(ptr).utf8String() : null;
        }

        private List<String> getInputSourceList() {
            Pointer arrayPtr = Carbon.objc_msgSend(this, sel_keyboardInputSources);
            List<String> list = new ArrayList<>();
            if (arrayPtr != Pointer.NULL) {
                NSArray array = new NSArray(arrayPtr);
                for (int i = 0, len = array.getLength(); i < len; ++i) {
                    list.add(array.getStringAt(i));
                }
            }
            return list;
        }
    }

    private static class NSArray extends Pointer {

        private static final Selector sel_count = new Selector("count");
        private static final Selector sel_objectAtIndex = new Selector("objectAtIndex:");

        private NSArray(Pointer ptr) {
            super(Pointer.nativeValue(ptr));
        }

        private int getLength() {
            return Carbon.objc_msgSend(this, sel_count).intValue();
        }

        private String getStringAt(int index) {
            Pointer ptr = getElementPtr(index);
            return ptr != Pointer.NULL ? new NSString(ptr).utf8String() : null;
        }

        private Pointer getElementPtr(int index) {
            return Carbon.objc_msgSend(this, sel_objectAtIndex, new NativeLong(index));
        }

    }

    private static class NSString extends Pointer {

        private static final NSString CLASS_PTR
                = new NSString(Carbon.objc_lookUpClass("NSString"));

        private static final Selector sel_alloc = new Selector("alloc");
        private static final Selector sel_initWithUTF8String = new Selector("initWithUTF8String:");
//        private static final Selector sel_stringWithUTF8String = new Selector("stringWithUTF8String:");
        private static final Selector sel_UTF8String = new Selector("UTF8String");

        private NSString(Pointer ptr) {
            super(Pointer.nativeValue(ptr));
        }

        private NSString(String str) {
            this(createPointer(str));
        }

        private String utf8String() {
            Pointer ptr = Carbon.objc_msgSend(this, sel_UTF8String);
            return ptr.getString(0, "UTF-8");
        }

        private static Pointer createPointer(String str) {
            NSString nsString = new NSString(Carbon.objc_msgSend(CLASS_PTR, sel_alloc));
            return Carbon.objc_msgSend(nsString, sel_initWithUTF8String, str);
//            return Carbon.objc_msgSend(CLASS_PTR, sel_stringWithUTF8String, str);
        }

    }

    private static class Selector extends Pointer {

        private Selector(String name) {
            super(Pointer.nativeValue(Carbon.sel_getUid(name)));
        }
    }

}
