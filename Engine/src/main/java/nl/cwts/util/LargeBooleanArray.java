/*
 * Copyright (c) 2018 Centre for Science and Technology Studies (CWTS), Leiden University
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package nl.cwts.util;

import java.util.Arrays;

/**
 * A large boolean array implemented as an array of boolean arrays to circumvent
 * the Java array size limit. Implements a dynamic array with push/pop and
 * supports filling ranges with a constant boolean.
 *
 * @author Vincent Traag
 * @author Nees Jan van Eck
 */
public final class LargeBooleanArray implements Cloneable
{
    /**
     * The number of bits to use for each individual array.
     */
    public static final byte ARRAY_BIT_SIZE = 30;
    /**
     * The maximum size of each individual array.
     */
    public static final int MAX_SIZE_ARRAY = 1 << ARRAY_BIT_SIZE;
    /**
     * Minimum initial capacity of the array.
     */
    public static final long MINIMUM_INITIAL_CAPACITY = 10;
    /**
     * The relative increase in the capacity when increasing the capacity.
     */
    public static final double RELATIVE_CAPACITY_INCREASE = 0.5;
    /**
     * The array mask that is used to obtain the proper lower index.
     */
    private static final long ARRAY_MASK = MAX_SIZE_ARRAY - 1;

    private long capacity;
    private long size;
    private boolean[][] values;

    public LargeBooleanArray(long size)
    {
        if (size < 0)
            throw new IllegalArgumentException("Size cannot be negative.");

        this.capacity = Math.max(size, MINIMUM_INITIAL_CAPACITY);
        this.size = size;

        int nSegments = getSegment(this.capacity);
        if (getOffset(this.capacity) > 0)
            nSegments += 1;
        this.values = new boolean[nSegments][];

        long remainingLength = this.capacity;
        int segment = 0;
        while (remainingLength > MAX_SIZE_ARRAY)
        {
            this.values[segment] = new boolean[MAX_SIZE_ARRAY];
            remainingLength -= MAX_SIZE_ARRAY;
            segment++;
        }
        if (remainingLength > 0)
            this.values[segment] = new boolean[(int)remainingLength];
    }

    public LargeBooleanArray(long size, boolean constant)
    {
        this(size);
        this.fill(constant);
    }

    public LargeBooleanArray(boolean[] values)
    {
        this(values.length);

        int offset = 0;
        int segment = 0;
        for (boolean b : values)
        {
            if (offset >= MAX_SIZE_ARRAY)
            {
                offset = 0;
                segment++;
            }
            this.values[segment][offset] = b;
            offset++;
        }
    }

    public static int getSegment(long index)
    {
        return (int)(index >>> ARRAY_BIT_SIZE);
    }

    public static int getOffset(long index)
    {
        return (int)(index & ARRAY_MASK);
    }

    public int nSegments()
    {
        return values.length;
    }

    public int length(int segment)
    {
        return values[segment].length;
    }

    public boolean get(long index)
    {
        return this.values[getSegment(index)][getOffset(index)];
    }

    public boolean get(int segment, int offset)
    {
        return this.values[segment][offset];
    }

    public void set(long index, boolean value)
    {
        this.values[getSegment(index)][getOffset(index)] = value;
    }

    public void set(int segment, int offset, boolean value)
    {
        this.values[segment][offset] = value;
    }

    public void fill(boolean constant)
    {
        fill(0, this.size, constant);
    }

    public void fill(long from, long to, boolean constant)
    {
        int segmentTo = getSegment(to);
        int offsetTo = getOffset(to);

        int segment = getSegment(from);
        Arrays.fill(this.values[segment], getOffset(from),
                segment == segmentTo ? getOffset(to) : this.values[segment].length, constant);
        segment++;

        for (; segment < segmentTo; segment++)
            Arrays.fill(this.values[segment], 0, this.values[segment].length, constant);

        if (segment == segmentTo && offsetTo > 0)
            Arrays.fill(this.values[segment], 0, offsetTo, constant);
    }

    public void append(boolean value)
    {
        ensureCapacity(size + 1);
        set(size, value);
        size++;
    }

    public void push(boolean value)
    {
        append(value);
    }

    public boolean pop()
    {
        boolean value = get(size - 1);
        size--;
        return value;
    }

    public void clear()
    {
        size = 0;
    }

    public void ensureCapacity(long minCapacity)
    {
        long oldCapacity = capacity;
        if (minCapacity > oldCapacity)
        {
            long newCapacity = (long)((1 + RELATIVE_CAPACITY_INCREASE) * oldCapacity);
            if (newCapacity < minCapacity)
                newCapacity = minCapacity;

            int nNewSegments = getSegment(newCapacity);
            if (getOffset(newCapacity) > 0)
                nNewSegments += 1;
            boolean[][] newValues = new boolean[nNewSegments][];

            int nOldSegments = getSegment(oldCapacity);
            if (getOffset(oldCapacity) > 0)
                nOldSegments += 1;

            long remainingLength = newCapacity;
            int segment = 0;
            for (; segment < nOldSegments - 1; segment++)
            {
                newValues[segment] = values[segment];
                remainingLength -= values[segment].length;
            }

            if (remainingLength > MAX_SIZE_ARRAY)
                newValues[segment] = Arrays.copyOf(values[segment], MAX_SIZE_ARRAY);
            else
                newValues[segment] = Arrays.copyOf(values[segment], (int)remainingLength);

            remainingLength -= newValues[segment].length;
            segment++;

            while (remainingLength > MAX_SIZE_ARRAY)
            {
                newValues[segment] = new boolean[MAX_SIZE_ARRAY];
                remainingLength -= MAX_SIZE_ARRAY;
                segment++;
            }

            if (remainingLength > 0)
                newValues[segment] = new boolean[(int)remainingLength];

            this.values = newValues;
            this.capacity = newCapacity;
        }
    }

    public long size()
    {
        return size;
    }

    public long capacity()
    {
        return capacity;
    }

    public boolean[] toArray()
    {
        int n = (int)size;
        boolean[] out = new boolean[n];
        int offset = 0;
        int segment = 0;
        for (int i = 0; i < n; i++)
        {
            if (offset >= values[segment].length)
            {
                offset = 0;
                segment++;
            }
            out[i] = values[segment][offset];
            offset++;
        }
        return out;
    }
}