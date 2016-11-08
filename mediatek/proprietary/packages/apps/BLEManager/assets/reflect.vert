/**************************************************************************
 *
 * Copyright (c) 2012 MediaTek Inc. All Rights Reserved.
 * --------------------
 * This software is protected by copyright and the information contained
 * herein is confidential. The software may not be copied and the information
 * contained herein may not be used or disclosed except with the written
 * permission of MediaTek Inc.
 *
 ***************************************************************************/
/** \file
 * A vertex shader for reflection effect.
 *
 */

/* Transformation uniforms */
uniform mat4 u_t_modelViewProjection;

attribute vec4 a_position;    // Vertex position (model space)
attribute vec2 a_uv0;         // Texture coordinate

varying mediump vec4 v_texCoord1;

void main()
{
  gl_Position = u_t_modelViewProjection * a_position;
  v_texCoord1 = gl_Position;
}
