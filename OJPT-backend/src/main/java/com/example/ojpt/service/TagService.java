package com.example.ojpt.service;

import com.example.ojpt.dto.TagCreateDTO;
import com.example.ojpt.dto.TagUpdateDTO;
import com.example.ojpt.vo.TagVO;

import java.util.List;

public interface TagService {

    TagVO createTag(TagCreateDTO dto);

    void updateTag(Long tagId, TagUpdateDTO dto);

    void deleteTag(Long tagId);

    List<TagVO> listAll();

    void addTagToProblem(Long problemId, Long tagId);

    void removeTagFromProblem(Long problemId, Long tagId);
}

