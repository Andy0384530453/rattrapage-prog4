package com.example.demo.repository;

import com.example.demo.repository.model.Submission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, String> {

  @Override
  List<Submission> findAll();
}
