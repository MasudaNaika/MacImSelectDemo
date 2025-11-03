package org.masudanaika.macimselect;

import com.sun.jna.Callback;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * MacImSelect.
 * 
 * @author masuda, Masudana Ika
 */
public class MacImSelect {
    
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
        Thread.ofVirtual().start(() -> {
            selectInputSourceJnaCocoa(sourceId);
        });
    }
    
    public String getSelectedInputSourceId() {
        Callable<String> task = () -> {
            return getSelectedInputSourceIdJnaCocoa();
        };
        Future<String> f = Executors.newVirtualThreadPerTaskExecutor().submit(task);
        try {
            return f.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace(System.err);
        }
        return "";
    }
    
    public List<String> getInputSourceList() {
        Callable<List<String>> task = () -> {
            String str = getInputSourceListJnaCocoa();
            return Arrays.asList(str.split(","));
        };
        Future<List<String>> f = Executors.newVirtualThreadPerTaskExecutor().submit(task);
        try {
            return f.get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace(System.err);
        }
        return List.of();
    }

    private interface DispatchTask extends Callback {

        public void invoke(Pointer context);
    }

    private static class Carbon {

        private static final int kCFStringEncodingUTF8 = 0x08000100;

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

        private static native long objc_msgSend(Pointer theReceiver, Pointer theSelector);

        private static native long objc_msgSend(Pointer theReceiver, Pointer theSelector, long arg1);

        private static native long objc_msgSend(Pointer theReceiver, Pointer theSelector, long arg1, long arg2);

        private static native Pointer objc_lookUpClass(String name);

        private static native Pointer sel_getUid(String name);

        private static native long CFStringGetLength(Pointer theString);

        private static native boolean CFStringGetCString(Pointer theString, Pointer buffer, long bufferSize, int encoding);

        private static native Pointer CFStringCreateWithCString(Pointer alloc, String str, int encoding);

        private static String CFStringGetCString(long peer) {
            Pointer p = new Pointer(peer);
            long size = CFStringGetLength(p) + 1;
            Memory mem = new Memory(size);
            boolean ok = CFStringGetCString(p, mem, size, kCFStringEncodingUTF8);
            return ok ? mem.getString(0) : null;
        }

        private static Pointer CFStringCreateWithCString(String str) {
            return CFStringCreateWithCString(null, str, kCFStringEncodingUTF8);
        }
    }

    private static class Selector extends Pointer {

        private static final Selector currentInputContext = new Selector("currentInputContext");
        private static final Selector keyboardInputSources = new Selector("keyboardInputSources");
        private static final Selector selectedKeyboardInputSource = new Selector("selectedKeyboardInputSource");
        private static final Selector count = new Selector("count");
        private static final Selector objectAtIndex = new Selector("objectAtIndex:");
        private static final Selector setValueForKey = new Selector("setValue:forKey:");

        private static final Pointer FOR_KEY_VALUE_SELECTED_KEYBOARD_INPUT_SOURCE
                = Carbon.CFStringCreateWithCString("selectedKeyboardInputSource");

        private Selector(String name) {
            super(Pointer.nativeValue(Carbon.sel_getUid(name)));
        }
    }

    private static class NSTextInputContext extends Pointer {

        private static final Pointer CLASS_PTR = Carbon.objc_lookUpClass("NSTextInputContext");

        private NSTextInputContext(long peer) {
            super(peer);
        }

    }

    private static class NSArray extends Pointer {

        private NSArray(long peer) {
            super(peer);
        }

        private int getLength() {
            return (int) Carbon.objc_msgSend(this, Selector.count);
        }

        private String getStringAt(int index) {
            long sptr = getPeerAt(index);
            return sptr != 0 ? Carbon.CFStringGetCString(sptr) : null;
        }

        private long getPeerAt(int index) {
            return Carbon.objc_msgSend(this, Selector.objectAtIndex, index);
        }

    }

    private NSTextInputContext getCurrentInputContext() {
        long contextPtr = Carbon.objc_msgSend(NSTextInputContext.CLASS_PTR, Selector.currentInputContext);
        NSTextInputContext context = contextPtr != 0
                ? new NSTextInputContext(contextPtr)
                : null;
        return context;
    }

    private int selectInputSourceJnaCocoa(String sourceId) {

        final int[] result = {1};

        DispatchTask task = ctx -> {
            try {
                NSTextInputContext context = getCurrentInputContext();
                if (context == null) {
                    return;
                }
                long arrayPtr = Carbon.objc_msgSend(context, Selector.keyboardInputSources);
                if (arrayPtr != 0) {
                    NSArray array = new NSArray(arrayPtr);
                    for (int i = 0, len = array.getLength(); i < len; ++i) {
                        long sptr = array.getPeerAt(i);
                        if (sptr != 0) {
                            String is = Carbon.CFStringGetCString(sptr);
                            if (is != null && is.equals(sourceId)) {
                                result[0] = (int) Carbon.objc_msgSend(context, Selector.setValueForKey, sptr,
                                        Pointer.nativeValue(Selector.FOR_KEY_VALUE_SELECTED_KEYBOARD_INPUT_SOURCE));
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        };

        Carbon.dispatch_sync(task);

        return result[0];
    }

    private String getInputSourceListJnaCocoa() {

        final String[] result = {""};

        DispatchTask task = ctx -> {
            try {
                NSTextInputContext context = getCurrentInputContext();
                if (context == null) {
                    return;
                }
                long arrayPtr = Carbon.objc_msgSend(context, Selector.keyboardInputSources);
                if (arrayPtr != 0) {
                    NSArray array = new NSArray(arrayPtr);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0, len = array.getLength(); i < len; ++i) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append(array.getStringAt(i));
                    }
                    result[0] = sb.toString();
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        };

        Carbon.dispatch_sync(task);

        return result[0];
    }

    private String getSelectedInputSourceIdJnaCocoa() {

        final String[] result = {""};

        DispatchTask task = ctx -> {
            try {
                NSTextInputContext context = getCurrentInputContext();
                if (context == null) {
                    return;
                }
                long selected = Carbon.objc_msgSend(context, Selector.selectedKeyboardInputSource);
                if (selected != 0) {
                    result[0] = Carbon.CFStringGetCString(selected);
                }
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        };

        Carbon.dispatch_sync(task);

        return result[0];
    }

}
