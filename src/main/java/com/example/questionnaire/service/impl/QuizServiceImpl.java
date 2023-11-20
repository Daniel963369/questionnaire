package com.example.questionnaire.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.questionnaire.Repository.QuestionDao;
import com.example.questionnaire.Repository.QuestionnaireDao;
import com.example.questionnaire.constants.RtnCode;
import com.example.questionnaire.entity.Question;
import com.example.questionnaire.entity.Questionnaire;
import com.example.questionnaire.service.ifs.QuizService;
import com.example.questionnaire.vo.QuizReq;
import com.example.questionnaire.vo.QuizRes;
import com.example.questionnaire.vo.QuizVo;
import com.example.questionnaire.vo.QuestionRes;
import com.example.questionnaire.vo.QuestionnaireRes;

@Service
public class QuizServiceImpl implements QuizService{
	
	@Autowired
	private QuestionnaireDao questionnaireDao;
	
	@Autowired
	private QuestionDao questionDao;

	@Transactional
	@Override
	public QuizRes create(QuizReq req) {
		QuizRes checkResult = checkParam(req);
		List<Question>quList = req.getQuestionList();
		if( checkResult != null) {
			return checkResult;
		}
		int quid = questionnaireDao.save(req.getQuestionnaire()).getId();
//		questionnaireDao.save(req.getQuestionnaire());
//		List<Question> quList = req.getQuestionList();
		if (quList.isEmpty()) {
			return new QuizRes(RtnCode.SUCCESSFUL);
		}
		for(Question qu: quList) {
			qu.setQnId(quid);
		}
		
		questionDao.saveAll(quList);
		return new QuizRes(RtnCode.SUCCESSFUL);
		
	}

	private QuizRes checkParam(QuizReq req) {
		Questionnaire qn = req.getQuestionnaire();
		if(!StringUtils.hasText(qn.getTitle()) || !StringUtils.hasText(qn.getDescription()) 
				|| qn.getStartDate() == null || qn.getEndDate() == null || qn.getStartDate().isAfter(qn.getEndDate()))
			 {
			return new QuizRes(RtnCode.QUESTIONNAIRE_PARAM_ERROR);
		}
		
		
		List<Question> quList = req.getQuestionList();
		for(Question qu:quList) {
			if(qu.getQuid() <= 0 || !StringUtils.hasText(qu.getqTitle()) 
					|| !StringUtils.hasText(qu.getOptionType())) {
				return new QuizRes(RtnCode.QUESTIONNAIRE_ID_PARAM_ERROR);
			}
		}
		return null;
	}
	
	@Transactional
	@Override
	public QuizRes update(QuizReq req) {
		QuizRes checkResult = checkParam(req);
		if( checkResult != null) {
			return checkResult;
		}
		checkResult = checkQuestionnaireId(req);
		if(checkResult != null) {
			return checkResult;
		}
		Optional<Questionnaire> qnOp = questionnaireDao.findById(req.getQuestionnaire().getId());
		if(qnOp.isEmpty()) {
			return new QuizRes(RtnCode.QUESTIONNAIRE_ID_NOT_FOUND);
		}
		Questionnaire qn = qnOp.get();
		//�i�H�ק諸����:
		//1.�|���o��:is_published == false �i�H�ק�
		//2.�w�o�����|���}�l�i��:is_published == true ���e�ɶ������p��start_date
		if(qn.isPublished()== false || (qn.isPublished()==true && LocalDate.now().isBefore(qn.getStartDate()))) {
			questionnaireDao.save(req.getQuestionnaire());
			questionDao.saveAll(req.getQuestionList());
			return new QuizRes(RtnCode.SUCCESSFUL);
		}
		return new QuizRes(RtnCode.UPDATE_ERROR);
	}

	
	

	
	
	private QuizRes checkQuestionnaireId(QuizReq req) {
		if(req.getQuestionnaire().getId()<= 0) {
			return new QuizRes(RtnCode.QUESTIONNAIRE_ID_PARAM_ERROR);
		}
		List<Question> quList = req.getQuestionList();
		for(Question qu : quList) {
			if(qu.getQnId() != req.getQuestionnaire().getId()) {
				return new QuizRes(RtnCode.QUESTIONNAIRE_ID_PARAM_ERROR);
			}
		}
		return null;
	}

