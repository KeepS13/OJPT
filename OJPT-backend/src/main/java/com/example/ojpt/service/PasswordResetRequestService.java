package com.example.ojpt.service;

import com.example.ojpt.vo.PasswordResetRequestVO;

import java.util.List;

public interface PasswordResetRequestService {

    void submitRequest(String account);

    List<PasswordResetRequestVO> listRequests(String status);

    void approveRequest(Long requestId, Long reviewerId);

    void rejectRequest(Long requestId, Long reviewerId);
}
