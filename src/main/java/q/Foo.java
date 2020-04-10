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

package q;

import p.MHAccessBench.IntBox;

public class Foo {

    /** Returns an `IntBoxImpl` with the given value, x. */
    public static IntBoxImpl intBoxOf(int x) {
        return new IntBoxImpl(x);
    }

    /** Returns an `IntBox` with the given value plus one. */
    public static IntBox incrementAccess(IntBox intBox) {
        return new IntBoxImpl(intBox.getInt() + 1);
    }

    /** Returns an `IntBoxImpl` with the given value plus one. */
    // The method is accessible, but the declared parameter and return types are not
    public static IntBoxImpl incrementInAccess(IntBoxImpl intBox) {
        return new IntBoxImpl(intBox.getInt() + 1);
    }

    // --- private

    private final static class IntBoxImpl implements IntBox {
        private final int x;

        IntBoxImpl(int x) {
            this.x = x;
        }

        @Override
        public final int getInt() {
            return x;
        }
    }
}
