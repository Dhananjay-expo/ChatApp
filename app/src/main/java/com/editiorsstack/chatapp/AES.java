package com.editiorsstack.chatapp;

public class AES {
    private static final int[] arr ={0x54,0x68,0x61,0x74,0x73,0x20,0x6D,0x79,0x20,0x4B,0x75,0x6E,0x67,0x20,0x46,0x75};
    static byte [] getKey(){
        byte [] key = new byte[16];
        for(int i=0;i<16;i++){
            key[i] = (byte)arr[i];
        }
        return key;
    }
}
