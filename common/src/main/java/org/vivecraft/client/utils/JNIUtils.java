package org.vivecraft.client.utils;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.FFIType;
import org.lwjgl.system.libffi.LibFFI;
import org.vivecraft.client_vr.settings.VRSettings;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JNIUtils {

    private static final Map<String, FFIInfo> FFIs = new HashMap<>();
    private static int counter = 0;

    private static FFIInfo get(String format) {
        FFIInfo info = FFIs.get(format);
        //VRSettings.LOGGER.warn("calling: {}", Thread.currentThread().getStackTrace()[3]);
        if (info == null) {
            String[] parameters = format.split("_");
            FFICIF cif = FFICIF.calloc();
            PointerBuffer argTypes = null;
            if (!parameters[0].isEmpty()) {
                argTypes = MemoryUtil.memAllocPointer(parameters[0].length());
                for (int i = 0; i < parameters[0].length(); i++) {
                    argTypes.put(getType(parameters[0].charAt(i)));
                }
                argTypes.flip();
            }
            int ret = LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, getType(parameters[1].charAt(0)),
                argTypes);
            if (ret != LibFFI.FFI_OK) {
                throw new RuntimeException("FFI error: " + ret);
            }
            info = new FFIInfo(cif, parameters[0].toCharArray(), argTypes);
            FFIs.put(format, info);
        }
        return info;
    }

    public static float callF(String signature, long __functionAddress, Object... args) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FFIInfo info = get(signature);
/*
            PointerBuffer pointers = getPointers(stack, info.args, args);

            ByteBuffer returnValue = stack.malloc(64);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < info.args.length; i++) {
                s.append("called with: type: ").append(info.args[i])
                    .append(" value: ").append(info.args[i] == 'P' ? Long.toHexString((long) args[i]) : args[i])
                    .append("  address: ").append(Long.toHexString(pointers.get(i)))
                    .append("\n");
            }
            s.append(" return buffer at: ").append(MemoryUtil.memAddress(returnValue));
            VRSettings.LOGGER.warn(s.toString());

            LibFFI.ffi_call(info.cif, __functionAddress, returnValue, pointers);

            return returnValue.getFloat(0);*/
            return 0;
        }
    }

    public static int callI(String signature, long __functionAddress, Object... args) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FFIInfo info = get(signature);

            PointerBuffer pointers = getPointers(stack, info.args, args);

            ByteBuffer returnValue = stack.malloc(64);
            counter++;
            StringBuilder s = new StringBuilder();
            s.append(counter);
            s.append(", function: ").append(Long.toHexString(__functionAddress));
            s.append(", return buffer at: ").append(Long.toHexString(MemoryUtil.memAddress(returnValue)));
            VRSettings.LOGGER.warn(s.toString());
            LibFFI.ffi_call(info.cif, __functionAddress, returnValue, pointers);

            return returnValue.getInt(0);
        }
    }

    public static void callV(String signature, long __functionAddress, Object... args) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FFIInfo info = get(signature);
/*
            PointerBuffer pointers = getPointers(stack, info.args, args);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < info.args.length; i++) {
                s.append("called with: type: ").append(info.args[i])
                    .append(" value: ").append(info.args[i] == 'P' ? Long.toHexString((long) args[i]) : args[i])
                    .append("  address: ").append(Long.toHexString(pointers.get(i)))
                    .append("\n");
            }
            VRSettings.LOGGER.warn(s.toString());

            LibFFI.ffi_call(info.cif, __functionAddress, null, pointers);*/
        }
    }

    public static boolean callZ(String signature, long __functionAddress, Object... args) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FFIInfo info = get(signature);
/*
            PointerBuffer pointers = getPointers(stack, info.args, args);

            ByteBuffer returnValue = stack.malloc(64);
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < info.args.length; i++) {
                s.append("called with: type: ").append(info.args[i])
                    .append(" value: ").append(info.args[i] == 'P' ? Long.toHexString((long) args[i]) : args[i])
                    .append(" address: ").append(Long.toHexString(pointers.get(i)))
                    .append("\n");
            }
            s.append(" return buffer at: ").append(MemoryUtil.memAddress(returnValue));
            VRSettings.LOGGER.warn(s.toString());

            LibFFI.ffi_call(info.cif, __functionAddress, returnValue, pointers);

            return returnValue.get(0) != 0;*/
            return false;
        }
    }

    private static PointerBuffer getPointers(MemoryStack stack, char[] types, Object[] args) {
        if (types.length > 0 && args == null || (args != null && types.length != args.length)) {
            throw new IllegalArgumentException(
                "arguments needed but not enough supplied! types:" + Arrays.toString(types) + ", args:" +
                    (args == null ? "null" :
                        Arrays.toString(Arrays.stream(args).map(o -> o.getClass().getSimpleName()).toArray())
                    ));
        }
        if (args == null) {
            return null;
        }
        PointerBuffer pointers = stack.mallocPointer(args.length);
        for (int i = 0; i < args.length; i++) {
            switch (types[i]) {
                case 'I', 'U' -> pointers.put(stack.ints((int) args[i]));
                case 'J' -> pointers.put(stack.longs((long) args[i]));
                case 'F' -> pointers.put(stack.floats((int) args[i]));
                case 'S' -> pointers.put(stack.shorts((short) args[i]));
                case 'Z' -> pointers.put(stack.bytes((boolean) args[i] ? (byte) 1 : (byte) 0));
                case 'P' -> pointers.put(stack.pointers((long) args[i]).address());
            }
        }
        pointers.flip();
        return pointers;
    }

    private static FFIType getType(char c) {
        return switch (c) {
            case 'I' -> LibFFI.ffi_type_sint32;
            case 'U' -> LibFFI.ffi_type_uint32;
            case 'J' -> LibFFI.ffi_type_uint64;
            case 'P' -> LibFFI.ffi_type_pointer;
            case 'F' -> LibFFI.ffi_type_float;
            case 'Z' -> LibFFI.ffi_type_uint8;
            case 'S' -> LibFFI.ffi_type_uint16;
            default -> throw new IllegalArgumentException("unknown parameter type: " + c);
        };
    }

    private record FFIInfo(FFICIF cif, char[] args, PointerBuffer argTypes) {}
}
