/******************************************************************************
 *  This program is protected under international and U.S. copyright laws as
 *  an unpublished work. This program is confidential and proprietary to the
 *  copyright owners. Reproduction or disclosure, in whole or in part, or the
 *  production of derivative works therefrom without the express permission of
 *  the copyright owners is prohibited.
 *
 *                 Copyright (C) 2014 by Dolby Laboratories,
 *                             All rights reserved.
 ******************************************************************************/

package com.dolby.api;

/**
 * DsAccessException class provides the error code for the application.
 */
public class DsAccessException extends Exception
{
    private static final long serialVersionUID = -5376758782028938790L;

    public DsAccessException(String detail)
    {
        super(detail);
    }
}

