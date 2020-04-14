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
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
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
 * Benchmark to compare reflective field access times, first  a field
 * with an accessible declared type, and second a field with
 * inaccessible declared type.
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
public class VHAccessBench {

    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }

    private static final Foo foo = new Foo();
    private static final Foo fooImpl = Foo.ofImpl();  // instance of Foo$Impl

    // ACCESSIBLE/INACCESSIBLE refers to the field's declaration type
    private static final Field        ACCESSIBLE_CORE_REFLECT_FIELD;
    private static final Field        INACCESSIBLE_CORE_REFLECT_FIELD;
    private static final VarHandle    ACCESSIBLE_UNREFLECT_VHANDLE;
    private static final VarHandle    ACCESSIBLE_VHANDLE;
    private static final MethodHandle ACCESSIBLE_INVOKER_MHANDLE;
    private static final VarHandle    INACCESSIBLE_UNREFLECT_VHANDLE;
    private static final VarHandle    INACCESSIBLE_VHANDLE;
    private static final MethodHandle INACCESSIBLE_INVOKER_MHANDLE;

    static {
        try {
            ACCESSIBLE_CORE_REFLECT_FIELD = Foo.class.getDeclaredField("INT_BOX");
            ACCESSIBLE_UNREFLECT_VHANDLE = MethodHandles.lookup().unreflectVarHandle(ACCESSIBLE_CORE_REFLECT_FIELD);
            ACCESSIBLE_VHANDLE = MethodHandles.lookup().findVarHandle(Foo.class, "INT_BOX", IntBox.class);
            var mh = MethodHandles.varHandleExactInvoker(VarHandle.AccessMode.GET, MethodType.methodType(IntBox.class, Foo.class));
            ACCESSIBLE_INVOKER_MHANDLE = mh.bindTo(ACCESSIBLE_VHANDLE);

            Class<?> fooImplCls = Class.forName("q.Foo$Impl"); // inaccessible type
            Class<?> boxImplCls = Class.forName("q.Foo$IntBoxImpl"); // inaccessible type
            INACCESSIBLE_CORE_REFLECT_FIELD = fooImplCls.getDeclaredField("INT_BOX_IMPL");
            INACCESSIBLE_CORE_REFLECT_FIELD.setAccessible(true);
            INACCESSIBLE_UNREFLECT_VHANDLE = MethodHandles.privateLookupIn(fooImplCls, MethodHandles.lookup()).unreflectVarHandle(INACCESSIBLE_CORE_REFLECT_FIELD);
            INACCESSIBLE_VHANDLE = MethodHandles.privateLookupIn(fooImplCls, MethodHandles.lookup()).findVarHandle(fooImplCls, "INT_BOX_IMPL", boxImplCls);
            mh = MethodHandles.varHandleExactInvoker(VarHandle.AccessMode.GET, MethodType.methodType(boxImplCls, fooImplCls));
            INACCESSIBLE_INVOKER_MHANDLE = mh.bindTo(INACCESSIBLE_VHANDLE).asType(MethodType.methodType(IntBox.class, Foo.class));
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }

    @Benchmark
    public int plainAccess() {
        return foo.INT_BOX.getInt();
    }

    @Benchmark
    public int accessible_unreflect_vh_get() {
        return ((IntBox)ACCESSIBLE_UNREFLECT_VHANDLE.get(foo)).getInt();
    }

    @Benchmark
    public int accessible_vh_get() {
        return ((IntBox)ACCESSIBLE_VHANDLE.get(foo)).getInt();
    }

    @Benchmark
    public int accessible_mh_invokeExact() throws Throwable {
        return ((IntBox)ACCESSIBLE_INVOKER_MHANDLE.invokeExact(foo)).getInt();
    }

    @Benchmark
    public int accessible_core_reflection() throws Exception {
        return ((IntBox)ACCESSIBLE_CORE_REFLECT_FIELD.get(foo)).getInt();
    }

    // --- inaccessible param/return type variants

    @Benchmark
    public int inaccessible_unreflect_vh_get() {
        return ((IntBox)INACCESSIBLE_UNREFLECT_VHANDLE.get(fooImpl)).getInt();
    }

    @Benchmark
    public int inaccessible_vh_get() {
        return ((IntBox)INACCESSIBLE_VHANDLE.get(fooImpl)).getInt();
    }

    @Benchmark
    public int inaccessible_mh_invokeExact() throws Throwable {
        return ((IntBox)INACCESSIBLE_INVOKER_MHANDLE.invokeExact(fooImpl)).getInt();
    }

    @Benchmark
    public int inaccessible_core_reflection() throws Exception {
        return ((IntBox)INACCESSIBLE_CORE_REFLECT_FIELD.get(fooImpl)).getInt();
    }
}
