package org.workswap.core.services.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.workswap.core.datasource.central.model.Resume;
import org.workswap.core.datasource.central.model.User;
import org.workswap.core.datasource.central.repository.ResumeRepository;
import org.workswap.core.services.ResumeService;

import lombok.RequiredArgsConstructor;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;

    @Override
    public List<Resume> findPublishedResumes(Pageable pageable) {
        return resumeRepository.findByPublishedTrue(pageable).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeByIdWithUser(Long id) {
        return resumeRepository.findByIdWithUser(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeByUser(User user) {
        return resumeRepository.findByUser(user).orElse(null);
    }
}
