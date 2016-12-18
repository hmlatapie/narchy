/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util;


import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Represents byte[], as a slice (offset + length) into an
 * existing byte[].  The {@link #bytes} member should never be null;
 * use {@link #EMPTY_BYTES} if necessary.
 * <p>
 * <p><b>Important note:</b> Unless otherwise noted, Lucene uses this class to
 * represent terms that are encoded as <b>UTF8</b> bytes in the index. To
 * convert them to a Java {@link String} (which is UTF16), use {@link #utf8ToString}.
 * Using code like {@code new String(bytes, offset, length)} to do this
 * is <b>wrong</b>, as it does not respect the correct character set
 * and may return wrong results (depending on the platform's defaults)!
 * <p>
 * <p>{@code BytesRef} implements {@link Comparable}. The underlying byte arrays
 * are sorted lexicographically, numerically treating elements as unsigned.
 * This is identical to Unicode codepoint order.
 */
public class BytesRef implements Comparable<BytesRef>, Cloneable {

    /**
     * The contents of the BytesRef. Should never be {@code null}.
     */
    public byte[] bytes;

    /**
     * Offset of first valid byte.
     */
    public int offset;

    /**
     * Length of used bytes.
     */
    public int length;

    /**
     * Create a BytesRef with {@link #EMPTY_BYTES}
     */
    public BytesRef() {
        this(ArrayUtils.EMPTY_BYTE_ARRAY);
    }

    /**
     * This instance will directly reference bytes w/o making a copy.
     * bytes should not be null.
     */
    public BytesRef(byte[] bytes, int offset, int length) {
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
        assert isValid();
    }

    public BytesRef(@Nullable BytesRef deepCopied) {
        boolean nulll = deepCopied != null;
        this.bytes = nulll ? deepCopyOfBytes(deepCopied) : null;
        this.offset = 0;
        this.length = nulll ? deepCopied.length : 0;
    }

    /**
     * This instance will directly reference bytes w/o making a copy.
     * bytes should not be null
     */
    public BytesRef(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Create a BytesRef pointing to a new array of size <code>capacity</code>.
     * Offset and length will both be zero.
     */
    public BytesRef(int capacity) {
        this.bytes = new byte[capacity];
    }

    /**
     * Initialize the byte[] from the UTF8 bytes
     * for the provided String.
     *
     * @param text This must be well-formed
     *             unicode text, with no unpaired surrogates.
     */
    public BytesRef(CharSequence text) {
        this(new byte[UnicodeUtil.maxUTF8Length(text.length())]);
        length = UnicodeUtil.UTF16toUTF8(text, 0, text.length(), bytes);
    }

    /**
     * Expert: compares the bytes against another BytesRef,
     * returning true if the bytes are equal.
     *
     * @param other Another BytesRef, should not be null.
     * @lucene.internal
     */
    public boolean bytesEquals(BytesRef other) {
        assert other != null;
        if (length == other.length) {
            final byte[] thisBytes = this.bytes;
            final byte[] otherBytes = other.bytes;
            if (thisBytes == otherBytes && offset == other.offset)
                return true;
            int otherUpto = other.offset;
            final int end = offset + length;
            for (int upto = offset; upto < end; upto++, otherUpto++) {
                if (thisBytes[upto] != otherBytes[otherUpto]) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a shallow clone of this instance (the underlying bytes are
     * <b>not</b> copied and will be shared by both the returned object and this
     * object.
     *
     * @see #deepCopyOf
     */
    @Override
    public BytesRef clone() {
        return new BytesRef(bytes, offset, length);
    }

    /**
     * Calculates the hash code as required by TermsHash during indexing.
     * <p> This is currently implemented as MurmurHash3 (32
     * bit), using the seed from {@link
     * StringHelper#GOOD_FAST_HASH_SEED}, but is subject to
     * change from release to release.
     */
    @Override
    public int hashCode() {
        return StringHelper.murmurhash3_x86_32(this, StringHelper.GOOD_FAST_HASH_SEED);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        if (other == null)
            return false;
        //if (other instanceof BytesRef) { //HACK assume it's a ByteRef
            return this.bytesEquals((BytesRef) other);
        //}
        //return false;
    }

    /**
     * Interprets stored bytes as UTF8 bytes, returning the
     * resulting string
     */
    public String utf8ToString() {
        final char[] ref = new char[length];
        final int len = UnicodeUtil.UTF8toUTF16(bytes, offset, length, ref);
        return new String(ref, 0, len);
    }

    /**
     * Returns hex encoded bytes, eg [0x6c 0x75 0x63 0x65 0x6e 0x65]
     */
    @Override
    public String toString() {
        return toHexString();
    }

    public String toHexString() {
        int offset = this.offset;
        final int end = offset + length;
        StringBuilder sb = new StringBuilder(2 + (end - offset) * 2 ).append('[');
        byte[] bytes = this.bytes;
        for (int i = offset; i < end; i++) {
            if (i > offset) {
                sb.append(' ');
            }
            sb.append(Integer.toHexString(bytes[i] & 0xff));
        }
        return sb.append(']').toString();
    }

    /**
     * Unsigned byte order comparison
     */
    @Override
    public int compareTo(BytesRef other) {
        // TODO: Once we are on Java 9 replace this by java.util.Arrays#compareUnsigned()
        // which is implemented by a Hotspot intrinsic! Also consider building a
        // Multi-Release-JAR!
        final byte[] aBytes = this.bytes;
        int aUpto = this.offset;
        final byte[] bBytes = other.bytes;
        int bUpto = other.offset;

        final int aStop = aUpto + Math.min(this.length, other.length);
        while (aUpto < aStop) {
            int aByte = aBytes[aUpto++] & 0xff;
            int bByte = bBytes[bUpto++] & 0xff;

            int diff = aByte - bByte;
            if (diff != 0) {
                return diff;
            }
        }

        // One is a prefix of the other, or, they are equal:
        return this.length - other.length;
    }

    /**
     * Creates a new BytesRef that points to a copy of the bytes from
     * <code>other</code>
     * <p>
     * The returned BytesRef will have a length of other.length
     * and an offset of zero.
     */
    public static BytesRef deepCopyOf(BytesRef other) {
        BytesRef copy = new BytesRef();
        copy.bytes = deepCopyOfBytes(other);
        copy.offset = 0;
        copy.length = other.length;
        return copy;
    }

    public static byte[] deepCopyOfBytes(BytesRef other) {
        return Arrays.copyOfRange(other.bytes, other.offset, other.offset + other.length);
    }

    /**
     * Performs internal consistency checks.
     * Always returns true (or throws IllegalStateException)
     */
    public boolean isValid() {
        if (bytes == null) {
            throw new IllegalStateException("bytes is null");
        }
        if (length < 0) {
            throw new IllegalStateException("length is negative: " + length);
        }
        if (length > bytes.length) {
            throw new IllegalStateException("length is out of bounds: " + length + ",bytes.length=" + bytes.length);
        }
        if (offset < 0) {
            throw new IllegalStateException("offset is negative: " + offset);
        }
        if (offset > bytes.length) {
            throw new IllegalStateException("offset out of bounds: " + offset + ",bytes.length=" + bytes.length);
        }
        if (offset + length < 0) {
            throw new IllegalStateException("offset+length is negative: offset=" + offset + ",length=" + length);
        }
        if (offset + length > bytes.length) {
            throw new IllegalStateException("offset+length out of bounds: offset=" + offset + ",length=" + length + ",bytes.length=" + bytes.length);
        }
        return true;
    }
}