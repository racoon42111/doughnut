package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.AnswerResult;
import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import com.odde.doughnut.models.*;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.testability.TestabilitySettings;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.validation.Valid;

@Controller
@RequestMapping("/reviews")
public class ReviewController extends ApplicationMvcController  {
    private final ModelFactoryService modelFactoryService;


    @Resource(name = "testabilitySettings")
    private final TestabilitySettings testabilitySettings;

    public ReviewController(CurrentUserFetcher currentUserFetcher, ModelFactoryService modelFactoryService, TestabilitySettings testabilitySettings) {
        super(currentUserFetcher);
        this.modelFactoryService = modelFactoryService;
        this.testabilitySettings = testabilitySettings;
    }

    @GetMapping("")
    public String index() {
        return "vuejsed";
    }

    @GetMapping("/initial")
    public String initialReview(Model model) {
        return "vuejsed";
    }

    @PostMapping(path="", params="skip")
    @Transactional
    public String skip(@Valid ReviewPoint reviewPoint, @Valid ReviewSetting reviewSetting) {
        UserModel userModel = currentUserFetcher.getUser();
        ReviewPointModel reviewPointModel = modelFactoryService.toReviewPointModel(reviewPoint);
        reviewPoint.setRemovedFromReview(true);
        reviewPointModel.initialReview(userModel, reviewSetting, testabilitySettings.getCurrentUTCTimestamp());
        return "redirect:/reviews/initial";
    }

    @GetMapping("/repeat")
    public String repeatReview() {
        return "vuejsed";
    }

    @PostMapping(path="/{reviewPoint}", params="remove")
    public String removeFromRepeating(@Valid ReviewPoint reviewPoint) {
        reviewPoint.setRemovedFromReview(true);
        modelFactoryService.reviewPointRepository.save(reviewPoint);
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="again")
    public String doRepeatAgain(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).increaseRepetitionCountAndSave();
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="satisfying")
    public String doRepeat(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).repeated(testabilitySettings.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="sad")
    public String doRepeatSad(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).repeatedSad(testabilitySettings.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

    @PostMapping(path="/{reviewPoint}", params="happy")
    public String doRepeatHappy(@Valid ReviewPoint reviewPoint) {
        modelFactoryService.toReviewPointModel(reviewPoint).repeatedHappy(testabilitySettings.getCurrentUTCTimestamp());
        return "redirect:/reviews/repeat";
    }

}
