package com.github.cristianrb.smartnews.rest;

import com.github.cristianrb.smartnews.cf.DataModel;
import com.github.cristianrb.smartnews.cf.Recommender;
import com.github.cristianrb.smartnews.entity.*;
import com.github.cristianrb.smartnews.service.contributions.ContributionsMapper;
import com.github.cristianrb.smartnews.service.contributions.ContributionsService;
import com.github.cristianrb.smartnews.service.contributions.UsersService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class ContributionController {

    private final ContributionsService contributionsService;
    private final UsersService usersService;
    private final DataModel dataModel;
    private static final int PAGE_SIZE = 10;

    @Autowired
    public ContributionController(ContributionsService contributionsService, UsersService usersService) {
        this.contributionsService = contributionsService;
        this.usersService = usersService;
        this.dataModel = new DataModel(usersService);
    }

    @ApiOperation(value = "Retrieves at most 10 contributions of a given page")
    @GetMapping("/latest")
    public Page<Contribution> getAllContributions(@RequestParam(name = "page", defaultValue = "0") Integer page) {
        return contributionsService.getAll(PageRequest.of(page, PAGE_SIZE))
                .map(ContributionsMapper::mapContributionDAOToContribution);
    }

    @ApiOperation(value = "Retrieve a contribution of a given id")
    @GetMapping("/contributions")
    public Contribution getContributionById(@RequestParam(name = "id") Integer id, Principal principal) {
        ContributionDAO contributionDAO = contributionsService.getContributionById(id);
        if (principal != null) {
            Optional<UserDAO> optionalUser = usersService.getUser(principal.getName());
            UserDAO user = optionalUser.orElseGet(() -> new UserDAO(principal.getName()));
            Set<UserContributionDAO> contributionsVisited = user.getContributionsVisited();
            if (contributionsVisited == null) {
                contributionsVisited = new HashSet<>();
                user.setContributionsVisited(contributionsVisited);
            }
            contributionsVisited.add(new UserContributionDAO(user, contributionDAO));
            usersService.saveUser(user);
        }
        return ContributionsMapper.mapContributionDAOToContribution(contributionDAO);
    }

    @ApiOperation(value = "Retrieves the contributions recommended for a given user")
    @GetMapping("/myfeed")
    public Map<User, Map<Contribution, Double>> getFeed(@RequestParam(name = "userId") String userId, Principal principal) {
        Map<User, Map<Contribution, Double>> data = null;
        //if (userId.equals(principal.getName())) {
            data = dataModel.createDataModel();
        //}

        Recommender recomm = new Recommender(data);

        return recomm.getRecommendationMatrix();
    }
}
