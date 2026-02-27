package com.example.ojpt.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.ojpt.dto.TagCreateDTO;
import com.example.ojpt.dto.TagUpdateDTO;
import com.example.ojpt.entity.ProblemTag;
import com.example.ojpt.entity.Tag;
import com.example.ojpt.exception.BusinessException;
import com.example.ojpt.mapper.ProblemTagMapper;
import com.example.ojpt.mapper.TagMapper;
import com.example.ojpt.service.TagService;
import com.example.ojpt.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ProblemTagMapper problemTagMapper;

    @Override
    @Transactional
    public TagVO createTag(TagCreateDTO dto) {
        // 名称唯一校验
        Tag existed = tagMapper.selectOne(
            new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, dto.getName())
        );
        if (existed != null) {
            throw BusinessException.badRequest("标签名称已存在");
        }

        Tag tag = new Tag();
        tag.setName(dto.getName());
        tag.setType(dto.getType());
        tagMapper.insert(tag);
        return toVO(tag);
    }

    @Override
    @Transactional
    public void updateTag(Long tagId, TagUpdateDTO dto) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw BusinessException.notFound("标签不存在");
        }

        if (dto.getName() != null && !dto.getName().equals(tag.getName())) {
            // 新名称不能冲突
            Tag existed = tagMapper.selectOne(
                new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, dto.getName())
            );
            if (existed != null) {
                throw BusinessException.badRequest("标签名称已存在");
            }
            tag.setName(dto.getName());
        }

        if (dto.getType() != null) {
            tag.setType(dto.getType());
        }

        tagMapper.updateById(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            return;
        }

        // 先删除关联
        problemTagMapper.delete(
            new LambdaQueryWrapper<ProblemTag>()
                .eq(ProblemTag::getTagId, tagId)
        );

        tagMapper.deleteById(tagId);
    }

    @Override
    public List<TagVO> listAll() {
        List<Tag> tags = tagMapper.selectList(null);
        return tags.stream()
            .map(this::toVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTagToProblem(Long problemId, Long tagId) {
        // 简单幂等处理：已存在则直接返回
        ProblemTag existed = problemTagMapper.selectOne(
            new LambdaQueryWrapper<ProblemTag>()
                .eq(ProblemTag::getProblemId, problemId)
                .eq(ProblemTag::getTagId, tagId)
        );
        if (existed != null) {
            return;
        }

        ProblemTag pt = new ProblemTag();
        pt.setProblemId(problemId);
        pt.setTagId(tagId);
        problemTagMapper.insert(pt);
    }

    @Override
    @Transactional
    public void removeTagFromProblem(Long problemId, Long tagId) {
        problemTagMapper.delete(
            new LambdaQueryWrapper<ProblemTag>()
                .eq(ProblemTag::getProblemId, problemId)
                .eq(ProblemTag::getTagId, tagId)
        );
    }

    private TagVO toVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setType(tag.getType());
        return vo;
    }
}

