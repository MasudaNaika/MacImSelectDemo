package org.masudanaika.macimselect;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
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
public class MacImSelectFFM {

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
            Runnable task = () -> {
                try {
                    NSTextInputContext context = NSTextInputContext.getCurrentInputContext();
                    if (context != null) {
                        context.selectInputSource(sourceId);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                }
            };
            dispatch_sync(task);
        });
    }

    public String getSelectedInputSourceId() {

        final String[] result = {""};

        try {
            Executors.newVirtualThreadPerTaskExecutor().submit(() -> {
                Runnable task = () -> {
                    try {
                        NSTextInputContext context = NSTextInputContext.getCurrentInputContext();
                        if (context != null) {
                            result[0] = context.getSelectedInputSourceId();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                    }
                };
                dispatch_sync(task);
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
                Runnable task = () -> {
                    try {
                        NSTextInputContext context = NSTextInputContext.getCurrentInputContext();
                        if (context != null) {
                            list.addAll(context.getInputSourceList());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.err);
                    }
                };
                dispatch_sync(task);
            }).get(1, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            ex.printStackTrace(System.err);
        }

        return list;
    }

    private static final String LIB_CARBON = "/System/Library/Frameworks/Carbon.framework/Carbon";

    private static final MemorySegment objc_msgSend;
    private static final MemorySegment _dispatch_main_q;

    private static final MethodHandle mh_dispatch_sync_f;
    private static final MethodHandle mh_objc_lookUpClass;
    private static final MethodHandle mh_sel_getUid;

    private static final MethodHandle mh_mseg_objc_msgSend;
    private static final MethodHandle mh_mseg_objc_msgSendMseg1;
    private static final MethodHandle mh_mseg_objc_msgSendLong1;
    private static final MethodHandle mh_long_objc_msgSend;
    private static final MethodHandle mh_long_objc_msgSendMseg2;

    private static final MethodHandle mh_dispatchTask;

    static {
        Linker linker = Linker.nativeLinker();
        SymbolLookup carbon = SymbolLookup.libraryLookup(LIB_CARBON, Arena.ofAuto());

        MemorySegment dispatch_sync_f = carbon.findOrThrow("dispatch_sync_f");
        mh_dispatch_sync_f = linker.downcallHandle(dispatch_sync_f,
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        MemorySegment objc_lookUpClass = carbon.findOrThrow("objc_lookUpClass");
        mh_objc_lookUpClass = linker.downcallHandle(objc_lookUpClass,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        MemorySegment sel_getUid = carbon.findOrThrow("sel_getUid");
        mh_sel_getUid = linker.downcallHandle(sel_getUid,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

        objc_msgSend = carbon.findOrThrow("objc_msgSend");
        _dispatch_main_q = carbon.findOrThrow("_dispatch_main_q");

        mh_mseg_objc_msgSend = linker.downcallHandle(objc_msgSend,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        mh_mseg_objc_msgSendMseg1 = linker.downcallHandle(objc_msgSend,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS)
        );
        mh_mseg_objc_msgSendLong1 = linker.downcallHandle(objc_msgSend,
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.JAVA_LONG)
        );
        mh_long_objc_msgSendMseg2 = linker.downcallHandle(objc_msgSend,
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );
        mh_long_objc_msgSend = linker.downcallHandle(objc_msgSend,
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );

        try {
            mh_dispatchTask = MethodHandles.lookup()
                    .findVirtual(Runnable.class, "run", MethodType.methodType(void.class));
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static MemorySegment getObjcLookUpClass(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = arena.allocateFrom(name);
            return (MemorySegment) mh_objc_lookUpClass.invoke(namePtr);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
            return null;
        }
    }

    private static MemorySegment getSelector(String name) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment namePtr = arena.allocateFrom(name);
            return (MemorySegment) mh_sel_getUid.invoke(namePtr);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
            return null;
        }
    }

    private static MemorySegment mseg_objc_msgSend(MemorySegment target, MemorySegment selector)
            throws Throwable {
        return (MemorySegment) mh_mseg_objc_msgSend.invoke(target, selector);
    }

    private static MemorySegment mseg_objc_msgSend(MemorySegment target, MemorySegment selector,
            MemorySegment arg1) throws Throwable {
        return (MemorySegment) mh_mseg_objc_msgSendMseg1.invoke(target, selector, arg1);
    }

    private static MemorySegment mseg_objc_msgSend(MemorySegment target, MemorySegment selector,
            long index) throws Throwable {
        return (MemorySegment) mh_mseg_objc_msgSendLong1.invoke(target, selector, index);
    }

    private static long long_objc_msgSend(MemorySegment target, MemorySegment selector,
            MemorySegment arg1, MemorySegment arg2) throws Throwable {
        return (long) mh_long_objc_msgSendMseg2.invoke(target, selector, arg1, arg2);
    }

    private static long long_objc_msgSend(MemorySegment target, MemorySegment selector)
            throws Throwable {
        return (long) mh_long_objc_msgSend.invoke(target, selector);
    }

    private void dispatch_sync(Runnable task) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ms = Linker.nativeLinker()
                    .upcallStub(mh_dispatchTask.bindTo(task), FunctionDescriptor.ofVoid(), arena);
            mh_dispatch_sync_f.invoke(_dispatch_main_q, MemorySegment.NULL, ms);
        } catch (Throwable th) {
            th.printStackTrace(System.err);
        }
    }

    public static class NSTextInputContext {

        private static final MemorySegment CLASS_PTR = getObjcLookUpClass("NSTextInputContext");
        private static final MemorySegment sel_currentInputContext = getSelector("currentInputContext");
        private static final MemorySegment sel_keyboardInputSources = getSelector("keyboardInputSources");
        private static final MemorySegment sel_selectedKeyboardInputSource = getSelector("selectedKeyboardInputSource");
        private static final MemorySegment sel_setValueForKey = getSelector("setValue:forKey:");

        private static final MemorySegment SELECTED_KEYBOARD_INPUT_SOURCE
                = new NSString("selectedKeyboardInputSource").getMemorySegment();

        private final MemorySegment memorySegment;

        public NSTextInputContext(MemorySegment memorySegment) {
            this.memorySegment = memorySegment;
        }

        private static NSTextInputContext getCurrentInputContext() {
            try {
                MemorySegment ms = mseg_objc_msgSend(CLASS_PTR, sel_currentInputContext);
                return new NSTextInputContext(ms);
            } catch (Throwable th) {
                th.printStackTrace(System.err);
                return null;
            }
        }

        private int selectInputSource(String sourceId) {
            try {
                MemorySegment arrayPtr = mseg_objc_msgSend(memorySegment, sel_keyboardInputSources);
                if (arrayPtr != null) {
                    NSArray array = new NSArray(arrayPtr);
                    for (int i = 0, len = array.getLength(); i < len; ++i) {
                        MemorySegment ptr = array.getElementPtr(i);
                        if (ptr != null) {
                            String is = new NSString(ptr).utf8String();
                            if (is != null && is.equals(sourceId)) {
                                return (int) long_objc_msgSend(memorySegment, sel_setValueForKey,
                                        ptr, SELECTED_KEYBOARD_INPUT_SOURCE);
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                th.printStackTrace(System.err);
            }

            return 0;

        }

        private String getSelectedInputSourceId() {
            try {
                MemorySegment ms = mseg_objc_msgSend(memorySegment, sel_selectedKeyboardInputSource);
                return ms != null ? new NSString(ms).utf8String() : null;
            } catch (Throwable th) {
                th.printStackTrace(System.err);
                return null;
            }
        }

        private List<String> getInputSourceList() {
            List<String> list = new ArrayList<>();
            try {
                MemorySegment ms = mseg_objc_msgSend(memorySegment, sel_keyboardInputSources);
                if (ms != null) {
                    NSArray array = new NSArray(ms);
                    for (int i = 0, len = array.getLength(); i < len; ++i) {
                        list.add(array.getStringAt(i));
                    }
                }
            } catch (Throwable th) {
                th.printStackTrace(System.err);
            }
            return list;
        }
    }

    private static class NSArray {

        private static final MemorySegment sel_count = getSelector("count");
        private static final MemorySegment sel_objectAtIndex = getSelector("objectAtIndex:");

        private final MemorySegment memorySegment;

        private NSArray(MemorySegment memorySegment) {
            this.memorySegment = memorySegment;
        }

        private int getLength() {
            try {
                return (int) long_objc_msgSend(memorySegment, sel_count);
            } catch (Throwable th) {
                th.printStackTrace(System.err);
                return -1;
            }
        }

        private String getStringAt(int index) {
            MemorySegment ms = getElementPtr(index);
            return ms != null ? new NSString(ms).utf8String() : null;
        }

        private MemorySegment getElementPtr(int index) {
            try {
                return mseg_objc_msgSend(memorySegment, sel_objectAtIndex, index);
            } catch (Throwable th) {
                th.printStackTrace(System.err);
                return null;
            }
        }

    }

    private static class NSString {

        private static final MemorySegment CLASS_PTR = getObjcLookUpClass("NSString");
        private static final MemorySegment sel_alloc = getSelector("alloc");
        private static final MemorySegment sel_initWithUTF8String = getSelector("initWithUTF8String:");
        private static final MemorySegment sel_UTF8String = getSelector("UTF8String");

        private final MemorySegment memorySegment;

        private NSString(MemorySegment memorySegment) {
            this.memorySegment = memorySegment;
        }

        private NSString(String str) {
            this(createPointer(str));
        }

        private String utf8String() {
            try {
                MemorySegment strPtr = mseg_objc_msgSend(memorySegment, sel_UTF8String)
                        .reinterpret(Integer.MAX_VALUE);
                return strPtr.getString(0, StandardCharsets.UTF_8);
            } catch (Throwable th) {
                th.printStackTrace(System.err);
                return null;
            }
        }

        private static MemorySegment createPointer(String str) {
            try (Arena arena = Arena.ofConfined()) {
                MemorySegment ms = mseg_objc_msgSend(CLASS_PTR, sel_alloc);
                return mseg_objc_msgSend(ms, sel_initWithUTF8String, arena.allocateFrom(str));
            } catch (Throwable th) {
                th.printStackTrace(System.err);
                return null;
            }
        }

        private MemorySegment getMemorySegment() {
            return memorySegment;
        }

    }

}
