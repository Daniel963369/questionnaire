package com.example.questionnaire.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.questionnaire.entity.Question;
import com.example.questionnaire.entity.User;

@Repository
public interface UserDao extends JpaRepository<User, String> {

	
	public List<User>findAllByQnId(int qnId);
}
