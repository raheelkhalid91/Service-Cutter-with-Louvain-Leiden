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

/**
 * Fast math functions.
 *
 * @author Ludo Waltman
 * @author Nees Jan van Eck
 * @author Vincent Traag
 */
public final class FastMath
{
    private static final int N = 10; // Number of terms in the Taylor series

    private FastMath()
    {
    }

    /**
     * Approximates e^x using a Taylor series.
     *
     * @param x Exponent
     *
     * @return Approximation of e^x
     */
    public static double fastExp(double x)
    {
        double result;
        int i;

        result = 1.0;
        for (i = 1; i <= N; i++)
            result += Math.pow(x, i) / factorial(i);
        return result;
    }

    private static long factorial(int n)
    {
        long result;
        int i;

        result = 1;
        for (i = 1; i <= n; i++)
            result *= i;
        return result;
    }
}