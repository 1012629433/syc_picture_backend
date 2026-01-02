package com.syc.sycpicturebackend.model.entity;

import lombok.Data;

import java.util.List;

@Data
public class PictureTagCategory {
    private List<String> tagList;
    private List<String> CategoryList;
}
