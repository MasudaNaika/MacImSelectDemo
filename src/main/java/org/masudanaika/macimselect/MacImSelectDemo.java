package org.masudanaika.macimselect;

import com.sun.jna.Callback;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * MacImSelect.
 * 
 * @author masuda, Masudana Ika
 */
public class MacImSelectDemo {

    private static final String romanId = "com.apple.keylayout.ABC";
    private static final String kanjiId = "com.apple.inputmethod.Kotoeri.RomajiTyping.Japanese";

    public static void main(String... args) {
        MacImSelectDemo test = new MacImSelectDemo();
        test.start();
    }

    private void start() {

        JFrame frame = new JFrame("MacImSelect Demo, (C)Masudana Ika");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        frame.setContentPane(panel);

        JTextArea ta = new JTextArea(5, 38);
        panel.add(ta, BorderLayout.CENTER);
        JPanel btnPanel = new JPanel();
        btnPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton btn1 = new JButton("英数字");
        btnPanel.add(btn1);
        btn1.addActionListener(ae -> {
            Thread.ofVirtual().start(() -> {
                selectInputSourceJnaCocoa(romanId);
                ta.setText("Roman mode.");
            });
        });
        JButton btn2 = new JButton("漢字");
        btnPanel.add(btn2);
        btn2.addActionListener(ae -> {
            Thread.ofVirtual().start(() -> {
                selectInputSourceJnaCocoa(kanjiId);
                ta.setText("Kanji mode.");
            });
        });
        JButton btn3 = new JButton("現在");
        btnPanel.add(btn3);
        btn3.addActionListener(ae -> {
            Thread.ofVirtual().start(() -> {
                String sourceId = getSelectedInputSourceIdJnaCocoa();
                ta.setText(sourceId);
            });
        });
        JButton btn4 = new JButton("リスト");
        btnPanel.add(btn4);
        btn4.addActionListener(ae -> {
            Thread.ofVirtual().start(() -> {
                String list = getInputSourceListJnaCocoa();
                ta.setText(list.replace(",", "\n"));
            });
        });
        panel.add(btnPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setLocationRelativeTo(null);

        SwingUtilities.invokeLater(() -> {
            frame.setVisible(true);
        });
    }

    private interface DispatchTask extends Callback {

        void invoke(Pointer context);
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
