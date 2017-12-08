package com.zobot.androidtoandroid.model;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by jvazquez on 07/12/2017.
 */

public class Model implements Serializable{
    @Override
    public String toString() {
        return new Gson().toJson(this);
    }
}
