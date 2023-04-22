
#include <stdint.h>
#include <stdlib.h>

#include "NativeTools.h"


JNIEXPORT void JNICALL Java_NativeTools_calHist(
  JNIEnv *env,
  jclass cls,
  jint histSize,
  jint width,
  jint height,
  jint beginY,
  jint endY,
  jintArray indexDown,
  jfloatArray weightDown,
  jintArray indexUp,
  jfloatArray weightUp,
  jfloatArray input,
  jfloatArray output,
  jfloatArray tempBuffer)
{
    int i;
    int x;
    int y;
    int centerX;
    int centerY;
    int windowRadius = histSize - 1;
    int windowSize = 2 * windowRadius + 1;
    jboolean isCopy;
    jint* indexDownPtr = (*env)->GetIntArrayElements(env, indexDown, NULL);
    jfloat* weightDownPtr = (*env)->GetFloatArrayElements(env, weightDown, NULL);
    jint* indexUpPtr = (*env)->GetIntArrayElements(env, indexUp, NULL);
    jfloat* weightUpPtr = (*env)->GetFloatArrayElements(env, weightUp, NULL);
    jfloat* inputPtr = (*env)->GetFloatArrayElements(env, input, NULL);
    jfloat* outputPtr = (*env)->GetFloatArrayElements(env, output, &isCopy);
    jfloat* tempBufferPtr = NULL;
    jfloat* linePtr;

    if (isCopy || 1) {
      (*env)->ReleaseFloatArrayElements(env, output, outputPtr, JNI_ABORT);
      outputPtr = NULL;
      tempBufferPtr = (*env)->GetFloatArrayElements(env, tempBuffer, NULL);
    }

    for (centerY = beginY; centerY < endY; centerY++) {
      linePtr = tempBufferPtr ? tempBufferPtr : &outputPtr[centerY * width * histSize];
      for (i = 0; i < width * histSize; i++) {
        linePtr[i] = 0;
      }
      for (centerX = 0; centerX < width; centerX++) {
        int startX = centerX - windowRadius;
        int startY = centerY - windowRadius;
        int offset = centerX * histSize;
        int pixelsOffset = startX + startY * width;
        if (startX < 0 || startY < 0 || startX + windowSize > width || startY + windowSize > height) {
          continue;
        }
        for (y = 0; y < windowSize; y++) {
          for (x = 0; x < windowSize; x++) {
            linePtr[offset + indexUpPtr[x + y * windowSize]] += weightUpPtr[x + y * windowSize] * inputPtr[pixelsOffset + x + y * width];
            linePtr[offset + indexDownPtr[x + y * windowSize]] += weightDownPtr[x + y * windowSize] * inputPtr[pixelsOffset + x + y * width];
          }
        }
      }
      if (tempBufferPtr != NULL) {
        (*env)->SetFloatArrayRegion(env, output, centerY * width * histSize, width * histSize, tempBufferPtr);
      }
    }

    (*env)->ReleaseIntArrayElements(env, indexDown, indexDownPtr, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, weightDown, weightDownPtr, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, indexUp, indexUpPtr, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, weightUp, weightUpPtr, JNI_ABORT);
    (*env)->ReleaseFloatArrayElements(env, input, inputPtr, JNI_ABORT);
    if (outputPtr != NULL) {
      (*env)->ReleaseFloatArrayElements(env, output, outputPtr, 0);
    }
    if (tempBufferPtr != NULL) {
      (*env)->ReleaseFloatArrayElements(env, tempBuffer, tempBufferPtr, JNI_ABORT);
    }
}
