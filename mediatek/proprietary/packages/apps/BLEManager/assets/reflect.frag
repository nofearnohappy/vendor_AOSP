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
 * A fragment shader for ripple effect
 *
 */

// Reflection brightness. Higher values result in brighter reflections.
// This is now defined as a vector because at least one version of the IMG
// shader compiler will not compile the code when it is defined as a single
// float.
#define REFLECTION_BRIGHTNESS vec4( 0.7, 0.7, 0.7, 0.7 )

precision lowp float;
precision mediump int;

/* Material uniforms */
uniform sampler2D u_mirrorTexture;

varying mediump vec4 v_texCoord1;

void main()
{
  vec2 screen = (v_texCoord1.xy / v_texCoord1.w) * 0.5 + 0.5;
  screen.y = 1.0 - screen.y;

  gl_FragColor = REFLECTION_BRIGHTNESS * texture2D(u_mirrorTexture, screen);

  if (screen.y >= 0.5 && screen.y < 0.8) {
    gl_FragColor.a *= ((0.8 - screen.y) * 0.66 + 0.8);
  } else if (screen.y >= 0.8 && screen.y <= 1.0) {
    gl_FragColor.a *= ((1.0 - screen.y) * 3.0 + 0.2); 
  } 
}
