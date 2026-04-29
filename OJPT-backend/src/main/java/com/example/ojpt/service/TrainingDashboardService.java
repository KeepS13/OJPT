package com.example.ojpt.service;

import com.example.ojpt.vo.training.dashboard.UserTrainingDashboardVO;

public interface TrainingDashboardService {

    UserTrainingDashboardVO getTrainingDashboard(Long userId);
}