	@Override
	public QuizRes deleteQustionnaire(List<Integer> qnIdList) {
		List<Questionnaire> qnList = questionnaireDao.findByIdIn(qnIdList);
		List<Integer> idList = new ArrayList<>();
		for( Questionnaire qn :qnList) {
			if(qn.isPublished() == false || qn.isPublished()== true && LocalDate.now().isBefore(qn.getStartDate())) {
//				questionnaireDao.deleteById(qn.getId());
				idList.add(qn.getId());
			}
		}
		if(!idList.isEmpty()) {
			questionnaireDao.deleteAllById(idList);
			questionDao.deleteAllByQnIdIn(idList);
		}
		return new QuizRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public QuizRes deleteQustion(int qnId, List<Integer> quIdList) {
	Optional<Questionnaire> qnOp = questionnaireDao.findById(qnId);
	if(qnOp.isEmpty()) {
		return new QuizRes(RtnCode.SUCCESSFUL);
	}
	Questionnaire qn = qnOp.get();
	if(qn.isPublished() == false || qn.isPublished()== true && LocalDate.now().isBefore(qn.getStartDate())) {
		
		questionDao.deleteAllByQnIdAndQuIdIn(qnId, quIdList);
	}
	
	return new QuizRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public QuizRes search(String title, LocalDate startDate, LocalDate endDate) {
//		title = StringUtils.hasText(title)? title:"";
//		startDate =startDate != null ? startDate:LocalDate.of(1971,1,1);
//		endDate = endDate != null? endDate: LocalDate.of(2099,12,31);
		if(!StringUtils.hasText(title)) {
			title = "";
		}
		if(startDate == null) {
			startDate = LocalDate.of(1971,1,1);
			
		}
		if(endDate == null) {
			endDate = LocalDate.of(2099,12,31);
		}
		List<Questionnaire> qnList = questionnaireDao.findByTitleContainingAndStartDateGreaterThanEqualAndEndDateLessThanEqual(title, startDate, endDate);
		List<Integer> qnIdList = new ArrayList<>();
		for(Questionnaire qu:qnList) {
			qnIdList.add(qu.getId());
		}
		
		
		List<Question>quList = questionDao.findAllByQnIdIn(qnIdList);
		List<QuizVo> quizVoList = new ArrayList<>();
		for(Questionnaire qn :qnList) {
			QuizVo vo = new QuizVo();
			vo.setQuestionnaire(qn);
			List<Question>questionList = new ArrayList<>();
			for(Question qu :quList) {
				if(qu.getQnId() == qn.getId()) {
					questionList.add(qu);
				}
			}
		vo.setQuestionList(questionList);
		quizVoList.add(vo);
	}
	return new QuizRes(RtnCode.SUCCESSFUL);
	}

	@Override
	public QuestionnaireRes searchQuestionnaireList(String title, LocalDate startDate, LocalDate endDate,boolean isAll) {
		title = StringUtils.hasText(title)? title:"";
		startDate =startDate != null ? startDate:LocalDate.of(1971,1,1);
		endDate = endDate != null? endDate: LocalDate.of(2099,12,31);
		List<Questionnaire> qnList = new ArrayList<>();
		if(!isAll) {
			 qnList = questionnaireDao.findByTitleContainingAndStartDateGreaterThanEqualAndEndDateLessThanEqualAndPublishedTrue(title, startDate, endDate);
		}else {
			qnList = questionnaireDao.findByTitleContainingAndStartDateGreaterThanEqualAndEndDateLessThanEqual(title, startDate, endDate);
		}
		return new QuestionnaireRes(qnList,RtnCode.SUCCESSFUL);
	}

	@Override
	public QuestionRes searchQuestionList(int qnId) {
		if(qnId<=0) {
			return new QuestionRes (null,RtnCode.QUESTIONNAIRE_ID_PARAM_ERROR);
		}
		
		List<Question> quList = questionDao.findAllByQnIdIn(new ArrayList<>(Arrays.asList(qnId)));
		return new QuestionRes(quList,RtnCode.SUCCESSFUL);
	}
}