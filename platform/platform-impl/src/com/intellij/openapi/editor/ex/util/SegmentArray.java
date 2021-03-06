/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.editor.ex.util;

import com.intellij.openapi.diagnostic.Logger;

import java.lang.reflect.Array;

/**
 * This class is a data structure specialized for working with the indexed segments, i.e. it holds numerous mappings like
 * {@code 'index <-> (start; end)'} and provides convenient way for working with them, e.g. find index by particular offset that
 * belongs to target <code>(start; end)</code> segment etc.
 * <p/>
 * Not thread-safe.
 */
public class SegmentArray {
  private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.editor.ex.util.SegmentArray");

  protected SegmentArray() {
    myStarts = new int[INITIAL_SIZE];
    myEnds = new int[INITIAL_SIZE];
  }

  protected void setElementAt(int i, int startOffset, int endOffset) {
    if (startOffset < 0) {
      LOG.error("Invalid startOffset:" + startOffset);
    }
    if (endOffset < 0) {
      LOG.error("Invalid endOffset:" + endOffset);
    }

    if (i >= mySegmentCount) {
      mySegmentCount = i + 1;
    }

    myStarts = reallocateArray(myStarts, i);
    myStarts[i] = startOffset;

    myEnds = reallocateArray(myEnds, i);
    myEnds[i] = endOffset;
  }

  public void replace(int startOffset, SegmentArray data, int len) {
    System.arraycopy(data.myStarts, 0, myStarts, startOffset, len);
    System.arraycopy(data.myEnds, 0, myEnds, startOffset, len);
  }

  protected static int[] reallocateArray(int[] array, int index) {
    if (index < array.length) return array;

    int newArraySize = array.length;
    if (newArraySize == 0) {
      newArraySize = 16;
    }
    while (newArraySize <= index) {
      newArraySize = newArraySize * 120 / 100;
    }
    int[] newArray = new int[newArraySize];
    System.arraycopy(array, 0, newArray, 0, array.length);
    return newArray;
  }

  @SuppressWarnings({"unchecked"})
  protected static <T> T[] reallocateArray(T[] array, int index) {
    if (index < array.length) return array;

    int newArraySize = array.length;
    if (newArraySize == 0) {
      newArraySize = 16;
    }
    while (newArraySize <= index) {
      newArraySize = newArraySize * 120 / 100;
    }

    T[] newArray = (T[])Array.newInstance(array.getClass().getComponentType(), newArraySize);

    System.arraycopy(array, 0, newArray, 0, array.length);
    return newArray;
  }

  protected static short[] reallocateArray(short[] array, int index) {
    if (index < array.length) return array;

    int newArraySize = array.length;
    if (newArraySize == 0) {
      newArraySize = 16;
    }
    while (newArraySize <= index) {
      newArraySize = newArraySize * 120 / 100;
    }
    short[] newArray = new short[newArraySize];
    System.arraycopy(array, 0, newArray, 0, array.length);
    return newArray;
  }

  public final int findSegmentIndex(int offset) {
    if (mySegmentCount <= 0) {
      if (offset == 0) return 0;
      throw new IllegalStateException("no segments available");
    }

    final int lastValidOffset = getLastValidOffset();

    if (offset > lastValidOffset || offset < 0) {
      throw new IndexOutOfBoundsException("Wrong offset: " + offset + ". Should be in range: [0, " + lastValidOffset + "]");
    }

    final int lastValidIndex = mySegmentCount - 1;
    if (offset == lastValidOffset) return lastValidIndex;

    int start = 0;
    int end = lastValidIndex;

    while (start < end) {
      int i = (start + end) / 2;
      if (offset < myStarts[i]) {
        end = i - 1;
      }
      else if (offset >= myEnds[i]) {
        start = i + 1;
      }
      else {
        return i;
      }
    }

    // This means that there is a gap at given offset
    assert myStarts[start] <= offset && offset < myEnds[start] : start;

    return start;
  }

  public int getLastValidOffset() {
    return mySegmentCount == 0 ? 0 : myEnds[mySegmentCount - 1];
  }

  public final void changeSegmentLength(int startIndex, int change) {
    if (startIndex >= 0 && startIndex < mySegmentCount) {
      myEnds[startIndex] += change;
    }
    shiftSegments(startIndex + 1, change);
  }

