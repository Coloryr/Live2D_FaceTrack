package com.coloryr.facetrack.live2d;

public class ArgF {
    private final int size;
    private final float[] list;
    private int index;

    public ArgF(int size, float value) {
        this(size);
        for (int a = 0; a < size; a++) {
            list[a] = value;
        }
    }

    public ArgF(int size) {
        this.size = size;
        list = new float[size];
    }

    public void add(float value) {
        list[index] = value;
        index++;
        if (index >= size) {
            index = 0;
        }
    }

    public float get() {
        float all = 0f;
        for (float item : list) {
            all += item;
        }
        return all / size;
    }
}
