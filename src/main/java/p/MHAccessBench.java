/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package p;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import q.Foo;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Benchmark to compare reflective invocation times of a method, first
 * a method with accessible parameter type(s) and return type, and
 * second a method with inaccessible parameter type(s) and return type.
 * Invocation times between plain invoke and invokeExact are of most
 * interest, especially invokeExact of the method with inaccessible types
 * through a method handle adapter, asType, that ensures type conversion
 * "statically".
 *
 * Plain bytecode invocation and core reflection times are measured for
 * comparision also.
 */
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class MHAccessBench {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    public interface IntBox {
        int getInt();
    }

    private static final IntBox INT_BOX = Foo.intBoxOf(54); // instance of IntBoxImpl

    // ACCESSIBLE/INACCESSIBLE refers to the parameter type(s) and/or return type in the method declaration
    private static final Method       ACCESSIBLE_CORE_REFLECT_METHOD;
    private static final Method       INACCESSIBLE_CORE_REFLECT_METHOD;
    private static final MethodHandle ACCESSIBLE_UNREFLECT_HANDLE;
    private static final MethodHandle ACCESSIBLE_METHOD_HANDLE;
    private static final MethodHandle INACCESSIBLE_UNREFLECT_HANDLE;
    private static final MethodHandle INACCESSIBLE_UNREFLECT_EXACT_HANDLE;
    private static final MethodHandle INACCESSIBLE_METHOD_HANDLE;
    private static final MethodHandle INACCESSIBLE_METHOD_EXACT_HANDLE;

    private static final MethodType BOX_BOX_MT = MethodType.methodType(IntBox.class, IntBox.class);

    static {
        try {
            ACCESSIBLE_CORE_REFLECT_METHOD = Foo.class.getDeclaredMethod("incrementAccess", IntBox.class);
            ACCESSIBLE_UNREFLECT_HANDLE = MethodHandles.lookup().unreflect(ACCESSIBLE_CORE_REFLECT_METHOD);
            ACCESSIBLE_METHOD_HANDLE = MethodHandles.lookup().findStatic(Foo.class, "incrementAccess", BOX_BOX_MT);

            Class<?> cls = Class.forName("q.Foo$IntBoxImpl"); // inaccessible type
            INACCESSIBLE_CORE_REFLECT_METHOD = Foo.class.getDeclaredMethod("incrementInAccess", cls);
            INACCESSIBLE_UNREFLECT_HANDLE = MethodHandles.lookup().unreflect(INACCESSIBLE_CORE_REFLECT_METHOD);
            INACCESSIBLE_UNREFLECT_EXACT_HANDLE = INACCESSIBLE_UNREFLECT_HANDLE.asType(BOX_BOX_MT);
            var mt = MethodType.methodType(cls, cls);
            INACCESSIBLE_METHOD_HANDLE = MethodHandles.lookup().findStatic(Foo.class, "incrementInAccess", mt);
            INACCESSIBLE_METHOD_EXACT_HANDLE = INACCESSIBLE_METHOD_HANDLE.asType(BOX_BOX_MT);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @Benchmark
    public int plainAccess() {
        return Foo.incrementAccess(INT_BOX).getInt();
    }

    @Benchmark
    public int accessible_unreflect_invoke() throws Throwable {
        return ((IntBox)ACCESSIBLE_UNREFLECT_HANDLE.invoke(INT_BOX)).getInt();
    }

    @Benchmark
    public int accessible_unreflect_invokeExact() throws Throwable {
        return ((IntBox)ACCESSIBLE_UNREFLECT_HANDLE.invokeExact(INT_BOX)).getInt();
    }

    @Benchmark
    public int accessible_mh_invoke() throws Throwable {
        return ((IntBox)ACCESSIBLE_METHOD_HANDLE.invoke(INT_BOX)).getInt();
    }

    @Benchmark
    public int accessible_mh_invokeExact() throws Throwable {
        return ((IntBox)ACCESSIBLE_METHOD_HANDLE.invokeExact(INT_BOX)).getInt();
    }

    @Benchmark
    public int accessible_core_reflection() throws Exception {
        return ((IntBox)ACCESSIBLE_CORE_REFLECT_METHOD.invoke(null, INT_BOX)).getInt();
    }

    // --- inaccessible param/return type variants

    @Benchmark
    public int inaccessible_unreflect_invoke() throws Throwable {
        return ((IntBox)INACCESSIBLE_UNREFLECT_HANDLE.invoke(INT_BOX)).getInt();
    }

    @Benchmark
    public int inaccessible_unreflect_invokeExact() throws Throwable {
        return ((IntBox)INACCESSIBLE_UNREFLECT_EXACT_HANDLE.invokeExact(INT_BOX)).getInt();
    }

    @Benchmark
    public int inaccessible_mh_invoke() throws Throwable {
        return ((IntBox)INACCESSIBLE_METHOD_HANDLE.invoke(INT_BOX)).getInt();
    }

    @Benchmark
    public int inaccessible_mh_invokeExact() throws Throwable {
        return ((IntBox)INACCESSIBLE_METHOD_EXACT_HANDLE.invokeExact(INT_BOX)).getInt();
    }

    @Benchmark
    public int inaccessible_core_reflection() throws Exception {
        return ((IntBox)INACCESSIBLE_CORE_REFLECT_METHOD.invoke(null, INT_BOX)).getInt();
    }
}