  public final void shiftSegments(int startIndex, int shift) {
    for (int i = startIndex; i < mySegmentCount; i++) {
      myStarts[i] += shift;
      myEnds[i] += shift;
      if (myStarts[i] < 0 || myEnds[i] < 0) {
        LOG.error("Error shifting segments: myStarts[" + i + "] = " + myStarts[i] + ", myEnds[" + i + "] = " + myEnds[i]);
      }
    }
  }

  public void removeAll() {
    mySegmentCount = 0;
  }

  public void remove(int startIndex, int endIndex) {
    myStarts = remove(myStarts, startIndex, endIndex);
    myEnds = remove(myEnds, startIndex, endIndex);
    mySegmentCount -= endIndex - startIndex;
  }

  protected int[] remove(int[] array, int startIndex, int endIndex) {
    if (endIndex < mySegmentCount) {
      System.arraycopy(array, endIndex, array, startIndex, mySegmentCount - endIndex);
    }
    return array;
  }

  protected <T> T[] remove(T[] array, int startIndex, int endIndex) {
    if (endIndex < mySegmentCount) {
      System.arraycopy(array, endIndex, array, startIndex, mySegmentCount - endIndex);
    }
    return array;
  }


  protected short[] remove(short[] array, int startIndex, int endIndex) {
    if (endIndex < mySegmentCount) {
      System.arraycopy(array, endIndex, array, startIndex, mySegmentCount - endIndex);
    }
    return array;
  }

  protected long[] remove(long[] array, int startIndex, int endIndex) {
    if (endIndex < mySegmentCount) {
      System.arraycopy(array, endIndex, array, startIndex, mySegmentCount - endIndex);
    }
    return array;
  }

  protected void insert(SegmentArray segmentArray, int startIndex) {
    myStarts = insert(myStarts, segmentArray.myStarts, startIndex, segmentArray.getSegmentCount());
    myEnds = insert(myEnds, segmentArray.myEnds, startIndex, segmentArray.getSegmentCount());
    mySegmentCount += segmentArray.getSegmentCount();
  }

  protected int[] insert(int[] array, int[] insertArray, int startIndex, int insertLength) {
    int[] newArray = reallocateArray(array, mySegmentCount + insertLength);
    if (startIndex < mySegmentCount) {
      System.arraycopy(newArray, startIndex, newArray, startIndex + insertLength, mySegmentCount - startIndex);
    }
    System.arraycopy(insertArray, 0, newArray, startIndex, insertLength);
    return newArray;
  }

  protected <T> T[] insert(T[] array, T[] insertArray, int startIndex, int insertLength) {
    T[] newArray = reallocateArray(array, mySegmentCount + insertLength);
    if (startIndex < mySegmentCount) {
      System.arraycopy(newArray, startIndex, newArray, startIndex + insertLength, mySegmentCount - startIndex);
    }
    System.arraycopy(insertArray, 0, newArray, startIndex, insertLength);
    return newArray;
  }

  protected short[] insert(short[] array, short[] insertArray, int startIndex, int insertLength) {
    short[] newArray = reallocateArray(array, mySegmentCount + insertLength);
    if (startIndex < mySegmentCount) {
      System.arraycopy(newArray, startIndex, newArray, startIndex + insertLength, mySegmentCount - startIndex);
    }
    System.arraycopy(insertArray, 0, newArray, startIndex, insertLength);
    return newArray;
  }

  public int getSegmentStart(int index) {
    if (index < 0 || index >= mySegmentCount) {
      throw new IndexOutOfBoundsException("Wrong line: " + index + ". Available lines count: " + mySegmentCount);
    }
    return myStarts[index];
  }

  public int getSegmentEnd(int index) {
    if (index < 0 || index >= mySegmentCount) {
      throw new IndexOutOfBoundsException("Wrong line: " + index + ". Available lines count: " + mySegmentCount);
    }
    return myEnds[index];
  }


  public int getSegmentCount() {
    return mySegmentCount;
  }

  private int[] myStarts;
  private int[] myEnds;

  protected int mySegmentCount = 0;
  protected static final int INITIAL_SIZE = 64;
}

